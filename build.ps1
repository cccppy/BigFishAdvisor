param(
    [string]$GameDir = "",
    [string]$MtsJar = "",
    [string]$JdkHome = ""
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not $GameDir) {
    $candidates = @(
        "D:\steam\steamapps\common\SlayTheSpire",
        "C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire",
        "C:\Program Files\Steam\steamapps\common\SlayTheSpire"
    )
    foreach ($c in $candidates) {
        if (Test-Path (Join-Path $c "desktop-1.0.jar")) { $GameDir = $c; break }
    }
}
if (-not $GameDir -or -not (Test-Path (Join-Path $GameDir "desktop-1.0.jar"))) {
    throw "找不到 Slay the Spire 安装目录，请用 -GameDir 指定。"
}

if (-not $MtsJar) {
    $local = Join-Path $Root "tools\ModTheSpire.jar"
    if (Test-Path $local) { $MtsJar = $local }
}
if (-not $MtsJar -or -not (Test-Path $MtsJar)) {
    throw "找不到 ModTheSpire.jar，请用 -MtsJar 指定。"
}

if (-not $JdkHome) {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\javac.exe"))) {
        $JdkHome = $env:JAVA_HOME
    } else {
        $localJdk = Get-ChildItem (Join-Path $Root "tools") -Directory -Filter "jdk*" -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($localJdk) { $JdkHome = $localJdk.FullName }
    }
}
if (-not $JdkHome -or -not (Test-Path (Join-Path $JdkHome "bin\javac.exe"))) {
    throw "找不到 JDK 8，请安装 JDK 8 并设置 JAVA_HOME。"
}

$classes = Join-Path $Root "build\classes"
$stage = Join-Path $Root "build\jar"
$dist = Join-Path $Root "dist"
Remove-Item $classes, $stage -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes, $stage, $dist | Out-Null

$cp = (Join-Path $GameDir "desktop-1.0.jar") + ";" + (Resolve-Path $MtsJar)
$sources = Get-ChildItem (Join-Path $Root "src") -Recurse -Filter "*.java" | ForEach-Object FullName
& (Join-Path $JdkHome "bin\javac.exe") -encoding UTF-8 -source 1.8 -target 1.8 -cp $cp -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Copy-Item (Join-Path $Root "resources\bigfish") $stage -Recurse -Force
Copy-Item (Join-Path $Root "resources\ModTheSpire.json") $stage -Force
Copy-Item (Join-Path $Root "LICENSE") $stage -Force
Copy-Item (Join-Path $Root "README.md") $stage -Force
if (Test-Path (Join-Path $Root "OFL.txt")) { Copy-Item (Join-Path $Root "OFL.txt") $stage -Force }
Copy-Item (Join-Path $classes "*") $stage -Recurse -Force

$out = Join-Path $dist "BigFishAdvisor.jar"
& (Join-Path $JdkHome "bin\jar.exe") cf $out -C $stage .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }
Write-Host "Built: $out"

# Optional direct-launch shim used by config.json so the normal game icon boots
# ModTheSpire without showing the mod selector.
$launcherClasses = Join-Path $Root "build\launcher"
Remove-Item $launcherClasses -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $launcherClasses | Out-Null
$launcherSrc = Join-Path $Root "launcher\bigfish\launcher\BigFishLauncher.java"
& (Join-Path $JdkHome "bin\javac.exe") -encoding UTF-8 -source 1.8 -target 1.8 -cp $cp -d $launcherClasses $launcherSrc
if ($LASTEXITCODE -ne 0) { throw "launcher javac failed" }
$launcherOut = Join-Path $dist "BigFishLauncher.jar"
& (Join-Path $JdkHome "bin\jar.exe") cf $launcherOut -C $launcherClasses .
if ($LASTEXITCODE -ne 0) { throw "launcher jar failed" }
Write-Host "Built: $launcherOut"
# Replacement for the Workshop-only official mts-launcher.jar used by Steam's
# "Play with Mods" launch option. It starts the local ModTheSpire selector.
$mtsLauncherClasses = Join-Path $Root "build\mtslauncher"
Remove-Item $mtsLauncherClasses -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $mtsLauncherClasses | Out-Null
$mtsLauncherSrc = Join-Path $Root "launcher\com\megacrit\mtslauncher\Main.java"
& (Join-Path $JdkHome "bin\javac.exe") -encoding UTF-8 -source 1.8 -target 1.8 -d $mtsLauncherClasses $mtsLauncherSrc
if ($LASTEXITCODE -ne 0) { throw "mts-launcher javac failed" }
$mtsLauncherOut = Join-Path $dist "MtsLocalLauncher.jar"
& (Join-Path $JdkHome "bin\jar.exe") cfe $mtsLauncherOut com.megacrit.mtslauncher.Main -C $mtsLauncherClasses .
if ($LASTEXITCODE -ne 0) { throw "mts-launcher jar failed" }
Write-Host "Built: $mtsLauncherOut"