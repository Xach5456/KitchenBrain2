package com.example.kitchenbrain.search;

import android.util.Log;
import com.example.kitchenbrain.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔥 USER SEARCH ENGINE
 * Легкий поиск пользователей по username
 */
public class UserSearchEngine {
    
    private static final String TAG = "UserSearchEngine";
    private final FirebaseFirestore db;
    
    public UserSearchEngine() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Поиск пользователей по username (case-insensitive)
     */
    public Query searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        
        // Ищем по usernameLower для case-insensitive поиска
        String searchQuery = query.toLowerCase().trim();
        
        Log.d(TAG, "🔍 Searching users: " + searchQuery);
        
        return db.collection("users")
                .whereGreaterThanOrEqualTo("usernameLower", searchQuery)
                .whereLessThan("usernameLower", searchQuery + "\uf8ff") // Unicode range for prefix search
                .limit(20);
    }
    
    /**
     * Получение пользователей по ID списком
     */
    public void getUsersByIds(List<String> userIds, UserSearchCallback callback) {
        if (userIds == null || userIds.isEmpty()) {
            callback.onResult(new ArrayList<>());
            return;
        }
        
        db.collection("users")
                .whereIn("userId", userIds)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<User> users = new ArrayList<>();
                    for (var doc : snapshot) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    callback.onResult(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting users by IDs", e);
                    callback.onError(e.getMessage());
                });
    }
    
    public interface UserSearchCallback {
        void onResult(List<User> users);
        void onError(String error);
    }
}
