package com.example.kitchenbrain;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import java.util.List;

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
    
    private ShapeableImageView imageViewProfile;
    private TextView textViewUsername;
    private TextView textViewEmail;
    private TextView textFollowersCount;
    private TextView textFollowingCount;
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
        if (buttonChangePhoto != null) {
            buttonChangePhoto.setOnClickListener(v -> handleImagePickerClick());
        }
        
        if (buttonEditProfile != null) buttonEditProfile.setOnClickListener(v -> showEditUsernameDialog());
        if (layoutEditUsername != null) layoutEditUsername.setOnClickListener(v -> showEditUsernameDialog());
        if (layoutDeleteAccount != null) layoutDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
        if (layoutMessagePermissions != null) layoutMessagePermissions.setOnClickListener(v -> showMessagePermissionsDialog());
        if (layoutBlockedUsers != null) layoutBlockedUsers.setOnClickListener(v -> showBlockedUsersDialog());
        
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

        if (textFollowersCount != null) {
            ((View)textFollowersCount.getParent()).setOnClickListener(v -> openFriendsFragment(FriendsFragment.TAB_FOLLOWERS));
        }
        if (textFollowingCount != null) {
            ((View)textFollowingCount.getParent()).setOnClickListener(v -> openFriendsFragment(FriendsFragment.TAB_FOLLOWING));
        }
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
    
    private void updateUIWithUserData(User user) {
        if (!isAdded()) return;
        
        if (textViewUsername != null) textViewUsername.setText(user.getUsername());
        if (textCurrentUsername != null) textCurrentUsername.setText(user.getUsername());
        
        if (textViewEmail != null && mAuth.getCurrentUser() != null)
            textViewEmail.setText(mAuth.getCurrentUser().getEmail());

        if (textFollowersCount != null)
            textFollowersCount.setText(String.valueOf(user.getFollowersCount()));
        
        if (textFollowingCount != null)
            textFollowingCount.setText(String.valueOf(user.getFollowingCount()));
        
        if (imageViewProfile != null) {
            Glide.with(this)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(imageViewProfile);
        }
    }
    
    private void loadUserRecipes() {
        if (currentUserId == null || getContext() == null) return;
        
        userRecipesListener = db.collection("recipes").whereEqualTo("authorId", currentUserId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (!isAdded()) return;
                    
                    if (error != null) return;
                    
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
        if (switchDarkMode != null) switchDarkMode.setChecked(prefs.getBoolean("dark_mode", false));
        if (switchNotifications != null) switchNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        if (switchMessaging != null) switchMessaging.setChecked(prefs.getBoolean("messaging_enabled", true));
        
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
    }
    
    private void updateMessagingSetting(boolean enabled) {
        if (!isAdded() || currentUserId == null) return;
        db.collection("users").document(currentUserId).update("messagingEnabled", enabled);
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
        if (currentUser != null) editText.setText(currentUser.getUsername());
        
        new MaterialAlertDialogBuilder(requireContext())
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
        new MaterialAlertDialogBuilder(requireContext())
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
        new MaterialAlertDialogBuilder(requireContext())
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
        String[] options = {"Everyone", "Friends only", "Nobody"};
        String current = prefs.getString("message_permission", "everyone");
        int index = current.equals("friends") ? 1 : current.equals("nobody") ? 2 : 0;
        
        new MaterialAlertDialogBuilder(requireContext())
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
    
    private void showBlockedUsersDialog() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Blocked Users")
                .setMessage("No blocked users yet.")
                .setPositiveButton("OK", null)
                .show();
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
    public void onDestroyView() {
        super.onDestroyView();
        if (userDataListener != null) userDataListener.remove();
        if (userRecipesListener != null) userRecipesListener.remove();
    }
    
    private void showSnackbar(String message) {
        if (getView() != null) Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
    }
}
