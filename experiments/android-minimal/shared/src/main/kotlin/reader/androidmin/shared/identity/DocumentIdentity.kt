package reader.androidmin.shared.identity

import java.security.MessageDigest

/**
 * 跨端文档身份（D-16 候选方向：内容指纹 + 规范化元数据，不上传全文完成匹配）。
 */
data class DocumentIdentity(
    val contentSha256: String,
    val fileNameNormalized: String,
    val sizeBytes: Long,
)

enum class MatchConfidence { EXACT, LIKELY_NEW_VERSION, DIFFERENT }

object DocumentIdentityMatcher {

    fun of(fileName: String, content: ByteArray): DocumentIdentity {
        val digest = MessageDigest.getInstance("SHA-256").digest(content)
        return DocumentIdentity(
            contentSha256 = digest.joinToString("") { b -> (b.toInt() and 0xff).toString(16).padStart(2, '0') },
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
