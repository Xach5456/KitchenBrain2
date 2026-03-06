package com.example.kitchenbrain;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PERMISSION_REQUEST_CODE = 200;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    
    private ImageView imageViewProfile;
    private TextView textViewUsername;
    private TextView textViewEmail;
    private Button buttonEditProfile;
    private Button buttonLogout;
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Recipe> userPosts;
    private User currentUser;
    private String currentUserId;
    
    private SwitchMaterial switchMessaging;
    private SwitchMaterial switchLanguage;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        setupRecyclerView();
        loadUserData();
        setupClickListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        Log.d(TAG, "initViews called");
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        
        imageViewProfile = view.findViewById(R.id.imageViewProfile);
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts);
        
        switchMessaging = view.findViewById(R.id.switchMessaging);
        switchLanguage = view.findViewById(R.id.switchLanguage);
        
        // Check if user is authenticated before getting UID
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            Log.d(TAG, "Current user ID: " + currentUserId);
        } else {
            Log.e(TAG, "User not authenticated");
            // Show error message and potentially navigate away
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please log in to view profile", Toast.LENGTH_LONG).show();
            }
            // In a real app, you might want to redirect to login
            return;
        }
    }
    
    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView called");
        
        if (getContext() == null) {
            Log.e(TAG, "Context is null in setupRecyclerView");
            return;
        }
        
        userPosts = new ArrayList<>();
        postAdapter = new PostAdapter(userPosts, this); // Pass fragment reference to handle navigation
        if (recyclerViewPosts != null) {
            recyclerViewPosts.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewPosts.setAdapter(postAdapter);
        } else {
            Log.e(TAG, "recyclerViewPosts is null");
        }
    }
    
    private void loadUserData() {
        Log.d(TAG, "loadUserData called with currentUserId: " + currentUserId);
        
        if (currentUserId == null) {
            Log.e(TAG, "currentUserId is null, cannot load user data");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Unable to load user data - not authenticated", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        if (getContext() == null) {
            Log.e(TAG, "Context is null in loadUserData");
            return;
        }
        
        // Load user profile data
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    Log.d(TAG, "Successfully loaded user data");
                    
                    if (getContext() == null) {
                        Log.w(TAG, "Context became null after async operation");
                        return;
                    }

                    if (isAdded() && document.exists()) { // Check if fragment is still added to activity
                        currentUser = document.toObject(User.class);
                        if (currentUser != null) {
                            if (textViewUsername != null && currentUser.getUsername() != null) {
                                textViewUsername.setText(currentUser.getUsername());
                            }
                            if (textViewEmail != null && mAuth.getCurrentUser() != null) {
                                textViewEmail.setText(mAuth.getCurrentUser().getEmail());
                            }
                            
                            if (imageViewProfile != null) {
                                if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                                    try {
                                        Glide.with(this) // Use fragment context
                                                .load(currentUser.getAvatarUrl())
                                                .placeholder(R.drawable.ic_default_avatar)
                                                .into(imageViewProfile);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error loading image with Glide", e);
                                        imageViewProfile.setImageResource(R.drawable.ic_default_avatar);
                                    }
                                } else {
                                    imageViewProfile.setImageResource(R.drawable.ic_default_avatar);
                                }
                            }
                        } else {
                            Log.w(TAG, "User object is null from document");
                        }
                    } else {
                        Log.w(TAG, "Fragment not added to activity or document doesn't exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        
        // Load user's recipes/posts
        loadUserRecipes();
        
        // Load privacy settings
        loadPrivacySettings();
    }
    
    private void loadUserRecipes() {
        Log.d(TAG, "loadUserRecipes called");
        
        if (currentUserId == null) {
            Log.e(TAG, "Cannot load recipes - currentUserId is null");
            return;
        }
        
        if (getContext() == null) {
            Log.e(TAG, "Context is null in loadUserRecipes");
            return;
        }
        
        db.collection("recipes")
                .whereEqualTo("authorId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully loaded user recipes: " + queryDocumentSnapshots.size());
                    
                    if (getContext() == null || !isAdded()) {
                        Log.w(TAG, "Context is null or fragment not added after async operation");
                        return;
                    }
                    
                    if (userPosts != null) {
                        userPosts.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Recipe recipe = document.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe.setId(document.getId());
                                userPosts.add(recipe);
                                Log.d(TAG, "Added recipe: " + recipe.getName());
                            }
                        }
                        if (postAdapter != null) {
                            postAdapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load recipes", e);
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Failed to load posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadPrivacySettings() {
        Log.d(TAG, "loadPrivacySettings called");
        
        if (getContext() == null) {
            Log.e(TAG, "Context is null in loadPrivacySettings");
            return;
        }
        
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("privacy_settings", 0);
            boolean messagingDisabled = prefs.getBoolean("messaging_disabled", false);
            if (switchMessaging != null) {
                switchMessaging.setChecked(!messagingDisabled); // Inverted because switch is ON when messaging is enabled
            }
            
            // For simplicity, we'll assume language is English by default
            boolean isRussian = prefs.getBoolean("language_russian", false);
            if (switchLanguage != null) {
                switchLanguage.setChecked(isRussian);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading privacy settings", e);
        }
    }
    
    private void savePrivacySettings() {
        Log.d(TAG, "savePrivacySettings called");
        
        if (getContext() == null) {
            Log.e(TAG, "Context is null in savePrivacySettings");
            return;
        }
        
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("privacy_settings", 0);
            SharedPreferences.Editor editor = prefs.edit();
            
            // Messaging is disabled when switch is OFF
            if (switchMessaging != null) {
                editor.putBoolean("messaging_disabled", !switchMessaging.isChecked());
            }
            
            // Language setting
            if (switchLanguage != null) {
                editor.putBoolean("language_russian", switchLanguage.isChecked());
            }
            
            editor.apply();
            
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "Settings saved", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving privacy settings", e);
        }
    }
    
    private void setupClickListeners() {
        Log.d(TAG, "setupClickListeners called");
        
        if (imageViewProfile != null) {
            imageViewProfile.setOnClickListener(v -> {
                if (isAdded() && getContext() != null) { // Make sure fragment is still active
                    if (checkPermission()) {
                        openImagePicker();
                    } else {
                        requestPermission();
                    }
                }
            });
        }
        
        if (buttonEditProfile != null) {
            buttonEditProfile.setOnClickListener(v -> {
                if (isAdded() && getContext() != null) {
                    showEditProfileDialog();
                }
            });
        }
        
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> {
                if (isAdded() && getContext() != null) {
                    logout();
                }
            });
        }
        
        if (switchMessaging != null) {
            switchMessaging.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isAdded() && getContext() != null) {
                    savePrivacySettings();
                }
            });
        }
        
        if (switchLanguage != null) {
            switchLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isAdded() && getContext() != null) {
                    savePrivacySettings();
                }
            });
        }
    }
    
    private void openImagePicker() {
        Log.d(TAG, "openImagePicker called");
        
        if (getContext() == null || !isAdded()) {
            Log.e(TAG, "Context is null or fragment not added in openImagePicker");
            return;
        }
        
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }
    
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission called");
        
        if (getContext() == null) {
            Log.e(TAG, "Context is null in checkPermission");
            return false;
        }
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestPermission() {
        Log.d(TAG, "requestPermission called");
        
        if (requireActivity() == null) {
            Log.e(TAG, "requireActivity is null in requestPermission");
            return;
        }
        ActivityCompat.requestPermissions(requireActivity(), 
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult called with requestCode: " + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageToFirebase(imageUri);
        }
    }
    
    private void uploadImageToFirebase(Uri imageUri) {
        Log.d(TAG, "uploadImageToFirebase called");
        
        if (currentUserId == null) {
            Log.e(TAG, "Cannot upload image - currentUserId is null");
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        if (getContext() == null || !isAdded()) {
            Log.e(TAG, "Context is null or fragment not added in uploadImageToFirebase");
            return;
        }
        
        StorageReference fileRef = storageRef.child("profile_images/" + currentUserId + ".jpg");
        
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully");
                    
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        
                        // Update user profile with new image URL
                        db.collection("users").document(currentUserId)
                                .update("avatarUrl", imageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Profile image URL updated in database");
                                    
                                    if (getContext() != null && isAdded() && imageViewProfile != null) {
                                        try {
                                            Glide.with(this)
                                                    .load(imageUrl)
                                                    .into(imageViewProfile);
                                            Toast.makeText(getContext(), "Profile image updated", Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error updating image with Glide", e);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update avatar URL in database", e);
                                    if (getContext() != null && isAdded()) {
                                        Toast.makeText(getContext(), "Failed to update profile image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }).addOnFailureListener(uriException -> {
                        Log.e(TAG, "Failed to get download URL", uriException);
                        if (getContext() != null && isAdded()) {
                            Toast.makeText(getContext(), "Failed to get image URL: " + uriException.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image", e);
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void showEditProfileDialog() {
        Log.d(TAG, "showEditProfileDialog called");
        
        if (getContext() == null || !isAdded()) {
            Log.e(TAG, "Context is null or fragment not added in showEditProfileDialog");
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Profile");
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText editTextUsername = dialogView.findViewById(R.id.editTextUsername);
        if (textViewUsername != null && editTextUsername != null) {
            editTextUsername.setText(textViewUsername.getText());
        }
        
        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            if (editTextUsername != null) {
                String newUsername = editTextUsername.getText().toString().trim();
                if (!newUsername.isEmpty()) {
                    updateUsername(newUsername);
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        try {
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog", e);
        }
    }
    
    private void updateUsername(String newUsername) {
        Log.d(TAG, "updateUsername called with: " + newUsername);
        
        if (currentUserId == null) {
            Log.e(TAG, "Cannot update username - currentUserId is null");
            if (getContext() != null && isAdded()) {
                Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        if (getContext() == null || !isAdded()) {
            Log.e(TAG, "Context is null or fragment not added in updateUsername");
            return;
        }
        
        db.collection("users").document(currentUserId)
                .update("username", newUsername)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Username updated successfully");
                    
                    if (textViewUsername != null) {
                        textViewUsername.setText(newUsername);
                    }
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Username updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update username", e);
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Failed to update username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    // Метод для выхода из аккаунта
    private void logout() {
        Log.d(TAG, "logout called");
        
        // Check if we're in guest mode
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null && mainActivity.isGuestMode()) {
                // For guest mode, just go back to login without updating online status
                performSignOut();
                return;
            }
        }
        
        // Update user's online status to offline before signing out (for authenticated users)
        if (db != null && currentUserId != null) {
            db.collection("users").document(currentUserId)
                    .update("online", false, "lastSeen", Timestamp.now())
                    .addOnCompleteListener(task -> {
                        Log.d(TAG, "Online status updated, proceeding with sign out");
                        // Regardless of success or failure, proceed with sign out
                        performSignOut();
                    });
        } else {
            performSignOut();
        }
    }
    
    private void performSignOut() {
        Log.d(TAG, "performSignOut called");
        
        if (mAuth != null) {
            mAuth.signOut(); // Sign out from Firebase
        }
        
        // Navigate to login screen
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish(); // Close current activity
        }
    }
    
    // RecyclerView adapter for user posts
    public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
        private List<Recipe> recipes;
        private Fragment parentFragment; // Keep reference to parent fragment
        
        public PostAdapter(List<Recipe> recipes, Fragment parentFragment) {
            this.recipes = recipes;
            this.parentFragment = parentFragment;
        }
        
        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
            return new PostViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            if (recipes != null && position >= 0 && position < recipes.size()) {
                Recipe recipe = recipes.get(position);
                
                if (holder.textViewPostTitle != null && recipe != null && recipe.getName() != null) {
                    holder.textViewPostTitle.setText(recipe.getName());
                }
                if (holder.textViewPostDescription != null && recipe != null && recipe.getDescription() != null) {
                    holder.textViewPostDescription.setText(recipe.getDescription());
                }
                
                if (holder.imageViewPost != null && recipe != null) {
                    if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                        try {
                            Glide.with(parentFragment) // Use parent fragment context
                                    .load(recipe.getImageUrl())
                                    .placeholder(R.drawable.ic_placeholder)
                                    .into(holder.imageViewPost);
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading recipe image with Glide", e);
                            holder.imageViewPost.setImageResource(R.drawable.ic_placeholder);
                        }
                    } else {
                        holder.imageViewPost.setImageResource(R.drawable.ic_placeholder);
                    }
                }
            }
        }
        
        @Override
        public int getItemCount() {
            return recipes != null ? recipes.size() : 0;
        }
        
        public class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView imageViewPost;
            TextView textViewPostTitle;
            TextView textViewPostDescription;
            
            public PostViewHolder(@NonNull View itemView) {
                super(itemView);
                imageViewPost = itemView.findViewById(R.id.imageViewPost);
                textViewPostTitle = itemView.findViewById(R.id.textViewPostTitle);
                textViewPostDescription = itemView.findViewById(R.id.textViewPostDescription);
                
                // Add click listener to open recipe details
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && 
                        recipes != null && 
                        position >= 0 && 
                        position < recipes.size()) {
                        
                        Recipe recipe = recipes.get(position);
                        if (recipe != null && parentFragment.getActivity() != null && parentFragment.isAdded()) {
                            // Open recipe detail fragment
                            RecipeDetailFragment detailFragment = RecipeDetailFragment.newInstance(recipe);
                            if (detailFragment != null) {
                                try {
                                    parentFragment.getActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragment_container, detailFragment)
                                        .addToBackStack(null)
                                        .commitAllowingStateLoss(); // Use commitAllowingStateLoss to prevent crashes
                                } catch (IllegalStateException e) {
                                    Log.e(TAG, "Error committing fragment transaction", e);
                                    // Alternative: use post to delay the transaction
                                    itemView.post(() -> {
                                        if (!parentFragment.isRemoving() && !parentFragment.isDetached()) {
                                            parentFragment.getActivity().getSupportFragmentManager()
                                                .beginTransaction()
                                                .replace(R.id.fragment_container, detailFragment)
                                                .addToBackStack(null)
                                                .commitAllowingStateLoss();
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
            }
        }
    }
}