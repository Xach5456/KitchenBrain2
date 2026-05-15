# Firestore Structure Specification

## Production-Grade Database Design

### Users Collection
```
users/{userId}
{
  "userId": "string",
  "username": "string",
  "email": "string",
  "avatarUrl": "string",
  "bio": "string",
  "followingCount": "number",
  "followersCount": "number",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Following Collection
```
following/{userId}/userFollowing/{targetUserId}
{
  "followedAt": "timestamp"
}
```

### Followers Collection
```
followers/{userId}/userFollowers/{followerUserId}
{
  "followedAt": "timestamp"
}
```

### Chats Collection
```
chats/{chatId}
{
  "participants": ["userId1", "userId2"],
  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "lastMessage": "string",
  "lastMessageTime": "timestamp",
  "active": "boolean",
  "mutualFollowAt": "timestamp", // When mutual follow was established
  "archivedAt": "timestamp" // When chat was archived (if applicable)
}
```

### Messages Collection
```
chats/{chatId}/messages/{messageId}
{
  "messageId": "string",
  "senderId": "string",
  "receiverId": "string",
  "text": "string",
  "messageType": "string", // "text", "image", "file"
  "timestamp": "timestamp",
  "messageStatus": "string", // "sent", "delivered", "read"
  "isPinned": "boolean",
  "editedAt": "timestamp", // If message was edited
  "editedText": "string", // Original text before edit
  "reactions": {
    "userId": "emoji"
  }
}
```

## Indexes Required

### Composite Indexes
1. **Following Queries**
   - Collection: `following/{userId}/userFollowing`
   - Fields: `followedAt` (ascending)

2. **Followers Queries**
   - Collection: `followers/{userId}/userFollowers`
   - Fields: `followedAt` (ascending)

3. **Messages Queries**
   - Collection: `chats/{chatId}/messages`
   - Fields: `timestamp` (ascending)

4. **Chat List Queries**
   - Collection: `chats`
   - Fields: `participants` (array), `updatedAt` (descending)

## Security Rules

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users can read/write their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow read: if request.auth != null;
    }
    
    // Following relationships
    match /following/{userId}/userFollowing/{targetUserId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow read: if request.auth != null && 
        (request.auth.uid == userId || request.auth.uid == targetUserId);
    }
    
    // Followers relationships
    match /followers/{userId}/userFollowers/{followerUserId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow read: if request.auth != null && 
        (request.auth.uid == userId || request.auth.uid == followerUserId);
    }
    
    // Chats - only accessible by participants
    match /chats/{chatId} {
      allow read, write: if request.auth != null && 
        request.auth.uid in resource.data.participants;
      
      // Messages within chats
      match /messages/{messageId} {
        allow read, write: if request.auth != null && 
          request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.participants;
      }
    }
  }
}
```

## Key Design Principles

### 1. Consistent Chat IDs
- Chat IDs are generated using lexicographic ordering: `userId1_userId2`
- Ensures consistent chat room identification regardless of who initiates

### 2. Denormalized Counters
- `followingCount` and `followersCount` are maintained in user documents
- Updated atomically with follow/unfollow operations

### 3. Mutual Follow Detection
- Chat creation is triggered automatically when mutual follow is established
- `mutualFollowAt` timestamp records when chat became available

### 4. Message Status Tracking
- Three-state message delivery: sent → delivered → read
- Status updates are atomic and real-time

### 5. Efficient Queries
- All queries use indexed fields for optimal performance
- Pagination support for large message histories

### 6. Data Integrity
- All follow operations use Firestore transactions
- Chat creation is atomic with mutual follow detection

### 7. Privacy & Security
- Users can only access chats they participate in
- Follow relationships are bidirectional but independently stored
- Read access is granted to both participants in a relationship
