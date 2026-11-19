# W-E07 多进程并发领取实验

- 实验编号：W-E07 补充（结果编号 `TECH-WIN-019-R01`，承接 `TECH-WIN-007-R01`、`TECH-WIN-018-R01`）
- 关联决策：D-26 A、D-34 A
- 执行人：B
- 日期：2026-11-18
- 状态：**完成**（同机多进程竞争）
- 环境：本机 Windows 11，JDK 21，SQLite JDBC 3.46.1.3

## 1 目的

验证多个独立客户端进程同时从同一 SQLite 任务表领取任务时，是否出现重复领取、SQLITE_BUSY 失败或任务丢失。

## 2 方法

1. `seed` 模式入队 **200** 个任务；
2. 同时启动 **3 个独立 JVM**（`worker` 模式），各自循环 `claimNext` → `complete`，把领取的 taskId 写入独立文件；
3. `check` 模式汇总三个文件，检查重复与最终状态。

命令：

```powershell
java -cp "windowsApp\build\install\windowsApp\lib\*" reader.windowsmin.app.task.CrashProbeKt seed  <db> 200
# 并发启动 3 个 worker，各自输出 worker-wN.txt
java -cp "windowsApp\build\install\windowsApp\lib\*" reader.windowsmin.app.task.CrashProbeKt check <db> worker-w1.txt worker-w2.txt worker-w3.txt
```

## 3 结果

| 进程 | 领取数 |
|---|---|
| w1 | 64 |
| w2 | 59 |
| w3 | 77 |
| 合计 | **200** |

| 检查 | 结果 |
|---|---|
| 重复 taskId | **0** |
| 未领取任务 | **0** |
| 最终状态 | `SUCCEEDED=200` |
| SQLITE_BUSY / 异常 | **0**（worker 输出无 `ERR` 行） |

## 4 判定

| 通过证据 | 结果 |
|---|---|
| 多进程不重复领取 | ✅ |
| 全部任务完成 | ✅ |
| 无 SQLITE_BUSY | ✅（本机、200 任务、3 进程） |
| 崩溃与并发叠加 | ❌ 未测 |
| WAL / 网络文件系统 / 服务端任务表 | ❌ 未测 |

## 5 结论

- **原始事实（F）**：3 个独立进程并发领取 200 个任务，分配为 64/59/77，无重复、无丢失、无 SQLITE_BUSY，最终全部成功。
- **项目组解释（I）**：SQLite 的串行化写入与 `claimNext` 的租约语义在本机并发下可保证「一任务一领取」；样本规模较小，未触发锁等待上限。
- **技术/需求结论（C）**：W-E07 的多进程领取维度成立；崩溃+并发叠加、WAL/网络盘与服务端任务表仍待补，D-26 保持 Proposed。
