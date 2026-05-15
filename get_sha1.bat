@echo off
echo ========================================
echo KitchenBrain2 - Get Debug SHA-1
echo ========================================
echo.
echo Getting debug keystore SHA-1 fingerprint...
echo.

keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

echo.
echo ========================================
echo IMPORTANT: Copy the SHA1 value above
echo ========================================
echo.
echo Then:
echo 1. Go to https://console.firebase.google.com/
echo 2. Select project: kitchen-brain-58a1e
echo 3. Click Settings (gear icon)
echo 4. Find your app: com.example.kitchenbrain
echo 5. Click "Add fingerprint"
echo 6. Paste the SHA1 value
echo 7. Save and download updated google-services.json
echo 8. Replace app/google-services.json
echo 9. Sync Gradle
echo.
pause
