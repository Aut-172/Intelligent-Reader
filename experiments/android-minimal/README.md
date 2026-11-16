# 智能阅读器 Android 端最小工程实验（android-minimal）

这是成员 C 的「Android 端架构调研与比较」**待测项最小验证工程**，用于把
`docs/架构设计/02-平台调研/Android端架构调研与比较.md` 第 10.3 节的 6 项【待测】实验跑成可复现证据。

> 定位：最小工程，不是产品原型。所有"服务端/云端"均为本地模拟，不调用真实网络（本工程甚至不声明 `INTERNET` 权限）。
> 边界：本文档只产生证据并回填【待测】项，不自行改变 D-00 至 D-10 已关闭决策，不宣布 D-11 至 D-15 结论。

---

## 1 工程结构

```text
android-minimal/
├─ shared/                  # 纯 Kotlin 共享核心（无 Android 依赖，JVM 可测）
│  └─ src/main/kotlin/reader/androidmin/shared/
│     ├─ sync/               # 同步队列、幂等、游标、冲突策略、引擎
│     ├─ identity/           # 文档身份（内容指纹 + 元数据）
│     └─ retrieval/          # 关键词索引 + 降级策略
│  └─ src/test/              # A 层 JVM 单元测试
├─ app/                      # Android 平台适配 + B 层实验
│  └─ src/main/              # SAF、WorkManager、PDF 探针、文件存储适配、MainActivity
│  ├─ src/androidTest/       # 仪器测试（PDF 探针、文件持久化恢复）
│  └─ src/main/assets/probe/ # 固定样本 SELF-001（与桌面端同源）
└─ README.md                 # 本文档
```

`shared` 对应 D-03「共享核心」；`app` 对应「平台适配」。「共享核心 + 平台适配」的边界即通过
`QueueStorage` / `CursorStorage` 接口体现：共享核心只依赖接口，JVM 测试用内存实现，Android 用文件实现。

---

## 2 环境要求

| 项 | 要求 |
|---|---|
| JDK | 17（Temurin/Adoptium） |
| Android SDK | cmdline-tools + platform-tools + `platforms;android-34` + build-tools 34 |
| Gradle | 8.7（用 wrapper 固定；或直接安装 8.7） |
| 设备 | Android 12（API 31）及以上真机，开启 USB 调试 |
| 网络 | 可访问 `dl.google.com` + Maven Central + Google Maven（下载依赖） |

版本已固定：AGP 8.4.2、Kotlin 2.0.20、minSdk 31 / targetSdk 34、pdfbox-android 2.0.27.0。

---

## 3 一键运行

### 3.1 引导 wrapper（首次）

在 `experiments/android-minimal/` 目录：

```powershell
# 用已安装的 Gradle 生成 wrapper（固定 8.7），后续统一用 ./gradlew
gradle wrapper --gradle-version 8.7
```

> 也可直接用 Android Studio 打开本目录，IDE 会按 `gradle-wrapper.properties` 自动同步。

### 3.2 A 层：共享核心语义（纯 JVM，无需设备）

```powershell
./gradlew :shared:test
```

覆盖：同步队列/幂等/游标/部分失败隔离/进程重启恢复、冲突策略（D-13 A）、文档身份（D-16）、本地检索降级（D-06/D-15）。
产物在 `shared/build/reports/tests/`。

### 3.3 B 层：构建 + 仪器测试（需真机/模拟器）

```powershell
./gradlew :app:assembleDebug          # 构建 debug APK
./gradlew :app:connectedAndroidTest   # 连真机跑仪器测试（PDF 探针 + 文件持久化恢复）
```

### 3.4 手动实验（真机交互项）

```powershell
./gradlew :app:assembleRelease        # 构建 release APK（当前复用 debug 签名）
adb install -r app/build/outputs/apk/release/app-release.apk
```

安装后打开 App，按按钮执行实验（结果同时写入 `filesDir/results/*.json`）：

```powershell
adb shell run-as reader.androidmin.app ls -R files/results   # debug 包
adb exec-out run-as reader.androidmin.app cat files/results/TECH-ANDROID-003-R01.json
```

---

## 4 六项待测实验与通过标准

| 实验 | 执行方式 | 通过标准 | 对应待测项 |
|---|---|---|---|
| 1 SAF 文件授权 | 手动（App 按钮 1 / 1b） | 取得持久读授权；重启后仍可读；提供方撤权/删文件后出现 SecurityException 并提示重绑 | C-01、D-05 |
| 2 后台任务 | 手动（按钮 2 + 杀掉进程重开） | 入队后 Worker 消费；进程被杀后重开队列不丢失不重复 | C-01、D-26 |
| 3 PDF 文档探针 | 自动（`connectedAndroidTest`）+ 手动（按钮 3） | SELF-001 单页、块数>0、锚点命中、bbox 有限非负、SHA-256 同源 | C-02、D-22 |
| 4 同步最小实验 | 自动（`FileStorageInstrumentedTest`） | 文件持久化跨引擎重建无重复无丢失；游标正确推进 | C-04、D-13/D-21 |
| 5 release 验证 | 手动（`adb install -r`/卸载/重装 + 按钮 5） | 侧载安装、升级覆盖、卸载无残留；版本/SDK/ABI 信息正确 | C-06、D-08 |
| 6 AI 降级 | 自动（`shared` 测试）+ 手动（按钮 4） | 断网时降级为仅本地检索，本地索引可用时返回命中 | C-05、D-06/D-15 |

### 4.1 实验 5 的 adb 清单（侧载/升级/卸载）

```powershell
adb install app/build/outputs/apk/release/app-release.apk   # 首次侧载
# 修改 versionCode/versionName 后重新 assembleRelease：
adb install -r app-release.apk                              # 升级覆盖（验证数据/持久授权保留）
adb uninstall reader.androidmin.app                          # 卸载（验证无残留，见下）
adb shell pm list packages | findstr androidmin              # 确认已卸载
```

> 升级覆盖时验证：持久授权 URI（实验 1 保存的）与 `sync_queue.json` 是否保留；卸载后重装是否从空状态开始。

---

## 5 验证矩阵（证据等级 → 结论）

| 实验编号 | 验证内容 | 初始证据等级 | 跑通后登记 |
|---|---|---|---|
| `TECH-ANDROID-001-R01` | SAF 授权/持久授权/重绑 | 【待测】 | 【A独有】已验证 |
| `TECH-ANDROID-002-R01` | WorkManager 后台 + 恢复 | 【待测】 | 【A独有】已验证 |
| `TECH-ANDROID-003-R01` | PDF 探针 bbox/锚点/阅读顺序 | 【待测】 | 【官方】/【待测】→按结果 |
| `TECH-ANDROID-004-R01` | 同步文件持久化恢复 | 【待测】 | 【W共享】已验证 |
| `TECH-ANDROID-005-R01` | release 侧载/升级/卸载 | 【待测】 | 【A独有】已验证 |
| `TECH-ANDROID-006-R01` | 本地检索降级 | 【待测】 | 【W共享】已验证 |

---

## 6 结果回填模板

每个实验完成后，在 `experiments/android-minimal/results/` 下按模板登记（与桌面端 `TECH-DOC-*/TECH-ARCH-*` 同口径）：

```text
experiment_id:  TECH-ANDROID-003-R01
date:           2026-11-XX
sample_id:      SELF-001
source_sha256:  0391dd982a1c426c52ca865bde7c253baaa4071d7ca26317febe2391a5389c93
tool/version:   pdfbox-android 2.0.27.0
config_hash:    <记录本次构建/配置哈希或 git commit>
runtime:        <真机型号 + Android API 级别>
raw_output_path: files/results/TECH-ANDROID-003-R01.json
metrics:        pageCount / blockCount / anchorHits / readingOrderOk
failure_cases:  <无 / 具体失败样本>
interpretation(I):   <对结果的解释，不夸大>
conclusion(C):       <仅限当前样本+版本的技术结论>
open_risks(R):       <未覆盖的版式/设备/样本风险>
evidence_links:      <JSON + 日志路径>
```

---

## 7 编号与证据等级对齐

- 桌面/Windows 端证据沿用既有编号：`TECH-ARCH-*`（架构链路）、`TECH-DOC-*`（解析）、`TECH-OCR-*`（OCR）、`TECH-QA-*`（问答）。
- 本工程新增 `TECH-ANDROID-*` 前缀，专用于 Android 平台证据，不与桌面端结论混用。
- 跑完【待测】项后，回填 `docs/架构设计/02-平台调研/Android端架构调研与比较.md` 第 10.4 节缺口清单与证据等级标签（由成员 C 更新，A 汇总）。

---

## 8 已知脆弱点（运行时如报错先看这里）

1. **Gradle wrapper 未生成**：先跑 `gradle wrapper --gradle-version 8.7`（或让 Android Studio 同步）。
2. **pdfbox-android API 签名**：`PDFTextStripper.writeString(text, MutableList<TextPosition>)` 是该库 2.0.27.0 的签名；
   若 IDE 提示类型不匹配，把 `PdfProbe.kt` 中参数类型改为 `List<TextPosition>` 即可（Java 平台类型差异）。
3. **`connectedAndroidTest` 找不到设备**：确认 `adb devices` 有设备、已授权 USB 调试。
4. **release 签名**：当前复用 debug 签名以便覆盖安装；正式自签名请替换 `app/build.gradle.kts` 的 `signingConfig`。
