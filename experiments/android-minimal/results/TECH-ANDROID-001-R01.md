# TECH-ANDROID-001-R01 · SAF 文件授权/持久授权/重绑

experiment_id:  TECH-ANDROID-001-R01
date:           2026-11-16
sample_id:      /storage/emulated/0/Download/SELF-001.pdf
source_sha256:  0391dd982a1c426c52ca865bde7c253baaa4071d7ca26317febe2391a5389c93
tool/version:   平台 SAF（ACTION_OPEN_DOCUMENT + takePersistableUriPermission）
config_hash:    __
runtime:        Xiaomi 12S Pro (2206122SC) / Android 15 (API 35)
raw_output_path: files/results/TECH-ANDROID-001-R01.json
metrics:        persistable_granted=true; verify_after_restart=持久授权有效可读取 59367 字节; identity(sha256/size) 与金标一致

failure_cases:  无（首轮 adb install 被 MIUI「USB 安装」拦截，属安装侧限制非 SAF 问题）
interpretation(I):   SAF 打开 Download/SELF-001.pdf，takePersistableUriPermission(READ) 成功；force-stop 重启后 1B 仍返回「持久授权有效，可读取 59367 字节」。
conclusion(C):       Android 端持久授权跨进程重启有效；DocumentIdentity（sha256+mime+size+规范化文件名）可稳定取得。
open_risks(R):       云盘 Provider（如网盘 DocumentsProvider）的 persistable/失效行为未测；提供方撤权/删文件的 SecurityException 路径未在真机触发。
evidence_links:      App 按钮 1 + 1B；代码 app/.../saf/SafExperiment.kt；files/results/TECH-ANDROID-001-R01.json
