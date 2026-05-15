package com.example.kitchenbrain.util;

import android.os.Looper;
import android.util.Log;

/**
 * Performance Monitor
 * 
 * Purpose:
 * - Track execution time of critical operations
 * - Detect main thread blocking
 * - Identify performance bottlenecks
 * 
 * Usage:
 * ```java
 * long start = PerformanceMonitor.start("OperationName");
 * // ... do work ...
 * PerformanceMonitor.end(start, "OperationName");
 * ```
 */
public class PerformanceMonitor {
    
    private static final String TAG = "PerfMonitor";
    private static final long MAIN_THREAD_WARN_THRESHOLD_MS = 100; // Warn if >100ms on main thread
    private static final boolean ENABLED = true; // Set to false to disable in production
    
    /**
     * Start timing an operation
     * @return Start time in nanoseconds
     */
    public static long start(String operationName) {
        if (!ENABLED) return 0;
        
        boolean isMainThread = Looper.myLooper() == Looper.getMainLooper();
        Log.d(TAG, "▶️ START: " + operationName + 
                  (isMainThread ? " [MAIN THREAD]" : " [BG THREAD]"));
        
        return System.nanoTime();
    }
    
    /**
     * End timing an operation and log results
     * @param startTime Start time from start()
     * @param operationName Operation name
     */
    public static void end(long startTime, String operationName) {
        end(startTime, operationName, null);
    }
    
    /**
     * End timing an operation and log results with custom threshold
     * @param startTime Start time from start()
     * @param operationName Operation name
     * @param customThresholdMs Custom warning threshold in milliseconds
     */
    public static void end(long startTime, String operationName, Long customThresholdMs) {
        if (!ENABLED || startTime == 0) return;
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        
        boolean isMainThread = Looper.myLooper() == Looper.getMainLooper();
        long threshold = customThresholdMs != null ? customThresholdMs : MAIN_THREAD_WARN_THRESHOLD_MS;
        
        String message = String.format("⏱️ END: %s - %d ms%s", 
            operationName, 
            durationMs,
            isMainThread ? " [MAIN THREAD]" : " [BG THREAD]");
        
        if (isMainThread && durationMs > threshold) {
            // WARNING: Operation took too long on main thread!
            Log.w(TAG, "⚠️ SLOW OPERATION: " + message);
            Log.w(TAG, "   Consider moving this to a background thread!");
        } else {
            Log.d(TAG, message);
        }
    }
    
    /**
     * Check if currently running on main thread
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    
    /**
     * Assert that we're NOT on main thread (throws if on main thread)
     * Use this in methods that should only run on background threads
     */
    public static void assertNotMainThread() {
        assertNotMainThread("Operation must not run on main thread");
    }
    
    /**
     * Assert that we're NOT on main thread with custom message
     */
    public static void assertNotMainThread(String message) {
        if (isMainThread()) {
            throw new IllegalStateException("⚠️ MAIN THREAD VIOLATION: " + message);
        }
    }
    
    /**
     * Assert that we ARE on main thread (throws if on background thread)
     * Use this for UI update methods
     */
    public static void assertMainThread() {
        assertMainThread("UI updates must run on main thread");
    }
    
    /**
     * Assert that we ARE on main thread with custom message
     */
    public static void assertMainThread(String message) {
        if (!isMainThread()) {
            throw new IllegalStateException("⚠️ WRONG THREAD: " + message);
        }
    }
    
    /**
     * Log a checkpoint during a long operation
     * Useful for tracking progress
     */
    public static void checkpoint(String checkpointName) {
        if (!ENABLED) return;
        
        Log.d(TAG, "📍 CHECKPOINT: " + checkpointName);
    }
}
