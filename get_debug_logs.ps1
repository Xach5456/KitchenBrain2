# Filter logs for ANR debugging
Write-Host "=== CHAT FRAGMENT LOGS ===" -ForegroundColor Cyan
adb logcat -d | Select-String -Pattern "CHAT OPEN|STEP 0|STEP 1|STEP 2|HARDKOR|INIT 1|INIT 2|INIT 3|RV 1|RV 2|RV 3|ADAPTER_DEBUG" | Select-Object -Last 50

Write-Host "`n=== FOLLOW GRAPH LOGS ===" -ForegroundColor Cyan
adb logcat -d | Select-String -Pattern "FollowGraphRepository|GET_MUTUAL|GRAPH_INIT|GRAPH_TEST|GRAPH_DEBUG" | Select-Object -Last 50

Write-Host "`n=== FRIENDS LOGS ===" -ForegroundColor Cyan
adb logcat -d | Select-String -Pattern "FriendListFragment|FriendsFragment|SharedViewModel.*Loaded|Mutual" | Select-Object -Last 30

Write-Host "`n=== ANY ERRORS ===" -ForegroundColor Red
adb logcat -d | Select-String -Pattern "ANR|DEVELOPER_ERROR|Exception|Error|FATAL" | Select-Object -Last 30
