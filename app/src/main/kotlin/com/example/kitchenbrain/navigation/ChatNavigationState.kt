package com.example.kitchenbrain.navigation

/**
 * 🎯 NAVIGATION STATE MACHINE - Single source of truth for chat navigation
 * 
 * This is the SINGLE SOURCE OF TRUTH for all navigation state.
 * It ensures deterministic, lifecycle-aware navigation behavior.
 * 
 * Architecture:
 * Navigation events → ChatNavigatorImpl → ChatNavigationState → FragmentManager
 * 
 * Key properties:
 * - activeChatId: The currently active chat ID (null if no chat is active)
 * - navigationInProgress: True when navigation is actively happening
 */
object ChatNavigationState {
    /**
     * The currently active chat ID, or null if no chat is active
     */
    var activeChatId: String? = null
    
    /**
     * True when navigation is actively happening
     */
    var navigationInProgress: Boolean = false
    
    /**
     * Reset navigation state
     */
    fun reset() {
        activeChatId = null
        navigationInProgress = false
    }
    
    /**
     * Start navigation for a specific chat
     */
    fun startNavigation(chatKey: String) {
        activeChatId = chatKey
        navigationInProgress = true
    }
    
    /**
     * Complete navigation for the current chat
     */
    fun completeNavigation() {
        // Only reset if we're completing the current navigation
        if (navigationInProgress && activeChatId != null) {
            navigationInProgress = false
        }
    }
}