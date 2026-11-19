# Windows platform credential-boundary probe (role B, D-31 A / W-E10 input).
# Measures DPAPI user-scope protect/unprotect, plaintext leakage into the ciphertext file,
# tamper detection, entropy binding and ciphertext copy behaviour.
# No downloads, no cloud calls, no system-policy change. Does not touch Credential Manager.

[CmdletBinding()]
param([string]$OutFile)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSCommandPath
if (-not $OutFile) { $OutFile = Join-Path $root 'results\TECH-WIN-003-R01.json' }
$outDir = Join-Path $env:TEMP 'reader-win-probe'
$tmp = Join-Path $outDir 'dpapi-tmp'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

Add-Type -AssemblyName System.Security

$token = 'reader-probe-token-7f3c9a2e-DO-NOT-LEAK'
$tokenBytes = [System.Text.Encoding]::UTF8.GetBytes($token)
$entropy = [System.Text.Encoding]::UTF8.GetBytes('reader-probe-entropy')

function Invoke-Check {
    param([string]$Id, [string]$Name, [scriptblock]$Body)
    $r = [ordered]@{ id = $Id; check = $Name; status = 'observed'; value = $null; error = $null; errorType = $null }
    try { $r.value = & $Body }
    catch {
        $r.status = 'error'
        $r.error = ($_.Exception.Message -replace '\s+', ' ').Trim()
        $r.errorType = $_.Exception.GetType().FullName
    }
    [pscustomobject]$r
}

$checks = [System.Collections.Generic.List[object]]::new()
$script:cipher = $null

$checks.Add((Invoke-Check 'DP-01' 'DPAPI protect (CurrentUser, with entropy)' {
    $c = [System.Security.Cryptography.ProtectedData]::Protect($tokenBytes, $entropy, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    $script:cipher = $c
    [ordered]@{ plaintextBytes = $tokenBytes.Length; cipherBytes = $c.Length; base64Prefix = ([Convert]::ToBase64String($c)).Substring(0, 24) }
}))

$checks.Add((Invoke-Check 'DP-02' 'Ciphertext file contains no plaintext (ASCII or UTF-16)' {
    $file = Join-Path $tmp 'cred.bin'
    [System.IO.File]::WriteAllBytes($file, $script:cipher)
    $bytes = [System.IO.File]::ReadAllBytes($file)
    $ascii = [System.Text.Encoding]::ASCII.GetString($bytes)
    $utf16 = [System.Text.Encoding]::Unicode.GetString($bytes)
    [ordered]@{
        fileBytes = $bytes.Length
        containsAsciiToken = $ascii.Contains($token)
        containsUtf16Token = $utf16.Contains($token)
        filePath = $file
    }
}))

$checks.Add((Invoke-Check 'DP-03' 'Unprotect recovers the token' {
    $plain = [System.Security.Cryptography.ProtectedData]::Unprotect($script:cipher, $entropy, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    $text = [System.Text.Encoding]::UTF8.GetString($plain)
    [ordered]@{ recovered = ($text -eq $token); length = $plain.Length }
}))

$checks.Add((Invoke-Check 'DP-04' 'Copying the ciphertext file keeps it decryptable by the same user' {
    $file = Join-Path $tmp 'cred.bin'
    $copy = Join-Path $tmp 'cred-copy.bin'
    Copy-Item -LiteralPath $file -Destination $copy -Force
    $bytes = [System.IO.File]::ReadAllBytes($copy)
    $plain = [System.Security.Cryptography.ProtectedData]::Unprotect($bytes, $entropy, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    [ordered]@{ copyDecrypts = ([System.Text.Encoding]::UTF8.GetString($plain) -eq $token); copyPath = $copy }
}))

$checks.Add((Invoke-Check 'DP-05' 'Tampered ciphertext fails to decrypt' {
    $file = Join-Path $tmp 'cred.bin'
    $bytes = [System.IO.File]::ReadAllBytes($file)
    $bytes[$bytes.Length - 5] = $bytes[$bytes.Length - 5] -bxor 0xFF
    $failed = $false
    $errorType = $null
    try { [System.Security.Cryptography.ProtectedData]::Unprotect($bytes, $entropy, [System.Security.Cryptography.DataProtectionScope]::CurrentUser) | Out-Null }
    catch { $failed = $true; $errorType = $_.Exception.GetType().FullName }
    [ordered]@{ decryptFailed = $failed; errorType = $errorType }
}))

$checks.Add((Invoke-Check 'DP-06' 'Wrong entropy fails to decrypt' {
    $file = Join-Path $tmp 'cred.bin'
    $bytes = [System.IO.File]::ReadAllBytes($file)
    $wrong = [System.Text.Encoding]::UTF8.GetBytes('different-entropy')
    $failed = $false
    $errorType = $null
    try { [System.Security.Cryptography.ProtectedData]::Unprotect($bytes, $wrong, [System.Security.Cryptography.DataProtectionScope]::CurrentUser) | Out-Null }
    catch { $failed = $true; $errorType = $_.Exception.GetType().FullName }
    [ordered]@{ decryptFailed = $failed; errorType = $errorType }
}))

$checks.Add((Invoke-Check 'DP-07' 'Two encryptions of the same token differ' {
    $a = [System.Security.Cryptography.ProtectedData]::Protect($tokenBytes, $entropy, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    $b = [System.Security.Cryptography.ProtectedData]::Protect($tokenBytes, $entropy, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    [ordered]@{ ciphertextsDiffer = (-not [System.Linq.Enumerable]::SequenceEqual([byte[]]$a, [byte[]]$b)); lengthA = $a.Length; lengthB = $b.Length }
}))

$checks.Add((Invoke-Check 'DP-08' 'Machine-scope protect is available to this user' {
    $c = [System.Security.Cryptography.ProtectedData]::Protect($tokenBytes, $null, [System.Security.Cryptography.DataProtectionScope]::LocalMachine)
    $plain = [System.Security.Cryptography.ProtectedData]::Unprotect($c, $null, [System.Security.Cryptography.DataProtectionScope]::LocalMachine)
    [ordered]@{ machineScopeWorks = ([System.Text.Encoding]::UTF8.GetString($plain) -eq $token); cipherBytes = $c.Length }
}))

$checks.Add((Invoke-Check 'DP-09' 'Delete the ciphertext file' {
    $file = Join-Path $tmp 'cred.bin'
    Remove-Item -LiteralPath $file -Force
    [ordered]@{ existsAfterDelete = (Test-Path -LiteralPath $file) }
}))

$checks.Add((Invoke-Check 'DP-10' 'Data-protection directory is per user profile' {
    $protectDir = Join-Path $env:APPDATA 'Microsoft\Protect'
    [ordered]@{ exists = (Test-Path -LiteralPath $protectDir); path = $protectDir }
}))

$document = [ordered]@{
    experimentId = 'TECH-WIN-003-R01'
    date = (Get-Date).ToString('yyyy-MM-ddTHH:mm:ssK')
    host = $env:COMPUTERNAME
    script = 'experiments/windows-env-probe/probe_windows_dpapi.ps1'
    outputPath = $OutFile
    boundary = 'DPAPI user/machine scope only. No other user account, no reinstall, no Credential Manager write, no backup/restore test. Single host, single run.'
    checks = $checks
}
$json = $document | ConvertTo-Json -Depth 8
try {
    Set-Content -LiteralPath $OutFile -Value $json -Encoding UTF8 -ErrorAction Stop
} catch {
    $fallback = Join-Path $outDir (Split-Path -Leaf $OutFile)
    $document.outputPath = $fallback
    $json = $document | ConvertTo-Json -Depth 8
    Set-Content -LiteralPath $fallback -Value $json -Encoding UTF8
    Write-Output ("NOTE workspace not writable; raw JSON written to {0}" -f $fallback)
}
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
foreach ($c in $checks) {
    if ($c.status -eq 'observed') { Write-Output ("[OK]  {0} {1}" -f $c.id, $c.check) }
    else { Write-Output ("[ERR] {0} {1} :: {2}" -f $c.id, $c.check, $c.errorType) }
}
Write-Output ("RESULT {0} checks -> {1}" -f $checks.Count, $document.outputPath)
