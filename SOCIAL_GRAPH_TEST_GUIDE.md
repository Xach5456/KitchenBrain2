# 🔥 SOCIAL GRAPH SYSTEM - REAL EXECUTION TEST GUIDE

## ⚠️ IMPORTANT: I CANNOT RUN THESE TESTS FOR YOU

As an AI, I cannot:
- ❌ Execute your Android app
- ❌ Access your Firebase Firestore
- ❌ Click UI buttons
- ❌ Observe real-time behavior

**YOU MUST RUN THESE TESTS YOURSELF.**

---

## 📋 PREPARATION

### Step 1: Setup Test Accounts
1. **Create User A** (your main account)
   - Login to your app
   - Note the User ID from Firestore: `users/{USER_A_ID}`

2. **Create User B** (test account)
   - Use another device or logout/login with different credentials
   - Note the User ID: `users/{USER_B_ID}`

### Step 2: Configure Test Fragment
1. Open: `SocialGraphValidationTest.java`
2. Line 33: Replace with real User B ID:
   ```java
   private static final String TEST_USER_B_ID = "abc123xyz"; // ← PUT REAL ID HERE
   ```

### Step 3: Navigate to Test Fragment
Add this to your MainActivity navigation:
```java
// Temporary test button
findViewById(R.id.btnTest).setOnClickListener(v -> {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_container, new SocialGraphValidationTest())
        .addToBackStack(null)
        .commit();
});
```

---

## 🧪 MANUAL TEST SUITE

### **TEST 1: Follow User**

**Steps:**
1. Login as User A
2. Search for User B
3. Click "Follow" button

**Verify:**
- [ ] Button changes to "Following" instantly (< 1 second)
- [ ] Firestore Console shows:
  - `following/{USER_A_ID}/userFollowing/{USER_B_ID}` ✅
  - `followers/{USER_B_ID}/userFollowers/{USER_A_ID}` ✅
- [ ] Check Logcat for:
  ```
  ✅ [FOLLOW_DEBUG] FOLLOW SUCCESS - Transaction committed
  ```
- [ ] No crashes

**Expected Firestore Structure:**
```
following/
  └── {USER_A_ID}/
      └── userFollowing/
          └── {USER_B_ID}  ← Should exist with followedAt timestamp

followers/
  └── {USER_B_ID}/
      └── userFollowers/
          └── {USER_A_ID}  ← Should exist with followedAt timestamp
```

**Result:** ✅ PASS or ❌ FAIL

---

### **TEST 2: Unfollow User**

**Steps:**
1. Still logged in as User A
2. Find User B (should show "Following")
3. Click "Following" button to unfollow

**Verify:**
- [ ] Button changes back to "Follow"
- [ ] Firestore documents DELETED:
  - `following/{USER_A_ID}/userFollowing/{USER_B_ID}` ❌ (gone)
  - `followers/{USER_B_ID}/userFollowers/{USER_A_ID}` ❌ (gone)
- [ ] Check Logcat:
  ```
  ✅ [FOLLOW_DEBUG] UNFOLLOW SUCCESS - Transaction committed
  ```
- [ ] Counters decremented in user document

**Result:** ✅ PASS or ❌ FAIL

---

### **TEST 3: Mutual Follow**

**Steps:**
1. User A follows User B
2. **Switch to User B account** (different device or logout/login)
3. User B follows User A back
4. Switch back to User A account
5. Check User B in search results

**Verify:**
- [ ] Button shows "✓ Mutual" (blue filled button)
- [ ] "Message" button appears below Follow button
- [ ] Check Logcat on BOTH devices:
  ```
  ✅ Mutual follow detected: USER_A <-> USER_B
  ```
- [ ] Firestore shows both directions:
  ```
  following/USER_A/userFollowing/USER_B ✅
  following/USER_B/userFollowing/USER_A ✅
  ```

**Result:** ✅ PASS or ❌ FAIL

---

### **TEST 4: Chat Gate (CRITICAL SECURITY TEST)**

#### **Test 4A: Block Chat Without Mutual**

**Steps:**
1. User A follows User B (but B hasn't followed back)
2. Try to open chat with User B programmatically:
   ```java
   // In any fragment/activity
   Bundle args = new Bundle();
   args.putString("other_user_id", USER_B_ID);
   
   ChatFragment chat = new ChatFragment();
   chat.setArguments(args);
   
   getSupportFragmentManager()
       .beginTransaction()
       .replace(R.id.fragment_container, chat)
       .commit();
   ```

**Expected:**
- [ ] Dialog appears: "Chat Not Available"
- [ ] Message: "You can only chat with mutual friends..."
- [ ] Clicking "Go Back" navigates away
- [ ] Chat does NOT open

**Result:** ✅ PASS (blocked) or ❌ FAIL (security bug!)

#### **Test 4B: Allow Chat With Mutual**

**Steps:**
1. Ensure User A and User B are mutual (both following each other)
2. Click "Message" button on User B's search result
3. OR try to open ChatFragment programmatically

**Expected:**
- [ ] Chat opens successfully
- [ ] Can send messages
- [ ] No error dialog

**Result:** ✅ PASS or ❌ FAIL

---

### **TEST 5: Real-Time UI Update**

**Steps:**
1. User A has search screen open showing User B
2. User B currently does NOT follow User A (shows "Follow")
3. **On another device**, User B follows User A
4. Watch User A's screen (DO NOT refresh)

**Expected:**
- [ ] Within 1-2 seconds, button changes to "Follow Back" or "Mutual"
- [ ] No manual refresh needed
- [ ] If mutual, "Message" button appears automatically

**If button doesn't update:**
- You need to implement `getFollowStateRealtime()` listener in your adapter
- See `FollowRepository.getFollowStateRealtime()` method

**Result:** ✅ PASS (instant update) or ❌ FAIL (needs refresh)

---

### **TEST 6: Spam Click Prevention**

**Steps:**
1. Find User B (not following)
2. Click "Follow" button 10 times as fast as possible

**Expected:**
- [ ] Only 1 Firestore write happens (check Firestore Console)
- [ ] Button shows loading state during first click
- [ ] No crashes
- [ ] UI remains stable
- [ ] No duplicate documents created

**Verify in Firestore:**
```
following/USER_A/userFollowing/USER_B  ← Should be ONLY 1 document
```

**Check Logcat for:**
```
⚠️ Follow button click ignored - cooldown active
```

**Result:** ✅ PASS or ❌ FAIL

---

### **TEST 7: Counter Consistency**

**Steps:**
1. Follow 3 different users
2. Check your user document in Firestore:
   ```
   users/{YOUR_USER_ID}
   ```

**Expected:**
- [ ] `followingCount: 3` (matches actual count)
- [ ] Go to `following/{YOUR_ID}/userFollowing/` and count documents
- [ ] Count should match `followingCount` exactly

**Test Counter Auto-Fix:**
```java
// In any fragment
CounterConsistencyChecker checker = new CounterConsistencyChecker();
checker.checkAndFixCounters(userId, new ConsistencyCallback() {
    @Override
    public void onSuccess(boolean wasFixed, Map<String, Object> updates) {
        if (wasFixed) {
            Log.d("COUNTER", "✅ Counters fixed: " + updates);
        }
    }
});
```

**Result:** ✅ PASS or ❌ FAIL

---

### **TEST 8: Unfollow During Active Chat (EDGE CASE)**

**Steps:**
1. User A and User B are mutual
2. User A opens chat with User B
3. **On User B's device**, unfollow User A
4. Watch User A's chat screen

**Expected:**
- [ ] Within 2 seconds, dialog appears: "Chat No Longer Available"
- [ ] Message: "Mutual follow relationship was broken"
- [ ] Chat closes when clicking "Close Chat"
- [ ] Cannot send more messages

**Result:** ✅ PASS or ❌ FAIL

---

## 📊 AUTOMATED TEST

### Using SocialGraphValidationTest Fragment

1. Edit `TEST_USER_B_ID` in the test file
2. Run app and navigate to test fragment
3. Click "▶️ RUN ALL TESTS"
4. Watch results appear in real-time

**Expected Output:**
```
🔥 Starting Social Graph Validation Tests...
Current User: abc123...
Test User B: xyz789...
─────────────────────────────

📝 TEST 1: Follow User
✅ PASS: Follow succeeded
  ✅ following/abc123.../userFollowing/xyz789... exists
  ✅ followers/xyz789.../userFollowers/abc123... exists
✅ TEST 1 PASSED: Both documents created

📝 TEST 2: Unfollow User
✅ PASS: Unfollow succeeded
  ✅ following document deleted
  ✅ followers document deleted
✅ TEST 2 PASSED: Both documents deleted

...

══════════════════════════════
📊 FINAL RESULTS
══════════════════════════════
Tests Total: 5
✅ Passed: 5
❌ Failed: 0

🎉 ALL TESTS PASSED - SYSTEM WORKING 100%
```

---

## 🎯 FINAL VERDICT TEMPLATE

After running ALL tests, fill this out:

```
==================================================
SOCIAL GRAPH SYSTEM - TEST RESULTS
==================================================

TEST 1 (Follow):          [ ] PASS  [ ] FAIL
TEST 2 (Unfollow):        [ ] PASS  [ ] FAIL
TEST 3 (Mutual):          [ ] PASS  [ ] FAIL
TEST 4A (Chat Gate):      [ ] PASS  [ ] FAIL
TEST 4B (Chat Allowed):   [ ] PASS  [ ] FAIL
TEST 5 (Realtime):        [ ] PASS  [ ] FAIL
TEST 6 (Spam):            [ ] PASS  [ ] FAIL
TEST 7 (Counters):        [ ] PASS  [ ] FAIL
TEST 8 (Edge Case):       [ ] PASS  [ ] FAIL

==================================================
OVERALL RESULT:
==================================================

[ ] WORKING 100% - All tests passed
[ ] BROKEN - X tests failed

Failed Tests:
- Test #: Description of failure
- Test #: Description of failure

==================================================
```

---

## 🐛 IF TESTS FAIL

### Common Issues:

1. **"Follow fails with Already Following"**
   - Clean Firestore: delete old follow documents
   - Or run unfollow first

2. **"Chat opens without mutual"**
   - CRITICAL SECURITY BUG
   - Check `ChatFragment.checkMutualAndProceed()` exists
   - Verify `isMutual()` method works

3. **"UI doesn't update in real-time"**
   - You're not using `addSnapshotListener()`
   - Implement `getFollowStateRealtime()` in your adapter

4. **"Counters are wrong"**
   - Run `CounterConsistencyChecker.checkAndFixCounters()`
   - Check if old data exists before transactions were implemented

---

## 📞 NEED HELP?

If tests fail, provide:
1. Which test(s) failed
2. Logcat output (filter: `SocialGraphTest` or `FollowRepository`)
3. Screenshot of Firestore documents
4. What you expected vs what happened

I can then help debug the specific issue! 🚀
