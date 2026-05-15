# Capture Real Crash Logs for SIGKILL Process Death
# This script will help you find the REAL error before "channel is unrecoverably broken"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CRASH LOG CAPTURE TOOL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This tool will:" -ForegroundColor Yellow
Write-Host "1. Clear old logcat logs" -ForegroundColor White
Write-Host "2. Start fresh logging" -ForegroundColor White
Write-Host "3. Filter for crash-related messages" -ForegroundColor White
Write-Host ""
Write-Host "INSTRUCTIONS:" -ForegroundColor Green
Write-Host "1. Run this script FIRST" -ForegroundColor White
Write-Host "2. Then launch your app in Android Studio" -ForegroundColor White
Write-Host "3. Watch for crash logs appearing below" -ForegroundColor White
Write-Host "4. Look for errors 2-3 seconds BEFORE 'channel is unrecoverably broken'" -ForegroundColor White
Write-Host ""
Write-Host "Starting log capture..." -ForegroundColor Cyan
Write-Host ""

# Clear old logs
adb logcat -c

# Start capturing logs with filters for crash detection
# Using -v threadtime for better readability
adb logcat -v threadtime `
    | Select-String -Pattern `
        "com\.example\.kitchenbrain|" +
        "FATAL EXCEPTION|" +
        "CRASH_HANDLER|" +
        "uncaughtException|" +
        "sigsegv|" +
        "SIGABRT|" +
        "native crash|" +
        "JNI DETECTED|" +
        "AndroidRuntime|" +
        "WindowManager.*DIED|" +
        "InputDispatcher.*broken" `
    -Context 15,5

# If the above fails, fallback to simpler filter
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Switching to simpler filter..." -ForegroundColor Yellow
    adb logcat -v time | Select-String -Pattern "com.example.kitchenbrain|FATAL|CRASH" -Context 10
}
