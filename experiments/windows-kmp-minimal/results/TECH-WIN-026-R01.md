# W-E11 故障注入：解析/SSE/同步

- 实验编号：W-E11 补充（结果编号 `TECH-WIN-026-R01`，承接 `TECH-WIN-009-R01`、`TECH-WIN-012-R01`、`TECH-WIN-007-R01`）
- 执行人：B
- 日期：2026-11-18
- 状态：**部分通过**——解析/SSE/同步故障可捕获；模型故障与 UI 导出缺
- 环境：本机 Windows 11，JDK 21，PDFBox 3.0.5

## 1 目的

验证解析、SSE、同步三类故障是否以可捕获、可关联的方式暴露，避免静默失败或崩溃。

## 2 方法

| 故障 | 方法 | 证据 |
|---|---|---|
| 解析 | 生成损坏 PDF 与空 PDF，调用 `Loader.loadPDF` | `docProbe/results/we11-faults.tsv` |
| SSE 断流/超时/取消 | 既有 `SseClientTest` 6 项 | `TECH-WIN-012-R01` |
| 同步/队列失败 | 既有 `SqliteTaskStoreTest` 7 项 | `TECH-WIN-007-R01` |
| 诊断包 | 既有白名单/泄漏扫描 | `TECH-WIN-009-R01` |

## 3 结果

| 故障 | 结果 |
|---|---|
| 损坏 PDF | `CAUGHT IOException: Missing root object specification in trailer.` |
| 空 PDF | `CAUGHT IOException: End-of-File, expected line at offset 0` |
| SSE 6 项 | 全部通过（断流、超时、取消、轮询回退） |
| 队列 7 项 | 全部通过（失败/重试/取消/恢复） |
| 诊断包 | 白名单与泄漏扫描通过 |

## 4 判定

| 通过证据 | 结果 |
|---|---|
| 解析故障可捕获 | ✅ |
| SSE 故障可恢复 | ✅ |
| 同步/队列故障可恢复 | ✅ |
| 诊断包可关联 | ✅（规则层） |
| 模型故障注入 | ❌ 依赖 W-E04 |
| UI 导出/预览 | ❌ 依赖桌面壳 |

## 5 结论

- **原始事实（F）**：损坏/空 PDF 均抛出可捕获的 `IOException`，未导致进程崩溃；SSE 6/6、队列 7/7、诊断包扫描通过。
- **项目组解释（I）**：三类故障均有明确的失败信号，可在应用层映射为降级与诊断事件；模型故障与 UI 导出是剩余缺口。
- **技术/需求结论（C）**：W-E11 的解析/SSE/同步故障维度成立；模型与 UI 维度待补，D-32 保持 Proposed。
