# W-E01 依赖图与共享/客户端依赖隔离

- 实验编号：W-E01 补充（结果编号 `TECH-WIN-028-R01`，承接 `TECH-WIN-004-R01`）
- 执行人：B
- 日期：2026-11-18
- 状态：**部分通过**——依赖图已生成；Android 同源复跑仍缺
- 环境：JDK 21、Gradle 8.14.7

## 1 目的

补 W-E01 的“依赖图”证据，确认 Windows 客户端只通过 `shared` 消费公共契约，平台依赖不污染 shared。

## 2 方法

```powershell
gradle :shared:dependencies      > docProbe/results/we01-deps-shared.txt
gradle :windowsApp:dependencies  > docProbe/results/we01-deps-app.txt
```

## 3 结果

| 文件 | 大小 | 关键内容 |
|---|---|---|
| `we01-deps-shared.txt` | 16.8 KB | shared 的 Kotlin/JVM 依赖树 |
| `we01-deps-app.txt` | 10.3 KB | `kotlin-stdlib:2.1.20`、`sqlite-jdbc:3.46.1.3`、项目 `:shared` |

- 未出现 `FAILED` 依赖；
- 客户端平台依赖（SQLite JDBC）只出现在 `windowsApp`，未进入 `shared`；
- 桥接量沿用 `TECH-WIN-004-R01` 的 20 个符号。

## 4 判定

| 通过证据 | 结果 |
|---|---|
| 依赖图 | ✅ |
| 平台依赖不污染 shared | ✅ |
| 共享测试 29/29 | ✅ |
| Android 同源复跑 | ❌ 需 C |
| release-like 完整构建记录 | ⚠️ 已有 installDist/jvmTest，非正式 release |

## 5 结论

- **原始事实（F）**：shared 与 windowsApp 的依赖树已导出；SQLite JDBC 仅在 windowsApp；无 FAILED 依赖。
- **项目组解释（I）**：当前结构满足“shared 不含平台专属依赖”的隔离要求；正式 release 构建与 Android 同源仍缺。
- **技术/需求结论（C）**：W-E01 的依赖隔离证据成立；跨端同源与正式 release 仍待补。
