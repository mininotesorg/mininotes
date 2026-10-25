# The installer's two pictures, made from the app's own logo (the owner, 2026-10-24: "When we launch the msi execution,
# can we display our logo there, where is the one displayed coming from?"; decision 237). Without them WiX shows its own
# red and white artwork. The BMPs this writes are kept in the repository beside it, so a build needs neither this script
# nor System.Drawing; run it again only when the logo or the colours change:
#   powershell -ExecutionPolicy Bypass -File windows\installer\make-images.ps1
# The logo is drawn from its geometry, the same as DesktopIcon.java and Android's res/drawable/ic_note.xml (a 48 unit
# square: the page 13,9 to 36,40, its margin at x 18, three lines), so it stays sharp at any size.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$here = [IO.Path]::GetFullPath($PSScriptRoot)
$green = [Drawing.Color]::FromArgb(0x28, 0x56, 0x46)
$paper = [Drawing.Color]::FromArgb(0xF5, 0xF0, 0xE4)

# The logo at ($x,$y), $size pixels square; -NoSquare leaves out the green square, for a green background.
function Logo([Drawing.Graphics]$g, [double]$x, [double]$y, [double]$size, [switch]$NoSquare) {
    $state = $g.Save()
    $g.TranslateTransform($x, $y); $g.ScaleTransform($size / 48.0, $size / 48.0)
    $greenBrush = New-Object Drawing.SolidBrush $green; $paperBrush = New-Object Drawing.SolidBrush $paper
    $pen = New-Object Drawing.Pen $green, 2
    if (!$NoSquare) { $g.FillRectangle($greenBrush, 0, 0, 48, 48) }
    $g.FillRectangle($paperBrush, 13, 9, 23, 31)
    $g.DrawLine($pen, 18, 9, 18, 40)
    $g.DrawLine($pen, 23, 18, 32, 18)
    $g.DrawLine($pen, 23, 24, 32, 24)
    $g.DrawLine($pen, 23, 30, 29, 30)
    $pen.Dispose(); $greenBrush.Dispose(); $paperBrush.Dispose()
    $g.Restore($state)
}

# WiX wants 24-bit BMPs of these exact sizes.
function Picture([int]$width, [int]$height, [string]$name, [scriptblock]$draw) {
    $bitmap = New-Object Drawing.Bitmap $width, $height, ([Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [Drawing.Graphics]::FromImage($bitmap)
    try {
        $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        & $draw $g
    } finally { $g.Dispose() }
    $bitmap.Save((Join-Path $here $name), [Drawing.Imaging.ImageFormat]::Bmp)
    $bitmap.Dispose()
    Write-Output "Wrote $(Join-Path $here $name)"
}

# The first and last pages (493 x 312): WiX writes its title and text from about x 180 on, so the left part is the app's
# green with the page of the logo on it, and the rest is paper, where WiX's black text reads well.
Picture 493 312 'dialog.bmp' {
    param($g)
    $g.Clear($paper)
    $panel = New-Object Drawing.SolidBrush $green
    $g.FillRectangle($panel, 0, 0, 164, 312); $panel.Dispose()
    # The page is 23 of the logo's 48 units wide: 120 pixels of logo give a page of about 58, centred in the panel.
    Logo $g (82 - 60) (156 - 60 - 12) 120 -NoSquare
}

# The banner across the top of the other pages (493 x 58): WiX writes the page's title and text on its left, so the
# logo sits small at the right.
Picture 493 58 'banner.bmp' {
    param($g)
    $g.Clear($paper)
    Logo $g (493 - 46 - 8) 6 46
}
