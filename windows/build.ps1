param([switch]$Package,[switch]$SkipTests)
$ErrorActionPreference = 'Stop'
$project = [IO.Path]::GetFullPath($PSScriptRoot)
$workspace = [IO.Path]::GetFullPath((Join-Path $project '..'))
function WorkspacePath([string]$path) {
    $absolute = [IO.Path]::GetFullPath($path)
    if (!$absolute.StartsWith($workspace + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw "Packaging path leaves workspace: $absolute" }
    return $absolute
}
$jdk = $env:JAVA_HOME
if (!$jdk -or !(Test-Path (Join-Path $jdk 'bin\javac.exe'))) {
    $compiler = Get-Command javac.exe -ErrorAction Stop
    $jdk = Split-Path (Split-Path $compiler.Source)
}
$env:JAVA_HOME = $jdk
$env:DEBUG = ''
# -SkipTests only when the tests were just run on their own: the window tests fail while another Mininotes
# window on this PC holds the focus.
$gradle = Join-Path $project '..\android\gradlew.bat'
if ($SkipTests) { & $gradle -p $project --no-daemon packageInput }
else { & $gradle -p $project --no-daemon test packageInput }
if ($LASTEXITCODE -ne 0) { throw 'Windows build or tests failed.' }
if ($Package) {
    # The jar and the folder take the version windows/build.gradle names, so a bump there is the one that counts.
    $version = (Select-String -Path (Join-Path $project 'build.gradle') -Pattern "^version = '([^']+)'" | Select-Object -First 1).Matches.Groups[1].Value
    # A new destination makes rebuilds non-destructive, including when an older
    # build is open. The ZIP contains the entire app image, not just its launcher.
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $destination = WorkspacePath (Join-Path $project "build\package-$stamp")
    & (Join-Path $jdk 'bin\jpackage.exe') --type app-image --name Mininotes `
        --app-version (($version.Split('.') | ForEach-Object { [int]$_ }) -join '.') --vendor Mininotes --dest $destination `
        --icon (Join-Path $project 'build\icons\Mininotes.ico') `
        --input (Join-Path $project 'build\package-input') `
        --main-jar "Mininotes-Windows-$version.jar" --main-class org.mininotes.android.Desktop `
        --java-options '-Dsun.awt.fontconfig=$APPDIR\fontconfig.properties' `
        --add-modules java.base,java.desktop,java.sql,java.logging,java.naming,java.net.http,jdk.crypto.ec,jdk.unsupported
    if ($LASTEXITCODE -ne 0) { throw 'Windows packaging failed.' }
    $latest = WorkspacePath (Join-Path $project '..\dist\latest')
    $archived = WorkspacePath (Join-Path $project '..\dist\archive')
    New-Item -ItemType Directory $latest,$archived -Force | Out-Null
    # Archive older ZIPs only. Existing runnable folders stay where they are.
    $older = @(Get-ChildItem $latest -File -Filter 'Mininotes-Windows-*' | Where-Object { $_.Name -notlike "Mininotes-Windows-$version*" })
    # Preserve an earlier same-named archive and keep ZIP/checksum pairs together.
    if ($older | Where-Object { Test-Path -LiteralPath (Join-Path $archived $_.Name) }) {
        $archived = WorkspacePath (Join-Path $archived "retained-$stamp")
        New-Item -ItemType Directory $archived -Force | Out-Null
    }
    $older | ForEach-Object { try { Move-Item -LiteralPath (WorkspacePath $_.FullName) -Destination $archived } catch { Write-Output "Left in latest (it is open): $($_.TargetObject)" } }
    $archive = Join-Path $latest "Mininotes-Windows-$version.zip"
    # Retried: right after jpackage a file of the new app can still be held (a scan of new files, 2026-10-05), and the zip fails on it.
    for ($try = 1; $try -le 6; $try++) {
        try { Compress-Archive -Path (Join-Path $destination 'Mininotes') -DestinationPath $archive -Force -ErrorAction Stop; break }
        catch { if ($try -eq 6) { throw }; Start-Sleep -Seconds 10 }
    }
    $hash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  Mininotes-Windows-$version.zip" | Set-Content -Encoding ascii "$archive.sha256"
    # The app itself, ready to run, beside its zip - one folder, the EXE at its top - and a shortcut to it.
    $app = Join-Path $latest "Mininotes-Windows-$version"
    $app = WorkspacePath $app
    if (Test-Path -LiteralPath $app) { $app = WorkspacePath (Join-Path $latest "Mininotes-Windows-$version-$stamp") }
    Copy-Item (Join-Path $destination 'Mininotes') $app -Recurse
    $shell = New-Object -ComObject WScript.Shell
    $link = $shell.CreateShortcut((Join-Path $latest 'Mininotes.lnk'))
    $link.TargetPath = (Resolve-Path (Join-Path $app 'Mininotes.exe')).Path
    $link.WorkingDirectory = (Resolve-Path $app).Path
    $link.Description = "Mininotes for Windows $version"
    $link.Save()
    # Keep existing app images where they are: an older process may still be using one.
    Write-Output "Built: $app\Mininotes.exe"
    Write-Output "Archive: $archive"
}
