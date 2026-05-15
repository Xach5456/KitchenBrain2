# Fix Google Services Configuration Script
# This script will clean up duplicate files and help diagnose Firebase issues

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Google Services / Firebase Fix Tool" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$appDir = "C:\Users\Admin\AndroidStudioProjects\KitchenBrain2\app"

# Step 1: Check for duplicate google-services.json files
Write-Host "[1/5] Checking for duplicate google-services.json files..." -ForegroundColor Yellow

$jsonFiles = Get-ChildItem -Path $appDir -Filter "google-services*.json"
Write-Host "Found $($jsonFiles.Count) google-services.json file(s):"

foreach ($file in $jsonFiles) {
    Write-Host "  - $($file.Name)" -ForegroundColor Gray
}

if ($jsonFiles.Count -gt 1) {
    Write-Host ""
    Write-Host "⚠️  WARNING: Multiple google-services.json files found!" -ForegroundColor Red
    Write-Host "This can cause conflicts and SecurityException." -ForegroundColor Red
    Write-Host ""
    
    # Keep only the main one, move duplicates to backup folder
    $backupDir = Join-Path $appDir "google-services-backup"
    if (!(Test-Path $backupDir)) {
        New-Item -ItemType Directory -Path $backupDir | Out-Null
        Write-Host "Created backup directory: $backupDir" -ForegroundColor Green
    }
    
    foreach ($file in $jsonFiles) {
        if ($file.Name -ne "google-services.json") {
            Move-Item -Path $file.FullName -Destination $backupDir -Force
            Write-Host "✓ Moved duplicate '$($file.Name)' to backup" -ForegroundColor Green
        }
    }
    
    Write-Host "✓ Kept main 'google-services.json' file" -ForegroundColor Green
} else {
    Write-Host "✓ Only one google-services.json found (good!)" -ForegroundColor Green
}

Write-Host ""

# Step 2: Verify package name in google-services.json
Write-Host "[2/5] Verifying package name in google-services.json..." -ForegroundColor Yellow

try {
    $jsonContent = Get-Content (Join-Path $appDir "google-services.json") -Raw
    $jsonObject = $jsonContent | ConvertFrom-Json
    
    $packageName = $jsonObject.client[0].client_info.android_client_info.package_name
    Write-Host "Package name in google-services.json: $packageName" -ForegroundColor Cyan
    
    # Read build.gradle.kts to get applicationId
    $buildGradlePath = Join-Path $appDir "build.gradle.kts"
    $buildGradleContent = Get-Content $buildGradlePath -Raw
    
    if ($buildGradleContent -match 'applicationId\s*=\s*"([^"]+)"') {
        $applicationId = $matches[1]
        Write-Host "Application ID in build.gradle: $applicationId" -ForegroundColor Cyan
        
        if ($packageName -eq $applicationId) {
            Write-Host "✓ Package names match!" -ForegroundColor Green
        } else {
            Write-Host "❌ ERROR: Package name mismatch!" -ForegroundColor Red
            Write-Host "  google-services.json: $packageName" -ForegroundColor Red
            Write-Host "  build.gradle.kts: $applicationId" -ForegroundColor Red
            Write-Host "This will cause SecurityException!" -ForegroundColor Red
        }
    } else {
        Write-Host "⚠️  Could not find applicationId in build.gradle.kts" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ ERROR reading google-services.json: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Step 3: Check Firebase plugin configuration
Write-Host "[3/5] Checking Firebase plugin configuration..." -ForegroundColor Yellow

$projectBuildGradle = "C:\Users\Admin\AndroidStudioProjects\KitchenBrain2\build.gradle.kts"
if (Test-Path $projectBuildGradle) {
    $content = Get-Content $projectBuildGradle -Raw
    if ($content -match 'com\.google\.gms\.google-services') {
        Write-Host "✓ Google Services plugin found in project build.gradle" -ForegroundColor Green
    } else {
        Write-Host "❌ ERROR: Google Services plugin NOT found!" -ForegroundColor Red
        Write-Host "Add this to project build.gradle.kts:" -ForegroundColor Yellow
        Write-Host '  id("com.google.gms.google-services") version "4.4.4" apply false' -ForegroundColor Cyan
    }
} else {
    Write-Host "❌ ERROR: Project build.gradle.kts not found!" -ForegroundColor Red
}

$appBuildGradle = $buildGradlePath
if (Test-Path $appBuildGradle) {
    $content = Get-Content $appBuildGradle -Raw
    if ($content -match 'id\("com\.google\.gms\.google-services"\)') {
        Write-Host "✓ Google Services plugin applied in app build.gradle" -ForegroundColor Green
    } else {
        Write-Host "❌ ERROR: Google Services plugin NOT applied in app!" -ForegroundColor Red
        Write-Host "Add to app/build.gradle.kts plugins block:" -ForegroundColor Yellow
        Write-Host '  id("com.google.gms.google-services")' -ForegroundColor Cyan
    }
}

Write-Host ""

# Step 4: Check Firebase dependencies
Write-Host "[4/5] Checking Firebase dependencies..." -ForegroundColor Yellow

if ($buildGradleContent -match 'firebase-bom') {
    Write-Host "✓ Firebase BOM found" -ForegroundColor Green
} else {
    Write-Host "⚠️  Firebase BOM not found - consider adding it" -ForegroundColor Yellow
}

if ($buildGradleContent -match 'firebase-auth') {
    Write-Host "✓ Firebase Auth dependency found" -ForegroundColor Green
} else {
    Write-Host "⚠️  Firebase Auth not found" -ForegroundColor Yellow
}

if ($buildGradleContent -match 'firebase-firestore') {
    Write-Host "✓ Firebase Firestore dependency found" -ForegroundColor Green
} else {
    Write-Host "⚠️  Firebase Firestore not found" -ForegroundColor Yellow
}

Write-Host ""

# Step 5: SHA-1 Fingerprint Check
Write-Host "[5/5] Getting SHA-1 fingerprint for Firebase registration..." -ForegroundColor Yellow
Write-Host ""

$debugKeystore = "$env:USERPROFILE\.android\debug.keystore"

if (Test-Path $debugKeystore) {
    try {
        Write-Host "Debug keystore found at: $debugKeystore" -ForegroundColor Green
        Write-Host ""
        Write-Host "To get SHA-1, run this command:" -ForegroundColor Cyan
        Write-Host "keytool -list -v -keystore `"$debugKeystore`" -alias androiddebugkey -storepass android -keypass android" -ForegroundColor Gray
        Write-Host ""
        
        # Try to extract SHA-1 automatically
        $keytoolOutput = keytool -list -v -keystore $debugKeystore -alias androiddebugkey -storepass android -keypass android 2>&1
        
        if ($keytoolOutput -match 'SHA1:\s*([A-F0-9:]+)') {
            $sha1 = $matches[1]
            Write-Host "✅ SHA-1 Fingerprint: $sha1" -ForegroundColor Green
            Write-Host ""
            Write-Host "IMPORTANT: Add this SHA-1 to Firebase Console:" -ForegroundColor Yellow
            Write-Host "1. Go to https://console.firebase.google.com/" -ForegroundColor White
            Write-Host "2. Select project: kitchen-brain-58a1e" -ForegroundColor White
            Write-Host "3. Click ⚙️ Settings → Project Settings" -ForegroundColor White
            Write-Host "4. Go to 'Your apps' section" -ForegroundColor White
            Write-Host "5. Find app: com.example.kitchenbrain" -ForegroundColor White
            Write-Host "6. Click 'Add fingerprint'" -ForegroundColor White
            Write-Host "7. Paste SHA-1: $sha1" -ForegroundColor White
            Write-Host "8. Save and re-download google-services.json" -ForegroundColor White
        } else {
            Write-Host "⚠️  Could not extract SHA-1 automatically" -ForegroundColor Yellow
            Write-Host "Run the keytool command manually shown above" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ ERROR getting SHA-1: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Try running keytool command manually" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Debug keystore not found at expected location" -ForegroundColor Red
    Write-Host "Location: $debugKeystore" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Fix Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. ✓ Duplicate google-services.json files cleaned up" -ForegroundColor Green
Write-Host "2. If you had duplicates, rebuild your project" -ForegroundColor White
Write-Host "3. Register SHA-1 in Firebase Console (see instructions above)" -ForegroundColor White
Write-Host "4. Re-download google-services.json after adding SHA-1" -ForegroundColor White
Write-Host "5. Clean & rebuild project in Android Studio" -ForegroundColor White
Write-Host ""
Write-Host "To clean and rebuild:" -ForegroundColor Cyan
Write-Host "  Build → Clean Project" -ForegroundColor Gray
Write-Host "  Build → Rebuild Project" -ForegroundColor Gray
Write-Host "  File → Invalidate Caches / Restart" -ForegroundColor Gray
Write-Host ""
Write-Host "If SecurityException persists:" -ForegroundColor Yellow
Write-Host "- Make sure device/emulator has Google Play Services installed" -ForegroundColor White
Write-Host "- Update Google Play Services on device" -ForegroundColor White
Write-Host "- Try running on different emulator with Google APIs" -ForegroundColor White
Write-Host ""
