# Filtered Logcat - Suppresses Google Play Services emulator noise
# This script filters out DEVELOPER_ERROR spam while showing real issues

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Filtered Logcat (GMS Noise Suppressed)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Starting filtered logcat..." -ForegroundColor Green
Write-Host "Suppressing: GoogleApiManager, FlagRegistrar, Phenotype, ProviderInstaller" -ForegroundColor Gray
Write-Host "Press Ctrl+C to stop`n" -ForegroundColor Yellow

# Patterns to exclude (emulator noise)
$excludePatterns = @(
    "GoogleApiManager",
    "FlagRegistrar", 
    "Phenotype",
    "ProviderInstaller",
    "DEVELOPER_ERROR",
    "geeq:"
)

# Start logcat and filter
adb logcat -v time | ForEach-Object {
    $line = $_
    $isNoise = $false
    
    # Check if line contains any of the noise patterns
    foreach ($pattern in $excludePatterns) {
        if ($line -match $pattern) {
            $isNoise = $true
            break
        }
    }
    
    # Only output if it's not noise
    if (-not $isNoise) {
        Write-Output $line
    }
}
