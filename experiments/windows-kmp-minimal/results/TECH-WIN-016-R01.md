# MSI 签名与验签尝试（环境受阻）

- 实验编号：W-E13 签名补充（结果编号 `TECH-WIN-016-R01`）
- 关联：D-34/D-35、`TECH-WIN-014-R02`
- 执行人：B
- 日期：2026-11-18
- 状态：**未完成**——signtool 已取得，但当前会话无法访问证书私钥（`NTE_PERM 0x80090010`）

## 1 目的

验证 Windows 安装包的代码签名与验签流程，补齐 W-E13 的签名/SmartScreen 维度。

## 2 过程

| 步骤 | 结果 |
|---|---|
| 获取 signtool | 从 NuGet `Microsoft.Windows.SDK.BuildTools 10.0.26100.1742` 解压，`signtool.exe` 位于 `bin\10.0.26100.0\x64`，可运行 |
| 生成自签名代码签名证书 | `New-SelfSignedCertificate -Type CodeSigningCert` 成功（`CN=B Windows Test Signing`） |
| 用证书存储签名 | `signtool sign /sha1 … /fd SHA256` → **Access is denied** |
| 用 PFX 签名 | 导出 PFX 后 `signtool sign /f … /p …` → `Store::ImportCertObject() failed (0x80090010 NTE_PERM)` |
| 验签 | 无签名可验（`No signature found`） |

## 3 判定

| 维度 | 结果 |
|---|---|
| signtool 可用 | ✅（版本 10.0.26100） |
| 签名命令/参数可用 | ✅（已确定 `/fd SHA256`、PFX 路径、时间戳参数） |
| 实际签名 | ❌ 当前会话无证书私钥访问权限（NTE_PERM） |
| 验签/时间戳 | ❌ 未进入该步骤 |
| SmartScreen | ❌ 未验证（自签名即便成功也不受信任） |

## 4 结论

- **原始事实（F）**：signtool 可执行；自签名证书可创建；但在当前 DSH 会话中签名返回 0x80090010（NTE_PERM），无法完成签名。
- **项目组解释（I）**：签名需要正常用户会话下的证书私钥访问权限；沙箱/低完整性会话与证书存储 ACL 冲突。生产签名还需受信任 CA 证书，自签名不能消除 SmartScreen 警告。
- **技术/需求结论（C）**：签名流程与参数已确认，但本轮未取得签名/验签证据；W-E13 的签名维度保持缺口，需在普通管理员会话中重做。

> 复跑建议：在正常登录会话执行
> `signtool sign /f <pfx> /p <pwd> /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 <msi>`，
> 再 `signtool verify /pa /v <msi>`；生产包应使用 CA 颁发的代码签名证书。
