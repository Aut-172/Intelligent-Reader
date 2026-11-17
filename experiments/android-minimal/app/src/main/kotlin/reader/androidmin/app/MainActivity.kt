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
import reader.androidmin.shared.identity.MatchConfidence
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
        root.addView(btn("3 PDF 探针（SELF-001 单栏）") { runPdfSample("probe/SELF-001-single-column.pdf", listOf("智能阅读器自制 PDF 样本", "研究目标", "脚注"), "TECH-ANDROID-003-R01", "SELF-001") })
        root.addView(btn("3b PDF 探针（SELF-002 双栏）") { runPdfSample("probe/SELF-002-two-column.pdf", listOf("跨栏标题", "参考文献", "阅读顺序"), "A-E02-SELF-002-R01", "SELF-002") })
        root.addView(btn("3c PDF 探针（SELF-003 表格公式）") { runPdfSample("probe/SELF-003-table-formula.pdf", listOf("Recall@k", "合并单元格", "公式"), "A-E02-SELF-003-R01", "SELF-003") })
        root.addView(btn("3d PDF 探针（SELF-013 旋转页）") { runPdfSample("probe/SELF-013-rotated.pdf", listOf("旋转页", "Rotated page anchor"), "A-E02-SELF-013-R01", "SELF-013") })
        root.addView(btn("3e PDF 探针（SELF-014 字体）") { runPdfSample("probe/SELF-014-fonts.pdf", listOf("字体专项", "Font anchor", "Bold anchor", "Serif anchor"), "A-E02-SELF-014-R01", "SELF-014") })
        root.addView(btn("3f PDF 探针（SELF-015 图片）") { runPdfSample("probe/SELF-015-images.pdf", listOf("图片样本", "嵌入图片"), "A-E02-SELF-015-R01", "SELF-015") })
        root.addView(btn("3g PDF 探针（SELF-016 大页）") { runPdfSample("probe/SELF-016-large.pdf", listOf("大页样本", "page 30 anchor"), "A-E02-SELF-016-R01", "SELF-016") })
        root.addView(btn("4 本地检索降级实验") { runSearchExperiment() })
        root.addView(btn("5 Release 信息") { ReleaseInfo.report(this) })
        root.addView(btn("6 性能基准（三类 PDF + 20 文档）") { runBenchmark() })
        root.addView(btn("7 Keystore 安全凭据") { runKeystoreProbe() })
        root.addView(btn("8 清除本地数据（撤销/退出）") { runClearData() })
        root.addView(btn("9 版本变化/stale 检测") { runVersionStaleProbe() })

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

    private fun runPdfSample(asset: String, anchors: List<String>, experimentId: String, label: String): String {
        val bytes = assets.open(asset).use { it.readBytes() }
        val r = PdfProbe.extract(bytes, anchors)
        val report = buildString {
            appendLine("[$label] pageCount=${r.pageCount}")
            appendLine("blockCount=${r.blockCount}")
            appendLine("anchorHits=${r.anchorHits}")
            appendLine("readingOrderOk=${r.readingOrderOk}")
            appendLine("sha256=${r.sha256}")
            appendLine("首块示例=${r.blocks.firstOrNull()?.let { "[p${it.page}] ${it.text.take(20)} x=${it.x} y=${it.y} w=${it.width} h=${it.height}" } ?: "无"}")
        }
        ResultWriter.write(this, experimentId, mapOf(
            "sample" to label,
            "pageCount" to r.pageCount,
            "blockCount" to r.blockCount,
            "anchorHits" to r.anchorHits,
            "readingOrderOk" to r.readingOrderOk,
            "sha256" to r.sha256,
        ))
        return report
    }

    private fun runBenchmark(): String {
        val out = mutableMapOf<String, Any?>()
        val samples = listOf(
            "SELF-001" to "probe/SELF-001-single-column.pdf",
            "SELF-002" to "probe/SELF-002-two-column.pdf",
            "SELF-003" to "probe/SELF-003-table-formula.pdf",
        )
        for ((label, asset) in samples) {
            val bytes = assets.open(asset).use { it.readBytes() }
            val times = (1..3).map {
                val t0 = System.nanoTime()
                PdfProbe.extract(bytes)
                (System.nanoTime() - t0) / 1_000_000
            }.sorted()
            out["$label.parseMsP50"] = times[1]
            out["$label.parseMsP95"] = times[2]
        }
        // 20 文档关键词索引（本地全文基线，不含嵌入）
        val mem0 = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
        val t0 = System.currentTimeMillis()
        val idx = KeywordIndex()
        for (i in 1..20) {
            idx.index(listOf(TextBlock("d$i-b1", 1, 0, "智能阅读器 第$i 份文档 统一中间表示 引用定位 离线检索 脚注")))
        }
        val t1 = System.currentTimeMillis()
        val mem1 = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
        out["docs20.indexMs"] = t1 - t0
        out["docs20.blocks"] = idx.size()
        out["docs20.memDeltaKB"] = (mem1 - mem0) / 1024
        ResultWriter.write(this, "A-E12-R01", out)
        return buildString {
            for ((label, _) in samples) {
                appendLine("$label 解析耗时(ms) p50=${out["$label.parseMsP50"]} p95=${out["$label.parseMsP95"]}")
            }
            appendLine("20文档索引=${out["docs20.indexMs"]}ms 块数=${out["docs20.blocks"]} 内存增量=${out["docs20.memDeltaKB"]}KB")
        }
    }

    private fun runKeystoreProbe(): String {
        val r = KeystoreProbe.run()
        ResultWriter.write(this, "A-E10-R01", r)
        return "Keystore=${r.entries.joinToString { "${it.key}=${it.value}" }}"
    }

    private fun runClearData(): String {
        deleteFile("sync_queue.json")
        deleteFile("sync_cursor.txt")
        getSharedPreferences("saf_experiment", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        try {
            val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias("probe-aes")) ks.deleteEntry("probe-aes")
        } catch (e: Exception) {
            // ignore
        }
        return "已清除：同步队列、游标、SAF 持久授权、Keystore 密钥（对应退出/撤销清理）"
    }

    private fun runVersionStaleProbe(): String {
        val base = assets.open("probe/SELF-001-single-column.pdf").use { it.readBytes() }
        val id1 = DocumentIdentityMatcher.of("SELF-001.pdf", base)
        val modified = base + byteArrayOf(10.toByte(), 10.toByte())
        val id2 = DocumentIdentityMatcher.of("SELF-001.pdf", modified)
        val verdict = DocumentIdentityMatcher.match(id1, id2)
        val stale = verdict != MatchConfidence.EXACT
        ResultWriter.write(this, "A-E06-R01", mapOf(
            "baseSha256" to id1.contentSha256,
            "modifiedSha256" to id2.contentSha256,
            "verdict" to verdict.name,
            "staleMarked" to stale,
        ))
        return "版本变化检测：base=${id1.contentSha256.take(16)}… 修改后=${id2.contentSha256.take(16)}…\n判定=${verdict.name} → ${if (stale) "标记 stale（不静默迁移）" else "未变化"}（字符锚点=bbox、结构锚点=page+order 见 PDF 探针）"
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
