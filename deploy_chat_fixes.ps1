# 🔥 Deploy Production Chat System Fixes
# Run this script to deploy all updates to Firebase

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  PRODUCTION CHAT SYSTEM DEPLOYMENT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check if Firebase CLI is installed
Write-Host "[1/5] Checking Firebase CLI..." -ForegroundColor Yellow
try {
    $firebaseVersion = firebase --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Firebase CLI installed: $firebaseVersion" -ForegroundColor Green
    } else {
        Write-Host "❌ Firebase CLI not found!" -ForegroundColor Red
        Write-Host "Install it with: npm install -g firebase-tools" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "❌ Firebase CLI not found!" -ForegroundColor Red
    Write-Host "Install it with: npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Step 2: Install Cloud Functions dependencies
Write-Host "[2/5] Installing Cloud Functions dependencies..." -ForegroundColor Yellow
Set-Location functions
if (Test-Path "node_modules") {
    Write-Host "⚠️  node_modules exists, updating..." -ForegroundColor Yellow
    npm update
} else {
    npm install
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Dependencies installed" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to install dependencies" -ForegroundColor Red
    exit 1
}

Set-Location ..
Write-Host ""

# Step 3: Deploy Firestore Rules
Write-Host "[3/5] Deploying Firestore Security Rules..." -ForegroundColor Yellow
firebase deploy --only firestore:rules

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Firestore Rules deployed" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to deploy Firestore Rules" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 4: Deploy Firestore Indexes
Write-Host "[4/5] Deploying Firestore Indexes..." -ForegroundColor Yellow
firebase deploy --only firestore:indexes

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Firestore Indexes deployed" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to deploy Firestore Indexes" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 5: Deploy Cloud Functions
Write-Host "[5/5] Deploying Cloud Functions..." -ForegroundColor Yellow
firebase deploy --only functions

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Cloud Functions deployed" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to deploy Cloud Functions" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ✅ DEPLOYMENT COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Yellow
Write-Host "1. Test mutual follow → auto chat creation" -ForegroundColor White
Write-Host "2. Verify pagination works (20+ chats)" -ForegroundColor White
Write-Host "3. Check offline mode" -ForegroundColor White
Write-Host "4. Monitor Cloud Functions logs:" -ForegroundColor White
Write-Host "   firebase functions:log" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  IMPORTANT:" -ForegroundColor Yellow
Write-Host "- Index may take 5-10 minutes to build" -ForegroundColor White
Write-Host "- Test with 2 different user accounts" -ForegroundColor White
Write-Host "- Check Firestore console for errors" -ForegroundColor White
Write-Host ""
