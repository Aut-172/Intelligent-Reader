param(
    [Parameter(Mandatory = $true)][string]$Image,
    [string]$OutDir = 'F:\#_Working_Area\Intelligent-Reader\experiments\windows-ocr-probe\results'
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]
function Await($op, $type) {
    $m = $asTaskGeneric.MakeGenericMethod($type)
    $task = $m.Invoke($null, @($op))
    $task.Wait(-1) | Out-Null
    $task.Result
}
$null = [Windows.Media.Ocr.OcrEngine, Windows.Foundation, ContentType = WindowsRuntime]
$null = [Windows.Storage.StorageFile, Windows.Foundation, ContentType = WindowsRuntime]
$null = [Windows.Graphics.Imaging.BitmapDecoder, Windows.Foundation, ContentType = WindowsRuntime]
$engine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromUserProfileLanguages()
if (-not $engine) { throw 'no OCR engine' }

function Invoke-Ocr($path) {
    $file = Await ([Windows.Storage.StorageFile]::GetFileFromPathAsync($path)) ([Windows.Storage.StorageFile])
    $stream = Await ($file.OpenAsync([Windows.Storage.FileAccessMode]::Read)) ([Windows.Storage.Streams.IRandomAccessStream])
    $decoder = Await ([Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync($stream)) ([Windows.Graphics.Imaging.BitmapDecoder])
    $bitmap = Await ($decoder.GetSoftwareBitmapAsync()) ([Windows.Graphics.Imaging.SoftwareBitmap])
    $result = Await ($engine.RecognizeAsync($bitmap)) ([Windows.Media.Ocr.OcrResult])
    return $result
}

$cacheDir = Join-Path $OutDir 'we03-cache'
New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null
$hash = (Get-FileHash -LiteralPath $Image -Algorithm SHA256).Hash
$cacheFile = Join-Path $cacheDir ($hash + '.json')

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$cacheHit = Test-Path $cacheFile
if ($cacheHit) {
    $text = (Get-Content -LiteralPath $cacheFile -Raw -Encoding UTF8 | ConvertFrom-Json).text
} else {
    $result = Invoke-Ocr $Image
    $text = $result.Text
    [pscustomobject]@{ hash = $hash; text = $text } | ConvertTo-Json | Set-Content -LiteralPath $cacheFile -Encoding UTF8
}
$sw.Stop()
$firstMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 1)

$sw2 = [System.Diagnostics.Stopwatch]::StartNew()
if (Test-Path $cacheFile) {
    $text2 = (Get-Content -LiteralPath $cacheFile -Raw -Encoding UTF8 | ConvertFrom-Json).text
} else {
    $text2 = (Invoke-Ocr $Image).Text
}
$sw2.Stop()
$secondMs = [math]::Round($sw2.Elapsed.TotalMilliseconds, 1)

# Failure degradation: missing image must not crash the caller.
$degraded = $false
$degradeReason = ''
try {
    Invoke-Ocr (Join-Path $OutDir 'does-not-exist.png') | Out-Null
} catch {
    $degraded = $true
    $degradeReason = $_.Exception.Message
}

# Cancellation: Windows.Media.Ocr.RecognizeAsync has no CancellationToken API.
$cancelApi = 'Windows.Media.Ocr.RecognizeAsync(IAsyncOperation) has no CancellationToken; caller must abandon the task.'

$record = [ordered]@{
    image = $Image
    hash = $hash
    cache_hit_first = $cacheHit
    first_ms = $firstMs
    second_ms = $secondMs
    text_match = ($text -eq $text2)
    degraded_on_missing = $degraded
    degrade_reason = $degradeReason
    cancel_api = $cancelApi
}
$record | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $OutDir 'we03-cache-degrade.json') -Encoding UTF8
$record | ConvertTo-Json -Depth 4
