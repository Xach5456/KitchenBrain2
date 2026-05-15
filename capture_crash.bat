@echo off
echo ========================================
echo KitchenBrain2 Crash Log Capture Script
echo ========================================
echo.
echo Instructions:
echo 1. Start your app on the emulator/device
echo 2. When ready, press any key to start monitoring...
pause >nul
echo.
echo Monitoring for crashes... Press Ctrl+C to stop.
echo.
adb logcat -c
adb logcat AndroidRuntime:E Firebase:E GoogleApiManager:E *:F | findstr /C:"FATAL EXCEPTION" /C:"AndroidRuntime" /C:"java.lang" /C:"at com.example.kitchenbrain"
pause
