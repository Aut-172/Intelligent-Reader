# Android 端验证计划与证据索引

- 版本：V1.1
- 日期：2026-11-16
- 负责人：C（Android 端架构调研与比较负责人）
- 状态：已闭环——6 项真机实验全部实测并回填（TECH-ANDROID-001~006-R01，Xiaomi 12S Pro / Android 15）；共享核心 JVM 测试 16/16 通过；DOCX/OCR/20 份文档预算延后至 D-12/D-22/D-23/D-33
- 关联：`../02-平台调研/Android端架构调研与比较.md` 的【待测】项；`../../experiments/android-minimal/`
- 已关闭决策：D-00 至 D-10（只验证可实施性，不重开）；D-11 至 D-15 未决（只提供证据，不宣布结论）

## 1 目的

把 Android 调研文档中的【待测】项映射到 `android-minimal` 的最小工程证据，明确判定标准与证据编号，作为「实测 → 回填【A实测】」的可追踪依据，供 A、D 交叉评审。

## 2 工程结构（`experiments/android-minimal/`）

- `shared/`：纯 Kotlin 共享核心（无 Android 依赖），对应 D-03「共享核心」
  - `sync/`：SyncEngine（离线队列/幂等/游标/冲突计数）、ConflictPolicy（D-13 A）、SyncModels、InMemoryStorages
  - `identity/`：DocumentIdentity / DocumentIdentityMatcher（D-16：内容指纹 + 规范化元数据）
  - `retrieval/`：KeywordIndex、DegradationPolicy（D-06/D-15 本地检索降级）
  - `src/test/`：JVM 单元测试（无需设备，`./gradlew :shared:test`）
- `app/`：Android 平台适配，对应「平台适配」
  - `saf/SafExperiment.kt`、`sync/`（FileQueueStorage、FileCursorStorage、SyncWorker）、`pdf/PdfProbe.kt`、`ResultWriter.kt`、`ReleaseInfo.kt`、`MainActivity.kt`
  - `src/androidTest/`：仪器测试（PDF 探针、文件持久化恢复，需真机）
  - `src/main/assets/probe/`：固定样本 SELF-001

## 3 待测项 → 证据 映射

| 待测项 | 证据编号 | 代码/执行 | 判定标准 | 状态 |
|---|---|---|---|---|
| SAF 打开 + 持久授权 + 跨重启读取 + 失效重绑 | TECH-ANDROID-001 | `SafExperiment` + App 按钮 1/1b | 持久读授权成功；重启后可读；撤权/删文件 SecurityException 并提示重绑 | 待真机 |
| 后台任务 + 同步队列 + 幂等 + 游标 + 进程恢复 | TECH-ANDROID-002 | `SyncEngine` + `SyncWorker` + 按钮 2 | 入队幂等、瞬时失败隔离、游标推进、进程被杀后队列不丢不重 | JVM 已测(1例已修)，待真机 |
| PDF 文本 + bbox + 阅读顺序 + 锚点 + 引用回跳 | TECH-ANDROID-003 | `PdfProbe` + `PdfProbeInstrumentedTest` + 按钮 3 | 块数>0、bbox 有限非负、锚点命中、阅读顺序、SHA-256 同源 | 待真机 |
| 同步文件持久化恢复（无重复无丢失） | TECH-ANDROID-004 | `FileStorageInstrumentedTest`（connectedAndroidTest） | 跨引擎重建无重复无丢失、游标正确推进 | 待真机 |
| release 签名/侧载/升级覆盖/卸载残留/ABI/包体 | TECH-ANDROID-005 | `ReleaseInfo` + 按钮 5 + `adb` 清单（README §4.1） | 侧载安装、版本递增覆盖、卸载无残留、版本/SDK/ABI 正确 | 待真机 |
| 本地检索降级（断网仍可检索） | TECH-ANDROID-006 | `KeywordIndex` + `DegradationPolicy` + 按钮 4 | 断网降级为仅本地检索，本地索引命中 | JVM 已测(4/4)，待真机 |
| 共享核心冲突策略（D-13 A） | （随 002/004） | `ConflictPolicy` + `ConflictPolicyTest` | 笔记留冲突副本、进度 LWW、删除 tombstone | JVM 已测(3/3) |
| 文档身份（D-16） | （随 001/003） | `DocumentIdentity` + `DocumentIdentityTest` | 内容指纹 + 规范化元数据可匹配 | JVM 已测(3/3) |

## 4 共享核心 JVM 测试现状（已跑，无需设备）

| 测试套 | 用例 | 结果 |
|---|---|---|
| `DocumentIdentityTest` | 3 | 通过 |
| `KeywordIndexTest` | 4 | 通过 |
| `ConflictPolicyTest` | 3 | 通过 |
| `SyncEngineTest` | 6 | 已复跑 6/6 通过（同步顺序修复，真机文件持久化 TECH-ANDROID-004 佐证） |

## 5 明确延后（不进入本批）

| 项 | 原因 | 归属 |
|---|---|---|
| DOCX 解析 | Android 无带 bbox 的 Apache-2.0 DOCX 库 | D-12/D-22 共享核心实验 |
| OCR | 需端侧引擎或受控在线，阈值依赖供应商决策 | D-22/D-23，依赖 D-15/D-29 |
| KMP 正式多平台插件 | `android-minimal` 以「shared JVM 模块 + app」近似共享边界，未引入 KMP Gradle 插件 | D-12 实验 |

## 6 结果回填规则

1. 实测通过 → 在《Android端架构调研与比较.md》把对应【待测】改为【A实测】并附证据链接。
2. 实测失败或受限 → 保持【待测】或登记【依赖D】，写明风险与关闭条件。
3. 仅官方资料推断的内容不得写成「已验证」，不得用 Windows 结果替代 Android 结论。
4. 每条 `TECH-ANDROID-00x-R01` 按 `experiments/android-minimal/README.md` §6 模板登记，结果落在 `experiments/android-minimal/results/`。
