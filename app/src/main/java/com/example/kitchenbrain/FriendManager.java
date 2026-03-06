package com.example.kitchenbrain;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendManager {
    private static final String TAG = "FriendManager";
    private FirebaseFirestore db;
    private String currentUserId;
    private ExecutorService executor;

    public FriendManager(FirebaseFirestore db, String currentUserId) {
        this.db = db;
        this.currentUserId = currentUserId;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Clean up resources and shut down the executor service
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Follow a user
     */
    public Task<Void> followUser(String userIdToFollow) {
        if (currentUserId == null || userIdToFollow == null || currentUserId.equals(userIdToFollow)) {
            return Tasks.forException(new Exception("Invalid user IDs"));
        }

        // Update current user's following list
        DocumentReference currentUserRef = db.collection("users").document(currentUserId);
        // Update target user's followers list
        DocumentReference targetUserRef = db.collection("users").document(userIdToFollow);

        // Perform batch update to ensure both operations happen atomically
        return db.runBatch(batch -> {
            batch.update(currentUserRef, "following", FieldValue.arrayUnion(userIdToFollow));
            batch.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId));
        });
    }

    /**
     * Unfollow a user
     */
    public Task<Void> unfollowUser(String userIdToUnfollow) {
        if (currentUserId == null || userIdToUnfollow == null) {
            return Tasks.forException(new Exception("Invalid user IDs"));
        }

        // Update current user's following list
        DocumentReference currentUserRef = db.collection("users").document(currentUserId);
        // Update target user's followers list
        DocumentReference targetUserRef = db.collection("users").document(userIdToUnfollow);

        // Perform batch update to ensure both operations happen atomically
        return db.runBatch(batch -> {
            batch.update(currentUserRef, "following", FieldValue.arrayRemove(userIdToUnfollow));
            batch.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId));
        });
    }

    /**
     * Check if two users have a mutual follow relationship
     */
    public Task<Boolean> areMutualFriends(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) {
            return Tasks.forResult(false);
        }

        // First, get user1's data to check if they follow user2
        return db.collection("users").document(userId1).get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        User user1 = task.getResult().toObject(User.class);
                        if (user1 != null && user1.getFollowing() != null) {
                            boolean user1FollowsUser2 = user1.getFollowing().contains(userId2);
                            
                            if (user1FollowsUser2) {
                                // Now check if user2 follows user1
                                return db.collection("users").document(userId2).get()
                                    .continueWith(secondaryTask -> {
                                        if (secondaryTask.isSuccessful()) {
                                            User user2 = secondaryTask.getResult().toObject(User.class);
                                            if (user2 != null && user2.getFollowing() != null) {
                                                return user2.getFollowing().contains(userId1);
                                            }
                                        }
                                        return false;
                                    });
                            } else {
                                // If user1 doesn't follow user2, they're not mutual
                                return Tasks.forResult(false);
                            }
                        }
                    }
                    return Tasks.forResult(false);
                });
    }

    /**
     * Check if current user follows another user
     */
    public Task<Boolean> isFollowing(String userIdToCheck) {
        if (currentUserId == null || userIdToCheck == null) {
            return Tasks.forResult(false);
        }

        return db.collection("users").document(currentUserId).get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        User user = task.getResult().toObject(User.class);
                        if (user != null && user.getFollowing() != null) {
                            return user.getFollowing().contains(userIdToCheck);
                        }
                    }
                    return false;
                });
    }

    /**
     * Check if a user follows the current user
     */
    public Task<Boolean> isFollowedBy(String userIdToCheck) {
        if (currentUserId == null || userIdToCheck == null) {
            return Tasks.forResult(false);
        }

        return db.collection("users").document(userIdToCheck).get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        User user = task.getResult().toObject(User.class);
                        if (user != null && user.getFollowing() != null) {
                            return user.getFollowing().contains(currentUserId);
                        }
                    }
                    return false;
                });
    }

    /**
     * Clean up friend relationships when a user deletes their account
     */
    public Task<Void> cleanupUserRelationships(String userIdToDelete) {
        if (userIdToDelete == null) {
            return Tasks.forException(new Exception("User ID cannot be null"));
        }

        // Get the user document to find their followers and following lists
        return db.collection("users").document(userIdToDelete).get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        User userToDelete = task.getResult().toObject(User.class);
                        
                        if (userToDelete != null) {
                            List<String> followers = userToDelete.getFollowers();
                            List<String> following = userToDelete.getFollowing();
                            
                            // Create batch operation to clean up all relationships
                            return db.runBatch(batch -> {
                                // Remove this user from all their followers' following lists
                                if (followers != null) {
                                    for (String followerId : followers) {
                                        DocumentReference followerRef = db.collection("users").document(followerId);
                                        batch.update(followerRef, "following", FieldValue.arrayRemove(userIdToDelete));
                                    }
                                }
                                
                                // Remove this user from all users they were following's followers lists
                                if (following != null) {
                                    for (String followingId : following) {
                                        DocumentReference followingRef = db.collection("users").document(followingId);
                                        batch.update(followingRef, "followers", FieldValue.arrayRemove(userIdToDelete));
                                    }
                                }
                            });
                        }
                    }
                    // Return a successful task even if user document wasn't found
                    return Tasks.forResult(null);
                });
    }
}