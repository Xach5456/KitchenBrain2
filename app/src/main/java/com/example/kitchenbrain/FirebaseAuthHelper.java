package com.example.kitchenbrain;

import android.util.Patterns;

/**
 * Helper class for Firebase authentication operations
 */
public class FirebaseAuthHelper {

    /**
     * Validates email format
     * @param email The email to validate
     * @return true if email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validates password strength
     * @param password The password to validate
     * @return true if password is strong enough, false otherwise
     */
    public static boolean isStrongPassword(String password) {
        // Password should be at least 6 characters
        return password != null && password.length() >= 6;
    }

    /**
     * Sanitizes email by trimming whitespace
     * @param email The email to sanitize
     * @return sanitized email
     */
    public static String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}