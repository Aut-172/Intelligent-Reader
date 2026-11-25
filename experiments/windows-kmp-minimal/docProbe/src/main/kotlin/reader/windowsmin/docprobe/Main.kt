package reader.windowsmin.docprobe

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import reader.windowsmin.shared.document.BBox
import reader.windowsmin.shared.document.Capability
import reader.windowsmin.shared.document.Degradation
import reader.windowsmin.shared.document.DocumentContract
import reader.windowsmin.shared.document.PageInfo
import reader.windowsmin.shared.document.TextRun
import java.io.File
import java.security.MessageDigest

/**
 * W-E02 document probe: uses Apache PDFBox 3 (a mature JVM component) to produce the
 * D-22 A unified document contract on fixed samples, and reports capability coverage.
 *
 * Usage: :docProbe:run --args="SELF-001=<path>=293 SELF-002=<path>=572 ..."
 * Each argument is SAMPLE_ID=PATH=PDFBOX_BASELINE_CHARS (PyMuPDF baseline char count).
 */

private data class Spec(val sampleId: String, val path: File, val baselineChars: Int)

/** Collects text runs with page, character range and bbox (contract fields). */
private class PositionalStripper : PDFTextStripper() {
    val runs = mutableListOf<TextRun>()
    private var offset = 0

    init {
        sortByPosition = true
    }

    override fun writeString(text: String, textPositions: List<TextPosition>) {
        if (textPositions.isEmpty()) return
        val x = textPositions.minOf { it.x }
        val y = textPositions.minOf { it.y }
        val right = textPositions.maxOf { it.x + it.width }
        val bottom = textPositions.maxOf { it.y + it.height }
        val value = textPositions.joinToString("") { it.unicode }
        runs.add(
            TextRun(
                page = currentPageNo,
                charStart = offset,
                charEnd = offset + value.length,
                text = value,
                bbox = BBox(x, y, right - x, bottom - y),
            ),
        )
        offset += value.length + 1
    }
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { b -> (b.toInt() and 0xff).toString(16).padStart(2, '0') }
}

private fun probe(spec: Spec): Pair<String, DocumentContract> {
    val started = System.nanoTime()
    val hash = sha256(spec.path)

    Loader.loadPDF(spec.path).use { doc ->
        val pages = (0 until doc.numberOfPages).map { i ->
            val box = doc.getPage(i).mediaBox
            PageInfo(page = i + 1, widthPt = box.width, heightPt = box.height)
        }

        val stripper = PositionalStripper()
        stripper.startPage = 1
        stripper.endPage = doc.numberOfPages
        stripper.getText(doc)
        val runs = stripper.runs

        val renderStarted = System.nanoTime()
        val image = PDFRenderer(doc).renderImageWithDPI(0, 72f)
        val renderMs = (System.nanoTime() - renderStarted) / 1_000_000

        val capabilities = mutableSetOf(Capability.RENDER)
        val degradations = mutableListOf<Degradation>()
        if (runs.isEmpty()) {
            degradations += Degradation(
                code = "NO_TEXT_LAYER",
                reason = "PDFBox 未提取到文本层，扫描件需 OCR 或原页回看",
                page = 1,
            )
        } else {
            capabilities += Capability.TEXT
            capabilities += Capability.BBOX
        }

        val contract = DocumentContract(
            documentId = spec.sampleId,
            documentVersion = "v1",
            sourceHash = hash,
            parserName = "Apache PDFBox",
            parserVersion = "3.0.5",
            pages = pages,
            runs = runs,
            capabilities = capabilities,
            degradations = degradations,
        )

        val totalMs = (System.nanoTime() - started) / 1_000_000
        val ratio = if (spec.baselineChars > 0) contract.charCount.toDouble() / spec.baselineChars else 0.0
        val row = listOf(
            spec.sampleId,
            pages.size.toString(),
            runs.size.toString(),
            contract.charCount.toString(),
            "%.3f".format(ratio),
            "%.3f".format(contract.bboxCoverage),
            image.width.toString(),
            image.height.toString(),
            renderMs.toString(),
            totalMs.toString(),
            hash.take(12),
            degradations.joinToString(";") { it.code },
        ).joinToString("\t")
        return row to contract
    }
}

fun main(args: Array<String>) {
    val specs = args.mapNotNull { arg ->
        val parts = arg.split("=")
        if (parts.size == 3) Spec(parts[0], File(parts[1]), parts[2].toInt()) else null
    }
    require(specs.isNotEmpty()) {
        "usage: :docProbe:run --args=\"SAMPLE_ID=PATH=BASELINE_CHARS ...\""
    }

    val resultsDir = File("results").apply { mkdirs() }
    val outFile = File(resultsDir, "we12-perf.tsv")
    val header = listOf(
        "run", "sample", "pages", "runs", "chars", "charRatioVsPyMuPDF", "bboxCoverage",
        "renderW", "renderH", "renderMs", "contractMs", "heapDeltaKB", "sha12", "degradations",
    ).joinToString("\t")

    fun usedHeapKb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024
    }

    outFile.printWriter(Charsets.UTF_8).use { writer ->
        writer.println(header)
        println(header)
        for (spec in specs) {
            // Warm-up run (JIT/class loading), not written to the result.
            probe(spec)
            repeat(3) { iteration ->
                val before = usedHeapKb()
                val (row, contract) = probe(spec)
                val heapDelta = usedHeapKb() - before
                val fullRow = "${iteration + 1}\t$row\t$heapDelta"
                writer.println(fullRow)
                println(fullRow)
                if (spec.sampleId == "SELF-001" && iteration == 0) {
                    println("# first runs of SELF-001 (page, charStart-charEnd, bbox, text):")
                    contract.runs.take(3).forEach { run ->
                        println("#   p${run.page} ${run.charStart}-${run.charEnd} ${run.bbox} ${run.text.take(40)}")
                    }
                }
            }
        }
    }
    println("W-E12 performance rows written to ${outFile.absolutePath}")
}
