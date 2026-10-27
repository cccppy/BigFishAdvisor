param(
    [string]$GameDir = ""
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$ModJar = Join-Path $Root "BigFishAdvisor.jar"
if (-not (Test-Path $ModJar)) { $ModJar = Join-Path $Root "dist\BigFishAdvisor.jar" }
if (-not (Test-Path $ModJar)) { throw "BigFishAdvisor.jar not found. Run this from the release folder." }

function Find-SteamGame {
    $paths = @()
    try { $p = (Get-ItemProperty "HKCU:\Software\Valve\Steam" -ErrorAction Stop).SteamPath; if ($p) { $paths += $p } } catch {}
    try { $p = (Get-ItemProperty "HKLM:\SOFTWARE\WOW6432Node\Valve\Steam" -ErrorAction Stop).InstallPath; if ($p) { $paths += $p } } catch {}
    $paths += @("C:\Program Files (x86)\Steam", "C:\Program Files\Steam", "D:\steam", "E:\steam")
    foreach ($steam in $paths) {
        if (-not (Test-Path $steam)) { continue }
        $direct = Join-Path $steam "steamapps\common\SlayTheSpire"
        if (Test-Path (Join-Path $direct "desktop-1.0.jar")) { return $direct }
        $vdf = Join-Path $steam "steamapps\libraryfolders.vdf"
        if (Test-Path $vdf) {
            $text = Get-Content $vdf -Raw
            foreach ($m in [regex]::Matches($text, '"path"\s+"([^"]+)"')) {
                $lib = $m.Groups[1].Value -replace '\\\\', '\'
                $candidate = Join-Path $lib "steamapps\common\SlayTheSpire"
                if (Test-Path (Join-Path $candidate "desktop-1.0.jar")) { return $candidate }
            }
        }
    }
    return $null
}

if (-not $GameDir) { $GameDir = Find-SteamGame }
if (-not $GameDir -or -not (Test-Path (Join-Path $GameDir "desktop-1.0.jar"))) {
    throw "Slay the Spire not found. Pass -GameDir '...\steamapps\common\SlayTheSpire'."
}

$mtsJar = Join-Path $Root "vendor\ModTheSpire.jar"
$mtsCmd = Join-Path $Root "vendor\MTS.cmd"
if (-not (Test-Path $mtsJar)) {
    $mtsJar = Join-Path $Root "tools\ModTheSpire.jar"
    $mtsCmd = Join-Path $Root "tools\MTS.cmd"
}
if (-not (Test-Path $mtsJar)) {
    $tmp = Join-Path $env:TEMP ("bigfish-mts-" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force $tmp | Out-Null
    $zip = Join-Path $tmp "ModTheSpire.zip"
    Write-Host "Downloading ModTheSpire..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest "https://github.com/kiooeht/ModTheSpire/releases/download/v3.6.3/ModTheSpire.zip" -OutFile $zip
    Expand-Archive $zip -DestinationPath $tmp -Force
    $mtsJar = Get-ChildItem $tmp -Recurse -Filter "ModTheSpire.jar" | Select-Object -First 1
    $mtsCmd = Get-ChildItem $tmp -Recurse -Filter "MTS.cmd" | Select-Object -First 1
}

if (-not $mtsCmd -or -not (Test-Path $mtsCmd)) { throw "MTS.cmd not found." }

Write-Host "Installing into: $GameDir"
Copy-Item $mtsJar (Join-Path $GameDir "ModTheSpire.jar") -Force
Copy-Item $mtsCmd (Join-Path $GameDir "MTS.cmd") -Force
$debugCmd = Join-Path $Root "vendor\MTS-debug.cmd"
if (Test-Path $debugCmd) { Copy-Item $debugCmd (Join-Path $GameDir "MTS-debug.cmd") -Force }
$mods = Join-Path $GameDir "mods"
New-Item -ItemType Directory -Force $mods | Out-Null
Copy-Item $ModJar (Join-Path $mods "BigFishAdvisor.jar") -Force

# Steam's "Play with Mods" option runs mts-launcher.jar. The official launcher
# only accepts a Workshop copy; replace it with a local-launcher shim so the
# normal "Play with Mods" path opens the local ModTheSpire selector.
$mtsLocal = Join-Path $Root "vendor\mts-launcher.jar"
if (-not (Test-Path $mtsLocal)) { $mtsLocal = Join-Path $Root "dist\MtsLocalLauncher.jar" }
if (-not (Test-Path $mtsLocal)) { throw "mts-launcher.jar not found in the release package." }
$originalMtsLauncher = Join-Path $GameDir "mts-launcher-original.jar"
$installedMtsLauncher = Join-Path $GameDir "mts-launcher.jar"
if ((Test-Path $installedMtsLauncher) -and -not (Test-Path $originalMtsLauncher)) {
    Copy-Item $installedMtsLauncher $originalMtsLauncher -Force
}
Copy-Item $mtsLocal $installedMtsLauncher -Force

# Undo a previous direct-launch experiment, if present; "Play with Mods" is the
# supported path now.
$configPath = Join-Path $GameDir "config.json"
$backupPath = Join-Path $GameDir "config.json.bigfish-backup"
if (Test-Path $backupPath) {
    Copy-Item $backupPath $configPath -Force
    Remove-Item $backupPath -Force
}
Remove-Item (Join-Path $GameDir "BigFishLauncher.jar") -Force -ErrorAction SilentlyContinue

$cfgDir = Join-Path $env:LOCALAPPDATA "ModTheSpire"
New-Item -ItemType Directory -Force $cfgDir | Out-Null
$order = Join-Path $cfgDir "mod_order.xml"
if (Test-Path $order) {
    $raw = Get-Content $order -Raw
    if ($raw -notmatch [regex]::Escape("BigFishAdvisor.jar")) {
        if ($raw -match "</mts_cfg>") {
            $raw = $raw -replace "</mts_cfg>", "`t<mod>BigFishAdvisor.jar</mod>`r`n</mts_cfg>"
        } else {
            $raw = "<mts_cfg>`r`n`t<mod>BigFishAdvisor.jar</mod>`r`n</mts_cfg>`r`n"
        }
        Set-Content -Path $order -Value $raw -Encoding UTF8
    }
} else {
    Set-Content -Path $order -Value "<mts_cfg>`r`n`t<mod>BigFishAdvisor.jar</mod>`r`n</mts_cfg>`r`n" -Encoding UTF8
}

# A shortcut removes the need to hunt for the game folder.
try {
    $ws = New-Object -ComObject WScript.Shell
    $exe = Join-Path $GameDir "SlayTheSpire.exe"
    foreach ($dir in @([Environment]::GetFolderPath("Desktop"), $GameDir)) {
        if (-not $dir) { continue }
        $lnk = Join-Path $dir "Big Fish Advisor - Slay the Spire.lnk"
        $sc = $ws.CreateShortcut($lnk)
        $sc.TargetPath = (Join-Path $GameDir "MTS.cmd")
        $sc.WorkingDirectory = $GameDir
        $sc.IconLocation = "$exe,0"
        $sc.WindowStyle = 7
        $sc.Description = "Open ModTheSpire with Big Fish Advisor"
        $sc.Save()
    }
} catch {
    Write-Host "Could not create shortcut (non-fatal): $($_.Exception.Message)"
}

Write-Host ""
Write-Host "Done. In Steam choose 'Play with Mods'. It now opens the local ModTheSpire selector."
Write-Host "Select 'Big Fish Advisor' and press Play."
Write-Host "Or use the desktop shortcut / MTS.cmd."
Write-Host "In game, press F8 to show/hide the fish."
Write-Host ""
Write-Host "If 'Play with Mods' still fails: run MTS-debug.cmd in the game folder."
