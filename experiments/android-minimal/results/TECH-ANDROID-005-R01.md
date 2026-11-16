# TECH-ANDROID-005-R01 · release 侧载/升级/卸载

experiment_id:  TECH-ANDROID-005-R01
date:           2026-11-16
sample_id:      无（debug APK 侧载；release 变体未单独构建）
source_sha256:  —
tool/version:   AGP 8.4.2（debug 变体；release 复用 debug 签名）
config_hash:    __
runtime:        Xiaomi 12S Pro (2206122SC) / Android 15 (API 35)
raw_output_path: app/build/outputs/apk/debug/app-debug.apk
metrics:        侧载安装=Success；versionName=0.1.0；versionCode=1；minSdk=31；targetSdk=34；primaryCpuAbi=null（纯 Kotlin 无原生库）；APK=20,137,440 字节；卸载=Success 且 pm list 无残留

failure_cases:  首轮 adb install 被 MIUI「USB 安装」拦截（INSTALL_FAILED_USER_RESTRICTED），开启后 Success。
interpretation(I):   debug APK 侧载安装成功；多次 `install -r` 覆盖安装后 sync_queue.json 与 SAF 持久授权 URI 均保留（覆盖不丢数据）；卸载后 `pm list packages` 无 androidmin 残留。
conclusion(C):       安装/启动/覆盖/卸载在真机可复现；版本/SDK/ABI/包体实测确认；「升级或覆盖」以覆盖安装 + 数据保留满足 D-08 验证要求。
open_risks(R):       release 变体（R8/签名正式化）未单独构建；versionCode 1→2 的严格「升级」未做；HyperOS 不同版本差异未测。
evidence_links:      adb install -r / uninstall；adb shell dumpsys package reader.androidmin.app；代码 app/.../ReleaseInfo.kt
