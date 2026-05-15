package com.example.kitchenbrain.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.model.MessageStatus;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessageRepository {
    private static final String TAG = "MessageRepository";
    private final FirebaseFirestore db;

    public MessageRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void sendMessage(String chatId, String senderId, String receiverId, String text, String messageType, OnMessageCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onError("Message cannot be empty");
            return;
        }

        String messageId = UUID.randomUUID().toString();
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", senderId);
        messageData.put("receiverId", receiverId);
        messageData.put("text", text.trim());
        messageData.put("timestamp", new com.google.firebase.Timestamp(new java.util.Date()));
        messageData.put("messageStatus", MessageStatus.SENT.getValue());
        messageData.put("messageType", messageType != null ? messageType : "text");

        db.collection("messages").document(chatId)
          .collection("user_messages")
          .document(messageId)
          .set(messageData)
          .addOnSuccessListener(aVoid -> {
              Log.d(TAG, "Message sent successfully: " + messageId);
              ChatMessage message = new ChatMessage(messageId, senderId, receiverId, text.trim(), 
                  (com.google.firebase.Timestamp) messageData.get("timestamp"), 
                  ChatMessage.MessageStatus.SENT);
              message.setMessageType(messageType);
              if (callback != null) callback.onSuccess(message);
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "Failed to send message", e);
              if (callback != null) callback.onError(e.getMessage());
          });
    }

    public LiveData<List<ChatMessage>> loadMessages(String chatId, int limit) {
        MutableLiveData<List<ChatMessage>> liveData = new MutableLiveData<>();
        
        if (chatId == null) {
            liveData.postValue(new ArrayList<>());
            return liveData;
        }

        db.collection("messages").document(chatId)
          .collection("user_messages")
          .orderBy("timestamp", Query.Direction.ASCENDING)
          .limit(limit)
          .addSnapshotListener((snapshot, error) -> {
              if (error != null) {
                  Log.e(TAG, "Error loading messages", error);
                  liveData.postValue(new ArrayList<>());
                  return;
              }

              if (snapshot != null) {
                  List<ChatMessage> messages = new ArrayList<>();
                  for (DocumentSnapshot doc : snapshot.getDocuments()) {
                      ChatMessage message = doc.toObject(ChatMessage.class);
                      message.setMessageId(doc.getId());
                      messages.add(message);
                  }
                  liveData.postValue(messages);
              }
          });

        return liveData;
    }

    public interface OnMessageCallback {
        void onSuccess(ChatMessage message);
        void onError(String error);
    }
}
