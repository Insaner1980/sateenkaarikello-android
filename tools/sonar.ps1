#Requires -Version 5.1

[CmdletBinding()]
param(
    [switch]$PlanOnly,

    [switch]$AllowExternalUpload,

    [ValidateRange(1, 86400)]
    [int]$GradleTimeoutSeconds = 3600
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [Console]::OutputEncoding

function Get-SonarProjectProperties {
    param([string]$Path)

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding utf8) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -gt 0) {
            $properties[$trimmed.Substring(0, $separator).Trim()] = $trimmed.Substring($separator + 1).Trim()
        }
    }

    return $properties
}

function Test-SonarTokenConfigured {
    param([string]$RepoRoot)

    if (-not [string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
        return $true
    }

    foreach ($path in @(
        (Join-Path $env:USERPROFILE ".gradle\gradle.properties"),
        (Join-Path $RepoRoot "gradle.properties")
    )) {
        if (
            (Test-Path -LiteralPath $path -PathType Leaf) -and
            (Select-String -LiteralPath $path -Pattern "^\s*systemProp\.sonar\.token\s*=" -Quiet)
        ) {
            return $true
        }
    }

    return $false
}

function Get-ProjectEmulatorSerial {
    $devices = & adb devices
    if ($LASTEXITCODE -ne 0) {
        throw "Could not list Android devices."
    }
    foreach ($device in $devices) {
        if ($device -match '^(emulator-\d+)\s+(\S+)') {
            $serial = $Matches[1]
            if ($Matches[2] -ne 'device') {
                throw "Emulator $serial is not ready; cannot safely identify its AVD."
            }
            $avdName = & adb -s $serial emu avd name
            if ($LASTEXITCODE -ne 0) {
                throw "Could not identify the AVD for $serial."
            }
            if ($avdName -contains 'Sk_API37') {
                return $serial
            }
        }
    }
}

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$propertiesPath = Join-Path $repoRoot "sonar-project.properties"
if (-not (Test-Path -LiteralPath $propertiesPath -PathType Leaf)) {
    throw "sonar-project.properties ei loytynyt: $propertiesPath"
}

$sonarProperties = Get-SonarProjectProperties -Path $propertiesPath
$projectKey = [string]$sonarProperties["sonar.projectKey"]
$hostUrl = [string]$sonarProperties["sonar.host.url"]
if ([string]::IsNullOrWhiteSpace($projectKey)) {
    throw "sonar.projectKey puuttuu sonar-project.properties-tiedostosta."
}
if ([string]::IsNullOrWhiteSpace($hostUrl)) {
    $hostUrl = "https://sonarcloud.io"
}

if ($PlanOnly) {
    Write-Output @(
        "sonar"
        "  - Gradle debug build, JVM and instrumented test coverage, and SonarCloud upload: reports/sonar.txt"
        "  - automatically starts Sk_API37 when needed and stops it after analysis"
        "  - reuses a running emulator selected with ANDROID_SERIAL, or the running Sk_API37"
        "  - requires SONAR_TOKEN or systemProp.sonar.token"
        "  - actual external upload requires -AllowExternalUpload"
        "  - project: $projectKey"
        "  - host: $hostUrl"
    )
    exit 0
}

$reportsDirectory = Join-Path $repoRoot "reports"
$reportPath = Join-Path $reportsDirectory "sonar.txt"
New-Item -ItemType Directory -Force -Path $reportsDirectory | Out-Null
Set-Content -LiteralPath $reportPath -Encoding utf8 -Value @(
    "sonar"
    "Root: $repoRoot"
    "Project: $projectKey"
    "Command: .\gradlew.bat sonar --console=plain --no-daemon"
    "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    ""
)

if (-not $AllowExternalUpload) {
    Add-Content -LiteralPath $reportPath -Encoding utf8 -Value @(
        "ERROR: EXTERNAL_UPLOAD_APPROVAL_REQUIRED"
        "Sonar-analyysi voi lahettaa lahdekoodia ja analyysimetatietoa ulkoiseen palveluun."
        "Kayta -AllowExternalUpload-valitsinta vain, kun ulkoinen lahetys on sallittu."
    )
    Get-Content -LiteralPath $reportPath
    exit 2
}

if (-not (Test-SonarTokenConfigured -RepoRoot $repoRoot)) {
    Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "ERROR: SONAR_TOKEN_MISSING"
    Get-Content -LiteralPath $reportPath
    exit 2
}

$previousAndroidSerial = $env:ANDROID_SERIAL
$startedEmulator = $false
Push-Location -LiteralPath $repoRoot
try {
    $env:SONAR_HOST_URL = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { $hostUrl }

    try {
        Import-Module "C:\Dev\Android-check\tools\CheckRuntime.psm1" -Force -ErrorAction Stop
        if ($env:ANDROID_SERIAL -notmatch '^emulator-\d+$') {
            $env:ANDROID_SERIAL = Get-ProjectEmulatorSerial
            if (-not $env:ANDROID_SERIAL) {
                Write-Output "Starting Sonar coverage emulator Sk_API37..."
                $startedEmulator = $true
                & android emulator start Sk_API37 --headless
                if ($LASTEXITCODE -ne 0) {
                    throw "Could not start Sk_API37."
                }
                $env:ANDROID_SERIAL = Get-ProjectEmulatorSerial
            }
        }
        if ($env:ANDROID_SERIAL -notmatch '^emulator-\d+$') {
            throw "Sk_API37 did not become available."
        }
        $deviceState = & adb -s $env:ANDROID_SERIAL get-state
        if ($LASTEXITCODE -ne 0 -or $deviceState -ne 'device') {
            throw "Selected emulator $env:ANDROID_SERIAL is not ready."
        }
        $deviceMessage = "Coverage emulator: $env:ANDROID_SERIAL"
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value $deviceMessage
        Write-Output $deviceMessage
        $result = Invoke-ManagedProcess `
            -Executable (Join-Path $repoRoot "gradlew.bat") `
            -Arguments @("sonar", "--console=plain", "--no-daemon") `
            -WorkingDirectory $repoRoot `
            -TimeoutSeconds $GradleTimeoutSeconds
    }
    catch {
        $message = "ERROR: SONAR_ANALYSIS_PROCESS_ERROR: $($_.Exception.Message)"
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value $message
        Write-Output $message
        exit 2
    }

    foreach ($text in @($result.StandardOutput, $result.StandardError)) {
        if (-not [string]::IsNullOrWhiteSpace($text)) {
            Add-Content -LiteralPath $reportPath -Encoding utf8 -Value $text
            Write-Output $text
        }
    }

    if ($result.TimedOut) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "ERROR: SONAR_ANALYSIS_TIMEOUT ($GradleTimeoutSeconds s)"
        exit 2
    }
    if ($result.ExitCode -ne 0) {
        Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "ERROR: SONAR_ANALYSIS_FAILED (exit $($result.ExitCode))"
        exit 2
    }

    Add-Content -LiteralPath $reportPath -Encoding utf8 -Value @(
        "SONAR_ANALYSIS_UPLOAD_COMPLETED"
        "The JaCoCo import combines JVM unit-test and emulator instrumentation coverage."
    )
    exit 0
}
finally {
    try {
        if ($startedEmulator) {
            & android emulator stop Sk_API37
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "Could not stop the Sk_API37 emulator started for Sonar."
            }
        }
    }
    finally {
        $env:ANDROID_SERIAL = $previousAndroidSerial
        Pop-Location
    }
}
