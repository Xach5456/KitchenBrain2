@echo off
REM ========================================
REM  COMPLETE DEVELOPER_ERROR FIX SCRIPT
REM ========================================

echo ============================================
echo  FIXING DEVELOPER_ERROR - Complete Cleanup
echo ============================================
echo.

REM Step 1: Clean build directories
echo [1/5] Cleaning build directories...
if exist "app\build" (
    rmdir /s /q app\build
    echo   - Removed app\build
) else (
    echo   - app\build not found, skipping
)

if exist ".gradle" (
    rmdir /s /q .gradle
    echo   - Removed .gradle cache
) else (
    echo   - .gradle not found, skipping
)

if exist ".idea" (
    rmdir /s /q .idea
    echo   - Removed .idea cache
) else (
    echo   - .idea not found, skipping
)

echo.

REM Step 2: Verify build.gradle.kts has App Check disabled
echo [2/5] Checking build.gradle.kts...
findstr /C:"// Firebase App Check - DISABLED FOR DEVELOPMENT" app\build.gradle.kts >nul
if %ERRORLEVEL% EQU 0 (
    echo   - App Check is disabled in build.gradle.kts
) else (
    echo   - WARNING: App Check may still be enabled!
    echo   - Please manually comment out these lines in app/build.gradle.kts:
    echo     implementation("com.google.firebase:firebase-appcheck-playintegrity")
    echo     implementation("com.google.firebase:firebase-appcheck-debug")
)

echo.

REM Step 3: Clean Gradle cache
echo [3/5] Running Gradle clean...
call gradlew.bat --stop
call gradlew.bat clean --refresh-dependencies

echo.

REM Step 4: Rebuild project
echo [4/5] Rebuilding project...
call gradlew.bat build

echo.

REM Step 5: Summary
echo [5/5] Fix complete!
echo.
echo ============================================
echo  NEXT STEPS:
echo ============================================
echo  1. Open Android Studio
echo  2. Click File -> Invalidate Caches / Restart
echo  3. Click File -> Sync Project with Gradle Files
echo  4. Build -> Clean Project
echo  5. Build -> Rebuild Project
echo  6. Run your app
echo.
echo  Expected result: NO MORE DEVELOPER_ERROR!
echo ============================================
echo.

pause
