$ErrorActionPreference = 'Continue'
$outDir = 'F:\#_Working_Area\Intelligent-Reader\experiments\windows-env-probe\results'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$json = Join-Path $outDir 'TECH-WIN-025-logscan.json'

$patterns = [ordered]@{
    openai_key = 'sk-[A-Za-z0-9]{20,}'
    bearer = 'Bearer\s+[A-Za-z0-9\-\._~\+/]+=*'
    api_key = 'api[_-]?key'
    password = 'password\s*[:=]'
    private_key = 'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY'
    user_path = 'C:\\Users\\[^\\\s]+'
}

$roots = @()
$roots += Join-Path $env:LOCALAPPDATA 'ReaderWinProbe'
$roots += Join-Path $env:LOCALAPPDATA 'CrashDumps'
$roots += 'C:\ProgramData\Microsoft\Windows\WER\ReportArchive'
$roots += 'C:\ProgramData\Microsoft\Windows\WER\ReportQueue'
$files = @()
foreach ($r in $roots) {
    if (Test-Path -LiteralPath $r) {
        $files += Get-ChildItem -LiteralPath $r -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Length -le 5MB }
    }
}
$temp = Get-ChildItem -LiteralPath $env:TEMP -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match 'reader|winprobe|we1[0-9]' -and $_.Length -le 5MB }
$files += $temp

$hits = @()
foreach ($f in $files) {
    try {
        $text = [System.IO.File]::ReadAllText($f.FullName)
    } catch { continue }
    foreach ($k in $patterns.Keys) {
        $m = [regex]::Matches($text, $patterns[$k], 'IgnoreCase')
        if ($m.Count -gt 0) {
            $hits += [pscustomobject]@{
                file = $f.FullName
                pattern = $k
                count = $m.Count
            }
        }
    }
}

$record = [ordered]@{
    scanned_roots = @($roots)
    temp_filter = 'reader|winprobe|we1[0-9]'
    files_scanned = $files.Count
    hits = $hits
    hit_files = @($hits | Select-Object -ExpandProperty file -Unique).Count
}
$record | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $json -Encoding UTF8
$record | ConvertTo-Json -Depth 5
