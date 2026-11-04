# Keeps dist tidy (the owner, 2026-11-02: "why do we have so many build in dist\latest, please take all past version in archive and
# make sure it properly maintained in the future"). Run by use.ps1 at every install, once the PC has moved to the new version.
#   dist\latest   the version the shortcut opens and the version being built (windows/build.gradle), and Mininotes.lnk; every
#                 other version goes to dist\archive.
#   dist\archive  the released versions (the v* tags) and the newest three others; older ones are moved to the owner's holding
#                 folder, never deleted (HANDSHAKE, keep three): the owner deletes them there.
# Nothing a running Mininotes was started from is moved.
param([string]$Holding = 'C:\Users\Charles\To delete')
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$latest = Join-Path $project 'dist\latest'
$archive = Join-Path $project 'dist\archive'
$versionOf = { param($name) $m = [regex]::Match($name, '\d+\.\d+\.\d+'); if ($m.Success) { $m.Value } else { $null } }
$order = { param($v) [version]($v -replace '\.0*(\d)', '.$1') }

$keep = @{}
$built = (Select-String -Path (Join-Path $project 'windows\build.gradle') -Pattern "^version = '([^']+)'" | Select-Object -First 1).Matches.Groups[1].Value
if ($built) { $keep[$built] = $true }
$lnk = Join-Path $latest 'Mininotes.lnk'
if (Test-Path -LiteralPath $lnk) {
    $opens = (New-Object -ComObject WScript.Shell).CreateShortcut($lnk).TargetPath
    $v = & $versionOf $opens; if ($v) { $keep[$v] = $true }
}
$running = @(Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.Path } | ForEach-Object { $_.Path })
$inUse = { param($item) $p = $item.FullName; @($running | Where-Object { $_.StartsWith($p, [StringComparison]::OrdinalIgnoreCase) }).Count -gt 0 }

New-Item -ItemType Directory -Force $archive | Out-Null
$moved = 0
foreach ($item in Get-ChildItem -LiteralPath $latest) {
    $v = & $versionOf $item.Name
    if (-not $v -or $keep[$v] -or (& $inUse $item)) { continue }
    $to = Join-Path $archive $item.Name
    if (Test-Path -LiteralPath $to) { Write-Output "Left in latest, already in the archive: $($item.Name)"; continue }
    Move-Item -LiteralPath $item.FullName -Destination $to
    $moved++
}
Write-Output "dist\latest: $moved item(s) moved to dist\archive; kept $((($keep.Keys | Sort-Object) -join ', '))."

$released = @{}
# By the version a tag names, so v0.1.041-final keeps 0.1.041.
foreach ($tag in (git -C $project tag -l 'v*')) { $v = & $versionOf $tag; if ($v) { $released[$v] = $true } }
$items = Get-ChildItem -LiteralPath $archive | Where-Object { & $versionOf $_.Name }
$others = @($items | ForEach-Object { & $versionOf $_.Name } | Where-Object { -not $released[$_] } | Sort-Object -Unique { & $order $_ })
$newest = @{}; $others | Select-Object -Last 3 | ForEach-Object { $newest[$_] = $true }
$old = @($items | Where-Object { $v = & $versionOf $_.Name; -not $released[$v] -and -not $newest[$v] -and -not (& $inUse $_) })
if ($old.Count -gt 0) {
    $into = Join-Path $Holding 'Mininotes old builds'
    New-Item -ItemType Directory -Force $into | Out-Null
    foreach ($item in $old) {
        $to = Join-Path $into $item.Name
        if (Test-Path -LiteralPath $to) { $to = Join-Path $into ($item.Name + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
        Move-Item -LiteralPath $item.FullName -Destination $to
        Add-Content -LiteralPath (Join-Path $Holding 'Where each came from.txt') ($to + "`t<- " + $item.FullName)
    }
}
Write-Output "dist\archive: $($old.Count) item(s) moved to $Holding; kept the released versions and $((($newest.Keys | Sort-Object { & $order $_ }) -join ', '))."
