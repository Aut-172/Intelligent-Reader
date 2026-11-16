# TECH-ANDROID-004-R01 · 同步文件持久化恢复

experiment_id:  TECH-ANDROID-004-R01
date:           2026-11-16
sample_id:      无（文件队列/游标持久化恢复）
source_sha256:  —
tool/version:   FileQueueStorage + FileCursorStorage（filesDir JSON/txt）
config_hash:    __
runtime:        Xiaomi 12S Pro (2206122SC) / Android 15 (API 35)
raw_output_path: files/sync_queue.json、files/sync_cursor.txt
metrics:        force-stop 前后 sync_queue.json 内容一致（仍含 fail-1）；sync_cursor=3（SyncWorker 后台推进）

failure_cases:  无
interpretation(I):   入队 3 条后同步，note-1/note-2 上传成功、fail-1 瞬时失败留队；`am force-stop` + 重启后队列文件内容逐字节一致，证明队列/游标落在磁盘。
conclusion(C):       文件持久化在 Android 真机上跨进程重启有效，无重复无丢失；后台 SyncWorker 完成游标推进。
open_risks(R):       真实弱网/乱序/部分失败未测；tombstone 与删除传播未覆盖。
evidence_links:      adb exec-out run-as ... cat files/sync_queue.json（force-stop 前后比对）；代码 app/.../sync/FileQueueStorage.kt、FileCursorStorage.kt
