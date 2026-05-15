# FriendMapper Architecture

## 🔴 Problem: userId vs friendId Confusion

**Common Bug Pattern:**
```java
// ❌ WRONG - breaks everything
friend.setUserId(user.getUserId());      // This is the FRIEND, not YOU
friend.setFriendId(user.getUserId());    // Duplicate!
```

**Consequences:**
- ❌ Wrong chat opens
- ❌ Follow buttons break
- ❌ Friend deletion doesn't work
- ❌ Graph state corruption

---

## ✅ Solution: FriendMapper Class

### Architecture Rule

```
Friend = (Current User → Friend)
         ↓              ↓
      userId         friendId
```

**Fields:**
- `userId` = The current logged-in user (owner of friends list)
- `friendId` = The friend's user ID (from the list)

---

## 📦 Implementation

### FriendMapper.java

```java
public class FriendMapper {
    
    /**
     * Convert User to Friend object
     * 
     * @param user The user to convert (will become the friend)
     * @param currentUserId The current logged-in user (owner)
     * @return Friend object with correct mapping
     */
    public static Friend fromUser(User user, String currentUserId) {
        if (user == null || currentUserId == null) {
            return null;
        }
        
        Friend friend = new Friend();
        
        // ✅ CRITICAL: userId = YOU, friendId = THE FRIEND
        friend.setUserId(currentUserId);          // YOU
        friend.setFriendId(user.getUserId());     // THE FRIEND
        
        // Map user data to friend fields
        friend.setFriendUsername(user.getUsername());
        friend.setFriendAvatarUrl(user.getAvatarUrl());
        
        // Default values
        friend.setOnline(false);
        
        return friend;
    }
}
```

### Usage

```java
// In FriendsFragment
for (User user : mutualUsers) {
    Friend friend = FriendMapper.fromUser(user, currentUserId, true);
    if (friend != null) {
        mutualFriends.add(friend);
    }
}
```

---

## 🧠 Why This Matters

### Before (Inline Mapping - Error Prone):

```java
// Scattered across multiple files
Friend friend = new Friend();
friend.setUserId(...);        // Easy to mess up
friend.setFriendId(...);      // Which one is which?
friend.setFriendUsername(...);
friend.setFriendAvatarUrl(...); // Wait, is it setAvatarUrl or setFriendAvatarUrl?
```

**Problems:**
- ❌ Logic duplicated in multiple places
- ❌ Easy to mix up userId/friendId
- ❌ Hard to maintain
- ❌ No null safety

### After (Centralized Mapper - Safe):

```java
// Single source of truth
Friend friend = FriendMapper.fromUser(user, currentUserId);
```

**Benefits:**
- ✅ One place to maintain
- ✅ Impossible to mix up userId/friendId
- ✅ Null safety built-in
- ✅ Clear intent
- ✅ Reusable across app

---

## 📊 Field Mapping Reference

| User Field | Friend Field | Notes |
|------------|--------------|-------|
| `user.userId` | `friend.friendId` | The friend's ID |
| `user.username` | `friend.friendUsername` | Display name |
| `user.avatarUrl` | `friend.friendAvatarUrl` | Profile picture |
| N/A | `friend.userId` | Current user (owner) |
| N/A | `friend.isOnline` | Set explicitly |

---

## 🔍 Verification

### Test Cases:

**1. Current User Check:**
```java
Friend friend = FriendMapper.fromUser(user, "currentUser123");
assert friend.getUserId().equals("currentUser123");  // ✅ Owner
assert friend.getFriendId().equals(user.getUserId()); // ✅ Friend
```

**2. Null Safety:**
```java
Friend nullFriend = FriendMapper.fromUser(null, "currentUser123");
assert nullFriend == null;  // ✅ Safe
```

**3. Chat Opening:**
```java
// Should open chat with friend, not current user
openChat(friend.getFriendId());  // ✅ Correct
```

---

## 🎯 Key Takeaways

1. **Friend = (Me → Friend)**
   - `userId` = Me (current user)
   - `friendId` = Friend (from list)

2. **Always use FriendMapper**
   - Don't create Friend objects inline
   - Centralized logic prevents bugs

3. **Null safety**
   - Mapper returns null for invalid inputs
   - Check before adding to list

4. **Single source of truth**
   - One place to update if model changes
   - Consistent across entire app

---

## ✅ Checklist

- [x] FriendMapper class created
- [x] userId/friendId mapping documented
- [x] Null safety implemented
- [x] FriendsFragment uses FriendMapper
- [x] Old inline mapper removed
- [x] Architecture documented

---

## 🚀 Next Steps

Potential improvements:
1. Add Unit Tests for FriendMapper
2. Add reverse mapper: Friend → User (if needed)
3. Add logging for debugging
4. Consider using Builder pattern for complex mappings
