package reader.windowsmin.docprobe

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO

/**
 * W-E02 complex-sample probe: generates CJK, table, rotated and large-page PDFs,
 * then measures the same contract fields as the base docProbe.
 */
private data class Run(val page: Int, val text: String, val x: Float, val y: Float, val w: Float, val h: Float)

private class CollectingStripper : PDFTextStripper() {
    val runs = mutableListOf<Run>()
    init { sortByPosition = true }
    override fun writeString(text: String, textPositions: List<TextPosition>) {
        if (textPositions.isEmpty()) return
        val x = textPositions.minOf { it.x }
        val y = textPositions.minOf { it.y }
        val right = textPositions.maxOf { it.x + it.width }
        val bottom = textPositions.maxOf { it.y + it.height }
        runs.add(Run(currentPageNo, textPositions.joinToString("") { it.unicode }, x, y, right - x, bottom - y))
    }
}

private fun sha256(file: File): String {
    val d = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            d.update(buf, 0, n)
        }
    }
    return d.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}

private fun makeCjkPdf(path: File, fontFile: File, text: List<String>, pageSize: PDRectangle = PDRectangle.A4) {
    PDDocument().use { doc ->
        val page = PDPage(pageSize)
        doc.addPage(page)
        val font = PDType0Font.load(doc, fontFile)
        PDPageContentStream(doc, page).use { cs ->
            cs.beginText()
            cs.setFont(font, 18f)
            cs.newLineAtOffset(72f, pageSize.height - 90f)
            for (line in text) {
                cs.showText(line)
                cs.newLineAtOffset(0f, -28f)
            }
            cs.endText()
        }
        doc.save(path)
    }
}

private fun makeTablePdf(path: File, fontFile: File) {
    PDDocument().use { doc ->
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        val font = PDType0Font.load(doc, fontFile)
        val rows = listOf(
            listOf("编号", "名称", "状态", "备注"),
            listOf("1", "智能阅读助手", "启用", "文档解析"),
            listOf("2", "引用回跳", "启用", "双锚点"),
            listOf("3", "本地检索", "启用", "BM25"),
        )
        val x0 = 72f
        val y0 = 700f
        val w = 120f
        val h = 32f
        PDPageContentStream(doc, page).use { cs ->
            cs.setLineWidth(0.8f)
            for (r in rows.indices) {
                for (c in rows[r].indices) {
                    val x = x0 + c * w
                    val y = y0 - r * h
                    cs.addRect(x, y, w, h)
                }
            }
            cs.stroke()
            for (r in rows.indices) {
                for (c in rows[r].indices) {
                    val x = x0 + c * w + 6f
                    val y = y0 - r * h + 10f
                    cs.beginText()
                    cs.setFont(font, 11f)
                    cs.newLineAtOffset(x, y)
                    cs.showText(rows[r][c])
                    cs.endText()
                }
            }
        }
        doc.save(path)
    }
}

private fun makeRotatedPdf(path: File, fontFile: File) {
    PDDocument().use { doc ->
        val page = PDPage(PDRectangle.A4)
        page.rotation = 90
        doc.addPage(page)
        val font = PDType0Font.load(doc, fontFile)
        PDPageContentStream(doc, page).use { cs ->
            cs.beginText()
            cs.setFont(font, 16f)
            cs.newLineAtOffset(72f, 500f)
            cs.showText("旋转页面 90 度：智能阅读助手 W-E02")
            cs.endText()
        }
        doc.save(path)
    }
}

private fun makeLargePdf(path: File, fontFile: File) {
    val a3 = PDRectangle(1191f, 842f)
    PDDocument().use { doc ->
        val page = PDPage(a3)
        doc.addPage(page)
        val font = PDType0Font.load(doc, fontFile)
        PDPageContentStream(doc, page).use { cs ->
            cs.beginText()
            cs.setFont(font, 9f)
            cs.newLineAtOffset(48f, a3.height - 60f)
            for (i in 1..60) {
                cs.showText("第 $i 行：大页 A3 文本抽取与坐标覆盖测试 Reader Studio W-E02")
                cs.newLineAtOffset(0f, -12f)
            }
            cs.endText()
        }
        doc.save(path)
    }
}

private data class Result(
    val sample: String, val pages: Int, val runs: Int, val chars: Int,
    val bboxCoverage: Double, val pageW: Float, val pageH: Float, val rotation: Int,
    val renderW: Int, val renderH: Int, val renderMs: Long, val contractMs: Long,
    val sha12: String, val degradations: String,
)

private fun probe(sample: String, file: File): Result {
    val t0 = System.nanoTime()
    Loader.loadPDF(file).use { doc ->
        val stripper = CollectingStripper()
        stripper.startPage = 1
        stripper.endPage = doc.numberOfPages
        stripper.getText(doc)
        val runs = stripper.runs
        val chars = runs.sumOf { it.text.length }
        val valid = runs.count { it.w > 0f && it.h > 0f }
        val coverage = if (runs.isEmpty()) 0.0 else valid.toDouble() / runs.size
        val renderT0 = System.nanoTime()
        val img = PDFRenderer(doc).renderImageWithDPI(0, 72f)
        val renderMs = (System.nanoTime() - renderT0) / 1_000_000
        val box = doc.getPage(0).mediaBox
        val deg = mutableListOf<String>()
        if (runs.isEmpty()) deg += "NO_TEXT_LAYER"
        if (doc.getPage(0).rotation != 0) deg += "ROTATED_PAGE"
        return Result(
            sample, doc.numberOfPages, runs.size, chars, coverage,
            box.width, box.height, doc.getPage(0).rotation,
            img.width, img.height, renderMs, (System.nanoTime() - t0) / 1_000_000,
            sha256(file).take(12), deg.joinToString(";"),
        )
    }
}

fun main() {
    val outDir = File("results/complex").apply { mkdirs() }
    val simhei = File("C:/Windows/Fonts/simhei.ttf")
    val simkai = File("C:/Windows/Fonts/simkai.ttf")
    require(simhei.exists()) { "simhei.ttf not found" }

    val samples = mutableListOf<Pair<String, File>>()
    val cjk = File(outDir, "cjk-simhei.pdf")
    makeCjkPdf(cjk, simhei, listOf(
        "智能阅读助手 中文合同测试",
        "第一段：统一文档合同需要页面、文本、坐标与能力标记。",
        "第二段：引用回跳必须保留双锚点与版本信息。",
        "第三段：扫描页需要 OCR 或原页回看作为降级路径。",
    ))
    samples += "CJK-SIMHEI" to cjk

    if (simkai.exists()) {
        val kai = File(outDir, "cjk-simkai.pdf")
        makeCjkPdf(kai, simkai, listOf("楷体中文样本", "用于验证不同 CJK 字体的文本与坐标抽取。"))
        samples += "CJK-SIMKAI" to kai
    }

    val table = File(outDir, "table-4x4.pdf")
    makeTablePdf(table, simhei)
    samples += "TABLE-4X4" to table

    val rot = File(outDir, "rotated-90.pdf")
    makeRotatedPdf(rot, simhei)
    samples += "ROTATED-90" to rot

    val large = File(outDir, "large-a3.pdf")
    makeLargePdf(large, simhei)
    samples += "LARGE-A3" to large

    val rows = samples.map { (id, file) -> probe(id, file) }
    val tsv = File("results/we02-complex.tsv")
    tsv.printWriter(Charsets.UTF_8).use { w ->
        w.println("sample\tpages\truns\tchars\tbboxCoverage\tpageW\tpageH\trotation\trenderW\trenderH\trenderMs\tcontractMs\tsha12\tdegradations")
        for (r in rows) {
            w.println(listOf(
                r.sample, r.pages, r.runs, r.chars, "%.3f".format(r.bboxCoverage),
                "%.0f".format(r.pageW), "%.0f".format(r.pageH), r.rotation,
                r.renderW, r.renderH, r.renderMs, r.contractMs, r.sha12, r.degradations,
            ).joinToString("\t"))
        }
    }
    rows.forEach {
        println("${it.sample}\tpages=${it.pages}\truns=${it.runs}\tchars=${it.chars}\tbbox=${"%.3f".format(it.bboxCoverage)}\trotation=${it.rotation}\tdegradations=${it.degradations}")
    }
    println("written ${tsv.absolutePath}")
}
