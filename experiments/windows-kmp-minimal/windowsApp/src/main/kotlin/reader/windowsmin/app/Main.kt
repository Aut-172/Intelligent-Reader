package reader.windowsmin.app

import reader.windowsmin.shared.citation.BlockSnapshot
import reader.windowsmin.shared.citation.CharacterAnchor
import reader.windowsmin.shared.citation.Citation
import reader.windowsmin.shared.citation.CitationStatus
import reader.windowsmin.shared.citation.CitationVerifier
import reader.windowsmin.shared.citation.DocumentSnapshot
import reader.windowsmin.shared.citation.DocumentVersionRef
import reader.windowsmin.shared.citation.StructuralAnchor
import reader.windowsmin.shared.diag.DiagnosticBundle
import reader.windowsmin.shared.diag.DiagnosticRecorder
import reader.windowsmin.shared.diag.DiagnosticScanner
import reader.windowsmin.shared.diag.Severity
import reader.windowsmin.shared.identity.DocumentIdentityMatcher
import reader.windowsmin.shared.identity.MatchConfidence
import reader.windowsmin.shared.retrieval.DegradationPolicy
import reader.windowsmin.shared.retrieval.KeywordIndex
import reader.windowsmin.shared.retrieval.ResponseMode
import reader.windowsmin.shared.retrieval.TextBlock
import reader.windowsmin.shared.sync.ConflictPolicy
import reader.windowsmin.shared.sync.ConflictResolution
import reader.windowsmin.shared.sync.DeviceId
import reader.windowsmin.shared.sync.InMemoryCursorStorage
import reader.windowsmin.shared.sync.InMemoryQueueStorage
import reader.windowsmin.shared.sync.ItemId
import reader.windowsmin.shared.sync.ItemKind
import reader.windowsmin.shared.sync.OperationId
import reader.windowsmin.shared.sync.Revision
import reader.windowsmin.shared.sync.SideChange
import reader.windowsmin.shared.sync.SyncEngine
import reader.windowsmin.shared.sync.SyncOpType
import reader.windowsmin.shared.sync.SyncOperation
import reader.windowsmin.shared.sync.UploadStatus

/**
 * W-E01 smoke run: the Windows JVM target consumes the shared domain, sync contract and
 * retrieval contract, and executes the same fixed test vectors used by the Android target.
 */
private var passed = 0
private var failed = 0

private fun check(name: String, condition: Boolean) {
    if (condition) {
        passed++
        println("PASS  $name")
    } else {
        failed++
        println("FAIL  $name")
    }
}

fun main() {
    println("W-E01 smoke run on Windows JVM target")

    // 1) Document identity: same content, different name -> EXACT (D-16 A).
    val a = DocumentIdentityMatcher.of("report.pdf", "hello".encodeToByteArray())
    val b = DocumentIdentityMatcher.of("paper.pdf", "hello".encodeToByteArray())
    check("identity/same-content-different-name", DocumentIdentityMatcher.match(a, b) == MatchConfidence.EXACT)

    val c = DocumentIdentityMatcher.of("report.pdf", "changed".encodeToByteArray())
    check("identity/same-name-changed-content", DocumentIdentityMatcher.match(a, c) == MatchConfidence.LIKELY_NEW_VERSION)

    // 2) Retrieval: keyword hit keeps reading order.
    val index = KeywordIndex()
    index.index(
        listOf(
            TextBlock("b1", 1, 0, "智能阅读器 研究目标"),
            TextBlock("b2", 1, 1, "关键结论 与 引用定位"),
            TextBlock("b3", 1, 2, "脚注 与 页眉页脚"),
        ),
    )
    val hits = index.search("引用")
    check("retrieval/hit-count", hits.size == 1)
    check("retrieval/hit-block-id", hits.firstOrNull()?.blockId == "b2")

    // 3) Degradation: offline keeps local retrieval.
    check("degradation/local-search-only", DegradationPolicy.decide(false, true) == ResponseMode.LOCAL_SEARCH_ONLY)
    check("degradation/local-hit", DegradationPolicy.localSearchOnly("引用", index).size == 1)

    // 4) Sync queue: enqueue, upload, cursor behavior.
    val queue = InMemoryQueueStorage()
    val cursor = InMemoryCursorStorage()
    var uploadCalls = 0
    val engine = SyncEngine(
        queue,
        cursor,
        upload = { uploadCalls++; UploadStatus.SUCCESS },
        download = { emptyList() },
    )
    engine.enqueue(
        SyncOperation(
            operationId = OperationId("op-1"),
            deviceId = DeviceId("win-device"),
            itemId = ItemId("note-1"),
            type = SyncOpType.NOTE_UPSERT,
            baseRevision = null,
            payload = "offline note",
        ),
    )
    engine.enqueue(
        SyncOperation(
            operationId = OperationId("op-1"),
            deviceId = DeviceId("win-device"),
            itemId = ItemId("note-1"),
            type = SyncOpType.NOTE_UPSERT,
            baseRevision = null,
            payload = "offline note",
        ),
    )
    val result = engine.sync()
    check("sync/idempotent-enqueue", uploadCalls == 1)
    check("sync/uploaded-one", result.uploaded == 1)
    check("sync/queue-drained", queue.size() == 0)

    // 5) Conflict policy: notes keep a conflict copy.
    val resolution = ConflictPolicy.resolve(
        ItemKind.NOTE,
        SideChange.Upsert(Revision(2)),
        SideChange.Upsert(Revision(3)),
    )
    check("conflict/note-keeps-copy", resolution == ConflictResolution.KeepLocal(true))

    // 6) Citation anchors and cross-version staleness (D-25 A).
    val v1 = DocumentVersionRef("doc-1", "v1", "hash-a", "pdfbox-3.0.5")
    val cited = Citation(
        citationId = "c1",
        version = v1,
        structural = StructuralAnchor("b1", "1", 0),
        character = CharacterAnchor(4, 8),
        quote = "引用定位",
    )
    val sameVersion = DocumentSnapshot(
        v1,
        listOf(BlockSnapshot("b1", "1", 0, "第一段：引用定位需要双锚点。")),
    )
    check("citation/valid-same-version", CitationVerifier.verify(cited, sameVersion) == CitationStatus.VALID)

    val v2 = v1.copy(versionId = "v2", contentHash = "hash-b")
    check(
        "citation/stale-on-new-version",
        CitationVerifier.verify(cited, sameVersion.copy(version = v2)) == CitationStatus.STALE_VERSION,
    )
    check(
        "citation/stale-on-parser-upgrade",
        CitationVerifier.verify(
            cited,
            sameVersion.copy(version = v1.copy(parserVersion = "pdfbox-3.1.0")),
        ) == CitationStatus.STALE_PARSER,
    )
    val reordered = DocumentSnapshot(
        v1,
        listOf(BlockSnapshot("b1", "1", 1, "第一段：引用定位需要双锚点。")),
    )
    check(
        "citation/locator-fails-on-reorder",
        CitationVerifier.verify(cited, reordered) == CitationStatus.LOCATOR_FAILED,
    )

    // 7) Diagnostics: content-free bundle and leak scan (D-32 A).
    val diagEvent = DiagnosticRecorder.record(
        eventId = "ev-1",
        category = "parse",
        code = "PARSE_FAILED",
        severity = Severity.ERROR,
        timestampEpochMs = 1_000L,
        correlationId = "corr-1",
        durationMs = 120L,
        retryCount = 1,
        attributes = mapOf(
            "stage" to "extract",
            "body" to "secret document text",
            "filePath" to "C:\\secret\\doc.pdf",
        ),
    )
    val bundle = DiagnosticBundle(listOf(diagEvent), generatedAtEpochMs = 2_000L)
    check("diag/forbidden-attrs-dropped", diagEvent.attributes.keys == setOf("stage"))
    check("diag/bundle-scanner-clean", DiagnosticScanner.findLeaks(bundle.toText()).isEmpty())
    check("diag/preview-one-line", bundle.preview().lines().size == 1)

    println("RESULT passed=$passed failed=$failed")
    if (failed > 0) {
        kotlin.system.exitProcess(1)
    }
}
