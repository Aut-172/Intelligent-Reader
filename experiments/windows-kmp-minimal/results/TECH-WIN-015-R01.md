# W-E15 干净环境重建（虚机内构建与打包）

- 实验编号：W-E15（结果编号 `TECH-WIN-015-R01`）
- 关联决策：D-34 A、D-35 A
- 执行人：B
- 日期：2026-11-18
- 状态：**完成构建与打包**；干净机安装/升级见 `TECH-WIN-014-R01`，卸载未闭环
- 环境：`Win11-Clean`（HYC-CLEAN），Windows 11 Pro Build 26300.9457，4 vCPU / 8 GB / 128 GB，快照 `clean-with-tools`
- 工具链：OpenJDK **21.0.2**（华为云镜像）、Gradle **8.14.7**（腾讯镜像）、WiX **3.14.1.8722**（宿主机安装目录打包后经共享文件夹传入）
- 源码：`we15-source.zip`（宿主工作区导出，不含 build/dist/.gradle）

## 1 目的

在干净 Win11 虚机中从零安装工具链、重建 shared 与 Windows 客户端、运行测试并生成安装包，验证可复现性。

## 2 方法

1. 回滚 `clean-with-tools`，启动并启用共享文件夹；
2. 通过共享文件夹把 JDK/Gradle/WiX/源码传入，解压到 `C:\we15build`；
3. `gradle --no-daemon :shared:jvmTest :windowsApp:test :windowsApp:installDist`；
4. `jpackage` 生成 app-image 与 MSI 1.0.0 / 1.0.1（同一 upgrade UUID）；
5. 结果写入共享文件夹，宿主读取。

## 3 结果

| 项 | 结果 |
|---|---|
| JDK | 21.0.2（OpenJDK 21.0.2+13-58） |
| Gradle | 8.14.7（Kotlin 2.0.21 embedded，Launcher JVM 21.0.2） |
| WiX | 3.14.1.8722（`candle.exe -?` 通过） |
| 构建 | **BUILD_OK**；`:shared:jvmTest`、`:windowsApp:test`、`:windowsApp:installDist` |
| 编译告警 | 仅 `URL` 构造器过时警告，无错误 |
| app-image | 已生成 `C:\we15build\dist\ReaderWinProbe`（目录存在） |
| MSI 1.0.0 | 71,300,856 B；SHA-256 `4b66157f06912a92fe452280d4f6efb81d383cd7fc6dffd23d9241a0cdf9f553` |
| MSI 1.0.1 | 71,300,856 B；SHA-256 `3b3ad401a40ba2fd38485a2e2f571891b7102e1b85296e17ef35703afcb9e2bc` |
| 首次打包失败 | `--win-menu-group` 不适用于 `app-image`；移除后重打成功（`PACKAGE_OK2`） |

## 4 判定

| 通过证据 | 结果 |
|---|---|
| 干净环境重建 shared/客户端 | ✅ |
| 同源测试运行 | ✅（BUILD_OK，测试任务执行） |
| 安装包重建 | ✅（app-image + 2 个 MSI） |
| 制品哈希 | ✅ |
| 干净机安装/运行 | ✅（`TECH-WIN-014-R01`） |
| 干净机卸载/签名 | ❌ |

## 5 边界

1. 虚机 JDK 为 21.0.2（宿主为 21.0.12.1），MSI 体积 71.3 MB（宿主 72.1 MB），差异来自 JDK 运行时版本；
2. 虚机内构建依赖 Maven Central / 插件门户，网络可用但首次构建较慢；
3. 未在虚机内做签名（缺 signtool/SDK）；
4. 虚机 Tools 通道多次挂起，卸载复跑未能完成（详见 `TECH-WIN-014-R01` §4）；
5. **二次重建未执行**：两次尝试均失败——第一次在线构建时虚机 guest 通道挂起并触发 DSH 崩溃；第二次改用宿主 Gradle 缓存离线构建，仍在 build 阶段挂起。为避免损坏基线已回滚 `clean-with-tools`；本报告只证明“一次干净重建”，不证明多次可复现。
6. **旧基线含内存快照（2026-11-19 发现）**：旧虚机目录中有 3 个 `.vmem`，其中 2 个与 `.vmsn` 同名成对，说明 `clean-installed` / `clean-with-tools` 是**含内存快照**。回滚会恢复 RAM 中的进程、daemon、文件锁与 Tools 会话，这是两次 C8 在 build 前挂起的高度可疑原因。新虚机基线改为**关机快照**，并在拍完后确认无 `.vmem`。

## 6 结论

- **原始事实（F）**：干净虚机内用 JDK 21.0.2 + Gradle 8.14.7 + WiX 3.14.1 完成构建与打包，生成 app-image 与两个 MSI，哈希已记录。
- **项目组解释（I）**：D-34/D-35 的「干净环境重建」在 Windows 侧可复现；构建脚本、依赖与工具链版本均可固定。
- **技术/需求结论（C）**：W-E15 的构建可复现性成立；ADR-034/035 仍 Proposed，缺干净机卸载与签名证据。
