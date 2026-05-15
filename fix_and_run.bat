@echo off
echo ============================================================
echo KitchenBrain - Complete App Launch Diagnostic
echo ============================================================
echo.

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/6] Stopping old Gradle daemons...
call gradlew.bat --stop 2>nul
timeout /t 2 /nobreak >nul

echo.
echo [2/6] Cleaning project...
call gradlew.bat clean --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED"
if %errorlevel% neq 0 (
    echo ERROR: Clean failed! Check Android Studio for details.
    pause
    exit /b 1
)

echo.
echo [3/6] Building debug APK...
call gradlew.bat :app:assembleDebug --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED" /C:"error:"
if %errorlevel% neq 0 (
    echo ERROR: Build failed! See errors above.
    pause
    exit /b 1
)

echo.
echo [4/6] Checking if APK was created...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo SUCCESS: APK found at app\build\outputs\apk\debug\app-debug.apk
    for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo Size: %%~zA bytes
) else (
    echo ERROR: APK not found! Build may have failed silently.
    pause
    exit /b 1
)

echo.
echo [5/6] Checking device connection...
adb devices | findstr "device"
if %errorlevel% neq 0 (
    echo ERROR: No device/emulator connected!
    echo Please start an emulator or connect a device.
    pause
    exit /b 1
)

echo.
echo [6/6] Installing APK...
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
if %errorlevel% neq 0 (
    echo ERROR: Installation failed!
    pause
    exit /b 1
)

echo.
echo ============================================================
echo SUCCESS: App installed!
echo ============================================================
echo.
echo NEXT STEPS in Android Studio:
echo 1. Open "Logcat" tab (bottom panel)
echo 2. Select your emulator/device from dropdown
echo 3. In filter box, type: "package:mine"
echo 4. Click Run button (green triangle) or press Shift+F10
echo 5. Watch logcat for "MyApplication" logs
echo.
echo If app still doesn't launch, run this command:
echo   adb logcat -c ^& adb logcat ^| findstr "kitchenbrain FATAL AndroidRuntime"
echo.
echo Then tap the app icon on emulator and check output.
echo.
pause
