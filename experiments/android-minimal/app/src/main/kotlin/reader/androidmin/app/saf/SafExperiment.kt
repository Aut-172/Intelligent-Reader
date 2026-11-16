package reader.androidmin.app.saf

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 实验 1：SAF 文件授权最小实验。
 * 验证：取得持久读授权、重启后仍可读、授权失效(SecurityException)需重新选择重绑定。
 */
object SafExperiment {
    const val PREFS = "saf_experiment"
    const val KEY_URI = "persisted_uri"

    fun takePersistablePermission(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_URI, uri.toString()).apply()
            "已取得持久读授权并保存 URI：$uri"
        } catch (e: SecurityException) {
            "取得持久授权失败（提供方未开放 persistable）：${e.message}"
        }
    }

    fun verifyPersisted(context: Context): String {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URI, null) ?: return "未保存持久 URI，请先选择文件"
        val uri = Uri.parse(saved)
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.size ?: -1
            "持久授权有效，可读取 $bytes 字节"
        } catch (e: SecurityException) {
            "持久授权已失效（SecurityException），需重新选择并重新绑定：${e.message}"
        }
    }
}
