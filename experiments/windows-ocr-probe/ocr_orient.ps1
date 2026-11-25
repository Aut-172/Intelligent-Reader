param(
    [Parameter(Mandatory = $true)][string]$Image,
    [string]$OutDir = 'F:\#_Working_Area\Intelligent-Reader\experiments\windows-ocr-probe\results'
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
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

function Get-Ocr($path) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $file = Await ([Windows.Storage.StorageFile]::GetFileFromPathAsync($path)) ([Windows.Storage.StorageFile])
    $stream = Await ($file.OpenAsync([Windows.Storage.FileAccessMode]::Read)) ([Windows.Storage.Streams.IRandomAccessStream])
    $decoder = Await ([Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync($stream)) ([Windows.Graphics.Imaging.BitmapDecoder])
    $bitmap = Await ($decoder.GetSoftwareBitmapAsync()) ([Windows.Graphics.Imaging.SoftwareBitmap])
    $result = Await ($engine.RecognizeAsync($bitmap)) ([Windows.Media.Ocr.OcrResult])
    $sw.Stop()
    return [pscustomobject]@{ result = $result; ms = [math]::Round($sw.Elapsed.TotalMilliseconds, 1) }
}

$base = [System.Drawing.Image]::FromFile($Image)
$candidates = @()
foreach ($rot in 0, 90, 180, 270) {
    $bmp = $base.Clone()
    $times = [int]($rot / 90)
    for ($i = 0; $i -lt $times; $i++) { $bmp.RotateFlip([System.Drawing.RotateFlipType]::Rotate90FlipNone) }
    $tmp = Join-Path $OutDir ("we03-orient-$rot.png")
    $bmp.Save($tmp, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $ocr = Get-Ocr $tmp
    $norm = ($ocr.result.Text -replace '\s', '')
    $candidates += [pscustomobject]@{
        rotate = $rot
        image = $tmp
        score = $norm.Length
        word_count = @($ocr.result.Lines | ForEach-Object { $_.Words }).Count
        text = $ocr.result.Text
        elapsed_ms = $ocr.ms
    }
}
$base.Dispose()
$best = $candidates | Sort-Object -Property @{Expression='score';Descending=$true}, @{Expression='rotate';Descending=$false} | Select-Object -First 1
Copy-Item $best.image (Join-Path $OutDir 'we03-orient-best.png') -Force
$record = [ordered]@{ source = $Image; best = $best; candidates = $candidates }
$record | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $OutDir 'we03-orient.json') -Encoding UTF8
$record | ConvertTo-Json -Depth 5
