package reader.androidmin.app.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.security.MessageDigest

/**
 * 实验 3：PDF 文档组件探针（使用 pdfbox-android）。
 * 抽取文本块（文本 + bbox + 阅读顺序），用于与桌面端 PyMuPDF/其他组件金标比对。
 * 注意：这是组件探针，不是产品解析器选型结论。
 */
data class PdfTextBlock(
    val page: Int,
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val order: Int,
)

data class PdfProbeResult(
    val pageCount: Int,
    val blockCount: Int,
    val blocks: List<PdfTextBlock>,
    val anchorHits: Map<String, Boolean>,
    val readingOrderOk: Boolean,
    val sha256: String,
)

object PdfProbe {

    fun extract(
        pdfBytes: ByteArray,
        anchors: List<String> = listOf("智能阅读器自制 PDF 样本", "研究目标", "脚注"),
    ): PdfProbeResult {
        PDDocument.load(pdfBytes).use { doc ->
            val blocks = mutableListOf<PdfTextBlock>()
            var order = 0

            val stripper = object : PDFTextStripper() {
                override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                    if (text.isNotBlank() && textPositions.isNotEmpty()) {
                        val x = textPositions.minOf { it.xDirAdj }
                        val y = textPositions.minOf { it.yDirAdj }
                        val maxX = textPositions.maxOf { it.xDirAdj + it.widthDirAdj }
                        val maxY = textPositions.maxOf { it.yDirAdj + it.heightDir }
                        blocks.add(
                            PdfTextBlock(
                                page = getCurrentPageNo(),
                                text = text.trim(),
                                x = x,
                                y = y,
                                width = maxX - x,
                                height = maxY - y,
                                order = order++,
                            ),
                        )
                    }
                    // 必须回写 base 类，否则 getText() 返回空串，anchorHits 恒为 false。
                    super.writeString(text, textPositions)
                }
            }
            stripper.sortByPosition = true
            val fullText = stripper.getText(doc)

            val anchorHits = anchors.associateWith { fullText.contains(it) }
            val readingOrderOk = checkReadingOrder(blocks)

            return PdfProbeResult(
                pageCount = doc.numberOfPages,
                blockCount = blocks.size,
                blocks = blocks.toList(),
                anchorHits = anchorHits,
                readingOrderOk = readingOrderOk,
                sha256 = sha256(pdfBytes),
            )
        }
    }

    /** 粗粒度阅读顺序检查：同页内（按抽取顺序）相邻块的 y 不应大幅回跳。 */
    private fun checkReadingOrder(blocks: List<PdfTextBlock>): Boolean {
        if (blocks.size < 2) return true
        val byPage = blocks.groupBy { it.page }
        var okPairs = 0
        var totalPairs = 0
        for ((_, pageBlocks) in byPage) {
            val sorted = pageBlocks.sortedBy { it.order }
            for (i in 0 until sorted.size - 1) {
                totalPairs++
                // getYDirAdj() 以左上为原点（y 向下递增）；阅读顺序自上而下 => y 应单调不减。允许小幅容差。
                if (sorted[i].y <= sorted[i + 1].y + 2f) okPairs++
            }
        }
        return totalPairs == 0 || okPairs.toDouble() / totalPairs >= 0.7
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { b -> (b.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
}
