#requires -Version 7.0
<#
.SYNOPSIS
    Sonic UI acceptance pipeline: build/lint, light/dark screenshots, six-marker checklist.

.EXAMPLE
    .\scripts\ui-verify.ps1 -Action doctor
    .\scripts\ui-verify.ps1 -Action install
    .\scripts\ui-verify.ps1 -Action capture -Screen player -Theme dark -State after
    .\scripts\ui-verify.ps1 -Action check
#>
[CmdletBinding()]
param(
    [ValidateSet('doctor', 'build', 'lint', 'install', 'launch', 'capture', 'capture-set', 'check')]
    [string]$Action = 'doctor',

    [string]$Screen,

    [ValidateSet('light', 'dark', 'current')]
    [string]$Theme = 'current',

    [string]$State = 'current',

    [string]$Serial,

    [string]$AdbPath,

    [string[]]$Screens = @('home', 'player', 'library', 'settings'),

    [switch]$FullBuild,

    [switch]$Launch,

    [int]$WaitSeconds = 2
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot
$script:Adb = $null
$script:Gradlew = $null
$script:Serial = $null

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "  OK  $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Warning $Message
}

function Resolve-Adb {
    if ($AdbPath) {
        if (-not (Test-Path $AdbPath)) { throw "adb not found at -AdbPath: $AdbPath" }
        return (Resolve-Path $AdbPath).Path
    }

    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $sdkCandidates = @()
    if ($env:ANDROID_HOME) { $sdkCandidates += $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT) { $sdkCandidates += $env:ANDROID_SDK_ROOT }

    $localProps = Join-Path $RepoRoot 'local.properties'
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
        if ($line) {
            $value = ($line -split '=', 2)[1].Trim()
            $value = $value -replace '\\\\', '\' -replace '\\:', ':' -replace '\\=', '=' -replace '\\ ', ' '
            $sdkCandidates += $value
        }
    }

    foreach ($sdk in $sdkCandidates) {
        if (-not $sdk) { continue }
        foreach ($relative in @('platform-tools\adb.exe', 'platform-tools/adb')) {
            $candidate = Join-Path $sdk $relative
            if (Test-Path $candidate) { return (Resolve-Path $candidate).Path }
        }
    }

    throw 'adb not found. Install Android platform-tools, set ANDROID_HOME/ANDROID_SDK_ROOT, or pass -AdbPath.'
}

function Resolve-Gradle {
    $bat = Join-Path $RepoRoot 'gradlew.bat'
    $sh = Join-Path $RepoRoot 'gradlew'
    if ($env:OS -eq 'Windows_NT' -and (Test-Path $bat)) { return $bat }
    if (Test-Path $sh) { return $sh }
    if (Test-Path $bat) { return $bat }
    throw "Gradle wrapper not found in $RepoRoot"
}

function Initialize-Tools {
    $script:Adb = Resolve-Adb
    $script:Gradlew = Resolve-Gradle
}

function Get-Devices {
    $output = & $script:Adb devices
    $devices = @()
    foreach ($line in $output) {
        if ($line -match '^\s*(\S+)\s+device\b') { $devices += $Matches[1] }
    }
    return $devices
}

function Resolve-Serial {
    if ($Serial) {
        $devices = @(Get-Devices)
        if ($devices -notcontains $Serial) {
            throw "Device '$Serial' is not online. Online: $($devices -join ', ')"
        }
        return $Serial
    }

    $devices = @(Get-Devices)
    if ($devices.Count -eq 0) {
        throw "No online device. Start an emulator or connect a device; check 'adb devices'."
    }
    if ($devices.Count -gt 1) {
        throw "Multiple devices online ($($devices -join ', ')). Pass -Serial <serial>."
    }
    return $devices[0]
}

function Ensure-Device {
    if (-not $script:Serial) {
        $script:Serial = Resolve-Serial
    }
    return $script:Serial
}

function Invoke-Adb {
    param([string[]]$AdbArgs)
    & $script:Adb -s $script:Serial @AdbArgs
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: adb -s $($script:Serial) $($AdbArgs -join ' ')"
    }
}

function Get-AdbText {
    param([string[]]$AdbArgs)
    $output = & $script:Adb -s $script:Serial @AdbArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: adb -s $($script:Serial) $($AdbArgs -join ' ')"
    }
    return ($output | Out-String).Trim()
}

function Get-DeviceInfo {
    $model = Get-AdbText -AdbArgs @('shell', 'getprop', 'ro.product.model')
    $api = Get-AdbText -AdbArgs @('shell', 'getprop', 'ro.build.version.sdk')
    $release = Get-AdbText -AdbArgs @('shell', 'getprop', 'ro.build.version.release')
    $packages = Get-AdbText -AdbArgs @('shell', 'pm', 'list', 'packages', 'com.cycling.sonic')
    $night = try {
        Get-AdbText -AdbArgs @('shell', 'cmd', 'uimode', 'night')
    } catch {
        'unsupported'
    }
    return [pscustomobject]@{
        Serial    = $script:Serial
        Model     = $model
        Api       = $api
        Release   = $release
        Installed = ($packages -match 'package:com\.cycling\.sonic')
        NightMode = $night
    }
}

function Get-AppVersion {
    try {
        $dump = Get-AdbText -AdbArgs @('shell', 'dumpsys', 'package', 'com.cycling.sonic')
        if ($dump -match 'versionName=([^\s]+)') { return $Matches[1] }
    } catch { }
    return 'unknown'
}

function Set-NightMode {
    param([string]$Mode)
    if ($Mode -eq 'current') { return }

    $value = if ($Mode -eq 'dark') { 'yes' } else { 'no' }
    try {
        Invoke-Adb -AdbArgs @('shell', 'cmd', 'uimode', 'night', $value) | Out-Null
        Start-Sleep -Seconds $WaitSeconds
        $current = Get-AdbText -AdbArgs @('shell', 'cmd', 'uimode', 'night')
        if ($current -notmatch "Night mode:\s*$value") {
            Write-Warn "Night mode did not report '$value' (got: $current). Toggle it manually on the device."
        } else {
            Write-Ok "Night mode: $value"
        }
    } catch {
        Write-Warn "Could not change night mode via adb: $($_.Exception.Message). Toggle it manually on the device."
    }
}

function Launch-App {
    Ensure-Device | Out-Null
    Invoke-Adb -AdbArgs @('shell', 'am', 'start', '-n', 'com.cycling.sonic/.MainActivity') | Out-Null
    Start-Sleep -Seconds $WaitSeconds
    Write-Ok 'Launched com.cycling.sonic/.MainActivity'
}

function Get-ScreenSlug {
    param([string]$Name)
    if (-not $Name) { throw '-Screen is required (e.g. -Screen home).' }
    $slug = $Name.Trim().ToLowerInvariant() -replace '[^a-z0-9]+', '-'
    $slug = $slug.Trim('-')
    if (-not $slug) { throw "Invalid -Screen value: '$Name'." }
    return $slug
}

function Get-ScreenDirectory {
    param([string]$Slug)
    $dir = Join-Path $RepoRoot "docs/design/screens/$Slug"
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    return $dir
}

function Get-ScreenshotFileName {
    param([string]$Mode, [string]$Batch)
    $themePart = if ($Mode -eq 'current') { 'current' } else { $Mode }
    if ($Batch -eq 'current' -or -not $Batch) { return "$themePart.png" }
    return "$themePart-$Batch.png"
}

function Ensure-Checklist {
    param(
        [string]$Dir,
        [string]$ScreenName,
        [string]$ShotDark,
        [string]$ShotLight
    )
    $target = Join-Path $Dir 'checklist.md'
    if (Test-Path $target) { return }

    $template = Join-Path $RepoRoot 'docs/design/screens/_template/checklist.md'
    if (-not (Test-Path $template)) { throw "Checklist template missing: $template" }

    $content = Get-Content -Raw $template
    $content = $content.Replace('{{SCREEN}}', $ScreenName)
    $content = $content.Replace('{{DATE}}', (Get-Date -Format 'yyyy-MM-dd'))
    $content = $content.Replace('{{STATE}}', $State)
    $content = $content.Replace('{{SHOT_DARK}}', $ShotDark)
    $content = $content.Replace('{{SHOT_LIGHT}}', $ShotLight)
    Set-Content -Path $target -Value $content -Encoding utf8
    Write-Ok "Created $target"
}

function Get-GitCommit {
    try {
        $value = & git -C $RepoRoot rev-parse --short HEAD 2>$null
        if ($LASTEXITCODE -eq 0 -and $value) { return ($value | Select-Object -First 1).Trim() }
    } catch { }
    return 'unknown'
}

function Write-CaptureMetadata {
    param(
        [string]$Dir,
        [string]$ScreenName,
        [string]$Mode,
        [string]$Batch,
        [string]$ShotFile
    )
    $info = Get-DeviceInfo
    $meta = @(
        '---',
        "timestamp: $(Get-Date -Format 'yyyy-MM-ddTHH:mm:ssK')",
        "screen: $ScreenName",
        "theme: $Mode",
        "state: $Batch",
        "screenshot: $ShotFile",
        "device: $($info.Serial) / $($info.Model)",
        "android: $($info.Release) (API $($info.Api))",
        "app: com.cycling.sonic $(Get-AppVersion)",
        "git: $(Get-GitCommit)"
    ) -join "`n"
    Add-Content -Path (Join-Path $Dir 'capture.txt') -Value $meta -Encoding utf8
}

function Capture-Screenshot {
    param(
        [string]$ScreenName,
        [string]$Mode,
        [string]$Batch
    )
    Ensure-Device | Out-Null

    $slug = Get-ScreenSlug $ScreenName
    $dir = Get-ScreenDirectory $slug
    $fileName = Get-ScreenshotFileName -Mode $Mode -Batch $Batch
    $target = Join-Path $dir $fileName
    $remote = '/sdcard/sonic-ui-verify.png'

    Invoke-Adb -AdbArgs @('shell', 'screencap', '-p', $remote) | Out-Null
    & $script:Adb -s $script:Serial pull $remote $target 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $target)) {
        throw "Screenshot pull failed: $remote -> $target"
    }
    Invoke-Adb -AdbArgs @('shell', 'rm', $remote) | Out-Null

    $size = (Get-Item $target).Length
    if ($size -lt 10000) {
        Write-Warn "Screenshot is only $size bytes; it may be blank."
    }

    $shotDark = Get-ScreenshotFileName -Mode 'dark' -Batch $Batch
    $shotLight = Get-ScreenshotFileName -Mode 'light' -Batch $Batch
    Ensure-Checklist -Dir $dir -ScreenName $slug -ShotDark $shotDark -ShotLight $shotLight
    Write-CaptureMetadata -Dir $dir -ScreenName $slug -Mode $Mode -Batch $Batch -ShotFile $fileName
    Write-Ok "Saved $target ($([math]::Round($size / 1KB)) KB)"
}

function Invoke-Build {
    Push-Location $RepoRoot
    try {
        $gradleArgs = if ($FullBuild) {
            @('build', '--console=plain')
        } else {
            @(':app:assembleDebug', '--console=plain')
        }
        Write-Step "Gradle $($gradleArgs -join ' ')"
        & $script:Gradlew @gradleArgs
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }
        Write-Ok 'Build passed'
    } finally {
        Pop-Location
    }
}

function Invoke-Lint {
    Push-Location $RepoRoot
    try {
        Write-Step 'Gradle :app:lint'
        & $script:Gradlew ':app:lint' '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw 'Gradle lint failed.' }
        Write-Ok 'Lint passed'
    } finally {
        Pop-Location
    }
}

function Invoke-Install {
    Ensure-Device | Out-Null
    Push-Location $RepoRoot
    $previousSerial = $env:ANDROID_SERIAL
    try {
        $env:ANDROID_SERIAL = $script:Serial
        Write-Step 'Gradle :app:installDebug'
        & $script:Gradlew ':app:installDebug' '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw 'Gradle install failed.' }
        Write-Ok 'Installed'
    } finally {
        $env:ANDROID_SERIAL = $previousSerial
        Pop-Location
    }
}

function Invoke-Doctor {
    Write-Step 'Environment'
    Write-Ok "adb: $($script:Adb)"
    Write-Ok "gradlew: $($script:Gradlew)"

    $devices = @(Get-Devices)
    if ($devices.Count -eq 0) {
        Write-Warn 'No online device. Start an emulator or connect a device.'
        return
    }
    Write-Ok "Devices: $($devices -join ', ')"

    try {
        $script:Serial = Resolve-Serial
    } catch {
        Write-Warn $_.Exception.Message
        return
    }

    $info = Get-DeviceInfo
    Write-Ok "Selected: $($info.Serial) / $($info.Model) / Android $($info.Release) (API $($info.Api))"
    Write-Ok "App installed: $($info.Installed)"
    Write-Ok "Night mode: $($info.NightMode)"
}

function Invoke-Capture {
    if (-not $Screen) { throw '-Screen is required for -Action capture.' }
    Ensure-Device | Out-Null
    if ($Launch) { Launch-App }
    Set-NightMode -Mode $Theme
    Capture-Screenshot -ScreenName $Screen -Mode $Theme -Batch $State
}

function Invoke-CaptureSet {
    Ensure-Device | Out-Null
    foreach ($screenName in $Screens) {
        foreach ($mode in @('dark', 'light')) {
            Set-NightMode -Mode $mode
            Read-Host "Navigate to '$screenName' ($mode), then press Enter to capture" | Out-Null
            Capture-Screenshot -ScreenName $screenName -Mode $mode -Batch $State
        }
    }
}

try {
    Initialize-Tools

    switch ($Action) {
        'doctor' { Invoke-Doctor }
        'build' { Invoke-Build }
        'lint' { Invoke-Lint }
        'install' { Invoke-Install }
        'launch' { Launch-App }
        'capture' { Invoke-Capture }
        'capture-set' { Invoke-CaptureSet }
        'check' {
            Invoke-Doctor
            Invoke-Build
            Invoke-Lint
            if ($Screen) { Invoke-Capture }
        }
        default { throw "Unknown -Action '$Action'." }
    }
} catch {
    Write-Host "`nERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
