package com.example.kitchenbrain.repository;

import android.util.Log;

import com.example.kitchenbrain.User;
import com.example.kitchenbrain.model.Result;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseFirestore db;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void checkMutualFollow(String currentUserId, String targetUserId, Callback<Result<Boolean>> callback) {
        if (currentUserId == null || targetUserId == null) {
            if (callback != null) callback.onResult(Result.error("Invalid user IDs"));
            return;
        }

        db.collection("follows")
          .whereEqualTo("fromUserId", currentUserId)
          .whereEqualTo("toUserId", targetUserId)
          .get()
          .addOnSuccessListener(currentFollowsTarget -> {
              if (currentFollowsTarget.isEmpty()) {
                  // Not following - this is NOT an error, just business logic
                  if (callback != null) callback.onResult(Result.success(false));
                  return;
              }

              db.collection("follows")
                .whereEqualTo("fromUserId", targetUserId)
                .whereEqualTo("toUserId", currentUserId)
                .get()
                .addOnSuccessListener(targetFollowsCurrent -> {
                    boolean isMutual = !targetFollowsCurrent.isEmpty();
                    // Success with result (true or false)
                    if (callback != null) callback.onResult(Result.success(isMutual));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking target follow status", e);
                    // This is an actual ERROR (network/firestore issue)
                    if (callback != null) callback.onResult(Result.error("Error: " + e.getMessage()));
                });
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "Error checking current follow status", e);
              // This is an actual ERROR (network/firestore issue)
              if (callback != null) callback.onResult(Result.error("Error: " + e.getMessage()));
          });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
