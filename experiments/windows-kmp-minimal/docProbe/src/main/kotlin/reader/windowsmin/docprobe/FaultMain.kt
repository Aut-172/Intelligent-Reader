package reader.windowsmin.docprobe

import org.apache.pdfbox.Loader
import java.io.File

/** W-E11 parse fault probe: corrupt/empty PDFs must fail in a catchable way. */
fun main() {
    val dir = File("results/faults").apply { mkdirs() }
    val corrupt = File(dir, "corrupt.pdf")
    corrupt.writeBytes("%PDF-1.4\nthis is not a valid pdf\n%%EOF\n".toByteArray())
    val empty = File(dir, "empty.pdf")
    empty.writeBytes(ByteArray(0))

    val results = mutableListOf<String>()
    for (f in listOf(corrupt, empty)) {
        try {
            Loader.loadPDF(f).use { doc -> results += "${f.name}\tOK\tpages=${doc.numberOfPages}" }
        } catch (e: Exception) {
            results += "${f.name}\tCAUGHT\t${e.javaClass.simpleName}\t${e.message?.take(80)}"
        }
    }
    val out = File("results/we11-faults.tsv")
    out.writeText("file\tresult\tdetail\n" + results.joinToString("\n") + "\n")
    results.forEach { println(it) }
    println("written ${out.absolutePath}")
}
