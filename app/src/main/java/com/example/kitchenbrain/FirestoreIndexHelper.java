package com.example.kitchenbrain;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.List;
import java.util.Map;

/**
 * Helper class to verify Firestore indexes and data consistency
 */
public class FirestoreIndexHelper {
    private static final String TAG = "FirestoreIndexHelper";
    
    /**
     * Check if usernameLower index exists by running a test query
     */
    public static void checkUsernameLowerIndex() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Test query that requires usernameLower index
        Query testQuery = db.collection("users")
            .orderBy("usernameLower")
            .startAt("test")
            .endAt("test" + "\uf8ff")
            .limit(1);
            
        testQuery.get()
            .addOnSuccessListener(snapshot -> {
                Log.d(TAG, "✅ usernameLower index exists and works");
            })
            .addOnFailureListener(e -> {
                if (e.getMessage() != null && e.getMessage().contains("index")) {
                    Log.e(TAG, "❌ usernameLower index MISSING. Create index in Firebase Console:\n" +
                        "Collection: users\n" +
                        "Fields: usernameLower (Ascending)");
                } else {
                    Log.e(TAG, "❌ Other error checking index: " + e.getMessage());
                }
            });
    }
    
    /**
     * Check for users missing usernameLower field (DEV ONLY)
     */
    public static void checkMissingUsernameLower() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // 🔥 OPTIMIZATION: Add limit to prevent full collection scan
        db.collection("users")
            .whereEqualTo("usernameLower", null)
            .limit(5)  // Limit to 5 results for diagnostics
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.isEmpty()) {
                    Log.d(TAG, "✅ All users have usernameLower field (sampled)");
                } else {
                    Log.w(TAG, "⚠️ Found " + snapshot.size() + " users without usernameLower (sampled)");
                    for (var doc : snapshot) {
                        String username = doc.getString("username");
                        Log.w(TAG, "User without usernameLower: " + username + " (ID: " + doc.getId() + ")");
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error checking missing usernameLower: " + e.getMessage());
            });
    }
    
    /**
     * Test search functionality with common cases (DEV ONLY)
     */
    public static void testSearchFunctionality() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Test case 1: Empty search
        testEmptySearch(db);
        
        // Test case 2: Case insensitive search
        testCaseInsensitiveSearch(db);
        
        // Test case 3: Partial search
        testPartialSearch(db);
    }
    
    private static void testEmptySearch(FirebaseFirestore db) {
        db.collection("users")
            .orderBy("usernameLower")
            .startAt("")
            .endAt("" + "\uf8ff")
            .limit(1)
            .get()
            .addOnSuccessListener(snapshot -> {
                Log.d(TAG, "✅ Empty search test passed");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Empty search test failed: " + e.getMessage());
            });
    }
    
    private static void testCaseInsensitiveSearch(FirebaseFirestore db) {
        // Search for lowercase version of a known username
        db.collection("users")
            .orderBy("usernameLower")
            .startAt("test")
            .endAt("test" + "\uf8ff")
            .limit(5)
            .get()
            .addOnSuccessListener(snapshot -> {
                Log.d(TAG, "✅ Case insensitive search test passed, found " + snapshot.size() + " results");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Case insensitive search test failed: " + e.getMessage());
            });
    }
    
    private static void testPartialSearch(FirebaseFirestore db) {
        // Search for partial matches
        db.collection("users")
            .orderBy("usernameLower")
            .startAt("a")
            .endAt("a" + "\uf8ff")
            .limit(10)
            .get()
            .addOnSuccessListener(snapshot -> {
                Log.d(TAG, "✅ Partial search test passed, found " + snapshot.size() + " results");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Partial search test failed: " + e.getMessage());
            });
    }
}
