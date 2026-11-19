package reader.windowsmin.shared.sync

/** In-memory queue for tests and the smoke run. */
class InMemoryQueueStorage : QueueStorage {
    private val ops = LinkedHashMap<OperationId, SyncOperation>()

    override fun enqueue(op: SyncOperation) { ops[op.operationId] = op }
    override fun peekBatch(limit: Int): List<SyncOperation> = ops.values.take(limit)
    override fun remove(operationId: OperationId) { ops.remove(operationId) }
    override fun contains(operationId: OperationId): Boolean = ops.containsKey(operationId)
    override fun all(): List<SyncOperation> = ops.values.toList()
    override fun size(): Int = ops.size
}

/** In-memory cursor for tests and the smoke run. */
class InMemoryCursorStorage(initial: SyncCursor = SyncCursor(0)) : CursorStorage {
    private var cursor = initial

    override fun load(): SyncCursor = cursor
    override fun save(cursor: SyncCursor) { this.cursor = cursor }
}
