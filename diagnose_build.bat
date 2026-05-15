@echo off
echo ========================================
echo KitchenBrain - Build Diagnostics
echo ========================================
echo.

REM Set JAVA_HOME
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Step 1: Stopping Gradle daemons...
call gradlew.bat --stop
timeout /t 3 /nobreak >nul

echo.
echo Step 2: Cleaning project...
call gradlew.bat clean --console=plain
if %errorlevel% neq 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)

echo.
echo Step 3: Building project with KSP...
call gradlew.bat :app:kspDebugKotlin --console=plain --info 2>&1 | findstr /C:"FAILED" /C:"error:" /C:"Error:" /C:"Exception"
if %errorlevel% neq 0 (
    echo.
    echo WARNING: KSP may have encountered issues. Checking output...
)

echo.
echo Step 4: Checking if AIChatDatabase_Impl was generated...
if exist "app\build\generated\ksp\debug\java\com\example\kitchenbrain\data\AIChatDatabase_Impl.java" (
    echo SUCCESS: AIChatDatabase_Impl.java found!
) else (
    echo ERROR: AIChatDatabase_Impl.java NOT found!
    echo.
    echo This means KSP failed to generate the Room implementation.
    echo Please check the build output above for errors.
)

echo.
echo Step 5: Attempting full build...
call gradlew.bat :app:assembleDebug --console=plain 2>&1 | findstr /C:"BUILD" /C:"FAILED"

echo.
echo ========================================
echo Diagnostics Complete
echo ========================================
echo.
echo NEXT STEPS:
echo 1. Open Android Studio
echo 2. Click File -> Invalidate Caches...
echo 3. Check "Clear file system cache" and click "Invalidate and Restart"
echo 4. After restart, click Build -> Clean Project
echo 5. Then click Build -> Rebuild Project
echo 6. Try running the app
echo.
pause
