package com.example.kitchenbrain.navigation;

/**
 * 🎯 NAVIGATION CONTRACT - Interface for Chat Navigation
 * 
 * This interface defines WHAT navigation can do,
 * without specifying HOW it's implemented.
 * 
 * Benefits:
 * - UI components depend on abstraction, not implementation
 * - Easy to test (mock navigator)
 * - Can swap implementation (e.g., different navigation system)
 * - Follows Dependency Inversion Principle
 * 
 * Usage:
 * ChatNavigator navigator = ChatNavigatorProvider.get();
 * navigator.openChat(userId, username);
 */
public interface ChatNavigator {
    
    /**
     * Open chat with another user
     * 
     * @param userId User ID to chat with
     * @param username Username for display/logging
     */
    void openChat(String userId, String username);
    
    /**
     * Open chat with additional context
     * 
     * @param userId User ID to chat with
     * @param username Username for display/logging
     * @param roomId Optional chat room ID (for existing conversations)
     */
    void openChat(String userId, String username, String roomId);
}
