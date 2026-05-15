@echo off
echo ============================================================
echo KitchenBrain - NUCLEAR REINSTALL (Fixes Silent Launch)
echo ============================================================
echo.

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/7] Checking device connection...
adb devices | findstr "device"
if %errorlevel% neq 0 (
    echo ERROR: No device/emulator connected!
    echo Start an emulator first, then run this script again.
    pause
    exit /b 1
)

echo.
echo [2/7] Uninstalling old app completely...
adb uninstall com.example.kitchenbrain 2>nul
if %errorlevel% equ 0 (
    echo Old app uninstalled successfully.
) else (
    echo App was not installed (or already removed).
)

echo.
echo [3/7] Clearing Gradle cache...
call gradlew.bat --stop 2>nul
timeout /t 2 /nobreak >nul

echo.
echo [4/7] Cleaning project...
call gradlew.bat clean --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED"

echo.
echo [5/7] Building fresh APK...
call gradlew.bat :app:assembleDebug --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED"
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Build failed! Check Android Studio Build tab.
    pause
    exit /b 1
)

echo.
echo [6/7] Verifying APK exists...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo SUCCESS: APK found!
    for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo Size: %%~zA bytes
) else (
    echo ERROR: APK not found!
    pause
    exit /b 1
)

echo.
echo [7/7] Installing fresh APK...
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Installation failed!
    echo Try: adb uninstall com.example.kitchenbrain then run this script again.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo SUCCESS: Fresh APK installed!
echo ============================================================
echo.
echo FINAL STEP:
echo 1. In Android Studio, open Logcat tab
echo 2. Set filter to: "package:mine"
echo 3. Click the green RUN button (or Shift+F10)
echo 4. Watch for "MyApplication" logs
echo.
echo If the app STILL doesn't launch after this, run:
echo   adb logcat -c ^& adb logcat ^| findstr "kitchenbrain FATAL"
echo Then tap the app icon and paste the output here.
echo.
pause
