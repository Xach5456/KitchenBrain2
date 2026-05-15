# ========================================
#  DEVELOPER_ERROR COMPLETE FIX
# ========================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  FIXING DEVELOPER_ERROR - Complete Cleanup" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Stop Gradle daemons
Write-Host "[1/6] Stopping Gradle daemons..." -ForegroundColor Yellow
.\gradlew --stop

# Step 2: Clean build directories
Write-Host "[2/6] Cleaning build directories..." -ForegroundColor Yellow
if (Test-Path "app\build") {
    Remove-Item -Path "app\build" -Recurse -Force
    Write-Host "   - Removed app\build" -ForegroundColor Green
}
if (Test-Path ".gradle") {
    Remove-Item -Path ".gradle" -Recurse -Force
    Write-Host "   - Removed .gradle cache" -ForegroundColor Green
}
if (Test-Path ".idea") {
    Remove-Item -Path ".idea" -Recurse -Force
    Write-Host "   - Removed .idea cache" -ForegroundColor Green
}

# Step 3: Verify App Check is disabled
Write-Host "[3/6] Verifying build.gradle.kts..." -ForegroundColor Yellow
$content = Get-Content "app\build.gradle.kts" -Raw
if ($content -match "// Firebase App Check - DISABLED FOR DEVELOPMENT") {
    Write-Host "   - App Check is disabled in build.gradle.kts" -ForegroundColor Green
} else {
    Write-Host "   - WARNING: App Check may still be enabled!" -ForegroundColor Red
    Write-Host "   - Please manually comment out App Check dependencies" -ForegroundColor Yellow
}

# Step 4: Clean Gradle cache and rebuild
Write-Host "[4/6] Running Gradle clean with refreshed dependencies..." -ForegroundColor Yellow
.\gradlew clean --refresh-dependencies

# Step 5: Build project
Write-Host "[5/6] Building project..." -ForegroundColor Yellow
.\gradlew build

# Step 6: Summary
Write-Host ""
Write-Host "[6/6] Fix complete!" -ForegroundColor Green
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  NEXT STEPS IN ANDROID STUDIO:" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  1. File -> Invalidate Caches / Restart" -ForegroundColor White
Write-Host "  2. File -> Sync Project with Gradle Files" -ForegroundColor White
Write-Host "  3. Build -> Clean Project" -ForegroundColor White
Write-Host "  4. Build -> Rebuild Project" -ForegroundColor White
Write-Host "  5. Run your app" -ForegroundColor White
Write-Host ""
Write-Host "  Expected result: NO MORE DEVELOPER_ERROR!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
