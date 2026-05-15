package com.example.kitchenbrain.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sealed navigation event hierarchy for type-safe, exhaustive handling
 * 
 * This is the SINGLE SOURCE OF TRUTH for all navigation events in the app
 */
sealed interface NavigationEvent {
    /**
     * Open chat with another user
     * 
     * @param chatId The unique chat room ID
     */
    data class OpenChat(
        val chatId: String
    ) : NavigationEvent {
        /**
         * Public getter for Java compatibility
         */
        @JvmName("getChatIdForJava")
        fun getChatId(): String = chatId
    }
    
    /**
     * Open chat list
     */
    object OpenChatList : NavigationEvent
    
    /**
     * Navigate to home screen
     */
    object NavigateHome : NavigationEvent
}

/**
 * Extension function to create a SharedFlow for NavigationEvent
 * 
 * Usage:
 * val navigationEvents = MutableSharedFlow<NavigationEvent>(replay = 0, extraBufferCapacity = 1)
 */
fun <T> MutableSharedFlow<T>.asNavigationFlow(): SharedFlow<T> = asSharedFlow()
