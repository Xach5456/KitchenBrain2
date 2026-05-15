package com.example.kitchenbrain;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Premium ChatViewModel with Complete Business Logic
 * Clean MVVM architecture for chat system
 */
public class ChatViewModelPremium extends ViewModel {
    
    private static final String TAG = "ChatViewModelPremium";
    private final ChatRepositoryPremium repository;
    private final FirebaseAuth mAuth;
    
    // UI State
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.LOADING);
    private final MutableLiveData<List<ChatMessage>> messagesList = new MutableLiveData<>();
    private final MutableLiveData<String> chatId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOtherUserOnline = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Data
    private List<ChatMessage> allMessages = new ArrayList<>();
    private ListenerRegistration messagesListener;
    private ListenerRegistration onlineStatusListener;
    
    // Typing indicator
    private boolean isTyping = false;
    private long typingStartTime = 0;
    
    public enum UiState {
        LOADING,
        SUCCESS,
        EMPTY,
        ERROR
    }
    
    public ChatViewModelPremium() {
        this.repository = new ChatRepositoryPremium();
        this.mAuth = FirebaseAuth.getInstance();
    }
    
    // ========== LiveData Observers ==========
    
    public LiveData<UiState> getUiState() { return uiState; }
    public LiveData<List<ChatMessage>> getMessagesList() { return messagesList; }
    public LiveData<String> getChatId() { return chatId; }
    public LiveData<Boolean> isOtherUserOnline() { return isOtherUserOnline; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    
    // ========== Core Functions ==========
    
    /**
     * Initialize chat with another user
     */
    public void initializeChat(String otherUserId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            uiState.setValue(UiState.ERROR);
            errorMessage.setValue("User not logged in");
            return;
        }
        
        uiState.setValue(UiState.LOADING);
        
        // Get or create chat room
        repository.getOrCreateChatRoom(currentUserId, otherUserId, new ChatRepositoryPremium.ChatRoomCallback() {
            @Override
            public void onSuccess(String roomId) {
                chatId.postValue(roomId);
                setupMessagesListener(roomId);
                setupOnlineStatusListener(otherUserId);
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error initializing chat", e);
                errorMessage.postValue("Failed to initialize chat: " + e.getMessage());
                uiState.postValue(UiState.ERROR);
            }
        });
    }
    
    /**
     * Send a message
     */
    public void sendMessage(String text, String receiverId, MessageSendCallback callback) {
        String currentUserId = getCurrentUserId();
        String currentChatId = chatId.getValue();
        
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }
        
        if (currentChatId == null) {
            callback.onError("Chat not initialized");
            return;
        }
        
        if (text == null || text.trim().isEmpty()) {
            callback.onError("Message cannot be empty");
            return;
        }
        
        // Reset typing state
        resetTypingIndicator(currentChatId, currentUserId);
        
        // Send message with callback
        repository.sendMessage(currentChatId, currentUserId, receiverId, text, new ChatRepositoryPremium.MessageCallback() {
            @Override
            public void onSuccess(String messageId) {
                Log.d(TAG, "Message sent successfully: " + messageId);
                if (callback != null) {
                    callback.onSuccess(messageId);
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error sending message", e);
                if (callback != null) {
                    callback.onError("Failed to send message: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Mark all messages from other user as read
     */
    public void markMessagesAsRead(String otherUserId) {
        String currentChatId = chatId.getValue();
        if (currentChatId == null) return;
        
        repository.markAllMessagesAsRead(currentChatId, otherUserId, new ChatRepositoryPremium.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "All messages marked as read");
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error marking messages as read", e);
            }
        });
    }
    
    /**
     * User started typing
     */
    public void onUserStartedTyping() {
        isTyping = true;
        typingStartTime = System.currentTimeMillis();
        
        String currentChatId = chatId.getValue();
        String currentUserId = getCurrentUserId();
        
        if (currentChatId != null && currentUserId != null) {
            repository.setTypingIndicator(currentChatId, currentUserId, true, new ChatRepositoryPremium.OperationCallback() {
                @Override
                public void onSuccess() {
                    // Typing indicator set successfully
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error setting typing indicator", e);
                }
            });
        }
    }
    
    /**
     * User stopped typing (sent message or cancelled)
     */
    public void onUserStoppedTyping() {
        if (!isTyping) return;
        
        isTyping = false;
        String currentChatId = chatId.getValue();
        String currentUserId = getCurrentUserId();
        
        if (currentChatId != null && currentUserId != null) {
            // Only clear typing indicator if typed for more than 1 second
            if (System.currentTimeMillis() - typingStartTime > 1000) {
                repository.setTypingIndicator(currentChatId, currentUserId, false, new ChatRepositoryPremium.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        // Typing indicator cleared successfully
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error clearing typing indicator", e);
                    }
                });
            }
        }
    }
    
    /**
     * Setup real-time messages listener
     */
    private void setupMessagesListener(String chatId) {
        messagesListener = repository.addMessagesListener(chatId, new ChatRepositoryPremium.MessagesCallback() {
            @Override
            public void onMessagesLoaded(List<ChatMessage> messages) {
                allMessages = messages;
                
                if (messages.isEmpty()) {
                    uiState.setValue(UiState.EMPTY);
                } else {
                    messagesList.setValue(new ArrayList<>(messages));
                    uiState.setValue(UiState.SUCCESS);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading messages: " + error);
                errorMessage.setValue(error);
                uiState.setValue(UiState.ERROR);
            }
        });
    }
    
    /**
     * Setup online status listener for other user
     */
    private void setupOnlineStatusListener(String otherUserId) {
        onlineStatusListener = repository.getUserOnlineStatus(otherUserId, new ChatRepositoryPremium.OnlineStatusCallback() {
            @Override
            public void onOnlineStatusChanged(boolean isOnline) {
                isOtherUserOnline.setValue(isOnline);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting online status: " + error);
            }
        });
    }
    
    /**
     * Reset typing indicator
     */
    private void resetTypingIndicator(String chatId, String userId) {
        repository.setTypingIndicator(chatId, userId, false, new ChatRepositoryPremium.OperationCallback() {
            @Override
            public void onSuccess() {
                // Reset successful
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error resetting typing indicator", e);
            }
        });
    }
    
    /**
     * Format timestamp for display
     */
    public String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        
        long now = System.currentTimeMillis();
        long messageTime = timestamp.toDate().getTime();
        long diff = now - messageTime;
        
        // Less than a minute
        if (diff < 60000) {
            return "now";
        }
        // Less than an hour
        else if (diff < 3600000) {
            long minutes = diff / 60000;
            return minutes + "m ago";
        }
        // Today
        else if (diff < 86400000) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
        // Yesterday
        else if (diff < 172800000) {
            return "Yesterday";
        }
        // This week
        else if (diff < 604800000) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
        // Older
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
    }
    
    /**
     * Get current user ID safely
     */
    private String getCurrentUserId() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }
        return null;
    }
    
    /**
     * Cleanup resources
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        
        // Clear typing indicator
        String currentChatId = chatId.getValue();
        String currentUserId = getCurrentUserId();
        if (currentChatId != null && currentUserId != null) {
            repository.setTypingIndicator(currentChatId, currentUserId, false, new ChatRepositoryPremium.OperationCallback() {
                @Override
                public void onSuccess() {
                    // Cleanup successful
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error clearing typing indicator", e);
                }
            });
        }
        
        // Remove listeners
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        
        if (onlineStatusListener != null) {
            onlineStatusListener.remove();
            onlineStatusListener = null;
        }
        
        // Update online status back to true (user is still in app)
        if (currentUserId != null) {
            repository.updateUserOnlineStatus(currentUserId, true, new ChatRepositoryPremium.OperationCallback() {
                @Override
                public void onSuccess() {
                    // Status updated successfully
                }
                
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error updating online status", e);
                }
            });
        }
    }
    
    // Callback interface
    public interface MessageSendCallback {
        void onSuccess(String messageId);
        void onError(String error);
    }
}
