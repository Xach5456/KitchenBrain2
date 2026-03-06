package com.example.kitchenbrain;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;
    private boolean isGuestMode = false;
    
    public boolean isGuestMode() {
        return isGuestMode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check Google Play Services availability early
        if (!GooglePlayServicesHelper.isGooglePlayServicesAvailable(this)) {
            Log.w(TAG, "Google Play Services not available. App may have limited functionality.");
            Toast.makeText(this, "Google Play Services unavailable. Some features may not work properly.", 
                          Toast.LENGTH_LONG).show();
        }

        mAuth = FirebaseAuth.getInstance();
        
        // Check if launched in guest mode
        isGuestMode = getIntent().getBooleanExtra("is_guest_mode", false);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Initialize BottomNavigationView and show it immediately
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.VISIBLE); // Show menu immediately

        // Check if we should show profile setup directly
        boolean showProfileSetup = getIntent().getBooleanExtra("show_profile_setup", false);
        
        // If not guest mode and no current user, redirect to login screen
        if (!isGuestMode && (currentUser == null || !currentUser.isEmailVerified())) {
            if (currentUser != null) {
                currentUser.reload().addOnCompleteListener(task -> {
                    FirebaseUser updatedUser = mAuth.getCurrentUser();
                    if (updatedUser == null || !updatedUser.isEmailVerified()) {
                        // If not verified, redirect to WaitingForConfirmationActivity
                        startActivity(new Intent(MainActivity.this, WaitingForConfirmationActivity.class));
                        finish();
                    } else {
                        // If verified, continue with app
                        initializeApp(updatedUser, showProfileSetup);
                    }
                });
            } else {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
        } else {
            // Either guest mode or user is authenticated
            if (isGuestMode) {
                // User is in guest mode, continue with app
                initializeApp(null, false);
            } else {
                // User is authenticated, continue with app
                initializeApp(currentUser, showProfileSetup);
            }
        }
    }

    private void initializeApp(FirebaseUser currentUser, boolean showProfileSetup) {
        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set listener for navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            // Use if-else instead of switch
            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                if (isGuestMode) {
                    // In guest mode, show a different profile fragment or alert
                    selectedFragment = new GuestProfileFragment();
                } else {
                    selectedFragment = new ProfileFragment();
                }
            } else if (item.getItemId() == R.id.nav_friends) {
                if (isGuestMode) {
                    // In guest mode, show a message or disable this feature
                    showGuestRestrictionMessage();
                    return false;
                } else {
                    selectedFragment = new FriendListFragment();
                }
            } else if (item.getItemId() == R.id.nav_add_recipe) {
                if (isGuestMode) {
                    // In guest mode, show a message or disable this feature
                    showGuestRestrictionMessage();
                    return false;
                } else {
                    selectedFragment = new AddRecipeFragment();
                }
            } else if (item.getItemId() == R.id.nav_chat) {
                if (isGuestMode) {
                    // In guest mode, show a message or disable this feature
                    showGuestRestrictionMessage();
                    return false;
                } else {
                    selectedFragment = new ChatListFragment();
                }
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        // Continue with initial fragment loading if not guest mode
        if (!isGuestMode) {
            if (currentUser != null && currentUser.isEmailVerified()) {
                if (showProfileSetup) {
                    // If coming from registration, show profile setup directly
                    showProfileSetupFragment();
                } else {
                    checkProfileSetup(currentUser.getUid());
                }
            }
        } else {
            // For guest mode, load the home fragment by default
            loadFragment(new HomeFragment());
        }
    }

    private void showGuestRestrictionMessage() {
        // Show a toast message indicating that the feature is not available in guest mode
        android.widget.Toast.makeText(this, "This feature requires login. Please sign in to access.", android.widget.Toast.LENGTH_LONG).show();
    }

    // Method to show profile setup fragment directly
    private void showProfileSetupFragment() {
        // Hide BottomNavigationView when showing profile setup
        bottomNavigationView.setVisibility(View.GONE);
        loadFragment(new ProfileSetupFragment());
    }

    // Check profile setup
    public void checkProfileSetup(String userId) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists() || !document.contains("username")) {
                        // If profile is not set up, hide BottomNavigationView and show ProfileSetupFragment
                        bottomNavigationView.setVisibility(View.GONE);
                        loadFragment(new ProfileSetupFragment());
                    } else {
                        // If profile is set up, show HomeFragment and BottomNavigationView
                        bottomNavigationView.setVisibility(View.VISIBLE);
                        loadFragment(new HomeFragment());
                    }
                })
                .addOnFailureListener(e -> {
                    // In case of error immediately show main screen and BottomNavigationView
                    bottomNavigationView.setVisibility(View.VISIBLE);
                    loadFragment(new HomeFragment());
                });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Update user's online status when app comes to foreground (only if not guest)
        if (!isGuestMode) {
            updateOnlineStatus(true);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Update user's online status when app goes to background (only if not guest)
        if (!isGuestMode) {
            updateOnlineStatus(false);
        }
    }
    
    private void updateOnlineStatus(boolean isOnline) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("online", isOnline, "lastSeen", com.google.firebase.Timestamp.now());
        }
    }

    // Load fragments
    private void loadFragment(Fragment fragment) {
        if (fragment != null && !isFinishing() && !isDestroyed()) {
            try {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                // Only add to back stack if it's not a bottom navigation item
                if (!(fragment instanceof HomeFragment) && !(fragment instanceof SearchFragment) && 
                    !(fragment instanceof ProfileFragment) && !(fragment instanceof AddRecipeFragment) && 
                    !(fragment instanceof ChatFragment)) {
                    transaction.addToBackStack(null);
                }
                transaction.commitAllowingStateLoss();
            } catch (Exception e) {
                // Silent catch to prevent crashes
            }
        }
    }
}