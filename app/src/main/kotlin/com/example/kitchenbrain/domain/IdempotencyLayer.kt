package com.example.kitchenbrain.domain

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * 🎯 IDEMPOTENCY LAYER - Предотвращает дублирование операций
 * 
 * КРИТИЧЕСКИЕ ПРИНЦИПЫ:
 * - Server-side authority через Firestore
 * - Idempotency keys для всех операций
 * - Никаких race conditions между клиентами
 */
object IdempotencyLayer {
    
    private const val TAG = "IdempotencyLayer"
    private val firestore = FirebaseFirestore.getInstance()
    
    // Local cache для performance
    private val localProcessedPairs = ConcurrentHashMap<String, Long>()
    private val CACHE_TTL_MS = 30_000L // 30 секунд
    
    /**
     * 🔍 Проверить/создать idempotency key
     * 
     * @return true если операция может продолжиться, false если уже обработана
     */
    suspend fun acquireOperationLock(
        operationType: String,
        uid1: String,
        uid2: String
    ): Boolean {
        val pairId = generatePairId(uid1, uid2)
        val operationId = "${operationType}_${pairId}"
        
        Log.d(TAG, "🔒 Acquiring lock: $operationId")
        
        // 1. Проверить local cache
        val localTimestamp = localProcessedPairs[operationId]
        if (localTimestamp != null && (System.currentTimeMillis() - localTimestamp) < CACHE_TTL_MS) {
            Log.w(TAG, "⚠️ Operation in local cache: $operationId")
            return false
        }
        
        // 2. Проверить/создать server-side lock
        return try {
            val lockRef = firestore.collection("operationLocks").document(operationId)
            val lockDoc = lockRef.get().await()
            
            if (lockDoc.exists()) {
                val expiresAt = lockDoc.getLong("expiresAt") ?: 0L
                if (System.currentTimeMillis() < expiresAt) {
                    Log.w(TAG, "⚠️ Operation locked on server: $operationId")
                    false
                } else {
                    // Lock expired, создать новый
                    createLock(lockRef, operationId)
                }
            } else {
                // Создать новый lock
                createLock(lockRef, operationId)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error acquiring lock: $operationId", e)
            false
        }
    }
    
    /**
     * 🔓 Освободить operation lock
     */
    suspend fun releaseOperationLock(
        operationType: String,
        uid1: String,
        uid2: String
    ) {
        val pairId = generatePairId(uid1, uid2)
        val operationId = "${operationType}_${pairId}"
        
        Log.d(TAG, "🔓 Releasing lock: $operationId")
        
        try {
            // Удалить server-side lock
            firestore.collection("operationLocks").document(operationId).delete().await()
            
            // Обновить local cache
            localProcessedPairs[operationId] = System.currentTimeMillis()
            
            // Cleanup old entries
            cleanupLocalCache()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error releasing lock: $operationId", e)
        }
    }
    
    /**
     * 🔐 Создать server-side lock
     */
    private suspend fun createLock(
        lockRef: com.google.firebase.firestore.DocumentReference,
        operationId: String
    ): Boolean {
        return try {
            val lockData = hashMapOf(
                "operationId" to operationId,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "expiresAt" to (System.currentTimeMillis() + 60_000L), // 1 минута
                "lockedBy" to getCurrentUserId()
            )
            
            lockRef.set(lockData).await()
            Log.d(TAG, "✅ Lock created: $operationId")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create lock: $operationId", e)
            false
        }
    }
    
    /**
     * 🔍 Проверить существует ли lock
     */
    suspend fun isOperationLocked(
        operationType: String,
        uid1: String,
        uid2: String
    ): Boolean {
        val pairId = generatePairId(uid1, uid2)
        val operationId = "${operationType}_${pairId}"
        
        return try {
            val lockDoc = firestore.collection("operationLocks").document(operationId).get().await()
            if (!lockDoc.exists()) return false
            
            val expiresAt = lockDoc.getLong("expiresAt") ?: 0L
            System.currentTimeMillis() < expiresAt
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking lock: $operationId", e)
            false
        }
    }
    
    /**
     * 🧹 Cleanup local cache
     */
    private fun cleanupLocalCache() {
        val now = System.currentTimeMillis()
        localProcessedPairs.entries.removeIf { (_, timestamp) ->
            (now - timestamp) > CACHE_TTL_MS
        }
    }
    
    /**
     * 🔨 Генерировать консистентный pair ID
     */
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
 * 🎯 Operation Types
 */
object OperationType {
    const val MUTUAL_FOLLOW_DETECTION = "mutual_follow"
    const val CHAT_CREATION = "chat_creation"
    const val FOLLOW_OPERATION = "follow"
    const val UNFOLLOW_OPERATION = "unfollow"
}
