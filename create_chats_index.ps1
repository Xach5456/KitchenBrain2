# Firestore Index Creation Script for Chats
# This script opens the Firebase Console to create the required composite index for chats collection

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Chats Firestore Index Creation" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Pre-filled index creation URL for chats collection:
# - participants ARRAY_CONTAINS
# - updatedAt DESCENDING
$indexUrl = "https://console.firebase.google.com/v1/r/project/kitchen-brain-58a1e/firestore/indexes?create_composite=Cllwcm9qZWN0cy9raXRjaGVuLWJyYWluLTU4YTFlL2RhdGFiYXNlcy8oZGVmYXVsdCkvY29sbGVjdGlvbkdyb3Vwcy9jaGF0cy9pbmRleGVzL1wBASgJKAIqB3BhcnRpY2lwYW50cxACGg0KC3VwZGF0ZWRBdBAAGg0KCGFycmF5Q29uZmlnEAMaDAoIX19uYW1lX18QAhoMCghfX25hbWVfXxAC"

Write-Host "📋 Index Details:" -ForegroundColor Yellow
Write-Host "   Collection: chats" -ForegroundColor Green
Write-Host "   Fields: participants (ARRAY_CONTAINS), updatedAt (DESCENDING)" -ForegroundColor Green
Write-Host "   Query: where arrayContains(\"participants\", userId) orderBy(\"updatedAt\")" -ForegroundColor Green
Write-Host ""

Write-Host "🚀 Opening Firebase Console..." -ForegroundColor Cyan
Start-Process $indexUrl

Write-Host ""
Write-Host "✅ Next Steps:" -ForegroundColor Yellow
Write-Host "   1. Click 'Create Index' in Firebase Console" -ForegroundColor White
Write-Host "   2. Wait 2–5 minutes for status to change from 'Building' → 'Enabled'" -ForegroundColor White
Write-Host "   3. Restart your Android app" -ForegroundColor White
Write-Host ""

Write-Host "⏱️  Build Time Estimate: 2–5 minutes" -ForegroundColor Cyan
Write-Host ""

Write-Host "📝 Verification Checklist:" -ForegroundColor Yellow
Write-Host "   [ ] No 'FAILED_PRECONDITION: The query requires an index' in Logcat" -ForegroundColor White
Write-Host "   [ ] ChatListFragment loads instantly" -ForegroundColor White
Write-Host "   [ ] Real-time updates work (new messages appear without refresh)" -ForegroundColor White
Write-Host "   [ ] No more reload loops or crashes on ChatList open" -ForegroundColor White
Write-Host ""

Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
