package com.example.kitchenbrain.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.kitchenbrain.repository.NewsRepositoryImpl
import java.util.concurrent.TimeUnit

/**
 * NewsSyncWorker - Background synchronization for News
 */
class NewsSyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("NewsSyncWorker", "Starting news sync...")
        val repository = NewsRepositoryImpl(applicationContext)
        
        return try {
            val result = repository.refreshNews(force = true)
            if (result.isSuccess) {
                Log.d("NewsSyncWorker", "Sync successful")
                Result.success()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.w("NewsSyncWorker", "Sync failed: $error")
                if (error.contains("429")) {
                    Result.failure() // Stop retrying on API limit
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e("NewsSyncWorker", "Fatal sync error", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "news_sync_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<NewsSyncWorker>(3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
