package com.example.kitchenbrain.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.kitchenbrain.model.ChatMessage;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatRepositoryExtended {

    private static final String TAG = "ChatRepositoryExt";
    private final FirebaseFirestore db;
    private String chatId;

    public ChatRepositoryExtended() {
        db = FirebaseFirestore.getInstance();
    }

    public void initialize(String currentUserId, String otherUserId) {
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatId = currentUserId + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUserId;
        }
    }

    public LiveData<List<ChatMessage>> loadMessagesWithPagination(int limit, int startAt) {
        MutableLiveData<List<ChatMessage>> liveData = new MutableLiveData<>();
        
        if (chatId == null) {
            liveData.postValue(new ArrayList<>());
            return liveData;
        }

        db.collection("chats").document(chatId)
          .collection("messages")
          .orderBy("timestamp", Query.Direction.DESCENDING)
          .startAt(startAt)
          .limit(limit)
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              List<ChatMessage> messages = new ArrayList<>();
              for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                  ChatMessage message = doc.toObject(ChatMessage.class);
                  message.setMessageId(doc.getId());
                  messages.add(message);
              }
              liveData.postValue(messages);
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "Error loading paginated messages", e);
              liveData.postValue(new ArrayList<>());
          });

        return liveData;
    }

    public LiveData<List<ChatMessage>> searchMessagesAdvanced(String query) {
        MutableLiveData<List<ChatMessage>> liveData = new MutableLiveData<>();
        
        if (chatId == null || query == null || query.trim().isEmpty()) {
            liveData.postValue(new ArrayList<>());
            return liveData;
        }

        db.collection("chats").document(chatId)
          .collection("messages")
          .whereGreaterThanOrEqualTo("text", query)
          .whereLessThanOrEqualTo("text", query + "\uf8ff")
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              List<ChatMessage> results = new ArrayList<>();
              for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                  ChatMessage message = doc.toObject(ChatMessage.class);
                  message.setMessageId(doc.getId());
                  if (message.getText().toLowerCase().contains(query.toLowerCase())) {
                      results.add(message);
                  }
              }
              liveData.postValue(results);
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "Advanced search failed", e);
              liveData.postValue(new ArrayList<>());
          });

        return liveData;
    }

    public LiveData<List<ChatMessage>> getMessagesByType(String messageType) {
        MutableLiveData<List<ChatMessage>> liveData = new MutableLiveData<>();
        
        if (chatId == null) {
            liveData.postValue(new ArrayList<>());
            return liveData;
        }

        db.collection("chats").document(chatId)
          .collection("messages")
          .whereEqualTo("messageType", messageType)
          .orderBy("timestamp", Query.Direction.DESCENDING)
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              List<ChatMessage> messages = new ArrayList<>();
              for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                  ChatMessage message = doc.toObject(ChatMessage.class);
                  message.setMessageId(doc.getId());
                  messages.add(message);
              }
              liveData.postValue(messages);
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "Get messages by type failed", e);
              liveData.postValue(new ArrayList<>());
          });

        return liveData;
    }

    public LiveData<List<ChatMessage>> getPinnedMessages() {
        MutableLiveData<List<ChatMessage>> liveData = new MutableLiveData<>();
        
        if (chatId == null) {
            liveData.postValue(new ArrayList<>());
            return liveData;
        }

        db.collection("chats").document(chatId)
          .collection("messages")
          .whereEqualTo("pinned", true)
          .orderBy("timestamp", Query.Direction.DESCENDING)
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              List<ChatMessage> messages = new ArrayList<>();
              for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                  ChatMessage message = doc.toObject(ChatMessage.class);
                  message.setMessageId(doc.getId());
                  messages.add(message);
              }
              liveData.postValue(messages);
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "Get pinned messages failed", e);
              liveData.postValue(new ArrayList<>());
          });

        return liveData;
    }
}
