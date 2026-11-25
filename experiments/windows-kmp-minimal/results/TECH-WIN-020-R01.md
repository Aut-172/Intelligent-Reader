# W-E07 崩溃 + 并发叠加实验

- 实验编号：W-E07 补充（结果编号 `TECH-WIN-020-R01`，承接 `TECH-WIN-018-R01`、`TECH-WIN-019-R01`）
- 关联决策：D-26 A、D-34 A
- 执行人：B
- 日期：2026-11-18
- 状态：**完成**（同机崩溃 + 并发；含一次并发加固）
- 环境：本机 Windows 11，JDK 21，SQLite JDBC 3.46.1.3

## 1 目的

验证「一个进程领取任务后被硬杀、其他进程同时消费」时，是否会出现**租约未到期就被重复领取**、任务丢失或 SQLITE_BUSY。

## 2 方法

1. 入队 5 个任务；
2. 先启动 `worker-crash`（领取 1 个、租约 120 s、`Runtime.halt(7)`）；
3. 确认其领取文件后，再启动 `worker`（120 s 租约）消费剩余 4 个；
4. 检查领取日志与 DB 状态；再以 `verify 120001` 模拟租约到期，回收并完成任务。

> 早期用 100/20 个任务、30 s 租约时整轮耗时 31.6 s，超过租约，崩溃任务被合法回收，日志中出现同一 taskId 两次；这是**租约到期恢复**，不是并发重复领取。为区分两者，改用「先崩溃后启动 + 120 s 租约」的受控实验。

## 3 结果

| 阶段 | 观测 |
|---|---|
| 崩溃 | `wc claimed=task-1`，退出码 7 |
| 并发消费 | `w2 claimed=4 pending=1`；DB：`SUCCEEDED=4`、`RUNNING=1` |
| 领取日志 | `claims=5 distinct=5 duplicates=0`（租约未到期，无重复） |
| 租约到期 | `recovered=1`，重新领取 `task-1`（`attempts=2`）并完成 |
| 最终 | `SUCCEEDED=5`，`pending=0`，重复仍为 0 |
| SQLITE_BUSY | 加固后无（`WC_ERR`/worker err 为空） |

## 4 并发加固（本轮代码变更）

压测初期 `claimNext` 在并发启动时抛 `SQLITE_BUSY`；随后发现「先 SELECT 再 UPDATE」在极端时序下可能让两个进程选中同一候选。已对 `SqliteTaskStore` 做如下加固：

| 变更 | 目的 |
|---|---|
| `busy_timeout=5000` + `SQLITE_BUSY` 重试（20 次退避） | 避免瞬时锁冲突直接失败 |
| 单条 `UPDATE … RETURNING` 原子领取 | 消除 SELECT/UPDATE 之间的竞争窗口 |
| `journal_mode=WAL` + `synchronous=FULL` | 提高崩溃后的提交持久性 |
| `transaction_mode=IMMEDIATE` + 显式 `commit` | 写锁前置，提交后再返回 |

加固后：`shared` 29 + `windowsApp` 13 = **42 项测试全部通过**。

## 5 判定

| 通过证据 | 结果 |
|---|---|
| 租约内不重复领取 | ✅（5 claims / 5 distinct） |
| 崩溃任务可恢复 | ✅（recovered=1，attempts=2） |
| 无任务丢失 | ✅（最终 5 SUCCEEDED） |
| 无 SQLITE_BUSY | ✅（加固后） |
| 真实断电/网络盘/多机 | ❌ 未测 |

## 6 结论

- **原始事实（F）**：崩溃进程持有的任务在租约内未被其他进程领取；租约到期后被回收并完成；最终 5/5 成功、无丢失；加固后无 BUSY。
- **项目组解释（I）**：此前的「重复」是压测时长超过租约导致的合法回收；真正的并发缺陷是 `SQLITE_BUSY` 与 SELECT/UPDATE 竞争窗口，已用 busy_timeout/重试、原子 UPDATE、WAL 修复。
- **技术/需求结论（C）**：W-E07 的崩溃+并发叠加维度成立；D-26 的客户端任务表在单机范围内具备可用性，服务端任务表仍待补，ADR 保持 Proposed。
