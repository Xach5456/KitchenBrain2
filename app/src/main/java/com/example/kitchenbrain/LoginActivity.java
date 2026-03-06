package com.example.kitchenbrain;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button loginButton, createAccountButton;
    private TextView textRegisterLink, textGuestMode;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    public void onBackPressed() {
        // Blocking back button
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI elements
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton); // New button
        textRegisterLink = findViewById(R.id.textRegisterLink);
        textGuestMode = findViewById(R.id.textGuestMode);
        progressBar = findViewById(R.id.progressBar);

        // Make progress bar invisible initially
        if(progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            // If user is already signed in and verified, redirect to MainActivity immediately
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish(); // Close LoginActivity
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUser();
            }
        });

        // New button click listener for creating account
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to RegisterActivity
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        textRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        textGuestMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(LoginActivity.this, getString(R.string.guest_mode_activated), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("is_guest_mode", true);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();

        // Sanitize email
        String sanitizedEmail = FirebaseAuthHelper.sanitizeEmail(email);

        // Clear focus to hide keyboard
        loginButton.clearFocus();

        // Validate inputs
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, getString(R.string.please_fill_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!FirebaseAuthHelper.isValidEmail(sanitizedEmail)) {
            Toast.makeText(LoginActivity.this, getString(R.string.valid_email_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!FirebaseAuthHelper.isStrongPassword(password)) {
            Toast.makeText(LoginActivity.this, getString(R.string.password_length_error), Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar
        if(progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            createAccountButton.setEnabled(false);
        }

        mAuth.signInWithEmailAndPassword(sanitizedEmail, password)
                .addOnCompleteListener(this, task -> {
                    // Hide progress bar
                    if(progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);
                        createAccountButton.setEnabled(true);
                    }

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else if (user != null) {
                            Toast.makeText(LoginActivity.this, getString(R.string.verify_email_before_login), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Handle specific error types
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            Toast.makeText(LoginActivity.this, getString(R.string.user_not_found_error), Toast.LENGTH_LONG).show();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            Toast.makeText(LoginActivity.this, getString(R.string.invalid_credentials_error), Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}