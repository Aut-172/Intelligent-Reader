# TECH-WIN-001-R01 Windows 平台事实探针结果

- 编号：`TECH-WIN-001-R01`
- 日期：2026-11-17 04:02（UTC+08:00）
- 负责人：B（Windows 端架构调研与比较负责人）
- 脚本：`experiments/windows-env-probe/probe_windows_env.ps1`
- 原始 JSON：本次运行因代理沙箱为低完整性进程、无法写工作区，原始 JSON 落在平台临时目录 `%TEMP%\reader-win-probe\TECH-WIN-001-R01.json`；脚本在可写环境下默认写 `experiments/windows-env-probe/results/TECH-WIN-001-R01.json`。下表数值与 JSON 内 `checks` 逐项一致。
- 环境：主机 `HYC-1`，未提权（`isElevated=false`），探测进程完整性级别为 Low（这是代理沙箱造成，不是普通桌面应用的正常运行上下文）。
- 边界：只测平台事实；不下载依赖、不调用云端、不改注册表与 ACL。单机单次，不外推为 Windows 11 全版本结论。

## 1 TECH-WIN-001 运行环境矩阵

| 检查 | 观测值（原始事实） |
|---|---|
| WIN-001-01 系统版本 | `ProductName=Windows 10 Home China`；`DisplayVersion=26H2`；`CurrentBuild=26300.9974`；`EditionID=CoreCountrySpecific`；`isWin11Build=true`（Build ≥ 22000） |
| WIN-001-02 架构与宿主机 | `AMD64`；Intel64 Family 6 Model 183；PowerShell 5.1.26100.9818；64 位进程；进程完整性级别 Low |
| WIN-001-03 长路径 | `LongPathsEnabled=1`（已开启）；应用仍需清单声明 `longPathAware` |
| WIN-001-04 WebView2 | 已安装，`157.0.4341.46`（HKLM WOW6432Node EdgeUpdate 客户端项） |
| WIN-001-05 桌面运行时 | .NET Framework `Release=534296`（4.8 及以上）；`java.exe` 存在（Oracle `javapath` 启动器，版本未确认）；`JAVA_HOME` 为空；`dotnet` CLI 不存在；`node` 不在 PATH |
| WIN-001-06 凭据存储入口 | `cmdkey.exe` 存在；`%APPDATA%\Microsoft\Credentials` 与 `%APPDATA%\Microsoft\Protect`（DPAPI 主密钥目录）均存在 |
| WIN-001-07 用户与数据目录 | 用户 `HYC-1\HYC-1`；未提权；`%LOCALAPPDATA%=C:\Users\HYC-1\AppData\Local`；`%ProgramFiles%=C:\Program Files` |

## 2 TECH-WIN-003 显示与缩放

| 检查 | 观测值 |
|---|---|
| WIN-003-01 显示器 | 1 台，`\\.\DISPLAY1`，逻辑分辨率 `1707 × 1067`，主显示器 |
| 缩放 | `AppliedDPI=144` → 150%（对应物理分辨率约 2560×1600 的 150% 缩放） |
| `LogPixels` | 未设置（该值不是可靠的缩放来源，`AppliedDPI` 可用） |

## 3 TECH-WIN-002 文件路径身份与访问语义

| 检查 | 观测值 | 异常类型 |
|---|---|---|
| WIN-002-01 写入/读取/哈希 | 写入成功；SHA-256 记录；回读内容 `windows-probe-v1` | — |
| WIN-002-02 移动文件 | 原路径 `oldPathExists=false`、`oldPathReadable=false`；新路径可读；内容未变 | `ItemNotFoundException`（PowerShell 层） |
| WIN-002-03 按旧路径重新打开 | 失败（`failed=true`） | 外层 `MethodInvocationException`，内层 `System.IO.FileNotFoundException` |
| WIN-002-04 只读属性 | 覆盖写被阻断；文件哈希不变；内容仍为 v1 | `UnauthorizedAccessException` |
| WIN-002-05 共享模式占用 | 第一句柄以 `FileShare.None` 打开后，第二句柄读写打开被阻断 | 外层 `MethodInvocationException`，内层 `System.IO.IOException` |
| WIN-002-06 覆盖写 | 覆盖成功，内容变为 v3；同目录仅 1 个文件（无自动旧版本备份） | — |
| WIN-002-07 删除 | 路径不复存在；重新打开失败 | 打开抛异常 |

## 4 原始事实（F）

1. 本机为 Build 26300 的 Windows 11 分支系统，但注册表 `ProductName` 仍写作 `Windows 10 Home China`。`ProductName` 字符串不能用于判断 Windows 11；应使用 `CurrentBuild ≥ 22000` 或 `DisplayVersion`。
2. 长路径开关在本机已开启，但 Windows 官方要求应用清单同时声明 `longPathAware`，否则进程仍按 MAX_PATH=260 处理。
3. WebView2 运行时已由系统预装（157.0.4341.46），说明 Tauri 类路线的运行时可依赖系统预装，而不必自带完整浏览器内核。
4. 机器上没有项目可用的 JDK/Maven/Gradle 工具链（`java.exe` 只是 Oracle 的启动器，`JAVA_HOME` 为空，`dotnet`/`node` 不在 PATH），因此本机无法构建或打包任何一条桌面路线。
5. 机器为单显示器、150% 缩放；未观察到 100% 与多显示器混合 DPI 场景。
6. Windows 端的「文件授权」是路径 + 账户 ACL 语义：文件被移动或删除后，旧路径立即失效并抛 `FileNotFoundException`；不存在类似 Android 持久 URI 授权的可转移凭据。
7. 只读属性会在写入时抛 `UnauthorizedAccessException` 且不改内容；被其他进程以独占共享模式打开时会抛 `IOException`；普通覆盖写不留旧版本。

## 5 项目组解释（I）

1. **文档身份必须以内容与元数据为准，路径只能当线索。** 路径失效是常态而不是异常；`DocumentIdentity` 需要「源哈希 + 规范化元数据 +（可选）卷序列号/文件 ID」组合，并在路径变化时提供重新绑定入口（D-16 输入）。
2. **「重新授权」在 Windows 上没有平台原语。** 应用只能在用户再次选择文件后重新校验身份；不能像 SAF 那样 `takePersistableUriPermission`。因此 D-05 的「两端分别重新授权」在 Windows 侧等价于「重新选择 + 身份比对」。
3. **长任务恢复必须自建。** Windows 桌面应用没有 WorkManager 类调度器；进程退出、注销或崩溃后任务即消失，恢复只能靠本地持久任务表在前台重启时续跑。
4. **打包工具链是当前的硬缺口。** 三条候选路线的 Windows 制品都依赖本机不存在的工具链（JDK/jpackage 或 Rust/Node/WiX 或 Qt/Qt Installer），因此发布验证全部登记为【待测】。
5. **150% 缩放与单屏是弱覆盖。** 本机不能证明多显示器、混合 DPI、窄窗口和高分屏下的选区/bbox 命中，需要专门的桌面壳实验。

## 6 技术/需求结论（C）

1. Windows 端不存在持久文件授权；文件身份与重新绑定属于必须自建的职责域（对应 FR-01、BR11、NFR-13、AC-01/AC-02）。
2. 导出覆盖写会直接替换目标文件且不留旧版本，导出流程必须自行实现「覆盖确认 + 写入结果 + 失败不静默覆盖」（对应 FR-09、AC-20）。
3. 本机的 WebView2 可用，可作为 Tauri 路线运行时的可实施性正面证据；但不能外推到受管控的企业机器（可改用固定版本运行时，包体增加约 100–250 MB，见官方资料）。
4. 本机缺少桌面构建工具链，`B-D05` 的安装、升级、卸载验证无法在本轮完成，只能给出可复现计划并登记为【待测】。

## 7 与既有实验的关系

本探针只补 Windows **平台/宿主层**事实，不替代也不覆盖 `TECH-DOC-*`、`TECH-OCR-*`、`TECH-RET-*`、`TECH-QA-*`、`TECH-ARCH-*`；后者是库层/进程内能力证据。两类证据一起使用时，仍不能证明「Windows 桌面壳 + 打包 + 安装」链路可行。
