# ========================================
# KitchenBrain - API Key & Configuration Diagnostic
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "KitchenBrain Configuration Diagnostic" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check local.properties exists
$localPropsPath = "c:\Users\Admin\AndroidStudioProjects\KitchenBrain2\local.properties"

if (Test-Path $localPropsPath) {
    Write-Host "[✓] local.properties exists" -ForegroundColor Green
    Write-Host "    Location: $localPropsPath" -ForegroundColor Gray
    Write-Host ""
    
    # Read file content
    $content = Get-Content $localPropsPath
    
    # Check YouTube API Key
    $youtubeKeyLine = $content | Where-Object { $_ -match "^YOUTUBE_API_KEY=" }
    
    if ($youtubeKeyLine) {
        $youtubeKey = $youtubeKeyLine.Split('=')[1].Trim()
        
        if ([string]::IsNullOrWhiteSpace($youtubeKey) -or $youtubeKey -eq "your_youtube_api_key_here") {
            Write-Host "[✗] YouTube API Key is NOT configured" -ForegroundColor Red
            Write-Host "    Current: Empty or placeholder" -ForegroundColor Gray
            Write-Host ""
            Write-Host "    HOW TO FIX:" -ForegroundColor Yellow
            Write-Host "    1. Go to https://console.cloud.google.com/" -ForegroundColor Gray
            Write-Host "    2. Enable YouTube Data API v3" -ForegroundColor Gray
            Write-Host "    3. Create API Key" -ForegroundColor Gray
            Write-Host "    4. Edit local.properties and add:" -ForegroundColor Gray
            Write-Host "       YOUTUBE_API_KEY=AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" -ForegroundColor Gray
        } else {
            $keyPreview = $youtubeKey.Substring(0, [Math]::Min(10, $youtubeKey.Length)) + "..."
            Write-Host "[✓] YouTube API Key is configured" -ForegroundColor Green
            Write-Host "    Preview: $keyPreview" -ForegroundColor Gray
            Write-Host "    Length: $($youtubeKey.Length) characters" -ForegroundColor Gray
            
            if ($youtubeKey.Length -lt 30) {
                Write-Host ""
                Write-Host "[!] Warning: API key seems too short" -ForegroundColor Yellow
                Write-Host "    Expected: ~39 characters" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "[✗] YouTube API Key line not found" -ForegroundColor Red
        Write-Host "    Add this line to local.properties:" -ForegroundColor Gray
        Write-Host "    YOUTUBE_API_KEY=your_key_here" -ForegroundColor Gray
    }
    
    Write-Host ""
    
    # Check other API keys
    Write-Host "Other API Keys Status:" -ForegroundColor Cyan
    $keys = @("NEWS_API_KEY", "GROK_API_KEY", "GEMINI_API_KEY", "OPENAI_API_KEY", "DEEPSEEK_API_KEY", "GROQ_API_KEY")
    
    foreach ($keyName in $keys) {
        $keyLine = $content | Where-Object { $_ -match "^$keyName=" }
        if ($keyLine) {
            $keyValue = $keyLine.Split('=')[1].Trim()
            if ([string]::IsNullOrWhiteSpace($keyValue) -or $keyValue -match "^your_.*_here$") {
                Write-Host "  [✗] $keyName - Not configured" -ForegroundColor Red
            } else {
                Write-Host "  [✓] $keyName - Configured" -ForegroundColor Green
            }
        } else {
            Write-Host "  [-] $keyName - Not found" -ForegroundColor Yellow
        }
    }
    
} else {
    Write-Host "[✗] local.properties NOT FOUND" -ForegroundColor Red
    Write-Host "    Expected location: $localPropsPath" -ForegroundColor Gray
    Write-Host ""
    Write-Host "    HOW TO FIX:" -ForegroundColor Yellow
    Write-Host "    1. Copy local.properties.example to local.properties" -ForegroundColor Gray
    Write-Host "    2. Add your API keys" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Gradle Configuration Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check build.gradle.kts YouTube API config
$buildGradlePath = "c:\Users\Admin\AndroidStudioProjects\KitchenBrain2\app\build.gradle.kts"

if (Test-Path $buildGradlePath) {
    $gradleContent = Get-Content $buildGradlePath
    $hasYouTubeConfig = $gradleContent | Where-Object { $_ -match "YOUTUBE_API_KEY" }
    
    if ($hasYouTubeConfig) {
        Write-Host "[✓] build.gradle.kts has YouTube API configuration" -ForegroundColor Green
        
        # Check if BuildConfig field is defined
        $hasBuildConfig = $gradleContent | Where-Object { $_ -match "buildConfigField.*YOUTUBE_API_KEY" }
        if ($hasBuildConfig) {
            Write-Host "[✓] BuildConfig.YOUTUBE_API_KEY field defined" -ForegroundColor Green
        } else {
            Write-Host "[✗] BuildConfig field not found" -ForegroundColor Red
        }
    } else {
        Write-Host "[✗] build.gradle.kts missing YouTube API configuration" -ForegroundColor Red
    }
} else {
    Write-Host "[✗] build.gradle.kts not found" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary and Recommendations" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Final recommendations
$youtubeKeyLine = $null
if (Test-Path $localPropsPath) {
    $content = Get-Content $localPropsPath
    $youtubeKeyLine = $content | Where-Object { $_ -match "^YOUTUBE_API_KEY=" }
}

if ($youtubeKeyLine -and $youtubeKeyLine.Split('=')[1].Trim().Length -gt 30) {
    Write-Host "✓ YouTube API Key is configured correctly" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "  1. Sync Gradle in Android Studio" -ForegroundColor Gray
    Write-Host "  2. Build app: .\gradlew assembleDebug" -ForegroundColor Gray
    Write-Host "  3. Check build log for: 'YOUTUBE_API_KEY configured.'" -ForegroundColor Gray
    Write-Host "  4. Install and test app" -ForegroundColor Gray
} else {
    Write-Host "✗ YouTube API Key needs configuration" -ForegroundColor Red
    Write-Host ""
    Write-Host "Required Steps:" -ForegroundColor Yellow
    Write-Host "  1. Get API Key from Google Cloud Console" -ForegroundColor Gray
    Write-Host "     https://console.cloud.google.com/" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  2. Add to local.properties:" -ForegroundColor Gray
    Write-Host "     YOUTUBE_API_KEY=AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  3. Sync Gradle" -ForegroundColor Gray
    Write-Host "  4. Rebuild app" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Diagnostic Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
