package reader.androidmin.app.sync

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import reader.androidmin.shared.sync.DeviceId
import reader.androidmin.shared.sync.ItemId
import reader.androidmin.shared.sync.RemoteChange
import reader.androidmin.shared.sync.Revision
import reader.androidmin.shared.sync.SyncEngine
import reader.androidmin.shared.sync.SyncOpType
import reader.androidmin.shared.sync.UploadStatus

/**
 * 实验 2：WorkManager 后台任务最小实验。
 * 服务端用模拟下载源；上传的 operationId 以 "fail" 开头时模拟瞬时失败。
 */
class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val queue = FileQueueStorage(applicationContext)
        val cursor = FileCursorStorage(applicationContext)
        val engine = SyncEngine(
            queue = queue,
            cursorStorage = cursor,
            upload = { op ->
                if (op.operationId.value.startsWith("fail")) UploadStatus.TRANSIENT_FAILURE
                else UploadStatus.SUCCESS
            },
            download = { c ->
                if (c.value < 3) {
                    listOf(
                        RemoteChange(1, ItemId("n1"), SyncOpType.NOTE_UPSERT, Revision(1), DeviceId("server"), false),
                        RemoteChange(2, ItemId("p1"), SyncOpType.PROGRESS_UPSERT, Revision(2), DeviceId("server"), false),
                        RemoteChange(3, ItemId("n2"), SyncOpType.NOTE_UPSERT, Revision(1), DeviceId("server"), false),
                    )
                } else {
                    emptyList()
                }
            },
        )
        val result = engine.sync()
        return Result.success(
            Data.Builder()
                .putInt("uploaded", result.uploaded)
                .putInt("downloaded", result.downloaded)
                .putInt("transientFailed", result.transientFailed)
                .putLong("cursor", result.cursorAdvancedTo)
                .build(),
        )
    }
}
