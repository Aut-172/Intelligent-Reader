package reader.androidmin.app.sync

import android.content.Context
import reader.androidmin.shared.sync.CursorStorage
import reader.androidmin.shared.sync.SyncCursor
import java.io.File

/** 游标的文件持久化实现。 */
class FileCursorStorage(context: Context) : CursorStorage {
    private val file = File(context.filesDir, "sync_cursor.txt")

    override fun load(): SyncCursor =
        SyncCursor(if (file.exists()) file.readText().trim().toLongOrNull() ?: 0L else 0L)

    override fun save(cursor: SyncCursor) {
        file.writeText(cursor.value.toString())
    }
}
