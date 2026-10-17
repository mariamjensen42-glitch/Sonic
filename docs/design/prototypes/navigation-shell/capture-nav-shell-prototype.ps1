# PROTOTYPE helper -- captures the navigation-shell A/B/C matrix.
# Run AFTER the user has built and installed the debug APK:
#   .\gradlew.bat :app:installDebug
# This script only drives adb; it does not run Gradle.
[CmdletBinding()]
param(
    [string]$AdbPath = "adb",
    [string]$Serial = "",
    [string]$OutDir = "docs/design/screens/nav-shell",
    [int]$WaitSeconds = 6
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$outPath = Join-Path $repoRoot $OutDir
New-Item -ItemType Directory -Force -Path $outPath | Out-Null

$adbBase = @()
if ($Serial) { $adbBase += @("-s", $Serial) }

& $AdbPath @adbBase get-state | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "adb is not ready. Connect a device or pass -Serial."
}

$variants = @("A", "B", "C")
$themes = @("light", "dark")
$minis = @("on", "off")

foreach ($variant in $variants) {
    foreach ($theme in $themes) {
        $night = if ($theme -eq "dark") { "yes" } else { "no" }
        & $AdbPath @adbBase shell cmd uimode night $night | Out-Null

        foreach ($mini in $minis) {
            $launch = $adbBase + @(
                "shell", "am", "start", "-S", "-n", "com.cycling.sonic/.MainActivity",
                "--ez", "prototype_nav_shell", "true",
                "--es", "prototype_variant", $variant,
                "--es", "prototype_mini", $mini,
                "--es", "prototype_theme", $theme
            )
            & $AdbPath @launch | Out-Null
            Start-Sleep -Seconds $WaitSeconds

            $name = "$($variant.ToLower())-$theme-mini-$mini.png"
            $remote = "/sdcard/sonic-nav-shell.png"
            & $AdbPath @adbBase shell screencap -p $remote | Out-Null
            & $AdbPath @adbBase pull $remote (Join-Path $outPath $name) | Out-Null
            & $AdbPath @adbBase shell rm $remote | Out-Null
            Write-Host "captured $name"
        }
    }
}

$model = "$(& $AdbPath @adbBase shell getprop ro.product.model)".Trim()
$release = "$(& $AdbPath @adbBase shell getprop ro.build.version.release)".Trim()
$sdk = "$(& $AdbPath @adbBase shell getprop ro.build.version.sdk)".Trim()
$commit = ""
try { $commit = (git -C $repoRoot rev-parse --short HEAD).Trim() } catch { }
$stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"
@(
    "device: $model",
    "android: $release (API $sdk)",
    "commit: $commit",
    "captured: $stamp",
    "matrix: A/B/C x light/dark x mini on/off"
) | Set-Content -Path (Join-Path $outPath "capture.txt") -Encoding UTF8

Write-Host "wrote $outPath"
