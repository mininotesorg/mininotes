param([switch]$Package,[switch]$SkipTests,[switch]$NoMsi)
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
    # Windows reads a version as numbers: 0.4.015 is 0.4.15 to it, for the app image and the MSI alike.
    $appVersion = ($version.Split('.') | ForEach-Object { [int]$_ }) -join '.'
    & (Join-Path $jdk 'bin\jpackage.exe') --type app-image --name Mininotes `
        --app-version $appVersion --vendor Mininotes --dest $destination `
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
    # The installer, beside the ZIP (the owner, 2026-10-22: "I installed the app on a second laptop using Mininotes.exe and
    # can't find it in apps, also nothing to uninstall"; decision 235): a real one, in Settings, Apps and uninstalled there.
    # The ZIP stays, the same as before: every PC already running an unzipped copy updates by it, and the website offers it.
    # -NoMsi for a quick build without it.
    if (!$NoMsi) {
        # WiX 3, which jpackage makes the MSI with: $env:WIX where it is set (the release sets it), else this PC's own copy.
        $wix = $null
        $places = @(); if ($env:WIX) { $places += $env:WIX, (Join-Path $env:WIX 'bin') }; $places += Join-Path $env:USERPROFILE 'tools\wix314'
        foreach ($place in $places) {
            if ($place -and (Test-Path (Join-Path $place 'candle.exe')) -and (Test-Path (Join-Path $place 'light.exe'))) { $wix = $place; break }
        }
        if (!$wix) { throw 'The MSI needs WiX Toolset 3.14 (candle.exe and light.exe): set WIX to its folder, put it in tools\wix314 in your user folder, or build with -NoMsi.' }
        # Fixed for ever: Windows knows a newer Mininotes MSI is the same app, and replaces the older one with it, by this
        # code alone. A new one would install a second Mininotes beside the first. Never change it.
        $upgrade = '63dc3f5a-ccff-49d6-9826-0c0b42a0beea'
        # The same app image, copied, with one file more: app\msi-installed is how the installed app knows it came from the
        # MSI, and so updates by the MSI and uninstalls through Windows (see DesktopUpdate.byMsi). An unzipped copy has none.
        $msiImage = WorkspacePath (Join-Path $project "build\package-$stamp-msi")
        New-Item -ItemType Directory $msiImage -Force | Out-Null
        Copy-Item (Join-Path $destination 'Mininotes') $msiImage -Recurse
        "Installed by Mininotes-Windows-$version.msi. Updates come as an MSI; Settings, Apps uninstalls it." | Set-Content -Encoding ascii (Join-Path $msiImage 'Mininotes\app\msi-installed')
        $msiOut = WorkspacePath (Join-Path $project "build\msi-$stamp")
        $path = $env:PATH; $env:PATH = "$wix;$path"
        # The installer's own pages (decision 237, the owner, 2026-10-24: "When we launch the msi execution, can we display
        # our logo there", "When we click finish we should have the option to launch the app"): windows\installer holds
        # main.wxs, jpackage's own with Mininotes' pages in it, and the logo's two pictures, which main.wxs finds through
        # MININOTES_INSTALLER (jpackage copies only main.wxs to where WiX runs). See windows\installer\main.wxs.
        $installer = Join-Path $project 'installer'
        $env:MININOTES_INSTALLER = $installer
        try {
            # Per user: no administrator is asked for. Under %LOCALAPPDATA%\Mininotes App (one folder: JDK 17's jpackage cannot build a per-user MSI into a nested one,
            # ICE64 refusing a folder it never removes), not jpackage's own choice
            # for a per-user install, %LOCALAPPDATA%\Mininotes: that is the notebook's folder, which the install must never
            # hold (uninstalling from Settings, Apps keeps the notes; decision 235).
            # No --license-file (the owner, 2026-10-24: "The general licence there, is it coming from fsf directly, is it a
            # generic one, why do we need that?"; decision 237): it was the GPL's own text, and the GPL asks nobody to
            # accept it to install or run a copy, only sets terms for passing it on or changing it. LICENSE stays in the source.
            # --win-dir-chooser is there only because jpackage 17 then gives WiX its dialogs (WixUIExtension), which
            # Mininotes' pages are made of; main.wxs leaves out jpackage's pages, so no folder page is ever shown and the
            # folder stays the one --install-dir names.
            & (Join-Path $jdk 'bin\jpackage.exe') --type msi --app-image (Join-Path $msiImage 'Mininotes') --name Mininotes `
                --app-version $appVersion --vendor Mininotes --dest $msiOut `
                --description 'Mininotes, a paper pad for the people close to you' `
                --icon (Join-Path $project 'build\icons\Mininotes.ico') --about-url 'https://mininotes.org' `
                --resource-dir $installer --win-dir-chooser `
                --win-per-user-install --install-dir 'Mininotes App' `
                --win-menu --win-menu-group Mininotes --win-shortcut --win-upgrade-uuid $upgrade
            if ($LASTEXITCODE -ne 0) { throw 'The Windows installer (MSI) failed.' }
        } finally { $env:PATH = $path; Remove-Item Env:MININOTES_INSTALLER -ErrorAction SilentlyContinue }
        $made = @(Get-ChildItem $msiOut -File -Filter '*.msi')
        if ($made.Count -ne 1) { throw "jpackage left $($made.Count) MSI files in $msiOut, not one." }
        $msi = Join-Path $latest "Mininotes-Windows-$version.msi"
        Move-Item -LiteralPath (WorkspacePath $made[0].FullName) -Destination $msi -Force
        $hash = (Get-FileHash -LiteralPath $msi -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  Mininotes-Windows-$version.msi" | Set-Content -Encoding ascii "$msi.sha256"
        Write-Output "Installer: $msi"
    } elseif (Test-Path -LiteralPath (Join-Path $latest "Mininotes-Windows-$version.msi")) {
        Write-Output "No MSI built (-NoMsi); the MSI of $version in dist\latest is from an earlier build."
    }
    # Keep existing app images where they are: an older process may still be using one.
    Write-Output "Built: $app\Mininotes.exe"
    Write-Output "Archive: $archive"
}
