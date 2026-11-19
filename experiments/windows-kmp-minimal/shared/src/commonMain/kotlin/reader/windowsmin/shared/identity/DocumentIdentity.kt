package reader.windowsmin.shared.identity

/**
 * expect/actual boundary: the shared contract lives in commonMain, while the JVM
 * (Windows target) provides the hashing implementation. This is the port-isolation
 * evidence required by D-28 A: no platform type leaks into the shared module.
 */
expect fun sha256Hex(bytes: ByteArray): String

data class DocumentIdentity(
    val contentSha256: String,
    val fileNameNormalized: String,
    val sizeBytes: Long,
)

enum class MatchConfidence { EXACT, LIKELY_NEW_VERSION, DIFFERENT }

object DocumentIdentityMatcher {

    fun of(fileName: String, content: ByteArray): DocumentIdentity {
        return DocumentIdentity(
            contentSha256 = sha256Hex(content),
            fileNameNormalized = normalize(fileName),
            sizeBytes = content.size.toLong(),
        )
    }

    fun match(local: DocumentIdentity, remote: DocumentIdentity): MatchConfidence {
        if (local.contentSha256 == remote.contentSha256) return MatchConfidence.EXACT
        if (local.fileNameNormalized == remote.fileNameNormalized) return MatchConfidence.LIKELY_NEW_VERSION
        return MatchConfidence.DIFFERENT
    }

    private fun normalize(name: String): String = name.trim().lowercase()
}
