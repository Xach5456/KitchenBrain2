package com.example.kitchenbrain.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.example.kitchenbrain.utils.CloudinaryHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MediaRepository {
    private static final String TAG = "MediaRepository";
    private final FirebaseFirestore db;
    private final Context context;

    public MediaRepository(Context context) {
        this.context = context.getApplicationContext();
        db = FirebaseFirestore.getInstance();
        // ✅ CloudinaryHelper initialized in MyApplication - no local instance needed
    }

    public void uploadImage(Uri imageUri, String chatId, String messageId, OnMediaUploadCallback callback) {
        if (imageUri == null || chatId == null || messageId == null) {
            if (callback != null) callback.onError("Invalid parameters");
            return;
        }

        Log.d(TAG, "Uploading image for chat: " + chatId + ", message: " + messageId);
        
        // ✅ Use CloudinaryHelper with unsigned uploads
        CloudinaryHelper.INSTANCE.uploadImage(
            context,
            imageUri,
            url -> {
                Log.d(TAG, "Image uploaded successfully: " + url);
                if (callback != null) callback.onSuccess(url, null);
            },
            error -> {
                Log.e(TAG, "Image upload failed: " + error);
                if (callback != null) callback.onError(error);
            },
            progress -> {
                Log.d(TAG, "Upload progress: " + (int) progress + "%");
            }
        );
    }

    public void uploadVideo(Uri videoUri, String chatId, String messageId, OnMediaUploadCallback callback) {
        if (videoUri == null || chatId == null || messageId == null) {
            if (callback != null) callback.onError("Invalid parameters");
            return;
        }

        Log.d(TAG, "Uploading video for chat: " + chatId + ", message: " + messageId);
        
        // ✅ Upload video using CloudinaryHelper
        CloudinaryHelper.INSTANCE.uploadFile(
            context,
            videoUri,
            url -> {
                Log.d(TAG, "Video uploaded successfully: " + url);
                // For simplicity, return same URL as thumbnail (or generate separately if needed)
                if (callback != null) callback.onSuccess(url, url);
            },
            error -> {
                Log.e(TAG, "Video upload failed: " + error);
                if (callback != null) callback.onError(error);
            },
            progress -> {
                Log.d(TAG, "Upload progress: " + (int) progress + "%");
            }
        );
    }

    public void uploadAudio(byte[] audioData, long duration, String chatId, String messageId, 
                           OnMediaUploadCallback callback) {
        if (audioData == null || chatId == null || messageId == null) {
            if (callback != null) callback.onError("Invalid parameters");
            return;
        }

        Log.d(TAG, "Uploading audio for chat: " + chatId + ", message: " + messageId);
        
        // For audio, we'll need to save to a temp file first then upload
        try {
            File audioFile = File.createTempFile("audio_" + messageId, ".aac", context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(audioFile);
            fos.write(audioData);
            fos.close();
            
            Uri audioUri = Uri.fromFile(audioFile);
            
            // ✅ Upload audio using CloudinaryHelper
            CloudinaryHelper.INSTANCE.uploadFile(
                context,
                audioUri,
                url -> {
                    Log.d(TAG, "Audio uploaded successfully: " + url);
                    if (callback != null) callback.onSuccess(url, null);
                    
                    // Clean up temp file
                    audioFile.delete();
                },
                error -> {
                    Log.e(TAG, "Audio upload failed: " + error);
                    if (callback != null) callback.onError(error);
                    
                    // Clean up temp file
                    audioFile.delete();
                },
                progress -> {
                    Log.d(TAG, "Upload progress: " + (int) progress + "%");
                }
            );
        } catch (IOException e) {
            Log.e(TAG, "Failed to create temp audio file", e);
            if (callback != null) callback.onError(e.getMessage());
        }
    }

    private File createThumbnail(Uri videoUri) {
        try {
            File tempFile = File.createTempFile("thumbnail", ".jpg", context.getCacheDir());
            
            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(context, videoUri);
            
            android.graphics.Bitmap bitmap = retriever.getFrameAtTime(1000000);
            if (bitmap != null) {
                FileOutputStream out = new FileOutputStream(tempFile);
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out);
                out.flush();
                out.close();
                return tempFile;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to create thumbnail", e);
        }
        return null;
    }

    public void deleteMedia(String mediaUrl, OnDeleteCallback callback) {
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            if (callback != null) callback.onSuccess();
            return;
        }

        Log.d(TAG, "Note: Cloudinary media deletion requires admin API. Media URL: " + mediaUrl);
        
        // For now, we'll just log the deletion request
        // In production, you would implement server-side deletion using Cloudinary Admin API
        // Or configure Cloudinary auto-cleanup rules
        
        if (callback != null) callback.onSuccess();
    }

    public interface OnMediaUploadCallback {
        void onSuccess(String url, String thumbnailUrl);
        void onError(String error);
    }

    public interface OnDeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
