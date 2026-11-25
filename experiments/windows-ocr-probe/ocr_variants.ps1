param([string]$OutDir = 'F:\#_Working_Area\Intelligent-Reader\experiments\windows-ocr-probe\results')
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

$text = 'Reader Studio W-E03 2026'
$font = New-Object System.Drawing.Font 'Segoe UI', 40
$font2 = New-Object System.Drawing.Font 'Microsoft YaHei', 40
$records = @()
foreach ($angle in 0, 90, 180, 45) {
    $bmp = New-Object System.Drawing.Bitmap 900, 900
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::White)
    $g.TranslateTransform(450, 450)
    $g.RotateTransform($angle)
    $size = $g.MeasureString($text, $font)
    $g.DrawString($text, $font, [System.Drawing.Brushes]::Black, -$size.Width / 2, -$size.Height / 2)
    $g.Dispose()
    $path = Join-Path $OutDir ("we03-rot-$angle.png")
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $ocr = Get-Ocr $path
    $norm = ($ocr.result.Text -replace '\s', '')
    $records += [pscustomobject]@{
        angle = $angle
        image = $path
        elapsed_ms = $ocr.ms
        text = $ocr.result.Text
        word_count = @($ocr.result.Lines | ForEach-Object { $_.Words }).Count
        has_reader = $norm.Contains('Reader')
        has_2026 = $norm.Contains('2026')
    }
}
# Scan-like variant: grayscale + JPEG quality 55 from the 200 dpi render
$src = Join-Path $OutDir 'we03-render.png'
$img = [System.Drawing.Image]::FromFile($src)
$gray = New-Object System.Drawing.Bitmap $img.Width, $img.Height
$g2 = [System.Drawing.Graphics]::FromImage($gray)
$cm = New-Object System.Drawing.Imaging.ColorMatrix
$cm.Matrix00 = 0.299; $cm.Matrix01 = 0.299; $cm.Matrix02 = 0.299
$cm.Matrix10 = 0.587; $cm.Matrix11 = 0.587; $cm.Matrix12 = 0.587
$cm.Matrix20 = 0.114; $cm.Matrix21 = 0.114; $cm.Matrix22 = 0.114
$cm.Matrix33 = 1; $cm.Matrix44 = 1
$ia = New-Object System.Drawing.Imaging.ImageAttributes
$ia.SetColorMatrix($cm)
$rect = New-Object System.Drawing.Rectangle 0, 0, $img.Width, $img.Height
$g2.DrawImage($img, $rect, 0, 0, $img.Width, $img.Height, [System.Drawing.GraphicsUnit]::Pixel, $ia)
$g2.Dispose(); $img.Dispose()
$scan = Join-Path $OutDir 'we03-scan-like.jpg'
$codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$ep = New-Object System.Drawing.Imaging.EncoderParameters 1
$ep.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), 55
$gray.Save($scan, $codec, $ep)
$gray.Dispose()
$ocr = Get-Ocr $scan
$norm = ($ocr.result.Text -replace '\s', '')
$records += [pscustomobject]@{
    angle = 'scan-like'
    image = $scan
    elapsed_ms = $ocr.ms
    text = $ocr.result.Text
    word_count = @($ocr.result.Lines | ForEach-Object { $_.Words }).Count
    has_reader = $norm.Contains('Reader')
    has_2026 = $norm.Contains('2026')
}
$records | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $OutDir 'we03-variants.json') -Encoding UTF8
$records | ConvertTo-Json -Depth 4
