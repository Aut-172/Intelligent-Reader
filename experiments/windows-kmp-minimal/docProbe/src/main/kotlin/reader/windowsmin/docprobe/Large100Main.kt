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

private class Counter : PDFTextStripper() {
    var runs = 0
    var chars = 0
    var valid = 0
    init { sortByPosition = true }
    override fun writeString(text: String, textPositions: List<TextPosition>) {
        if (textPositions.isEmpty()) return
        runs++
        chars += textPositions.size
        val w = textPositions.maxOf { it.x + it.width } - textPositions.minOf { it.x }
        val h = textPositions.maxOf { it.y + it.height } - textPositions.minOf { it.y }
        if (w > 0f && h > 0f) valid++
    }
}

fun main() {
    val dir = File("results").apply { mkdirs() }
    val pdf = File(dir, "large-100p.pdf")
    val fontFile = File("C:/Windows/Fonts/simhei.ttf")
    PDDocument().use { doc ->
        val font = PDType0Font.load(doc, fontFile)
        for (p in 1..100) {
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.setFont(font, 9f)
                cs.newLineAtOffset(40f, 800f)
                for (i in 1..20) {
                    cs.showText("第 $p 页 第 $i 行：100 页压力样本 Reader Studio W-E02")
                    cs.newLineAtOffset(0f, -16f)
                }
                cs.endText()
            }
        }
        doc.save(pdf)
    }

    val t0 = System.nanoTime()
    Loader.loadPDF(pdf).use { doc ->
        val counter = Counter().apply { startPage = 1; endPage = doc.numberOfPages }
        counter.getText(doc)
        val contractMs = (System.nanoTime() - t0) / 1_000_000
        val renderT0 = System.nanoTime()
        val img = PDFRenderer(doc).renderImageWithDPI(0, 72f)
        val renderMs = (System.nanoTime() - renderT0) / 1_000_000
        val coverage = if (counter.runs == 0) 0.0 else counter.valid.toDouble() / counter.runs
        val row = "LARGE-100P\t${doc.numberOfPages}\t${counter.runs}\t${counter.chars}\t" +
            "%.3f".format(coverage) + "\t${img.width}\t${img.height}\t$renderMs\t$contractMs\t${pdf.length()}"
        File(dir, "we02-large100.tsv").writeText(
            "sample\tpages\truns\tchars\tbboxCoverage\trenderW\trenderH\trenderMs\tcontractMs\tfileBytes\n" + row + "\n",
        )
        println(row)
    }
}
