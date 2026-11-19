# W-E10 测试报告：Windows 凭据用户隔离（DPAPI 跨用户）

- 实验编号：W-E10（结果编号 `TECH-WIN-008-R01`）
- 关联决策：D-31 A（平台安全凭据、秘密不进普通文件）、D-20 A（分层本地存储）
- 关联 ADR：ADR-031
- 执行人：B
- 日期：2026-11-18
- 状态：**部分完成**——DPAPI 用户作用域跨用户不可解密、数据目录隔离已验证；凭据管理器写入、重装/换机、日志扫描未验证

## 1 目的

验证 D-31 A / D-20 A 在 Windows 上的关键边界：

1. 用户 A 用 DPAPI（CurrentUser）加密的秘密，用户 B 无法解密；
2. 用户 B 无法读取用户 A 的本地数据目录与 DPAPI 主密钥目录；
3. 被复制的密文文件仍不可跨用户使用；
4. 记录「工作区目录对其他本地用户可见」这一实际风险。

## 2 环境配置快照

| 项 | 值 |
|---|---|
| 主机/系统 | HYC-1 / Build 26300.9974 |
| 用户 A | `HYC-1`（执行加密） |
| 用户 B | `readerlab`（第二本地用户，普通用户；测试密码由负责人保管，不写入本报告） |
| 探针脚本 | `experiments/windows-env-probe/probe_dpapi_crossuser.ps1`、`probe_user_isolation.ps1` |
| 共享目录 | `C:\Users\Public\we10`（All Users 可读写，用于传递密文） |
| 加密 API | `System.Security.Cryptography.ProtectedData`（DPAPI，CurrentUser） |
| 启动方式 | `Start-Process -Credential readerlab -LoadUserProfile`（需创建其他用户令牌） |

## 3 步骤

1. 用户 A 加密固定令牌 → `secret-a.bin`（246 字节，SHA-256 `8BBFAD2A…C883D6`）；
2. 用户 B 读取同一文件并尝试 `Unprotect`；
3. 用户 B 做一次自己的 DPAPI 往返（证明其 DPAPI 本身可用）；
4. 用户 B 列出用户 A 的 profile、`AppData\Local`、`AppData\Roaming\Microsoft\Protect` 以及项目工作区 `docs`。

## 4 结果

### 4.1 跨用户解密

| 检查 | 用户 A（HYC-1） | 用户 B（readerlab） |
|---|---|---|
| 密文文件存在 | 是（246 B） | 是（可读） |
| 解密结果 | 成功（自身） | **失败**：`MethodInvocationException`（`A_SECRET_NOT_DECRYPTABLE_EXPECTED`） |
| 自身 DPAPI 往返 | — | **成功**（`ownRoundTrip=true`） |
| 本地数据目录 | `C:\Users\HYC-1\AppData\Local` | `C:\Users\readerlab\AppData\Local` |

### 4.2 数据目录隔离（以 readerlab 身份）

| 目标 | 存在 | 可列目录 | 异常 |
|---|---|---|---|
| `C:\Users\HYC-1` | 是 | **否** | `UnauthorizedAccessException` |
| `C:\Users\HYC-1\AppData\Local` | 访问被拒 | **否** | `UnauthorizedAccessException` |
| `C:\Users\HYC-1\AppData\Roaming\Microsoft\Protect` | 访问被拒 | **否** | `UnauthorizedAccessException` |
| `F:\#_Working_Area\Intelligent-Reader\docs` | 是 | **是（可读）** | — |

## 5 判定（对照 W-E10 通过证据）

| 通过证据 | 是否取得 | 说明 |
|---|---|---|
| 跨用户不可解密 | ✅ | B 无法解 A 的 DPAPI 密文 |
| 数据目录隔离 | ✅ | B 无法列出 A 的 profile 与 DPAPI 目录 |
| 凭据不在普通数据文件 | ✅（机制层） | 密文文件本身不含明文；但应用是否把秘密写入配置/日志未扫描 |
| 用户切换/退出/撤销流程 | ❌ | 需 UI 与账号流程 |
| 日志与崩溃转储扫描 | ❌ | 需客户端与故障注入 |
| Credential Manager 条目 | ❌ | 未写入（避免留下凭据痕迹） |
| 重装/换机不可解 | ❌ | 需重装或另一台机器 |

## 6 重要发现

1. **工作区目录对其他本地用户可读**：`readerlab` 可以列出 `F:\#_Working_Area\Intelligent-Reader\docs`。这是开发目录 ACL 的现状（Authenticated Users 可读），不是应用数据目录；但提醒：
   - 不要把测试密码、令牌、密钥放入工作区（含 `_local/`）；
   - 应用数据必须放 `%LOCALAPPDATA%`（已验证对其他用户不可见），不能复用工作区路径。
2. **`Start-Process -Credential` 的异常表现**：命令返回 `Access is denied`，但子进程实际已启动并产出结果文件；后续跨用户脚本应以「结果文件是否生成」为准，不以 `Start-Process` 的返回码为准。本报告已按实际结果判定。
3. **测试账户应清理**：`readerlab` 为测试账户，密码弱（用户已知），建议 W-E10 全部完成后删除或改密（`net user readerlab /delete`）。

## 7 结论

- **原始事实（F）**：A 的 246 字节 DPAPI 密文被 B 读取后解密失败；B 自身 DPAPI 往返正常；B 无法列出 A 的 profile/Local/Protect；B 可读项目 `docs` 目录。
- **项目组解释（I）**：DPAPI 用户作用域提供了跨用户隔离，满足 D-31 A 的「平台安全凭据」方向；但「工作区/普通目录」不是保护边界，应用数据必须落在用户私有目录，秘密不得写入工作区、日志或诊断包。
- **技术/需求结论（C）**：W-E10 的凭据隔离层成立；ADR-031 仍为 Proposed，缺凭据写入、日志扫描、重装/换机与退出撤销证据。

## 8 回填动作

1. 证据索引 §3：W-E10 状态改为「部分完成（跨用户隔离）」。
2. 调研文档 §9.2 W-E10、§9.3 D-31 映射更新。
3. 提醒 A：不要把测试密码写入交付材料；建议删除/改密测试账户。
