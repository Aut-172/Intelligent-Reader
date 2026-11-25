package reader.windowsmin.docprobe

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.lang.management.ManagementFactory
import java.util.Locale

/**
 * W-E12 batch probe: 20 generated PDFs, measures first-readable, first-feedback,
 * full completion, CPU time and disk footprint.
 */
private fun makeDoc(path: File, font: PDType0Font? = null, pages: Int, linesPerPage: Int, a3: Boolean = false) {
    PDDocument().use { doc ->
        val size = if (a3) PDRectangle(1191f, 842f) else PDRectangle.A4
        val f = font ?: PDType0Font.load(doc, File("C:/Windows/Fonts/simhei.ttf"))
        for (p in 1..pages) {
            val page = PDPage(size)
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.setFont(f, 10f)
                cs.newLineAtOffset(40f, size.height - 50f)
                for (i in 1..linesPerPage) {
                    cs.showText("第 $p 页 第 $i 行：批量性能样本 Reader Studio W-E12 智能阅读助手")
                    cs.newLineAtOffset(0f, -14f)
                }
                cs.endText()
            }
        }
        doc.save(path)
    }
}

private fun ms(nanos: Long): Double = nanos / 1_000_000.0

private fun pct(values: List<Double>, p: Double): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val idx = ((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)
    return sorted[idx]
}

fun main() {
    val dir = File("results/perf20").apply { mkdirs() }
    val files = mutableListOf<File>()
    for (i in 1..20) {
        val pages = when {
            i % 10 == 0 -> 3
            i % 3 == 0 -> 2
            else -> 1
        }
        val lines = if (i % 7 == 0) 45 else 20
        val a3 = i % 9 == 0
        val f = File(dir, "perf-%02d.pdf".format(i))
        makeDoc(f, pages = pages, linesPerPage = lines, a3 = a3)
        files += f
    }

    val os = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    // Warm-up: JIT/class loading, not recorded.
    Loader.loadPDF(files[0]).use { doc ->
        PDFTextStripper().apply { startPage = 1; endPage = 1 }.getText(doc)
        PDFRenderer(doc).renderImageWithDPI(0, 72f)
    }
    val rows = mutableListOf<List<Any>>()
    for ((idx, file) in files.withIndex()) {
        val cpu0 = os.processCpuTime
        val t0 = System.nanoTime()
        Loader.loadPDF(file).use { doc ->
            val n = doc.numberOfPages
            val firstStripper = PDFTextStripper().apply { startPage = 1; endPage = 1 }
            val tFirst0 = System.nanoTime()
            val firstText = firstStripper.getText(doc)
            val firstReadable = ms(System.nanoTime() - tFirst0)

            val tRender0 = System.nanoTime()
            PDFRenderer(doc).renderImageWithDPI(0, 72f)
            val firstFeedback = ms(System.nanoTime() - tRender0)

            val fullStripper = PDFTextStripper().apply { startPage = 1; endPage = n }
            val tFull0 = System.nanoTime()
            val fullText = fullStripper.getText(doc)
            val complete = ms(System.nanoTime() - tFull0)

            val cpu = ms(os.processCpuTime - cpu0)
            rows.add(listOf(
                file.name, n, firstText.length, fullText.length,
                "%.1f".format(Locale.ROOT, firstReadable),
                "%.1f".format(Locale.ROOT, firstFeedback),
                "%.1f".format(Locale.ROOT, complete),
                "%.1f".format(Locale.ROOT, cpu),
                "%.1f".format(Locale.ROOT, file.length() / 1024.0),
            ))
        }
    }

    val tsv = File("results/we12-batch.tsv")
    tsv.printWriter(Charsets.UTF_8).use { w ->
        w.println("file\tpages\tfirstChars\tfullChars\tfirstReadableMs\tfirstFeedbackMs\tcompleteMs\tcpuMs\tsizeKB")
        rows.forEach { w.println(it.joinToString("\t")) }
    }

    fun col(i: Int): List<Double> = rows.map { it[i].toString().toDouble() }
    val firstR = col(4); val firstF = col(5); val complete = col(6); val cpu = col(7); val size = col(8)
    val summary = buildString {
        appendLine("docs=${rows.size} pages=${rows.sumOf { (it[1] as Int) }}")
        appendLine("firstReadable p50=${"%.1f".format(pct(firstR, 0.5))} p95=${"%.1f".format(pct(firstR, 0.95))} max=${"%.1f".format(firstR.max())}")
        appendLine("firstFeedback p50=${"%.1f".format(pct(firstF, 0.5))} p95=${"%.1f".format(pct(firstF, 0.95))} max=${"%.1f".format(firstF.max())}")
        appendLine("complete p50=${"%.1f".format(pct(complete, 0.5))} p95=${"%.1f".format(pct(complete, 0.95))} max=${"%.1f".format(complete.max())}")
        appendLine("cpuMs p50=${"%.1f".format(pct(cpu, 0.5))} p95=${"%.1f".format(pct(cpu, 0.95))}")
        appendLine("sizeKB total=${"%.1f".format(size.sum())} max=${"%.1f".format(size.max())}")
    }
    File("results/we12-batch-summary.txt").writeText(summary)
    print(summary)
    println("written ${tsv.absolutePath}")
}
