package com.example.kitchenbrain;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.repository.RecipeRepository;
import com.example.kitchenbrain.utils.CloudinaryHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Modern Material 3 Profile & Settings Fragment
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    public static ProfileFragment newInstance(String userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("user_id", userId);
        fragment.setArguments(args);
        return fragment;
    }

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    private String currentUserId;
    private User currentUser;
    
    private ListenerRegistration userDataListener;
    private ListenerRegistration userRecipesListener;
    private ListenerRegistration followersCountListener;
    private ListenerRegistration followingCountListener;
    private final Set<String> profileFollowerIds = new HashSet<>();
    private final Set<String> profileFollowingIds = new HashSet<>();
    
    private ShapeableImageView imageViewProfile;
    private TextView textViewUsername;
    private TextView textViewEmail;
    private TextView textFollowersCount;
    private TextView textFollowingCount;
    private TextView textMutualCount;
    private MaterialButton buttonEditProfile;
    private MaterialButton buttonShareProfile;
    private MaterialButton buttonOpenSettings;
    private MaterialButton buttonAddRecipe;
    private View buttonChangePhoto;
    private NestedScrollView profileScrollView;
    private View textPreferencesHeader;
    private LinearLayout layoutEditUsername;
    private LinearLayout layoutDeleteAccount;
    private LinearLayout layoutMessagePermissions;
    private LinearLayout layoutLogout;
    private TextView textCurrentUsername;
    private TextView textMessagePermission;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchMessaging;
    private EditText editTextSearchRecipes;
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Recipe> allUserPosts;
    private List<Recipe> userPosts;
    private boolean followStatsObserved;

    // Modern Activity Result Launchers
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        uploadProfileImage(uri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    showSnackbar("Permission denied. Cannot select image.");
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_modern, container, false);
        
        initFirebase();
        prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        
        initViews(view);
        setupClickListeners();
        loadSettings();
        setupRecyclerView();
        
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                loadUserData();
            }
        }, 400);
        
        return view;
    }
    
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        Bundle args = getArguments();
        if (args != null) {
            currentUserId = args.getString("user_id");
        }
        
        if (currentUserId == null && mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }
    }
    
    private void initViews(View view) {
        imageViewProfile = view.findViewById(R.id.imageViewProfile);
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        textFollowersCount = view.findViewById(R.id.textFollowersCount);
        textFollowingCount = view.findViewById(R.id.textFollowingCount);
        textMutualCount = view.findViewById(R.id.textMutualCount);
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
        buttonShareProfile = view.findViewById(R.id.buttonShareProfile);
        buttonOpenSettings = view.findViewById(R.id.buttonOpenSettings);
        buttonAddRecipe = view.findViewById(R.id.buttonAddRecipe);
        buttonChangePhoto = view.findViewById(R.id.buttonChangePhoto);
        profileScrollView = view.findViewById(R.id.profileScrollView);
        textPreferencesHeader = view.findViewById(R.id.textPreferencesHeader);
        layoutEditUsername = view.findViewById(R.id.layoutEditUsername);
        layoutDeleteAccount = view.findViewById(R.id.layoutDeleteAccount);
        layoutMessagePermissions = view.findViewById(R.id.layoutMessagePermissions);
        layoutLogout = view.findViewById(R.id.layoutLogout);
        textCurrentUsername = view.findViewById(R.id.textCurrentUsername);
        textMessagePermission = view.findViewById(R.id.textMessagePermission);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchMessaging = view.findViewById(R.id.switchMessaging);
        editTextSearchRecipes = view.findViewById(R.id.etSearchRecipes);
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts);
    }
    
    private void setupClickListeners() {
        if (buttonChangePhoto != null) {
            buttonChangePhoto.setOnClickListener(v -> handleImagePickerClick());
        }
        
        if (buttonEditProfile != null) buttonEditProfile.setOnClickListener(v -> showEditProfileOptionsDialog());
        if (buttonShareProfile != null) buttonShareProfile.setOnClickListener(v -> shareProfile());
        if (buttonOpenSettings != null) buttonOpenSettings.setOnClickListener(v -> scrollToSettings());
        if (buttonAddRecipe != null) buttonAddRecipe.setOnClickListener(v -> openCreateRecipe());
        if (layoutEditUsername != null) layoutEditUsername.setOnClickListener(v -> showEditUsernameDialog());
        if (layoutDeleteAccount != null) layoutDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
        if (layoutMessagePermissions != null) layoutMessagePermissions.setOnClickListener(v -> showMessagePermissionsDialog());
        if (layoutLogout != null) layoutLogout.setOnClickListener(v -> showLogoutConfirmation());
        
        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((b, isChecked) -> {
                applyDarkMode(isChecked);
                saveSettings();
            });
        }
        
        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((b, isChecked) -> {
                saveSettings();
                showSnackbar(isChecked ? "Notifications enabled" : "Notifications disabled");
            });
        }
        
        if (switchMessaging != null) {
            switchMessaging.setOnCheckedChangeListener((b, isChecked) -> {
                updateMessagingSetting(isChecked);
                saveSettings();
            });
        }

        if (editTextSearchRecipes != null) {
            editTextSearchRecipes.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterRecipes(s != null ? s.toString() : "");
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        if (textFollowersCount != null) {
            ((View)textFollowersCount.getParent()).setOnClickListener(v -> openFriendsFragment(FriendsFragment.TAB_FOLLOWERS));
        }
        if (textFollowingCount != null) {
            ((View)textFollowingCount.getParent()).setOnClickListener(v -> openFriendsFragment(FriendsFragment.TAB_FOLLOWING));
        }
        if (textMutualCount != null) {
            ((View) textMutualCount.getParent()).setOnClickListener(v -> openFriendsFragment(FriendsFragment.TAB_MUTUAL));
        }

        updateOwnerOnlyControls();
    }

    private void updateOwnerOnlyControls() {
        boolean ownProfile = isOwnProfile();
        setVisible(buttonChangePhoto, ownProfile);
        setVisible(buttonEditProfile, ownProfile);
        setVisible(buttonOpenSettings, ownProfile);
        setVisible(buttonAddRecipe, ownProfile);
        setVisible(layoutEditUsername, ownProfile);
        setVisible(layoutMessagePermissions, ownProfile);
        setVisible(layoutLogout, ownProfile);
        setVisible(layoutDeleteAccount, ownProfile);
        if (switchDarkMode != null) switchDarkMode.setEnabled(ownProfile);
        if (switchNotifications != null) switchNotifications.setEnabled(ownProfile);
        if (switchMessaging != null) switchMessaging.setEnabled(ownProfile);
    }

    private void setVisible(View view, boolean visible) {
        if (view != null) view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean isOwnProfile() {
        return mAuth != null
                && mAuth.getCurrentUser() != null
                && !TextUtils.isEmpty(currentUserId)
                && currentUserId.equals(mAuth.getCurrentUser().getUid());
    }

    private void shareProfile() {
        if (!isAdded()) return;
        String username = currentUser != null ? currentUser.getUsername() : null;
        if (TextUtils.isEmpty(username)) username = "Kitchen Brain profile";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, username);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out " + username + " on Kitchen Brain.");
        startActivity(Intent.createChooser(shareIntent, "Share Profile"));
    }

    private void scrollToSettings() {
        if (profileScrollView == null || textPreferencesHeader == null) return;
        profileScrollView.post(() -> profileScrollView.smoothScrollTo(0, textPreferencesHeader.getTop()));
    }

    private void openCreateRecipe() {
        if (!isOwnProfile()) {
            showSnackbar("You can add recipes only from your own profile.");
            return;
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showFragment(new CreateRecipeFragment(), "create_recipe");
        }
    }

    private void showEditProfileOptionsDialog() {
        if (!isOwnProfile() || !isAdded()) return;
        String[] options = {"Edit Username", "Change Photo"};
        new MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                .setTitle("Edit Profile")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditUsernameDialog();
                    } else {
                        handleImagePickerClick();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleImagePickerClick() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImage(Uri imageUri) {
        if (currentUserId == null || !isAdded()) return;
        
        showSnackbar("Uploading profile picture...");
        
        CloudinaryHelper.uploadImage(
            requireContext(),
            imageUri,
            url -> {
                if (isAdded()) {
                    db.collection("users").document(currentUserId).update("avatarUrl", url)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    showSnackbar("Profile photo updated!");
                                    loadUserData();
                                }
                            });
                }
            },
            error -> {
                if (isAdded()) showSnackbar("Upload failed: " + error);
            },
            null
        );
    }

    private void openFriendsFragment(int initialTab) {
        if (getActivity() instanceof MainActivity) {
            FriendsFragment fragment = new FriendsFragment();
            Bundle args = new Bundle();
            args.putInt("current_tab", initialTab);
            fragment.setArguments(args);
            ((MainActivity) getActivity()).showFragment(fragment, "friends");
        }
    }
    
    private void loadUserData() {
        if (currentUserId == null || getContext() == null) return;

        observeFollowStats();
        if (userDataListener != null) {
            userDataListener.remove();
            userDataListener = null;
        }
        if (userRecipesListener != null) {
            userRecipesListener.remove();
            userRecipesListener = null;
        }
        
        userDataListener = db.collection("users").document(currentUserId)
                .addSnapshotListener((document, error) -> {
                    if (!isAdded()) return;
                    
                    if (error != null) {
                        Log.e(TAG, "Error listening to user data", error);
                        return;
                    }
                    
                    if (document != null && document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            currentUser.setId(document.getId());
                            updateUIWithUserData(currentUser);
                        }
                    }
                });
        
        loadUserRecipes();
    }

    private void observeFollowStats() {
        if (followStatsObserved || currentUserId == null || !isAdded()) return;
        followStatsObserved = true;

        followersCountListener = db.collection("followers")
                .document(currentUserId)
                .collection("userFollowers")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        Log.e(TAG, "Error listening to profile followers", error);
                        return;
                    }

                    profileFollowerIds.clear();
                    if (snapshot != null) {
                        snapshot.forEach(doc -> profileFollowerIds.add(doc.getId()));
                    }

                    if (textFollowersCount != null) {
                        textFollowersCount.setText(String.valueOf(profileFollowerIds.size()));
                    }
                    updateMutualCount();
                });

        followingCountListener = db.collection("following")
                .document(currentUserId)
                .collection("userFollowing")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        Log.e(TAG, "Error listening to profile following", error);
                        return;
                    }

                    profileFollowingIds.clear();
                    if (snapshot != null) {
                        snapshot.forEach(doc -> profileFollowingIds.add(doc.getId()));
                    }

                    if (textFollowingCount != null) {
                        textFollowingCount.setText(String.valueOf(profileFollowingIds.size()));
                    }
                    updateMutualCount();
                });
    }

    private void updateMutualCount() {
        if (textMutualCount == null) return;
        Set<String> mutualIds = new HashSet<>(profileFollowingIds);
        mutualIds.retainAll(profileFollowerIds);
        mutualIds.remove(currentUserId);
        textMutualCount.setText(String.valueOf(mutualIds.size()));
    }
    
    private void updateUIWithUserData(User user) {
        if (!isAdded()) return;
        
        if (textViewUsername != null) textViewUsername.setText(user.getUsername());
        if (textCurrentUsername != null) textCurrentUsername.setText(user.getUsername());
        
        if (textViewEmail != null && mAuth.getCurrentUser() != null)
            textViewEmail.setText(mAuth.getCurrentUser().getEmail());

        if (imageViewProfile != null) {
            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(imageViewProfile);
            } else {
                imageViewProfile.setImageResource(R.drawable.ic_default_avatar);
            }
        }
    }
    
    private void loadUserRecipes() {
        if (currentUserId == null || getContext() == null) return;
        
        userRecipesListener = db.collection("recipes").whereEqualTo("authorId", currentUserId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (!isAdded()) return;
                    
                    if (error != null) return;
                    
                    if (allUserPosts != null && queryDocumentSnapshots != null) {
                        allUserPosts.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe.setId(doc.getId());
                                allUserPosts.add(recipe);
                            }
                        }
                        String query = editTextSearchRecipes != null
                                ? editTextSearchRecipes.getText().toString()
                                : "";
                        filterRecipes(query);
                    }
                });
    }
    
    private void loadSettings() {
        if (!isAdded()) return;
        if (switchDarkMode != null) switchDarkMode.setChecked(prefs.getBoolean("dark_mode", false));
        if (switchNotifications != null) switchNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        if (switchMessaging != null) switchMessaging.setChecked(prefs.getBoolean("messaging_enabled", true));
        
        String permission = prefs.getString("message_permission", "everyone");
        updateMessagePermissionText(permission);
    }
    
    private void saveSettings() {
        if (!isAdded()) return;
        prefs.edit()
                .putBoolean("dark_mode", switchDarkMode != null && switchDarkMode.isChecked())
                .putBoolean("notifications_enabled", switchNotifications == null || switchNotifications.isChecked())
                .putBoolean("messaging_enabled", switchMessaging == null || switchMessaging.isChecked())
                .apply();
    }
    
    private void applyDarkMode(boolean enabled) {
        if (!isAdded()) return;
        AppCompatDelegate.setDefaultNightMode(enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
    
    private void updateMessagingSetting(boolean enabled) {
        if (!isAdded() || currentUserId == null) return;
        db.collection("users").document(currentUserId).update("messagingEnabled", enabled);
    }
    
    private void updateMessagePermissionText(String permission) {
        if (textMessagePermission == null || !isAdded()) return;
        switch (permission) {
            case "friends": textMessagePermission.setText(R.string.profile_message_friends_only); break;
            case "nobody": textMessagePermission.setText(R.string.profile_message_nobody); break;
            default: textMessagePermission.setText(R.string.profile_message_everyone);
        }
    }

    private void showEditUsernameDialog() {
        if (!isAdded() || getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText editText = dialogView.findViewById(R.id.editTextUsername);
        if (currentUser != null) editText.setText(currentUser.getUsername());
        
        // Material dialog colors follow the active light/dark theme.
        new MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                .setTitle("Edit Username")
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String newUsername = editText.getText().toString().trim();
                    if (!newUsername.isEmpty()) updateUsername(newUsername);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void updateUsername(String newUsername) {
        if (currentUserId == null || !isAdded()) return;
        db.collection("users").document(currentUserId).update("username", newUsername)
                .addOnSuccessListener(aVoid -> loadUserData());
    }
    
    private void showLogoutConfirmation() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setPositiveButton("Logout", (d, w) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void logout() {
        if (db != null && currentUserId != null) {
            db.collection("users").document(currentUserId)
                    .update("online", false, "lastSeen", Timestamp.now())
                    .addOnCompleteListener(t -> performSignOut());
        } else {
            performSignOut();
        }
    }
    
    private void performSignOut() {
        if (mAuth != null) mAuth.signOut();
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
    
    private void showDeleteAccountConfirmation() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                .setTitle("Delete Account")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteAccount() {
        if (currentUserId == null) return;
        db.collection("users").document(currentUserId).delete()
                .addOnSuccessListener(aVoid -> deleteAuthAccount());
    }
    
    private void deleteAuthAccount() {
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().delete().addOnCompleteListener(task -> performSignOut());
        }
    }
    
    private void showMessagePermissionsDialog() {
        if (!isAdded()) return;
        String[] options = {
                getString(R.string.profile_message_everyone),
                getString(R.string.profile_message_friends_only),
                getString(R.string.profile_message_nobody)
        };
        String current = prefs.getString("message_permission", "everyone");
        int index = current.equals("friends") ? 1 : current.equals("nobody") ? 2 : 0;
        
        new MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                .setTitle("Who Can Message Me")
                .setSingleChoiceItems(options, index, (d, which) -> {
                    String perm = which == 1 ? "friends" : which == 2 ? "nobody" : "everyone";
                    prefs.edit().putString("message_permission", perm).apply();
                    updateMessagePermissionText(perm);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void setupRecyclerView() {
        if (getContext() == null) return;
        allUserPosts = new ArrayList<>();
        userPosts = new ArrayList<>();
        postAdapter = new PostAdapter(userPosts, this, currentUserId, new PostAdapter.OnPostActionListener() {
            @Override
            public void onViewRecipe(Recipe recipe) {
                openRecipeDetail(recipe);
            }

            @Override
            public void onEditRecipe(Recipe recipe) {
                openEditRecipe(recipe);
            }

            @Override
            public void onDeleteRecipe(Recipe recipe) {
                confirmDeleteRecipe(recipe);
            }
        });
        if (recyclerViewPosts != null) {
            recyclerViewPosts.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewPosts.setAdapter(postAdapter);
        }
    }

    private void filterRecipes(String query) {
        if (allUserPosts == null || userPosts == null) return;

        String normalizedQuery = query != null
                ? query.trim().toLowerCase(Locale.getDefault())
                : "";
        List<Recipe> filteredRecipes = new ArrayList<>();

        if (normalizedQuery.isEmpty()) {
            filteredRecipes.addAll(allUserPosts);
        } else {
            for (Recipe recipe : allUserPosts) {
                String title = recipe != null ? recipe.getTitle() : null;
                if (!TextUtils.isEmpty(title)
                        && title.toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                    filteredRecipes.add(recipe);
                }
            }
        }

        if (postAdapter != null) {
            postAdapter.updateRecipes(filteredRecipes);
        }
        userPosts.clear();
        userPosts.addAll(filteredRecipes);
    }

    private void openRecipeDetail(Recipe recipe) {
        if (recipe == null || !isAdded()) return;
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, RecipeDetailFragment.newInstance(recipe))
                .addToBackStack(null)
                .commit();
    }

    private void openEditRecipe(Recipe recipe) {
        if (recipe == null || !isOwnProfile()) {
            showSnackbar("You can edit only recipes you created.");
            return;
        }
        String recipeId = recipe.getId();
        if (TextUtils.isEmpty(recipeId)) {
            showSnackbar("Recipe is missing an ID.");
            return;
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showFragment(CreateRecipeFragment.newInstance(recipe), "edit_recipe_" + recipeId);
        }
    }

    private void confirmDeleteRecipe(Recipe recipe) {
        if (recipe == null || !isOwnProfile()) {
            showSnackbar("You can delete only recipes you created.");
            return;
        }
        String recipeId = recipe.getId();
        if (TextUtils.isEmpty(recipeId)) {
            showSnackbar("Recipe is missing an ID.");
            return;
        }
        new MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete this recipe?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecipe(recipeId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecipe(String recipeId) {
        new RecipeRepository().deleteRecipe(recipeId, new RecipeRepository.RecipeCallback<>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) showSnackbar("Recipe deleted");
            }

            @Override
            public void onError(String error) {
                if (isAdded()) showSnackbar("Delete failed: " + error);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userDataListener != null) userDataListener.remove();
        if (userRecipesListener != null) userRecipesListener.remove();
        if (followersCountListener != null) followersCountListener.remove();
        if (followingCountListener != null) followingCountListener.remove();
        profileFollowerIds.clear();
        profileFollowingIds.clear();
        followStatsObserved = false;
    }
    
    private void showSnackbar(String message) {
        if (getView() != null) Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
    }
}
