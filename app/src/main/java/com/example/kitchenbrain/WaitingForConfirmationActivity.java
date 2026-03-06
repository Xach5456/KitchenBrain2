package com.example.kitchenbrain;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class WaitingForConfirmationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Handler handler;
    private Runnable checkEmailVerificationRunnable;
    private Button resendEmailButton;
    private Button backToRegisterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_for_confirmation);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize buttons
        resendEmailButton = findViewById(R.id.resendEmailButton);
        backToRegisterButton = findViewById(R.id.backToRegisterButton);

        if (currentUser == null) {
            goToRegisterScreen();
            return;
        }

        // Resend email button handler
        resendEmailButton.setOnClickListener(view -> resendVerificationEmail());

        // Back button handler with account removal
        backToRegisterButton.setOnClickListener(view -> deleteAccountAndGoToRegister());

        // Start email verification check
        startEmailVerificationCheck();
    }

    private void startEmailVerificationCheck() {
        handler = new Handler();
        checkEmailVerificationRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentUser != null) {
                    currentUser.reload().addOnCompleteListener(task -> {
                        if (currentUser.isEmailVerified()) {
                            handler.removeCallbacks(checkEmailVerificationRunnable);

                            // Direct user to profile setup after verification
                            goToProfileSetupActivity();
                        } else {
                            handler.postDelayed(this, 3000);
                        }
                    });
                }
            }
        };
        handler.postDelayed(checkEmailVerificationRunnable, 3000);
    }

    private void resendVerificationEmail() {
        if (currentUser != null) {
            currentUser.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, getString(R.string.verification_resend_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.verification_resend_error), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void deleteAccountAndGoToRegister() {
        if (currentUser != null) {
            // Sign out the user instead of deleting the account
            mAuth.signOut();
            goToRegisterScreen();
        }
    }

    private void goToProfileSetupActivity() {
        // Navigate to ProfileSetupFragment directly within MainActivity
        Intent intent = new Intent(WaitingForConfirmationActivity.this, MainActivity.class);
        intent.putExtra("show_profile_setup", true); // Flag to show profile setup fragment
        startActivity(intent);
        finish();
    }

    private void goToRegisterScreen() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(checkEmailVerificationRunnable);
        }
    }
}