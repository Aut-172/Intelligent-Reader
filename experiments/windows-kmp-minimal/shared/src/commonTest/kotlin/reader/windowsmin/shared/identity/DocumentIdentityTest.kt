package reader.windowsmin.shared.identity

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentIdentityTest {

    @Test
    fun sameContentDifferentNameIsSameDocument() {
        val a = DocumentIdentityMatcher.of("report.pdf", "hello".encodeToByteArray())
        val b = DocumentIdentityMatcher.of("paper.pdf", "hello".encodeToByteArray())
        assertEquals(MatchConfidence.EXACT, DocumentIdentityMatcher.match(a, b))
    }

    @Test
    fun sameNameChangedContentIsLikelyNewVersion() {
        val a = DocumentIdentityMatcher.of("report.pdf", "v1".encodeToByteArray())
        val b = DocumentIdentityMatcher.of("report.pdf", "v2".encodeToByteArray())
        assertEquals(MatchConfidence.LIKELY_NEW_VERSION, DocumentIdentityMatcher.match(a, b))
    }

    @Test
    fun differentNameAndContentIsDifferentDocument() {
        val a = DocumentIdentityMatcher.of("a.pdf", "x".encodeToByteArray())
        val b = DocumentIdentityMatcher.of("b.pdf", "y".encodeToByteArray())
        assertEquals(MatchConfidence.DIFFERENT, DocumentIdentityMatcher.match(a, b))
    }
}
