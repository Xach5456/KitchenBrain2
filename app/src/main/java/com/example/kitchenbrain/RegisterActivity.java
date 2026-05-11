package com.example.kitchenbrain;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button registerButton;
    private TextView textLoginLink;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Block back button using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing - block back button
            }
        });

        // Initialize UI elements
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        registerButton = findViewById(R.id.registerButton);
        textLoginLink = findViewById(R.id.textLoginLink);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Make progress bar invisible initially
        if(progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // Registration button handler - lambda expression
        registerButton.setOnClickListener(v -> registerUser());

        // Login link handler - lambda expression
        textLoginLink.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        // Sanitize email
        String sanitizedEmail = FirebaseAuthHelper.sanitizeEmail(email);

        // Clear focus to hide keyboard
        registerButton.clearFocus();

        // Validate inputs
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(RegisterActivity.this, getString(R.string.please_fill_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!FirebaseAuthHelper.isValidEmail(sanitizedEmail)) {
            Toast.makeText(RegisterActivity.this, getString(R.string.valid_email_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, getString(R.string.password_match_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!FirebaseAuthHelper.isStrongPassword(password)) {
            Toast.makeText(RegisterActivity.this, getString(R.string.password_length_error), Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar
        if(progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
        }

        // Attempt to create user
        mAuth.createUserWithEmailAndPassword(sanitizedEmail, password)
                .addOnCompleteListener(this, task -> {
                    // Hide progress bar
                    if(progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                        registerButton.setEnabled(true);
                    }

                    if (task.isSuccessful()) {
                        // Registration successful
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Store user data in Firestore
                            storeUserData(user.getUid(), sanitizedEmail);

                            // Send verification email
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, getString(R.string.verification_sent), Toast.LENGTH_SHORT).show();
                                            
                                            // Navigate to confirmation screen
                                            Intent intent = new Intent(RegisterActivity.this, WaitingForConfirmationActivity.class);
                                            intent.putExtra("email", sanitizedEmail); // Pass email to the next activity
                                            startActivity(intent);
                                            finish(); // Close RegisterActivity
                                        } else {
                                            Toast.makeText(RegisterActivity.this, getString(R.string.error_sending_verification), Toast.LENGTH_SHORT).show();
                                            // Still navigate to confirmation screen but inform user about the issue
                                            Intent intent = new Intent(RegisterActivity.this, WaitingForConfirmationActivity.class);
                                            intent.putExtra("email", sanitizedEmail);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                        }
                    } else {
                        // Handle specific error types
                        Exception exception = task.getException();
                        if (exception == null) {
                            Toast.makeText(RegisterActivity.this, "Registration failed: Unknown error", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        try {
                            throw exception;
                        } catch (FirebaseAuthWeakPasswordException e) {
                            Toast.makeText(RegisterActivity.this, getString(R.string.weak_password_error), Toast.LENGTH_LONG).show();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            Toast.makeText(RegisterActivity.this, getString(R.string.invalid_credentials_error), Toast.LENGTH_LONG).show();
                        } catch (FirebaseAuthUserCollisionException e) {
                            Toast.makeText(RegisterActivity.this, getString(R.string.user_exists_error), Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(RegisterActivity.this, "Registration error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void storeUserData(String userId, String email) {
        // Create a User object with initial data
        User userData = new User();
        userData.setUserId(userId); // ✅ CRITICAL: Set userId field for Firestore queries
        userData.setEmail(email);
        userData.setAvatarUrl(""); // Default avatar
        userData.setCreatedAt(Timestamp.now()); // Current timestamp
        userData.setIsOnline(false); // Default to offline
        userData.setLastSeen(Timestamp.now()); // Current timestamp
        userData.setRecipesCount(0); // Start with 0 recipes

        // Save to Firestore
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data stored successfully for userId: " + userId);
                })
                .addOnFailureListener(e -> {
                    // Handle failure to store user data
                    Log.e(TAG, "Error storing user data: " + e.getMessage(), e);
                    Toast.makeText(RegisterActivity.this, "Error storing user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}