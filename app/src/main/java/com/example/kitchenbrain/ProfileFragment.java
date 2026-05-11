package com.example.kitchenbrain;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import java.util.List;

/**
 * Modern Material 3 Profile & Settings Fragment
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PERMISSION_REQUEST_CODE = 200;

    /**
     * Factory method for creating ProfileFragment with user ID
     */
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
    
    // Firestore listeners for real-time updates
    private ListenerRegistration userDataListener;
    private ListenerRegistration userRecipesListener;
    
    // UI Components
    private ShapeableImageView imageViewProfile;
    private TextView textViewUsername;
    private TextView textViewEmail;
    private MaterialButton buttonEditProfile;
    private View buttonChangePhoto;
    private LinearLayout layoutEditUsername;
    private LinearLayout layoutDeleteAccount;
    private LinearLayout layoutMessagePermissions;
    private LinearLayout layoutBlockedUsers;
    private TextView textCurrentUsername;
    private TextView textMessagePermission;
    private TextView textBlockedCount;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchMessaging;
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Recipe> userPosts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        
        View view = inflater.inflate(R.layout.fragment_profile_modern, container, false);
        
        // Initialize Firebase FIRST (lightweight)
        initFirebase();
        prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        
        // Initialize views NEXT (lightweight)
        initViews(view);
        setupClickListeners();
        loadSettings();
        setupRecyclerView();
        
        // DEFER heavy Firestore operations to let UI render first
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                loadUserData();
            }
        }, 400); // 400ms delay
        
        return view;
    }
    
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Get user ID from arguments or current user
        Bundle args = getArguments();
        if (args != null) {
            currentUserId = args.getString("user_id");
        }
        
        if (currentUserId == null && mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }
        
        if (currentUserId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please log in to View Profile", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void initViews(View view) {
        imageViewProfile = view.findViewById(R.id.imageViewProfile);
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
        buttonChangePhoto = view.findViewById(R.id.buttonChangePhoto);
        layoutEditUsername = view.findViewById(R.id.layoutEditUsername);
        layoutDeleteAccount = view.findViewById(R.id.layoutDeleteAccount);
        layoutMessagePermissions = view.findViewById(R.id.layoutMessagePermissions);
        layoutBlockedUsers = view.findViewById(R.id.layoutBlockedUsers);
        textCurrentUsername = view.findViewById(R.id.textCurrentUsername);
        textMessagePermission = view.findViewById(R.id.textMessagePermission);
        textBlockedCount = view.findViewById(R.id.textBlockedCount);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchMessaging = view.findViewById(R.id.switchMessaging);
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts);
        
        MaterialButton buttonLogout = view.findViewById(R.id.buttonLogout);
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }
    
    private void setupClickListeners() {
        buttonChangePhoto.setOnClickListener(v -> {
            if (checkPermission()) openImagePicker();
            else requestPermission();
        });
        
        buttonEditProfile.setOnClickListener(v -> showEditUsernameDialog());
        layoutEditUsername.setOnClickListener(v -> showEditUsernameDialog());
        layoutDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
        layoutMessagePermissions.setOnClickListener(v -> showMessagePermissionsDialog());
        layoutBlockedUsers.setOnClickListener(v -> showBlockedUsersDialog());
        
        // FIXED: Add null check before setting listener
        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((b, isChecked) -> {
                applyDarkMode(isChecked);
                saveSettings();
            });
        } else {
            Log.w(TAG, "switchDarkMode is null - skipping listener setup");
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
    }
    
    private void loadUserData() {
        if (currentUserId == null || getContext() == null) return;
        
        // Use real-time listener instead of blocking .get()
        userDataListener = db.collection("users").document(currentUserId)
                .addSnapshotListener((document, error) -> {
                    if (!isAdded()) return;
                    
                    if (error != null) {
                        Log.e(TAG, "Error listening to user data", error);
                        showSnackbar("Error loading profile: " + error.getMessage());
                        return;
                    }
                    
                    if (document != null && document.exists()) {
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) updateUIWithUserData(currentUser);
                    }
                });
        
        // Load recipes with real-time listener
        loadUserRecipes();
    }
    
    private void updateUIWithUserData(User user) {
        if (!isAdded()) return;
        
        if (textViewUsername != null && user.getUsername() != null)
            textViewUsername.setText(user.getUsername());
        if (textCurrentUsername != null && user.getUsername() != null)
            textCurrentUsername.setText(user.getUsername());
        
        if (textViewEmail != null && mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null)
            textViewEmail.setText(mAuth.getCurrentUser().getEmail());
        
        if (imageViewProfile != null) {
            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    Glide.with(this).load(avatarUrl).placeholder(R.drawable.ic_default_avatar).circleCrop().into(imageViewProfile);
                } catch (Exception e) {
                    imageViewProfile.setImageResource(R.drawable.ic_default_avatar);
                }
            } else {
                imageViewProfile.setImageResource(R.drawable.ic_default_avatar);
            }
        }
    }
    
    private void loadUserRecipes() {
        if (currentUserId == null || getContext() == null) return;
        
        // Use real-time listener instead of blocking .get()
        userRecipesListener = db.collection("recipes").whereEqualTo("authorId", currentUserId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (!isAdded()) return;
                    
                    if (error != null) {
                        Log.e(TAG, "Error loading recipes", error);
                        showSnackbar("Error loading recipes: " + error.getMessage());
                        return;
                    }
                    
                    if (userPosts != null && queryDocumentSnapshots != null) {
                        userPosts.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe.setId(doc.getId());
                                userPosts.add(recipe);
                            }
                        }
                        if (postAdapter != null) postAdapter.notifyDataSetChanged();
                    }
                });
    }
    
    private void loadSettings() {
        if (!isAdded()) return;
        
        // FIXED: Add null checks before accessing switches
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(prefs.getBoolean("dark_mode", false));
        }
        if (switchNotifications != null) {
            switchNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        }
        if (switchMessaging != null) {
            switchMessaging.setChecked(prefs.getBoolean("messaging_enabled", true));
        }
        
        String permission = prefs.getString("message_permission", "everyone");
        updateMessagePermissionText(permission);
        updateBlockedUsersCount();
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
        showSnackbar(enabled ? "Dark mode enabled" : "Light mode enabled");
    }
    
    private void updateMessagingSetting(boolean enabled) {
        if (!isAdded() || currentUserId == null) return;
        
        db.collection("users").document(currentUserId).update("messagingEnabled", enabled)
                .addOnSuccessListener(aVoid -> showSnackbar(enabled ? "Messaging enabled" : "Messaging disabled"))
                .addOnFailureListener(e -> showSnackbar("Failed to update setting"));
    }
    
    private void updateMessagePermissionText(String permission) {
        if (textMessagePermission == null || !isAdded()) return;
        
        switch (permission) {
            case "friends": textMessagePermission.setText("Friends only"); break;
            case "nobody": textMessagePermission.setText("Nobody"); break;
            default: textMessagePermission.setText("Everyone");
        }
    }
    
    private void updateBlockedUsersCount() {
        if (textBlockedCount != null && isAdded())
            textBlockedCount.setText("0 users blocked");
    }
    
    private void showEditUsernameDialog() {
        if (!isAdded() || getContext() == null) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText editText = dialogView.findViewById(R.id.editTextUsername);
        if (currentUser != null && currentUser.getUsername() != null)
            editText.setText(currentUser.getUsername());
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Username")
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String newUsername = editText.getText().toString().trim();
                    if (!newUsername.isEmpty()) updateUsername(newUsername);
                    else showSnackbar("Username cannot be empty");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void updateUsername(String newUsername) {
        if (currentUserId == null || !isAdded()) return;
        
        db.collection("users").document(currentUserId).update("username", newUsername)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    if (textViewUsername != null) textViewUsername.setText(newUsername);
                    if (textCurrentUsername != null) textCurrentUsername.setText(newUsername);
                    showSnackbar("Username updated");
                    loadUserData();
                })
                .addOnFailureListener(e -> showSnackbar("Failed: " + e.getMessage()));
    }
    
    private void showLogoutConfirmation() {
        if (!isAdded()) return;
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setPositiveButton("Logout", (d, w) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void logout() {
        if (db != null && currentUserId != null && isAdded()) {
            db.collection("users").document(currentUserId)
                    .update("online", false, "lastSeen", Timestamp.now())
                    .addOnCompleteListener(t -> performSignOut());
        } else {
            performSignOut();
        }
    }
    
    private void performSignOut() {
        currentUserId = null;
        currentUser = null;
        if (mAuth != null) mAuth.signOut();
        
        if (getActivity() != null && !getActivity().isFinishing()) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
    
    private void showDeleteAccountConfirmation() {
        if (!isAdded()) return;
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("This action cannot be undone. All data will be deleted.")
                .setPositiveButton("Delete", (d, w) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteAccount() {
        if (!isAdded() || currentUserId == null) return;
        
        db.collection("users").document(currentUserId).delete()
                .addOnSuccessListener(aVoid -> deleteUserRecipes())
                .addOnFailureListener(e -> showSnackbar("Failed: " + e.getMessage()));
    }
    
    private void deleteUserRecipes() {
        if (!isAdded() || currentUserId == null) return;
        
        db.collection("recipes").whereEqualTo("authorId", currentUserId).get()
                .addOnSuccessListener(docs -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : docs) ids.add(doc.getId());
                    deleteRecipesBatch(ids, 0);
                })
                .addOnFailureListener(e -> deleteRecipesBatch(new ArrayList<>(), 0));
    }
    
    private void deleteRecipesBatch(List<String> ids, int index) {
        if (index >= ids.size()) {
            deleteAuthAccount();
            return;
        }
        
        db.collection("recipes").document(ids.get(index)).delete()
                .addOnCompleteListener(t -> deleteRecipesBatch(ids, index + 1));
    }
    
    private void deleteAuthAccount() {
        if (!isAdded() || mAuth.getCurrentUser() == null) return;
        
        mAuth.getCurrentUser().delete()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    showSnackbar("Account deleted");
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> showSnackbar("Database deleted, contact support"));
    }
    
    private void showMessagePermissionsDialog() {
        if (!isAdded()) return;
        
        String[] options = {"Everyone", "Friends only", "Nobody"};
        String current = prefs.getString("message_permission", "everyone");
        int index = current.equals("friends") ? 1 : current.equals("nobody") ? 2 : 0;
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Who Can Message Me")
                .setSingleChoiceItems(options, index, (d, which) -> {
                    String perm = which == 1 ? "friends" : which == 2 ? "nobody" : "everyone";
                    prefs.edit().putString("message_permission", perm).apply();
                    updateMessagePermissionText(perm);
                    showSnackbar("Updated");
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showBlockedUsersDialog() {
        if (!isAdded()) return;
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Blocked Users")
                .setMessage("No blocked users yet.")
                .setPositiveButton("OK", null)
                .show();
    }
    
    private void openImagePicker() {
        if (!isAdded()) return;
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }
    
    private boolean checkPermission() {
        return getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            uploadProfileImage(data.getData());
        }
    }
    
    private void uploadProfileImage(Uri imageUri) {
        if (currentUserId == null || !isAdded() || getContext() == null) return;
        
        Log.d(TAG, "Starting profile image upload to Cloudinary");
        
        // Use Cloudinary for upload
        com.example.kitchenbrain.utils.CloudinaryHelper.INSTANCE.uploadImage(
            requireContext(),
            imageUri,
            url -> {
                // Success - update Firestore
                Log.d(TAG, "Image uploaded successfully: " + url);
                db.collection("users").document(currentUserId).update("avatarUrl", url)
                        .addOnSuccessListener(aVoid -> {
                            if (!isAdded()) return;
                            if (imageViewProfile != null) {
                                try {
                                    Glide.with(this).load(url).circleCrop().into(imageViewProfile);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error loading image with Glide", e);
                                }
                            }
                            showSnackbar("Profile image updated successfully");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update avatar URL", e);
                            showSnackbar("Failed to update database: " + e.getMessage());
                        });
            },
            error -> {
                // Error
                Log.e(TAG, "Upload failed: " + error);
                showSnackbar("Upload failed: " + error);
            },
            null
        );
    }
    
    private void setupRecyclerView() {
        if (getContext() == null) return;
        userPosts = new ArrayList<>();
        postAdapter = new PostAdapter(userPosts, this);
        if (recyclerViewPosts != null) {
            recyclerViewPosts.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewPosts.setAdapter(postAdapter);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Don't reload data - Firestore listeners are already active
        Log.d(TAG, "onResume - no reload needed (listeners active)");
    }
    
    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called - cleaning up listeners");
        super.onDestroyView();
        
        // Remove Firestore listeners to prevent memory leaks and crashes
        if (userDataListener != null) {
            userDataListener.remove();
            userDataListener = null;
            Log.d(TAG, "User data listener removed");
        }
        
        if (userRecipesListener != null) {
            userRecipesListener.remove();
            userRecipesListener = null;
            Log.d(TAG, "User recipes listener removed");
        }
        
        // Clear references to prevent memory leaks
        currentUserId = null;
        currentUser = null;
        postAdapter = null;
        userPosts = null;
        
        Log.d(TAG, "onDestroyView completed - all resources cleaned up");
    }
    
    private void showSnackbar(String message) {
        if (!isAdded() || getView() == null) return;
        Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
    }
}
