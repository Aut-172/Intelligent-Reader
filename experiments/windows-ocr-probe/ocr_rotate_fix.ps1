param([string]$OutDir = 'F:\#_Working_Area\Intelligent-Reader\experiments\windows-ocr-probe\results')
Add-Type -AssemblyName System.Drawing
$jobs = @(
    @{ src = 'we03-rot-90.png';  dst = 'we03-rot-90-fixed.png';  op = 'Rotate270FlipNone' },
    @{ src = 'we03-rot-180.png'; dst = 'we03-rot-180-fixed.png'; op = 'Rotate180FlipNone' }
)
foreach ($j in $jobs) {
    $src = Join-Path $OutDir $j.src
    $img = [System.Drawing.Image]::FromFile($src)
    $img.RotateFlip([System.Drawing.RotateFlipType]::($j.op))
    $dst = Join-Path $OutDir $j.dst
    $img.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png)
    $img.Dispose()
    Write-Output ("fixed: " + $dst)
}
