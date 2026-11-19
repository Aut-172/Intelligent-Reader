package reader.windowsmin.shared.citation

/**
 * D-25 A citation contract: structural anchor + character anchor bound to a DocumentVersion.
 * Cross-version citations are STALE by default; migration is explicit and never automatic.
 */

data class DocumentVersionRef(
    val documentId: String,
    val versionId: String,
    val contentHash: String,
    val parserVersion: String,
)

data class StructuralAnchor(
    val blockId: String,
    val sectionPath: String,
    val orderInSection: Int,
)

data class CharacterAnchor(val start: Int, val end: Int)

data class Citation(
    val citationId: String,
    val version: DocumentVersionRef,
    val structural: StructuralAnchor,
    val character: CharacterAnchor,
    val quote: String,
)

data class BlockSnapshot(
    val blockId: String,
    val sectionPath: String,
    val orderInSection: Int,
    val text: String,
)

data class DocumentSnapshot(
    val version: DocumentVersionRef,
    val blocks: List<BlockSnapshot>,
)

enum class CitationStatus {
    VALID,
    STALE_VERSION,
    STALE_PARSER,
    LOCATOR_FAILED,
    QUOTE_MISMATCH,
}

object CitationVerifier {

    fun verify(citation: Citation, current: DocumentSnapshot): CitationStatus {
        val cited = citation.version
        val now = current.version

        if (cited.documentId != now.documentId) return CitationStatus.LOCATOR_FAILED
        if (cited.versionId != now.versionId || cited.contentHash != now.contentHash) {
            return CitationStatus.STALE_VERSION
        }
        if (cited.parserVersion != now.parserVersion) return CitationStatus.STALE_PARSER

        val block = current.blocks.firstOrNull { it.blockId == citation.structural.blockId }
            ?: return CitationStatus.LOCATOR_FAILED
        if (block.sectionPath != citation.structural.sectionPath) return CitationStatus.LOCATOR_FAILED
        if (block.orderInSection != citation.structural.orderInSection) return CitationStatus.LOCATOR_FAILED

        val start = citation.character.start
        val end = citation.character.end
        if (start < 0 || end > block.text.length || start >= end) return CitationStatus.LOCATOR_FAILED

        val actual = normalize(block.text.substring(start, end))
        if (actual != normalize(citation.quote)) return CitationStatus.QUOTE_MISMATCH
        return CitationStatus.VALID
    }

    /**
     * Explicit rebind only: the caller must supply the new anchors (typically after the user
     * confirms a migrated location). Verification never calls this automatically.
     */
    fun rebindExplicitly(
        citation: Citation,
        current: DocumentSnapshot,
        blockId: String,
        start: Int,
        end: Int,
        quote: String,
    ): Citation {
        val block = current.blocks.first { it.blockId == blockId }
        return citation.copy(
            version = current.version,
            structural = StructuralAnchor(block.blockId, block.sectionPath, block.orderInSection),
            character = CharacterAnchor(start, end),
            quote = quote,
        )
    }

    private fun normalize(text: String): String = text.replace(Regex("\\s+"), " ").trim()
}
