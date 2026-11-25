# Windows KMP Minimal（W-E01/W-E02/W-E06 最小工程）

本目录是架构调研阶段角色 B 的最小工程，不是产品原型：

- `shared/`：Kotlin Multiplatform 模块。`commonMain` 承载与平台无关的领域、同步合同、检索合同、文档合同、引用合同；`jvmMain` 只提供 `sha256Hex` 的 `actual` 实现（演示 D-28 A 的端口隔离，不允许平台类型泄漏进 common）。
- `windowsApp/`：Windows JVM 目标（`kotlin("jvm")` + `application`），消费 `shared` 并执行固定 smoke 向量。
- `docProbe/`：W-E02 文档探针，使用 Apache PDFBox 3.0.5 产出 D-22 A 统一合同。
- `shared/src/commonTest/`：与 Android 端同源的测试向量（身份 3、检索/降级 4、冲突 3、同步 6、引用 8 = **24 个用例**）。

## 运行

```powershell
$env:JAVA_HOME = 'D:\#_Dev\Java_compiler'
$env:GRADLE_USER_HOME = 'D:\#_Dev\#_CACHES\gradle'
& 'D:\#_Dev\gradle-8.14.7\bin\gradle.bat' --no-daemon --console=plain :shared:jvmTest :windowsApp:run
```

文档探针（W-E02）：

```powershell
& 'D:\#_Dev\gradle-8.14.7\bin\gradle.bat' --no-daemon --console=plain :docProbe:run --args="SELF-001=<pdf>=293 SELF-002=<pdf>=572 SELF-003=<pdf>=287 SELF-004=<pdf>=1"
```

## 结果与证据

| 实验 | 结果编号 | 报告 |
|---|---|---|
| W-E01 KMP Windows target | `TECH-WIN-004-R01` | `results/TECH-WIN-004-R01.md` |
| W-E02 文档合同（PDFBox） | `TECH-WIN-005-R01` | `results/TECH-WIN-005-R01.md` |
| W-E06 引用与 stale | `TECH-WIN-006-R01` | `results/TECH-WIN-006-R01.md` |
| W-E07 持久任务（SQLite） | `TECH-WIN-007-R01` | `results/TECH-WIN-007-R01.md` |
| W-E08 SSE/轮询回退 | `TECH-WIN-012-R01` | `results/TECH-WIN-012-R01.md` |
| W-E11 诊断包 | `TECH-WIN-009-R01` | `results/TECH-WIN-009-R01.md` |
| W-E12 三类样本耗时 | `TECH-WIN-010-R01` | `results/TECH-WIN-010-R01.md` |
| W-E07 崩溃重启恢复 | `TECH-WIN-018-R01` | `results/TECH-WIN-018-R01.md` |
| W-E07 多进程领取 | `TECH-WIN-019-R01` | `results/TECH-WIN-019-R01.md` |
| W-E07 崩溃+并发 | `TECH-WIN-020-R01` | `results/TECH-WIN-020-R01.md` |
| W-E07 硬断电 | `TECH-WIN-021-R01` | `results/TECH-WIN-021-R01.md` |
| W-E10 Credential Manager | `TECH-WIN-013-R01` | `../windows-env-probe/results/TECH-WIN-013-R01.md` |
| W-E13 打包与安装 | `TECH-WIN-011-R01` | `results/TECH-WIN-011-R01.md`（制品在 `dist/`，已 gitignore） |
| W-E13 升级覆盖 | `TECH-WIN-011-R02` | `results/TECH-WIN-011-R02.md` |
| W-E13 干净虚机（无效记录） | `TECH-WIN-014-R01` | `results/TECH-WIN-014-R01.md` |
| W-E13 干净虚机（复跑通过） | `TECH-WIN-014-R02` | `results/TECH-WIN-014-R02.md` |
| W-E15 干净环境重建 | `TECH-WIN-015-R01` | `results/TECH-WIN-015-R01.md` |
| W-E15 二次重建 | `TECH-WIN-029-R01` | `results/TECH-WIN-029-R01.md` |
| W-E02 复杂样本 | `TECH-WIN-022-R01` | `results/TECH-WIN-022-R01.md` |
| W-E12 批量性能 | `TECH-WIN-023-R01` | `results/TECH-WIN-023-R01.md` |
| W-E03 高亮/缓存 | `TECH-WIN-024-R01` | `results/TECH-WIN-024-R01.md` |
| W-E10 日志扫描 | `TECH-WIN-025-R01` | `../windows-env-probe/results/TECH-WIN-025-R01.md` |
| W-E03 Windows OCR | `TECH-WIN-017-R01` | `../windows-ocr-probe/results/TECH-WIN-017-R01.md` |
| W-E11 故障注入 | `TECH-WIN-026-R01` | `results/TECH-WIN-026-R01.md` |
| W-E06 解析器升级 | `TECH-WIN-027-R01` | `results/TECH-WIN-027-R01.md` |
| W-E01 依赖图 | `TECH-WIN-028-R01` | `results/TECH-WIN-028-R01.md` |

测试规模：`shared` 29 个用例 + `windowsApp` 13 个用例 = **42**；smoke 运行 17 项。

## 边界

1. 本工程不是最终 KMP 工程：不含 UI、数据库、后台任务与真实文档数据层。
2. `shared` 是 Android `shared` 的移植，尚未成为两端物理共用的单一模块。
3. 没有 Compose Desktop UI；选区、高亮、回跳未验证。
4. 构建需要网络访问 Maven Central / Gradle Plugin Portal；离线复跑需已有缓存。
