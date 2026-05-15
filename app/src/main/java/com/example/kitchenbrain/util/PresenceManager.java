package com.example.kitchenbrain.util;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class PresenceManager {

    private static final String TAG = "PresenceManager";
    private final FirebaseFirestore db;
    private final String userId;
    private ListenerRegistration presenceListener;
    private boolean isOnline = false;
    private long lastActiveTime = System.currentTimeMillis();
    private final MutableLiveData<Boolean> onlineStatus = new MutableLiveData<>(false);
    private final MutableLiveData<String> otherUserStatus = new MutableLiveData<>("Offline");

    public PresenceManager(String userId) {
        this.userId = userId;
        this.db = FirebaseFirestore.getInstance();
    }

    public void setOnline() {
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isOnline", true);
        updates.put("lastSeen", new com.google.firebase.Timestamp(new java.util.Date()));

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                isOnline = true;
                lastActiveTime = System.currentTimeMillis();
                onlineStatus.setValue(true);
                Log.d(TAG, "User set to online");
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to set online status", e));
    }

    public void setOffline() {
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isOnline", false);
        updates.put("lastSeen", new com.google.firebase.Timestamp(new java.util.Date()));

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                isOnline = false;
                onlineStatus.setValue(false);
                Log.d(TAG, "User set to offline");
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to set offline status", e));
    }

    public void listenToUserStatus(String otherUserId) {
        if (otherUserId == null) return;

        if (presenceListener != null) {
            presenceListener.remove();
        }

        presenceListener = db.collection("users").document(otherUserId)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error listening to user status", error);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Boolean isOnline = snapshot.getBoolean("isOnline");
                    Object lastSeen = snapshot.get("lastSeen");

                    if (isOnline != null && isOnline) {
                        otherUserStatus.setValue("Online");
                    } else if (lastSeen != null) {
                        com.google.firebase.Timestamp timestamp = 
                            (com.google.firebase.Timestamp) lastSeen;
                        long diff = System.currentTimeMillis() - timestamp.toDate().getTime();
                        
                        if (diff < 60000) {
                            otherUserStatus.setValue("Just now");
                        } else if (diff < 3600000) {
                            long minutes = diff / 60000;
                            otherUserStatus.setValue(minutes + "m ago");
                        } else if (diff < 86400000) {
                            long hours = diff / 3600000;
                            otherUserStatus.setValue(hours + "h ago");
                        } else {
                            otherUserStatus.setValue("Yesterday");
                        }
                    } else {
                        otherUserStatus.setValue("Offline");
                    }
                }
            });

        Log.d(TAG, "Listening to status of user: " + otherUserId);
    }

    public LiveData<Boolean> getOnlineStatus() {
        return onlineStatus;
    }

    public LiveData<String> getOtherUserStatus() {
        return otherUserStatus;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void cleanup() {
        if (presenceListener != null) {
            presenceListener.remove();
            presenceListener = null;
        }
        setOffline();
    }
}
