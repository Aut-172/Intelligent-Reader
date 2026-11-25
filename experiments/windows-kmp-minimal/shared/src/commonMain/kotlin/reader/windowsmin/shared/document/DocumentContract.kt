package reader.windowsmin.shared.document

/**
 * D-22 A unified document capability contract (platform-independent).
 * Windows and Android produce this contract from their own mature document components;
 * no component type may leak into this module.
 */

data class BBox(val x: Float, val y: Float, val width: Float, val height: Float)

data class TextRun(
    val page: Int,
    val charStart: Int,
    val charEnd: Int,
    val text: String,
    val bbox: BBox?,
)

data class PageInfo(val page: Int, val widthPt: Float, val heightPt: Float)

enum class Capability { RENDER, TEXT, BBOX, SELECTION, OCR }

data class Degradation(val code: String, val reason: String, val page: Int?)

data class DocumentContract(
    val documentId: String,
    val documentVersion: String,
    val sourceHash: String,
    val parserName: String,
    val parserVersion: String,
    val pages: List<PageInfo>,
    val runs: List<TextRun>,
    val capabilities: Set<Capability>,
    val degradations: List<Degradation>,
) {
    val charCount: Int get() = runs.sumOf { it.text.length }

    val bboxCoverage: Double
        get() = if (runs.isEmpty()) 0.0 else runs.count { it.bbox != null }.toDouble() / runs.size
}
