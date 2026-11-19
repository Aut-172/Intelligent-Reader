# W-E10 cross-user DPAPI probe (ASCII only).
# Mode EncryptAsA: encrypt a fixed token with DPAPI CurrentUser and write secret-a.bin.
# Mode DecryptAsB: attempt to decrypt A's file as the current user; also do an own round-trip.
param(
    [ValidateSet('EncryptAsA', 'DecryptAsB')] [string]$Mode,
    [string]$Path = 'C:\Users\Public\we10'
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Security
New-Item -ItemType Directory -Force -Path $Path | Out-Null

$token = 'we10-cross-user-secret-token'
$bytes = [System.Text.Encoding]::UTF8.GetBytes($token)
$result = [ordered]@{
    mode      = $Mode
    user      = $env:USERNAME
    time      = (Get-Date).ToString('o')
    success   = $false
    errorType = $null
    notes     = @()
}

if ($Mode -eq 'EncryptAsA') {
    $cipher = [System.Security.Cryptography.ProtectedData]::Protect(
        $bytes, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    $secretFile = Join-Path $Path 'secret-a.bin'
    [System.IO.File]::WriteAllBytes($secretFile, $cipher)
    $result['success'] = $true
    $result['cipherBytes'] = $cipher.Length
    $result['sha256'] = (Get-FileHash -LiteralPath $secretFile -Algorithm SHA256).Hash
}
else {
    $secretFile = Join-Path $Path 'secret-a.bin'
    $result['fileExists'] = (Test-Path -LiteralPath $secretFile)
    try {
        $cipher = [System.IO.File]::ReadAllBytes($secretFile)
        $plain = [System.Security.Cryptography.ProtectedData]::Unprotect(
            $cipher, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
        $result['success'] = $true
        $result['notes'] += 'A_SECRET_DECRYPTED_UNEXPECTED'
    }
    catch {
        $result['errorType'] = $_.Exception.GetType().FullName
        $result['notes'] += 'A_SECRET_NOT_DECRYPTABLE_EXPECTED'
    }
    try {
        $own = [System.Security.Cryptography.ProtectedData]::Protect(
            $bytes, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
        $ownPlain = [System.Security.Cryptography.ProtectedData]::Unprotect(
            $own, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
        $result['ownRoundTrip'] = ([System.Text.Encoding]::UTF8.GetString($ownPlain) -eq $token)
    }
    catch {
        $result['ownRoundTrip'] = $false
    }
    $result['localAppData'] = $env:LOCALAPPDATA
}

$json = $result | ConvertTo-Json -Depth 4
$json | Set-Content -LiteralPath (Join-Path $Path ("result-{0}.json" -f $Mode)) -Encoding UTF8
Write-Output $json
