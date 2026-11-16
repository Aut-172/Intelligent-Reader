package reader.androidmin.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import reader.androidmin.app.sync.FileCursorStorage
import reader.androidmin.app.sync.FileQueueStorage
import reader.androidmin.shared.sync.DeviceId
import reader.androidmin.shared.sync.ItemId
import reader.androidmin.shared.sync.OperationId
import reader.androidmin.shared.sync.Revision
import reader.androidmin.shared.sync.SyncEngine
import reader.androidmin.shared.sync.SyncOpType
import reader.androidmin.shared.sync.SyncOperation
import reader.androidmin.shared.sync.UploadStatus

/** 实验 4：文件持久化 + 引擎重建（模拟进程被杀恢复），需真机/模拟器。 */
@RunWith(AndroidJUnit4::class)
class FileStorageInstrumentedTest {

    private fun op(id: String, item: String = "item-$id") = SyncOperation(
        OperationId(id), DeviceId("dev-1"), ItemId(item),
        SyncOpType.NOTE_UPSERT, Revision(0), "payload-$id",
    )

    @Test
    fun fileStorage_survivesEngineRecreation_noDupNoLoss() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.deleteFile("sync_queue.json")
        ctx.deleteFile("sync_cursor.txt")

        val uploadedIds = mutableListOf<String>()

        // 第一轮：入队 3 条，其中 fail 瞬时失败。
        val q1 = FileQueueStorage(ctx)
        val c1 = FileCursorStorage(ctx)
        val e1 = SyncEngine(
            q1, c1,
            upload = { o ->
                if (o.operationId.value == "fail") UploadStatus.TRANSIENT_FAILURE
                else { uploadedIds += o.operationId.value; UploadStatus.SUCCESS }
            },
            download = { emptyList() },
        )
        e1.enqueue(op("a"))
        e1.enqueue(op("fail"))
        e1.enqueue(op("b"))
        val r1 = e1.sync()
        assertEquals(2, r1.uploaded)
        assertEquals(1, q1.size())

        // 模拟进程被杀后重建：从文件重新载入队列与游标。
        val q2 = FileQueueStorage(ctx)
        val c2 = FileCursorStorage(ctx)
        val e2 = SyncEngine(
            q2, c2,
            upload = { o -> uploadedIds += o.operationId.value; UploadStatus.SUCCESS },
            download = { emptyList() },
        )
        val r2 = e2.sync()
        assertEquals(1, r2.uploaded)
        assertEquals(0, q2.size())
        assertEquals(3, uploadedIds.size)
        assertEquals(setOf("a", "b", "fail"), uploadedIds.toSet())
    }
}
