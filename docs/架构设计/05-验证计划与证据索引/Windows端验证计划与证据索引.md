# Windows 端验证计划与证据索引

- 版本：V1.1
- 日期：2026-11-17
- 负责人：B（Windows 端架构调研与比较负责人）
- 状态：P0 证据盘点完成；平台事实三项已实测（`TECH-WIN-001` R01/R02、`TECH-WIN-002-R01`、`TECH-WIN-003-R01`）；**W-E01 最小闭环已完成**（`TECH-WIN-004-R01`）；**W-E02 库层合同供给已验证**（`TECH-WIN-005-R01`）；**W-E06 引用/stale 规则层已完成**（`TECH-WIN-006-R01`）；**W-E07 持久任务队列层、崩溃恢复、多进程、崩溃并发与断电完整性已完成**（`TECH-WIN-007/018/019/020/021-R01`）；**W-E08 SSE/轮询回退规则层已完成**（`TECH-WIN-012-R01`）；**W-E10 跨用户凭据隔离与 Credential Manager 已验证**（`TECH-WIN-008/013-R01`）；**W-E11 诊断包规则层已完成**（`TECH-WIN-009-R01`）；**W-E12 三类样本耗时已测**（`TECH-WIN-010-R01`）；**W-E03 Windows OCR 引擎与识别已完成**（`TECH-WIN-017-R01`，文档接入待补）；**W-E13 本机安装/升级/卸载已完成**（`TECH-WIN-011-R01/R02`），**干净虚机安装/升级/卸载/无残留已完成**（`TECH-WIN-014-R02`）；**W-E15 虚机内干净重建已完成**（`TECH-WIN-015-R01`）；签名/桌面壳/正式同步/正式 API 等仍为缺口
- 关联：`../02-平台调研/Windows端架构调研与比较.md`；仓库根目录 `experiments/windows-env-probe/`；角色交接 `../B角色交接/`（本机操作辅助在 `_local/`，不提交）
- 决策状态：D-00 至 D-10 Accepted；D-11 至 D-35 方向已选、ADR 仍 Proposed
- 权威待测范围：`../阶段成果-V0.4-双端需求补充/31-Windows端架构调研待测清单-V1.0.md`（W-E01 至 W-E15）

## 0 版本变更（V1.0 → V1.1）【本阶段新增】

| 位置 | 上阶段 V1.0 | 本阶段 V1.1 | 依据 |
|---|---|---|---|
| 头部状态 | 仅 TECH-WIN-001 | 增加 002、003 与 001 的 R02 复测；明确正式待测由 W-E 清单管理 | 本次新增实验 |
| §2 证据索引 | 按能力域列 10 组证据 | 增加列「对应 W-E」，并补 TECH-WIN-002/003 | `31-Windows待测清单` |
| §3 待测映射 | 自建 TECH-WIN-002~007 计划编号 | **废止自建编号**（与 002/003 实际内容冲突），统一改用 W-E01~E15 | 编号冲突修正 |
| §4 已执行 | 001 一项 | 001（R01/R02）、002、003 三项 | 结果文件 |
| §7 环境 | 单次记录 | 增加 R02 复测一致性、DPAPI/身份探针环境 | 结果文件 |
| §8 双端对照 | 6 行 | 增加「正式证据成熟度」行，强调 W-E01 未开始 | D-34 A |

**未变部分**：证据质量提示、结果回填规则、延后项、与 Android 的对应关系，均保留并只做增量修订。

## 1 目的

分工方案 P0 要求 B 完成「Windows 已有实验和缺失证据」的盘点。本文档：

1. 把工作区全部 Windows 侧既有实验按能力域登记为可引用证据，标明运行环境、对应 W-E 与**不能证明什么**；
2. 记录本轮已执行的平台事实探针（001/002/003）与复测结果；
3. 把 Windows 侧正式待测项统一映射到 A 的 W-E01 至 W-E15，避免自建编号与官方清单冲突；
4. 规定「实测 → 回填证据等级」规则，供 A、D 交叉评审。

本文档不新增需求编号，不改变 D-00 至 D-10 结论，不把方向选择写成验收通过，不作为 SLA 依据。

## 2 已有 Windows 证据索引（P0 盘点，V1.1 增补）

### 2.1 能力域与证据编号

| 能力域 | 证据编号 | 运行环境/工具版本 | 已观察事实（摘要） | 对应 W-E | 不能证明什么 |
|---|---|---|---|---|---|
| 解析基线 | `TECH-DOC-001-R01` | Windows 宿主；Python 3.13.2、PyMuPDF 1.25.3、python-docx 1.1.2、markdown-it-py 4.2.0、Java 21.0.8、Maven 3.9.11 | 文本型 PDF 可返回页/块/坐标；DOCX 可读样式与表格但无视觉页码；MD 识别 4 个标题；GB18030 需回退 | W-E02 | 正式客户端渲染、选区、打包运行 |
| 解析评分 | `TECH-DOC-007-R02-MANUAL-SCORE` | 同上 + Docling 2.130.0 + Tika 3.2.1 | 三样本字符召回 1.0；PyMuPDF 双栏段落边界 0.2857；Docling 标题 F1 0.80–0.9091、表格 0.7778、脚注 0；Tika 无页码/bbox | W-E02 | 正式组件金标、真实论文/合同 |
| 结构识别 | `TECH-STRUCT-001~017` | Windows 宿主；Docling/PP-Structure 等 | 标题层级、脚注、表格候选可输出但需校验 | W-E02 | 结构即事实；跨端一致 |
| OCR | `TECH-OCR-001/002/009/015/016/018/019/020/022/023/024` | Windows 宿主；Tesseract、RapidOCR 3.9.2+ONNX、PaddleOCR/PP-Structure、EasyOCR、ocrmypdf | 清晰两页 RapidOCR 144 DPI 达暂定门槛；公开图片 11 问 RapidOCR 4/11、Tesseract 3/11 | W-E03 | 真实扫描件 CER/WER、客户端调度 |
| 检索 | `TECH-RET-001/003/007/008/009/010/011` | Windows 宿主；Python + 本地 bge-small-zh-v1.5 | Recall@3=1.0、RRF MRR=0.905、无答案误召回 1.0；真实解析 IR Hit@3=0.833、元数据完整率 0.606 | W-E04 | 客户端索引持久化、嵌入运行时、并发 |
| 引用 | `TECH-CIT-001~012`、`TECH-CIT-FOLLOWUP-R01` | Windows 宿主 | 页码映射率 1.0、bbox 检查通过、路由完成率 1.0；95 条证据中完整证据 0 条 | W-E06 | 桌面高亮 IoU、真实用户跳转 |
| 多文档与权限 | `TECH-MDOC-001-014`、`TECH-MDOC-004-009-013`、`TECH-MDOC-005-011`、`TECH-MDOC-011-R01~R07` | Windows 宿主；合成夹具 + 临时 SQLite FTS5 | 未授权泄露率 0、元数据完整率 1.0、平均证据覆盖 0.833；撤权/stale/删除/并发通过 | W-E05/W-E09 | 生产存储、备份删除、真实跨设备 |
| 问答与模型 | `TECH-QA-001/003/016~033` | Windows 宿主 + 官方 Codex CLI | 门控 11 题覆盖 0.778、拒答/复核 1.0；gpt-5.6-sol 状态 0.636、引用 0.818；p50/p95 28.8/33.6 s | W-E08/W-E14（仅参考） | 应用 API 政策、SLA、成本（D-29 A 禁止替代） |
| JVM 最小链路 | `TECH-ARCH-001/002/003/004` | Windows 宿主；Java 21 + Maven | Markdown→检索→证据→引用校验；路由降级；Evidence Gate 拒绝 4 类；CLI 适配器约 7.1 s | W-E01（近似但不等价） | KMP 多平台 target、Compose 桌面壳 |
| 平台事实 | `TECH-WIN-001-R01` / `R02` | 本机 Windows（Build 26300）；PowerShell 5.1；低完整性沙箱 | 系统/运行时/WebView2/缩放/路径身份/文件访问语义/凭据入口 15 项；两次运行一致 | W-E02（DPI）、W-E10（凭据入口）、W-E13（环境） | UI 壳、安装包、混合 DPI、凭据写入 |
| 文件身份与版本 | `TECH-WIN-002-R01`【新增】 | 本机 Windows；PowerShell 5.1 | 复制/改名/移动哈希不变；仅改时间哈希不变；同路径替换哈希变化；换行符差异改变哈希；删除重建哈希相同 | W-E06 | 卷级文件 ID、网盘占位、跨设备 |
| 凭据边界 | `TECH-WIN-003-R01`【新增】 | 本机 Windows；.NET Framework DPAPI | DPAPI 用户/机器作用域可用；密文无明文；篡改/换熵失败；复制可解；删除后消失 | W-E10 | 其他用户/重装、Credential Manager、日志扫描 |

### 2.2 证据质量提示（须随引用一并说明）

1. **机器不一致**：部分结果记录的执行环境与本工作区不同（如 `TECH-ARCH-004-R01` 的 executable 与工程路径指向另一台机器/另一个检出目录，详见该结果文件）。只证明「在某个 Windows 环境可运行」，不能作为本机性能、路径或依赖基线。
2. **层级不一致**：解析/OCR/检索/问答证据都在库层与进程内；没有任何一条证明「正式客户端里能选区、能高亮、能回跳」。
3. **样本规模小**：解析 3 份自制 PDF、OCR 2 页清晰样本 + 8 张公开图片 11 问、多文档 12 份合成文档；不得外推为真实文档质量。
4. **形态不一致**：没有任何一条证据来自打包后的制品（无 jpackage/MSI/zip 运行记录）。
5. **决策状态**：D-11 至 D-35 的「已选方向」不是「已验证」；ADR 仍 Proposed（`23-ADR` 第 2 节）。
6. **证据不可互替**：Android 结论不能替代 Windows，桌面脚本不能替代正式客户端（D-34 A、`19-V1.4` 第 6 节）。

## 3 待测项映射（统一使用官方 W-E 编号）

> V1.0 曾自建 `TECH-WIN-002~007` 计划编号；本轮 002/003 已用于实际执行的文件身份与 DPAPI 探针，为避免编号歧义，**计划项统一改用 A 的 W-E01~E15**，不再新增自建编号。后续实验编号由 A 的清单维护。

| W-E | 覆盖决策 | Windows 现状 | 必测实验（摘要） | 判定标准 | 状态 |
|---|---|---|---|---|---|
| W-E01 | D-12、D-28、D-34 | KMP Windows target 最小工程：构建成功、共享测试 29/29、桥接 20 符号；依赖图已导出，SQLite JDBC 仅在 windowsApp（`TECH-WIN-004/028-R01`） | 建真实 Windows target，消费同一 shared 领域/同步/合同/测试向量 | release-like 构建日志、共享测试、依赖图、桥接量、调试记录 | **最小闭环完成**（依赖隔离已证）；待 Android 同源复跑 |
| W-E02 | D-22、D-33 | PDFBox 库层合同：4 基础样本 bbox 1.000；CJK 字体/4×4 表格/A3 大页 bbox 1.000；**100 页/2000 runs/84940 字符 bbox 1.000**；**旋转 90° 页 bbox 0.000**（`TECH-WIN-005`、`TECH-WIN-022`） | 用 Windows 成熟组件验证固定文本/双栏/旋转/字体/表格/图片/大页 PDF | 页面、文本、坐标、高亮、能力与降级对照 | **部分完成**（字体/表格/大页/100 页通过；旋转页坐标缺口；选区/高亮/双端金标缺） |
| W-E03 | D-22、D-33 | Windows 内置 OCR（zh-Hans-CN）：中英数字 5/5、85.8 ms；PDF 渲染+OCR 3/3；词级 bbox 误差约 5 px；45°/灰度 JPEG 通过；90°/180° 自动回正后通过；静态高亮/缓存(3.6×)/失败降级通过，取消无 API（`TECH-WIN-017`、`TECH-WIN-024`） | 扫描/混合页的 OCR 触发、语言、置信度、取消、缓存、失败降级 | OCR 来源、准确率样本、耗时与资源 | **部分完成**（引擎/渲染/词框/方向/静态高亮/缓存/降级）；取消受 API 限制、交互高亮移交 UI |
| W-E04 | D-23、D-33 | 有桌面检索实验；BM25 已有；无客户端嵌入运行时；**模型/权重属服务选型（D-23），不在 B 的决策范围** | 本地全文/BM25 + 固定中文嵌入，测关键词/同义/增量/删除/损坏 | 金标、召回/排序、p50/p95、内存、磁盘、体积 | **登记缺口/移交下一阶段**（模型选型属服务层；Windows 运行时待验证） |
| W-E05 | D-24、D-29、D-31 | 有门控实验；无网络载荷审计 | 文件权限、同意、重排、去重、裁剪、撤权、并发 | 抓包与网关记录证明只外发授权证据 | **本轮不涉及**（服务端未决策） |
| W-E06 | D-25 | 引用双锚点与 stale 规则（`TECH-WIN-006-R01`）：VALID/STALE_VERSION/STALE_PARSER/LOCATOR_FAILED/QUOTE_MISMATCH；解析器升级测试 `parserUpgradeMarksStale` 通过（`TECH-WIN-027-R01`） | 插页、删页、重排、OCR 变化、解析器升级 | 双锚点回跳；跨版本明确 stale；无静默迁移 | **规则层完成**；真实库升级、UI 回跳未测 |
| W-E07 | D-26、D-34 | SQLite 持久队列（`TECH-WIN-007-R01`，7/7）；硬杀恢复（`TECH-WIN-018-R01`）；3 进程并发（`TECH-WIN-019-R01`）；崩溃+并发受控（`TECH-WIN-020-R01`）；硬断电后 `integrity_check=ok`、4790 任务一致（`TECH-WIN-021-R01`） | 排队、取消、崩溃、重启、重复调度、退出 | 状态迁移、恢复日志、无重复副作用 | **队列层+崩溃恢复+多进程+崩溃并发+断电完整性完成**；物理损坏/服务端任务表未测 |
| W-E08 | D-27、D-29 | SSE 解析、取消、超时与轮询回退已本地模拟（`TECH-WIN-012-R01`）：6/6 | 弱网、代理、VPN、睡眠唤醒、断流、取消、限流、轮询回退 | 网络日志、UI 状态、无重复片段 | **规则层完成**；真实代理/TLS/供应方未测 |
| W-E09 | D-21、D-26、D-30 | 有合成同步实验；无真实服务端 | 重复提交、响应丢失、cursor 崩溃点、删除竞争、设备撤销、REBASE_REQUIRED | 无漏变更、无重复副作用、未确认操作保留 | **本轮不涉及**（服务端未决策） |
| W-E10 | D-18、D-20、D-31 | DPAPI 跨用户隔离（`TECH-WIN-008-R01`）与 Credential Manager 写入/读回/删除（`TECH-WIN-013-R01`）已测；工作区目录对其他本地用户可读；日志/转储扫描：无密钥/令牌/密码，但 8 个崩溃转储含 41 处用户路径（`TECH-WIN-025-R01`） | 安全凭据、用户切换、退出/撤销、备份/复制、日志与崩溃转储扫描 | 凭据不在普通数据文件；扫描通过 | **部分完成**（两条凭据通道；路径扫描未通过）；跨用户 CredMan/重装未测 |
| W-E11 | D-32 | 诊断白名单/内容无关包（`TECH-WIN-009-R01`，5 用例 + 3 smoke）；解析损坏/空 PDF 可捕获 IOException，SSE 6/6、队列 7/7（`TECH-WIN-026-R01`） | 注入解析/同步/SSE/模型故障，生成可预览诊断包 | 关联 ID 可定位；包内无内容与秘密 | **部分通过**（解析/SSE/同步故障）；模型/UI 缺 |
| W-E12 | D-33、D-34 | 三类样本耗时（`TECH-WIN-010-R01`）：合同 p50 12–22 ms、扫描渲染 p50 116 ms；RSS app 7.9 MB / PDF 渲染 123.7 MB；20 份/30 页批量：首读 p50 1.9 ms、首反馈 4.8 ms、完成 2.7 ms、CPU 62.5 ms（`TECH-WIN-023-R01`） | 三类 PDF 测首次可读、首反馈、完成、p50/p95、内存、磁盘、CPU | 原始记录与汇总；候选数字只作对照 | **部分完成**（耗时/RSS/批量/首读已测）；双端/正式阈值未测 |
| W-E13 | D-34、D-35 | 本机安装/运行/升级/卸载闭环（`TECH-WIN-011-R01/R02`）；干净虚机单线程复跑：安装/升级/运行/卸载全部通过、无残留（`TECH-WIN-014-R02`；R01 为并发无效记录） | 签名/SmartScreen 待补 | 干净机安装/升级/卸载/残留 | **完成**（签名缺） |
| W-E14 | D-29 | 无应用 API 证据 | 通过受控网关调用 Responses API 候选快照，测结构化输出/流式/取消/限流/质量/费用 | 固定证据集结果、元数据、费用与政策记录 | **本轮不涉及**（服务端/账号未决策） |
| W-E15 | D-34、D-35 | 干净虚机内 JDK 21.0.2 + Gradle 8.14.7 + WiX 3.14.1 重建；首次构建/打包（`TECH-WIN-015-R01`），新虚机二次重建 **BUILD_OK / PACKAGE_OK2**、MSI 同大小、smoke 17/17（`TECH-WIN-029-R01`） | 干净 Win11 重建 shared、Windows 客户端与安装包，执行关键测试 | 环境清单、构建日志、制品哈希、测试结果 | **二次重建完成**（哈希因时间戳/卷序列号不同，大小/版本/测试一致）；签名未闭环 |

**执行顺序建议**（与 `31-Windows待测清单` 一致）：W-E01 → W-E02~E06 → W-E07~E09 → W-E10~E11 → W-E12~E15。

**本轮跳过汇总（按负责人 2026-11-18 指示，报告中说明后其余视为完整）**：

| 项 | 跳过原因 | 后续条件 |
|---|---|---|
| W-E03 OCR | 系统 OCR 可用；PDF 页面渲染/版面未接 | 接入 PDFBox 渲染 + 选区/高亮后补 |
| W-E04 混合检索 | 模型/权重属服务选型（D-23），不在 B 范围；Windows 运行时未验证 | 移交下一阶段：模型选型后补运行时与检索评测 |
| W-E05/W-E09/W-E14 | 服务端未决策 | 项目决策后由 A/负责人排期 |
| W-E13/W-E15 | W-E13 本机已完成、干净机部分；W-E15 虚机内构建/打包已完成（`TECH-WIN-014/015`） | 仅剩干净机卸载/签名；VM 通道不稳定，需另择时间复跑 |

**停止条件**：任一实验出现「无法获得固定样本可复现结果」或「工具链/账号/清洁机在课程周期内不可得」，即停止并登记原因，不得用其他平台或脚本证据替代。

## 4 本轮已执行的 Windows 探针

| 编号 | 脚本 | 结果文件 | 关键结论 |
|---|---|---|---|
| `TECH-WIN-001-R01` | `probe_windows_env.ps1` | `results/TECH-WIN-001-R01.md` | 环境矩阵、路径身份语义、缩放与凭据入口 15 项 |
| `TECH-WIN-001-R02` | 同上（复测） | 记录于 `results/TECH-WIN-001-R01.md` §0 | 15 项全部复现，系统/WebView2/缩放未变 |
| `TECH-WIN-002-R01` | `probe_windows_identity.ps1` | `results/TECH-WIN-002-R01.md` | 精确哈希可支撑 D-16 A；时间戳不可作判据；同路径内容替换必须新建版本；换行符差异会改变哈希 |
| `TECH-WIN-003-R01` | `probe_windows_dpapi.ps1` | `results/TECH-WIN-003-R01.md` | DPAPI 用户/机器作用域可用；密文无明文；篡改/换熵失败；复制可解；删除有效 |
| `TECH-WIN-004-R01` | Gradle KMP 工程 | `../../windows-kmp-minimal/results/TECH-WIN-004-R01.md` | Windows JVM target 构建成功；共享测试 16/16；smoke 10/10；依赖仅 kotlin-stdlib + `:shared`；桥接 20 符号；`expect/actual` 1 处 |
| `TECH-WIN-005-R01` | PDFBox 文档探针 | `../../windows-kmp-minimal/results/TECH-WIN-005-R01.md` | PDFBox 3.0.5 输出页面/文本/bbox/能力/降级；3 份文本型 PDF bbox 覆盖率 1.000、字符为 PyMuPDF 的 0.941–0.942；扫描件 `NO_TEXT_LAYER` |
| `TECH-WIN-006-R01` | 引用/stale 规则 | `../../windows-kmp-minimal/results/TECH-WIN-006-R01.md` | 8 个引用用例覆盖 5 种状态；跨版本默认 stale；重排/越界定位失败；显式重绑才可迁移；共享测试 24/24、smoke 14/14 |
| `TECH-WIN-007-R01` | SQLite 持久任务 | `../../windows-kmp-minimal/results/TECH-WIN-007-R01.md` | 幂等入队、lease 领取、完成/失败/取消、重启恢复；`windowsApp` 测试 7/7 |
| `TECH-WIN-008-R01` | DPAPI 跨用户隔离 | `results/TECH-WIN-008-R01.md`（原始 JSON 同目录） | A 加密的 246B 密文被 B 读取但无法解密；B 自身往返成功；B 无法列出 A 的 profile/Protect；B 可读项目 docs 目录 |
| `TECH-WIN-009-R01` | 诊断包规则 | `../../windows-kmp-minimal/results/TECH-WIN-009-R01.md` | 属性白名单、禁止字段丢弃、包泄漏扫描、关联 ID 保留；共享测试 29/29、smoke 17/17 |
| `TECH-WIN-010-R01` | 三类样本耗时 | `../../windows-kmp-minimal/results/TECH-WIN-010-R01.md` | 合同生成 p50 12–22 ms；扫描件渲染 p50 116 ms；内存受 GC 影响未采用 |
| `TECH-WIN-011-R01` | 打包与安装 | `../../windows-kmp-minimal/results/TECH-WIN-011-R01.md` | app-image 164 MB（294 文件）、MSI 68.8 MB；MSI 每用户安装/运行/卸载成功；app-image 冷启动 0.37 s、安装版 0.59 s |
| `TECH-WIN-011-R02` | 升级覆盖 | `../../windows-kmp-minimal/results/TECH-WIN-011-R02.md` | 1.0.0 → 1.0.1 覆盖成功；ProductCode 变更、路径不变；升级后 17/17；卸载无残留 |
| `TECH-WIN-014-R01` | 干净虚机安装/升级（无效） | `../../windows-kmp-minimal/results/TECH-WIN-014-R01.md` | 并发安装器导致 1618/1605；保留作根因记录 |
| `TECH-WIN-014-R02` | 干净虚机安装/升级/卸载 | `../../windows-kmp-minimal/results/TECH-WIN-014-R02.md` | 单线程：安装/升级/卸载 exit 0；两次 17/17；卸载后无残留 |
| `TECH-WIN-015-R01` | 干净环境重建 | `../../windows-kmp-minimal/results/TECH-WIN-015-R01.md` | JDK 21.0.2 + Gradle 8.14.7 + WiX 3.14.1 重建；BUILD_OK；MSI 71,300,856 B（哈希已记） |
| `TECH-WIN-016-R01` | W-E13 签名 | `../../windows-kmp-minimal/results/TECH-WIN-016-R01.md` | signtool 可用；当前会话私钥访问被拒（NTE_PERM），签名未完成 |
| `TECH-WIN-017-R01` | W-E03 OCR | `../../windows-ocr-probe/results/TECH-WIN-017-R01.md` | Windows 内置 OCR：zh-Hans-CN，5/5，85.8 ms |
| `TECH-WIN-022-R01` | W-E02 复杂样本 | `../../windows-kmp-minimal/results/TECH-WIN-022-R01.md` | CJK/表格/大页 bbox 1.000；旋转页 bbox 0.000 |
| `TECH-WIN-023-R01` | W-E12 批量 | `../../windows-kmp-minimal/results/TECH-WIN-023-R01.md` | 20 份/30 页：首读 p50 1.9 ms、首反馈 4.8 ms、完成 2.7 ms |
| `TECH-WIN-024-R01` | W-E03 高亮/缓存 | `../../windows-kmp-minimal/results/TECH-WIN-024-R01.md` | 11 词静态高亮；缓存 125.5→34.7 ms；缺失图降级 |
| `TECH-WIN-025-R01` | W-E10 日志扫描 | `../../windows-env-probe/results/TECH-WIN-025-R01.md` | 无秘密；8 个 dump 含 41 处用户路径（未通过） |
| `TECH-WIN-026-R01` | W-E11 故障注入 | `../../windows-kmp-minimal/results/TECH-WIN-026-R01.md` | 损坏/空 PDF 可捕获；SSE 6/6；队列 7/7 |
| `TECH-WIN-027-R01` | W-E06 解析器升级 | `../../windows-kmp-minimal/results/TECH-WIN-027-R01.md` | `parserUpgradeMarksStale` 通过（规则层） |
| `TECH-WIN-028-R01` | W-E01 依赖图 | `../../windows-kmp-minimal/results/TECH-WIN-028-R01.md` | shared/windowsApp 依赖树；无 FAILED |
| `TECH-WIN-029-R01` | W-E15 二次重建 | `../../windows-kmp-minimal/results/TECH-WIN-029-R01.md` | 新虚机 BUILD_OK/PACKAGE_OK2；MSI 同大小；17/17 |
| `TECH-WIN-018-R01` | W-E07 崩溃恢复 | `../../windows-kmp-minimal/results/TECH-WIN-018-R01.md` | 硬杀退出 7；回收租约；5 任务全部 SUCCEEDED |
| `TECH-WIN-019-R01` | W-E07 多进程 | `../../windows-kmp-minimal/results/TECH-WIN-019-R01.md` | 3 进程/200 任务：无重复、无 BUSY、全部成功 |
| `TECH-WIN-020-R01` | W-E07 崩溃+并发 | `../../windows-kmp-minimal/results/TECH-WIN-020-R01.md` | 租约内不重复；到期回收；busy_timeout/原子 UPDATE/WAL 加固 |
| `TECH-WIN-021-R01` | W-E07 硬断电 | `../../windows-kmp-minimal/results/TECH-WIN-021-R01.md` | 硬断电后 integrity=ok；4790 任务全部 SUCCEEDED |
| `TECH-WIN-012-R01` | SSE/轮询回退 | `../../windows-kmp-minimal/results/TECH-WIN-012-R01.md` | SSE 顺序、取消、超时、HTTP 错误回退、双失败共 6/6；仅本地桩 |
| `TECH-WIN-013-R01` | Credential Manager | `results/TECH-WIN-013-R01.md` | 写入/读回/删除/残留检查全部通过；`cmdkey` 无条目 |

本轮同时确认（2026-11-18 更新）：Windows 平台事实可复现；JDK **21.0.12.1**（含 `jpackage`）与 Gradle **8.14.7** 均已就绪（`GRADLE_USER_HOME=D:\#_Dev\#_CACHES\gradle`）；**W-E01 最小闭环已完成**（KMP Windows target 构建成功、共享测试 16/16）。项目负责人已授权在「仅服务架构调研」前提下安装工具链（配置须写入测试报告），**本轮不含服务端**。W-E13/W-E15 另需 WiX 与清洁环境；W-E05/W-E09/W-E14 本轮不执行。本机安装内容不属于架构证据，不写入本索引。

## 5 明确延后（不进入本批）

| 项 | 原因 | 归属 |
|---|---|---|
| KMP 正式多平台 Gradle 工程 | 项目未建立 KMP Gradle 工具链基线；需固定版本并联网拉依赖 | W-E01 |
| 正式文档组件集成（PDFBox/PDFium） | 需 Windows target 与固定金标 | W-E02 |
| 客户端 OCR 调度 | 需正式客户端 | W-E03 |
| 本地嵌入运行时与索引 | 需客户端工程与模型资产 | W-E04 |
| 真实同步服务与 PostgreSQL | 需最小服务端 | W-E09 |
| 正式 OpenAI 应用 API | 需项目账号与费用上限 | W-E14 |
| 签名安装包与干净机 | 需工具链与清洁环境 | W-E13/W-E15 |
| 三类文档正式阈值 | 阈值属 D-33 B，未测得前不得写成 SLA | W-E12 |

## 6 结果回填规则

1. 实测通过 → 在《Windows端架构调研与比较.md》把对应【待测】改为【W实测】并附证据编号与文件链接。
2. 实测失败或受限 → 保持【待测】或改为【依赖D】，写明受限原因与关闭条件；失败证据不得删除。
3. 仅官方资料支持的内容不得写成「已验证」；不得用 Android 结果替代 Windows，也不得用库层/脚本结果替代正式客户端。
4. 每条结果需登记：样本、设备、Windows 版本/构建、缩放/显示器、工具链版本、计量口径、停止条件与原始结果路径。
5. 若实测与 D-00 至 D-35 冲突，只登记证据、影响与重开建议，由 A 按 ADR 重新打开条件处理，不在本文件改结论。
6. 正式待测项一律使用 W-E 编号；不再新增 `TECH-WIN-*` 计划编号，`TECH-WIN-*` 只用于已执行的探针结果。

## 7 环境与样本声明（防「结果不可复现」）

- 环境：实验机 `HYC-1`，Build 26300.9974（Windows 11 分支），x64，单显示器，150% 缩放，未提权，`LongPathsEnabled=1`，WebView2 `157.0.4341.46`；探测进程为低完整性沙箱进程（代理环境造成，不代表普通桌面应用上下文）。
- 复测：`TECH-WIN-001-R02` 于 2026-11-17 21:32 复跑，15 项与 R01 一致。
- 样本：沿用项目自制固定样本 `SELF-001`（单栏）、`SELF-002`（双栏）、`SELF-003`（表格/公式）、`SELF-004`（扫描），均有 SHA-256 与人工标注；新增样本须登记来源与授权状态。
- 计量：冷启动、首屏、打开文档、首字反馈、完整回答、解析/索引 CPU 与内存峰值；分别报告 Windows 与 Android。
- 合规：样本公开或经授权；不使用未授权真实资料；日志与结果不含原文、密钥与查询内容。

## 8 与 Android 验证计划的对应关系（供 A 横向比较）

| 维度 | Android（C） | Windows（B） |
|---|---|---|
| 已实测平台事实 | SAF 持久授权、WorkManager、Keystore、release APK、PDF bbox（`TECH-ANDROID-001~006`） | 环境/WebView2/缩放/路径身份/文件语义/凭据入口（`TECH-WIN-001`）；哈希身份（`TECH-WIN-002`）；DPAPI（`TECH-WIN-003`） |
| 正式客户端证据 | 最小工程 + 真机已测 | **KMP Windows target 最小工程已建（W-E01 部分）**，无 Compose 壳与文档组件 |
| 桌面壳/发布 | release APK 侧载已实测 | 桌面壳与打包全部【待测】 |
| 离线任务 | WorkManager 持久队列 | 无系统调度，自建持久任务（D-26 A） |
| 文件授权 | SAF + 持久 URI + 失效重绑 | 路径 + ACL，无持久授权；哈希判定同一内容（D-16 A） |
| 凭据 | Keystore（硬件级） | DPAPI/Credential Manager（用户绑定，已实测能力） |
| 主要风险 | 文档组件与进程回收 | UI 壳、打包、混合 DPI、正式 target |

> 结论：两端的「证据成熟度不对称」仍然存在——Android 已完成真机最小工程，Windows 仍停在库层、宿主层与平台事实层。A 在横向比较时必须保留该差异（D-34 A）。
