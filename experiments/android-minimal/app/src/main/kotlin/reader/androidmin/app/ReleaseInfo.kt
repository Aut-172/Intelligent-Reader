package reader.androidmin.app

import android.content.Context
import android.os.Build

/** 实验 5（release 验证）的静态信息报告：版本、SDK、ABI、包名。 */
object ReleaseInfo {
    fun report(context: Context): String {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode else pkg.versionCode.toLong()
        val abi = Build.SUPPORTED_ABIS.joinToString(",")
        return buildString {
            appendLine("package = ${context.packageName}")
            appendLine("versionName = ${pkg.versionName}")
            appendLine("versionCode = $versionCode")
            appendLine("minSdk = ${context.applicationInfo.minSdkVersion}")
            appendLine("targetSdk = ${context.applicationInfo.targetSdkVersion}")
            appendLine("ABI = $abi")
            appendLine("Android API = ${Build.VERSION.SDK_INT}")
        }
    }
}
