package reader.windowsmin.app.ai

import java.net.HttpURLConnection
import java.net.URL

/**
 * W-E08: minimal SSE client with cancellation and polling fallback (D-27 A).
 * Uses only the JDK HTTP client; no external dependency.
 */

class SseException(val status: Int, message: String) : Exception(message)

enum class AiResponseMode { SSE, POLLING, FAILED }

data class AiCallResult(
    val mode: AiResponseMode,
    val tokens: List<String>,
    val finalText: String?,
)

object SseClient {

    fun stream(
        url: String,
        readTimeoutMs: Int = 2_000,
        onToken: (String) -> Boolean = { true },
    ): List<String> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "text/event-stream")
            connectTimeout = 2_000
            this.readTimeout = readTimeoutMs
        }
        try {
            val status = connection.responseCode
            if (status != 200) throw SseException(status, "SSE HTTP $status")
            val tokens = mutableListOf<String>()
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data.isNotEmpty() && data != "[DONE]") {
                            tokens += data
                            if (!onToken(data)) break
                        }
                    }
                    line = reader.readLine()
                }
            }
            return tokens
        } finally {
            connection.disconnect()
        }
    }

    fun poll(url: String, readTimeoutMs: Int = 2_000): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2_000
            this.readTimeout = readTimeoutMs
        }
        try {
            val status = connection.responseCode
            if (status != 200) throw SseException(status, "Poll HTTP $status")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).readText().trim()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Tries SSE first; on HTTP error, timeout or cancellation failure falls back to polling.
     * A user cancellation is reported as SSE with the tokens received so far (no fallback).
     */
    fun streamWithFallback(
        sseUrl: String,
        pollUrl: String,
        readTimeoutMs: Int = 2_000,
        onToken: (String) -> Boolean = { true },
    ): AiCallResult {
        return try {
            AiCallResult(AiResponseMode.SSE, stream(sseUrl, readTimeoutMs, onToken), null)
        } catch (timeoutOrHttp: Exception) {
            try {
                AiCallResult(AiResponseMode.POLLING, emptyList(), poll(pollUrl, readTimeoutMs))
            } catch (pollFailure: Exception) {
                AiCallResult(AiResponseMode.FAILED, emptyList(), null)
            }
        }
    }
}
