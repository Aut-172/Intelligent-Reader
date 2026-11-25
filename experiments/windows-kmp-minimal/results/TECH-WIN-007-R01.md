# W-E07 测试报告：客户端持久任务与崩溃恢复（SQLite）

- 实验编号：W-E07（结果编号 `TECH-WIN-007-R01`）
- 关联决策：D-26 A（客户端持久任务 + 服务端 PostgreSQL 任务表；lease/重试/幂等/取消）
- 关联 ADR：ADR-026
- 执行人：B
- 日期：2026-11-18
- 状态：**部分完成**——单进程 SQLite 持久队列、lease 回收、重试与取消验证通过；真实进程被杀、并发多进程与 UI 退出语义未验证
- 跳过说明：按负责人 2026-11-18 指示，需 UI/多进程/服务端配合的项本轮跳过，其余视为本项完整。

## 1 目的

验证 Windows 客户端「持久任务队列」的最小闭环：重复入队幂等、lease 领取、完成后出队、失败重试与上限、取消、进程重启后未完成任务不丢失、过期 lease 可回收。

## 2 环境配置快照

| 项 | 值 |
|---|---|
| 主机/系统 | HYC-1 / Build 26300.9974 |
| JDK | Oracle 21.0.12.1 |
| Gradle | 8.14.7；`GRADLE_USER_HOME=D:\#_Dev\#_CACHES\gradle` |
| Kotlin | 2.1.20；JVM toolchain 21 |
| 新增依赖 | **`org.xerial:sqlite-jdbc:3.46.1.3`**（Gradle 自动下载，含 Windows 原生库；无需安装 SQLite） |
| 数据库 | 每个用例一个临时 `.db` 文件（`File.createTempFile`），用例结束删除 |
| 网络 | 首次构建需 Maven Central；本次构建 31 秒（含下载） |

配置变更：`windowsApp` 增加 sqlite-jdbc 依赖、`SqliteTaskStore`、测试；回滚为删除这些文件与依赖。

## 3 实现要点

- 共享合同（`shared/task/Task.kt`）：`TaskState`、`TaskRecord`、`TaskStore` 接口；
- Windows 实现（`windowsApp/.../task/SqliteTaskStore.kt`）：单表 `tasks`，字段 `task_id/type/payload/state/attempts/lease_until/last_error/updated_at`；
- `claimNext` 用事务选择 `QUEUED` 或 `lease_until < now` 的任务，置 `RUNNING`、`attempts+1`、写入 lease；
- `recoverExpiredLeases` 把过期 `RUNNING` 拉回 `QUEUED`；
- `fail` 在 `attempts >= maxAttempts` 时置 `FAILED`，否则回 `QUEUED` 重试；
- `cancel` 仅对 `QUEUED` 生效。

## 4 结果

| 用例 | 断言 | 结果 |
|---|---|---|
| 重复入队幂等 | 第二次返回 false，payload 不被覆盖 | ✅ |
| 领取即 RUNNING | `attempts=1`，第二次领取返回 null | ✅ |
| 完成出队 | `SUCCEEDED`，pending=0 | ✅ |
| 重启 + lease 过期回收 | 关闭后新建 store，`recoverExpiredLeases=1`，再次领取 `attempts=2` | ✅ |
| 失败重试与上限 | maxAttempts=2：第一次回 `QUEUED`，第二次 `FAILED` | ✅ |
| 取消 | 取消后不可领取，状态 `CANCELLED` | ✅ |
| 重启后队列保留 | 新 store 仍能看到并领取未完成任务 | ✅ |

- 测试：**7/7 通过**（`:windowsApp:test`，31 秒含依赖下载）；
- 构建：`BUILD SUCCESSFUL`。

## 5 判定（对照 W-E07 通过证据）

| 通过证据 | 是否取得 | 说明 |
|---|---|---|
| 排队与状态迁移 | ✅ | QUEUED→RUNNING→SUCCEEDED/FAILED/CANCELLED |
| 取消 | ✅ | 取消后不可被领取 |
| 重启恢复 | ✅（模拟） | 关闭连接后新实例读取同一 DB；lease 过期回收 |
| 重复调度无重复副作用 | ✅（单进程） | 领取后第二次返回 null；入队幂等 |
| 真实杀进程 | ❌ | 未使用独立进程 + 强杀；当前为「关闭连接后重建」模拟 |
| 多进程并发争抢 | ❌ | 未测试两个进程同时 claim |
| 应用退出/UI 语义 | ❌ | 需桌面壳 |
| 服务端 PostgreSQL 任务表 | ❌ | 本轮不含服务端 |

## 6 跳过项与原因

| 项 | 原因 | 归属 |
|---|---|---|
| 真实进程强杀（taskkill） | 需要独立进程执行任务；可在后续用 `windowsApp` 子进程实现 | 后续实验 |
| 多进程并发 claim | 需要多进程同时访问同一 DB；SQLite 锁行为需单独验证 | 后续实验 |
| 服务端任务表/lease | 服务端未决策 | W-E09 |
| UI 退出与恢复提示 | 需桌面壳 | W-E13 前后 |

## 7 结论

- **原始事实（F）**：7 个用例全部通过；重复入队不覆盖；领取置 RUNNING 且 attempts 递增；关闭后新实例可回收过期 lease 并再次领取；失败按上限转 FAILED；取消后不可领取。
- **项目组解释（I）**：D-26 A 的客户端持久任务模型可在 Windows 上用 SQLite 落地；lease + 幂等键能覆盖「进程退出后恢复」的主要语义。
- **技术/需求结论（C）**：W-E07 的队列层成立；ADR-026 仍为 Proposed，缺真实强杀、多进程并发与服务端任务表证据。

## 8 回填动作

1. 证据索引 §3：W-E07 状态改为「部分完成（客户端队列层）」。
2. 调研文档 §9.2 W-E07、§9.3 D-26 映射更新。
3. 不改变 D-26 A 决策状态。
