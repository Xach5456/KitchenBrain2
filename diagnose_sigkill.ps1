# SIGKILL / Force-Stop Diagnostic Script for Windows
# Run this in PowerShell as Administrator

$APP_PACKAGE = "com.example.kitchenbrain"
$LOG_FILE = "sigkill_diagnostics_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SIGKILL/Force-Stop Diagnostic Report" -ForegroundColor Cyan
Write-Host "Generated: $(Get-Date)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
"" | Out-File -Append $LOG_FILE

# 1. Check for automation tools
Write-Host "`n1. CHECKING FOR AUTOMATION TOOLS..." -ForegroundColor Yellow
Get-Process | Where-Object { $_.Name -match "gradle|appium|uiautomator|python|node" } | Format-Table | Tee-Object -Append $LOG_FILE

# 2. ADB devices
Write-Host "`n2. ADB DEVICES:" -ForegroundColor Yellow
adb devices | Tee-Object -Append $LOG_FILE

# 3. Gradle processes
Write-Host "`n3. GRADLE PROCESSES:" -ForegroundColor Yellow
Get-Process | Where-Object { $_.Name -like "*gradle*" } | Format-Table | Tee-Object -Append $LOG_FILE

# 4. Android Studio
Write-Host "`n4. ANDROID STUDIO PROCESSES:" -ForegroundColor Yellow
Get-Process | Where-Object { $_.Name -like "*studio*" -or $_.Name -like "*idea*" } | Format-Table | Tee-Object -Append $LOG_FILE

# 5. Logcat force-stop events
Write-Host "`n5. RECENT FORCE-STOP EVENTS:" -ForegroundColor Yellow
adb logcat -d | Select-String -Pattern "force-stop|am_kill|killing.*$APP_PACKAGE" | Select-Object -Last 100 | Tee-Object -Append $LOG_FILE

# 6. Package state
Write-Host "`n6. PACKAGE STATE:" -ForegroundColor Yellow
adb shell dumpsys package $APP_PACKAGE | Select-String "userId|versionCode|forceStop" | Tee-Object -Append $LOG_FILE

# 7. Check for test frameworks in project
Write-Host "`n7. TEST CONFIGURATION FILES:" -ForegroundColor Yellow
Get-ChildItem -Recurse -Include "*.gradle","*.xml","*.sh","*.bat" | Where-Object { 
    $_.LastWriteTime -gt (Get-Date).AddDays(-7) 
} | Select-Object FullName, LastWriteTime | Tee-Object -Append $LOG_FILE

# 8. Network connections (ADB port)
Write-Host "`n8. NETWORK CONNECTIONS (ADB port 5037):" -ForegroundColor Yellow
Get-NetTCPConnection -LocalPort 5037 -ErrorAction SilentlyContinue | Tee-Object -Append $LOG_FILE

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTIC COMPLETE" -ForegroundColor Green
Write-Host "Log saved to: $LOG_FILE" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nQUICK FIX COMMANDS:" -ForegroundColor Yellow
Write-Host "1. Kill all Gradle: Stop-Process -Name '*gradle*' -Force" -ForegroundColor White
Write-Host "2. Restart ADB: adb kill-server; adb start-server" -ForegroundColor White
Write-Host "3. Monitor in real-time: adb logcat -b events | Select-String 'am_kill'" -ForegroundColor White
