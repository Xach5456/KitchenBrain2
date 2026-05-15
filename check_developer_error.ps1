# Quick Developer Error Diagnostic Script for KitchenBrain2
# Run this in PowerShell to check SHA-1 and apply logcat filters

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "KitchenBrain2 Developer Error Checker" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Extract Debug SHA-1
Write-Host "Step 1: Extracting Debug SHA-1..." -ForegroundColor Yellow
$debugKeystorePath = "$env:USERPROFILE\.android\debug.keystore"

if (Test-Path $debugKeystorePath) {
    Write-Host "Debug keystore found at: $debugKeystorePath" -ForegroundColor Green
    
    try {
        $sha1Output = keytool -list -v -keystore $debugKeystorePath -alias androiddebugkey -storepass android -keypass android 2>&1
        $sha1Line = $sha1Output | Select-String "SHA1:"
        
        if ($sha1Line) {
            Write-Host ""
            Write-Host "Your Debug SHA-1:" -ForegroundColor Cyan
            Write-Host $sha1Line.Line -ForegroundColor White
            Write-Host ""
            
            # Copy to clipboard
            $sha1Line.Line | Set-Clipboard
            Write-Host "✓ SHA-1 copied to clipboard!" -ForegroundColor Green
            Write-Host ""
            Write-Host "ACTION: Go to Firebase Console → Project Settings → Your apps" -ForegroundColor Yellow
            Write-Host "        Verify this SHA-1 is added to BOTH debug and release configs" -ForegroundColor Yellow
        } else {
            Write-Host "⚠ Could not extract SHA-1 from keystore output" -ForegroundColor Red
        }
    } catch {
        Write-Host "⚠ Error running keytool. Make sure Java JDK is installed and in PATH" -ForegroundColor Red
    }
} else {
    Write-Host "⚠ Debug keystore not found at expected location" -ForegroundColor Red
    Write-Host "Creating new keystore may be necessary" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 2: Current Emulator Configuration" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Get running emulators
$emulators = adb devices | Select-String "emulator-"

if ($emulators) {
    Write-Host "Running emulators/devices:" -ForegroundColor Green
    $emulators | ForEach-Object { Write-Host $_.Line -ForegroundColor White }
    
    # Try to get emulator info
    $firstEmulator = $emulators[0].Line -split '\s+' | Select-Object -First 1
    
    if ($firstEmulator) {
        Write-Host ""
        Write-Host "Checking emulator properties..." -ForegroundColor Yellow
        
        # Get Android version
        $androidVersion = adb -s $firstEmulator shell getprop ro.build.version.release
        Write-Host "Android Version: $androidVersion" -ForegroundColor Cyan
        
        # Get SDK level
        $sdkLevel = adb -s $firstEmulator shell getprop ro.build.version.sdk
        Write-Host "SDK Level: $sdkLevel" -ForegroundColor Cyan
        
        # Check for Google Play
        $gmsVersion = adb -s $firstEmulator shell pm list packages | Select-String "com.google.android.gms"
        if ($gmsVersion) {
            Write-Host "Google Play Services: Installed ✓" -ForegroundColor Green
        } else {
            Write-Host "Google Play Services: Not detected" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "No emulators or devices currently running" -ForegroundColor Yellow
    Write-Host "Start an emulator to check configuration" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 3: Apply Logcat Filter" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host ""
Write-Host "To suppress DEVELOPER_ERROR spam, use this logcat filter:" -ForegroundColor Yellow
Write-Host ""
Write-Host "-logtag:GoogleApiManager -logtag:FlagRegistrar -logtag:Phenotype -logtag:ProviderInstaller" -ForegroundColor White
Write-Host ""
Write-Host "Or run this command to see filtered logs in real-time:" -ForegroundColor Cyan
Write-Host "adb logcat | Select-String -Pattern 'GoogleApiManager|FlagRegistrar|Phenotype|ProviderInstaller' -Exclude" -ForegroundColor Gray
Write-Host ""

# Create a shortcut script for filtered logcat
$filterScriptPath = Join-Path $PSScriptRoot "filtered_logcat.ps1"
@"
# Filtered Logcat - Suppresses Google Play Services emulator noise
Write-Host "Starting filtered logcat (suppressing GMS emulator errors)..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop`n" -ForegroundColor Gray

adb logcat | ForEach-Object {
    \$line = \$_
    if (\$line -notmatch "GoogleApiManager|FlagRegistrar|Phenotype|ProviderInstaller|DEVELOPER_ERROR") {
        Write-Output \$line
    }
}
"@ | Out-File -FilePath $filterScriptPath -Encoding UTF8

Write-Host "✓ Created filtered_logcat.ps1 at: $filterScriptPath" -ForegroundColor Green
Write-Host "  Run this script anytime to see clean logs without GMS spam" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 4: Quick Functionality Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host ""
Write-Host "Testing basic app functionality..." -ForegroundColor Yellow

if ($firstEmulator) {
    # Check if app is installed
    $appInstalled = adb -s $firstEmulator shell pm list packages | Select-String "com.example.kitchenbrain"
    
    if ($appInstalled) {
        Write-Host "✓ KitchenBrain app is installed" -ForegroundColor Green
        
        # Check if app is running
        $appRunning = adb -s $firstEmulator shell dumpsys window windows | Select-String "com.example.kitchenbrain"
        
        if ($appRunning) {
            Write-Host "✓ KitchenBrain app is currently running" -ForegroundColor Green
            
            # Try to capture recent logs
            Write-Host ""
            Write-Host "Recent error logs (last 50 lines):" -ForegroundColor Yellow
            
            $recentLogs = adb -s $firstEmulator logcat -d -v time | Select-String -Pattern "E/|W/" | Select-Object -Last 50
            
            if ($recentLogs) {
                $recentLogs | ForEach-Object { Write-Host $_.Line -ForegroundColor Gray }
                
                # Count DEVELOPER_ERROR occurrences
                $devErrorCount = $recentLogs | Select-String "DEVELOPER_ERROR"
                
                if ($devErrorCount) {
                    Write-Host ""
                    Write-Host "⚠ Found $($devErrorCount.Count) DEVELOPER_ERROR messages" -ForegroundColor Yellow
                    Write-Host "This is normal on emulators if app functions correctly" -ForegroundColor Green
                } else {
                    Write-Host "✓ No DEVELOPER_ERROR messages in recent logs" -ForegroundColor Green
                }
            }
        } else {
            Write-Host "App is installed but not currently running" -ForegroundColor Gray
            Write-Host "Launch the app to test functionality" -ForegroundColor Cyan
        }
    } else {
        Write-Host "⚠ App not installed on this emulator" -ForegroundColor Yellow
        Write-Host "Install with: .\gradlew installDebug" -ForegroundColor Cyan
    }
} else {
    Write-Host "Start an emulator first, then re-run this script" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary & Next Steps" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "If your app works fine despite the error logs:" -ForegroundColor Green
Write-Host "  ✓ This is harmless emulator noise" -ForegroundColor Green
Write-Host "  ✓ Use the filtered_logcat.ps1 script for cleaner logs" -ForegroundColor Green
Write-Host "  ✓ No action needed - continue development" -ForegroundColor Green
Write-Host ""
Write-Host "If you see errors on a PHYSICAL device:" -ForegroundColor Red
Write-Host "  1. Copy your SHA-1 above" -ForegroundColor Yellow
Write-Host "  2. Add it to Firebase Console" -ForegroundColor Yellow
Write-Host "  3. Download new google-services.json" -ForegroundColor Yellow
Write-Host "  4. Rebuild and reinstall" -ForegroundColor Yellow
Write-Host ""
Write-Host "For complete troubleshooting steps, see:" -ForegroundColor Cyan
Write-Host "COMPLETE_DEVELOPER_ERROR_GUIDE_2026.md" -ForegroundColor White
Write-Host ""
