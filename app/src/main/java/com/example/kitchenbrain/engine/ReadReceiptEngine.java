package com.example.kitchenbrain.engine;

import android.util.Log;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.repository.ChatRepository;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.List;

/**
 * 🔥 READ RECEIPTS ENGINE - Telegram Style (✔✔ seen)
 * Features: Mark as read on chat open + UI status updates
 */
public class ReadReceiptEngine {
    
    private static final String TAG = "ReadReceiptEngine";
    private final ChatRepository repository;
    private final ReadReceiptListener listener;
    
    public interface ReadReceiptListener {
        void onMessageRead(String messageId);
        void onReadReceiptError(Exception error);
    }
    
    public ReadReceiptEngine(ChatRepository repository, ReadReceiptListener listener) {
        this.repository = repository;
        this.listener = listener;
    }
    
    /**
     * 📖 MARK MESSAGES AS READ - Called when user opens chat
     * Marks all unread messages from other user as read
     */
    public void markMessagesAsRead(String chatId, String currentUserId, String otherUserId) {
        Log.d(TAG, "📖 Marking messages as read for chat: " + chatId);
        
        repository.markMessagesAsRead(chatId, currentUserId, otherUserId, new ChatRepository.FirebaseCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Messages marked as read successfully");
            }
            
            @Override
            public void onFailure(Exception error) {
                Log.e(TAG, "❌ Failed to mark messages as read", error);
                if (listener != null) {
                    listener.onReadReceiptError(error);
                }
            }
        });
    }
    
    /**
     * 👂 LISTEN TO READ RECEIPT UPDATES - Real-time status changes
     */
    public void listenToReadReceipts(String chatId, String currentUserId) {
        Log.d(TAG, "👂 Starting read receipts listener for chat: " + chatId);
        
        repository.listenToReadReceipts(chatId, currentUserId, new ChatRepository.ReadReceiptListener() {
            @Override
            public void onMessageReadStatusChanged(String messageId, boolean isRead) {
                Log.d(TAG, "📖 Message read status changed: " + messageId + " = " + isRead);
                
                if (isRead && listener != null) {
                    listener.onMessageRead(messageId);
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "❌ Read receipts listener error", error);
                if (listener != null) {
                    listener.onReadReceiptError(error);
                }
            }
        });
    }
    
    /**
     * 📊 GET READ STATUS FOR UI - Returns appropriate status text
     */
    public String getReadStatusText(ChatMessage message, String currentUserId) {
        if (message.getSenderId().equals(currentUserId)) {
            // My message - show read status
            if (message.isRead()) {
                return "✔✔";  // Double check for read
            } else {
                return "✔";   // Single check for sent
            }
        } else {
            // Other message - no read status needed
            return "";
        }
    }
    
    /**
     * 🎨 GET READ STATUS COLOR - Returns appropriate color for UI
     */
    public int getReadStatusColor(ChatMessage message, String currentUserId) {
        if (message.getSenderId().equals(currentUserId)) {
            if (message.isRead()) {
                return 0xFF2196F3;  // Blue for read
            } else {
                return 0xB3FFFFFF;  // White for sent
            }
        } else {
            return 0x00000000;  // Transparent for other messages
        }
    }
    
    /**
     * 📊 GET READ STATUS DESCRIPTION - For accessibility
     */
    public String getReadStatusDescription(ChatMessage message, String currentUserId) {
        if (message.getSenderId().equals(currentUserId)) {
            if (message.isRead()) {
                return "Message read";
            } else {
                return "Message sent";
            }
        } else {
            return "Message from " + message.getSenderId();
        }
    }
}
