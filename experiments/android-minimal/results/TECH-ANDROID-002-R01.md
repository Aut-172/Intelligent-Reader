# TECH-ANDROID-002-R01 · 后台任务 + 同步队列

experiment_id:  TECH-ANDROID-002-R01
date:           2026-11-16
sample_id:      无（离线 3 条模拟修改，其中 fail-1 瞬时失败）
source_sha256:  —
tool/version:   shared SyncEngine + WorkManager(SyncWorker)
config_hash:    __
runtime:        Xiaomi 12S Pro (2206122SC) / Android 15 (API 35)
raw_output_path: files/results/TECH-ANDROID-002-R01.json
metrics:        uploaded=2; transientFailed=1; queueRemaining=1; cursor=0

failure_cases:  无（行为符合预期：fail-1 瞬时失败留队，其余 2 条成功）
interpretation(I):   入队 3 条（note-1/fail-1/note-2），同步后 2 条成功上传、1 条瞬时失败留在队列、游标未推进（下载为空）。
conclusion(C):       入队幂等、部分失败隔离、瞬时失败留队重试在真机 App 路径成立，与 JVM SyncEngineTest（6/6 通过）一致。
open_risks(R):       真实网络弱网/乱序/部分失败未测；进程被杀后的队列恢复由 TECH-ANDROID-004 仪器测试覆盖。
evidence_links:      App 按钮 2；代码 shared/.../sync/SyncEngine.kt、app/.../sync/SyncWorker.kt；files/results/TECH-ANDROID-002-R01.json
