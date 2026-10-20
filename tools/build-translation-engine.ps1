# The translation engine for the PC (docs/TRANSLATION.md): bergamot-translator (MPL-2.0) at a fixed commit, only the parts
# it needs, the Windows fixes in translation-engine-windows.patch, built with Visual Studio 2022 Build Tools into
# native\build-pc\app\bergamot.exe. Open-source parts only: no MKL. native\ is never in the tree (.gitignore).
#
#   powershell -ExecutionPolicy Bypass -File tools\build-translation-engine.ps1
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$native = Join-Path $root 'native'
$engine = Join-Path $native 'bt'
$commit = '9271618ebbdc5d21ac4dc4df9e72beb7ce644774'   # bergamot-translator main, 2024-05-12
New-Item -ItemType Directory -Force $native | Out-Null
if (!(Test-Path $engine)) {
    git -c core.longpaths=true clone https://github.com/browsermt/bergamot-translator.git $engine
}
git -C $engine config core.longpaths true
git -C $engine checkout -q $commit
# Only what the engine is built from: the tests' data and the server parts are left out.
git -C $engine submodule update --init 3rd_party/marian-dev 3rd_party/ssplit-cpp
$marian = Join-Path $engine '3rd_party\marian-dev'
git -C $marian config core.longpaths true
foreach ($part in 'src/3rd_party/intgemm','src/3rd_party/sentencepiece','src/3rd_party/ruy','src/3rd_party/simd_utils') {
    git -C $marian submodule update --init $part
}
# PCRE2 built in release with the static runtime, under the name MSVC gives it, and declared static where it is used.
$ssplit = Join-Path $engine '3rd_party\ssplit-cpp'
git -C $ssplit checkout -- .
git -C $ssplit apply --whitespace=nowarn (Join-Path $PSScriptRoot 'translation-engine-windows.patch')
$tools = 'C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools'
$cmake = Join-Path $tools 'Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe'
$vars = Join-Path $tools 'VC\Auxiliary\Build\vcvars64.bat'
$build = Join-Path $native 'build-pc'
$configure = "`"$cmake`" -S `"$engine`" -B `"$build`" -G Ninja -DCMAKE_BUILD_TYPE=Release -DUSE_WASM_COMPATIBLE_SOURCE=ON " +
    "-DSSPLIT_USE_INTERNAL_PCRE2=ON -DUSE_STATIC_LIBS=ON -DCOMPILE_TESTS=OFF -DCOMPILE_UNIT_TESTS=OFF -DGIT_SUBMODULE=OFF " +
    "-DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded -DCMAKE_POLICY_DEFAULT_CMP0091=NEW"
cmd /c "call `"$vars`" >nul && $configure && `"$cmake`" --build `"$build`" --config Release -j 6"
if ($LASTEXITCODE -ne 0) { throw "The engine did not build ($LASTEXITCODE)" }
Get-Item (Join-Path $build 'app\bergamot.exe') | Select-Object FullName, Length
