# W-E03 OCR 能力实验（Windows 内置 OCR）

- 实验编号：W-E03（结果编号 `TECH-WIN-017-R01`）
- 关联决策：D-14 A、D-22 A
- 执行人：B
- 日期：2026-11-18
- 状态：**部分完成**——引擎可用、中英数字识别通过；尚未接入 PDF 页面渲染与版面/选区
- 环境：本机 Windows 11 Build 26300，PowerShell 5.1，无需安装第三方 OCR

## 1 目的

验证 Windows 端 OCR 运行时是否可用、中文/英文/数字识别是否可靠、单页耗时量级，为「扫描页 OCR」提供依据。

## 2 方法

1. 用 `System.Drawing` 生成 1400×420 PNG，两行文本：
   - `智能阅读助手 OCR 测试 2026`
   - `Reader Studio W-E03 12345`
2. 用 WinRT `Windows.Media.Ocr.OcrEngine`（用户配置语言 `zh-Hans-CN`）识别；
3. 记录文本、行数、耗时与 5 项关键词命中；
4. **PDF 接入**：用 PDFBox 3.0.5 生成一页文本 PDF → `PDFRenderer.renderImageWithDPI(0, 200)` 渲染为 PNG → 同一 OCR 引擎识别，记录渲染与识别耗时。

脚本与样本：`experiments/windows-ocr-probe/ocr_winrt.ps1`、`ocr_image.ps1`、`results/we03-sample.png`、`results/we03-render.pdf`、`results/we03-render.png`、`results/we03-render-ocr.json`；渲染器：`experiments/windows-kmp-minimal/docProbe/src/main/kotlin/reader/windowsmin/docprobe/RenderMain.kt`。

## 3 结果

| 项 | 结果 |
|---|---|
| 识别引擎 | `Windows.Media.Ocr`（系统内置） |
| 可用语言 | `zh-Hans-CN` |
| 耗时 | **85.8 ms**（单张 1400×420，含解码与识别） |
| 行数 | 2 |
| 识别文本 | `智 能 阅 读 助 手 OCR 测 试 2026` / `Reader Studio W-E03 12345` |
| 关键词命中 | 中文、OCR、2026、Reader、12345 **5/5** |

### 3.1 PDF 渲染 → OCR 接入

| 项 | 结果 |
|---|---|
| PDF 生成 | PDFBox 3.0.5，A4，3 行 Helvetica 文本 |
| 渲染 | `PDFRenderer.renderImageWithDPI(0, 200)` → PNG **1653×2338**，**231.7 ms** |
| OCR | **105 ms**，3 行，文本与源一致 |
| 关键词命中 | `Reader Studio W-E03 2026`、`PDF render`、`12345` **3/3** |

> 注：Windows OCR 在中文词之间插入空格，属其分词输出特性；下游需做空白归一化。

### 3.2 词级 bbox 与坐标映射

`ocr_image.ps1` 现已输出每个词的 `BoundingRect`（x/y/width/height，像素）：

| 样本 | 词数 | 示例 |
|---|---|---|
| 中文图片（1400×420） | 14 | `智`（53, 76, 69, 73）；中文按字给出 |
| PDF 渲染页（200 dpi） | 11 | `Reader`（205, 346, 215, 49） |

坐标映射（200 dpi，`scale = 200/72 = 2.7778`）：

- 页面像素 → PDF 点：`pdf_x = x / scale`，`pdf_y = 842 - (y + h) / scale`；
- 第一行文本在 PDF 中起点为 72 pt、基线 700 pt：期望左边界 `72×2.7778 ≈ 200 px`，期望顶部 `≈346 px`；
- `Reader` 识别框为 `x=205, y=346`，误差约 **5 px（≈1.8 pt）** 与 **<1 px**，可用于选区/高亮定位；
- 选择框可按词框归一化：`(x/W, y/H, w/W, h/H)`，再映射回 PDF 点坐标。

边界：bbox 为轴对齐矩形，旋转/倾斜文本未测；尚无 UI 高亮与回跳实现。

### 3.3 旋转与扫描件鲁棒性

| 变体 | 识别文本 | 词数 | 结果 |
|---|---|---|---|
| 0° | `Reader Studio W-E03 2026` | 4 | ✅ |
| 45° | `Reader Studio W-E03 2026` | 4 | ✅（Windows OCR 可处理 45° 倾斜） |
| 90° | （空） | 0 | ❌ 完全未识别 |
| 180° | `9 乙 0 乙 EO 彐 一 0!pms P` | 9 | ❌ 倒置乱码 |
| 90° 回正后 | `Reader Studio W-E03 2026` | 4 | ✅ 2/2 |
| 180° 回正后 | `Reader Studio W-E03 2026` | 4 | ✅ 2/2 |
| 灰度 + JPEG q55（模拟扫描） | 全部 3 行 | 11 | ✅ 51 ms |

结论：Windows OCR 对 45° 倾斜与灰度/JPEG 压缩较稳健，但 **90°/180° 需要先做方向检测与旋转预处理**；回正后 90°/180° 均恢复为 4 词、2/2 命中。实际管线应先检测页面方向（或读取 PDF `/Rotate`），把图像转正后再 OCR。脚本：`ocr_variants.ps1`、`ocr_rotate_fix.ps1`；样本：`we03-rot-*.png`、`we03-rot-*-fixed.png`、`we03-scan-like.jpg`；数据：`we03-variants.json`、`we03-rot-*-fixed.json`。

### 3.4 自动方向检测（4 向搜索）

不依赖 PDF `/Rotate`，对同一图片尝试 0°/90°/180°/270° 四个方向，各做一次 OCR，以「归一化字符数」为分数选最优：

| 源图 | 选中旋转 | 识别结果 | 词数 |
|---|---|---|---|
| `we03-rot-90.png` | **270°** | `Reader Studio W-E03 2026` | 4 |
| `we03-rot-180.png` | **180°** | `Reader Studio W-E03 2026` | 4 |
| `we03-rot-45.png` | 0°（已可识别） | `Reader Studio W-E03 2026` | 4 |

脚本：`ocr_orient.ps1`；数据：`we03-orient-90/180/45.json`、`we03-orient-*.png`。代价是 4 次 OCR（每次约 20–30 ms），对扫描页可接受；真实管线可先用低分辨率快速判向，再对正图做一次高精度 OCR。

## 4 判定

| 维度 | 结果 |
|---|---|
| OCR 运行时可用 | ✅ 系统内置，无需安装 |
| 中文识别 | ✅ |
| 英文/数字识别 | ✅ |
| 单页耗时 | ✅ 85.8 ms 量级（图片）；PDF 渲染 231.7 ms + OCR 105 ms |
| PDF 页面渲染 → OCR 接入 | ✅ 已接（PDFBox 3.0.5 → PNG → Windows OCR，3/3） |
| 版面/选区回跳 | ⚠️ 部分：词级 bbox 已输出并可映射到 PDF 点；UI 高亮/回跳未接 |
| 旋转文本 | ✅ 45° 直接通过；90°/180° 经 4 向自动检测回正后通过 |
| 扫描件（灰度/JPEG） | ✅ 通过（11 词，51 ms） |
| 手写/多语言包 | ❌ 未测 |

## 5 结论

- **原始事实（F）**：Windows 11 自带 OCR（zh-Hans-CN）在本机可用，中英数字样本 5/5、85.8 ms；PDFBox 渲染 200 dpi 231.7 ms + OCR 105 ms、3/3；词级 bbox 误差约 5 px/1 px；45° 与灰度 JPEG 通过；90°/180° 原始失败，4 向自动检测回正后全部通过。
- **项目组解释（I）**：系统 OCR 可作为扫描页最低成本方案；PDF 渲染 → OCR → 词框链路可支撑选区定位；垂直/倒置页面需方向检测（4 向搜索或 PDF `/Rotate`），Windows OCR 不会自动纠正 90°/180°。
- **技术/需求结论（C）**：W-E03 的运行时、识别、PDF 渲染、词级 bbox、扫描件鲁棒性与方向回正成立；UI 高亮回跳与手写仍待补，D-14 保持 Proposed。
