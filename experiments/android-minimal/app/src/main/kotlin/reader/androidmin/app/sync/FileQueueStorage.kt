package reader.androidmin.app.sync

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import reader.androidmin.shared.sync.DeviceId
import reader.androidmin.shared.sync.ItemId
import reader.androidmin.shared.sync.OperationId
import reader.androidmin.shared.sync.QueueStorage
import reader.androidmin.shared.sync.Revision
import reader.androidmin.shared.sync.SyncOpType
import reader.androidmin.shared.sync.SyncOperation
import java.io.File

/** 队列的文件持久化实现（Android 平台适配，落到应用沙箱 filesDir）。 */
class FileQueueStorage(context: Context) : QueueStorage {
    private val file = File(context.filesDir, "sync_queue.json")
    private val ops: LinkedHashMap<OperationId, SyncOperation> = load()

    private fun load(): LinkedHashMap<OperationId, SyncOperation> {
        val map = LinkedHashMap<OperationId, SyncOperation>()
        if (!file.exists()) return map
        val arr = JSONArray(file.readText())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val op = SyncOperation(
                operationId = OperationId(o.getString("operationId")),
                deviceId = DeviceId(o.getString("deviceId")),
                itemId = ItemId(o.getString("itemId")),
                type = SyncOpType.valueOf(o.getString("type")),
                baseRevision = if (o.has("baseRevision")) Revision(o.getLong("baseRevision")) else null,
                payload = o.getString("payload"),
            )
            map[op.operationId] = op
        }
        return map
    }

    private fun persist() {
        val arr = JSONArray()
        ops.values.forEach { op ->
            arr.put(
                JSONObject().apply {
                    put("operationId", op.operationId.value)
                    put("deviceId", op.deviceId.value)
                    put("itemId", op.itemId.value)
                    put("type", op.type.name)
                    op.baseRevision?.let { put("baseRevision", it.value) }
                    put("payload", op.payload)
                },
            )
        }
        file.writeText(arr.toString())
    }

    override fun enqueue(op: SyncOperation) { ops[op.operationId] = op; persist() }
    override fun peekBatch(limit: Int): List<SyncOperation> = ops.values.take(limit)
    override fun remove(operationId: OperationId) { ops.remove(operationId); persist() }
    override fun contains(operationId: OperationId): Boolean = ops.containsKey(operationId)
    override fun all(): List<SyncOperation> = ops.values.toList()
    override fun size(): Int = ops.size
}
