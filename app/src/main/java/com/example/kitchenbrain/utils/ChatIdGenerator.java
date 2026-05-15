package com.example.kitchenbrain.utils;

import android.util.Log;

/**
 * 🔥 CENTRALIZED ChatId Generator
 * 
 * CRITICAL: This is the ONLY place where chatId should be generated
 * Prevents duplicate chats from inconsistent ID generation
 * 
 * Usage:
 * String chatId = ChatIdGenerator.generate(userA, userB);
 * 
 * ⚠️ NEVER write chatId generation manually anywhere else!
 */
public class ChatIdGenerator {
    
    private static final String TAG = "ChatIdGenerator";
    
    /**
     * 🔥 Generate consistent chatId (alphabetically sorted)
     * 
     * This ensures:
     * - userA + userB always produces the SAME chatId
     * - userB + userA produces the SAME chatId
     * - Prevents duplicate chats
     * 
     * @param userA First user ID
     * @param userB Second user ID
     * @return Consistent chatId (e.g., "abc123_def456")
     */
    public static String generate(String userA, String userB) {
        if (userA == null || userB == null) {
            Log.e(TAG, "❌ Cannot generate chatId - null user IDs");
            throw new IllegalArgumentException("User IDs cannot be null");
        }
        
        if (userA.equals(userB)) {
            Log.e(TAG, "❌ Cannot generate chatId - same user: " + userA);
            throw new IllegalArgumentException("Cannot create chat with yourself");
        }
        
        // 🔥 Alphabetically sort to ensure consistency
        String chatId = userA.compareTo(userB) < 0 
            ? userA + "_" + userB 
            : userB + "_" + userA;
        
        Log.d(TAG, "🔗 Generated chatId: " + chatId + " (from " + userA + ", " + userB + ")");
        
        return chatId;
    }
    
    /**
     * 🔥 Extract other user ID from chatId
     * 
     * @param chatId Chat ID (e.g., "userA_userB")
     * @param currentUserId Current user's ID
     * @return Other user's ID
     */
    public static String getOtherUserId(String chatId, String currentUserId) {
        if (chatId == null || currentUserId == null) {
            Log.e(TAG, "❌ Cannot extract otherUserId - null parameters");
            return null;
        }
        
        String[] parts = chatId.split("_");
        if (parts.length != 2) {
            Log.e(TAG, "❌ Invalid chatId format: " + chatId);
            return null;
        }
        
        String userA = parts[0];
        String userB = parts[1];
        
        if (userA.equals(currentUserId)) {
            return userB;
        } else if (userB.equals(currentUserId)) {
            return userA;
        } else {
            Log.e(TAG, "❌ Current user not in chatId: " + currentUserId + " not in " + chatId);
            return null;
        }
    }
    
    /**
     * 🔥 Validate chatId format
     * 
     * @param chatId Chat ID to validate
     * @return true if valid format
     */
    public static boolean isValidFormat(String chatId) {
        if (chatId == null || chatId.isEmpty()) {
            return false;
        }
        
        String[] parts = chatId.split("_");
        return parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty();
    }
}
