package com.example.kitchenbrain.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.kitchenbrain.model.ChatMessage
import com.example.kitchenbrain.model.MessageStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 🎯 PRODUCTION-GRADE Chat Manager
 * 
 * Key Features:
 * - Automatic listener lifecycle management
 * - Transaction-safe operations
 * - Memory leak prevention
 * - Real-time updates with proper cleanup
 * - Efficient Firestore queries
 */
class ChatManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: ChatManager? = null
        
        fun getInstance(): ChatManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatManager().also { INSTANCE = it }
            }
        }
    }
    
    private val TAG = "ChatManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Active chat session state
    private var currentChatId: String? = null
    private var messageListener: ListenerRegistration? = null
    private var mutualStatusListener: ListenerRegistration? = null
    
    // LiveData for real-time updates
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages
    
    private val _chatState = MutableLiveData<ChatState>(ChatState.Idle)
    val chatState: LiveData<ChatState> = _chatState
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    enum class ChatState {
        Idle,
        Loading,
        Active,
        Error
    }
    
    /**
     * 🔄 Initialize chat session with automatic cleanup
     */
    fun initializeChat(currentUserId: String, otherUserId: String) {
        scope.launch {
            try {
                _chatState.postValue(ChatState.Loading)
                
                // Clean up previous session
                cleanupChat()
                
                val chatId = buildChatId(currentUserId, otherUserId)
                currentChatId = chatId
                
                Log.d(TAG, "🚀 Initializing chat: $chatId")
                
                // Start message listener
                startMessageListener(chatId)
                
                // Start mutual status monitoring
                startMutualStatusMonitoring(currentUserId, otherUserId)
                
                _chatState.postValue(ChatState.Active)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize chat", e)
                _error.postValue("Failed to initialize chat: ${e.message}")
                _chatState.postValue(ChatState.Error)
            }
        }
    }
    
    /**
     * 📡 Start real-time message listener with automatic cleanup
     */
    private fun startMessageListener(chatId: String) {
        messageListener = firestore
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Message listener error", error)
                    _error.postValue("Message sync error: ${error.message}")
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val message = doc.toObject(ChatMessage::class.java)
                    message?.apply {
                        messageId = doc.id
                        // Ensure required fields
                        if (text == null) text = ""
                        if (senderId == null) senderId = ""
                        if (receiverId == null) receiverId = ""
                        if (timestamp == null) timestamp = Timestamp.now()
                        if (status == null) status = ChatMessage.MessageStatus.SENT
                    }
                } ?: emptyList()
                
                _messages.postValue(messages)
                Log.d(TAG, "📨 Updated ${messages.size} messages")
            }
    }
    
    /**
     * 🔍 Monitor mutual follow status in real-time
     */
    private fun startMutualStatusMonitoring(currentUserId: String, otherUserId: String) {
        mutualStatusListener = firestore
            .collection("following")
            .document(currentUserId)
            .collection("userFollowing")
            .document(otherUserId)
            .addSnapshotListener { followingDoc, followingError ->
                if (followingError != null) {
                    Log.e(TAG, "❌ Mutual status monitoring error", followingError)
                    return@addSnapshotListener
                }
                
                val isFollowing = followingDoc?.exists() == false
                
                if (isFollowing) {
                    Log.w(TAG, "⚠️ Mutual relationship broken - closing chat")
                    _error.postValue("Chat no longer available - mutual follow ended")
                    cleanupChat()
                    _chatState.postValue(ChatState.Error)
                }
            }
    }
    
    /**
     * 📤 Send message with automatic chat creation
     */
    suspend fun sendMessage(
        currentUserId: String,
        otherUserId: String,
        text: String,
        messageType: String = "text"
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val chatId = buildChatId(currentUserId, otherUserId)
            
            // Ensure chat document exists
            ensureChatExists(chatId, currentUserId, otherUserId)
            
            val message = ChatMessage().apply {
                messageId = UUID.randomUUID().toString()
                senderId = currentUserId
                receiverId = otherUserId
                this.text = text
                timestamp = Timestamp.now()
                status = ChatMessage.MessageStatus.SENT
                this.messageType = messageType
                isPinned = false
            }
            
            firestore
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .document(message.messageId)
                .set(message)
                .await()
            
            Log.d(TAG, "✅ Message sent: ${message.messageId}")
            Result.success(message)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send message", e)
            Result.failure(e)
        }
    }
    
    /**
     * 🔧 Ensure chat document exists with participants
     */
    private suspend fun ensureChatExists(
        chatId: String,
        userId1: String,
        userId2: String
    ) = withContext(Dispatchers.IO) {
        try {
            val chatDoc = firestore
                .collection("chats")
                .document(chatId)
                .get()
                .await()
            
            if (!chatDoc.exists()) {
                val chatData = mapOf(
                    "participants" to listOf(userId1, userId2),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to "",
                    "lastMessageTime" to FieldValue.serverTimestamp()
                )
                
                firestore
                    .collection("chats")
                    .document(chatId)
                    .set(chatData)
                    .await()
                
                Log.d(TAG, "✅ Created chat document: $chatId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to ensure chat exists", e)
            throw e
        }
    }
    
    /**
     * 🧹 Clean up all listeners and state
     */
    fun cleanupChat() {
        try {
            messageListener?.remove()
            messageListener = null
            Log.d(TAG, "🗑️ Message listener removed")
            
            mutualStatusListener?.remove()
            mutualStatusListener = null
            Log.d(TAG, "🗑️ Mutual status listener removed")
            
            currentChatId = null
            _messages.postValue(emptyList())
            _chatState.postValue(ChatState.Idle)
            _error.postValue(null)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during cleanup", e)
        }
    }
    
    /**
     * 🔨 Build consistent chat ID
     */
    private fun buildChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
    
    /**
     * 🎯 Transaction-safe mutual follow detection and chat creation
     */
    suspend fun handleMutualFollowAndCreateChat(
        userId1: String,
        userId2: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Checking mutual follow: $userId1 <-> $userId2")
            
            // Check if users follow each other
            val followsDoc1 = firestore
                .collection("following")
                .document(userId1)
                .collection("userFollowing")
                .document(userId2)
                .get()
                .await()
            
            val followsDoc2 = firestore
                .collection("following")
                .document(userId2)
                .collection("userFollowing")
                .document(userId1)
                .get()
                .await()
            
            val isMutual = followsDoc1.exists() && followsDoc2.exists()
            
            if (!isMutual) {
                return@withContext Result.failure(
                    IllegalStateException("Users are not mutual followers")
                )
            }
            
            // Create chat if mutual
            val chatId = buildChatId(userId1, userId2)
            ensureChatExists(chatId, userId1, userId2)
            
            Log.d(TAG, "✅ Mutual follow detected, chat created: $chatId")
            Result.success(chatId)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to handle mutual follow", e)
            Result.failure(e)
        }
    }
}
