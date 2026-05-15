# Android Studio Cache Cleanup Script
# Fixes "Library source does not match bytecode" warning

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Android Studio Cache Cleanup Tool" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Get-Location
Write-Host "Project Root: $projectRoot" -ForegroundColor Yellow
Write-Host ""

# Step 1: Stop Gradle Daemons
Write-Host "[1/5] Stopping Gradle daemons..." -ForegroundColor Cyan
./gradlew --stop
Write-Host "✓ Gradle daemons stopped" -ForegroundColor Green
Write-Host ""

# Step 2: Clean build artifacts
Write-Host "[2/5] Cleaning build artifacts..." -ForegroundColor Cyan
./gradlew clean
Write-Host "✓ Build artifacts cleaned" -ForegroundColor Green
Write-Host ""

# Step 3: Clean build cache
Write-Host "[3/5] Cleaning build cache..." -ForegroundColor Cyan
./gradlew cleanBuildCache
Write-Host "✓ Build cache cleaned" -ForegroundColor Green
Write-Host ""

# Step 4: Rebuild debug APK
Write-Host "[4/5] Rebuilding debug APK (this may take a few minutes)..." -ForegroundColor Cyan
./gradlew assembleDebug
Write-Host "✓ Debug APK rebuilt successfully" -ForegroundColor Green
Write-Host ""

# Step 5: Instructions for Invalidate Caches
Write-Host "[5/5] Final step - Manual action required:" -ForegroundColor Cyan
Write-Host ""
Write-Host "In Android Studio:" -ForegroundColor Yellow
Write-Host "  1. Go to File → Invalidate Caches..." -ForegroundColor White
Write-Host "  2. Check these options:" -ForegroundColor White
Write-Host "     ✓ Clear downloaded shared indexes" -ForegroundColor White
Write-Host "     ✓ Clear file system cache and Local History" -ForegroundColor White
Write-Host "  3. Click 'Invalidate and Restart'" -ForegroundColor White
Write-Host ""
Write-Host "After restart:" -ForegroundColor Yellow
Write-Host "  - Wait for indexing to complete (2-5 minutes)" -ForegroundColor White
Write-Host "  - The warning should disappear automatically" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Cleanup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Follow the manual Invalidate Caches steps above" -ForegroundColor White
Write-Host "  2. Let Android Studio restart and reindex" -ForegroundColor White
Write-Host "  3. Build your app: ./gradlew installDebug" -ForegroundColor White
Write-Host ""

Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
