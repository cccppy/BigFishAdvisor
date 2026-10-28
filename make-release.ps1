param(
    [string]$Version = ""
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $Version) {
    $meta = Get-Content (Join-Path $Root "resources\ModTheSpire.json") -Raw -Encoding UTF8 | ConvertFrom-Json
    $Version = $meta.version
}
$mod = Join-Path $Root "dist\BigFishAdvisor.jar"
if (-not (Test-Path $mod)) {
    throw "Build first: .\build.ps1 -GameDir <game dir> -MtsJar <ModTheSpire.jar>"
}

$stage = Join-Path $Root "release\BigFishAdvisor-$Version"
Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $stage, (Join-Path $stage "vendor") | Out-Null

Copy-Item $mod $stage -Force
Copy-Item (Join-Path $Root "install.ps1") $stage -Force
Copy-Item (Join-Path $Root "uninstall.ps1") $stage -Force
Copy-Item (Join-Path $Root "README.md") $stage -Force
Copy-Item (Join-Path $Root "GUIDES.md") $stage -Force
if (Test-Path (Join-Path $Root "GUIDES.html")) {
    Copy-Item (Join-Path $Root "GUIDES.html") $stage -Force
}
if (Test-Path (Join-Path $Root "GUIDES.docx")) {
    Copy-Item (Join-Path $Root "GUIDES.docx") $stage -Force
}
Copy-Item (Join-Path $Root "LICENSE") $stage -Force
Copy-Item (Join-Path $Root "OFL.txt") $stage -Force
Copy-Item (Join-Path $Root "THIRD_PARTY_NOTICES.md") $stage -Force
Copy-Item (Join-Path $Root "ARTWORK_LICENSE.md") $stage -Force
Copy-Item (Join-Path $Root "CHANGELOG.md") $stage -Force
Copy-Item (Join-Path $Root "PUBLISH.md") $stage -Force

$mtsJar = Join-Path $Root "tools\ModTheSpire.jar"
$mtsCmd = Join-Path $Root "tools\MTS.cmd"
if (-not (Test-Path $mtsCmd)) { $mtsCmd = Join-Path $Root "vendor\MTS.cmd" }
if (-not (Test-Path $mtsJar)) { throw "Place ModTheSpire.jar in tools/ before packaging." }
$mtsLocal = Join-Path $Root "dist\MtsLocalLauncher.jar"
if (-not (Test-Path $mtsLocal)) { throw "Build MtsLocalLauncher.jar first." }
Copy-Item $mtsLocal (Join-Path $stage "vendor\mts-launcher.jar") -Force
Copy-Item $mtsJar (Join-Path $stage "vendor\ModTheSpire.jar") -Force
Copy-Item $mtsCmd (Join-Path $stage "vendor\MTS.cmd") -Force
if (Test-Path (Join-Path $Root "vendor\MTS-debug.cmd")) {
    Copy-Item (Join-Path $Root "vendor\MTS-debug.cmd") (Join-Path $stage "vendor\MTS-debug.cmd") -Force
}
Copy-Item (Join-Path $Root "ModTheSpire-LICENSE.txt") (Join-Path $stage "vendor\ModTheSpire-LICENSE.txt") -Force

$zip = Join-Path $Root "release\BigFishAdvisor-$Version.zip"
Remove-Item $zip -Force -ErrorAction SilentlyContinue
Compress-Archive -Path (Join-Path $stage "*") -DestinationPath $zip -Force
Write-Host "Release: $zip"
