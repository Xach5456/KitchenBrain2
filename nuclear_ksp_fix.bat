@echo off
echo ============================================================
echo KitchenBrain - NUCLEAR KSP FIX (Complete Cache Cleanup)
echo ============================================================
echo.
echo This will completely remove ALL Gradle caches and rebuild
echo from scratch with Gradle 8.10.2
echo.

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/7] Stopping ALL Gradle daemons...
call gradlew.bat --stop 2>nul
timeout /t 3 /nobreak >nul

echo.
echo [2/7] Deleting .gradle folder (user cache)...
if exist "%USERPROFILE%\.gradle\caches" (
    echo WARNING: This may take a moment...
    rmdir /s /q "%USERPROFILE%\.gradle\caches\8.13" 2>nul
    rmdir /s /q "%USERPROFILE%\.gradle\caches\9.2" 2>nul
    rmdir /s /q "%USERPROFILE%\.gradle\caches\9.2.1" 2>nul
    echo Done.
)

echo.
echo [3/7] Deleting local .gradle folder (project cache)...
if exist ".gradle" (
    rmdir /s /q .gradle 2>nul
    echo Done.
)

echo.
echo [4/7] Deleting build folders...
if exist "app\build" rmdir /s /q "app\build" 2>nul
if exist "build" rmdir /s /q "build" 2>nul
echo Done.

echo.
echo [5/7] Deleting .kotlin folder...
if exist ".kotlin" rmdir /s /q ".kotlin" 2>nul
echo Done.

echo.
echo [6/7] Verifying gradle-wrapper.properties...
findstr "gradle-8.10.2" gradle\wrapper\gradle-wrapper.properties >nul
if %errorlevel% equ 0 (
    echo CONFIRMED: Using Gradle 8.10.2
) else (
    echo WARNING: Gradle version might not be 8.10.2!
    type gradle\wrapper\gradle-wrapper.properties | findstr "distributionUrl"
)

echo.
echo [7/7] Building from scratch with Gradle 8.10.2...
echo This will download Gradle 8.10.2 (first time only)...
echo.
call gradlew.bat :app:assembleDebug --no-build-cache --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED" /C:"error:" /C:"Error:" /C:"Exception" /C:"Downloading"

echo.
echo ============================================================
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo SUCCESS: APK built successfully!
    for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo Size: %%~zA bytes
) else (
    echo BUILD FAILED: Check errors above carefully
    echo.
    echo Common issues:
    echo 1. Still seeing "jvm signature V" = KSP still using old Gradle
    echo 2. "Could not download" = Internet connection issue
    echo 3. Other errors = Copy the full error and paste it here
)
echo ============================================================
echo.
pause
