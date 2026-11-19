# W-E10 日志/崩溃转储秘密与路径扫描

- 实验编号：W-E10 补充（结果编号 `TECH-WIN-025-R01`，承接 `TECH-WIN-008/013-R01`）
- 执行人：B
- 日期：2026-11-18
- 状态：**未通过（完整路径维度）**——无秘密泄漏，但崩溃转储含绝对用户路径
- 环境：本机 Windows 11；扫描用户级目录与 WER 归档

## 1 目的

验证普通数据文件、日志、崩溃转储中是否出现密钥/令牌/密码/正文/完整路径，为 W-E10 的“秘密、正文和完整路径扫描通过”提供证据。

## 2 方法

`experiments/windows-env-probe/probe_log_scan.ps1` 扫描：

- `%LOCALAPPDATA%\ReaderWinProbe`
- `%LOCALAPPDATA%\CrashDumps`
- `C:\ProgramData\Microsoft\Windows\WER\ReportArchive` / `ReportQueue`
- `%TEMP%` 中名称匹配 `reader|winprobe|we1x` 的文件

正则：OpenAI key、`Bearer`、`api_key`、`password`、私钥头、`C:\Users\<name>` 路径。

## 3 结果

| 项 | 结果 |
|---|---|
| 扫描文件 | 14 |
| 命中文件 | 8 |
| 命中模式 | **仅 `user_path`**，41 次 |
| 密钥/令牌/密码/私钥 | **0** |
| 正文 | 0（仅二进制转储中的路径字符串） |

命中集中在 Java 崩溃转储（`java.exe.*.dmp`）、`javac.exe.*.dmp`，内容为 `C:\Users\HYC-1\...` 绝对路径。

## 4 判定

| 通过证据 | 结果 |
|---|---|
| 凭据/秘密不进入普通文件 | ✅（本次样本） |
| 正文不进入日志/转储 | ✅（本次样本） |
| **完整路径不进入日志/转储** | ❌（8 个转储含 41 处用户路径） |
| 跨用户 CredMan / 重装 | ❌ 需第二用户（A） |

## 5 结论

- **原始事实（F）**：14 个文件中未发现密钥/令牌/密码/私钥；但 8 个 Java 崩溃转储含 41 处 `C:\Users\HYC-1\...` 绝对路径。
- **项目组解释（I）**：崩溃转储天然包含进程内存中的路径字符串；若诊断包或日志归档包含 WER/dump，会泄露用户名与目录结构。必须在打包时排除 dump/WER，或做路径脱敏。
- **技术/需求结论（C）**：W-E10 的“完整路径扫描”未通过；诊断包必须显式排除崩溃转储并做路径规范化。跨用户 CredMan 仍待 A。
