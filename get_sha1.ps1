# DEVELOPER_ERROR FIX - Get SHA-1 Fingerprint
# Run this script in PowerShell

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  GET SHA-1 FINGERPRINT FOR FIREBASE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Common JDK locations
$jdkPaths = @(
    "C:\Program Files\Java\jdk-17\bin\keytool.exe",
    "C:\Program Files\Java\jdk-11\bin\keytool.exe",
    "C:\Program Files (x86)\Java\jdk-17\bin\keytool.exe",
    "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
)

$keytoolPath = $null
foreach ($path in $jdkPaths) {
    if (Test-Path $path) {
        $keytoolPath = $path
        break
    }
}

if ($null -eq $keytoolPath) {
    # Try to find keytool in PATH
    $keytoolInPath = Get-Command keytool -ErrorAction SilentlyContinue
    if ($null -ne $keytoolInPath) {
        $keytoolPath = $keytoolInPath.Source
    }
}

if ($null -eq $keytoolPath) {
    Write-Host "ERROR: keytool not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install JDK or set JAVA_HOME environment variable." -ForegroundColor Yellow
    Write-Host "Common locations:" -ForegroundColor Yellow
    Write-Host "  - C:\Program Files\Java\jdk-17\bin\keytool.exe" -ForegroundColor Gray
    Write-Host "  - C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Alternative: Run this command in CMD:" -ForegroundColor Cyan
    Write-Host 'keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android' -ForegroundColor White
    exit 1
}

Write-Host "Found keytool at: $keytoolPath" -ForegroundColor Green
Write-Host ""

$keystorePath = "$env:USERPROFILE\.android\debug.keystore"

if (-not (Test-Path $keystorePath)) {
    Write-Host "ERROR: Debug keystore not found at: $keystorePath" -ForegroundColor Red
    Write-Host ""
    Write-Host "This usually means you haven't built the project yet." -ForegroundColor Yellow
    Write-Host "Try building the project first, then run this script again." -ForegroundColor Yellow
    exit 1
}

Write-Host "Keystore found at: $keystorePath" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  EXTRACTING SHA-1 FINGERPRINT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

try {
    $output = & $keytoolPath -list -v -keystore $keystorePath -alias androiddebugkey -storepass android -keypass android 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Successfully extracted SHA-1!" -ForegroundColor Green
        Write-Host ""
        
        # Extract SHA1 line
        $sha1Line = $output | Select-String "SHA1:"
        if ($sha1Line) {
            $sha1 = $sha1Line.Line.Split(":")[1].Trim()
            
            Write-Host "========================================" -ForegroundColor Cyan
            Write-Host "  YOUR SHA-1 FINGERPRINT:" -ForegroundColor Cyan
            Write-Host "========================================" -ForegroundColor Cyan
            Write-Host ""
            Write-Host $sha1 -ForegroundColor White -BackgroundColor DarkGray -FontSize 14
            Write-Host ""
            Write-Host "========================================" -ForegroundColor Cyan
            Write-Host "  NEXT STEPS:" -ForegroundColor Cyan
            Write-Host "========================================" -ForegroundColor Cyan
            Write-Host ""
            Write-Host "1. COPY the SHA-1 above (Ctrl+C)" -ForegroundColor White
            Write-Host "2. Go to Firebase Console:" -ForegroundColor White
            Write-Host "   https://console.firebase.google.com/" -ForegroundColor Cyan
            Write-Host "3. Select project: kitchen-brain-58a1e" -ForegroundColor White
            Write-Host "4. Click Settings ⚙️ → Project settings" -ForegroundColor White
            Write-Host "5. Scroll to 'Your apps' section" -ForegroundColor White
            Write-Host "6. Click 'Add fingerprint'" -ForegroundColor White
            Write-Host "7. Paste SHA-1 and Save" -ForegroundColor White
            Write-Host "8. Download fresh google-services.json" -ForegroundColor White
            Write-Host "9. Replace app/google-services.json" -ForegroundColor White
            Write-Host "10. Build → Clean Project → Rebuild Project" -ForegroundColor White
            Write-Host ""
            Write-Host "Press any key to continue..." -ForegroundColor Gray
            $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        } else {
            Write-Host "ERROR: Could not extract SHA-1 from output" -ForegroundColor Red
            Write-Host ""
            Write-Host "Full output:" -ForegroundColor Yellow
            Write-Host $output -ForegroundColor Gray
        }
    } else {
        Write-Host "ERROR: keytool failed with exit code $LASTEXITCODE" -ForegroundColor Red
        Write-Host ""
        Write-Host "Output:" -ForegroundColor Yellow
        Write-Host $output -ForegroundColor Gray
    }
} catch {
    Write-Host "ERROR: Exception occurred" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
