# W-E01 测试报告：KMP Windows target 最小工程

- 实验编号：W-E01（结果编号 `TECH-WIN-004-R01`）
- 关联决策：D-12 A（只补齐 KMP 双端证据）、D-28 A（KMP 共享领域/同步/合同/测试）、D-34 A（双端最小验证）
- 关联 ADR：ADR-028
- 执行人：B
- 日期：2026-11-18
- 状态：已执行，最小闭环通过；**不构成 ADR-028 关闭**

## 1 目的

验证「Windows 正式 target 能构建并消费同一 shared 领域、同步、合同与测试向量」，并记录构建、共享测试、依赖图、桥接量与调试证据（`31-Windows待测清单` W-E01 的通过证据）。

## 2 环境配置快照（按授权要求记录）

| 项 | 值 |
|---|---|
| 主机名 | HYC-1 |
| Windows 版本/构建 | Build 26300.9974（DisplayVersion 26H2，Windows 11 分支） |
| CPU 架构 | x64 |
| 显示器/缩放 | 单屏 150%（`AppliedDPI=144`） |
| JDK | Oracle JDK **21.0.12.1**（含 `jdk.jpackage`） |
| JDK 路径 | `D:\#_Dev\Java_compiler` |
| `JAVA_HOME` | `D:\#_Dev\Java_compiler`（用户级） |
| Gradle | **8.14.7**（`D:\#_Dev\gradle-8.14.7`，用户级 `GRADLE_HOME`） |
| `GRADLE_USER_HOME` | `D:\#_Dev\#_CACHES\gradle` |
| Kotlin Gradle Plugin | **2.1.20**（`kotlin("multiplatform")` / `kotlin("jvm")`） |
| JVM toolchain | 21（`jvmToolchain(21)`） |
| 网络 | 首次构建需访问 Maven Central / Gradle Plugin Portal；首次构建 3 分 8 秒 |
| 第二本地用户 | `何亚傻`（已建，本次未使用；留给 W-E10） |
| WiX | 未安装（本次不需要） |
| 干净虚机 | 未准备（W-E13/W-E15 用） |

**配置变更记录**：

| 时间 | 变更 | 回滚 |
|---|---|---|
| 2026-11-17 | 用户安装 JDK 21 至 D 盘并设 `JAVA_HOME`；安装 Gradle 8.14.7；设 `GRADLE_USER_HOME` | 删除对应目录并移除环境变量 |
| 2026-11-18 | B 新增 `experiments/windows-kmp-minimal/` 工程文件；首次构建生成 `build/`、`.gradle/` | 删除该目录即可，无系统级变更 |

## 3 实验步骤与命令

```powershell
$env:JAVA_HOME = 'D:\#_Dev\Java_compiler'
$env:GRADLE_USER_HOME = 'D:\#_Dev\#_CACHES\gradle'
& 'D:\#_Dev\gradle-8.14.7\bin\gradle.bat' --no-daemon --console=plain --stacktrace :shared:jvmTest :windowsApp:run
& 'D:\#_Dev\gradle-8.14.7\bin\gradle.bat' --no-daemon --console=plain :windowsApp:installDist :windowsApp:dependencies --configuration runtimeClasspath
```

工程结构：

```text
windows-kmp-minimal/
├─ shared/                 # KMP：commonMain 领域/同步/检索合同；jvmMain 只放 sha256Hex actual
│  └─ src/commonTest/      # 与 Android 端同源测试向量
└─ windowsApp/             # Windows JVM target（application），消费 shared
```

## 4 结果

### 4.1 构建与测试

| 项 | 结果 |
|---|---|
| 构建 | **BUILD SUCCESSFUL**（首次 3 分 8 秒 / 188.7 秒；增量 7.3 秒） |
| 共享测试 | **16/16 通过**：DocumentIdentity 3、KeywordIndex 4、ConflictPolicy 3、SyncEngine 6 |
| smoke 运行 | **10/10 PASS**：身份 2、检索 2、降级 2、同步 3、冲突 1 |
| 退出码 | 首次 `EXIT=0`；增量 `EXIT=0` |

测试报告 XML 位于 `shared/build/test-results/jvmTest/`（4 个文件，failures=0、errors=0）。

### 4.2 依赖图（runtimeClasspath）

```text
+--- org.jetbrains.kotlin:kotlin-stdlib:2.1.20
|    \--- org.jetbrains:annotations:13.0
\--- project :shared
     \--- org.jetbrains.kotlin:kotlin-stdlib:2.1.20 (*)
```

结论：`windowsApp` 的运行时依赖只有 Kotlin 标准库与 `:shared`，**没有平台/文档/UI 依赖泄漏进共享核心**；`shared` 内 JVM 专用代码仅限 `jvmMain` 的 `actual` 实现。

### 4.3 桥接量与制品

| 指标 | 值 |
|---|---|
| `windowsApp` 引用的 shared 符号 | **20 个唯一 import**（identity 2、retrieval 5、sync 13） |
| `shared-jvm.jar` | 53,688 B |
| `windowsApp.jar` | 4,912 B |
| `installDist` 目录 | 6 个文件，1,769.5 KB（其中 `kotlin-stdlib-2.1.20.jar` 1,724,057 B） |
| 是否自带 JRE | **否**（installDist 不是自包含运行时；app-image 需后续 jpackage） |

### 4.4 证据清单

| 类型 | 路径 |
|---|---|
| 构建/依赖日志 | `experiments/windows-kmp-minimal/we01-deps-and-dist.log` |
| 测试报告 | `experiments/windows-kmp-minimal/shared/build/test-results/jvmTest/TEST-*.xml` |
| 可运行分布 | `experiments/windows-kmp-minimal/windowsApp/build/install/windowsApp/` |
| 工程源文件 | `experiments/windows-kmp-minimal/`（Gradle 配置、commonMain、jvmMain、commonTest、Main.kt） |

## 5 判定（对照 W-E01 通过证据）

| 通过证据 | 是否取得 | 说明 |
|---|---|---|
| release-like 构建日志 | ✅ | 两次 Gradle 构建日志，均 BUILD SUCCESSFUL |
| 共享测试 | ✅ | 16/16，含同步状态机与冲突策略 |
| 依赖图 | ✅ | 仅 kotlin-stdlib + `:shared` |
| 桥接量 | ✅ | 20 个共享符号引用；`expect/actual` 仅 1 处（SHA-256） |
| 调试记录 | ✅ | `--stacktrace` 运行，无失败；首次构建下载耗时可复现 |

## 6 边界与未覆盖

1. **不是最终 KMP 工程**：`shared` 是 Android `shared` 的移植（包名改为 `reader.windowsmin.shared`），尚未成为两端物理共用的单一模块；D-28 A 要求的「同一 shared 模块 + 同一测试向量双端复跑」还需 C/A 配合。
2. **Android 目标未在本轮复跑**：C 的 `TECH-ANDROID-*` 结果可对照，但不是本次运行的同一构建。
3. **无 UI/无文档组件**：本工程没有 Compose Desktop、PDF 渲染、选区或数据库；W-E02 才覆盖。
4. **无自包含打包**：`installDist` 不含 JRE；jpackage `app-image` 与 MSI 属 W-E13。
5. **网络依赖**：首次构建依赖 Maven Central；离线环境未验证。
6. **Kotlin 2.1.20 + Gradle 8.14.7**：本次未出现兼容性错误，但未做长期稳定性验证。

## 7 结论

- **原始事实（F）**：Windows JVM target 构建成功；同源 16 个共享测试全部通过；smoke 10/10；运行时依赖仅 Kotlin 标准库与 `:shared`；桥接 20 个符号；`shared` 中唯一的平台实现是 `jvmMain` 的 SHA-256。
- **项目组解释（I）**：KMP 的「共享合同 + 平台 actual」在 Windows 上可落地；平台依赖可以被隔离在 `jvmMain`，未污染 `commonMain`；构建与测试链路可复现。
- **技术/需求结论（C）**：W-E01 的最小闭环达成，支持 D-28 A 方向；但 ADR-028 的关闭门槛还缺「Android 正式目标同源复跑 + 制品/调试证据 + 桥接量双端对比」，因此 **ADR-028 保持 Proposed**，不因本次结果转 Accepted。

## 8 回填动作

1. `Windows端验证计划与证据索引.md` §3：W-E01 状态改为「最小闭环完成（`TECH-WIN-004-R01`），待双端同源复跑」。
2. `Windows端架构调研与比较.md` §9.2 / §10：更新 W-E01 现状与剩余缺口。
3. 本报告不改变 D-12/D-28 的决策状态，不替代 A 的汇总与项目负责人批准。
