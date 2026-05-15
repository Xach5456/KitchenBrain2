package com.example.kitchenbrain.domain

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * 🎯 SERVER-SIDE AUTHORITY - Leader election через transaction
 * 
 * КРИТИЧЕСКИЕ ПРИНЦИПЫ:
 * - Server-side authority через Firestore transaction
 * - Leader election для предотвращения конкуренции клиентов
 * - Никаких дублированных чатов
 */
object ServerSideAuthority {
    
    private const val TAG = "ServerSideAuthority"
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * 🎯 Server-side chat creation с leader election
     * 
     * Только один клиент может создать chat для данной пары
     */
    suspend fun authoritativeChatCreation(uid1: String, uid2: String): AuthorityResult {
        val chatId = generateChatId(uid1, uid2)
        val pairId = generatePairId(uid1, uid2)
        
        Log.d(TAG, "🎯 Authoritative chat creation: $uid1 <-> $uid2")
        
        return try {
            val result = firestore.runTransaction { transaction ->
                // 1. Попытаться получить leader lock
                val lockRef = firestore.collection("chatCreationLocks").document(pairId)
                val lockDoc = transaction.get(lockRef)
                
                val now = System.currentTimeMillis()
                val lockExpiresAt = lockDoc.getLong("expiresAt") ?: 0L
                
                if (lockDoc.exists() && now < lockExpiresAt) {
                    // Другой клиент уже создаёт chat
                    val lockedBy = lockDoc.getString("lockedBy")
                    Log.w(TAG, "⚠️ Chat creation locked by: $lockedBy")
                    throw IllegalStateException("Chat creation in progress by another client")
                }
                
                // 2. Установить leader lock
                val lockData = hashMapOf(
                    "pairId" to pairId,
                    "lockedBy" to getCurrentUserId(),
                    "lockedAt" to now,
                    "expiresAt" to (now + 30_000L), // 30 секунд
                    "operation" to "chat_creation"
                )
                
                transaction.set(lockRef, lockData)
                
                // 3. Проверить существует ли chat
                val chatRef = firestore.collection("chats").document(chatId)
                val chatDoc = transaction.get(chatRef)
                
                if (chatDoc.exists()) {
                    // Chat уже существует - валидировать
                    val participants = chatDoc.get("participants") as? List<String>
                    val participantsSet = participants?.toSet()
                    
                    if (participantsSet == setOf(uid1, uid2)) {
                        Log.d(TAG, "✅ Chat exists and valid: $chatId")
                        // Освободить lock
                        transaction.delete(lockRef)
                        AuthorityResult.ChatExists(chatId)
                    } else {
                        Log.e(TAG, "❌ Corrupted chat data")
                        transaction.delete(lockRef)
                        AuthorityResult.CorruptedData("Invalid participants")
                    }
                } else {
                    // 4. Создать chat atomically
                    val chatData = hashMapOf(
                        "participants" to listOf(uid1, uid2),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastMessage" to null,
                        "lastMessageTime" to FieldValue.serverTimestamp(),
                        "active" to true,
                        "version" to 1,
                        "createdBy" to getCurrentUserId(),
                        "authority" to "server_side"
                    )
                    
                    transaction.set(chatRef, chatData)
                    
                    // 5. Создать userChats entries
                    createUserChatEntries(transaction, uid1, uid2, chatId)
                    
                    // 6. Освободить lock
                    transaction.delete(lockRef)
                    
                    Log.d(TAG, "✅ Chat created authoritatively: $chatId")
                    AuthorityResult.ChatCreated(chatId)
                }
            }.await()
            
            result
            
        } catch (e: IllegalStateException) {
            // Lock занят другим клиентом
            Log.w(TAG, "⚠️ Chat creation blocked: ${e.message}")
            AuthorityResult.Locked(e.message ?: "Operation in progress")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Authoritative chat creation failed", e)
            AuthorityResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * 🔍 Проверить chat creation lock
     */
    suspend fun isChatCreationLocked(uid1: String, uid2: String): Boolean {
        val pairId = generatePairId(uid1, uid2)
        
        return try {
            val lockDoc = firestore.collection("chatCreationLocks").document(pairId).get().await()
            if (!lockDoc.exists()) return false
            
            val expiresAt = lockDoc.getLong("expiresAt") ?: 0L
            System.currentTimeMillis() < expiresAt
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking lock", e)
            false
        }
    }
    
    /**
     * 🔓 Принудительно освободить lock (для cleanup)
     */
    suspend fun forceReleaseLock(uid1: String, uid2: String) {
        val pairId = generatePairId(uid1, uid2)
        
        try {
            firestore.collection("chatCreationLocks").document(pairId).delete().await()
            Log.d(TAG, "🔓 Force released lock: $pairId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error force releasing lock", e)
        }
    }
    
    /**
     * 🔧 Гарантировать создание userChats entries
     */
    private fun createUserChatEntries(
        transaction: com.google.firebase.firestore.Transaction,
        uid1: String,
        uid2: String,
        chatId: String
    ) {
        val userChatData1 = hashMapOf(
            "chatId" to chatId,
            "otherUserId" to uid2,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "active" to true,
            "authority" to "server_side"
        )
        
        val userChatData2 = hashMapOf(
            "chatId" to chatId,
            "otherUserId" to uid1,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "active" to true,
            "authority" to "server_side"
        )
        
        transaction.set(
            firestore.collection("userChats").document(uid1)
                .collection("chats").document(chatId),
            userChatData1
        )
        
        transaction.set(
            firestore.collection("userChats").document(uid2)
                .collection("chats").document(chatId),
            userChatData2
        )
        
        Log.d(TAG, "✅ UserChats entries created: $chatId")
    }
    
    /**
     * 🔨 Генерировать консистентные ID
     */
    private fun generateChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }
    
    private fun generatePairId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }
    
    /**
     * 👤 Получить текущий user ID
     */
    private fun getCurrentUserId(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid 
            ?: "unknown"
    }
}

/**
 * 🎯 Authority Result
 */
sealed class AuthorityResult {
    data class ChatCreated(val chatId: String) : AuthorityResult()
    data class ChatExists(val chatId: String) : AuthorityResult()
    data class Locked(val message: String) : AuthorityResult()
    data class CorruptedData(val message: String) : AuthorityResult()
    data class Error(val message: String) : AuthorityResult()
}
