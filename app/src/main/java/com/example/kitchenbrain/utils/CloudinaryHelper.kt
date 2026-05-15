package com.example.kitchenbrain.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.policy.TimeWindow
import com.example.kitchenbrain.BuildConfig

/**
 * CloudinaryHelper - Simple and clean media upload helper
 */
object CloudinaryHelper {

    private const val TAG = "CloudinaryHelper"
    
    private const val CLOUD_NAME = "di5j6z7i3"
    private const val UPLOAD_PRESET = "kitchenbrain"
    
    @Volatile
    private var isInitialized = false
    
    fun interface UploadSuccessCallback {
        fun onSuccess(url: String)
    }
    
    fun interface UploadErrorCallback {
        fun onError(error: String)
    }
    
    fun interface UploadProgressCallback {
        fun onProgress(progress: Double)
    }

    @JvmStatic
    fun init(context: Context) {
        if (isInitialized) return
        
        val config = mutableMapOf<String, Any>(
            "cloud_name" to CLOUD_NAME,
            "api_key" to BuildConfig.CLOUDINARY_API_KEY
        )
        
        try {
            MediaManager.init(context, config)
            isInitialized = true
            Log.d(TAG, "Cloudinary initialized successfully")
        } catch (e: Exception) {
            if (e.message?.contains("already initialized") == true) {
                isInitialized = true
            } else {
                Log.e(TAG, "Cloudinary init failed: ${e.message}")
            }
        }
    }

    /**
     * Standard upload method with ResourceType.
     */
    @JvmStatic
    @JvmOverloads
    fun uploadFile(
        context: Context,
        fileUri: Uri?,
        resourceType: String,
        onSuccess: UploadSuccessCallback,
        onError: UploadErrorCallback,
        onProgress: UploadProgressCallback? = null
    ) {
        if (fileUri == null) {
            onError.onError("File URI is null")
            return
        }

        if (!isInitialized) init(context)
        
        try {
            MediaManager.get()
                .upload(fileUri)
                .unsigned(UPLOAD_PRESET)
                .option("resource_type", resourceType)
                .constrain(TimeWindow.immediate())
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = if (totalBytes > 0) (bytes * 100.0 / totalBytes) else 0.0
                        onProgress?.onProgress(progress)
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String ?: ""
                        onSuccess.onSuccess(url)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        val desc = error.description ?: "Unknown error"
                        if (desc.contains("whitelist") || desc.contains("preset")) {
                            onError.onError("Dashboard Configuration Error: Preset '$UPLOAD_PRESET' must be set to 'Unsigned' mode in Cloudinary Settings.")
                        } else {
                            onError.onError(desc)
                        }
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        } catch (e: Exception) {
            onError.onError("Upload failed: ${e.message}")
        }
    }

    /**
     * Overload for Java callers without resourceType (defaults to "auto").
     */
    @JvmStatic
    @JvmOverloads
    fun uploadFile(
        context: Context,
        fileUri: Uri?,
        onSuccess: UploadSuccessCallback,
        onError: UploadErrorCallback,
        onProgress: UploadProgressCallback? = null
    ) {
        uploadFile(context, fileUri, "auto", onSuccess, onError, onProgress)
    }

    /**
     * Convenience method for images.
     */
    @JvmStatic
    @JvmOverloads
    fun uploadImage(
        context: Context,
        imageUri: Uri?,
        onSuccess: UploadSuccessCallback,
        onError: UploadErrorCallback,
        onProgress: UploadProgressCallback? = null
    ) {
        uploadFile(context, imageUri, "image", onSuccess, onError, onProgress)
    }
}
