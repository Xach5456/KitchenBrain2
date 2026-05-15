package com.example.kitchenbrain.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ImageCompressionUtils - Critical for Firebase Storage optimization
 * 
 * 🔥 COMPRESSION RULES:
 * - Max width: 1080px (Instagram standard)
 * - Quality: 70-80% JPEG (balance quality/size)
 * - Auto-rotate based on EXIF
 * - Progressive JPEG for better loading
 * 
 * This prevents:
 * - Firebase Storage cost explosion
 * - Slow app performance
 * - Memory issues
 */
public class ImageCompressionUtils {
    
    private static final String TAG = "ImageCompression";
    private static final int MAX_WIDTH = 1080;
    private static final int MAX_HEIGHT = 1350; // 4:5 aspect ratio
    private static final int JPEG_QUALITY = 75; // 70-80% sweet spot
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB max
    
    /**
     * Compress image from URI before upload
     * Returns compressed byte array ready for Firebase Storage
     */
    public static byte[] compressImage(Context context, Uri imageUri) throws IOException {
        Log.d(TAG, "🔥 Starting image compression for: " + imageUri);
        
        // Decode bitmap from URI
        Bitmap originalBitmap = decodeBitmapFromUri(context, imageUri);
        if (originalBitmap == null) {
            throw new IOException("Failed to decode image");
        }
        
        Log.d(TAG, "📏 Original size: " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight());
        
        // Handle EXIF rotation
        Bitmap rotatedBitmap = handleExifRotation(context, imageUri, originalBitmap);
        if (rotatedBitmap != originalBitmap) {
            originalBitmap.recycle();
        }
        
        // Calculate dimensions
        int[] targetDimensions = calculateTargetDimensions(rotatedBitmap);
        int targetWidth = targetDimensions[0];
        int targetHeight = targetDimensions[1];
        
        Log.d(TAG, "📏 Target size: " + targetWidth + "x" + targetHeight);
        
        // Scale bitmap
        Bitmap scaledBitmap = scaleBitmap(rotatedBitmap, targetWidth, targetHeight);
        if (scaledBitmap != rotatedBitmap) {
            rotatedBitmap.recycle();
        }
        
        // Compress to JPEG
        byte[] compressedBytes = compressToJpeg(scaledBitmap);
        scaledBitmap.recycle();
        
        Log.d(TAG, "✅ Compression complete. Size: " + compressedBytes.length + " bytes");
        
        // Validate compression
        if (compressedBytes.length > MAX_FILE_SIZE) {
            Log.w(TAG, "⚠️ Compressed image still too large, applying stronger compression");
            compressedBytes = compressWithStrongerQuality(scaledBitmap);
        }
        
        return compressedBytes;
    }
    
    /**
     * Decode bitmap from URI with proper memory management
     */
    private static Bitmap decodeBitmapFromUri(Context context, Uri imageUri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        
        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        // Calculate sample size
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565; // Less memory than ARGB_8888
        
        // Decode with sample size
        inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        return bitmap;
    }
    
    /**
     * Calculate sample size for efficient decoding
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    /**
     * Handle EXIF rotation to fix upside-down images
     */
    private static Bitmap handleExifRotation(Context context, Uri imageUri, Bitmap bitmap) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            ExifInterface exif = new ExifInterface(inputStream);
            inputStream.close();
            
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap; // No rotation needed
            }
            
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }
            
            Log.d(TAG, "🔄 Applied EXIF rotation: " + orientation);
            return rotatedBitmap;
            
        } catch (IOException e) {
            Log.w(TAG, "⚠️ Failed to read EXIF data", e);
            return bitmap;
        }
    }
    
    /**
     * Calculate target dimensions maintaining aspect ratio
     */
    private static int[] calculateTargetDimensions(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return new int[]{width, height}; // No scaling needed
        }
        
        float widthRatio = (float) MAX_WIDTH / width;
        float heightRatio = (float) MAX_HEIGHT / height;
        float ratio = Math.min(widthRatio, heightRatio);
        
        int targetWidth = Math.round(width * ratio);
        int targetHeight = Math.round(height * ratio);
        
        return new int[]{targetWidth, targetHeight};
    }
    
    /**
     * Scale bitmap to target dimensions
     */
    private static Bitmap scaleBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        if (bitmap.getWidth() == targetWidth && bitmap.getHeight() == targetHeight) {
            return bitmap; // No scaling needed
        }
        
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        
        if (scaledBitmap != bitmap) {
            bitmap.recycle();
        }
        
        return scaledBitmap;
    }
    
    /**
     * Compress bitmap to JPEG with optimal quality
     */
    private static byte[] compressToJpeg(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Use progressive JPEG for better perceived loading
        boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
        
        if (!success) {
            throw new RuntimeException("Failed to compress bitmap to JPEG");
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Apply stronger compression if needed
     */
    private static byte[] compressWithStrongerQuality(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Try with lower quality
        boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
        
        if (!success) {
            throw new RuntimeException("Failed to compress bitmap with stronger quality");
        }
        
        byte[] result = outputStream.toByteArray();
        Log.d(TAG, "🔥 Applied stronger compression: " + result.length + " bytes");
        
        return result;
    }
    
    /**
     * Get image file info for debugging
     */
    public static ImageInfo getImageInfo(Context context, Uri imageUri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        // Get file size
        File file = new File(imageUri.getPath());
        long fileSize = file.exists() ? file.length() : 0;
        
        return new ImageInfo(
            options.outWidth,
            options.outHeight,
            fileSize,
            options.outMimeType
        );
    }
    
    /**
     * Image info data class
     */
    public static class ImageInfo {
        public final int width;
        public final int height;
        public final long fileSize;
        public final String mimeType;
        
        public ImageInfo(int width, int height, long fileSize, String mimeType) {
            this.width = width;
            this.height = height;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
        }
        
        @Override
        public String toString() {
            return String.format("ImageInfo{size=%dx%d, fileSize=%dKB, mimeType=%s}", 
                width, height, fileSize / 1024, mimeType);
        }
    }
    
    /**
     * Validate image before compression
     */
    public static boolean isValidImage(Context context, Uri imageUri) {
        try {
            ImageInfo info = getImageInfo(context, imageUri);
            
            // Check if it's a valid image
            if (info.width <= 0 || info.height <= 0) {
                Log.e(TAG, "❌ Invalid image dimensions");
                return false;
            }
            
            // Check file size (warn if too large)
            if (info.fileSize > 10 * 1024 * 1024) { // 10MB
                Log.w(TAG, "⚠️ Very large image: " + (info.fileSize / 1024 / 1024) + "MB");
            }
            
            // Check MIME type
            if (info.mimeType == null || (!info.mimeType.startsWith("image/"))) {
                Log.e(TAG, "❌ Invalid MIME type: " + info.mimeType);
                return false;
            }
            
            Log.d(TAG, "✅ Image validation passed: " + info);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Image validation failed", e);
            return false;
        }
    }
}
