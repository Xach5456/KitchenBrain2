#!/bin/bash

# CRITICAL: Diagnostic Script for SIGKILL / Force-Stop Issue
# This script will help identify what's killing your app

APP_PACKAGE="com.example.kitchenbrain"
LOG_FILE="sigkill_diagnostics_$(date +%Y%m%d_%H%M%S).txt"

echo "========================================" | tee -a $LOG_FILE
echo "SIGKILL/Force-Stop Diagnostic Report" | tee -a $LOG_FILE
echo "Generated: $(date)" | tee -a $LOG_FILE
echo "========================================" | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 1. Check for running automation tools
echo "1. CHECKING FOR AUTOMATION TOOLS..." | tee -a $LOG_FILE
ps aux | grep -E "gradle|appium|uiautomator|python.*adb|node.*test" | grep -v grep | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 2. Check ADB connections
echo "2. ADB CONNECTIONS:" | tee -a $LOG_FILE
adb devices | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 3. Check for Gradle test runners
echo "3. GRADLE DAEMONS:" | tee -a $LOG_FILE
jps -l | grep -i gradle | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 4. Check Android Studio processes
echo "4. ANDROID STUDIO PROCESSES:" | tee -a $LOG_FILE
ps aux | grep -i "android-studio\|studio" | grep -v grep | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 5. Look for shell scripts with adb commands
echo "5. RUNNING SHELL SCRIPTS WITH ADB:" | tee -a $LOG_FILE
ps aux | grep -E "bash.*adb|sh.*adb" | grep -v grep | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 6. Check logcat for force-stop evidence
echo "6. RECENT FORCE-STOP EVENTS (last 100 lines):" | tee -a $LOG_FILE
adb logcat -d | grep -i "force-stop\|am_kill\|killing.*$APP_PACKAGE" | tail -100 | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 7. Check package manager state
echo "7. PACKAGE MANAGER STATE:" | tee -a $LOG_FILE
adb shell dumpsys package $APP_PACKAGE | grep -E "userId|versionCode|forceStop" | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 8. Check activity manager for recent launches
echo "8. RECENT ACTIVITY LAUNCHES:" | tee -a $LOG_FILE
adb shell dumpsys activity activities | grep -A 3 "Run #0" | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 9. Look for cron jobs or scheduled tasks
echo "9. CRON JOBS / SCHEDULED TASKS:" | tee -a $LOG_FILE
crontab -l 2>/dev/null | grep -v "^#" | tee -a $LOG_FILE
launchctl list 2>/dev/null | grep -i adb | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 10. Check for IDE plugins
echo "10. ANDROID STUDIO PLUGINS (ADB-related):" | tee -a $LOG_FILE
find ~/.AndroidStudio* -name "*.jar" 2>/dev/null | xargs -I {} basename {} .jar | grep -i adb | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 11. Network connections (looking for test frameworks)
echo "11. NETWORK CONNECTIONS (port 5037 = ADB):" | tee -a $LOG_FILE
netstat -tulpn 2>/dev/null | grep 5037 | tee -a $LOG_FILE
lsof -i :5037 2>/dev/null | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

# 12. Recent file modifications in project
echo "12. RECENTLY MODIFIED FILES (last 24h):" | tee -a $LOG_FILE
find . -name "*.sh" -o -name "*.bat" -o -name "*.gradle" -mtime -1 2>/dev/null | head -20 | tee -a $LOG_FILE
echo "" | tee -a $LOG_FILE

echo "========================================" | tee -a $LOG_FILE
echo "DIAGNOSTIC COMPLETE" | tee -a $LOG_FILE
echo "Log saved to: $LOG_FILE" | tee -a $LOG_FILE
echo "========================================" | tee -a $LOG_FILE

# Next steps instructions
echo ""
echo "NEXT STEPS:"
echo "1. Review the output above for suspicious processes"
echo "2. Kill any automation: pkill -f 'gradle.*test'"
echo "3. Disable ADB plugins in Android Studio"
echo "4. Check run configurations for force-stop commands"
echo "5. Run: adb logcat -b events | grep -i 'am_kill'"
