package reader.androidmin.shared.sync

/**
 * 同步领域的纯 Kotlin 数据模型（共享核心，无 Android 依赖）。
 * 与 Windows 端共享同一套语义；本模块只承载"与平台无关"的领域规则与状态机。
 */

/** 同步操作类型。 */
enum class SyncOpType { NOTE_UPSERT, PROGRESS_UPSERT, DELETE }

/** 设备标识（去标识化的自生成实例 ID，不使用硬件 IMEI）。 */
data class DeviceId(val value: String)

/** 幂等操作 ID。 */
data class OperationId(val value: String)

/** 业务主键：笔记 / 阅读进度条目。 */
data class ItemId(val value: String)

/** 条目版本号（单调递增）。 */
data class Revision(val value: Long)

/** 一次本地离线修改，进入同步队列。 */
data class SyncOperation(
    val operationId: OperationId,
    val deviceId: DeviceId,
    val itemId: ItemId,
    val type: SyncOpType,
    val baseRevision: Revision?, // 客户端所见的 base；null 表示新建
    val payload: String,
)

/** 同步游标：已消费到的服务端变更序列号。 */
data class SyncCursor(val value: Long)

/** 服务端下发的一条远程变更。 */
data class RemoteChange(
    val seq: Long,
    val itemId: ItemId,
    val type: SyncOpType,
    val revision: Revision,
    val deviceId: DeviceId,
    val deleted: Boolean,
)

/** 上传结果。 */
enum class UploadStatus { SUCCESS, TRANSIENT_FAILURE, REJECTED }

/** 一次同步的汇总结果。 */
data class SyncResult(
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val transientFailed: Int = 0,
    val rejected: Int = 0,
    val cursorAdvancedTo: Long = 0,
    val conflictsResolved: Int = 0,
)

/** 队列持久化抽象。共享核心只依赖接口，平台侧提供实现。 */
interface QueueStorage {
    fun enqueue(op: SyncOperation)
    fun peekBatch(limit: Int): List<SyncOperation>
    fun remove(operationId: OperationId)
    fun contains(operationId: OperationId): Boolean
    fun all(): List<SyncOperation>
    fun size(): Int
}

/** 游标持久化抽象。 */
interface CursorStorage {
    fun load(): SyncCursor
    fun save(cursor: SyncCursor)
}
