package reader.androidmin.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import reader.androidmin.app.pdf.PdfProbe

/** 实验 3：PDF 探针仪器测试（需真机/模拟器；SELF-001 为单页文本型 PDF 金标）。 */
@RunWith(AndroidJUnit4::class)
class PdfProbeInstrumentedTest {

    @Test
    fun extractSelf001_bundledSample_hasTextLayerAndAnchor() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val bytes = ctx.assets.open("probe/SELF-001-single-column.pdf").use { it.readBytes() }
        val result = PdfProbe.extract(bytes)

        // 金标：SELF-001 单页、文本层存在、10 个块（桌面 PyMuPDF 基线）
        assertEquals("页数应为 1", 1, result.pageCount)
        assertTrue("应抽取到文本块", result.blockCount > 0)
        assertTrue("应命中标题锚点", result.anchorHits["智能阅读器自制 PDF 样本"] == true)
        assertTrue("应命中脚注锚点", result.anchorHits["脚注"] == true)
        assertTrue(
            "bbox 应为有限非负",
            result.blocks.all { it.x.isFinite() && it.y.isFinite() && it.width >= 0 && it.height >= 0 },
        )
        // 校验打包样本与既有固定样本同源（SELF-001 的 SHA-256）
        assertEquals(
            "0391dd982a1c426c52ca865bde7c253baaa4071d7ca26317febe2391a5389c93",
            result.sha256,
        )
    }
}
