# W-E13 测试报告：Windows 打包、安装与卸载

- 实验编号：W-E13（结果编号 `TECH-WIN-011-R01`）
- 关联决策：D-08（安装包或解压运行演示包）、D-34 A（双端最小验证）、D-35 A（条件部署）
- 关联 ADR：ADR-034、ADR-035
- 执行人：B
- 日期：2026-11-18
- 状态：**部分完成**——app-image 与 MSI 均构建成功，本机 MSI 安装/运行/卸载通过；升级覆盖、签名、干净虚机未验证
- 跳过说明：干净环境（W-E15）与签名不在本次范围，按负责人指示在报告中说明。

## 1 目的

验证 Windows 交付制品的可构建性与本机安装生命周期：`jpackage` 生成 `app-image`（解压运行）与 `msi`（安装包），并检查安装路径、启动、卸载与残留。

## 2 环境配置快照

| 项 | 值 |
|---|---|
| 主机/系统 | HYC-1 / Build 26300.9974，x64 |
| JDK | Oracle 21.0.12.1（`D:\#_Dev\Java_compiler`，`jpackage` 21.0.12.1） |
| WiX | **WiX Toolset 3.14.1.8722**（`C:\Program Files (x86)\WiX Toolset v3.14\bin`） |
| Gradle | 8.14.7（`GRADLE_USER_HOME=D:\#_Dev\#_CACHES\gradle`） |
| 应用 | `windowsApp`（KMP 共享核心 + 17 项 smoke） |
| 打包命令 | `jpackage --type app-image` / `--type msi --win-per-user-install --win-menu --win-shortcut --win-upgrade-uuid …` |
| 网络 | 不需要（依赖已缓存） |

配置变更：安装 WiX 3.14；生成 `dist/` 制品；测试后已卸载 MSI。回滚：删除 `dist/`；`winget uninstall WiXToolset.WiXToolset`（如需）。

## 3 结果

### 3.1 制品

| 制品 | 大小 | 文件数 | 构建耗时 | SHA-256 |
|---|---:|---:|---:|---|
| `dist/ReaderWinProbe/`（app-image） | **163.79 MB** | 294 | 4.5 s | exe：`2C5F6C8B…27CE` |
| `dist/ReaderWinProbe-1.0.0.msi` | **68.8 MB**（72,147,500 B） | — | 25.4 s | `AB2623A1…EAF1` |

app-image 含完整 JRE，因此体积显著大于 MSI 压缩后的安装包。

### 3.2 运行与安装生命周期

| 步骤 | 结果 |
|---|---|
| app-image 直接运行 | **17/17 PASS**，冷启动 **0.37 s** |
| MSI 每用户安装（`msiexec /i /qn`） | 退出码 **0** |
| 安装目录 | `C:\Users\HYC-1\AppData\Local\ReaderWinProbe` |
| 已安装程序运行 | **17/17 PASS**，冷启动 **0.59 s** |
| 开始菜单快捷方式 | `%APPDATA%\…\Start Menu\Programs\未知\ReaderWinProbe.lnk` |
| 卸载（`msiexec /x /qn`） | 退出码 **0** |
| 卸载残留 | 安装目录已删除、快捷方式已删除、未发现残留卸载注册项 |

### 3.3 观察到的两个问题

1. **开始菜单分组为「未知」**：未传 `--win-menu-group`，jpackage 使用了默认空分组。正式打包应显式指定分组名（如 `智能阅读器`），否则快捷方式落在异常目录。
2. **每用户安装仍写入 HKLM 卸载项**：安装路径在 `%LOCALAPPDATA%`，但卸载注册项出现在 `HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\{631441D6-FED4-3ECD-BBDB-5F5A1DD86ABB}`。说明「每用户路径」不等于「每用户注册」，无管理员权限的安装/卸载行为需在 W-E15 干净环境复核。

## 4 判定（对照 W-E13 通过证据）

| 通过证据 | 是否取得 | 说明 |
|---|---|---|
| 安装包信息 | ✅ | MSI 版本 1.0.0、厂商、产品码 |
| 安装日志 | ✅ | `msi-install.log`、`msi-uninstall.log` |
| 启动 | ✅ | app-image 与已安装版均 17/17 PASS |
| 卸载与残留 | ✅（本机） | 目录/快捷方式已清理；注册项未见残留 |
| 升级/覆盖安装 | ❌ | 未做版本递增覆盖 |
| 签名与 SmartScreen | ❌ | 未签名 |
| 干净 Windows 11 | ❌ | 本机为开发环境（W-E15） |
| 用户数据生命周期 | ❌ | 应用当前不写用户数据，未验证数据保留策略 |

## 5 结论

- **原始事实（F）**：WiX 3.14 + jpackage 21 可生成 app-image（164 MB）与 MSI（69 MB）；MSI 每用户安装/运行/卸载均成功，卸载后无目录/快捷方式/注册项残留；app-image 冷启动 0.37 s、安装版 0.59 s。
- **项目组解释（I）**：D-08 的两种 Windows 交付形式均可行；`app-image` 适合课程「解压运行演示包」，MSI 提供标准安装体验但需要 WiX 与签名策略。
- **技术/需求结论（C）**：W-E13 的本机安装生命周期成立；ADR-034/035 仍 Proposed，缺升级覆盖、签名、干净环境与数据生命周期证据。

## 6 回填动作

1. 证据索引 §3：W-E13 状态改为「部分完成（本机安装/卸载）」。
2. 调研文档 §8.5/§9.2/§9.3 更新 W-E13 与 D-34/D-35 映射。
3. 交付提示：正式打包需补 `--win-menu-group`，并决定是否签名。
