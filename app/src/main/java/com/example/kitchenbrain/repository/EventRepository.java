package com.example.kitchenbrain.repository;

import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

/**
 * 🔥 Event Repository - BACKEND-DRIVEN ARCHITECTURE
 * 
 * Client emits events ONLY - backend handles business logic
 * This is the PROPER way to build scalable systems
 * 
 * Usage:
 * EventRepository.getInstance().emitMutualFollowEvent(userA, userB);
 */
public class EventRepository {
    
    private static final String TAG = "EventRepository";
    private static EventRepository instance;
    
    private final FirebaseFirestore db;
    private final String currentUserId;
    
    private EventRepository(String currentUserId) {
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = currentUserId;
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized EventRepository getInstance(String currentUserId) {
        if (instance == null) {
            instance = new EventRepository(currentUserId);
        }
        return instance;
    }
    
    /**
     * 🔥 Emit MUTUAL_FOLLOW event
     * 
     * This is ALL the client does - backend handles the rest:
     * 1. Client emits event
     * 2. Cloud Function triggers
     * 3. Backend validates mutual follow
     * 4. Backend creates chat atomically
     * 5. Client observes result via Firestore listener
     * 
     * @param targetUserId The user to check mutual follow with
     */
    public void emitMutualFollowEvent(String targetUserId, EventCallback callback) {
        if (targetUserId == null || targetUserId.isEmpty()) {
            Log.e(TAG, "❌ Cannot emit event - targetUserId is null");
            if (callback != null) {
                callback.onError("Target user ID is required");
            }
            return;
        }
        
        if (currentUserId == null) {
            Log.e(TAG, "❌ Cannot emit event - currentUserId is null");
            if (callback != null) {
                callback.onError("User not authenticated");
            }
            return;
        }
        
        Log.d(TAG, "📤 Emitting MUTUAL_FOLLOW event: " + currentUserId + " → " + targetUserId);
        
        // Create event document
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("type", "MUTUAL_FOLLOW");
        eventData.put("fromUserId", currentUserId);
        eventData.put("toUserId", targetUserId);
        eventData.put("status", "pending");
        eventData.put("createdAt", FieldValue.serverTimestamp());
        
        // Add to events collection - triggers Cloud Function
        db.collection("events")
          .add(eventData)
          .addOnSuccessListener(documentReference -> {
              String eventId = documentReference.getId();
              Log.d(TAG, "✅ Event emitted successfully: " + eventId);
              
              if (callback != null) {
                  callback.onSuccess(eventId);
              }
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "❌ Failed to emit event: " + e.getMessage(), e);
              
              if (callback != null) {
                  callback.onError(e.getMessage());
              }
          });
    }
    
    /**
     * 🔥 Observe event status
     * 
     * Client can watch for event processing status:
     * - pending → Cloud Function hasn't processed yet
     * - processed → Chat created successfully (chatId available)
     * - failed → Something went wrong (reason available)
     * 
     * @param eventId Event ID to observe
     * @param callback Status updates
     * @return ListenerRegistration (call remove() when done)
     */
    public ListenerRegistration observeEventStatus(String eventId, EventStatusCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "❌ Cannot observe event - eventId is null");
            return null;
        }
        
        Log.d(TAG, "👀 Observing event status: " + eventId);
        
        return db.collection("events")
          .document(eventId)
          .addSnapshotListener((snapshot, error) -> {
              if (error != null) {
                  Log.e(TAG, "❌ Error observing event: " + error.getMessage(), error);
                  if (callback != null) {
                      callback.onError(error.getMessage());
                  }
                  return;
              }
              
              if (snapshot == null || !snapshot.exists()) {
                  Log.w(TAG, "⚠️ Event document not found: " + eventId);
                  return;
              }
              
              String status = snapshot.getString("status");
              String chatId = snapshot.getString("chatId");
              String reason = snapshot.getString("reason");
              
              Log.d(TAG, "📊 Event status update: " + status);
              
              if (callback != null) {
                  callback.onStatusChanged(status, chatId, reason);
              }
          });
    }
    
    /**
     * Callback for event emission
     */
    public interface EventCallback {
        void onSuccess(String eventId);
        void onError(String error);
    }
    
    /**
     * Callback for event status updates
     */
    public interface EventStatusCallback {
        void onStatusChanged(String status, String chatId, String reason);
        void onError(String error);
    }
}
