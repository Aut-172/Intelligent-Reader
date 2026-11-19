# W-E06 解析器升级后的 stale 验证

- 实验编号：W-E06 补充（结果编号 `TECH-WIN-027-R01`，承接 `TECH-WIN-006-R01`）
- 执行人：B
- 日期：2026-11-18
- 状态：**部分通过**——规则层通过；真实 PDFBox 版本升级与 UI 回跳缺
- 环境：JDK 21、Kotlin 2.1.20

## 1 目的

验证解析器版本变化时，引用是否会标记为 stale，而不是静默迁移或错误落点。

## 2 方法

既有测试 `shared/src/commonTest/.../citation/CitationVerifierTest.kt` 中的 `parserUpgradeMarksStale`：

- 同一文档版本，`parserVersion` 从 `pdfbox-3.0.5` 改为 `pdfbox-3.1.0`；
- 断言引用状态为 `CitationStatus.STALE_PARSER`。

## 3 结果

| 项 | 结果 |
|---|---|
| 测试 | `parserUpgradeMarksStale` |
| 期望 | `STALE_PARSER` |
| 实际 | 通过（见全量测试 42/42） |
| 覆盖的其他 stale 场景 | 新版本、重排、引用不匹配（`TECH-WIN-006`） |

## 4 判定

| 通过证据 | 结果 |
|---|---|
| 解析器版本变化 → stale | ✅（规则层） |
| 无静默迁移 | ✅（规则层） |
| 真实 PDFBox 3.0.5 → 3.1.x 升级重解析 | ❌ 未做（同版本库，仅模拟 parserVersion） |
| UI 回跳 | ❌ 依赖桌面壳 |

## 5 结论

- **原始事实（F）**：`parserVersion` 变化时引用状态为 `STALE_PARSER`，测试通过。
- **项目组解释（I）**：版本字段参与 stale 判定，规则层满足“解析器升级不静默迁移”；真实库升级后的坐标漂移仍需单独验证。
- **技术/需求结论（C）**：W-E06 的解析器升级规则成立；真实库升级/UI 回跳仍缺，D-25 保持 Proposed。
