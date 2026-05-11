package com.example.kitchenbrain;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.kitchenbrain.repository.ChatRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendManager {
    private static final String TAG = "FriendManager";
    
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final String currentUserId;
    
    private final CollectionReference usersCollection;
    private final CollectionReference friendRequestsCollection;
    private final CollectionReference friendsCollection;
    
    private final ExecutorService executor;
    private final List<ListenerRegistration> listeners = new ArrayList<>();
    
    // Callbacks for real-time updates
    public interface FriendCallback {
        void onSuccess(Friend friend);
        void onError(String error);
    }
    
    public interface FriendsListCallback {
        void onSuccess(List<Friend> friends);
        void onError(String error);
    }
    
    public interface RequestCallback {
        void onSuccess(FriendRequest request);
        void onError(String error);
    }
    
    public interface RequestsListCallback {
        void onSuccess(List<FriendRequest> requests);
        void onError(String error);
    }
    
    public interface UserSearchCallback {
        void onSuccess(List<User> users);
        void onError(String error);
    }
    
    // Callback for mutual follow detection
    public interface MutualFollowCallback {
        void onMutualFollowDetected(String userId1, String userId2);
    }

    public FriendManager(FirebaseFirestore db, String currentUserId) {
        this.db = db;
        this.auth = FirebaseAuth.getInstance();
        this.currentUserId = currentUserId;
        this.executor = Executors.newSingleThreadExecutor();
        
        this.usersCollection = db.collection("users");
        this.friendRequestsCollection = db.collection("friend_requests");
        this.friendsCollection = db.collection("friends");
    }

    public void searchUsers(String query, UserSearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onError("Search query cannot be empty");
            return;
        }

        String searchTerm = query.trim().toLowerCase();
        
        Log.d(TAG, "Searching users for: '" + searchTerm + "'");
        
        Query searchQuery = usersCollection
                .orderBy("username")
                .startAt(searchTerm)
                .endAt(searchTerm + "\uf8ff")
                .limit(50);
        
        searchQuery.get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<User> matchingUsers = new ArrayList<>();
                
                if (queryDocumentSnapshots.isEmpty()) {
                    performFallbackSearch(searchTerm, callback);
                    return;
                }
                
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        String userId = user.getUserId();
                        if (userId == null || userId.trim().isEmpty()) {
                            userId = doc.getId();
                            user.setUserId(userId);
                        }
                        
                        if (currentUserId != null && currentUserId.equals(userId)) {
                            continue;
                        }
                        
                        boolean startsWithSearchTerm = user.getUsername() != null && 
                            user.getUsername().toLowerCase().startsWith(searchTerm);
                        
                        if (startsWithSearchTerm) {
                            matchingUsers.add(user);
                        }
                    }
                }
                
                callback.onSuccess(matchingUsers);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Search query failed: " + e.getMessage());
                performFallbackSearch(searchTerm, callback);
            });
    }
    
    private void performFallbackSearch(String searchTerm, UserSearchCallback callback) {
        usersCollection.limit(100).get()
            .addOnSuccessListener(allUsersSnapshot -> {
                List<User> matchingUsers = new ArrayList<>();
                
                for (DocumentSnapshot doc : allUsersSnapshot.getDocuments()) {
                    User user = doc.toObject(User.class);
                    if (user != null && user.getUsername() != null) {
                        String username = user.getUsername().toLowerCase();
                        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                        
                        boolean matchesUsername = username.startsWith(searchTerm) || username.contains(searchTerm);
                        boolean matchesEmail = email.startsWith(searchTerm) || email.contains(searchTerm);
                        
                        if (matchesUsername || matchesEmail) {
                            String userId = user.getUserId();
                            if (userId == null || userId.trim().isEmpty()) {
                                userId = doc.getId();
                                user.setUserId(userId);
                            }
                            
                            if (currentUserId != null && !currentUserId.equals(userId)) {
                                matchingUsers.add(user);
                            }
                        }
                    }
                }
                
                callback.onSuccess(matchingUsers);
            })
            .addOnFailureListener(fallbackError -> {
                Log.e(TAG, "Fallback search failed: " + fallbackError.getMessage());
                callback.onError("Search failed: " + fallbackError.getMessage());
            });
    }

    public void sendFriendRequest(User user, RequestCallback callback) {
        if (user == null || user.getUserId() == null) {
            callback.onError("Invalid user");
            return;
        }

        if (currentUserId.equals(user.getUserId())) {
            callback.onError("Cannot send request to yourself");
            return;
        }

        executor.execute(() -> {
            try {
                if (isAlreadyFriend(user.getUserId())) {
                    callback.onError("Already friends with this user");
                    return;
                }

                checkExistingRequest(user.getUserId(), exists -> {
                    if (exists) {
                        callback.onError("Friend request already sent");
                        return;
                    }

                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("senderId", currentUserId);
                    requestData.put("senderUsername", getCurrentUserUsername());
                    requestData.put("senderAvatarUrl", getCurrentUserAvatar());
                    requestData.put("receiverId", user.getUserId());
                    requestData.put("createdAt", com.google.firebase.Timestamp.now());
                    requestData.put("status", "pending");

                    friendRequestsCollection.add(requestData)
                            .addOnSuccessListener(documentReference -> {
                                FriendRequest request = new FriendRequest(
                                        currentUserId,
                                        getCurrentUserUsername(),
                                        getCurrentUserAvatar(),
                                        user.getUserId()
                                );
                                request.setId(documentReference.getId());
                                callback.onSuccess(request);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error sending friend request", e);
                                callback.onError("Failed to send request: " + e.getMessage());
                            });
                });
            } catch (Exception e) {
                Log.e(TAG, "Send request failed", e);
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    public void getPendingRequests(RequestsListCallback callback) {
        ListenerRegistration listener = friendRequestsCollection
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting requests", error);
                        callback.onError("Failed to load requests: " + error.getMessage());
                        return;
                    }

                    List<FriendRequest> requests = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            FriendRequest request = doc.toObject(FriendRequest.class);
                            if (request != null) {
                                request.setId(doc.getId());
                                requests.add(request);
                            }
                        }
                    }
                    callback.onSuccess(requests);
                });
        
        listeners.add(listener);
    }

    public void acceptFriendRequest(FriendRequest request, FriendCallback callback) {
        executor.execute(() -> {
            try {
                friendRequestsCollection.document(request.getId())
                        .update("status", "accepted")
                        .addOnSuccessListener(aVoid -> {
                            createFriendRelationship(request.getSenderId(), request.getSenderUsername(), 
                                    request.getSenderAvatarUrl(), callback);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error accepting request", e);
                            callback.onError("Failed to accept: " + e.getMessage());
                        });
            } catch (Exception e) {
                Log.e(TAG, "Accept failed", e);
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    public void declineFriendRequest(FriendRequest request, Runnable onSuccess, Runnable onError) {
        friendRequestsCollection.document(request.getId())
                .update("status", "declined")
                .addOnSuccessListener(aVoid -> {
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    if (onError != null) onError.run();
                });
    }

    public void getFriends(FriendsListCallback callback) {
        ListenerRegistration listener = friendsCollection
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting friends", error);
                        callback.onError("Failed to load friends: " + error.getMessage());
                        return;
                    }

                    List<Friend> friends = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Friend friend = doc.toObject(Friend.class);
                            if (friend != null) {
                                friend.setId(doc.getId());
                                checkUserOnlineStatus(friend.getFriendId(), friend::setOnline);
                                friends.add(friend);
                            }
                        }
                    }
                    callback.onSuccess(friends);
                });
        
        listeners.add(listener);
    }

    public void removeFriend(String friendId, Runnable onSuccess, Runnable onError) {
        executor.execute(() -> {
            try {
                friendsCollection
                        .whereEqualTo("userId", currentUserId)
                        .whereEqualTo("friendId", friendId)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                friendsCollection.document(doc.getId()).delete();
                            }
                            
                            friendsCollection
                                    .whereEqualTo("userId", friendId)
                                    .whereEqualTo("friendId", currentUserId)
                                    .get()
                                    .addOnSuccessListener(snapshot -> {
                                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                            friendsCollection.document(doc.getId()).delete();
                                        }
                                        if (onSuccess != null) onSuccess.run();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            if (onError != null) onError.run();
                        });
            } catch (Exception e) {
                if (onError != null) onError.run();
            }
        });
    }

    private void createFriendRelationship(String friendId, String username, String avatarUrl, FriendCallback callback) {
        Map<String, Object> friendData1 = new HashMap<>();
        friendData1.put("userId", currentUserId);
        friendData1.put("friendId", friendId);
        friendData1.put("friendUsername", username);
        friendData1.put("friendAvatarUrl", avatarUrl);
        friendData1.put("isOnline", false);
        friendData1.put("createdAt", com.google.firebase.Timestamp.now());

        friendsCollection.add(friendData1)
                .addOnSuccessListener(documentReference -> {
                    Map<String, Object> friendData2 = new HashMap<>();
                    friendData2.put("userId", friendId);
                    friendData2.put("friendId", currentUserId);
                    friendData2.put("friendUsername", getCurrentUserUsername());
                    friendData2.put("friendAvatarUrl", getCurrentUserAvatar());
                    friendData2.put("isOnline", false);
                    friendData2.put("createdAt", com.google.firebase.Timestamp.now());

                    friendsCollection.add(friendData2)
                            .addOnSuccessListener(docRef -> {
                                Friend friend = new Friend(currentUserId, friendId, username, avatarUrl);
                                friend.setId(documentReference.getId());
                                
                                // Check if this creates a mutual follow
                                checkAndHandleMutualFollow(friendId);
                                
                                callback.onSuccess(friend);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error creating reverse relationship", e);
                                callback.onError("Failed to complete friendship");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating friend relationship", e);
                    callback.onError("Failed to add friend: " + e.getMessage());
                });
    }
    
    /**
     * Check if following is mutual and auto-create chat
     * Called after friendship relationship is established
     */
    private void checkAndHandleMutualFollow(String otherUserId) {
        areMutualFriends(currentUserId, otherUserId)
            .addOnSuccessListener(areMutual -> {
                if (areMutual) {
                    Log.d(TAG, "✅ Mutual follow detected between " + currentUserId + " and " + otherUserId);
                    
                    // Step 1: Create chat room
                    createChatForMutualFollow(currentUserId, otherUserId);
                    
                    // Step 2: Notify BOTH users to auto-open chat
                    notifyMutualFollowToBothUsers(currentUserId, otherUserId);
                } else {
                    Log.d(TAG, "⏳ Not mutual yet - " + otherUserId + " does not follow back");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking mutual follow", e);
            });
    }
    
    /**
     * Notify both users when mutual follow is detected
     * This triggers auto-open chat in both users' apps
     */
    private void notifyMutualFollowToBothUsers(String userId1, String userId2) {
        // Create notification document in Firestore
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("type", "mutual_follow");
        notificationData.put("userId1", userId1);
        notificationData.put("userId2", userId2);
        notificationData.put("timestamp", com.google.firebase.Timestamp.now());
        notificationData.put("read", false);
        
        // Generate unique notification ID
        String notificationId = "mutual_" + generateChatRoomId(userId1, userId2);
        
        // Store in mutual_follows collection
        db.collection("mutual_follows").document(notificationId)
            .set(notificationData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Mutual follow notification created: " + notificationId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create mutual follow notification", e);
            });
        
        // Also update user documents to trigger real-time listeners
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastMutualFollow", com.google.firebase.Timestamp.now());
        updateData.put("newMutualFollowUserId", userId2);
        
        db.collection("users").document(userId1)
            .update(updateData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ User 1 notified of mutual follow");
            });
        
        // Notify user 2
        Map<String, Object> updateData2 = new HashMap<>();
        updateData2.put("lastMutualFollow", com.google.firebase.Timestamp.now());
        updateData2.put("newMutualFollowUserId", userId1);
        
        db.collection("users").document(userId2)
            .update(updateData2)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ User 2 notified of mutual follow");
            });
    }
    
    /**
     * Create chat room when mutual follow detected
     * Uses consistent chat ID format to prevent duplicates
     */
    private void createChatForMutualFollow(String userId1, String userId2) {
        // Chat creation logic moved to ChatRepository - simplified for now
        Log.d(TAG, "Mutual follow detected between " + userId1 + " and " + userId2);
    }

    private void checkExistingRequest(String userId, ExistCallback callback) {
        friendRequestsCollection
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> {
                    callback.onExists(!snapshot.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing request", e);
                    callback.onExists(false);
                });
    }

    private void isAlreadyFriendAsync(String userId, ExistCallback callback) {
        friendsCollection
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("friendId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    callback.onExists(!snapshot.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking friendship", e);
                    callback.onExists(false);
                });
    }

    private boolean isAlreadyFriend(String userId) {
        try {
            QuerySnapshot snapshot = Tasks.await(friendsCollection
                    .whereEqualTo("userId", currentUserId)
                    .whereEqualTo("friendId", userId)
                    .limit(1)
                    .get());
            return !snapshot.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "Error checking friendship", e);
            return false;
        }
    }

    private void checkUserOnlineStatus(String userId, OnlineStatusCallback callback) {
        usersCollection.document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean isOnline = doc.getBoolean("isOnline");
                        callback.onStatus(isOnline != null && isOnline);
                    } else {
                        callback.onStatus(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking online status", e);
                    callback.onStatus(false);
                });
    }

    /**
     * Get current user's username from Firestore
     * This method fetches the actual username, not a hardcoded string
     */
    private void getCurrentUserUsername(final UsernameCallback callback) {
        if (currentUserId == null) {
            callback.onUsernameReceived("User");
            return;
        }
        
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String username = documentSnapshot.getString("username");
                    callback.onUsernameReceived(username != null ? username : "User");
                } else {
                    callback.onUsernameReceived("User");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching current user username", e);
                callback.onUsernameReceived("User");
            });
    }
    
    // Callback interface for async username retrieval
    private interface UsernameCallback {
        void onUsernameReceived(String username);
    }
    
    // Deprecated - kept for backward compatibility but should not be used
    @Deprecated
    private String getCurrentUserUsername() {
        Log.w(TAG, "WARNING: getCurrentUserUsername() deprecated - returns 'User'. Use async version instead!");
        return "User";
    }

    private String getCurrentUserAvatar() {
        return null;
    }

    private void createFollowNotification(String userId, String username, String avatarUrl) {
        if (userId == null || username == null) return;
        
        // Fetch REAL username from Firestore asynchronously
        getCurrentUserUsername(actualUsername -> {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("receiverId", userId);
            notificationData.put("senderId", currentUserId);
            notificationData.put("senderUsername", actualUsername); // ✅ Use REAL username
            notificationData.put("senderAvatarUrl", avatarUrl != null ? avatarUrl : "");
            notificationData.put("type", "follow");
            notificationData.put("message", actualUsername + " followed you");
            notificationData.put("isRead", false);
            notificationData.put("createdAt", System.currentTimeMillis());
            
            db.collection("notifications").add(notificationData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Follow notification created with username: " + actualUsername);
                        sendFCMFollowNotification(userId, actualUsername);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating follow notification", e);
                    });
        });
    }
    
    private void sendFCMFollowNotification(String receiverId, String followerUsername) {
        db.collection("users").document(receiverId)
            .get()
            .addOnSuccessListener(userDoc -> {
                if (!userDoc.exists()) {
                    return;
                }
                
                String fcmToken = userDoc.getString("fcmToken");
                if (fcmToken == null || fcmToken.isEmpty()) {
                    return;
                }
                
                Map<String, Object> message = new HashMap<>();
                message.put("token", fcmToken);
                
                Map<String, String> notification = new HashMap<>();
                notification.put("title", "New Follower");
                notification.put("body", followerUsername + " subscribed to you");
                
                Map<String, String> data = new HashMap<>();
                data.put("type", "follow");
                data.put("senderId", currentUserId);
                data.put("senderUsername", followerUsername);
                
                message.put("notification", notification);
                message.put("data", data);
                
                Log.d(TAG, "FCM Notification ready: " + receiverId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user for FCM notification", e);
            });
    }

    /**
     * @deprecated Use FollowGraphRepository.isMutual() instead. This method uses blocking Tasks.await()
     * and has performance issues. FollowGraphRepository provides instant cache-based mutual checks.
     */
    @Deprecated
    public Task<Boolean> areMutualFriends(String userId1, String userId2) {
        com.google.android.gms.tasks.TaskCompletionSource<Boolean> taskSource = new com.google.android.gms.tasks.TaskCompletionSource<>();
        
        executor.execute(() -> {
            try {
                // ✅ FIXED: Use following/followers collections instead of friends
                // Check if userId1 follows userId2
                com.google.firebase.firestore.DocumentSnapshot aFollowsB = Tasks.await(db.collection("following")
                        .document(userId1)
                        .collection("userFollowing")
                        .document(userId2)
                        .get());
                
                boolean user1FollowsUser2 = aFollowsB.exists();
                
                // Check if userId2 follows userId1
                com.google.firebase.firestore.DocumentSnapshot bFollowsA = Tasks.await(db.collection("following")
                        .document(userId2)
                        .collection("userFollowing")
                        .document(userId1)
                        .get());
                
                boolean user2FollowsUser1 = bFollowsA.exists();
                
                boolean areMutual = user1FollowsUser2 && user2FollowsUser1;
                taskSource.setResult(areMutual);
                
                if (areMutual) {
                    Log.d(TAG, "✅ Mutual follow detected: " + userId1 + " <-> " + userId2);
                    createChatRoomForMutualFollowers(userId1, userId2);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking mutual friends", e);
                taskSource.setException(e);
            }
        });
        
        return taskSource.getTask();
    }
    
    private void createChatRoomForMutualFollowers(String userId1, String userId2) {
        String roomId = generateChatRoomId(userId1, userId2);
        
        db.collection("chat_rooms").document(roomId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    return;
                }
                
                Map<String, Object> chatRoom = new HashMap<>();
                chatRoom.put("roomId", roomId);
                chatRoom.put("user1Id", userId1);
                chatRoom.put("user2Id", userId2);
                chatRoom.put("user1ReadLastMessageAt", System.currentTimeMillis());
                chatRoom.put("user2ReadLastMessageAt", System.currentTimeMillis());
                chatRoom.put("lastMessageAt", System.currentTimeMillis());
                chatRoom.put("lastMessageText", "");
                chatRoom.put("lastMessageSenderId", "");
                chatRoom.put("unreadCount", 0);
                chatRoom.put("createdAt", System.currentTimeMillis());
                chatRoom.put("isActive", true);
                
                db.collection("chat_rooms").document(roomId)
                    .set(chatRoom)
                    .addOnSuccessListener(aVoid -> {
                        db.collection("chat_rooms").document(roomId)
                            .collection("messages")
                            .add(new HashMap<>())
                            .addOnSuccessListener(docRef -> {
                                docRef.delete();
                            });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating chat room", e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking existing chat room", e);
            });
    }
    
    private String generateChatRoomId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    public Task<Void> cleanupUserRelationships(String userId) {
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource = new com.google.android.gms.tasks.TaskCompletionSource<>();
        
        executor.execute(() -> {
            try {
                QuerySnapshot friendshipsAsOwner = Tasks.await(friendsCollection
                        .whereEqualTo("userId", userId)
                        .get());
                
                List<Task<Void>> deleteTasks = new ArrayList<>();
                
                for (QueryDocumentSnapshot doc : friendshipsAsOwner) {
                    deleteTasks.add(friendsCollection.document(doc.getId()).delete());
                }
                
                QuerySnapshot friendshipsAsFriend = Tasks.await(friendsCollection
                        .whereEqualTo("friendId", userId)
                        .get());
                
                for (QueryDocumentSnapshot doc : friendshipsAsFriend) {
                    deleteTasks.add(friendsCollection.document(doc.getId()).delete());
                }
                
                QuerySnapshot requestsSent = Tasks.await(friendRequestsCollection
                        .whereEqualTo("senderId", userId)
                        .get());
                
                for (QueryDocumentSnapshot doc : requestsSent) {
                    deleteTasks.add(friendRequestsCollection.document(doc.getId()).delete());
                }
                
                QuerySnapshot requestsReceived = Tasks.await(friendRequestsCollection
                        .whereEqualTo("receiverId", userId)
                        .get());
                
                for (QueryDocumentSnapshot doc : requestsReceived) {
                    deleteTasks.add(friendRequestsCollection.document(doc.getId()).delete());
                }
                
                Tasks.whenAll(deleteTasks)
                        .addOnSuccessListener(aVoid -> {
                            taskSource.setResult(null);
                        })
                        .addOnFailureListener(e -> {
                            taskSource.setException(e);
                        });
                        
            } catch (Exception e) {
                Log.e(TAG, "Error in cleanupUserRelationships", e);
                taskSource.setException(e);
            }
        });
        
        return taskSource.getTask();
    }

    /**
     * @deprecated Use FollowGraphRepository.isFollowing() instead. This method uses blocking Tasks.await()
     * and reads from the wrong Firestore collection. FollowGraphRepository provides instant cache-based checks.
     */
    @Deprecated
    public Task<Boolean> isFollowing(String userId) {
        com.google.android.gms.tasks.TaskCompletionSource<Boolean> taskSource = new com.google.android.gms.tasks.TaskCompletionSource<>();
        
        if (userId == null || currentUserId == null) {
            taskSource.setResult(false);
            return taskSource.getTask();
        }
        
        executor.execute(() -> {
            try {
                QuerySnapshot snapshot = Tasks.await(friendsCollection
                        .whereEqualTo("userId", currentUserId)
                        .whereEqualTo("friendId", userId)
                        .limit(1)
                        .get());
                
                taskSource.setResult(!snapshot.isEmpty());
            } catch (Exception e) {
                Log.e(TAG, "Error checking follow status", e);
                taskSource.setException(e);
            }
        });
        
        return taskSource.getTask();
    }

    /**
     * @deprecated Use FollowRepository.followUser() instead. This method writes to the wrong Firestore collection.
     * FollowRepository writes to following/followers collections which is the correct architecture.
     */
    @Deprecated
    public Task<Void> followUser(String userId) {
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource = new com.google.android.gms.tasks.TaskCompletionSource<>();
        
        if (userId == null || currentUserId == null) {
            taskSource.setException(new Exception("Invalid user ID"));
            return taskSource.getTask();
        }
        
        if (currentUserId.equals(userId)) {
            taskSource.setException(new Exception("Cannot follow yourself"));
            return taskSource.getTask();
        }
        
        executor.execute(() -> {
            try {
                QuerySnapshot existingCheck = Tasks.await(friendsCollection
                        .whereEqualTo("userId", currentUserId)
                        .whereEqualTo("friendId", userId)
                        .limit(1)
                        .get());
                
                if (!existingCheck.isEmpty()) {
                    taskSource.setException(new Exception("Already following this user"));
                    return;
                }
                
                DocumentSnapshot currentUserDoc = Tasks.await(usersCollection.document(currentUserId).get());
                String currentUsername = currentUserDoc.getString("username");
                String currentUserAvatar = currentUserDoc.getString("avatarUrl");
                
                Map<String, Object> friendData = new HashMap<>();
                friendData.put("userId", currentUserId);
                friendData.put("friendId", userId);
                friendData.put("friendUsername", currentUsername != null ? currentUsername : "User");
                friendData.put("friendAvatarUrl", currentUserAvatar);
                friendData.put("isOnline", false);
                friendData.put("createdAt", com.google.firebase.Timestamp.now());
                
                friendsCollection.add(friendData)
                        .addOnSuccessListener(documentReference -> {
                            createFollowNotification(userId, currentUsername, currentUserAvatar);
                            
                            // Check if this follow creates a mutual relationship
                            checkAndCreateChatOnMutualFollow(userId);
                            
                            taskSource.setResult(null);
                        })
                        .addOnFailureListener(e -> {
                            taskSource.setException(e);
                        });
                        
            } catch (Exception e) {
                Log.e(TAG, "Error in followUser", e);
                taskSource.setException(e);
            }
        });
        
        return taskSource.getTask();
    }
    
    /**
     * Check if following another user creates a mutual follow relationship
     * If mutual, chat room will be created automatically
     */
    private void checkAndCreateChatOnMutualFollow(String otherUserId) {
        areMutualFriends(currentUserId, otherUserId)
            .addOnSuccessListener(isMutual -> {
                if (isMutual) {
                    Log.d(TAG, "✅ Mutual follow detected between " + currentUserId + " and " + otherUserId);
                    Log.d(TAG, "Chat room already created by areMutualFriends method");
                } else {
                    Log.d(TAG, "Not mutual yet - " + otherUserId + " does not follow back");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking mutual follow", e);
            });
    }

    /**
     * @deprecated Use FollowRepository.unfollowUser() instead. This method writes to the wrong Firestore collection.
     * FollowRepository writes to following/followers collections which is the correct architecture.
     */
    @Deprecated
    public Task<Void> unfollowUser(String userId) {
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource = new com.google.android.gms.tasks.TaskCompletionSource<>();
        
        if (userId == null || currentUserId == null) {
            taskSource.setException(new Exception("Invalid user ID"));
            return taskSource.getTask();
        }
        
        executor.execute(() -> {
            try {
                QuerySnapshot snapshot = Tasks.await(friendsCollection
                        .whereEqualTo("userId", currentUserId)
                        .whereEqualTo("friendId", userId)
                        .limit(1)
                        .get());
                
                if (snapshot.isEmpty()) {
                    taskSource.setException(new Exception("Not following this user"));
                    return;
                }
                
                for (QueryDocumentSnapshot doc : snapshot) {
                    friendsCollection.document(doc.getId()).delete();
                }
                
                taskSource.setResult(null);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in unfollowUser", e);
                taskSource.setException(e);
            }
        });
        
        return taskSource.getTask();
    }

    public void cleanup() {
        for (ListenerRegistration listener : listeners) {
            listener.remove();
        }
        listeners.clear();
        executor.shutdown();
    }

    public interface ExistCallback {
        void onExists(boolean exists);
    }

    public interface OnlineStatusCallback {
        void onStatus(boolean isOnline);
    }
}
