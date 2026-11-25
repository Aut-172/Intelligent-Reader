# Windows file identity probe (role B, D-16 A / D-25 A input).
# Measures how path, content hash and timestamps behave under copy, rename/move,
# content change and mtime change. No downloads, no cloud calls, no system changes.
# Raw JSON goes to the workspace results dir when writable; otherwise to %TEMP%.

[CmdletBinding()]
param([string]$OutFile)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSCommandPath
if (-not $OutFile) { $OutFile = Join-Path $root 'results\TECH-WIN-002-R01.json' }
$outDir = Join-Path $env:TEMP 'reader-win-probe'
$tmp = Join-Path $outDir 'identity-tmp'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

function Get-Facts {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return $null }
    $i = Get-Item -LiteralPath $Path
    [ordered]@{
        path = $i.FullName
        exists = $true
        size = $i.Length
        lastWriteUtc = $i.LastWriteTimeUtc.ToString('o')
        sha256 = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
    }
}

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

$checks.Add((Invoke-Check 'ID-01' 'Baseline file facts' {
    $dirA = Join-Path $tmp 'a'
    New-Item -ItemType Directory -Force -Path $dirA | Out-Null
    $file = Join-Path $dirA 'doc-v1.txt'
    Set-Content -LiteralPath $file -Value "chapter one`nline two`n" -Encoding UTF8 -NoNewline
    Get-Facts $file
}))

$checks.Add((Invoke-Check 'ID-02' 'Copy to a different folder and name' {
    $dst = Join-Path $tmp 'b\copy-of-doc.txt'
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $dst) | Out-Null
    Copy-Item -LiteralPath (Join-Path $tmp 'a\doc-v1.txt') -Destination $dst -Force
    $src = Get-Facts (Join-Path $tmp 'a\doc-v1.txt')
    $cp = Get-Facts $dst
    [ordered]@{ sameHash = ($src.sha256 -eq $cp.sha256); srcHash = $src.sha256; copyPath = $cp.path; copyHash = $cp.sha256 }
}))

$checks.Add((Invoke-Check 'ID-03' 'Move and rename the original' {
    $src = Join-Path $tmp 'a\doc-v1.txt'
    $dst = Join-Path $tmp 'a\renamed-doc.txt'
    $before = Get-Facts $src
    Move-Item -LiteralPath $src -Destination $dst -Force
    $after = Get-Facts $dst
    [ordered]@{
        oldPathExists = (Test-Path -LiteralPath $src)
        newPath = $after.path
        hashPreserved = ($before.sha256 -eq $after.sha256)
        hash = $after.sha256
    }
}))

$checks.Add((Invoke-Check 'ID-04' 'Touch mtime with identical bytes' {
    $file = Join-Path $tmp 'a\renamed-doc.txt'
    $before = Get-Facts $file
    (Get-Item -LiteralPath $file).LastWriteTimeUtc = (Get-Date).ToUniversalTime().AddDays(1)
    $after = Get-Facts $file
    [ordered]@{
        hashPreserved = ($before.sha256 -eq $after.sha256)
        mtimeChanged = ($before.lastWriteUtc -ne $after.lastWriteUtc)
        beforeMtime = $before.lastWriteUtc
        afterMtime = $after.lastWriteUtc
    }
}))

$checks.Add((Invoke-Check 'ID-05' 'Content change at the same path' {
    $file = Join-Path $tmp 'a\renamed-doc.txt'
    $before = Get-Facts $file
    Set-Content -LiteralPath $file -Value "chapter one`nline two edited`n" -Encoding UTF8 -NoNewline
    $after = Get-Facts $file
    [ordered]@{
        samePath = ($before.path -eq $after.path)
        hashChanged = ($before.sha256 -ne $after.sha256)
        sizeChanged = ($before.size -ne $after.size)
        beforeHash = $before.sha256
        afterHash = $after.sha256
    }
}))

$checks.Add((Invoke-Check 'ID-06' 'Replace file with another file of the same name' {
    $path = Join-Path $tmp 'a\renamed-doc.txt'
    $before = Get-Facts $path
    $other = Join-Path $tmp 'other-source.txt'
    Set-Content -LiteralPath $other -Value "a completely different document`n" -Encoding UTF8 -NoNewline
    Copy-Item -LiteralPath $other -Destination $path -Force
    $after = Get-Facts $path
    [ordered]@{
        pathUnchanged = ($before.path -eq $after.path)
        hashChanged = ($before.sha256 -ne $after.sha256)
        sizeChanged = ($before.size -ne $after.size)
    }
}))

$checks.Add((Invoke-Check 'ID-07' 'Same bytes under two names and folders' {
    $p1 = Join-Path $tmp 'a\renamed-doc.txt'
    $p2 = Join-Path $tmp 'c\same-bytes-other-name.txt'
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $p2) | Out-Null
    Copy-Item -LiteralPath $p1 -Destination $p2 -Force
    $f1 = Get-Facts $p1
    $f2 = Get-Facts $p2
    [ordered]@{ sameHash = ($f1.sha256 -eq $f2.sha256); hash = $f1.sha256; path1 = $f1.path; path2 = $f2.path }
}))

$checks.Add((Invoke-Check 'ID-08' 'Line-ending difference (CRLF vs LF) at same logical text' {
    $lf = Join-Path $tmp 'eol\lf.txt'
    $crlf = Join-Path $tmp 'eol\crlf.txt'
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $lf) | Out-Null
    [System.IO.File]::WriteAllText($lf, "a`nb`n", (New-Object System.Text.UTF8Encoding($false)))
    [System.IO.File]::WriteAllText($crlf, "a`r`nb`r`n", (New-Object System.Text.UTF8Encoding($false)))
    $f1 = Get-Facts $lf
    $f2 = Get-Facts $crlf
    [ordered]@{
        sameHash = ($f1.sha256 -eq $f2.sha256)
        lfHash = $f1.sha256
        crlfHash = $f2.sha256
        lfSize = $f1.size
        crlfSize = $f2.size
    }
}))

$checks.Add((Invoke-Check 'ID-09' 'Hash stability across repeated reads' {
    $file = Join-Path $tmp 'a\renamed-doc.txt'
    $h1 = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
    Start-Sleep -Milliseconds 50
    $h2 = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
    [ordered]@{ stable = ($h1 -eq $h2); hash = $h1 }
}))

$checks.Add((Invoke-Check 'ID-10' 'Delete then re-create with identical bytes' {
    $file = Join-Path $tmp 'a\renamed-doc.txt'
    $before = Get-Facts $file
    Remove-Item -LiteralPath $file -Force
    Set-Content -LiteralPath $file -Value "a completely different document`n" -Encoding UTF8 -NoNewline
    $after = Get-Facts $file
    [ordered]@{
        recreated = $after.exists
        hashRestored = ($before.sha256 -eq $after.sha256)
        beforeHash = $before.sha256
        afterHash = $after.sha256
    }
}))

$document = [ordered]@{
    experimentId = 'TECH-WIN-002-R01'
    date = (Get-Date).ToString('yyyy-MM-ddTHH:mm:ssK')
    host = $env:COMPUTERNAME
    script = 'experiments/windows-env-probe/probe_windows_identity.ps1'
    outputPath = $OutFile
    boundary = 'File identity semantics only (hash/size/mtime/path). No user switch, no volume-level file ID, no cloud placeholders. Single host, single run.'
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
