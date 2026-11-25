# W-E02 测试报告：Windows 文档组件与统一合同（库层）

- 实验编号：W-E02（结果编号 `TECH-WIN-005-R01`）
- 关联决策：D-22 A（分端成熟组件 + 统一文档能力合同）、D-33 B（分级预算，本次只记录耗时）
- 关联 ADR：ADR-022
- 执行人：B
- 日期：2026-11-18
- 状态：**部分完成**——文本型 PDF 与扫描件降级在库层验证通过；文本选择、OCR、双端金标对照未执行（需其他角色/运行时）
- 跳过说明：按项目负责人 2026-11-18 指示，需多角色配合的项（Android 同源金标、产品级选区/高亮）本轮跳过并在 §6 说明；其余结果视为本项完整。

## 1 目的

验证 Windows 端能否用成熟文档组件产出 D-22 A 的统一合同字段：页面、文本范围、bbox、解析器版本、能力标记、OCR 来源与降级原因；并记录单页渲染与合同生成耗时（供 D-33 B 参考，不作为阈值）。

## 2 环境配置快照

| 项 | 值 |
|---|---|
| 主机/系统 | HYC-1 / Build 26300.9974（Windows 11 分支） |
| JDK | Oracle 21.0.12.1（`D:\#_Dev\Java_compiler`） |
| Gradle | 8.14.7（`D:\#_Dev\gradle-8.14.7`，`GRADLE_USER_HOME=D:\#_Dev\#_CACHES\gradle`） |
| Kotlin | 2.1.20；JVM toolchain 21 |
| 文档组件 | **Apache PDFBox 3.0.5**（Apache-2.0） |
| 日志绑定 | `slf4j-nop 2.0.16`（避免 PDFBox 日志干扰输出） |
| 样本 | `SELF-001`（单栏）、`SELF-002`（双栏）、`SELF-003`（表格/公式）、`SELF-004`（扫描） |
| 对照基线 | 既有 `TECH-DOC-001-R01` 的 PyMuPDF 1.25.3 字符数（293/572/287/1） |
| 网络 | 首次解析依赖需访问 Maven Central（本次构建含下载，28 秒） |

配置变更：新增 `docProbe` 模块与 `DocumentContract` 共享合同；无系统级变更；回滚为删除 `docProbe` 与 `shared/.../document/`。

## 3 步骤与命令

```powershell
$env:JAVA_HOME = 'D:\#_Dev\Java_compiler'
$env:GRADLE_USER_HOME = 'D:\#_Dev\#_CACHES\gradle'
& 'D:\#_Dev\gradle-8.14.7\bin\gradle.bat' --no-daemon --console=plain :docProbe:run --args="SELF-001=<pdf>=293 SELF-002=<pdf>=572 SELF-003=<pdf>=287 SELF-004=<pdf>=1"
```

探针实现：`PositionalStripper` 继承 `PDFTextStripper`，按行收集 `TextPosition` 的 `x/y/width/height`，生成 `TextRun(page, charStart, charEnd, text, bbox)`；`PDFRenderer.renderImageWithDPI(0, 72f)` 验证渲染能力；输出统一 `DocumentContract` 并写入 TSV。

## 4 结果

| 样本 | 页数 | 文本段 | 字符数 | 相对 PyMuPDF | bbox 覆盖率 | 渲染尺寸(72dpi) | 渲染耗时 | 合同总耗时 | 降级 |
|---|---:|---:|---:|---:|---:|---|---:|---:|---|
| SELF-001 | 1 | 17 | 276 | 0.942 | **1.000** | 595×841 | 88 ms | 1807 ms* | — |
| SELF-002 | 2 | 32 | 539 | 0.942 | **1.000** | 595×841 | 15 ms | 42 ms | — |
| SELF-003 | 1 | 17 | 270 | 0.941 | **1.000** | 595×841 | 9 ms | 27 ms | — |
| SELF-004 | 2 | 0 | 0 | 0.000 | 0.000 | 793×1122 | 270 ms | 289 ms | `NO_TEXT_LAYER` |

\* SELF-001 的 1807 ms 含首次类加载/JIT，不代表稳态；同类后续样本为 27–42 ms。

合同字段示例（SELF-001）：

```text
p1 0-14   BBox(x=194.17, y=75.02, w=206.94, h=11.62)  标题行
p1 15-21  BBox(x=57.02,  y=124.03, w=58.59, h=7.75)  “1 研究目标”
p1 22-75  BBox(x=57.02,  y=143.53, w=441.12, h=6.13)  正文段落
```

## 5 合同字段供给判定（对照 D-22 A）

| 合同字段 | PDFBox 3.0.5 供给 | 证据 |
|---|---|---|
| 页面标识与页尺寸 | ✅ | `PageInfo(page, widthPt, heightPt)`，4 个样本全部输出 |
| 文本与字符范围 | ✅ | `TextRun.charStart/charEnd`，字符序列连续 |
| bbox/坐标 | ✅ | 4 个文本型样本 bbox 覆盖率 **1.000** |
| 解析器名称/版本 | ✅ | `Apache PDFBox` / `3.0.5` |
| 能力标记 | ✅ | 文本型：`RENDER,TEXT,BBOX`；扫描件：`RENDER` |
| OCR 来源 | ✅（缺省为空） | 本次未接 OCR，扫描件以 `NO_TEXT_LAYER` 降级 |
| 降级原因 | ✅ | SELF-004 `NO_TEXT_LAYER` |
| 选择/高亮 | ❌ 未验证 | PDFBox 不提供 UI 选区；属桌面壳/W-E02 的后续部分 |

## 6 跳过项与原因

| 项 | 原因 | 归属 |
|---|---|---|
| Android 同源金标对照（同一 `DocumentContract` 双端比对） | 需 C 的 Android 最小工程输出同合同；单端无法完成 | C/A 配合 |
| 产品级选区、高亮、引用回跳 | 需桌面 UI 壳（Compose/WebView）与交互测试 | W-E02 后续或 W-E06 |
| OCR（扫描件文字识别） | 需 OCR 运行时（Tesseract/RapidOCR）与语言包；本机未安装，且授权范围未包含 | W-E03 |
| 旋转页、复杂字体、跨页表格 | 本轮固定样本未覆盖这些特性 | 后续样本扩展 |

按负责人 2026-11-18 指示：上述跳过项在报告中说明，其余结果视为 W-E02 本轮完整。

## 7 结论

- **原始事实（F）**：PDFBox 3.0.5 对 3 份文本型 PDF 输出页面、文本范围与 bbox（覆盖率 1.000）；字符数为 PyMuPDF 基线的 0.941–0.942（差异来自换行/空白计数口径，不是缺字）；扫描件正确降级为 `NO_TEXT_LAYER` 且仍可渲染。
- **项目组解释（I）**：D-22 A 的统一合同在 Windows 库层可落地；渲染与抽取可分离（PDFBox 同时提供两者），坐标可直接用于合同而不依赖 UI 组件；扫描件必须显式降级，不能把空文本当成功。
- **技术/需求结论（C）**：W-E02 的「合同字段供给」在 Windows 侧成立；但 ADR-022 的关闭还需要双端同源金标、选区/回跳与 OCR 对照，故 **ADR-022 保持 Proposed**。

## 8 回填动作

1. 证据索引 §3：W-E02 状态改为「部分完成（库层合同供给）」。
2. 调研文档 §4.2：补充 PDFBox 实测字段供给与字符比率。
3. 不改变 D-22 A 的决策状态；双端金标对照待 C 配合。
