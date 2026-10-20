# The translation engine for the phone (docs/TRANSLATION.md, phase 3): the same bergamot-translator as the PC's
# (tools/build-translation-engine.ps1 fetches it into native\bt), built for arm64 phones with the Android NDK r23b, as its
# own Android build does, into native\build-android\app\bergamot. The app carries it as lib/arm64-v8a/libbergamot.so so
# Android lets it run (a program the app downloads or writes could not), and starts it for each translation.
#
#   powershell -ExecutionPolicy Bypass -File tools\build-translation-engine-android.ps1
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$native = Join-Path $root 'native'
$engine = Join-Path $native 'bt'
if (!(Test-Path (Join-Path $engine '3rd_party\marian-dev\src'))) { throw 'Run tools\build-translation-engine.ps1 first: it fetches the sources.' }
$ndk = Join-Path $env:LOCALAPPDATA 'Android\Sdk\ndk\23.1.7779620'
if (!(Test-Path $ndk)) { throw 'The Android NDK 23.1.7779620 is not installed (sdkmanager "ndk;23.1.7779620").' }
$tools = 'C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\IDE\CommonExtensions\Microsoft\CMake'
$cmake = Join-Path $tools 'CMake\bin\cmake.exe'
$ninja = Join-Path $tools 'Ninja\ninja.exe'
$build = Join-Path $native 'build-android'
# PCRE2 is configured in a CMake run of its own, which finds Ninja on the PATH.
$env:PATH = (Split-Path $ninja) + ';' + $env:PATH
& $cmake -S $engine -B $build -G Ninja "-DCMAKE_MAKE_PROGRAM=$ninja" -DCMAKE_BUILD_TYPE=Release `
    "-DCMAKE_TOOLCHAIN_FILE=$ndk\build\cmake\android.toolchain.cmake" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 `
    -DANDROID_STL=c++_static -DANDROID_ARM_NEON=TRUE -DCOMPILE_CUDA=off -DCOMPILE_CPU=on -DCMAKE_HAVE_THREADS_LIBRARY=1 `
    -DCMAKE_USE_WIN32_THREADS_INIT=0 -DCMAKE_USE_PTHREADS_INIT=1 -DTHREADS_PREFER_PTHREAD_FLAG=ON -DBUILD_ARCH=armv8-a `
    -DSSPLIT_USE_INTERNAL_PCRE2=ON -DUSE_STATIC_LIBS=ON -DCOMPILE_TESTS=OFF -DCOMPILE_UNIT_TESTS=OFF -DGIT_SUBMODULE=OFF `
    "-DCMAKE_EXE_LINKER_FLAGS=-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
# Laid out for phones with 16 KB memory pages too (Android's 16 KB check, the owner's Graphene, 2026-10-13): NDK r23 lays
# programs out for 4 KB only.
if ($LASTEXITCODE -ne 0) { throw "The engine for the phone could not be set up ($LASTEXITCODE)" }
& $cmake --build $build --config Release -j 6
if ($LASTEXITCODE -ne 0) { throw "The engine for the phone did not build ($LASTEXITCODE)" }
$made = Join-Path $build 'app\bergamot'
# Smaller: its symbols are not needed on a phone.
& (Join-Path $ndk 'toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-strip.exe') --strip-unneeded $made
# Where the phone app's build takes it from (android/app/build.gradle), named as a library so Android unpacks it.
$libs = Join-Path (Join-Path $native 'jniLibs') 'arm64-v8a'
New-Item -ItemType Directory -Force $libs | Out-Null
Copy-Item $made (Join-Path $libs 'libbergamot.so') -Force
Get-Item (Join-Path $libs 'libbergamot.so') | Select-Object FullName, Length
