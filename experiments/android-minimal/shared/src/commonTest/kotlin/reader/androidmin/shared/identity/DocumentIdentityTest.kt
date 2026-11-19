package reader.androidmin.shared.identity

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentIdentityTest {

    @Test
    fun `相同内容不同文件名 = 同一文档`() {
        val a = DocumentIdentityMatcher.of("报告.pdf", "hello".toByteArray())
        val b = DocumentIdentityMatcher.of("report.pdf", "hello".toByteArray())
        assertEquals(MatchConfidence.EXACT, DocumentIdentityMatcher.match(a, b))
    }

    @Test
    fun `同名但内容变化 = 可能新版本`() {
        val a = DocumentIdentityMatcher.of("报告.pdf", "v1".toByteArray())
        val b = DocumentIdentityMatcher.of("报告.pdf", "v2".toByteArray())
        assertEquals(MatchConfidence.LIKELY_NEW_VERSION, DocumentIdentityMatcher.match(a, b))
    }

    @Test
    fun `不同文件名且内容不同 = 不同文档`() {
        val a = DocumentIdentityMatcher.of("a.pdf", "x".toByteArray())
        val b = DocumentIdentityMatcher.of("b.pdf", "y".toByteArray())
        assertEquals(MatchConfidence.DIFFERENT, DocumentIdentityMatcher.match(a, b))
    }
}
