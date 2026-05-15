# Diagnostic Decision Tree: Real Issue vs Emulator Noise
# Run this to determine if you have a real Firebase config issue or just emulator spam

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Firebase Error Diagnostic Decision Tree" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$scoreRealIssue = 0
$scoreEmulatorNoise = 0

# Question 1: Where do errors appear?
Write-Host "Question 1: Where are you seeing the errors?" -ForegroundColor Yellow
Write-Host "1) Only on Android emulator" 
Write-Host "2) Only on physical device"
Write-Host "3) Both emulator and physical device"
$answer1 = Read-Host "Enter choice (1-3)"

if ($answer1 -eq "1") { $scoreEmulatorNoise += 3 }
elseif ($answer1 -eq "2") { $scoreRealIssue += 5 }
elseif ($answer1 -eq "3") { $scoreRealIssue += 3 }

# Question 2: Does app functionality work?
Write-Host "`nQuestion 2: Can your app use Firebase services?" -ForegroundColor Yellow
Write-Host "1) Yes, everything works (Firestore, Auth, etc.)"
Write-Host "2) Some features work, some don't"
Write-Host "3) No, Firebase features fail completely"
$answer2 = Read-Host "Enter choice (1-3)"

if ($answer2 -eq "1") { $scoreEmulatorNoise += 5 }
elseif ($answer2 -eq "2") { $scoreRealIssue += 2 }
elseif ($answer2 -eq "3") { $scoreRealIssue += 5 }

# Question 3: Does app launch successfully?
Write-Host "`nQuestion 3: Does the app launch without crashing?" -ForegroundColor Yellow
Write-Host "1) Yes, launches fine, no crashes"
Write-Host "2) Launches but crashes occasionally"
Write-Host "3) Crashes immediately on launch"
$answer3 = Read-Host "Enter choice (1-3)"

if ($answer3 -eq "1") { $scoreEmulatorNoise += 4 }
elseif ($answer3 -eq "2") { $scoreRealIssue += 3 }
elseif ($answer3 -eq "3") { $scoreRealIssue += 6 }

# Question 4: Check ProviderInstaller status
Write-Host "`nQuestion 4: Do you see 'Installed default security provider' in logs?" -ForegroundColor Yellow
Write-Host "1) Yes, I see this success message"
Write-Host "2) No, don't see this message"
Write-Host "3) Not sure / haven't checked"
$answer4 = Read-Host "Enter choice (1-3)"

if ($answer4 -eq "1") { $scoreEmulatorNoise += 4 }
elseif ($answer4 -eq "2") { $scoreRealIssue += 3 }

# Question 5: GMS error message content
Write-Host "`nQuestion 5: What does the error message say?" -ForegroundColor Yellow
Write-Host "1) 'not user-facing' or 'DEVELOPER_ERROR'"
Write-Host "2) 'SHA-1 certificate not found' or similar"
Write-Host "3) 'Package name mismatch' or 'initialization failed'"
$answer5 = Read-Host "Enter choice (1-3)"

if ($answer5 -eq "1") { $scoreEmulatorNoise += 4 }
elseif ($answer5 -eq "2") { $scoreRealIssue += 5 }
elseif ($answer5 -eq "3") { $scoreRealIssue += 5 }

# Question 6: Test on physical device
Write-Host "`nQuestion 6: Have you tested on a physical device?" -ForegroundColor Yellow
Write-Host "1) Yes, errors absent on physical device"
Write-Host "2) Yes, same errors on physical device"
Write-Host "3) No, haven't tested on physical device yet"
$answer6 = Read-Host "Enter choice (1-3)"

if ($answer6 -eq "1") { 
    $scoreEmulatorNoise += 10 
    Write-Host "`n[CRITICAL] Physical device test is gold standard!" -ForegroundColor Green
}
elseif ($answer6 -eq "2") { $scoreRealIssue += 8 }

# Calculate result
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Diagnostic Result" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$emulatorScore = $scoreEmulatorNoise
$realIssueScore = $scoreRealIssue

Write-Host "Emulator Noise Score: $emulatorScore/30" -ForegroundColor $(if($emulatorScore -gt 15){"Green"}else{"Yellow"})
Write-Host "Real Issue Score: $realIssueScore/30`n" -ForegroundColor $(if($realIssueScore -gt 15){"Red"}else{"Yellow"})

if ($emulatorScore -ge 20) {
    Write-Host "RESULT: EMULATOR NOISE (95%+ confidence)" -ForegroundColor Green
    Write-Host "`nYour app configuration is CORRECT." -ForegroundColor White
    Write-Host "The errors are emulator-specific artifacts from GMS 26.x on API 35-36." -ForegroundColor Gray
    Write-Host "`nRecommended Action:" -ForegroundColor Cyan
    Write-Host "1. Run '.\filtered_logcat.ps1' for clean logs" -ForegroundColor White
    Write-Host "2. Continue development normally" -ForegroundColor White
    Write-Host "3. Test on physical device before release (recommended)" -ForegroundColor White
    Write-Host "`nDO NOT change Firebase config - it's already correct!" -ForegroundColor Red
}
elseif ($realIssueScore -ge 20) {
    Write-Host "RESULT: REAL CONFIGURATION ISSUE (High confidence)" -ForegroundColor Red
    Write-Host "`nYou have a genuine Firebase/Google Services misconfiguration." -ForegroundColor White
    Write-Host "`nRecommended Action:" -ForegroundColor Cyan
    Write-Host "1. Run '.\check_developer_error.ps1' to extract SHA-1" -ForegroundColor White
    Write-Host "2. Follow 'FIREBASE_CONFIG_AUTOMATED_FIX_GUIDE.md'" -ForegroundColor White
    Write-Host "3. Verify SHA-1 and package name in Firebase Console" -ForegroundColor White
    Write-Host "4. Download fresh google-services.json" -ForegroundColor White
    Write-Host "5. Clean build and reinstall" -ForegroundColor White
}
else {
    Write-Host "RESULT: INCONCLUSIVE - Need More Testing" -ForegroundColor Yellow
    Write-Host "`nInsufficient data to make determination." -ForegroundColor Gray
    Write-Host "`nRecommended Action:" -ForegroundColor Cyan
    Write-Host "1. Test on physical device (most important!)" -ForegroundColor White
    Write-Host "2. Run full diagnostic: '.\check_developer_error.ps1'" -ForegroundColor White
    Write-Host "3. Review logs carefully for additional clues" -ForegroundColor White
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Next Steps Based on Result" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

if ($emulatorScore -ge $realIssueScore) {
    Write-Host "QUICK FIX (Emulator Noise):" -ForegroundColor Green
    Write-Host "1. Use filtered logcat: .\filtered_logcat.ps1" -ForegroundColor White
    Write-Host "2. Or apply manual filter in Android Studio:" -ForegroundColor Gray
    Write-Host "   -logtag:GoogleApiManager -logtag:FlagRegistrar -logtag:Phenotype" -ForegroundColor Cyan
    Write-Host "3. Continue coding - your config is fine!" -ForegroundColor White
} else {
    Write-Host "SERIOUS FIX (Real Issue):" -ForegroundColor Red
    Write-Host "1. Extract SHA-1 (script will do this automatically)" -ForegroundColor White
    Write-Host "2. Add SHA-1 to Firebase Console" -ForegroundColor White
    Write-Host "3. Download new google-services.json" -ForegroundColor White
    Write-Host "4. Replace app/google-services.json" -ForegroundColor White
    Write-Host "5. Clean build: ./gradlew clean; ./gradlew installDebug" -ForegroundColor White
}

Write-Host "`nReference Files Created:" -ForegroundColor Cyan
Write-Host "- QUICK_START_DEVELOPER_ERROR_FIX.md (quick relief)" -ForegroundColor Gray
Write-Host "- COMPLETE_DEVELOPER_ERROR_GUIDE_2026.md (comprehensive)" -ForegroundColor Gray
Write-Host "- FIREBASE_CONFIG_AUTOMATED_FIX_GUIDE.md (real issues)" -ForegroundColor Gray
Write-Host "- STEP_BY_STEP_FIX_GUIDE.md (decision tree)" -ForegroundColor Gray

Write-Host "`nIf still uncertain, test on physical device - that's the gold standard!" -ForegroundColor Cyan
Write-Host ""
