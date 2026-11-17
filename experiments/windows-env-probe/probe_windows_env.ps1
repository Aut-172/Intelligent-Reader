# Windows platform fact probe (architecture research role B).
# Measures facts the Windows desktop shell depends on. No downloads, no cloud calls,
# no registry/system-policy writes. Result JSON is written to the platform temp area
# (the sandboxed process has Low integrity and may only write to Low-labeled paths).
# Usage: powershell.exe -NoProfile -ExecutionPolicy Bypass -File probe_windows_env.ps1

[CmdletBinding()]
param(
    [string]$OutFile
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSCommandPath
$outDir = Join-Path $env:TEMP 'reader-win-probe'
if (-not $OutFile) { $OutFile = Join-Path $root 'results\TECH-WIN-001-R01.json' }
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$tmp = Join-Path $outDir 'tmp'
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

function Invoke-Check {
    param([string]$Id, [string]$Name, [scriptblock]$Body)
    $record = [ordered]@{
        id = $Id; check = $Name; status = 'observed'; value = $null; error = $null; errorType = $null
    }
    try {
        $record.value = & $Body
    } catch {
        $record.status = 'error'
        $record.error = ($_.Exception.Message -replace '\s+', ' ').Trim()
        $record.errorType = $_.Exception.GetType().FullName
    }
    return [pscustomobject]$record
}

$checks = [System.Collections.Generic.List[object]]::new()

# ---------- TECH-WIN-001 environment matrix ----------
$checks.Add((Invoke-Check 'WIN-001-01' 'OS version and build' {
    $cv = Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion'
    [ordered]@{
        productName = $cv.ProductName
        displayVersion = $cv.DisplayVersion
        currentBuild = "$($cv.CurrentBuild).$($cv.UBR)"
        editionId = $cv.EditionID
        buildNumber = [int]$cv.CurrentBuild
        isWin11Build = ([int]$cv.CurrentBuild -ge 22000)
    }
}))

$checks.Add((Invoke-Check 'WIN-001-02' 'Architecture and PowerShell host' {
    [ordered]@{
        processorArchitecture = $env:PROCESSOR_ARCHITECTURE
        processorIdentifier = $env:PROCESSOR_IDENTIFIER
        psVersion = $PSVersionTable.PSVersion.ToString()
        is64BitProcess = [Environment]::Is64BitProcess
        integrityLevel = (whoami /groups | Select-String -Pattern 'Mandatory Label' | ForEach-Object { $_.ToString().Trim() })
    }
}))

$checks.Add((Invoke-Check 'WIN-001-03' 'Long path switch' {
    $v = Get-ItemProperty 'HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem' -Name LongPathsEnabled
    [ordered]@{ longPathsEnabled = [int]$v.LongPathsEnabled; appManifestStillRequired = $true }
}))

$checks.Add((Invoke-Check 'WIN-001-04' 'WebView2 runtime' {
    $paths = @(
        'HKLM:\SOFTWARE\WOW6432Node\Microsoft\EdgeUpdate\Clients\{F3017226-FE2A-4295-8BDF-00C3A9A7E4C5}',
        'HKLM:\SOFTWARE\Microsoft\EdgeUpdate\Clients\{F3017226-FE2A-4295-8BDF-00C3A9A7E4C5}',
        'HKCU:\SOFTWARE\Microsoft\EdgeUpdate\Clients\{F3017226-FE2A-4295-8BDF-00C3A9A7E4C5}'
    )
    $found = @()
    foreach ($p in $paths) {
        $item = Get-ItemProperty $p -ErrorAction SilentlyContinue
        if ($item -and $item.pv) { $found += [ordered]@{ key = $p; version = $item.pv } }
    }
    [ordered]@{ installed = ($found.Count -gt 0); entries = $found }
}))

$checks.Add((Invoke-Check 'WIN-001-05' 'Desktop runtimes present (.NET / JVM / Node)' {
    $dotnetRelease = $null
    try { $dotnetRelease = (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full' -Name Release).Release } catch { }
    [ordered]@{
        dotnetFrameworkRelease = $dotnetRelease
        dotnetCli = $(if (Get-Command dotnet -ErrorAction SilentlyContinue) { (Get-Command dotnet).Source } else { $null })
        javaOnPath = $(if (Get-Command java -ErrorAction SilentlyContinue) { (Get-Command java).Source } else { $null })
        javaHome = $env:JAVA_HOME
        nodeOnPath = $(if (Get-Command node -ErrorAction SilentlyContinue) { (Get-Command node).Source } else { $null })
    }
}))

$checks.Add((Invoke-Check 'WIN-001-06' 'Credential storage entry points' {
    [ordered]@{
        cmdkey = (Get-Command cmdkey -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source)
        credentialVaultDir = (Test-Path "$env:APPDATA\Microsoft\Credentials")
        dpapiProtectDir = (Test-Path "$env:APPDATA\Microsoft\Protect")
    }
}))

$checks.Add((Invoke-Check 'WIN-001-07' 'User context and data directories' {
    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object System.Security.Principal.WindowsPrincipal($identity)
    [ordered]@{
        user = $identity.Name
        isElevated = $principal.IsInRole([System.Security.Principal.WindowsBuiltInRole]::Administrator)
        localAppData = $env:LOCALAPPDATA
        programFiles = $env:ProgramFiles
        temp = $env:TEMP
    }
}))

# ---------- TECH-WIN-003 display and scaling ----------
$checks.Add((Invoke-Check 'WIN-003-01' 'Displays, resolution and system DPI' {
    $screens = @()
    try {
        Add-Type -AssemblyName System.Windows.Forms -ErrorAction Stop
        $screens = @([System.Windows.Forms.Screen]::AllScreens | ForEach-Object {
            [ordered]@{ device = $_.DeviceName; width = $_.Bounds.Width; height = $_.Bounds.Height; primary = $_.Primary }
        })
    } catch { }
    $appliedDpi = $null
    try { $appliedDpi = (Get-ItemProperty 'HKCU:\Control Panel\Desktop\WindowMetrics' -Name AppliedDPI -ErrorAction Stop).AppliedDPI } catch { }
    $logPixels = $null
    try { $logPixels = (Get-ItemProperty 'HKCU:\Control Panel\Desktop' -Name LogPixels -ErrorAction Stop).LogPixels } catch { }
    [ordered]@{
        screens = $screens
        screenCount = $screens.Count
        appliedDpi = $appliedDpi
        logPixels = $logPixels
        scalePercentFromAppliedDpi = if ($appliedDpi) { [math]::Round(100 * $appliedDpi / 96) } else { $null }
    }
}))

# ---------- TECH-WIN-002 path identity and access semantics ----------
$checks.Add((Invoke-Check 'WIN-002-01' 'Path access: write, read, hash' {
    $dirA = Join-Path $tmp 'a'
    New-Item -ItemType Directory -Force -Path $dirA | Out-Null
    $file = Join-Path $dirA 'sample.txt'
    Set-Content -LiteralPath $file -Value 'windows-probe-v1' -Encoding UTF8 -NoNewline
    [ordered]@{
        path = $file
        exists = (Test-Path -LiteralPath $file)
        sha256 = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
        contentAsRead = [System.IO.File]::ReadAllText($file)
    }
}))

$checks.Add((Invoke-Check 'WIN-002-02' 'Read access after the file is moved' {
    $dirA = Join-Path $tmp 'a'
    $dirB = Join-Path $tmp 'b'
    New-Item -ItemType Directory -Force -Path $dirB | Out-Null
    $src = Join-Path $dirA 'sample.txt'
    $dst = Join-Path $dirB 'sample-moved.txt'
    Move-Item -LiteralPath $src -Destination $dst -Force
    $oldExists = Test-Path -LiteralPath $src
    $oldReadable = $true
    $oldError = $null
    try { Get-Content -LiteralPath $src -Raw -ErrorAction Stop | Out-Null } catch { $oldReadable = $false; $oldError = $_.Exception.GetType().Name }
    [ordered]@{
        oldPath = $src
        newPath = $dst
        oldPathExists = $oldExists
        oldPathReadable = $oldReadable
        oldPathError = $oldError
        newPathReadable = (Test-Path -LiteralPath $dst)
        contentPreserved = ([System.IO.File]::ReadAllText($dst) -eq 'windows-probe-v1')
    }
}))

$checks.Add((Invoke-Check 'WIN-002-03' 'Reopen by stale path (old grant or old index)' {
    $src = Join-Path $tmp 'a\sample.txt'
    $result = [ordered]@{ failed = $null; errorType = $null }
    try {
        [System.IO.File]::OpenRead($src).Dispose()
        $result.failed = $false
    } catch {
        $result.failed = $true
        $result.errorType = $_.Exception.GetType().FullName
        $result.innerErrorType = if ($_.Exception.InnerException) { $_.Exception.InnerException.GetType().FullName } else { $null }
    }
    $result
}))

$checks.Add((Invoke-Check 'WIN-002-04' 'Read-only attribute blocks overwrite' {
    $file = Join-Path $tmp 'b\sample-moved.txt'
    (Get-Item -LiteralPath $file).IsReadOnly = $true
    $blocked = $false
    $errorType = $null
    $hashBefore = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
    try { Set-Content -LiteralPath $file -Value 'windows-probe-v2' -Encoding UTF8 -NoNewline -ErrorAction Stop }
    catch { $blocked = $true; $errorType = $_.Exception.GetType().Name }
    $hashAfter = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
    (Get-Item -LiteralPath $file).IsReadOnly = $false
    [ordered]@{
        writeBlocked = $blocked
        errorType = $errorType
        hashUnchanged = ($hashBefore -eq $hashAfter)
        contentStillV1 = ([System.IO.File]::ReadAllText($file) -eq 'windows-probe-v1')
    }
}))

$checks.Add((Invoke-Check 'WIN-002-05' 'Share-mode lock blocks concurrent writer' {
    $file = Join-Path $tmp 'b\sample-moved.txt'
    $first = [System.IO.File]::Open($file, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::None)
    $blocked = $false
    $errorType = $null
    try {
        $second = [System.IO.File]::Open($file, [System.IO.FileMode]::Open, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::ReadWrite)
        $second.Dispose()
    } catch {
        $blocked = $true
        $errorType = $_.Exception.GetType().FullName
        $innerErrorType = if ($_.Exception.InnerException) { $_.Exception.InnerException.GetType().FullName } else { $null }
    }
    $first.Dispose()
    [ordered]@{ secondOpenBlocked = $blocked; errorType = $errorType; innerErrorType = $innerErrorType }
}))

$checks.Add((Invoke-Check 'WIN-002-06' 'Overwrite keeps no automatic previous version' {
    $file = Join-Path $tmp 'b\sample-moved.txt'
    Set-Content -LiteralPath $file -Value 'windows-probe-v3' -Encoding UTF8 -NoNewline
    $siblings = @(Get-ChildItem -LiteralPath (Split-Path -Parent $file) -File | Select-Object -ExpandProperty Name)
    [ordered]@{
        contentAsRead = [System.IO.File]::ReadAllText($file)
        siblingFiles = $siblings
        autoBackupCreated = ($siblings.Count -gt 1)
    }
}))

$checks.Add((Invoke-Check 'WIN-002-07' 'Delete removes the path and the content' {
    $file = Join-Path $tmp 'b\sample-moved.txt'
    Remove-Item -LiteralPath $file -Force
    $exists = Test-Path -LiteralPath $file
    $openFailed = $false
    try { [System.IO.File]::OpenRead($file).Dispose() } catch { $openFailed = $true }
    [ordered]@{ existsAfterDelete = $exists; openFails = $openFailed }
}))

# ---------- summary ----------
$document = [ordered]@{
    experimentId = 'TECH-WIN-001-R01'
    date = (Get-Date).ToString('yyyy-MM-ddTHH:mm:ssK')
    host = $env:COMPUTERNAME
    script = 'experiments/windows-env-probe/probe_windows_env.ps1'
    outputPath = $OutFile
    boundary = 'Platform facts only. No dependency install, no cloud call, no system-policy change. Single host, single run; not generalised to all Windows 11 builds.'
    checks = $checks
}

$json = $document | ConvertTo-Json -Depth 8
try {
    Set-Content -LiteralPath $OutFile -Value $json -Encoding UTF8 -ErrorAction Stop
} catch {
    # The agent sandbox runs at Low integrity and may not write the workspace.
    # Fall back to the platform temp area and say so; the observations are the same.
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $fallback = Join-Path $outDir (Split-Path -Leaf $OutFile)
    $document.outputPath = $fallback
    $json = $document | ConvertTo-Json -Depth 8
    Set-Content -LiteralPath $fallback -Value $json -Encoding UTF8
    Write-Output ("NOTE workspace not writable from this process; raw JSON written to {0}" -f $fallback)
}
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }

foreach ($c in $checks) {
    if ($c.status -eq 'observed') { Write-Output ("[OK]  {0} {1}" -f $c.id, $c.check) }
    else { Write-Output ("[ERR] {0} {1} :: {2}" -f $c.id, $c.check, $c.errorType) }
}
Write-Output ("RESULT {0} checks -> {1}" -f $checks.Count, $document.outputPath)
