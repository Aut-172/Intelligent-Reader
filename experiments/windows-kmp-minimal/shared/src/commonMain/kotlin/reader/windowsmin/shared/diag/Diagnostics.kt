package reader.windowsmin.shared.diag

/**
 * D-32 A diagnostics contract: content-free structured events plus a user-exported,
 * previewable bundle. No document text, notes, evidence, tokens or full local paths.
 */

enum class Severity { INFO, WARN, ERROR }

data class DiagnosticEvent(
    val eventId: String,
    val category: String,
    val code: String,
    val severity: Severity,
    val timestampEpochMs: Long,
    val correlationId: String,
    val durationMs: Long? = null,
    val retryCount: Int? = null,
    val attributes: Map<String, String> = emptyMap(),
)

object DiagnosticPolicy {

    /** Only these attribute keys may leave the client. */
    val allowedAttributeKeys: Set<String> = setOf(
        "stage", "taskType", "errorCode", "httpStatus", "provider", "model",
        "bytes", "count", "unit", "version", "cacheHit",
    )

    fun sanitize(attributes: Map<String, String>): Map<String, String> =
        attributes
            .filterKeys { it in allowedAttributeKeys }
            .mapValues { (_, value) -> if (value.length <= 64) value else value.take(64) }
}

data class DiagnosticBundle(
    val events: List<DiagnosticEvent>,
    val generatedAtEpochMs: Long,
    val schemaVersion: Int = 1,
) {
    fun preview(): String =
        events.joinToString("\n") { event ->
            "${event.eventId} ${event.category}/${event.code} ${event.severity} corr=${event.correlationId}"
        }

    fun toText(): String = buildString {
        appendLine("schemaVersion=$schemaVersion")
        appendLine("generatedAt=$generatedAtEpochMs")
        appendLine("eventCount=${events.size}")
        events.forEach { event ->
            val attrs = event.attributes.entries.joinToString(",") { "${it.key}=${it.value}" }
            appendLine(
                "event=${event.eventId}|${event.category}|${event.code}|${event.severity}|" +
                    "${event.timestampEpochMs}|${event.correlationId}|${event.durationMs ?: ""}|" +
                    "${event.retryCount ?: ""}|$attrs",
            )
        }
    }
}

object DiagnosticRecorder {
    fun record(
        eventId: String,
        category: String,
        code: String,
        severity: Severity,
        timestampEpochMs: Long,
        correlationId: String,
        durationMs: Long? = null,
        retryCount: Int? = null,
        attributes: Map<String, String> = emptyMap(),
    ): DiagnosticEvent = DiagnosticEvent(
        eventId = eventId,
        category = category,
        code = code,
        severity = severity,
        timestampEpochMs = timestampEpochMs,
        correlationId = correlationId,
        durationMs = durationMs,
        retryCount = retryCount,
        attributes = DiagnosticPolicy.sanitize(attributes),
    )
}

object DiagnosticScanner {

    private val forbiddenPatterns = listOf(
        Regex("""[A-Za-z]:\\[^\s]+"""),
        Regex("""\\\\[^\s]+"""),
        Regex("""(?i)bearer\s+\S+"""),
        Regex("""(?i)token\s*[=:]\s*\S+"""),
        Regex("""(?i)(body|evidence|note|filePath|query)\s*[=:]"""),
    )

    /** Returns the matched leak fragments; empty means the bundle is content-free. */
    fun findLeaks(text: String): List<String> =
        forbiddenPatterns.mapNotNull { pattern -> pattern.find(text)?.value }
}
