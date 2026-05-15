# Filtered Logcat Script for KitchenBrain2
# This script removes emulator-specific Google Play Services noise
# while preserving real errors and ANR issues

Write-Host "=== KitchenBrain2 Filtered Logcat ===" -ForegroundColor Green
Write-Host "Filtering out emulator GMS artifacts..." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop`n" -ForegroundColor Cyan

adb logcat -v threadtime | Select-String -Pattern `
    "com.example.kitchenbrain",
    "AI_API_DEBUG",
    "ANR",
    "FATAL",
    "CRASH" | Select-String -Pattern `
    "artd",
    "Could not get dex checksums",
    "vdex next to the dex file",
    "GetBestInfo.*has no usable artifacts",
    "Failed to get service from broker.*DEVELOPER_ERROR",
    "Unknown calling package name 'com.google.android.gms'",
    "Phenotype.API.*not available",
    "Not showing notification since connectionResult is not user-facing" -NotMatch
