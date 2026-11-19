# W-E10 补充报告：Windows Credential Manager 写入/读取/删除

- 实验编号：W-E10 补充（结果编号 `TECH-WIN-013-R01`，与 `TECH-WIN-008-R01` 同属 W-E10）
- 关联决策：D-31 A（平台安全凭据）、D-18 A（令牌与设备会话）
- 执行人：B
- 日期：2026-11-18
- 状态：**部分完成**——Credential Manager 写入/读取/删除与残留检查通过；跨用户、日志扫描、重装未测

## 1 目的

补充 DPAPI 之外的凭据通道：验证 Windows Credential Manager 能否安全写入、读回、删除通用凭据，并确认删除后无残留。

## 2 环境与方法

| 项 | 值 |
|---|---|
| 主机/用户 | HYC-1 / `HYC-1`（当前用户） |
| 脚本 | `experiments/windows-env-probe/probe_credman.ps1` |
| API | `advapi32` 的 `CredWriteW` / `CredReadW` / `CredDeleteW`（P/Invoke） |
| 凭据类型 | `CRED_TYPE_GENERIC`，`CRED_PERSIST_SESSION`（会话级，测试后立即删除） |
| 残留检查 | `cmdkey /list:ReaderWinProbe/we10` |

## 3 结果

| 检查 | 结果 |
|---|---|
| 写入（CredWrite） | ✅ `writeOk=true` |
| 读取（CredRead） | ✅ `readOk=true` |
| 读回内容与写入一致 | ✅ `secretMatched=true` |
| 删除（CredDelete） | ✅ `deleteOk=true` |
| 删除后再次读取 | ❌ `readAfterDeleteOk=false`（预期） |
| `cmdkey /list` 残留 | `* NONE *` |
| 错误列表 | 空 |

## 4 判定

| 通过证据 | 是否取得 | 说明 |
|---|---|---|
| 凭据写入平台存储 | ✅ | 通用凭据写入成功 |
| 读回一致 | ✅ | 明文仅存在于进程内存与系统凭据库 |
| 删除与残留 | ✅ | 删除后不可读，`cmdkey` 无条目 |
| 跨用户不可读 | ❌ | 由 `TECH-WIN-008-R01` 的 DPAPI 跨用户覆盖；Credential Manager 条目未做跨用户验证 |
| 日志/转储扫描 | ❌ | 需客户端日志与 WER |
| 重装/换机 | ❌ | 需环境重建 |

## 5 结论

- **原始事实（F）**：Credential Manager 写入/读回/删除均成功，删除后无残留，`cmdkey` 无条目。
- **项目组解释（I）**：D-31 A 的「平台安全凭据」在 Windows 上有两条可行通道（DPAPI 与 Credential Manager）；应用应封装为同一读写契约，由平台适配层选择。
- **技术/需求结论（C）**：W-E10 的凭据写入通道成立；ADR-031 仍 Proposed，缺跨用户 Credential Manager、日志扫描与重装证据。
