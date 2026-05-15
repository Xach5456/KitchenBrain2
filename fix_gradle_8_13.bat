@echo off
echo ============================================================
echo KitchenBrain - GRADLE 8.13 FIX (AGP 8.13.2 compatible)
echo ============================================================
echo.
echo Final version matrix:
echo - AGP: 8.13.2
echo - Gradle: 8.13 (minimum required by AGP 8.13.2)
echo - Kotlin: 2.1.0
echo - KSP: 2.1.0-1.0.29
echo - Java: 17
echo.

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/5] Stopping ALL Gradle daemons...
call gradlew.bat --stop 2>nul
timeout /t 3 /nobreak >nul

echo.
echo [2/5] Cleaning project...
call gradlew.bat clean --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED"

echo.
echo [3/5] Downloading Gradle 8.13...
call gradlew.bat --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED" /C:"Downloading" | head -10

echo.
echo [4/5] Building with Gradle 8.13...
call gradlew.bat :app:assembleDebug --no-build-cache --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED" /C:"error:" /C:"Error:" /C:"Exception"

echo.
echo [5/5] Checking result...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ============================================================
    echo SUCCESS: APK built!
    for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo Size: %%~zA bytes
    echo ============================================================
    echo.
    echo Now run the app from Android Studio (green Run button)!
) else (
    echo.
    echo ============================================================
    echo BUILD FAILED: Check errors above
    echo ============================================================
    echo.
    echo Common issues:
    echo - "unexpected jvm signature" = KSP issue
    echo - "Plugin not found" = Version doesn't exist
    echo - Other = Copy the full error and paste it here
)
echo.
pause
