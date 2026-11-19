package reader.windowsmin.shared.sync

/**
 * Pure Kotlin sync-domain model shared by both clients (no platform dependency).
 * Ported from experiments/android-minimal/shared so the Windows target consumes
 * the same contract and test vectors.
 */

enum class SyncOpType { NOTE_UPSERT, PROGRESS_UPSERT, DELETE }

data class DeviceId(val value: String)

data class OperationId(val value: String)

data class ItemId(val value: String)

data class Revision(val value: Long)

data class SyncOperation(
    val operationId: OperationId,
    val deviceId: DeviceId,
    val itemId: ItemId,
    val type: SyncOpType,
    val baseRevision: Revision?,
    val payload: String,
)

data class SyncCursor(val value: Long)

data class RemoteChange(
    val seq: Long,
    val itemId: ItemId,
    val type: SyncOpType,
    val revision: Revision,
    val deviceId: DeviceId,
    val deleted: Boolean,
)

enum class UploadStatus { SUCCESS, TRANSIENT_FAILURE, REJECTED }

data class SyncResult(
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val transientFailed: Int = 0,
    val rejected: Int = 0,
    val cursorAdvancedTo: Long = 0,
    val conflictsResolved: Int = 0,
)

interface QueueStorage {
    fun enqueue(op: SyncOperation)
    fun peekBatch(limit: Int): List<SyncOperation>
    fun remove(operationId: OperationId)
    fun contains(operationId: OperationId): Boolean
    fun all(): List<SyncOperation>
    fun size(): Int
}

interface CursorStorage {
    fun load(): SyncCursor
    fun save(cursor: SyncCursor)
}
