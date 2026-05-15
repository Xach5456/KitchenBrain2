@echo off
REM Quick fix script for Firebase DEVELOPER_ERROR
REM Run this after downloading fresh google-services.json from Firebase Console

echo ========================================
echo Firebase DEVELOPER_ERROR Fix Script
echo ========================================
echo.

REM Check if adb is available
echo Checking ADB connection...
adb devices >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: ADB not found or no devices connected!
    echo Please connect your device/emulator and enable USB debugging.
    pause
    exit /b 1
)

echo ADB is working!
echo.

REM Uninstall app from device
echo Step 1: Uninstalling app from device...
adb uninstall com.example.kitchenbrain
if %errorlevel% equ 0 (
    echo SUCCESS: App uninstalled from device
) else (
    echo WARNING: App may not be installed on device
)
echo.

REM Clean Gradle cache
echo Step 2: Cleaning Gradle cache...
if exist .gradle (
    rmdir /s /q .gradle
    echo Deleted .gradle folder
)
if exist app\.gradle (
    rmdir /s /q app\.gradle
    echo Deleted app\.gradle folder
)
if exist app\build (
    rmdir /s /q app\build
    echo Deleted app\build folder
)
if exist build (
    rmdir /s /q build
    echo Deleted build folder
)
echo.

REM Verify google-services.json
echo Step 3: Verifying google-services.json...
if exist app\google-services.json (
    echo Found google-services.json
    echo Package name in file:
    findstr "package_name" app\google-services.json
) else (
    echo ERROR: google-services.json not found!
    echo Please download it from Firebase Console first.
    pause
    exit /b 1
)
echo.

echo ========================================
echo Automatic cleanup complete!
echo ========================================
echo.
echo NEXT STEPS:
echo 1. In Android Studio: File -^> Invalidate Caches... -^> Invalidate and Restart
echo 2. After restart: Build -^> Clean Project
echo 3. Then: Build -^> Rebuild Project
echo 4. Finally: Run -^> Run 'app'
echo.
echo The app will be freshly installed without old cached data.
echo.
pause
