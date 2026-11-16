# TECH-ANDROID-006-R01 · 本地检索降级

experiment_id:  TECH-ANDROID-006-R01
date:           2026-11-16
sample_id:      内置 3 个 TextBlock 索引，查询「引用定位」
source_sha256:  —
tool/version:   shared KeywordIndex + DegradationPolicy
config_hash:    __
runtime:        Xiaomi 12S Pro (2206122SC) / Android 15 (API 35)
raw_output_path: files/results/TECH-ANDROID-006-R01.json
metrics:        mode=LOCAL_SEARCH_ONLY; hitCount=1; hits=[b2]

failure_cases:  无
interpretation(I):   onlineAvailable=false 时 DegradationPolicy.decide 返回 LOCAL_SEARCH_ONLY；本地关键词索引命中 1 条（b2）。
conclusion(C):       断网时降级为「仅本地检索」且本地索引可用，D-06「无端侧生成模型仍保持本地检索」在真机成立。
open_risks(R):       真实供应方 SSE/WebSocket 与弱网未测；逐文件同意交互未覆盖。
evidence_links:      App 按钮 4；代码 shared/.../retrieval/KeywordIndex.kt、DegradationPolicy.kt；files/results/TECH-ANDROID-006-R01.json
