package reader.windowsmin.app.ai

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Minimal HTTP stub server (no extra dependencies): returns status/body after an optional delay. */
private class MockServer(
    private val handler: (path: String) -> Triple<Int, String, Long>,
) : AutoCloseable {

    private val serverSocket = ServerSocket(0)
    val port: Int get() = serverSocket.localPort

    private val worker = thread(start = true, isDaemon = true) {
        while (!serverSocket.isClosed) {
            try {
                val socket = serverSocket.accept()
                thread(start = true, isDaemon = true) {
                    try {
                        serve(socket)
                    } finally {
                        runCatching { socket.close() }
                    }
                }
            } catch (_: Exception) {
                // closed
            }
        }
    }

    private fun serve(socket: Socket) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        val requestLine = reader.readLine() ?: return
        val path = requestLine.split(" ").getOrElse(1) { "/" }
        val (status, body, delayMs) = handler(path)
        if (delayMs > 0) Thread.sleep(delayMs)
        val bytes = body.toByteArray(Charsets.UTF_8)
        val reason = if (status == 200) "OK" else "Error"
        val header = "HTTP/1.1 $status $reason\r\n" +
            "Content-Type: text/event-stream\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        socket.getOutputStream().apply {
            write(header.toByteArray(Charsets.US_ASCII))
            write(bytes)
            flush()
        }
    }

    override fun close() {
        serverSocket.close()
    }
}

class SseClientTest {

    private val servers = mutableListOf<MockServer>()

    private fun server(handler: (String) -> Triple<Int, String, Long>): MockServer =
        MockServer(handler).also { servers += it }

    @AfterTest
    fun cleanup() {
        servers.forEach { runCatching { it.close() } }
    }

    @Test
    fun streamsSseDataLinesInOrder() {
        val sse = server { _ ->
            Triple(200, "data: alpha\n\ndata: beta\n\ndata: [DONE]\n\n", 0L)
        }
        val tokens = SseClient.stream("http://127.0.0.1:${sse.port}/sse")
        assertEquals(listOf("alpha", "beta"), tokens)
    }

    @Test
    fun httpErrorFallsBackToPolling() {
        val mock = server { path ->
            if (path == "/sse") Triple(500, "boom", 0L) else Triple(200, "final-answer", 0L)
        }
        val result = SseClient.streamWithFallback(
            sseUrl = "http://127.0.0.1:${mock.port}/sse",
            pollUrl = "http://127.0.0.1:${mock.port}/poll",
        )
        assertEquals(AiResponseMode.POLLING, result.mode)
        assertEquals("final-answer", result.finalText)
    }

    @Test
    fun timeoutFallsBackToPolling() {
        val mock = server { path ->
            if (path == "/sse") Triple(200, "data: late\n\n", 800L) else Triple(200, "poll-after-timeout", 0L)
        }
        val result = SseClient.streamWithFallback(
            sseUrl = "http://127.0.0.1:${mock.port}/sse",
            pollUrl = "http://127.0.0.1:${mock.port}/poll",
            readTimeoutMs = 200,
        )
        assertEquals(AiResponseMode.POLLING, result.mode)
        assertEquals("poll-after-timeout", result.finalText)
    }

    @Test
    fun cancellationStopsReadingFurtherTokens() {
        val sse = server { _ ->
            Triple(200, "data: one\n\ndata: two\n\ndata: three\n\ndata: [DONE]\n\n", 0L)
        }
        val seen = mutableListOf<String>()
        val tokens = SseClient.stream("http://127.0.0.1:${sse.port}/sse") { token ->
            seen += token
            false // cancel after the first token
        }
        assertEquals(listOf("one"), tokens)
        assertEquals(listOf("one"), seen)
    }

    @Test
    fun malformedLinesAreIgnored() {
        val sse = server { _ ->
            Triple(200, "event: message\ndata: keep\n\n:comment\n\ndata: also\n\n", 0L)
        }
        val tokens = SseClient.stream("http://127.0.0.1:${sse.port}/sse")
        assertEquals(listOf("keep", "also"), tokens)
        assertTrue(tokens.isNotEmpty())
    }

    @Test
    fun bothFailuresReportFailed() {
        val mock = server { _ -> Triple(503, "down", 0L) }
        val result = SseClient.streamWithFallback(
            sseUrl = "http://127.0.0.1:${mock.port}/sse",
            pollUrl = "http://127.0.0.1:${mock.port}/poll",
        )
        assertEquals(AiResponseMode.FAILED, result.mode)
    }
}
