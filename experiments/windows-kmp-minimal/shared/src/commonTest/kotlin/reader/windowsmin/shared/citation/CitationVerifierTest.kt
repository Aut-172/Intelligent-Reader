package reader.windowsmin.shared.citation

import kotlin.test.Test
import kotlin.test.assertEquals

class CitationVerifierTest {

    private fun version(
        documentId: String = "doc-1",
        versionId: String = "v1",
        contentHash: String = "hash-a",
        parserVersion: String = "pdfbox-3.0.5",
    ) = DocumentVersionRef(documentId, versionId, contentHash, parserVersion)

    private fun snapshot(
        version: DocumentVersionRef = version(),
        blocks: List<BlockSnapshot> = listOf(
            BlockSnapshot("b1", "1", 0, "第一段：引用定位需要双锚点。"),
            BlockSnapshot("b2", "1", 1, "第二段：跨版本默认 stale。"),
        ),
    ) = DocumentSnapshot(version, blocks)

    private fun citation(
        version: DocumentVersionRef = version(),
        blockId: String = "b1",
        order: Int = 0,
        start: Int = 4,
        end: Int = 8,
        quote: String = "引用定位",
    ) = Citation(
        citationId = "c1",
        version = version,
        structural = StructuralAnchor(blockId, "1", order),
        character = CharacterAnchor(start, end),
        quote = quote,
    )

    @Test
    fun sameVersionAndAnchorsAreValid() {
        assertEquals(CitationStatus.VALID, CitationVerifier.verify(citation(), snapshot()))
    }

    @Test
    fun newVersionIsStaleEvenWhenTextIsIdentical() {
        val newVersion = version(versionId = "v2", contentHash = "hash-b")
        assertEquals(CitationStatus.STALE_VERSION, CitationVerifier.verify(citation(), snapshot(newVersion)))
    }

    @Test
    fun parserUpgradeMarksStale() {
        val upgraded = version(parserVersion = "pdfbox-3.1.0")
        assertEquals(CitationStatus.STALE_PARSER, CitationVerifier.verify(citation(), snapshot(upgraded)))
    }

    @Test
    fun deletedOrReplacedBlockFailsLocator() {
        val blocks = listOf(BlockSnapshot("b9", "1", 0, "重新解析后块 ID 改变。"))
        assertEquals(CitationStatus.LOCATOR_FAILED, CitationVerifier.verify(citation(), snapshot(blocks = blocks)))
    }

    @Test
    fun reorderedBlockFailsLocator() {
        val blocks = listOf(
            BlockSnapshot("b1", "1", 1, "第一段：引用定位需要双锚点。"),
            BlockSnapshot("b2", "1", 0, "第二段：跨版本默认 stale。"),
        )
        assertEquals(CitationStatus.LOCATOR_FAILED, CitationVerifier.verify(citation(), snapshot(blocks = blocks)))
    }

    @Test
    fun outOfRangeCharacterAnchorFailsLocator() {
        val updated = snapshot(
            blocks = listOf(BlockSnapshot("b1", "1", 0, "短文本")),
        )
        assertEquals(CitationStatus.LOCATOR_FAILED, CitationVerifier.verify(citation(), updated))
    }

    @Test
    fun changedTextAtSameBlockFailsQuoteCheck() {
        val blocks = listOf(
            BlockSnapshot("b1", "1", 0, "第一段：引用位置已经改写。"),
            BlockSnapshot("b2", "1", 1, "第二段：跨版本默认 stale。"),
        )
        assertEquals(CitationStatus.QUOTE_MISMATCH, CitationVerifier.verify(citation(), snapshot(blocks = blocks)))
    }

    @Test
    fun explicitRebindProducesValidCitationOnNewVersion() {
        val newVersion = version(versionId = "v2", contentHash = "hash-b")
        val text = "新版第二段：引用定位需要双锚点。"
        val current = snapshot(
            version = newVersion,
            blocks = listOf(BlockSnapshot("b7", "2", 0, text)),
        )
        val start = text.indexOf("引用定位")
        val rebound = CitationVerifier.rebindExplicitly(
            citation = citation(),
            current = current,
            blockId = "b7",
            start = start,
            end = start + 4,
            quote = "引用定位",
        )
        assertEquals("v2", rebound.version.versionId)
        assertEquals(CitationStatus.VALID, CitationVerifier.verify(rebound, current))
    }
}
