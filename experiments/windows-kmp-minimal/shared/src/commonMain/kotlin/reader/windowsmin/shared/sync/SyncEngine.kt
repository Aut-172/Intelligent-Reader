package reader.windowsmin.shared.sync

/**
 * Sync engine: drain the offline queue, pull remote changes by cursor, advance the cursor.
 * All state lives in [QueueStorage] / [CursorStorage], so rebuilding the engine after a
 * process kill is equivalent to continuing with a new instance.
 */
class SyncEngine(
    private val queue: QueueStorage,
    private val cursorStorage: CursorStorage,
    private val upload: (SyncOperation) -> UploadStatus,
    private val download: (SyncCursor) -> List<RemoteChange>,
    private val conflictPolicy: ConflictPolicy = ConflictPolicy,
) {

    fun enqueue(op: SyncOperation) {
        if (queue.contains(op.operationId)) return
        queue.enqueue(op)
    }

    fun sync(): SyncResult {
        var uploaded = 0
        var transientFailed = 0
        var rejected = 0

        // Pull remote changes first so a pending local item can be counted as a conflict.
        val remote = download(cursorStorage.load()).sortedBy { it.seq }
        var downloaded = 0
        var conflicts = 0
        var maxSeq = cursorStorage.load().value
        for (change in remote) {
            if (change.seq <= maxSeq) continue
            downloaded++
            val localPending = queue.all().firstOrNull { it.itemId == change.itemId }
            if (localPending != null && !change.deleted) {
                conflicts++
            }
            maxSeq = change.seq
        }
        if (maxSeq > cursorStorage.load().value) {
            cursorStorage.save(SyncCursor(maxSeq))
        }

        for (op in queue.peekBatch(Int.MAX_VALUE)) {
            when (upload(op)) {
                UploadStatus.SUCCESS -> { queue.remove(op.operationId); uploaded++ }
                UploadStatus.TRANSIENT_FAILURE -> transientFailed++
                UploadStatus.REJECTED -> { queue.remove(op.operationId); rejected++ }
            }
        }

        return SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            transientFailed = transientFailed,
            rejected = rejected,
            cursorAdvancedTo = maxSeq,
            conflictsResolved = conflicts,
        )
    }
}
