@echo off
echo ============================================================
echo KitchenBrain - Fix KSP JVM Signature Error
echo ============================================================
echo.
echo Downgrading Gradle from 9.2.1 to 8.10.2 (KSP compatible)
echo.

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/5] Stopping all Gradle daemons...
call gradlew.bat --stop 2>nul
timeout /t 3 /nobreak >nul

echo.
echo [2/5] Deleting Gradle cache (forces fresh download)...
if exist ".gradle" (
    echo Clearing .gradle directory...
    rmdir /s /q .gradle 2>nul
)

echo.
echo [3/5] Cleaning project...
call gradlew.bat clean --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED"

echo.
echo [4/5] Building with Gradle 8.10.2...
call gradlew.bat :app:assembleDebug --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED" /C:"error:" /C:"Error:"

echo.
echo [5/5] Checking if APK was created...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo SUCCESS: APK built successfully!
    for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo Size: %%~zA bytes
    echo.
    echo You can now run the app from Android Studio!
) else (
    echo ERROR: Build failed! Check errors above.
)

echo.
echo ============================================================
echo If build succeeded, click Run (green triangle) in Android Studio
echo ============================================================
echo.
pause
