package com.example.kitchenbrain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 🎯 ACTIVE SCREEN MANAGER - Global UI state control
 * 
 * This is the SINGLE SOURCE OF TRUTH for active screen state.
 * It enforces that only ONE chat-related screen can be active at a time.
 * 
 * Architecture:
 * ChatListFragment → ActiveScreenManager → ChatNavigatorImpl
 * ChatFragment → ActiveScreenManager → ChatNavigatorImpl
 * 
 * Key responsibilities:
 * - Track current active screen
 * - Prevent competing UI controllers
 * - Enforce single active chat session
 */
object ActiveScreenManager {
    
    /**
     * Current active screen
     */
    private val _activeScreen = MutableStateFlow<Screen>(Screen.ChatList)
    val activeScreen: StateFlow<Screen> = _activeScreen.asStateFlow()
    
    /**
     * Set active screen
     */
    @JvmStatic
    fun setActive(screen: Screen) {
        _activeScreen.value = screen
    }
    
    /**
     * Get current active screen
     */
    @JvmStatic
    fun getActive(): Screen {
        return _activeScreen.value
    }
    
    /**
     * Check if screen is active
     */
    @JvmStatic
    fun isActive(screen: Screen): Boolean {
        return _activeScreen.value == screen
    }
    
    /**
     * Screen enumeration
     */
    enum class Screen {
        ChatList,
        Chat,
        Home,
        Profile
    }
}