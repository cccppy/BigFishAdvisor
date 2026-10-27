param(
    [string]$GameDir = ""
)

$ErrorActionPreference = "Stop"
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
    throw "Slay the Spire not found. Pass -GameDir."
}

$jar = Join-Path $GameDir "mods\BigFishAdvisor.jar"
if (Test-Path $jar) {
    Remove-Item $jar -Force
    Write-Host "Removed $jar"
} else {
    Write-Host "BigFishAdvisor.jar was not installed in that folder."
}

Remove-Item (Join-Path $GameDir "BigFishLauncher.jar") -Force -ErrorAction SilentlyContinue

$originalMtsLauncher = Join-Path $GameDir "mts-launcher-original.jar"
if (Test-Path $originalMtsLauncher) {
    Copy-Item $originalMtsLauncher (Join-Path $GameDir "mts-launcher.jar") -Force
    Remove-Item $originalMtsLauncher -Force
    Write-Host "Restored the original mts-launcher.jar."
}

$configPath = Join-Path $GameDir "config.json"
$backupPath = Join-Path $GameDir "config.json.bigfish-backup"
if (Test-Path $backupPath) {
    Copy-Item $backupPath $configPath -Force
    Remove-Item $backupPath -Force
    Write-Host "Restored the original config.json."
}

$cfg = Join-Path $env:LOCALAPPDATA "ModTheSpire\mod_order.xml"
if (Test-Path $cfg) {
    $raw = Get-Content $cfg -Raw
    $raw = $raw -replace "\s*<mod>BigFishAdvisor\.jar</mod>", ""
    Set-Content -Path $cfg -Value $raw -Encoding UTF8
}

Write-Host "The mod was removed. ModTheSpire and other mods were left untouched."
