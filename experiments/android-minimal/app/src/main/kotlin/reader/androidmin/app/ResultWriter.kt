package reader.androidmin.app

import android.content.Context
import org.json.JSONObject
import java.io.File

/** 把实验结果写为 JSON 到 filesDir/results/，供 adb pull 回填证据。 */
object ResultWriter {
    fun write(context: Context, experimentId: String, payload: Map<String, Any?>): File {
        val dir = File(context.filesDir, "results").apply { mkdirs() }
        val file = File(dir, "$experimentId.json")
        val json = JSONObject()
        payload.forEach { (k, v) -> json.put(k, v) }
        file.writeText(json.toString(2))
        return file
    }
}
