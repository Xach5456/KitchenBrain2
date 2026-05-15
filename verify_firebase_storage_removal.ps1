# Firebase Storage Removal Verification Script
# This script checks if Firebase Storage code has been completely removed

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "FIREBASE STORAGE REMOVAL VERIFICATION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$projectRoot = "C:\Users\Admin\AndroidStudioProjects\KitchenBrain2"
$javaFilesPath = "$projectRoot\app\src\main\java\com\example\kitchenbrain"

# Search patterns
$patterns = @(
    "FirebaseStorage",
    "StorageReference", 
    "firebase-storage",
    "storage\.child\(",
    "storage\.getReference\(\)"
)

Write-Host "Scanning Java files for Firebase Storage references..." -ForegroundColor Yellow
Write-Host ""

$foundIssues = $false

foreach ($pattern in $patterns) {
    Write-Host "Searching for pattern: $pattern" -ForegroundColor Gray
    
    # Search only in .java files (exclude .md documentation files)
    $results = Get-ChildItem -Path $javaFilesPath -Recurse -Filter "*.java" | 
        Select-String -Pattern $pattern -CaseSensitive:$false
    
    if ($results.Count -gt 0) {
        Write-Host "  ❌ FOUND $($results.Count) match(es)!" -ForegroundColor Red
        foreach ($result in $results) {
            Write-Host "     File: $($result.Path)" -ForegroundColor Red
            Write-Host "     Line $($result.LineNumber): $($result.Line.Trim())" -ForegroundColor Red
        }
        $foundIssues = $true
    } else {
        Write-Host "  ✅ Clean - No matches found" -ForegroundColor Green
    }
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BUILD.GRADLE.KTS CHECK" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$buildGradlePath = "$projectRoot\app\build.gradle.kts"
if (Test-Path $buildGradlePath) {
    $content = Get-Content $buildGradlePath -Raw
    
    if ($content -match "firebase-storage") {
        Write-Host "❌ Firebase Storage dependency FOUND in build.gradle.kts!" -ForegroundColor Red
    } else {
        Write-Host "✅ Firebase Storage dependency NOT FOUND (removed successfully)" -ForegroundColor Green
    }
    
    if ($content -match "cloudinary-android") {
        Write-Host "✅ Cloudinary dependency FOUND (replacement added)" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Cloudinary dependency NOT FOUND (may need to add)" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ build.gradle.kts not found at expected location!" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "FINAL STATUS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not $foundIssues) {
    Write-Host "🎉 SUCCESS! Firebase Storage has been completely removed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. In Android Studio: File → Invalidate Caches... → Invalidate and Restart" -ForegroundColor White
    Write-Host "2. Update Cloudinary credentials in CloudinaryHelper.kt" -ForegroundColor White
    Write-Host "3. Build → Clean Project → Rebuild Project" -ForegroundColor White
} else {
    Write-Host "⚠️  Firebase Storage code still detected!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Action required:" -ForegroundColor Yellow
    Write-Host "1. Remove the Firebase Storage imports/fields shown above" -ForegroundColor White
    Write-Host "2. Replace with CloudinaryHelper.uploadImage() calls" -ForegroundColor White
    Write-Host "3. Invalidate IDE caches after cleanup" -ForegroundColor White
}

Write-Host ""
Write-Host "Press any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
