package reader.windowsmin.docprobe

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import javax.imageio.ImageIO

/**
 * W-E03 integration probe: generate a text PDF, render page 0 to PNG, and
 * print paths so the Windows OCR probe can consume the image.
 */
fun main(args: Array<String>) {
    val dir = File(if (args.isNotEmpty()) args[0] else ".")
    dir.mkdirs()
    val pdf = File(dir, "we03-render.pdf")
    val png = File(dir, "we03-render.png")

    PDDocument().use { doc ->
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        PDPageContentStream(doc, page).use { cs ->
            cs.beginText()
            cs.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 24f)
            cs.newLineAtOffset(72f, 700f)
            for (line in listOf("Reader Studio W-E03 2026", "PDF render to OCR pipeline", "12345 ABCDEF")) {
                cs.showText(line)
                cs.newLineAtOffset(0f, -40f)
            }
            cs.endText()
        }
        doc.save(pdf)
    }

    val t0 = System.nanoTime()
    Loader.loadPDF(pdf).use { doc ->
        val image = PDFRenderer(doc).renderImageWithDPI(0, 200f)
        ImageIO.write(image, "png", png)
        val ms = (System.nanoTime() - t0) / 1_000_000.0
        println("OK pdf=${pdf.absolutePath} png=${png.absolutePath} width=${image.width} height=${image.height} render_ms=${"%.1f".format(ms)}")
    }
}
