$ErrorActionPreference = "Stop"

Write-Host "=== TG Safe build ===" -ForegroundColor Cyan

if ([string]::IsNullOrWhiteSpace($env:TGSAFE_API_ID) -or [string]::IsNullOrWhiteSpace($env:TGSAFE_API_HASH)) {
    Write-Warning "TGSAFE_API_ID / TGSAFE_API_HASH are not set. The APK will use Telegram's public source placeholders; login may be rejected."
    Write-Host "For a private login-capable build, set both environment variables before running this script." -ForegroundColor Yellow
} else {
    Write-Host "Custom Telegram API credentials detected for this build." -ForegroundColor Green
}

Write-Host "1/4  Updating Telegram submodules..."
git submodule update --init --recursive --depth=1

Write-Host "2/4  Applying and verifying fail-closed safety guards..."
& .\gradlew.bat applyTgSafePatches --no-daemon
if ($LASTEXITCODE -ne 0) { throw "TG Safe safety patch validation failed. APK will NOT be built." }

Write-Host "3/4  Building afatDebug APK..."
& .\gradlew.bat :TMessagesProj_App:assembleAfatDebug --no-daemon
if ($LASTEXITCODE -ne 0) { throw "Android build failed." }

$sourceApk = Join-Path $PSScriptRoot "TMessagesProj_App\build\outputs\apk\afat\debug\app.apk"
$targetApk = Join-Path $PSScriptRoot "TG-Safe.apk"

if (-not (Test-Path $sourceApk)) {
    throw "Gradle completed but APK was not found at: $sourceApk"
}

Copy-Item $sourceApk $targetApk -Force

Write-Host "4/4  Done." -ForegroundColor Green
Write-Host "APK: $targetApk" -ForegroundColor Green
