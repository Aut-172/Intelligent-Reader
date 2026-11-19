package reader.androidmin.shared.sync

/**
 * 同步引擎：离线队列排水 + 服务端变更拉取 + 游标推进。
 * 全部状态通过 [QueueStorage] / [CursorStorage] 持久化，引擎自身无内存态，
 * 因此"进程被杀后重建引擎"与"新建引擎继续"行为等价，满足恢复要求。
 */
class SyncEngine(
    private val queue: QueueStorage,
    private val cursorStorage: CursorStorage,
    private val upload: (SyncOperation) -> UploadStatus,
    private val download: (SyncCursor) -> List<RemoteChange>,
    private val conflictPolicy: ConflictPolicy = ConflictPolicy,
) {

    /** 入队；同一 operationId 幂等，重复入队被忽略。 */
    fun enqueue(op: SyncOperation) {
        if (queue.contains(op.operationId)) return
        queue.enqueue(op)
    }

    fun sync(): SyncResult {
        var uploaded = 0
        var transientFailed = 0
        var rejected = 0

        // 1. 先按游标增量拉取远程变更，并在本地队列尚未排空时检测
        //    「远程变更 vs 本地待上传」的同 item 冲突。若先上传，本地项会被移除，
        //    冲突计数将永远为 0，故冲突检测必须放在上传之前。
        val remote = download(cursorStorage.load()).sortedBy { it.seq }
        var downloaded = 0
        var conflicts = 0
        var maxSeq = cursorStorage.load().value
        for (change in remote) {
            if (change.seq <= maxSeq) continue
            downloaded++
            val localPending = queue.all().firstOrNull { it.itemId == change.itemId }
            if (localPending != null && !change.deleted) {
                // 冲突检测：同一 item 本地仍有未上传修改。实际冲突副本生成由上层按
                // ConflictPolicy 决策；本最小实验只计数，策略语义见 ConflictPolicyTest。
                conflicts++
            }
            maxSeq = change.seq
        }
        if (maxSeq > cursorStorage.load().value) {
            cursorStorage.save(SyncCursor(maxSeq))
        }

        // 2. 上传本地待上传队列：瞬时失败留队下次重试，被拒则移除（不阻塞后续项）。
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
