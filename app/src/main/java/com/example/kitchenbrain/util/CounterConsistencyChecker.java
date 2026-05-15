package com.example.kitchenbrain.util;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * ✅ PRODUCTION UTILITY: Counter Consistency Checker
 * 
 * Ensures that followersCount and followingCount in user documents
 * match the actual size of their followers/following collections.
 * 
 * Run this:
 * 1. On app startup (background)
 * 2. After batch operations
 * 3. Periodically (e.g., every 24 hours)
 * 
 * Instagram Pattern: Background consistency reconciliation
 */
public class CounterConsistencyChecker {
    private static final String TAG = "CounterConsistency";
    private final FirebaseFirestore db;
    
    public CounterConsistencyChecker() {
        db = FirebaseFirestore.getInstance();
    }
    
    /**
     * ✅ Check and fix counters for a specific user
     * 
     * @param userId User ID to check
     * @param callback Result callback
     */
    public void checkAndFixCounters(String userId, ConsistencyCallback callback) {
        if (userId == null) {
            if (callback != null) callback.onError("Invalid user ID");
            return;
        }
        
        Log.d(TAG, "🔍 Checking counter consistency for user: " + userId);
        
        // Get actual collection sizes
        db.collection("following")
          .document(userId)
          .collection("userFollowing")
          .get()
          .addOnSuccessListener(followingSnapshot -> {
              int actualFollowingCount = followingSnapshot.size();
              
              db.collection("followers")
                .document(userId)
                .collection("userFollowers")
                .get()
                .addOnSuccessListener(followersSnapshot -> {
                    int actualFollowersCount = followersSnapshot.size();
                    
                    // Get current counters from user document
                    db.collection("users").document(userId).get()
                      .addOnSuccessListener(userDoc -> {
                          if (!userDoc.exists()) {
                              Log.w(TAG, "⚠️ User document not found: " + userId);
                              if (callback != null) callback.onError("User not found");
                              return;
                          }
                          
                          Long dbFollowingCount = userDoc.getLong("followingCount");
                          Long dbFollowersCount = userDoc.getLong("followersCount");
                          
                          int currentFollowing = dbFollowingCount != null ? dbFollowingCount.intValue() : 0;
                          int currentFollowers = dbFollowersCount != null ? dbFollowersCount.intValue() : 0;
                          
                          boolean needsFix = false;
                          Map<String, Object> updates = new HashMap<>();
                          
                          // Check following counter
                          if (currentFollowing != actualFollowingCount) {
                              Log.w(TAG, "❌ Following count mismatch: DB=" + currentFollowing + 
                                   ", Actual=" + actualFollowingCount);
                              updates.put("followingCount", actualFollowingCount);
                              needsFix = true;
                          }
                          
                          // Check followers counter
                          if (currentFollowers != actualFollowersCount) {
                              Log.w(TAG, "❌ Followers count mismatch: DB=" + currentFollowers + 
                                   ", Actual=" + actualFollowersCount);
                              updates.put("followersCount", actualFollowersCount);
                              needsFix = true;
                          }
                          
                          if (needsFix) {
                              // ✅ Fix counters
                              db.collection("users").document(userId).update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Fixed counters for user: " + userId);
                                    Log.d(TAG, "   Following: " + currentFollowing + " → " + actualFollowingCount);
                                    Log.d(TAG, "   Followers: " + currentFollowers + " → " + actualFollowersCount);
                                    
                                    if (callback != null) {
                                        callback.onSuccess(true, updates);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Failed to fix counters", e);
                                    if (callback != null) callback.onError("Failed to update: " + e.getMessage());
                                });
                          } else {
                              Log.d(TAG, "✅ Counters are consistent for user: " + userId);
                              if (callback != null) callback.onSuccess(false, null);
                          }
                      })
                      .addOnFailureListener(e -> {
                          Log.e(TAG, "❌ Failed to get user document", e);
                          if (callback != null) callback.onError("Failed to read user: " + e.getMessage());
                      });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get followers collection", e);
                    if (callback != null) callback.onError("Failed to read followers: " + e.getMessage());
                });
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "❌ Failed to get following collection", e);
              if (callback != null) callback.onError("Failed to read following: " + e.getMessage());
          });
    }
    
    /**
     * ✅ Check all users (admin operation - use sparingly)
     * 
     * WARNING: This reads ALL user documents. Use only for maintenance.
     */
    public void checkAllUsers() {
        Log.w(TAG, "⚠️ Starting full consistency check - this may take time");
        
        db.collection("users").get()
          .addOnSuccessListener(querySnapshot -> {
              Log.d(TAG, "📊 Found " + querySnapshot.size() + " users to check");
              
              int checked = 0;
              for (DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                  String userId = userDoc.getId();
                  
                  // Check each user asynchronously
                  checkAndFixCounters(userId, new ConsistencyCallback() {
                      @Override
                      public void onSuccess(boolean wasFixed, Map<String, Object> updates) {
                          if (wasFixed) {
                              Log.d(TAG, "✅ Fixed: " + userId);
                          }
                      }
                      
                      @Override
                      public void onError(String error) {
                          Log.e(TAG, "❌ Error checking " + userId + ": " + error);
                      }
                  });
                  
                  checked++;
                  if (checked % 50 == 0) {
                      Log.d(TAG, "Progress: " + checked + "/" + querySnapshot.size() + " users checked");
                  }
              }
              
              Log.d(TAG, "✅ Full consistency check completed");
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "❌ Failed to start consistency check", e);
          });
    }
    
    /**
     * Callback interface for consistency check results
     */
    public interface ConsistencyCallback {
        void onSuccess(boolean wasFixed, Map<String, Object> updates);
        void onError(String error);
    }
}
