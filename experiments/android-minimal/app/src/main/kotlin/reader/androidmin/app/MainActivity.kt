package reader.androidmin.app

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import reader.androidmin.app.pdf.PdfProbe
import reader.androidmin.app.saf.SafExperiment
import reader.androidmin.app.sync.FileCursorStorage
import reader.androidmin.app.sync.FileQueueStorage
import reader.androidmin.app.sync.SyncWorker
import reader.androidmin.shared.identity.DocumentIdentityMatcher
import reader.androidmin.shared.retrieval.KeywordIndex
import reader.androidmin.shared.retrieval.TextBlock
import reader.androidmin.shared.sync.DeviceId
import reader.androidmin.shared.sync.ItemId
import reader.androidmin.shared.sync.OperationId
import reader.androidmin.shared.sync.Revision
import reader.androidmin.shared.sync.SyncEngine
import reader.androidmin.shared.sync.SyncOpType
import reader.androidmin.shared.sync.SyncOperation
import reader.androidmin.shared.sync.UploadStatus

class MainActivity : ComponentActivity() {

    private lateinit var output: TextView

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                output.text = "未选择文件"
            } else {
                val report = SafExperiment.takePersistablePermission(this, uri)
                val verify = SafExperiment.verifyPersisted(this)
                val identity = DocumentIdentityMatcher.of(
                    uri.lastPathSegment ?: "unknown",
                    contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0),
                )
                output.text = "$report\n$verify\n文件名规范化=${identity.fileNameNormalized}\nsizeBytes=${identity.sizeBytes}\nsha256=${identity.contentSha256}"
                ResultWriter.write(this, "TECH-ANDROID-001-R01", mapOf(
                    "sample_uri" to uri.toString(),
                    "persistable_granted" to report.startsWith("已取得"),
                    "verify_after_restart" to verify,
                    "identity" to mapOf(
                        "file" to identity.fileNameNormalized,
                        "size" to identity.sizeBytes,
                        "sha256" to identity.contentSha256,
                    ),
                ))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        output = TextView(this).apply {
            setTextColor(Color.BLACK)
            textSize = 12f
            setPadding(24, 24, 24, 24)
        }
        val scroll = ScrollView(this).apply { addView(output) }

        root.addView(btn("1 SAF 授权实验：选择文件") { openDocument.launch(arrayOf("application/pdf", "text/markdown", "text/plain")); "已打开系统文件选择器" })
        root.addView(btn("1b SAF 验证持久授权（重启后）") { SafExperiment.verifyPersisted(this) })
        root.addView(btn("2 后台任务/同步实验（入队+触发 Worker）") { runSyncExperiment() })
        root.addView(btn("3 PDF 探针实验（SELF-001）") { runPdfProbe() })
        root.addView(btn("4 本地检索降级实验") { runSearchExperiment() })
        root.addView(btn("5 Release 信息") { ReleaseInfo.report(this) })

        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun btn(label: String, action: () -> String): Button =
        Button(this).apply {
            text = label
            setOnClickListener { output.text = action() }
        }

    private fun runSyncExperiment(): String {
        val queue = FileQueueStorage(this)
        val cursor = FileCursorStorage(this)
        val engine = SyncEngine(
            queue, cursor,
            upload = { if (it.operationId.value == "fail-1") UploadStatus.TRANSIENT_FAILURE else UploadStatus.SUCCESS },
            download = { emptyList() },
        )
        fun op(id: String, item: String) = SyncOperation(
            OperationId(id), DeviceId("dev-1"), ItemId(item),
            SyncOpType.NOTE_UPSERT, Revision(0), "payload-$id",
        )
        engine.enqueue(op("note-1", "n1"))
        engine.enqueue(op("fail-1", "n2"))
        engine.enqueue(op("note-2", "n3"))
        // 手动触发 Worker 验证 WorkManager 调度；也可点"杀掉进程后重开"验证恢复。
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
        val result = engine.sync()
        val report = "入队3条 → 同步 uploaded=${result.uploaded} transientFailed=${result.transientFailed} 剩余队列=${queue.size()}\n" +
            "（随后触发 SyncWorker 后台消费，重启应用后可再次观察队列变化）"
        ResultWriter.write(this, "TECH-ANDROID-002-R01", mapOf(
            "uploaded" to result.uploaded,
            "transientFailed" to result.transientFailed,
            "queueRemaining" to queue.size(),
            "cursor" to cursor.load().value,
        ))
        return report
    }

    private fun runPdfProbe(): String {
        val bytes = assets.open("probe/SELF-001-single-column.pdf").use { it.readBytes() }
        val r = PdfProbe.extract(bytes)
        val report = buildString {
            appendLine("pageCount=${r.pageCount}")
            appendLine("blockCount=${r.blockCount}")
            appendLine("anchorHits=${r.anchorHits}")
            appendLine("readingOrderOk=${r.readingOrderOk}")
            appendLine("sha256=${r.sha256}")
            appendLine("首块示例=${r.blocks.firstOrNull()?.let { "[p${it.page}] ${it.text.take(20)} x=${it.x} y=${it.y} w=${it.width} h=${it.height}" } ?: "无"}")
        }
        ResultWriter.write(this, "TECH-ANDROID-003-R01", mapOf(
            "pageCount" to r.pageCount,
            "blockCount" to r.blockCount,
            "anchorHits" to r.anchorHits,
            "readingOrderOk" to r.readingOrderOk,
            "sha256" to r.sha256,
        ))
        return report
    }

    private fun runSearchExperiment(): String {
        val idx = KeywordIndex()
        idx.index(
            listOf(
                TextBlock("b1", 1, 0, "智能阅读器 研究目标 与 离线阅读"),
                TextBlock("b2", 1, 1, "关键结论 引用定位 与 本地检索"),
                TextBlock("b3", 1, 2, "脚注 与 页眉页脚"),
            ),
        )
        val mode = reader.androidmin.shared.retrieval.DegradationPolicy.decide(
            onlineAvailable = false,
            localIndexReady = idx.size() > 0,
        )
        val hits = reader.androidmin.shared.retrieval.DegradationPolicy.localSearchOnly("引用定位", idx)
        val report = "断网模式=$mode\n命中=${hits.map { it.blockId + ":" + it.snippet }}"
        ResultWriter.write(this, "TECH-ANDROID-006-R01", mapOf(
            "mode" to mode.name,
            "hitCount" to hits.size,
            "hits" to hits.map { it.blockId },
        ))
        return report
    }
}
