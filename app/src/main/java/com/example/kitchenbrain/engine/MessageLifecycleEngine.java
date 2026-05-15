package com.example.kitchenbrain.engine;

import android.util.Log;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.repository.ChatRepository;
import java.util.UUID;

/**
 * 🔥 MESSAGE LIFECYCLE ENGINE - Production Level
 * Handles: SENDING → SENT → FAILED pipeline with optimistic UI
 */
public class MessageLifecycleEngine {
    
    private static final String TAG = "MessageEngine";
    private final ChatRepository repository;
    private final MessageLifecycleListener listener;
    
    public interface MessageLifecycleListener {
        void onMessageAdded(ChatMessage message);  // Optimistic UI
        void onMessageUpdated(ChatMessage message);  // Status change
        void onMessageFailed(ChatMessage message);  // Failed state
    }
    
    public MessageLifecycleEngine(ChatRepository repository, MessageLifecycleListener listener) {
        this.repository = repository;
        this.listener = listener;
    }
    
    /**
     * 🚀 CORE SEND MESSAGE - Optimistic UI + Firebase Sync
     */
    public void sendMessage(String text, String chatId, String senderId, String receiverId) {
        String messageId = UUID.randomUUID().toString();
        ChatMessage message = new ChatMessage(
            messageId,
            senderId,
            receiverId,
            text,
            com.google.firebase.Timestamp.now(),
            ChatMessage.MessageStatus.SENDING
        );
        performSend(chatId, message);
    }

    /**
     * 🎙️ SEND COMPLEX MESSAGE (Voice, Image, etc.)
     */
    public void sendComplexMessage(String chatId, ChatMessage message) {
        performSend(chatId, message);
    }

    private void performSend(String chatId, ChatMessage message) {
        Log.d(TAG, "📡 Sending message to Firebase: " + message.getMessageId());
        
        if (listener != null) {
            listener.onMessageAdded(message);
        }
        
        repository.sendMessage(chatId, message, new ChatRepository.FirebaseCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Firebase success - updating to SENT");
                message.setStatus(ChatMessage.MessageStatus.SENT);
                if (listener != null) {
                    listener.onMessageUpdated(message);
                }
            }
            
            @Override
            public void onFailure(Exception error) {
                Log.e(TAG, "❌ Firebase failed - updating to FAILED", error);
                message.setStatus(ChatMessage.MessageStatus.FAILED);
                if (listener != null) {
                    listener.onMessageFailed(message);
                }
            }
        });
    }
    
    public void retryMessage(ChatMessage failedMessage, String chatId) {
        failedMessage.setStatus(ChatMessage.MessageStatus.SENDING);
        if (listener != null) listener.onMessageUpdated(failedMessage);
        
        repository.sendMessage(chatId, failedMessage, new ChatRepository.FirebaseCallback() {
            @Override
            public void onSuccess() {
                failedMessage.setStatus(ChatMessage.MessageStatus.SENT);
                if (listener != null) listener.onMessageUpdated(failedMessage);
            }
            
            @Override
            public void onFailure(Exception error) {
                failedMessage.setStatus(ChatMessage.MessageStatus.FAILED);
                if (listener != null) listener.onMessageFailed(failedMessage);
            }
        });
    }
    
    public void editMessage(String chatId, String messageId, String newText) {
        repository.editMessage(chatId, messageId, newText, new ChatRepository.FirebaseCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(Exception error) {}
        });
    }
    
    public void deleteMessage(String chatId, String messageId) {
        repository.deleteMessage(chatId, messageId, new ChatRepository.FirebaseCallback() {
            @Override public void onSuccess() {}
            @Override public void onFailure(Exception error) {}
        });
    }
}
