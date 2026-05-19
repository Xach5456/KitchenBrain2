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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.kitchenbrain.viewmodel.SharedViewModel;
import com.example.kitchenbrain.manager.FollowGraphRepository;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private boolean isGuestMode = false;
    private SharedViewModel sharedViewModel;
    private FloatingActionButton fabAddRecipe;
    
    private String lastOpenedChatUserId = null;
    private long lastChatOpenTime = 0;
    private static final long CHAT_OPEN_COOLDOWN_MS = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mAuth = FirebaseAuth.getInstance();
        isGuestMode = getIntent().getBooleanExtra("is_guest_mode", false);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        setupNavigation();
        if (getApplication() instanceof MyApplication) {
            ((MyApplication) getApplication()).initializeInteractiveServices();
        }
        
        if (!isGuestMode && currentUser != null) {
            sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);
            FollowGraphRepository.getInstance().initialize(currentUser.getUid());
            observeMutualFollowEvents();
        }

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("show_profile_setup", false) && currentUser != null) {
                showFragment(new ProfileSetupFragment(), "profile_setup");
            } else if (!isGuestMode && currentUser != null) {
                checkProfileSetup(currentUser.getUid());
            } else {
                // Load original HomeFragment by default
                showFragment(new HomeFragment(), "home");
                updateBottomNavigationSelection(R.id.nav_home);
            }
        }
    }

    public boolean isGuestMode() {
        return isGuestMode;
    }

    private void setupNavigation() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fabAddRecipe = findViewById(R.id.fabAddRecipe);

        setupCustomBottomNavigation();
        setupFragmentVisibilityListener();
        setupFabClickListener();
    }
    
    private void setupCustomBottomNavigation() {
        // Navigation items based on new 6-item layout
        View navHome = findViewById(R.id.nav_home);
        View navSearch = findViewById(R.id.nav_search);
        View navAdd = findViewById(R.id.nav_add_recipe);
        View navFriends = findViewById(R.id.nav_friends);
        View navChat = findViewById(R.id.nav_chat);
        View navProfile = findViewById(R.id.nav_profile);

        if (navHome != null) navHome.setOnClickListener(v -> {
            showFragment(new HomeFragment(), "home");
            updateBottomNavigationSelection(R.id.nav_home);
        });
        
        if (navSearch != null) navSearch.setOnClickListener(v -> {
            showFragment(new SearchFragment(), "search");
            updateBottomNavigationSelection(R.id.nav_search);
        });
        
        if (navAdd != null) navAdd.setOnClickListener(v -> {
            if (isGuestMode) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            showFragment(new AddRecipeFragment(), "add_recipe");
            updateBottomNavigationSelection(R.id.nav_add_recipe);
        });

        if (navFriends != null) navFriends.setOnClickListener(v -> {
            if (isGuestMode) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            showFragment(new FriendsFragment(), "friends");
            updateBottomNavigationSelection(R.id.nav_friends);
        });
        
        if (navChat != null) navChat.setOnClickListener(v -> {
            if (isGuestMode) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            showFragment(new ChatListFragment(), "chat");
            updateBottomNavigationSelection(R.id.nav_chat);
        });
        
        if (navProfile != null) navProfile.setOnClickListener(v -> {
            if (isGuestMode) {
                showFragment(new GuestProfileFragment(), "guest_profile");
            } else {
                showFragment(new ProfileFragment(), "profile");
            }
            updateBottomNavigationSelection(R.id.nav_profile);
        });
    }

    private void updateBottomNavigationSelection(int selectedId) {
        int[] navIds = {R.id.nav_home, R.id.nav_search, R.id.nav_add_recipe, R.id.nav_friends, R.id.nav_chat, R.id.nav_profile};
        
        int selectedColor = ContextCompat.getColor(this, R.color.text_primary);
        int unselectedColor = ContextCompat.getColor(this, R.color.text_secondary);
        int accentColor = ContextCompat.getColor(this, R.color.primary_blue);

        for (int id : navIds) {
            View view = findViewById(id);
            if (view == null) continue;
            
            boolean isSelected = (id == selectedId);
            
            // We removed the hard background selection, using scaling and tinting instead
            view.setBackgroundResource(0); 
            
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child instanceof ImageView) {
                        ImageView icon = (ImageView) child;
                        
                        // Special handling for Add button accent
                        int color = isSelected ? selectedColor : unselectedColor;
                        if (id == R.id.nav_add_recipe && !isSelected) color = accentColor;
                        else if (id == R.id.nav_add_recipe && isSelected) color = accentColor;

                        icon.setImageTintList(ColorStateList.valueOf(color));
                        
                        // 🔥 ANIMATION: Pop scale effect
                        float scale = isSelected ? 1.2f : 1.0f;
                        icon.animate()
                            .scaleX(scale)
                            .scaleY(scale)
                            .setDuration(200)
                            .setInterpolator(new OvershootInterpolator())
                            .start();
                            
                    } else if (child instanceof TextView) {
                        TextView label = (TextView) child;
                        label.setTextColor(isSelected ? selectedColor : unselectedColor);
                        label.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
                    }
                }
            }
        }
    }
    
    private void setupFragmentVisibilityListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            
            boolean isAddRecipeFragment = f instanceof AddRecipeFragment;
            
            if (fabAddRecipe != null) {
                fabAddRecipe.setVisibility(isAddRecipeFragment ? View.VISIBLE : View.GONE);
            }
            
            boolean isFullPage = f instanceof ChatFragment || f instanceof ProfileSetupFragment || f instanceof CreateRecipeFragment;
            
            View bottomNav = findViewById(R.id.bottom_navigation_container);
            if (bottomNav != null) {
                bottomNav.setVisibility(isFullPage ? View.GONE : View.VISIBLE);
            }
        });
    }

    public void showFragment(Fragment fragment, String tag) {
        Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(tag);
        
        if (existingFragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right)
                    .replace(R.id.fragment_container, fragment, tag)
                    .addToBackStack(tag)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                    .replace(R.id.fragment_container, existingFragment, tag)
                    .commit();
        }
        
        updateFabVisibility(tag);
    }

    public void navigateToFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment);
        
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        
        transaction.commit();
    }

    private void updateFabVisibility(String fragmentTag) {
        if (fabAddRecipe == null) return;
        
        if (fragmentTag != null && fragmentTag.equals("add_recipe")) {
            fabAddRecipe.setVisibility(View.VISIBLE);
        } else {
            fabAddRecipe.setVisibility(View.GONE);
        }
    }

    private void setupFabClickListener() {
        if (fabAddRecipe != null) {
            fabAddRecipe.setOnClickListener(v -> {
                if (isGuestMode) {
                    Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                    return;
                }
                showFragment(new CreateRecipeFragment(), "create_recipe");
            });
        }
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
        sharedViewModel.getOpenChatUserId().observe(this, userId -> {
            if (userId != null && !userId.isEmpty()) {
                openChat(userId, null);
                sharedViewModel.emitOpenChatEvent(null);
            }
        });
    }

    public void checkProfileSetup(String userId) {
        if (userId == null) return;
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getString("username") != null 
                            && !documentSnapshot.getString("username").trim().isEmpty()) {
                        showFragment(new HomeFragment(), "home");
                        updateBottomNavigationSelection(R.id.nav_home);
                    } else {
                        showFragment(new ProfileSetupFragment(), "profile_setup");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking profile setup: " + e.getMessage());
                    showFragment(new HomeFragment(), "home");
                    updateBottomNavigationSelection(R.id.nav_home);
                });
    }
}
