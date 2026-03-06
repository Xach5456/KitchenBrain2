package com.example.kitchenbrain;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewUsers;
    private RecyclerView recyclerViewRecipes; // New RecyclerView for recipes
    private UserListAdapter userListAdapter;
    private RecipeSuggestionsAdapter recipeSuggestionsAdapter; // New adapter for recipes
    private List<User> userList;
    private List<Recipe> recipeList; // New list for recipes
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private FriendManager friendManager;
    private EditText editTextSearch;
    private Button buttonSearch;
    private Button buttonToggleRecipes; // Button to toggle showing other users' recipes

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        // Only continue if initialization was successful
        if (currentUserId != null && friendManager != null) {
            setupRecyclerView();
            loadUsers();
            loadRecipes(); // New method to load recipes
            setupSearchFunctionality();
            setupRecipeToggleFunctionality(); // New method to toggle recipe visibility
        }

        return view;
    }

    private void initViews(View view) {
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        recyclerViewRecipes = view.findViewById(R.id.recyclerViewRecipes); // New RecyclerView
        editTextSearch = view.findViewById(R.id.editTextSearch);
        buttonSearch = view.findViewById(R.id.buttonSearch);
        buttonToggleRecipes = view.findViewById(R.id.buttonToggleRecipes); // New button

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        FirebaseUser currentUser = auth.getCurrentUser();
        
        // Check if we're in guest mode by seeing if there's no current user
        if (currentUser != null) {
            // Regular authenticated user
            currentUserId = currentUser.getUid();
            friendManager = new FriendManager(db, currentUserId);
        } else {
            // Check if we're in guest mode by checking if we're in MainActivity and it's in guest mode
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.isGuestMode()) {
                    // In guest mode - create a placeholder ID or handle differently
                    currentUserId = "guest_" + System.currentTimeMillis();
                    // Don't initialize FriendManager for guest mode
                    // Show message about guest mode limitations
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Guest mode: Following features are limited", Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Not authenticated and not in guest mode - redirect to login
                    Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                        getActivity().finish();
                    }
                    return;
                }
            }
        }
    }

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        recipeList = new ArrayList<>(); // Initialize recipe list
        
        // Pass friendManager if available, otherwise pass null for guest mode
        userListAdapter = new UserListAdapter(userList, friendManager, new UserListAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                // Navigate to other user profile fragment
                if (getContext() != null && getContext() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getContext();
                    
                    // Create bundle with user ID
                    Bundle args = new Bundle();
                    args.putString("target_user_id", user.getUserId());
                    
                    // Create and show fragment
                    OtherUserProfileFragment fragment = new OtherUserProfileFragment();
                    fragment.setArguments(args);
                    
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack("other_user_profile")
                            .commit();
                }
            }
        });
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userListAdapter);
        
        // Setup recipe suggestions adapter
        recipeSuggestionsAdapter = new RecipeSuggestionsAdapter(recipeList, new RecipeSuggestionsAdapter.OnRecipeClickListener() {
            @Override
            public void onDeleteRecipe(Recipe recipe) {
                // Handle recipe deletion (only for user's own recipes)
                deleteRecipe(recipe);
            }

            @Override
            public void onHideRecipe(Recipe recipe) {
                // Hide recipe for current user
                if (recipe != null && recipe.getId() != null) {
                    recipeSuggestionsAdapter.hideRecipe(recipe.getId());
                    Toast.makeText(getContext(), "Recipe hidden from your view", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onRecipeClick(Recipe recipe) {
                // Navigate to recipe details
                if (getContext() != null && getContext() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getContext();
                    
                    // Create bundle with recipe ID
                    Bundle args = new Bundle();
                    args.putString("recipe_id", recipe.getId());
                    
                    // Create and show recipe details fragment
                    // Assuming there's a recipe details fragment
                    // RecipeDetailFragment fragment = new RecipeDetailFragment();
                    // fragment.setArguments(args);
                    // 
                    // activity.getSupportFragmentManager()
                    //         .beginTransaction()
                    //         .replace(R.id.fragment_container, fragment)
                    //         .addToBackStack("recipe_detail")
                    //         .commit();
                }
            }
        });
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)); // Horizontal scrolling for recipe suggestions
        recyclerViewRecipes.setAdapter(recipeSuggestionsAdapter);
    }

    private void loadUsers() {
        // Load all users except the current user
        if (db == null || currentUserId == null) {
            Toast.makeText(getContext(), "Database or user not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        db.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        getActivity().runOnUiThread(() -> {
                            userList.clear();
                            for (DocumentChange dc : value.getDocumentChanges()) {
                                User user = dc.getDocument().toObject(User.class);
                                String userId = dc.getDocument().getId();
                                
                                // Check if user exists and is not deleted (has username field)
                                if (user != null && 
                                    !currentUserId.equals(userId) &&  // Don't show current user
                                    user.getUsername() != null &&     // Filter out deleted users (no username)
                                    !user.getUsername().isEmpty()) {  // Filter out deleted users (empty username)
                                    
                                    user.setUserId(userId);
                                    userList.add(user);
                                }
                            }
                            userListAdapter.notifyDataSetChanged();
                        });
                    }
                });
    }
    
    private void loadRecipes() {
        // Load recipes from all users except the current user
        if (db == null || currentUserId == null) {
            Toast.makeText(getContext(), "Database or user not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        db.collection("recipes")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading recipes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        getActivity().runOnUiThread(() -> {
                            recipeList.clear();
                            for (DocumentChange dc : value.getDocumentChanges()) {
                                Recipe recipe = dc.getDocument().toObject(Recipe.class);
                                String recipeId = dc.getDocument().getId();
                                
                                // Check if recipe exists and is not deleted
                                if (recipe != null && recipe.getName() != null && !recipe.getName().isEmpty()) {
                                    recipe.setId(recipeId);
                                    recipeList.add(recipe);
                                }
                            }
                            
                            // Load user's hidden recipes to filter them out
                            recipeSuggestionsAdapter.loadHiddenRecipes();
                            recipeSuggestionsAdapter.updateRecipes(recipeList);
                        });
                    }
                });
    }
    
    private void setupRecipeToggleFunctionality() {
        if (buttonToggleRecipes != null) {
            buttonToggleRecipes.setOnClickListener(v -> {
                if (recipeSuggestionsAdapter != null) {
                    boolean currentlyShowing = recipeSuggestionsAdapter.areOtherUsersRecipesShown();
                    recipeSuggestionsAdapter.setShowOtherUsersRecipes(!currentlyShowing);
                    buttonToggleRecipes.setText(!currentlyShowing ? "Hide Other Recipes" : "Show Other Recipes");
                }
            });
        }
    }

    private void setupSearchFunctionality() {
        buttonSearch.setOnClickListener(v -> {
            if (db == null || currentUserId == null) {
                Toast.makeText(getContext(), "Database or user not initialized", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String searchTerm = editTextSearch.getText().toString().trim();
            if (!searchTerm.isEmpty()) {
                // Search for users by username
                db.collection("users")
                        .whereGreaterThanOrEqualTo("username", searchTerm.toLowerCase())
                        .whereLessThanOrEqualTo("username", searchTerm.toLowerCase() + "\uf8ff")
                        .addSnapshotListener((value, error) -> {
                            if (error != null) {
                                Toast.makeText(getContext(), "Error searching users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (value != null) {
                                getActivity().runOnUiThread(() -> {
                                    userList.clear();
                                    for (DocumentChange dc : value.getDocumentChanges()) {
                                        User user = dc.getDocument().toObject(User.class);
                                        String userId = dc.getDocument().getId();
                                        
                                        // Check if user exists and is not deleted
                                        if (user != null && 
                                            !currentUserId.equals(userId) &&  // Don't show current user
                                            user.getUsername() != null &&     // Filter out deleted users (no username)
                                            !user.getUsername().isEmpty()) {  // Filter out deleted users (empty username)
                                            
                                            user.setUserId(userId);
                                            userList.add(user);
                                        }
                                    }
                                    userListAdapter.notifyDataSetChanged();
                                });
                            }
                        });
            } else {
                // Load all users if search term is empty
                loadUsers();
            }
        });
    }
    
    private void deleteRecipe(Recipe recipe) {
        if (recipe != null && recipe.getId() != null && db != null) {
            // Only allow deletion of user's own recipes
            if (currentUserId != null && currentUserId.equals(recipe.getAuthorId())) {
                db.collection("recipes").document(recipe.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Recipe deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error deleting recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(getContext(), "You can only delete your own recipes", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Adapter for the user list
    public static class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserListViewHolder> {
        private List<User> userList;
        private FriendManager friendManager;
        private OnUserClickListener listener;

        public interface OnUserClickListener {
            void onUserClick(User user);
        }

        public UserListAdapter(List<User> userList, FriendManager friendManager, OnUserClickListener listener) {
            this.userList = userList;
            this.friendManager = friendManager;
            this.listener = listener;
        }

        @NonNull
        @Override
        public UserListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user, parent, false);
            return new UserListViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserListViewHolder holder, int position) {
            User user = userList.get(position);
            holder.textViewUsername.setText(user.getUsername() != null ? user.getUsername() : "Unknown User");
            
            // Update button status based on follow status
            holder.updateButtonStatus(user.getUserId(), friendManager);
            
            // Set click listener to view user profile
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }

        @Override
        public int getItemCount() {
            return userList != null ? userList.size() : 0;
        }

        static class UserListViewHolder extends RecyclerView.ViewHolder {
            TextView textViewUsername;
            Button buttonFollow;

            public UserListViewHolder(@NonNull View itemView) {
                super(itemView);
                
                textViewUsername = itemView.findViewById(R.id.textViewUsername);
                buttonFollow = itemView.findViewById(R.id.buttonFollow);
            }
            
            public void updateButtonStatus(String userId, FriendManager friendManager) {
                // Check if the current user follows this user
                if (friendManager != null) {
                    friendManager.isFollowing(userId)
                            .addOnSuccessListener(isFollowing -> {
                                if (isFollowing) {
                                    buttonFollow.setText("Unfollow");
                                    buttonFollow.setBackgroundResource(R.drawable.button_style); // Use a different style if desired
                                } else {
                                    buttonFollow.setText("Follow");
                                    buttonFollow.setBackgroundResource(R.drawable.button_style);
                                }
                                
                                buttonFollow.setEnabled(true);
                                buttonFollow.setOnClickListener(v -> {
                                    buttonFollow.setEnabled(false); // Prevent multiple clicks
                                    
                                    if (isFollowing) {
                                        // Unfollow the user
                                        friendManager.unfollowUser(userId)
                                                .addOnSuccessListener(aVoid -> {
                                                    buttonFollow.setText("Follow");
                                                    Toast.makeText(itemView.getContext(), "Unfollowed user", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(itemView.getContext(), "Error unfollowing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    buttonFollow.setEnabled(true); // Re-enable on error
                                                });
                                    } else {
                                        // Follow the user
                                        friendManager.followUser(userId)
                                                .addOnSuccessListener(aVoid -> {
                                                    buttonFollow.setText("Unfollow");
                                                    Toast.makeText(itemView.getContext(), "Followed user", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(itemView.getContext(), "Error following: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    buttonFollow.setEnabled(true); // Re-enable on error
                                                });
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                buttonFollow.setText("Follow");
                                buttonFollow.setEnabled(true);
                            });
                } else {
                    // Handle null friend manager
                    buttonFollow.setText("Follow");
                    buttonFollow.setEnabled(false);
                }
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up FriendManager resources
        if (friendManager != null) {
            friendManager.cleanup();
        }
    }
}