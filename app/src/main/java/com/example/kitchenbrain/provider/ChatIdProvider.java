package com.example.kitchenbrain.provider;

import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 🔥 CHAT ID PROVIDER - Single Source of Truth for Chat Identity
 * Features: Deterministic chat IDs + consistency across all layers
 */
public class ChatIdProvider {
    
    private static final String TAG = "ChatIdProvider";
    
    /**
     * 🎯 GET CHAT ID - Deterministic generation for 1-on-1 chats
     * Ensures same chatId regardless of who initiates the conversation
     */
    public static String getChatId(String userA, String userB) {
        if (userA == null || userB == null) {
            Log.e(TAG, "❌ Cannot generate chatId: null user IDs");
            throw new IllegalArgumentException("User IDs cannot be null");
        }
        
        if (userA.equals(userB)) {
            Log.e(TAG, "❌ Cannot generate chatId: same user ID");
            throw new IllegalArgumentException("Cannot create chat with same user");
        }
        
        // Sort user IDs to ensure consistent chatId regardless of who initiates
        String sortedUserA = userA.compareTo(userB) > 0 ? userB : userA;
        String sortedUserB = userA.compareTo(userB) > 0 ? userA : userB;
        
        // Generate deterministic chatId
        String chatId = sortedUserA + "_" + sortedUserB;
        
        Log.d(TAG, "🔑 Generated chatId: " + chatId + " for users: " + userA + " & " + userB);
        return chatId;
    }
    
    /**
     * 🔐 HASHED CHAT ID - For additional privacy (optional)
     * Creates a hash-based chatId instead of plain concatenation
     */
    public static String getHashedChatId(String userA, String userB) {
        String baseChatId = getChatId(userA, userB);
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(baseChatId.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String hashedChatId = hexString.toString().substring(0, 16); // Use first 16 chars
            Log.d(TAG, "🔐 Generated hashed chatId: " + hashedChatId);
            return hashedChatId;
            
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "❌ Hash algorithm not available, falling back to plain chatId", e);
            return baseChatId;
        }
    }
    
    /**
     * 📊 VALIDATE CHAT ID - Check if chatId follows expected format
     */
    public static boolean isValidChatId(String chatId) {
        if (chatId == null || chatId.isEmpty()) {
            return false;
        }
        
        // Basic validation: should contain exactly one underscore and two user IDs
        String[] parts = chatId.split("_");
        if (parts.length != 2) {
            return false;
        }
        
        // Check if both parts are non-empty and look like user IDs
        for (String part : parts) {
            if (part.isEmpty() || part.length() < 10) { // Basic user ID length check
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 🔍 EXTRACT USER IDS - Get user IDs from existing chatId
     */
    public static String[] extractUserIds(String chatId) {
        if (!isValidChatId(chatId)) {
            Log.e(TAG, "❌ Invalid chatId format: " + chatId);
            return null;
        }
        
        String[] userIds = chatId.split("_");
        Log.d(TAG, "🔍 Extracted user IDs from chatId: " + userIds[0] + " & " + userIds[1]);
        return userIds;
    }
    
    /**
     * 🤝 ARE USERS IN CHAT - Check if both users belong to the same chat
     */
    public static boolean areUsersInChat(String chatId, String userA, String userB) {
        if (!isValidChatId(chatId)) {
            return false;
        }
        
        String[] userIds = extractUserIds(chatId);
        if (userIds == null) {
            return false;
        }
        
        // Check if both users are in the chat (order doesn't matter)
        boolean userAInChat = userIds[0].equals(userA) || userIds[1].equals(userA);
        boolean userBInChat = userIds[0].equals(userB) || userIds[1].equals(userB);
        
        boolean result = userAInChat && userBInChat;
        Log.d(TAG, "🤝 Users " + userA + " & " + userB + " in chat " + chatId + ": " + result);
        return result;
    }
    
    /**
     * 📋 GET CHAT METADATA - Additional info about chat
     */
    public static ChatMetadata getChatMetadata(String chatId) {
        if (!isValidChatId(chatId)) {
            return null;
        }
        
        String[] userIds = extractUserIds(chatId);
        return new ChatMetadata(chatId, userIds[0], userIds[1]);
    }
    
    /**
     * 📊 CHAT METADATA CLASS
     */
    public static class ChatMetadata {
        public final String chatId;
        public final String userA;
        public final String userB;
        
        public ChatMetadata(String chatId, String userA, String userB) {
            this.chatId = chatId;
            this.userA = userA;
            this.userB = userB;
        }
        
        @Override
        public String toString() {
            return "ChatMetadata{" +
                    "chatId='" + chatId + '\'' +
                    ", userA='" + userA + '\'' +
                    ", userB='" + userB + '\'' +
                    '}';
        }
    }
}
