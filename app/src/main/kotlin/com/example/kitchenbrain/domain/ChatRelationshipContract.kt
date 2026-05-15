package com.example.kitchenbrain.domain

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * 🎯 CHAT RELATIONSHIP CONTRACT - True Single Source of Truth
 * 
 * КРИТИЧЕСКИЙ ПРИНЦИП:
 * Chat exists IF AND ONLY IF:
 * - chats/{chatId} exists
 * - userChats/{uid1}/{chatId} exists  
 * - userChats/{uid2}/{chatId} exists
 * 
 * Это 3 синхронные сущности, а не одна!
 */
object ChatRelationshipContract {
    
    private const val TAG = "ChatRelationshipContract"
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * 🔍 Проверить Chat Relationship Contract
     * 
     * @return true если chat существует во всех 3 местах
     */
    suspend fun validateChatRelationship(uid1: String, uid2: String): ChatRelationshipResult {
        val chatId = generateChatId(uid1, uid2)
        
        Log.d(TAG, "🔍 Validating chat relationship: $uid1 <-> $uid2 (chatId: $chatId)")
        
        return try {
            // 1. Проверить основной chat document
            val chatDoc = firestore.collection("chats").document(chatId).get().await()
            if (!chatDoc.exists()) {
                Log.w(TAG, "⚠️ Chat document missing: $chatId")
                return ChatRelationshipResult.MissingChatDocument
            }
            
            // 2. Проверить userChats для uid1
            val userChat1Doc = firestore
                .collection("userChats").document(uid1)
                .collection("chats").document(chatId)
                .get().await()
            
            if (!userChat1Doc.exists()) {
                Log.w(TAG, "⚠️ UserChat missing for $uid1: $chatId")
                return ChatRelationshipResult.MissingUserChat1
            }
            
            // 3. Проверить userChats для uid2
            val userChat2Doc = firestore
                .collection("userChats").document(uid2)
                .collection("chats").document(chatId)
                .get().await()
            
            if (!userChat2Doc.exists()) {
                Log.w(TAG, "⚠️ UserChat missing for $uid2: $chatId")
                return ChatRelationshipResult.MissingUserChat2
            }
            
            // 4. Валидировать participants
            val participants = chatDoc.get("participants") as? List<String>
            val participantsSet = participants?.toSet()
            
            if (participantsSet != setOf(uid1, uid2)) {
                Log.e(TAG, "❌ CORRUPTED participants: expected ${setOf(uid1, uid2)}, got $participantsSet")
                return ChatRelationshipResult.CorruptedParticipants
            }
            
            Log.d(TAG, "✅ Chat relationship valid: $chatId")
            ChatRelationshipResult.Valid(chatId)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating chat relationship", e)
            ChatRelationshipResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * 🔧 Восстановить Chat Relationship Contract
     * 
     * Используется когда contract нарушен
     */
    suspend fun repairChatRelationship(uid1: String, uid2: String): RepairResult {
        val chatId = generateChatId(uid1, uid2)
        
        Log.d(TAG, "🔧 Repairing chat relationship: $uid1 <-> $uid2")
        
        return try {
            firestore.runTransaction { transaction ->
                val chatRef = firestore.collection("chats").document(chatId)
                val chatDoc = transaction.get(chatRef)
                
                if (!chatDoc.exists()) {
                    // Создать chat document
                    val chatData = hashMapOf(
                        "participants" to listOf(uid1, uid2),
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "lastMessage" to null,
                        "lastMessageTime" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "active" to true,
                        "version" to 1
                    )
                    
                    transaction.set(chatRef, chatData)
                    Log.d(TAG, "🆕 Created chat document: $chatId")
                }
                
                // Создать/восстановить userChats entries
                ensureUserChatEntries(transaction, uid1, uid2, chatId)
                
                RepairResult.Success(chatId)
            }.await()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to repair chat relationship", e)
            RepairResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * 🔧 Гарантировать создание userChats entries
     */
    private fun ensureUserChatEntries(
        transaction: com.google.firebase.firestore.Transaction,
        uid1: String,
        uid2: String,
        chatId: String
    ) {
        val userChatData1 = hashMapOf(
            "chatId" to chatId,
            "otherUserId" to uid2,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "active" to true
        )
        
        val userChatData2 = hashMapOf(
            "chatId" to chatId,
            "otherUserId" to uid1,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "active" to true
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
        
        Log.d(TAG, "✅ UserChats entries ensured: $chatId")
    }
    
    /**
     * 🔨 Генерировать консистентный chat ID
     */
    private fun generateChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }
}

/**
 * 🎯 Chat Relationship Result
 */
sealed class ChatRelationshipResult {
    data class Valid(val chatId: String) : ChatRelationshipResult()
    object MissingChatDocument : ChatRelationshipResult()
    object MissingUserChat1 : ChatRelationshipResult()
    object MissingUserChat2 : ChatRelationshipResult()
    object CorruptedParticipants : ChatRelationshipResult()
    data class Error(val message: String) : ChatRelationshipResult()
}

/**
 * 🔧 Repair Result
 */
sealed class RepairResult {
    data class Success(val chatId: String) : RepairResult()
    data class Error(val message: String) : RepairResult()
}
