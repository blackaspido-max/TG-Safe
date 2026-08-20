$ErrorActionPreference = "Stop"

Write-Host "=== TG Safe build ===" -ForegroundColor Cyan
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
