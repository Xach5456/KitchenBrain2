# Simple Google Services Fix Script
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Google Services Fix Tool" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$appDir = "C:\Users\Admin\AndroidStudioProjects\KitchenBrain2\app"

# Step 1: Clean up duplicates
Write-Host "[1/3] Cleaning duplicate google-services.json files..." -ForegroundColor Yellow

$jsonFiles = Get-ChildItem -Path $appDir -Filter "google-services*.json"
Write-Host "Found $($jsonFiles.Count) file(s):"

foreach ($file in $jsonFiles) {
    Write-Host "  - $($file.Name)"
}

if ($jsonFiles.Count -gt 1) {
    $backupDir = Join-Path $appDir "google-services-backup"
    if (!(Test-Path $backupDir)) {
        New-Item -ItemType Directory -Path $backupDir | Out-Null
    }
    
    foreach ($file in $jsonFiles) {
        if ($file.Name -ne "google-services.json") {
            Move-Item -Path $file.FullName -Destination $backupDir -Force
            Write-Host "✓ Moved '$($file.Name)' to backup" -ForegroundColor Green
        }
    }
    Write-Host "✓ Kept main 'google-services.json'" -ForegroundColor Green
} else {
    Write-Host "✓ No duplicates found" -ForegroundColor Green
}

Write-Host ""

# Step 2: Verify package name
Write-Host "[2/3] Checking package configuration..." -ForegroundColor Yellow

try {
    $jsonContent = Get-Content (Join-Path $appDir "google-services.json") -Raw
    $jsonObject = $jsonContent | ConvertFrom-Json
    
    $packageName = $jsonObject.client[0].client_info.android_client_info.package_name
    Write-Host "Package in google-services.json: $packageName" -ForegroundColor Cyan
    
    $buildGradlePath = Join-Path $appDir "build.gradle.kts"
    $buildGradleContent = Get-Content $buildGradlePath -Raw
    
    # Simple string check
    if ($buildGradleContent -match 'applicationId') {
        Write-Host "✓ applicationId found in build.gradle" -ForegroundColor Green
    }
    
    if ($packageName -eq "com.example.kitchenbrain") {
        Write-Host "✓ Package name is correct!" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Package name might be incorrect" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Step 3: SHA-1 Instructions
Write-Host "[3/3] SHA-1 Registration Instructions" -ForegroundColor Yellow
Write-Host ""
Write-Host "To fix SecurityException, you MUST register SHA-1 in Firebase Console:" -ForegroundColor Red
Write-Host ""
Write-Host "STEP 1: Get SHA-1 fingerprint" -ForegroundColor Cyan
Write-Host "Run this command in PowerShell:" -ForegroundColor White
Write-Host ""
Write-Host 'keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android' -ForegroundColor Gray
Write-Host ""
Write-Host "Copy the SHA1 value (looks like: A1:B2:C3:...)" -ForegroundColor Yellow
Write-Host ""
Write-Host "STEP 2: Add to Firebase Console" -ForegroundColor Cyan
Write-Host "1. Go to: https://console.firebase.google.com/" -ForegroundColor White
Write-Host "2. Select project: kitchen-brain-58a1e" -ForegroundColor White
Write-Host "3. Click Settings → Project Settings" -ForegroundColor White
Write-Host "4. Your apps → Add fingerprint" -ForegroundColor White
Write-Host "5. Paste SHA-1 and save" -ForegroundColor White
Write-Host "6. Download new google-services.json" -ForegroundColor White
Write-Host "7. Replace existing google-services.json" -ForegroundColor White
Write-Host ""
Write-Host "STEP 3: Rebuild project" -ForegroundColor Cyan
Write-Host "Build → Clean Project → Rebuild Project" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Done!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
