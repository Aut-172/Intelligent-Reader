package reader.androidmin.shared.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncEngineTest {

    private fun op(id: String, item: String = "item-$id", type: SyncOpType = SyncOpType.NOTE_UPSERT) =
        SyncOperation(OperationId(id), DeviceId("dev-1"), ItemId(item), type, Revision(0), "payload-$id")

    @Test
    fun `离线修改入队并在同步时上传`() {
        val queue = InMemoryQueueStorage()
        val cursor = InMemoryCursorStorage()
        val uploadedIds = mutableListOf<String>()
        val engine = SyncEngine(
            queue, cursor,
            upload = { op -> uploadedIds += op.operationId.value; UploadStatus.SUCCESS },
            download = { emptyList() },
        )
        engine.enqueue(op("1"))
        engine.enqueue(op("2"))
        val r = engine.sync()
        assertEquals(2, r.uploaded)
        assertEquals(listOf("1", "2"), uploadedIds)
        assertEquals(0, queue.size())
    }

    @Test
    fun `幂等 - 同一操作不重复入队也不重复上传`() {
        val queue = InMemoryQueueStorage()
        val cursor = InMemoryCursorStorage()
        var uploadCount = 0
        val engine = SyncEngine(
            queue, cursor,
            upload = { uploadCount++; UploadStatus.SUCCESS },
            download = { emptyList() },
        )
        engine.enqueue(op("dup"))
        engine.enqueue(op("dup"))
        engine.sync()
        engine.sync()
        assertEquals(1, uploadCount)
        assertEquals(0, queue.size())
    }

    @Test
    fun `部分失败隔离 - 瞬时失败留在队列 其余成功`() {
        val queue = InMemoryQueueStorage()
        val cursor = InMemoryCursorStorage()
        val engine = SyncEngine(
            queue, cursor,
            upload = { o -> if (o.operationId.value == "fail") UploadStatus.TRANSIENT_FAILURE else UploadStatus.SUCCESS },
            download = { emptyList() },
        )
        engine.enqueue(op("ok1"))
        engine.enqueue(op("fail"))
        engine.enqueue(op("ok2"))
        val r = engine.sync()
        assertEquals(2, r.uploaded)
        assertEquals(1, r.transientFailed)
        assertEquals(1, queue.size())
        assertTrue(queue.contains(OperationId("fail")))
    }

    @Test
    fun `游标只在成功下载后推进 且重复同步幂等`() {
        val queue = InMemoryQueueStorage()
        val cursor = InMemoryCursorStorage(SyncCursor(0))
        val remote = listOf(
            RemoteChange(1, ItemId("n1"), SyncOpType.NOTE_UPSERT, Revision(1), DeviceId("dev-2"), false),
            RemoteChange(2, ItemId("n2"), SyncOpType.NOTE_UPSERT, Revision(1), DeviceId("dev-2"), false),
        )
        var served = 0
        val engine = SyncEngine(
            queue, cursor,
            upload = { UploadStatus.SUCCESS },
            download = { if (served == 0) { served++; remote } else emptyList() },
        )
        val r1 = engine.sync()
        assertEquals(2, r1.downloaded)
        assertEquals(2, cursor.load().value)
        val r2 = engine.sync()
        assertEquals(0, r2.downloaded)
        assertEquals(2, cursor.load().value)
    }

    @Test
    fun `进程重启恢复 - 用持久存储重建引擎 无重复无丢失`() {
        val queue = InMemoryQueueStorage()
        val cursor = InMemoryCursorStorage()
        val uploadedIds = mutableListOf<String>()

        val e1 = SyncEngine(
            queue, cursor,
            upload = { o -> if (o.operationId.value == "fail") UploadStatus.TRANSIENT_FAILURE else { uploadedIds += o.operationId.value; UploadStatus.SUCCESS } },
            download = { emptyList() },
        )
        e1.enqueue(op("a"))
        e1.enqueue(op("fail"))
        e1.enqueue(op("b"))
        val r1 = e1.sync()
        assertEquals(2, r1.uploaded)
        assertEquals(1, queue.size())

        // 模拟进程被杀后重启：复用同一个 queue/cursor 存储新建引擎。
        val e2 = SyncEngine(
            queue, cursor,
            upload = { o -> uploadedIds += o.operationId.value; UploadStatus.SUCCESS },
            download = { emptyList() },
        )
        val r2 = e2.sync()
        assertEquals(1, r2.uploaded)
        assertEquals(0, queue.size())
        assertEquals(3, uploadedIds.size)
        assertEquals(setOf("a", "b", "fail"), uploadedIds.toSet())
    }

    @Test
    fun `远程变更与本地待上传冲突被计数`() {
        val queue = InMemoryQueueStorage()
        val cursor = InMemoryCursorStorage()
        val engine = SyncEngine(
            queue, cursor,
            upload = { UploadStatus.SUCCESS },
            download = { listOf(RemoteChange(1, ItemId("item-x"), SyncOpType.NOTE_UPSERT, Revision(2), DeviceId("dev-2"), false)) },
        )
        engine.enqueue(op("local", item = "item-x"))
        val r = engine.sync()
        assertEquals(1, r.downloaded)
        assertEquals(1, r.conflictsResolved)
    }
}
