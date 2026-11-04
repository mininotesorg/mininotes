# Points dist/latest/Mininotes.lnk at one built version of the app, at install and only then (the owner, 2026-10-30:
# "switch to a new version only when you install it"). Run after the notebook is backed up and the running app has made way.
param([Parameter(Mandatory = $true)][string]$Folder)
$ErrorActionPreference = 'Stop'
$latest = Join-Path (Split-Path -Parent $PSScriptRoot) 'dist\latest'
$app = if (Test-Path -LiteralPath $Folder) { $Folder } else { Join-Path $latest $Folder }
$exe = Join-Path $app 'Mininotes.exe'
if (-not (Test-Path -LiteralPath $exe)) { throw "No Mininotes.exe in $app" }
$link = (New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $latest 'Mininotes.lnk'))
$link.TargetPath = (Resolve-Path $exe).Path
$link.WorkingDirectory = (Resolve-Path $app).Path
$link.Description = 'Mininotes for Windows ' + ((Split-Path -Leaf $app) -replace '^Mininotes-Windows-','')
$link.Save()
"Mininotes.lnk now opens $($link.TargetPath)"
# The version left behind goes to the archive, and the archive keeps what HANDSHAKE says (keep three): see tidy-dist.ps1.
& (Join-Path $PSScriptRoot 'tidy-dist.ps1')
