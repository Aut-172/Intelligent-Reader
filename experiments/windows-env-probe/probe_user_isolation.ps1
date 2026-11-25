# W-E10 data-isolation probe: run as the second user and report which paths are visible.
param([string]$Path = 'C:\Users\Public\we10')
$ErrorActionPreference = 'Continue'
$result = [ordered]@{ user = $env:USERNAME; time = (Get-Date).ToString('o') }
$targets = [ordered]@{
    hyc1Profile  = 'C:\Users\HYC-1'
    hyc1Local    = 'C:\Users\HYC-1\AppData\Local'
    hyc1Protect  = 'C:\Users\HYC-1\AppData\Roaming\Microsoft\Protect'
    workspaceDocs = 'F:\#_Working_Area\Intelligent-Reader\docs'
}
foreach ($key in $targets.Keys) {
    $p = $targets[$key]
    $result["${key}_exists"] = Test-Path -LiteralPath $p
    try {
        $null = Get-ChildItem -LiteralPath $p -ErrorAction Stop | Select-Object -First 1
        $result["${key}_listable"] = $true
    }
    catch {
        $result["${key}_listable"] = $false
        $result["${key}_error"] = $_.Exception.GetType().Name
    }
}
$json = $result | ConvertTo-Json -Depth 4
$json | Set-Content -LiteralPath (Join-Path $Path 'result-isolation.json') -Encoding UTF8
Write-Output $json
