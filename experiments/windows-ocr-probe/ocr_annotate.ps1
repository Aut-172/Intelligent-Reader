param(
    [Parameter(Mandatory = $true)][string]$Image,
    [Parameter(Mandatory = $true)][string]$Json,
    [Parameter(Mandatory = $true)][string]$Out
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$data = Get-Content -LiteralPath $Json -Encoding UTF8 | ConvertFrom-Json
$bmp = [System.Drawing.Image]::FromFile($Image)
$canvas = New-Object System.Drawing.Bitmap $bmp.Width, $bmp.Height
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.DrawImage($bmp, 0, 0, $bmp.Width, $bmp.Height)
$bmp.Dispose()
$fill = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(60, 255, 235, 59))
$pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(220, 220, 30, 30)), 2
foreach ($w in $data.words) {
    $rect = New-Object System.Drawing.RectangleF ([float]$w.x), ([float]$w.y), ([float]$w.w), ([float]$w.h)
    $g.FillRectangle($fill, $rect)
    $g.DrawRectangle($pen, [float]$w.x, [float]$w.y, [float]$w.w, [float]$w.h)
}
$g.Dispose()
$canvas.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose()
Write-Output ("annotated words=" + @($data.words).Count + " out=" + $Out)
