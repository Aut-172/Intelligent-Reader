package reader.windowsmin.shared.diag

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticsTest {

    @Test
    fun forbiddenAttributesAreDropped() {
        val event = DiagnosticRecorder.record(
            eventId = "e1",
            category = "parse",
            code = "PARSE_FAILED",
            severity = Severity.ERROR,
            timestampEpochMs = 1_000L,
            correlationId = "corr-1",
            durationMs = 120L,
            retryCount = 1,
            attributes = mapOf(
                "stage" to "extract",
                "errorCode" to "E_TIMEOUT",
                "body" to "secret document text",
                "filePath" to "C:\\Users\\a\\doc.pdf",
                "token" to "Bearer abc",
            ),
        )
        assertEquals(setOf("stage", "errorCode"), event.attributes.keys)
        assertFalse(event.attributes.values.any { it.contains("secret") })
    }

    @Test
    fun bundleTextIsContentFreeAndScannable() {
        val events = listOf(
            DiagnosticRecorder.record("e1", "parse", "PARSE_OK", Severity.INFO, 1_000L, "c1", 90L, 0, mapOf("stage" to "extract")),
            DiagnosticRecorder.record("e2", "sync", "SYNC_RETRY", Severity.WARN, 2_000L, "c2", 300L, 2, mapOf("taskType" to "note")),
        )
        val bundle = DiagnosticBundle(events, generatedAtEpochMs = 3_000L)
        val text = bundle.toText()
        assertTrue(text.contains("eventCount=2"))
        assertTrue(text.contains("corr=c1") || text.contains("|c1|"))
        assertTrue(DiagnosticScanner.findLeaks(text).isEmpty())
        assertEquals(2, bundle.preview().lines().size)
    }

    @Test
    fun scannerDetectsInjectedPathAndToken() {
        val leaked = "event=e1|parse|X|ERROR|1|c1|||stage=C:\\Users\\a\\doc.pdf token=Bearer abc"
        val leaks = DiagnosticScanner.findLeaks(leaked)
        assertTrue(leaks.isNotEmpty())
        assertTrue(leaks.any { it.contains("C:\\") })
        assertTrue(leaks.any { it.contains("Bearer", ignoreCase = true) })
    }

    @Test
    fun longAttributeValuesAreTruncated() {
        val event = DiagnosticRecorder.record(
            "e1", "ai", "MODEL_OK", Severity.INFO, 1L, "c1",
            attributes = mapOf("model" to "x".repeat(200)),
        )
        assertEquals(64, event.attributes.getValue("model").length)
    }

    @Test
    fun correlationIdSurvivesForTracing() {
        val event = DiagnosticRecorder.record("e1", "ai", "MODEL_TIMEOUT", Severity.ERROR, 1L, "corr-42")
        assertEquals("corr-42", event.correlationId)
    }
}
