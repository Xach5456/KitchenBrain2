@echo off
echo ============================================================
echo KitchenBrain - JVM Version Fix
echo ============================================================
echo.

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/4] Checking Java version...
java -version 2>&1 | findstr "version"
echo.

echo [2/4] Stopping old Gradle daemons...
call gradlew.bat --stop 2>nul
timeout /t 2 /nobreak >nul

echo.
echo [3/4] Setting GRADLE_OPTS to use correct JDK...
set "GRADLE_OPTS=-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr"

echo.
echo [4/4] Attempting build with correct JVM...
call gradlew.bat :app:assembleDebug --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED" /C:"error:" /C:"Error:" /C:"Exception"

echo.
echo ============================================================
echo If build FAILED, check the errors above.
echo If build SUCCEEDED, run the app from Android Studio!
echo ============================================================
echo.
pause
