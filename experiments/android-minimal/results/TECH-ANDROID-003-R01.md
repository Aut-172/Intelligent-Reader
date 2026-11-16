# TECH-ANDROID-003-R01 · PDF 文本/bbox/阅读顺序/锚点

experiment_id:  TECH-ANDROID-003-R01
date:           2026-11-16
sample_id:      SELF-001-single-column.pdf
source_sha256:  0391dd982a1c426c52ca865bde7c253baaa4071d7ca26317febe2391a5389c93
tool/version:   pdfbox-android 2.0.27.0
config_hash:    __
runtime:        Xiaomi 12S Pro (2206122SC) / Android 15 (API 35)
raw_output_path: files/results/TECH-ANDROID-003-R01.json
metrics:        pageCount=1; blockCount=17; anchorHits(标题/研究目标/脚注)=true; readingOrderOk=true; sha256 与金标一致

failure_cases:  无（首轮因 PdfBox 未初始化崩溃 + 探针自身两处 bug，修复后转正）
interpretation(I):   PdfBox-Android 抽取 17 个文本块（PyMuPDF 桌面基线为 10 块，粒度不同）、字符级 bbox 有限非负、三个中文锚点命中、阅读顺序自上而下成立。
conclusion(C):       Android 端 PdfBox-Android 可产出「文本块 + bbox + 页码 + 阅读顺序 + 锚点命中」，SELF-001 上达成与桌面金标同源（sha256 一致）。
open_risks(R):       双栏/表格/公式/扫描页未覆盖；块粒度与坐标体系与桌面 PyMuPDF 的差异仍需固定样本逐一比对。
evidence_links:      App 按钮 3；代码 app/.../pdf/PdfProbe.kt；files/results/TECH-ANDROID-003-R01.json；金标 experiments/parser-benchmark/results/local-baseline/SELF-001.json
