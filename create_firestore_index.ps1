# Firestore Index Creation Script for Notifications
# This script opens the Firebase Console to create the required index

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Firestore Index Creation Assistant" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$indexUrl = "https://console.firebase.google.com/v1/r/project/kitchen-brain-58a1e/firestore/indexes?create_composite=Cllwcm9qZWN0cy9raXRjaGVuLWJyYWluLTU4YTFlL2RhdGFiYXNlcy8oZGVmYXVsdCkvY29sbGVjdGlvbkdyb3Vwcy9ub3RpZmljYXRpb25zL2luZGV4ZXMvXxABGg4KCnJlY2VpdmVySWQQARoNCgljcmVhdGVkQXQQAhoMCghfX25hbWVfXxAC"

Write-Host "📋 Index Details:" -ForegroundColor Yellow
Write-Host "   Collection: notifications" -ForegroundColor Green
Write-Host "   Fields: receiverId (ASC), createdAt (DESC)" -ForegroundColor Green
Write-Host "   Query: where receiverId == ? orderBy createdAt DESC" -ForegroundColor Green
Write-Host ""

Write-Host "🚀 Opening Firebase Console..." -ForegroundColor Cyan
Start-Process $indexUrl

Write-Host ""
Write-Host "✅ Next Steps:" -ForegroundColor Yellow
Write-Host "   1. Click the 'Create' button in Firebase Console" -ForegroundColor White
Write-Host "   2. Wait 1-5 minutes for index to build (status: Building → Enabled)" -ForegroundColor White
Write-Host "   3. Restart your Android app" -ForegroundColor White
Write-Host ""

Write-Host "⏱️  Build Time Estimate: 1-5 minutes" -ForegroundColor Cyan
Write-Host ""

Write-Host "📝 Verification Checklist:" -ForegroundColor Yellow
Write-Host "   [ ] No 'index required' error in Logcat" -ForegroundColor White
Write-Host "   [ ] Notifications load successfully" -ForegroundColor White
Write-Host "   [ ] Sorted by date (newest first)" -ForegroundColor White
Write-Host "   [ ] Smooth scrolling performance" -ForegroundColor White
Write-Host ""

Write-Host "🔧 If automatic link doesn't work, create manually:" -ForegroundColor Yellow
Write-Host "   1. Go to console.firebase.google.com" -ForegroundColor White
Write-Host "   2. Select project: kitchen-brain-58a1e" -ForegroundColor White
Write-Host "   3. Firestore Database → Indexes tab" -ForegroundColor White
Write-Host "   4. Add Index → Collection: notifications" -ForegroundColor White
Write-Host "   5. Fields: receiverId (ASC), createdAt (DESC)" -ForegroundColor White
Write-Host ""

Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
