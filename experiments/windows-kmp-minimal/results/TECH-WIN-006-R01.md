# W-E06 测试报告：引用双锚点与跨版本 stale

- 实验编号：W-E06（结果编号 `TECH-WIN-006-R01`）
- 关联决策：D-25 A（结构锚点 + 字符锚点；跨版本默认 stale，不自动迁移）、D-16 A（内容变化建立新版本）
- 关联 ADR：ADR-025
- 执行人：B
- 日期：2026-11-18
- 状态：**部分完成**——锚点规则与 stale 判定在共享核心验证通过；桌面高亮回跳与真实解析器升级未验证
- 跳过说明：按负责人 2026-11-18 指示，需 UI/双端配合的「真实回跳与跨端金标」本轮跳过，其余视为本项完整。

## 1 目的

验证 D-25 A 的引用规则在 Windows 目标上可执行：

1. 同版本、同锚点、同引文 → `VALID`；
2. 版本变化（versionId/contentHash 变化）→ `STALE_VERSION`，即使文本相同也不自动迁移；
3. 解析器版本升级 → `STALE_PARSER`；
4. 块被删/替换、重排、字符范围越界 → `LOCATOR_FAILED`；
5. 同块文本变化导致引文不匹配 → `QUOTE_MISMATCH`；
6. 只有显式重绑才能产生新版本上的有效引用。

## 2 环境配置快照

| 项 | 值 |
|---|---|
| 主机/系统 | HYC-1 / Build 26300.9974 |
| JDK | Oracle 21.0.12.1 |
| Gradle | 8.14.7；`GRADLE_USER_HOME=D:\#_Dev\#_CACHES\gradle` |
| Kotlin | 2.1.20；JVM toolchain 21 |
| 新增依赖 | 无（纯共享核心代码） |
| 样本 | 合成 `DocumentSnapshot`（块 ID、章节、阅读顺序、文本），不依赖外部文件 |
| 网络 | 不需要 |

配置变更：新增 `shared/src/commonMain/.../citation/Citation.kt` 与对应测试；回滚为删除该文件。

## 3 实现要点

- `Citation` 同时保存 `StructuralAnchor(blockId, sectionPath, orderInSection)` 与 `CharacterAnchor(start, end)`，并绑定 `DocumentVersionRef(documentId, versionId, contentHash, parserVersion)`；
- `CitationVerifier.verify` 的判定顺序：文档 → 版本/哈希 → 解析器版本 → 块存在 → 章节 → 顺序 → 字符范围 → 引文归一化比对；
- `rebindExplicitly` 只能由调用方在用户确认后显式调用，`verify` 内不做任何迁移。

## 4 结果

| 用例 | 判定 | 结果 |
|---|---|---|
| 同版本同锚点同引文 | `VALID` | ✅ |
| 新版本、文本相同 | `STALE_VERSION` | ✅ 不自动迁移 |
| 解析器升级（parserVersion 变化） | `STALE_PARSER` | ✅ |
| 块被替换/删除（块 ID 变化） | `LOCATOR_FAILED` | ✅ |
| 块重排（orderInSection 变化） | `LOCATOR_FAILED` | ✅ |
| 字符范围越界 | `LOCATOR_FAILED` | ✅ |
| 同块文本改写导致引文不符 | `QUOTE_MISMATCH` | ✅ |
| 显式重绑后在新版本上校验 | `VALID` | ✅ |

- 共享测试：**24/24 通过**（原 16 + 引用 8）；
- smoke 运行：**14/14 PASS**（原 10 + 引用 4）；
- 构建：`BUILD SUCCESSFUL in 11s`（增量）。
- 调试记录：首次运行有 2 个用例失败，原因是测试向量里字符偏移落在「引用定位」之后（文本已改写但引文仍匹配）以及重绑样本偏移算错；修正测试向量后全部通过，产品代码未改。

## 5 判定（对照 W-E06 通过证据）

| 通过证据 | 是否取得 | 说明 |
|---|---|---|
| 双锚点回跳（数据层） | ✅ | 结构锚点 + 字符锚点同时校验，任一不符即失败 |
| 跨版本明确 stale | ✅ | `STALE_VERSION` / `STALE_PARSER` 可区分 |
| 无静默迁移 | ✅ | `verify` 不产生新引用；只有 `rebindExplicitly` 能更新 |
| 桌面高亮回跳 | ❌ | 需要 UI 壳（Compose/WebView）与渲染坐标 |
| 真实解析器升级 | ❌ | 本轮用 `parserVersion` 模拟，未真正替换解析器重跑 |

## 6 跳过项与原因

| 项 | 原因 | 归属 |
|---|---|---|
| 高亮回跳与选区 | 需桌面 UI 壳与渲染坐标 | W-E02 后续 / W-E06 的 UI 部分 |
| 真实解析器升级（PDFBox 换版本重跑） | 需固定两版解析器与金标；单端可做但需额外样本 | 后续实验 |
| 插页/删页的真实 PDF 样本 | 本轮用合成快照覆盖语义；真实样本可复用 `SELF-002/003` | 后续 |
| Android 同源 stale 对照 | 需 C 配合 | C/A |

## 7 结论

- **原始事实（F）**：8 个引用用例全部按预期返回 5 种状态；版本变化即使文本相同也判 stale；重排与越界均判定位失败；显式重绑后校验通过。
- **项目组解释（I）**：D-25 A 的「双锚点 + 默认 stale + 显式迁移」可以在共享核心中实现，且不需要平台依赖；UI 只需消费状态并展示。
- **技术/需求结论（C）**：W-E06 的数据层规则成立；ADR-025 仍为 Proposed，缺真实高亮回跳与双端对照。

## 8 回填动作

1. 证据索引 §3：W-E06 状态改为「部分完成（规则层）」。
2. 调研文档 §9.2 W-E06、§9.3 D-25 映射更新。
3. 不改变 D-25 A 决策状态。
