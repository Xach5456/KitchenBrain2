package com.example.kitchenbrain;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.example.kitchenbrain.viewmodel.SharedViewModel;
import com.example.kitchenbrain.manager.FollowGraphRepository;
import com.example.kitchenbrain.ui.CreateRecipeFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private boolean isGuestMode = false;
    private SharedViewModel sharedViewModel;
    
    private String lastOpenedChatUserId = null;
    private long lastChatOpenTime = 0;
    private static final long CHAT_OPEN_COOLDOWN_MS = 2000;
    
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        isGuestMode = getIntent().getBooleanExtra("is_guest_mode", false);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Authentication check
        if (!isGuestMode && (currentUser == null || !currentUser.isEmailVerified())) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        setupNavigation();
        setupWindowInsets();
        
        if (!isGuestMode && currentUser != null) {
            sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
            FollowGraphRepository.getInstance().initialize(currentUser.getUid());
            observeMutualFollowEvents();
            startUserStatusListener(currentUser.getUid());
        }

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("show_profile_setup", false) && currentUser != null) {
                showFragment(new ProfileSetupFragment(), "profile_setup");
            } else if (!isGuestMode && currentUser != null) {
                checkProfileSetup(currentUser.getUid());
            } else {
                showFragment(new HomeFragment(), "home");
                updateBottomNavigationSelection(R.id.nav_home);
            }
        }
    }

    /**
     * 🔥 REAL-TIME MUTUAL FOLLOW DETECTION
     */
    private void startUserStatusListener(String userId) {
        if (userId == null) return;
        
        userListener = FirebaseFirestore.getInstance().collection("users").document(userId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.e(TAG, "User listener error", e);
                    return;
                }
                
                if (snapshot != null && snapshot.exists()) {
                    String newMutualUserId = snapshot.getString("newMutualFollowUserId");
                    if (newMutualUserId != null && !newMutualUserId.isEmpty()) {
                        Log.d(TAG, "🎯 Mutual follow detected! Auto-opening chat with: " + newMutualUserId);
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                            .update("newMutualFollowUserId", null);
                        openChat(newMutualUserId, null);
                    }
                }
            });
    }

    private void setupWindowInsets() {
        View mainRoot = findViewById(R.id.main_root);
        View toolbarView = findViewById(R.id.toolbar);
        View bottomNav = findViewById(R.id.bottom_navigation_container);
        View fragmentContainer = findViewById(R.id.fragment_container);

        if (mainRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, windowInsets) -> {
                Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                if (toolbarView != null) toolbarView.setPadding(0, systemBars.top, 0, 0);
                if (bottomNav != null) bottomNav.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
                if (fragmentContainer != null) fragmentContainer.setPadding(systemBars.left, 0, systemBars.right, 0);
                return windowInsets;
            });
        }
    }

    private void setupNavigation() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupCustomBottomNavigation();
        setupFragmentVisibilityListener();
    }
    
    private void setupCustomBottomNavigation() {
        View navHome = findViewById(R.id.nav_home);
        View navSearch = findViewById(R.id.nav_search);
        View navAdd = findViewById(R.id.nav_add_recipe);
        View navFriends = findViewById(R.id.nav_friends);
        View navChat = findViewById(R.id.nav_chat);
        View navProfile = findViewById(R.id.nav_profile);

        if (navHome != null) navHome.setOnClickListener(v -> { showFragment(new HomeFragment(), "home"); updateBottomNavigationSelection(R.id.nav_home); });
        if (navSearch != null) navSearch.setOnClickListener(v -> { showFragment(new SearchFragment(), "search"); updateBottomNavigationSelection(R.id.nav_search); });
        if (navAdd != null) navAdd.setOnClickListener(v -> { 
            showFragment(new AddRecipeFragment(), "add_recipe"); 
            updateBottomNavigationSelection(R.id.nav_add_recipe); 
        });
        if (navFriends != null) navFriends.setOnClickListener(v -> { showFragment(new FriendsFragment(), "friends"); updateBottomNavigationSelection(R.id.nav_friends); });
        if (navChat != null) navChat.setOnClickListener(v -> { showFragment(new ChatListFragment(), "chat"); updateBottomNavigationSelection(R.id.nav_chat); });
        if (navProfile != null) navProfile.setOnClickListener(v -> {
            if (isGuestMode) showFragment(new GuestProfileFragment(), "guest_profile");
            else showFragment(new ProfileFragment(), "profile");
            updateBottomNavigationSelection(R.id.nav_profile);
        });
    }

    private void updateBottomNavigationSelection(int selectedId) {
        int[] navIds = {R.id.nav_home, R.id.nav_search, R.id.nav_add_recipe, R.id.nav_friends, R.id.nav_chat, R.id.nav_profile};
        int selectedColor = ContextCompat.getColor(this, R.color.text_primary);
        int unselectedColor = ContextCompat.getColor(this, R.color.text_secondary);

        for (int id : navIds) {
            View view = findViewById(id);
            if (view == null) continue;
            boolean isSelected = (id == selectedId);
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child instanceof ImageView) {
                        ((ImageView) child).setImageTintList(ColorStateList.valueOf(isSelected ? selectedColor : unselectedColor));
                    } else if (child instanceof TextView) {
                        ((TextView) child).setTextColor(isSelected ? selectedColor : unselectedColor);
                    }
                }
            }
        }
    }
    
    private void setupFragmentVisibilityListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            boolean isFullPage = f instanceof ChatFragment || f instanceof ProfileSetupFragment || f instanceof CreateRecipeFragment;
            View bottomNav = findViewById(R.id.bottom_navigation_container);
            if (bottomNav != null) bottomNav.setVisibility(isFullPage ? View.GONE : View.VISIBLE);
        });
    }

    public void showFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .addToBackStack(tag)
            .commit();
    }

    public void navigateToFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment);
        if (addToBackStack) transaction.addToBackStack(null);
        transaction.commit();
    }

    public void openChat(String otherUserId, String username) {
        if (otherUserId == null || otherUserId.isEmpty()) return;
        long currentTime = System.currentTimeMillis();
        if (otherUserId.equals(lastOpenedChatUserId) && (currentTime - lastChatOpenTime) < CHAT_OPEN_COOLDOWN_MS) return;
        
        lastOpenedChatUserId = otherUserId;
        lastChatOpenTime = currentTime;
        
        ChatFragment chatFragment = ChatFragment.newInstance(otherUserId);
        if (username != null) {
            Bundle args = chatFragment.getArguments();
            if (args != null) args.putString("other_username", username);
        }
        
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, chatFragment)
            .addToBackStack("chat_" + otherUserId)
            .commit();
    }

    private void observeMutualFollowEvents() {
        if (sharedViewModel != null) {
            sharedViewModel.getOpenChatUserId().observe(this, userId -> {
                if (userId != null && !userId.isEmpty()) {
                    openChat(userId, null);
                    sharedViewModel.emitOpenChatEvent(null);
                }
            });
        }
    }

    public void checkProfileSetup(String userId) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getString("username") != null 
                            && !documentSnapshot.getString("username").trim().isEmpty()) {
                        showFragment(new HomeFragment(), "home");
                        updateBottomNavigationSelection(R.id.nav_home);
                    } else {
                        showFragment(new ProfileSetupFragment(), "profile_setup");
                    }
                });
    }

    public boolean isGuestMode() {
        return isGuestMode;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
    }
}
