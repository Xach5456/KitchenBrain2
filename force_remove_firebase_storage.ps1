# Force Remove Firebase Storage from ProfileModernFragment.java
# This script removes all Firebase Storage code that wasn't saved properly

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "FIREBASE STORAGE FORCE REMOVAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$file = "app\src\main\java\com\example\kitchenbrain\ProfileModernFragment.java"

if (-not (Test-Path $file)) {
    Write-Host "File not found: $file" -ForegroundColor Red
    exit 1
}

Write-Host "Reading file: $file" -ForegroundColor Yellow
$content = Get-Content $file -Raw -Encoding UTF8

$originalLength = $content.Length
Write-Host "Original file size: $originalLength characters" -ForegroundColor Gray
Write-Host ""

# Count replacements
$firebaseImports = ([regex]::Matches($content, 'import com\.google\.firebase\.storage\.(FirebaseStorage|StorageReference);')).Count
$firebaseFields = ([regex]::Matches($content, 'private FirebaseStorage storage;|private StorageReference storageRef;')).Count
$firebaseInit = ([regex]::Matches($content, 'storage = FirebaseStorage\.getInstance\(\);|storageRef = storage\.getReference\(\);')).Count

Write-Host "Found:" -ForegroundColor Yellow
Write-Host "  Firebase Storage imports: $firebaseImports" -ForegroundColor Gray
Write-Host "  Firebase Storage fields: $firebaseFields" -ForegroundColor Gray
Write-Host "  Firebase Storage initialization: $firebaseInit" -ForegroundColor Gray
Write-Host ""

if ($firebaseImports -eq 0 -and $firebaseFields -eq 0 -and $firebaseInit -eq 0) {
    Write-Host "✅ File is already clean! No Firebase Storage code found." -ForegroundColor Green
    exit 0
}

Write-Host "Removing Firebase Storage code..." -ForegroundColor Yellow

# Remove Firebase Storage imports
$content = $content -replace 'import com\.google\.firebase\.storage\.FirebaseStorage;\r?\n', ''
$content = $content -replace 'import com\.google\.firebase\.storage\.StorageReference;\r?\n', ''

# Remove fields
$content = $content -replace 'private FirebaseStorage storage;\r?\n', ''
$content = $content -replace 'private StorageReference storageRef;\r?\n', ''

# Remove initialization
$content = $content -replace 'storage = FirebaseStorage\.getInstance\(\);\r?\n', ''
$content = $content -replace 'storageRef = storage\.getReference\(\);\r?\n', ''

$newLength = $content.Length
$removed = $originalLength - $newLength

Write-Host "New file size: $newLength characters" -ForegroundColor Gray
Write-Host "Removed: $removed characters" -ForegroundColor Green
Write-Host ""

try {
    $content | Set-Content $file -Encoding UTF8 -NoNewline -Force
    Write-Host "✅ File saved successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "REMOVED:" -ForegroundColor Red
    if ($firebaseImports -gt 0) { Write-Host "  - $firebaseImports Firebase Storage imports" }
    if ($firebaseFields -gt 0) { Write-Host "  - $firebaseFields Firebase Storage fields" }
    if ($firebaseInit -gt 0) { Write-Host "  - $firebaseInit Firebase Storage initialization lines" }
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. In Android Studio: File → Invalidate Caches... → Invalidate and Restart" -ForegroundColor White
    Write-Host "2. Build → Clean Project → Rebuild Project" -ForegroundColor White
    Write-Host "3. Update Cloudinary credentials in CloudinaryHelper.kt" -ForegroundColor White
} catch {
    Write-Host "❌ Failed to save file!" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Try this instead:" -ForegroundColor Yellow
    Write-Host "1. Close Android Studio completely" -ForegroundColor White
    Write-Host "2. Edit the file manually in Notepad++ or VS Code" -ForegroundColor White
    Write-Host "3. Delete lines containing FirebaseStorage and StorageReference" -ForegroundColor White
    Write-Host "4. Save and reopen Android Studio" -ForegroundColor White
}

Write-Host ""
Write-Host "Press any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
