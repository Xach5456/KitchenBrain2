# 🔥 BACKEND-DRIVEN CHAT ARCHITECTURE

## 🎯 Core Principle

```
Client = emits events only
Backend = single source of truth  
Firestore = storage, not logic
```

---

## 🧱 1. FIRESTORE SCHEMA (Clean & Scalable)

### Users
```
users/{userId}
  - username: string
  - photoUrl: string
  - createdAt: timestamp
```

### Follows
```
follows/{userId}/following/{targetUserId}
  - createdAt: timestamp

follows/{userId}/followers/{targetUserId}
  - createdAt: timestamp
```

### Chats
```
chats/{chatId}
  - participants: [userA, userB]
  - createdAt: timestamp
  - updatedAt: timestamp
  - lastMessage: string
  - lastMessageTime: timestamp
  - lastMessageSenderId: string
```

### Messages (Subcollection)
```
chats/{chatId}/messages/{messageId}
  - senderId: string
  - text: string
  - timestamp: timestamp
  - type: text | image | system
  - status: sent | delivered | read
```

### Events (NEW - Critical Layer)
```
events/{eventId}
  - type: MUTUAL_FOLLOW
  - fromUserId: string
  - toUserId: string
  - status: pending | processed | failed
  - chatId: string (added after processing)
  - createdAt: timestamp
  - processedAt: timestamp
```

---

## ☁️ 2. CLOUD FUNCTIONS (Backend Authority)

### Function 1: onMutualFollow Event
```javascript
exports.onMutualFollow = functions.firestore
  .document("events/{eventId}")
  .onCreate(async (snap, context) => {
    const event = snap.data();
    
    if (event.type !== "MUTUAL_FOLLOW") return;
    
    const userA = event.fromUserId;
    const userB = event.toUserId;
    
    // 1. Verify mutual follow (server-side)
    const isMutual = await checkMutualFollow(userA, userB);
    if (!isMutual) {
      await snap.ref.update({ status: "failed", reason: "not_mutual" });
      return;
    }
    
    // 2. Generate stable chatId (server-side)
    const chatId = generateChatId(userA, userB);
    const chatRef = db.collection("chats").doc(chatId);
    
    // 3. Create chat atomically (transaction)
    await db.runTransaction(async (transaction) => {
      const chatDoc = await transaction.get(chatRef);
      
      if (!chatDoc.exists) {
        transaction.set(chatRef, {
          participants: [userA, userB],
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          lastMessage: null,
          lastMessageTime: null,
          lastMessageSenderId: null
        });
      }
    });
    
    // 4. Mark event as processed
    await snap.ref.update({
      status: "processed",
      chatId: chatId,
      processedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    console.log(`✅ Chat created: ${chatId}`);
  });
```

### Function 2: onMessageCreate (Update Chat Metadata)
```javascript
exports.onMessageCreate = functions.firestore
  .document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const chatId = context.params.chatId;
    
    // Update chat with last message
    await db.collection("chats").doc(chatId).update({
      lastMessage: message.text?.substring(0, 100) || null,
      lastMessageTime: message.timestamp,
      lastMessageSenderId: message.senderId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });
```

### Helper Functions
```javascript
async function checkMutualFollow(userA, userB) {
  const aFollowsB = await db
    .collection("follows")
    .doc(userA)
    .collection("following")
    .doc(userB)
    .get();
  
  const bFollowsA = await db
    .collection("follows")
    .doc(userB)
    .collection("following")
    .doc(userA)
    .get();
  
  return aFollowsB.exists && bFollowsA.exists;
}

function generateChatId(userA, userB) {
  return userA < userB 
    ? `${userA}_${userB}` 
    : `${userB}_${userA}`;
}
```

---

## 📱 3. CLIENT ARCHITECTURE (Clean - No Business Logic)

### ❌ REMOVE from Android:
- `ChatRepository.createChatIfMutual()`
- `MutualFollowListener.ensureChatRoomExists()`
- `FriendManager.createChatForMutualFollow()`
- Any code that writes to `chats` collection

### ✅ KEEP in Android:
- Emit events
- Observe Firestore
- Render UI
- User interactions

### Client Event Emission:
```java
// When mutual follow detected
Map<String, Object> eventData = new HashMap<>();
eventData.put("type", "MUTUAL_FOLLOW");
eventData.put("fromUserId", currentUserId);
eventData.put("toUserId", targetUserId);
eventData.put("status", "pending");
eventData.put("createdAt", FieldValue.serverTimestamp());

db.collection("events").add(eventData);
```

### Client Chat Observer:
```java
db.collection("chats")
  .whereArrayContains("participants", currentUserId)
  .orderBy("updatedAt", Query.Direction.DESCENDING)
  .limit(20)
  .addSnapshotListener((snapshot, error) -> {
      // Just render UI - no logic
      adapter.submitList(parseChats(snapshot));
  });
```

---

## 💬 4. MESSAGING SYSTEM (Scalable)

### Send Message:
```java
Map<String, Object> message = new HashMap<>();
message.put("senderId", currentUserId);
message.put("text", text);
message.put("timestamp", FieldValue.serverTimestamp());
message.put("type", "text");
message.put("status", "sent");

db.collection("chats")
  .document(chatId)
  .collection("messages")
  .add(message);
```

### Cloud Function updates chat metadata automatically!

---

## 📄 5. PAGINATION STRATEGY

### Chats Pagination:
```java
Query firstPage = db.collection("chats")
  .whereArrayContains("participants", currentUserId)
  .orderBy("updatedAt", Query.Direction.DESCENDING)
  .limit(20);

// Next page
Query nextPage = db.collection("chats")
  .whereArrayContains("participants", currentUserId)
  .orderBy("updatedAt", Query.Direction.DESCENDING)
  .startAfter(lastVisibleDoc)
  .limit(20);
```

### Messages Pagination:
```java
Query messages = db.collection("chats")
  .document(chatId)
  .collection("messages")
  .orderBy("timestamp", Query.Direction.DESCENDING)
  .limitToLast(30);
```

---

## 📡 6. OFFLINE STRATEGY

### Enable Persistence:
```java
FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
  .setPersistenceEnabled(true)
  .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
  .build();
db.setFirestoreSettings(settings);
```

### Offline Writes:
- Client writes to `events` collection → queued locally
- Cloud Function processes when online
- Client observes `events/{id}/status` for confirmation

---

## 🧠 FINAL ARCHITECTURE

### BEFORE (Current):
```
Android App
  → decides logic
  → writes Firestore
  → UI reacts
```

### AFTER (Backend-Driven):
```
Android App
  → emits events only

Cloud Functions (AUTHORITY)
  → validates logic
  → creates chats
  → updates state

Firestore
  → storage + sync

Android App
  → observes only
```

---

## 📊 IMPACT

| Area | Before | After |
|------|--------|-------|
| **Chat Creation** | Client | Backend |
| **Trust** | None | Enforced |
| **Scalability** | Limited | High |
| **Bugs** | Race conditions | Controlled |
| **Security** | Client-side | Server-side |
| **Testing** | Hard | Easy |

---

## 🚀 NEXT STEPS

1. ✅ Update Cloud Functions (already have foundation)
2. ❌ Add events collection
3. ❌ Refactor Android client (remove business logic)
4. ❌ Update Firestore rules
5. ❌ Test end-to-end flow

---

**Status: Architecture Defined - Ready for Implementation** 🎯
