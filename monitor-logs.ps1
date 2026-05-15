# KitchenBrain2 Log Monitor Script
# This script monitors your app's logs in real-time

$env:ANDROID_HOME = "C:\Users\Admin\AppData\Local\Android\Sdk"
$ADB = "$env:ANDROID_HOME\platform-tools\adb.exe"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KitchenBrain2 Log Monitor" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if device is connected
$devices = & $ADB devices | Select-String "device$"
if (-not $devices) {
    Write-Host "❌ No device connected!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please:" -ForegroundColor Yellow
    Write-Host "  1. Connect your Android device via USB, OR" -ForegroundColor Yellow
    Write-Host "  2. Start an emulator from Android Studio" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Then run this script again." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Device connected!" -ForegroundColor Green
Write-Host ""
Write-Host "Monitoring logs for:" -ForegroundColor Cyan
Write-Host "  • CloudinaryHelper (image uploads)" -ForegroundColor White
Write-Host "  • HomeFragment (news loading)" -ForegroundColor White
Write-Host "  • ChatFragmentPremium (chat features)" -ForegroundColor White
Write-Host "  • MainActivity (app lifecycle)" -ForegroundColor White
Write-Host ""
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Start logcat with filters
& $ADB logcat -s `
    CloudinaryHelper:D `
    HomeFragment:D `
    ChatFragmentPremium:D `
    MainActivity:D `
    *:E
