@echo off
REM ========================================
REM Firebase DEVELOPER_ERROR Fix Script
REM ========================================
echo.
echo ========================================
echo Firebase DEVELOPER_ERROR Complete Fix
echo ========================================
echo.

echo STEP 1: Getting SHA-1 fingerprint...
echo ----------------------------------------

REM Try to find Java installation
set JAVA_FOUND=0
for %%i in (java.exe) do set JAVA_PATH=%%~dpni

if exist "%JAVA_PATH%\keytool.exe" (
    echo Found keytool at: %JAVA_PATH%\keytool.exe
    "%JAVA_PATH%\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android > sha1_output.txt 2>&1
    type sha1_output.txt | findstr "SHA1:"
    echo.
    echo ^>^> Copy the SHA1 value above and add it to Firebase Console
    set JAVA_FOUND=1
) else if exist "C:\Program Files\Java\jdk-17\bin\keytool.exe" (
    echo Using standard Java path...
    "C:\Program Files\Java\jdk-17\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android > sha1_output.txt 2>&1
    type sha1_output.txt | findstr "SHA1:"
    echo.
    echo ^>^> Copy the SHA1 value above and add it to Firebase Console
    set JAVA_FOUND=1
) else (
    echo [WARNING] keytool not found! Please install JDK or set JAVA_HOME
    echo.
    echo Manual steps required:
    echo 1. Find your SHA-1 using Gradle:
    echo    gradlew signingReport
    echo.
)

echo.
echo STEP 2: Cleaning build environment...
echo ----------------------------------------

echo Stopping Gradle daemon...
call gradlew --stop
echo.

echo Deleting .gradle folder...
if exist ".gradle" (
    rmdir /s /q ".gradle"
    echo [OK] Deleted .gradle
) else (
    echo [INFO] .gradle not found, skipping
)
echo.

echo Deleting app\build folder...
if exist "app\build" (
    rmdir /s /q "app\build"
    echo [OK] Deleted app\build
) else (
    echo [INFO] app\build not found, skipping
)
echo.

echo Deleting build folder...
if exist "build" (
    rmdir /s /q "build"
    echo [OK] Deleted build
) else (
    echo [INFO] build not found, skipping
)
echo.

echo Clearing IDE caches...
if exist ".idea\caches" (
    rmdir /s /q ".idea\caches"
    echo [OK] Deleted .idea\caches
)
if exist ".idea\.name" (
    del /q ".idea\.name"
    echo [OK] Deleted .idea\.name
)
echo.

echo Running gradle clean...
call gradlew clean
echo.

echo STEP 3: Verifying configuration...
echo ----------------------------------------

REM Check google-services.json
if exist "app\google-services.json" (
    echo [OK] google-services.json found
    findstr /C:"package_name" app\google-services.json
) else (
    echo [ERROR] google-services.json NOT found!
    echo Download from: https://console.firebase.google.com/
    goto MANUAL_STEPS
)
echo.

REM Check build.gradle.kts
findstr /C:"applicationId" app\build.gradle.kts
echo.

echo ========================================
echo AUTOMATED STEPS COMPLETE!
echo ========================================
echo.
echo MANUAL STEPS REQUIRED:
echo.
echo 1. Add SHA-1 to Firebase Console:
echo    - Go to https://console.firebase.google.com/
echo    - Select project: kitchen-brain-58a1e
echo    - Project settings ^> Your apps ^> Add fingerprint
echo    - Paste SHA1 from above
echo    - Wait 2-3 minutes
echo.
echo 2. Download fresh google-services.json:
echo    - In Firebase Console, download for com.example.kitchenbrain
echo    - Replace: app\google-services.json
echo.
echo 3. Invalidate Android Studio caches:
echo    - File ^> Invalidate Caches ^> Invalidate and Restart
echo.
echo 4. Uninstall app from device/emulator:
echo    adb uninstall com.example.kitchenbrain
echo.
echo 5. Rebuild and run:
echo    gradlew assembleDebug
echo    adb install app\build\outputs\apk\debug\app-debug.apk
echo.
echo ========================================
echo.
pause

:MANUAL_STEPS
echo.
echo ========================================
echo CRITICAL: Please complete manual steps!
echo ========================================
echo.
echo See FIREBASE_DEVELOPER_ERROR_COMPLETE_FIX.md for detailed instructions.
echo.
pause
