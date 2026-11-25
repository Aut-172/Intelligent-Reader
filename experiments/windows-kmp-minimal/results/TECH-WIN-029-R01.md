# W-E15 二次干净重建（新虚机）

- 实验编号：W-E15 补充（结果编号 `TECH-WIN-029-R01`，承接 `TECH-WIN-015-R01`）
- 执行人：B
- 日期：2026-11-19
- 状态：**完成**——新虚机内二次构建、打包、关键测试均通过
- 环境：新建 `Win11-Clean` 虚机（同硬件配置、同 ISO、同 `#_share`），快照 `clean-installed` / `clean-with-tools`；工具链由 `we15-build.cmd` 从共享目录解压

## 1 目的

在全新虚机上复现 shared/Windows 客户端/安装包的构建，补齐 `TECH-WIN-015-R01` 的“二次重建”缺口。

## 2 方法

1. 新建虚机，OS 安装后打 `clean-installed`，装 Tools 后打 `clean-with-tools`；
2. 共享目录 `#_share` 保持原有 JDK/Gradle/WiX/源码压缩包；
3. 运行 `we15-build.cmd`（在线取依赖）：
   - 解压 JDK 21.0.2、Gradle 8.14.7、WiX 3.14.1、源码；
   - `:shared:jvmTest :windowsApp:test :windowsApp:installDist`；
   - `jpackage` app-image + MSI 1.0.0/1.0.1。
4. 首次打包因 `--win-menu-group` 对 app-image 非法而失败；用 `we15-package2.cmd` 只重打包（不重编译）后成功；
5. 运行 app-image 做 17 项 smoke。

## 3 结果

| 项 | 结果 |
|---|---|
| 构建 | **BUILD_OK**（`BUILD SUCCESSFUL in 4m 29s`） |
| 测试 | shared + windowsApp 测试通过（构建日志） |
| 打包 | **PACKAGE_OK2**（`we15-package2.log` 为空） |
| app-image | `C:\we15build\dist\ReaderWinProbe` |
| MSI 1.0.0 | 71,300,856 B；SHA-256 `5fe2461c0fd54c4ab55b463227e6e4c50b5570fac0facfada09b54c554cb13d7` |
| MSI 1.0.1 | 71,300,856 B；SHA-256 `cf1a8e02ab9df79a53e85c15e739f1c106afbf2a72b293e6f85f3c08d5cfdd83` |
| smoke | **17/17 PASS**（`c8-smoke.txt`） |

### 与首次构建对比

| 项 | 第一次 | 第二次 |
|---|---|---|
| MSI 大小 | 71,300,856 B | 71,300,856 B |
| 1.0.0 SHA-256 | `4b66157f…f553` | `5fe2461c…13d7` |
| 1.0.1 SHA-256 | `3b3ad401…e2bc` | `cf1a8e02…dd83` |

两次**大小完全一致**，但字节哈希不同。原因：新虚机磁盘卷序列号变化（`16A9-3D35` → `22F5-C91F`），WiX/jpackage 会把时间戳、卷序列号等元数据写入 MSI；因此**哈希不是可复现判据**。

## 4 判定

| 通过证据 | 结果 |
|---|---|
| 干净环境重建 shared/客户端 | ✅ |
| 测试通过 | ✅ |
| 安装包重建 | ✅（app-image + 2 MSI） |
| 关键 smoke | ✅ 17/17 |
| 两次制品大小/版本一致 | ✅ |
| 字节级哈希一致 | ❌（元数据导致，属正常） |
| 签名/SmartScreen | ❌ 未做 |

## 5 边界

1. 只做了两次重建，不是 N 次统计；
2. 第二次构建使用在线依赖（首次构建亦同），未固定依赖锁文件；
3. 未记录新虚机 VMware Tools 版本；
4. 未做签名与 SmartScreen；
5. 安装/升级/卸载的干净机证据见 `TECH-WIN-014-R02`（旧虚机），新虚机未重复安装测试。

## 6 与昨日两次失败的差异

| 因素 | 昨日（旧 VM） | 今日（新 VM） |
|---|---|---|
| 快照 | 交接记录注明旧快照含内存，回滚会恢复运行时状态 | `clean-installed` / `clean-with-tools` 均为关机快照 |
| 执行方式 | `vmrun runProgramInGuest` 服务会话 + 长阻塞调用 | 在 VM 图形界面手动运行 |
| 运行时状态 | 多轮回滚、Gradle daemon/Java 锁、Tools/HGFS 通道退化 | 全新设备通道、无残留进程/锁 |
| 宿主 | 期间 DSH renderer 崩溃、系统睡眠/唤醒 | 已重启，未睡眠 |
| 结果 | 两次卡在 `[5/6] build`，无 build.log | `BUILD_OK` 4m29s、`PACKAGE_OK2`、17/17 |

结论：不能把昨日失败单独归因于快照或 vmrun；更可能是**旧 VM 运行时状态（含内存快照恢复）+ vmrun guest 通道 + 宿主状态**叠加。今日新 VM + 手动运行 + 宿主重启共同消除了这些因素。该差异不改变 C8 的结论，但说明“干净基线必须用关机快照、长构建避免走 vmrun 服务会话”。

## 7 结论

- **原始事实（F）**：新虚机上 `BUILD_OK`（4m29s）、`PACKAGE_OK2`、MSI 大小 71,300,856 B、smoke 17/17；两次 MSI 大小一致但哈希不同。
- **项目组解释（I）**：构建可在全新虚机复现；哈希差异来自卷序列号/时间戳等元数据，不代表源码或工具链漂移。
- **技术/需求结论（C）**：W-E15 的“二次重建”成立；建议后续以「大小/版本/测试」而非 SHA-256 作为复现判据，签名仍缺。
