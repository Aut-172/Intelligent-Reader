# W-E07 硬断电与数据库完整性

- 实验编号：W-E07 补充（结果编号 `TECH-WIN-021-R01`，承接 `TECH-WIN-020-R01`）
- 关联决策：D-26 A、D-34 A
- 执行人：B
- 日期：2026-11-18
- 状态：**完成**（虚机级硬断电近似；非物理磁盘损坏）
- 环境：`Win11-Clean` 虚机（Win11 Pro Build 26300.9457），OpenJDK 21.0.2，SQLite JDBC 3.46.1.3

## 1 目的

在持续写入 SQLite 任务表时对虚机硬断电，重启后检查数据库完整性、任务状态与可恢复性。

## 2 方法

1. 在虚机内解压 JDK 与 `powerprobe.zip`（`SqliteTaskStore` + `CrashProbe`）；
2. 运行 `power` 模式：循环 `enqueue → claimNext → complete`，每 50 次把进度写入共享文件；
3. 观察到 `count≈4550`、WAL 约 2.5 MB 时，执行 `vmrun stop <vmx> hard`（硬断电）；
4. 重新启动虚机，运行 `power-verify`：
   - `PRAGMA integrity_check`
   - 回收过期租约、清空可领取任务
   - 统计各状态数量。

## 3 结果

| 项 | 结果 |
|---|---|
| 断电时进度 | `count=4550`（持续写入中） |
| 重启后 `integrity_check` | **ok** |
| 任务状态 | `SUCCEEDED=4790`，`pending=0` |
| 恢复 | `recovered=0`，`drained=0` |
| 数据库可打开/可查询 | ✅ |

## 4 判定

| 通过证据 | 结果 |
|---|---|
| 硬断电后 DB 可打开且完整 | ✅ |
| 无损坏行/无法恢复状态 | ✅（integrity=ok） |
| 任务计数一致 | ✅（全部 SUCCEEDED，无 pending） |
| 真实磁盘损坏/坏道 | ❌ 未模拟 |
| 网络文件系统 | ❌ 不适用（SQLite 不支持网络盘锁语义） |

## 5 结论

- **原始事实（F）**：持续写入约 4550 个任务时硬断电，重启后 `integrity_check=ok`，4790 个任务全部 SUCCEEDED，无 pending。
- **项目组解释（I）**：WAL + FULL 同步使已提交事务可恢复、未提交事务回滚，断电未造成逻辑损坏；硬断电是虚机级近似，不等同于物理坏道。
- **技术/需求结论（C）**：W-E07 的断电完整性维度成立；物理损坏与网络盘不在客户端本地 SQLite 的目标范围，登记为不适用/超出范围。
