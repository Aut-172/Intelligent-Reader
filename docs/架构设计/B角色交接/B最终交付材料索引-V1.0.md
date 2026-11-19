# B 角色最终交付材料索引

- 版本：V1.0
- 日期：2026-11-19
- 角色：B（Windows 端架构调研与比较负责人）
- 依据：《架构调研与设计阶段四人分工方案-V1.0》§4.4、§8；`31-Windows端架构调研待测清单-V1.0`
- 用途：汇总可交付文件、对应要求条目、完成状态与多角色依赖标记，供 A 验收与集成

## 1 交付判定

| 维度 | 结论 |
|---|---|
| 文档交付（B-D01~B-D06、P0 证据索引、交接材料） | **齐备** |
| 实验证据（W-E 范围内可执行项） | **已收口**；未执行项按规则登记移交（见交接清单 §6） |
| 本轮收口 | **2026-11-19 完成**；C1–C9 补测后不再新增实验 |

> 结论：**B 角色本轮可交付**；多角色/非架构项已在交接清单 §6 登记移交，不阻塞 B 验收。
> 未通过/不完整项明细：见交接清单 **§7**（通过 2、部分 9、未执行 4）。

## 2 交付文件清单与要求映射

| 编号 | 文件 | 对应要求条目 | 状态 | 单人可完成 |
|---|---|---|---|---|
| B-D01 | `docs/架构设计/02-平台调研/Windows端架构调研与比较.md` §3 | 分工方案 §4.4 B-D01 平台约束清单 | 完成 | ✅ |
| B-D02 | 同上 §5 | §4.4 B-D02 候选路线矩阵 | 完成（D-12 A 已收敛，矩阵保留为历史对照） | ✅ |
| B-D03 | 同上 §4 | §4.4 B-D03 文档能力与引用链 | 完成（库层 + 合同） | ✅ |
| B-D04 | 同上 §6 | §4.4 B-D04 同步与离线 | 完成（分析层） | ✅ |
| B-D05 | 同上 §8 | §4.4 B-D05 发布验证计划 | 完成（计划 + 本机/干净机实测） | ✅ |
| B-D06 | 同上 §10 | §4.4 B-D06 建议与验证缺口 | 完成 | ✅ |
| P0 | `docs/架构设计/05-验证计划与证据索引/Windows端验证计划与证据索引.md` §2 | 分工方案 §8 P0 Windows 证据索引 | 完成（29 份结果报告 + W-E 映射） | ✅ |
| 交接 | `docs/架构设计/B角色交接/B角色交接清单-V1.0.md` | 验收、反馈问题 F-01~F-11、W-E 负责人建议 | 完成 | ✅ |
| 模板 | `docs/架构设计/B角色交接/W-E测试报告模板-V1.0.md` | 实验记录格式 | 完成 | ✅ |
| P4 | Windows 审核记录 | 分工方案 §8 P4 | **未完成** | ❌ 依赖 A 的报告总稿 |
| 交叉评审 | A、D 对 B 成果的评审记录 | 分工方案 §7.2 | **未完成** | ❌ 依赖 A、D |

## 3 实验报告清单（W-E 证据）

| 结果编号 | 覆盖 W-E | 文件 | 状态 |
|---|---|---|---|
| `TECH-WIN-001-R01` | 平台事实/环境 | `experiments/windows-env-probe/results/TECH-WIN-001-R01.md` | 完成（含 R02 复测） |
| `TECH-WIN-002-R01` | W-E06 身份 | `experiments/windows-env-probe/results/TECH-WIN-002-R01.md` | 完成 |
| `TECH-WIN-003-R01` | W-E10 DPAPI 能力 | `experiments/windows-env-probe/results/TECH-WIN-003-R01.md` | 完成 |
| `TECH-WIN-004-R01` | W-E01 | `experiments/windows-kmp-minimal/results/TECH-WIN-004-R01.md` | 完成 |
| `TECH-WIN-005-R01` | W-E02 | 同上目录 | 库层完成（选区/OCR/双端金标缺） |
| `TECH-WIN-006-R01` | W-E06 | 同上 | 规则层完成（UI 回跳缺） |
| `TECH-WIN-007-R01` | W-E07 | 同上 | 队列/崩溃/并发/断电完成；服务端缺 |
| `TECH-WIN-018-R01` | W-E07 崩溃恢复 | `experiments/windows-kmp-minimal/results/TECH-WIN-018-R01.md` | 完成（硬杀+重启恢复） |
| `TECH-WIN-019-R01` | W-E07 多进程 | `experiments/windows-kmp-minimal/results/TECH-WIN-019-R01.md` | 完成（3 进程 200 任务无重复） |
| `TECH-WIN-020-R01` | W-E07 崩溃+并发 | `experiments/windows-kmp-minimal/results/TECH-WIN-020-R01.md` | 完成（租约内不重复、到期回收；并发加固） |
| `TECH-WIN-021-R01` | W-E07 硬断电 | `experiments/windows-kmp-minimal/results/TECH-WIN-021-R01.md` | 完成（integrity=ok） |
| `TECH-WIN-008-R01` | W-E10 跨用户 | `experiments/windows-env-probe/results/TECH-WIN-008-R01.md` | 完成 |
| `TECH-WIN-009-R01` | W-E11 | `experiments/windows-kmp-minimal/results/TECH-WIN-009-R01.md` | 规则层完成 |
| `TECH-WIN-010-R01` | W-E12 | 同上 | 耗时+RSS+批量+首读；双端缺 |
| `TECH-WIN-011-R01` | W-E13 本机 | 同上 | 完成 |
| `TECH-WIN-011-R02` | W-E13 升级 | 同上 | 完成 |
| `TECH-WIN-012-R01` | W-E08 | 同上 | 规则层完成（真实代理缺） |
| `TECH-WIN-013-R01` | W-E10 CredMan | `experiments/windows-env-probe/results/TECH-WIN-013-R01.md` | 完成 |
| `TECH-WIN-014-R01` | W-E13 干净机 | `experiments/windows-kmp-minimal/results/TECH-WIN-014-R01.md` | 无效记录（并发污染，被 R02 取代） |
| `TECH-WIN-017-R01` | W-E03 OCR | `experiments/windows-ocr-probe/results/TECH-WIN-017-R01.md` | 部分（引擎/渲染/词框/方向；交互高亮移交） |
| `TECH-WIN-016-R01` | W-E13 签名 | `experiments/windows-kmp-minimal/results/TECH-WIN-016-R01.md` | 未完成（私钥权限） |
| `TECH-WIN-014-R02` | W-E13 干净机复跑 | `experiments/windows-kmp-minimal/results/TECH-WIN-014-R02.md` | 完成（安装/升级/卸载无残留） |
| `TECH-WIN-015-R01` | W-E15 首次重建 | `experiments/windows-kmp-minimal/results/TECH-WIN-015-R01.md` | 完成（构建/打包） |
| `TECH-WIN-029-R01` | W-E15 二次重建 | `experiments/windows-kmp-minimal/results/TECH-WIN-029-R01.md` | 完成（BUILD_OK/PACKAGE_OK2/17-17） |
| `TECH-WIN-022-R01` | W-E02 复杂样本 | `experiments/windows-kmp-minimal/results/TECH-WIN-022-R01.md` | 部分（旋转页 bbox 缺口） |
| `TECH-WIN-023-R01` | W-E12 批量 | `experiments/windows-kmp-minimal/results/TECH-WIN-023-R01.md` | 部分（双端缺） |
| `TECH-WIN-024-R01` | W-E03 高亮/缓存 | `experiments/windows-kmp-minimal/results/TECH-WIN-024-R01.md` | 部分（交互高亮/取消缺） |
| `TECH-WIN-025-R01` | W-E10 日志扫描 | `experiments/windows-env-probe/results/TECH-WIN-025-R01.md` | 未通过（dump 含路径） |
| `TECH-WIN-026-R01` | W-E11 故障注入 | `experiments/windows-kmp-minimal/results/TECH-WIN-026-R01.md` | 部分（模型/UI 缺） |
| `TECH-WIN-027-R01` | W-E06 解析器升级 | `experiments/windows-kmp-minimal/results/TECH-WIN-027-R01.md` | 规则层完成 |
| `TECH-WIN-028-R01` | W-E01 依赖图 | `experiments/windows-kmp-minimal/results/TECH-WIN-028-R01.md` | 完成 |

辅助材料：
- `experiments/windows-kmp-minimal/`：KMP 最小工程、`docProbe`（合同/渲染/批量/故障探针）、README、`results/vm/` 虚机原始结果；
- `experiments/windows-env-probe/`：平台/身份/DPAPI/CredMan/跨用户/日志扫描探针与结果；
- `experiments/windows-ocr-probe/`：Windows OCR 探针、样本与 JSON。

## 4 缺口与标记

| 缺口 | 类型 | 可否由 B 单独解决 | 说明 |
|---|---|---|---|
| 签名/SmartScreen | 环境/权限 | ⚠️ 需普通会话 + CA 证书 | signtool 已有；当前会话私钥访问被拒（NTE_PERM） |
| W-E03 交互高亮/回跳 | UI | ❌ 移交 A/C/D | 静态 bbox/坐标已完成；交互层需桌面壳 |
| W-E04 嵌入检索 | 服务选型 | ❌ 移交服务层 | 模型/权重属 D-23；B 不决策 |
| W-E02 选区/高亮、双端金标 | UI/多角色 | ❌ | 需桌面壳 + C 配合 |
| W-E02 旋转页 bbox | 合同缺陷 | ✅（B 可修） | 已确认 `/Rotate` 页 bbox=0.000（`TECH-WIN-022`） |
| W-E06 UI 回跳 | UI | ❌（单独不可） | 需桌面壳 |
| W-E07 物理损坏/网络盘 | 环境/范围 | ❌ 不适用 | 硬断电已测（021）；物理坏道与网络盘不在客户端本地 SQLite 目标内 |
| W-E08 真实代理/TLS | 工具/环境 | ✅（Fiddler/Clash 可选） | 本轮本地模拟 |
| W-E05 证据外发审计 | 服务端 | ❌ | 需测试网关（A/负责人） |
| W-E09 真实同步 | 服务端 | ❌ | 需 PostgreSQL/服务端（A/D） |
| W-E14 正式 API | 账号 | ❌ | 需正式 OpenAI 账号/费用（负责人） |
| P4 Windows 审核记录 | 评审 | ❌ | 依赖 A 报告总稿 |
| A/D 交叉评审 | 评审 | ❌ | 依赖 A、D 排期 |

## 5 给 A 的验收建议

1. 以本索引 §2 逐项核对 B-D01~B-D06 与 P0；
2. 以 §3 核对证据编号与报告文件；
3. 以 §4 核对缺口；多角色/非架构项按交接清单 §6 移交，不计入 B 待办；
4. P4 审核记录待 A 的报告总稿形成后由 B 补充。

## 6 目录结构改动（摘要）

| 类型 | 路径 | 说明 |
|---|---|---|
| 新增目录 | `experiments/windows-kmp-minimal/` | KMP 工程、docProbe、结果 |
| 新增目录 | `experiments/windows-ocr-probe/` | OCR 脚本/样本/JSON |
| 既有目录追加 | `experiments/windows-env-probe/` | 新增探针与 002/003/008/013/025 结果 |
| 目录改名 | `docs/架构设计/B角色交接/` | 原 `06-角色交接` |
| 删除 | `docs/架构设计/B角色交接/B本机配置与W-E启动步骤清单-V1.0.md` | 非交付物存根；原件在 `_local/` |

详见 `B角色交接清单-V1.0.md` §8。

> 本索引只汇总已有文件，不新增结论；所有结论以对应报告与调研正文为准。
