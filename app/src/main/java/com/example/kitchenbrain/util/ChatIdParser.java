package com.example.kitchenbrain.util;

import android.util.Log;

/**
 * Utility class for parsing chat room IDs and extracting user information.
 * 
 * Chat room IDs are formatted as "user1_user2" where user1 < user2 lexicographically.
 * This ensures consistent room ID generation across different clients.
 */
public class ChatIdParser {
    private static final String TAG = "ChatIdParser";
    
    /**
     * Extract the other user ID from a chat room ID.
     * 
     * @param chatId The chat room ID in format "user1_user2"
     * @param currentUserId The current user's ID
     * @return The other user's ID, or null if parsing fails
     */
    public static String extractOtherUserId(String chatId, String currentUserId) {
        if (chatId == null || currentUserId == null) {
            Log.w(TAG, "Cannot extract other user ID: chatId or currentUserId is null");
            return null;
        }
        
        String[] parts = chatId.split("_");
        if (parts.length != 2) {
            Log.w(TAG, "Invalid chatId format: " + chatId + " - expected format: user1_user2");
            return null;
        }
        
        String userId1 = parts[0];
        String userId2 = parts[1];
        
        // Return the user ID that is not the current user
        if (userId1.equals(currentUserId)) {
            return userId2;
        } else if (userId2.equals(currentUserId)) {
            return userId1;
        } else {
            Log.w(TAG, "Current user ID not found in chatId: " + chatId + " (current: " + currentUserId + ")");
            return null;
        }
    }
    
    /**
     * Validate if a chat ID is properly formatted.
     * 
     * @param chatId The chat room ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidChatId(String chatId) {
        if (chatId == null || chatId.isEmpty()) {
            return false;
        }
        
        String[] parts = chatId.split("_");
        return parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty();
    }
}
