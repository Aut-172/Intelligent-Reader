# W-E 测试报告与环境配置模板

- 版本：V1.0
- 日期：2026-11-17
- 适用：B 在 Windows 端执行 W-E01 至 W-E15 时使用
- 依据：`31-Windows端架构调研待测清单-V1.0`；`23-ADR-D11-D35-V1.3`；项目负责人 2026-11-17 授权（仅架构调研用途，测试完成后须把工具链配置写入测试报告）
- 用法：每次实验复制本模板为 `TECH-WIN-<W-E编号>-R0x.md`，放在 `experiments/windows-kmp-minimal/results/` 或对应实验目录的 `results/`

## 1 实验元数据

| 字段 | 内容 |
|---|---|
| 实验编号 | W-E0x（对应 `31` 清单） |
| 关联决策 | D-xx（选择） |
| 关联 ADR | ADR-0xx |
| 目的 | 一句话说明要验证什么 |
| 负责人 / 执行人 | B |
| 日期 | YYYY-MM-DD |
| 状态 | 待执行 / 已执行 / 部分完成 / 失败 |

## 2 环境配置快照（必填）

> 授权要求：每次安装或变更工具链后，必须在本节记录版本、路径、安装方式与时间；本文件就是「测试报告」的配置证据。

| 项 | 值 | 记录方式 |
|---|---|---|
| 主机名 | HYC-1 | `hostname` |
| Windows 版本/构建 | Build 26300.9974（DisplayVersion 26H2） | `winver` 或 `TECH-WIN-001` |
| CPU 架构 | x64 | `$env:PROCESSOR_ARCHITECTURE` |
| 显示器/缩放 | 单屏 150%（`AppliedDPI=144`） | `TECH-WIN-001` |
| 内存/磁盘 | 待填 | 任务管理器 / `Get-CimInstance`（若可用） |
| JDK | 21.0.12.1（Oracle，`jdk.jpackage` 模块存在） | `java -version`、`jpackage --version` |
| JDK 路径 | `D:\#_Dev\Java_compiler` | `where.exe java` |
| JAVA_HOME | `D:\#_Dev\Java_compiler`（用户级） | `echo $env:JAVA_HOME` |
| Gradle | 待填（建议 8.10.2+） | `gradle -v` |
| Gradle 路径 | 待填（建议 `D:\#_Dev\gradle-8.x`） | `where.exe gradle` |
| GRADLE_USER_HOME | 待填（如需放 D 盘） | `echo $env:GRADLE_USER_HOME` |
| Kotlin/KMP 插件版本 | 待填（`gradle/libs.versions.toml`） | 构建日志 |
| WiX（仅 MSI） | 未安装 / 3.14.x | `candle.exe -?` |
| 干净虚机 | 未准备 / 已快照（名称） | 虚机管理器 |
| 代理/弱网工具 | 未安装 / Fiddler / Clumsy | 版本与配置 |
| 网络条件 | 直连 / 代理 / 限速 | 测速或代理日志 |

补充命令（复制执行并粘贴输出）：

```powershell
hostname
java -version
jpackage --version
gradle -v
echo $env:JAVA_HOME
where.exe java
where.exe gradle
```

## 3 配置变更记录

| 时间 | 变更 | 原因 | 回滚方式 | 验证 |
|---|---|---|---|---|
| 2026-11-17 | 安装 JDK 21.0.12.1 至 `D:\#_Dev\Java_compiler`；设置用户级 `JAVA_HOME` | W-E01/W-E13 前置 | 删除目录并移除 `JAVA_HOME` | `jpackage --version` 通过 |
| YYYY-MM-DD | 安装 Gradle 8.x 至 `D:\#_Dev\gradle-8.x`；追加用户 PATH | W-E01 前置 | 删除目录并移除 PATH 项 | `gradle -v` 通过 |

## 4 实验步骤

| # | 命令/操作 | 预期 | 实际 | 耗时 |
|---|---|---|---|---|
| 1 |  |  |  |  |
| 2 |  |  |  |  |

## 5 结果与判定

| 判定项（来自 `31` 清单） | 阈值/标准 | 实测 | 结论 |
|---|---|---|---|
|  |  |  | 通过 / 不通过 / 待定 |

## 6 证据清单

| 类型 | 路径/链接 | 说明 |
|---|---|---|
| 构建日志 |  |  |
| 测试输出 |  |  |
| 依赖图 |  |  |
| 制品哈希 |  |  |
| 截图 |  |  |

## 7 边界与未覆盖

- 未验证内容：
- 环境限制（如单屏、无第二用户、无干净虚机）：
- 是否可外推：

## 8 结论

- 原始事实（F）：
- 项目组解释（I）：
- 技术/需求结论（C）：
- 对 ADR 状态的影响（保持 Proposed / 满足关闭门槛 / 触发重新打开）：

## 9 回填动作

1. 在 `Windows端验证计划与证据索引.md` §3 更新该 W-E 的状态与证据链接。
2. 在 `Windows端架构调研与比较.md` 对应【待测】处改为【W实测】并附编号。
3. 若触发 ADR 重新打开条件，按 `23-ADR` 记录，不自行改结论。
