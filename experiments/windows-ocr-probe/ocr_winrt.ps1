# W-E03 Windows OCR probe (WinRT Windows.Media.Ocr). ASCII source; Chinese built from code points.
$ErrorActionPreference = 'Stop'
$outDir = 'F:\#_Working_Area\Intelligent-Reader\experiments\windows-ocr-probe\results'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$png = Join-Path $outDir 'we03-sample.png'
$json = Join-Path $outDir 'we03-ocr.json'

Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap 1400, 420
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::White)
$font = New-Object System.Drawing.Font 'Microsoft YaHei', 56, ([System.Drawing.FontStyle]::Regular)
$brush = [System.Drawing.Brushes]::Black
$line1 = (-join ([char]0x667A, [char]0x80FD, [char]0x9605, [char]0x8BFB, [char]0x52A9, [char]0x624B)) + ' OCR ' + (-join ([char]0x6D4B, [char]0x8BD5)) + ' 2026'
$line2 = 'Reader Studio W-E03 12345'
$g.DrawString($line1, $font, $brush, 40, 60)
$g.DrawString($line2, $font, $brush, 40, 220)
$g.Dispose()
$bmp.Save($png, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

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
$langs = [Windows.Media.Ocr.OcrEngine]::AvailableRecognizerLanguages | ForEach-Object { $_.LanguageTag }
if (-not $engine) { throw 'no OCR engine' }

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$file = Await ([Windows.Storage.StorageFile]::GetFileFromPathAsync($png)) ([Windows.Storage.StorageFile])
$stream = Await ($file.OpenAsync([Windows.Storage.FileAccessMode]::Read)) ([Windows.Storage.Streams.IRandomAccessStream])
$decoder = Await ([Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync($stream)) ([Windows.Graphics.Imaging.BitmapDecoder])
$bitmap = Await ($decoder.GetSoftwareBitmapAsync()) ([Windows.Graphics.Imaging.SoftwareBitmap])
$result = Await ($engine.RecognizeAsync($bitmap)) ([Windows.Media.Ocr.OcrResult])
$sw.Stop()

$text = $result.Text
$lines = @($result.Lines | ForEach-Object { $_.Text })
$norm = ($text -replace '\s', '')
$checks = [ordered]@{
    has_chinese = $norm.Contains((-join ([char]0x667A, [char]0x80FD, [char]0x9605, [char]0x8BFB, [char]0x52A9, [char]0x624B)))
    has_ocr = $norm.Contains('OCR')
    has_2026 = $norm.Contains('2026')
    has_reader = $norm.Contains('Reader')
    has_12345 = $norm.Contains('12345')
}
$record = [ordered]@{
    engine = 'Windows.Media.Ocr'
    languages = @($langs)
    image = $png
    elapsed_ms = [math]::Round($sw.Elapsed.TotalMilliseconds, 1)
    line_count = $lines.Count
    text = $text
    lines = $lines
    checks = $checks
    passed = (@($checks.Values | Where-Object { $_ }).Count)
}
$record | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $json -Encoding UTF8
$record | ConvertTo-Json -Depth 5
