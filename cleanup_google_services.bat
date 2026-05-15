@echo off
echo ========================================
echo Google Services Fix - Cleaning Duplicates
echo ========================================
echo.

cd /d "C:\Users\Admin\AndroidStudioProjects\KitchenBrain2\app"

echo Checking for duplicate google-services.json files...
echo.

dir google-services*.json /b

echo.
echo Moving duplicates to backup folder...

if not exist "google-services-backup" mkdir google-services-backup

if exist "google-services (1).json" (
    move "google-services (1).json" google-services-backup\
    echo Moved: google-services (1).json
)

if exist "google-services (2).json" (
    move "google-services (2).json" google-services-backup\
    echo Moved: google-services (2).json
)

echo.
echo ✓ Cleanup complete!
echo.
echo ========================================
echo NEXT STEP: Register SHA-1 in Firebase
echo ========================================
echo.
echo Run this command to get your SHA-1:
echo.
echo keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
echo.
echo Then add it to Firebase Console:
echo https://console.firebase.google.com/
echo.
pause
