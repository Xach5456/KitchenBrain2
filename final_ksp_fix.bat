@echo off
echo ============================================================
echo KitchenBrain - FINAL KSP FIX (Kotlin 2.1.0 + KSP 2.1.0-1.0.29)
echo ============================================================
echo.
echo Downgraded to stable, compatible versions:
echo - Kotlin: 2.2.0 -^> 2.1.0
echo - KSP: 2.2.0-2.0.2 -^> 2.1.0-1.0.29
echo - Gradle: 9.2.1 -^> 8.10.2
echo.

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/6] Stopping ALL Gradle daemons...
call gradlew.bat --stop 2>nul
timeout /t 3 /nobreak >nul

echo.
echo [2/6] Cleaning project...
call gradlew.bat clean --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED"

echo.
echo [3/6] Syncing Gradle (will download Kotlin 2.1.0 + KSP)...
call gradlew.bat --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED" /C:"Downloading" | head -20

echo.
echo [4/6] Building app with new versions...
call gradlew.bat :app:assembleDebug --no-build-cache --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED" /C:"error:" /C:"Error:" /C:"Exception"

echo.
echo [5/6] Verifying APK...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo SUCCESS: APK created!
    for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo Size: %%~zA bytes
) else (
    echo WARNING: APK not found!
)

echo.
echo [6/6] Checking for common errors...
echo.
echo If you see any of these errors, copy the FULL output and paste it:
echo - "Plugin not found" = KSP version doesn't exist
echo - "unexpected jvm signature" = Still using wrong Gradle/Kotlin
echo - "SpillingKt" = Kotlin version mismatch
echo.
echo ============================================================
echo If build succeeded, run the app from Android Studio!
echo ============================================================
echo.
pause
