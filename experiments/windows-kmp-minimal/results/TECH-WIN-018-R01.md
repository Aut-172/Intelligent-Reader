# W-E07 崩溃重启恢复补充实验

- 实验编号：W-E07 补充（结果编号 `TECH-WIN-018-R01`，承接 `TECH-WIN-007-R01`）
- 关联决策：D-26 A、D-34 A
- 执行人：B
- 日期：2026-11-18
- 状态：**部分完成**——单进程硬杀 + 重启恢复已验证；多进程竞争、WAL/断电未测
- 环境：本机 Windows 11，JDK 21，SQLite JDBC 3.46.1.3

## 1 目的

验证进程在持有任务租约时被硬杀后，重启能否回收过期租约、保持幂等、继续消费队列。

## 2 方法

探针 `windowsApp/src/main/kotlin/reader/windowsmin/app/task/CrashProbe.kt`：

1. `crash` 模式：打开 SQLite，入队 5 个任务，领取 1 个（30 s 租约），打印状态后 `Runtime.halt(7)`；
2. `verify` 模式：重新打开同一 DB，按指定时间偏移调用 `recoverExpiredLeases`、`claimNext`、`complete`，并检查重复入队；
3. `dump` 模式：输出全部任务状态。

命令：

```powershell
java -cp "windowsApp\build\install\windowsApp\lib\*" reader.windowsmin.app.task.CrashProbeKt crash  <db>
java -cp "windowsApp\build\install\windowsApp\lib\*" reader.windowsmin.app.task.CrashProbeKt verify <db> 120000
java -cp "windowsApp\build\install\windowsApp\lib\*" reader.windowsmin.app.task.CrashProbeKt dump   <db>
```

## 3 结果

| 阶段 | 观测 |
|---|---|
| 硬杀 | `CRASH claimed=task-1 state=RUNNING`，进程退出码 **7**（模拟崩溃） |
| 重启恢复 | `VERIFY recovered=1 pending=5`；`claimNext` 继续领取；`duplicate_enqueue=false` |
| 租约对齐后 | `task-2` 被回收（`attempts` 增加）、领取并完成 |
| 最终 | 5 个任务全部 `SUCCEEDED`，`pending=0`；再次 verify 无任务可领 |

任务表最终状态：

```
task-1 SUCCEEDED attempts=2
task-2 SUCCEEDED attempts=3
task-3 SUCCEEDED attempts=1
task-4 SUCCEEDED attempts=1
task-5 SUCCEEDED attempts=1
```

## 4 判定

| 通过证据 | 结果 |
|---|---|
| 崩溃时租约状态落盘 | ✅ |
| 重启回收过期租约 | ✅（recovered=1） |
| 幂等入队 | ✅（重复入队返回 false） |
| 继续消费、无重复副作用 | ✅（全部完成，pending=0） |
| 多进程同时领取 | ❌ 未测 |
| WAL/断电/磁盘损坏 | ❌ 未测 |

## 5 结论

- **原始事实（F）**：持租约进程被硬杀后，重启可回收该租约并继续消费；重复入队被拒绝；最终 5 个任务全部成功。
- **项目组解释（I）**：SQLite 任务表 + 租约恢复满足 D-26 A 的单机崩溃恢复要求；时间偏移用于模拟租约到期，非真实等待。
- **技术/需求结论（C）**：W-E07 的崩溃重启恢复成立；多进程竞争与服务端任务表仍待补，D-26 保持 Proposed。
