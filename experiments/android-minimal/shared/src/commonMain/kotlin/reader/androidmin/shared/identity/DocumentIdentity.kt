package reader.androidmin.shared.identity

/**
 * 跨端文档身份（D-16：内容指纹 + 规范化元数据，不上传全文完成匹配）。
 */
data class DocumentIdentity(
    val contentSha256: String,
    val fileNameNormalized: String,
    val sizeBytes: Long,
)

enum class MatchConfidence { EXACT, LIKELY_NEW_VERSION, DIFFERENT }

/** 平台抽象：SHA-256 十六进制；androidMain/jvmMain 分别用 MessageDigest 实现，避免污染 commonMain。 */
expect fun sha256Hex(bytes: ByteArray): String

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
