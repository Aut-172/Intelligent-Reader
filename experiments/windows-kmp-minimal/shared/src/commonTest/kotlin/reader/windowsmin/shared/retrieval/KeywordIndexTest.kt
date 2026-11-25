package reader.windowsmin.shared.retrieval

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeywordIndexTest {

    @Test
    fun keywordHitKeepsReadingOrder() {
        val idx = KeywordIndex()
        idx.index(
            listOf(
                TextBlock("b1", 1, 0, "智能阅读器 研究目标"),
                TextBlock("b2", 1, 1, "关键结论 与 引用定位"),
                TextBlock("b3", 1, 2, "脚注 与 页眉页脚"),
            ),
        )
        val hits = idx.search("引用")
        assertEquals(1, hits.size)
        assertEquals("b2", hits[0].blockId)
    }

    @Test
    fun multiKeywordHitsSortedByScore() {
        val idx = KeywordIndex()
        idx.index(
            listOf(
                TextBlock("b1", 1, 0, "引用定位 引用定位 引用定位"),
                TextBlock("b2", 1, 1, "引用定位"),
            ),
        )
        val hits = idx.search("引用定位")
        assertEquals(2, hits.size)
        assertEquals("b1", hits.first().blockId)
        assertEquals(3, hits.first().score)
        assertTrue(hits.first().score >= hits.last().score)
    }

    @Test
    fun onlineUsesGenerationAndNoIndexMeansNoAnswer() {
        assertEquals(ResponseMode.ONLINE_GENERATIVE, DegradationPolicy.decide(true, true))
        assertEquals(ResponseMode.NO_ANSWER, DegradationPolicy.decide(false, false))
    }

    @Test
    fun offlineDegradesToLocalSearch() {
        val idx = KeywordIndex()
        idx.index(listOf(TextBlock("b1", 1, 0, "离线阅读 与 本地检索")))
        val mode = DegradationPolicy.decide(onlineAvailable = false, localIndexReady = true)
        assertEquals(ResponseMode.LOCAL_SEARCH_ONLY, mode)
        assertEquals(1, DegradationPolicy.localSearchOnly("本地检索", idx).size)
    }
}
