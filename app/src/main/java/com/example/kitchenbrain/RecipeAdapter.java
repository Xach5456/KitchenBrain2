package com.example.kitchenbrain;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.ArrayList;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private List<Recipe> filteredRecipeList;
    private OnRecipeClickListener listener;
    private boolean allowEditing;
    private boolean showOtherUsersRecipes; // New property to control showing other users' recipes
    private boolean requireMutualFollow; // New property to require mutual follow for viewing recipes
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<String> myFollowingList; // List of user IDs that current user follows
    private List<String> myFollowersList; // List of user IDs that follow current user

    public interface OnRecipeClickListener {
        void onDeleteRecipe(Recipe recipe);
        void onRecipeClick(Recipe recipe);
    }

    public RecipeAdapter(List<Recipe> recipeList, OnRecipeClickListener listener) {
        this(recipeList, listener, true); // Default to allow editing
    }
    
    public RecipeAdapter(List<Recipe> recipeList, OnRecipeClickListener listener, boolean allowEditing) {
        this.recipeList = recipeList != null ? recipeList : new ArrayList<>();
        this.filteredRecipeList = new ArrayList<>(this.recipeList);
        this.listener = listener;
        this.allowEditing = allowEditing;
        this.showOtherUsersRecipes = true; // By default, show other users' recipes
        this.requireMutualFollow = false; // By default, don't require mutual follow (backward compatible)
        this.myFollowingList = new ArrayList<>();
        this.myFollowersList = new ArrayList<>();
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        updateFilteredRecipes(); // Initialize filtered list
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        // Horizontal strip on Home: fixed card width so multiple recipes peek on screen.
        if (parent instanceof RecyclerView) {
            RecyclerView.LayoutManager lm = ((RecyclerView) parent).getLayoutManager();
            if (lm instanceof LinearLayoutManager
                    && ((LinearLayoutManager) lm).getOrientation() == LinearLayoutManager.HORIZONTAL) {
                int widthPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 280, parent.getResources().getDisplayMetrics());
                view.setLayoutParams(new RecyclerView.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = filteredRecipeList.get(position);
        holder.bind(recipe, listener, auth, allowEditing, currentUserId);
    }

    @Override
    public int getItemCount() {
        return filteredRecipeList.size();
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipeList = newRecipes != null ? newRecipes : new ArrayList<>();
        updateFilteredRecipes();
    }

    // Method to toggle showing other users' recipes
    public void setShowOtherUsersRecipes(boolean show) {
        this.showOtherUsersRecipes = show;
        updateFilteredRecipes();
    }
    
    // Method to enable/disable mutual follow requirement
    public void setRequireMutualFollow(boolean require) {
        this.requireMutualFollow = require;
        updateFilteredRecipes();
    }
    
    // Method to set the list of users that current user follows
    public void setMyFollowingList(List<String> followingList) {
        this.myFollowingList = followingList != null ? new ArrayList<>(followingList) : new ArrayList<>();
        updateFilteredRecipes();
    }
    
    // Method to set the list of users that follow current user
    public void setMyFollowersList(List<String> followersList) {
        this.myFollowersList = followersList != null ? new ArrayList<>(followersList) : new ArrayList<>();
        updateFilteredRecipes();
    }
    
    // Method to check if a specific user is being followed by current user
    public boolean amIFollowingUser(String userId) {
        return myFollowingList != null && myFollowingList.contains(userId);
    }
    
    // Method to check if a specific user follows current user
    public boolean isUserFollowingMe(String userId) {
        return myFollowersList != null && myFollowersList.contains(userId);
    }
    
    // Method to check if there's a mutual follow relationship with a specific user
    public boolean hasMutualFollowWithUser(String userId) {
        return amIFollowingUser(userId) && isUserFollowingMe(userId);
    }
    
    // Getter to check if other users' recipes are being shown
    public boolean areOtherUsersRecipesShown() {
        return this.showOtherUsersRecipes;
    }

    // Method to update the filtered list based on current settings
    private void updateFilteredRecipes() {
        filteredRecipeList.clear();
        for (Recipe recipe : recipeList) {
            // Include recipe if it passes all filters
            if (shouldIncludeRecipe(recipe)) {
                filteredRecipeList.add(recipe);
            }
        }
        notifyDataSetChanged();
    }

    // Helper method to determine if a recipe should be included in the filtered list
    private boolean shouldIncludeRecipe(Recipe recipe) {
        if (recipe == null) return false;

        // If showing other users' recipes is disabled, only show current user's recipes
        if (!showOtherUsersRecipes && currentUserId != null && !currentUserId.equals(recipe.getAuthorId())) {
            return false;
        }
        
        // If mutual follow is required, check if there's a mutual relationship
        if (requireMutualFollow && currentUserId != null && !currentUserId.equals(recipe.getAuthorId())) {
            String recipeAuthorId = recipe.getAuthorId();
            
            // Check if current user follows the recipe author AND recipe author follows current user
            boolean iFollowAuthor = myFollowingList != null && myFollowingList.contains(recipeAuthorId);
            boolean authorFollowsMe = myFollowersList != null && myFollowersList.contains(recipeAuthorId);
            
            // Only show recipe if there's mutual follow
            if (!iFollowAuthor || !authorFollowsMe) {
                return false;
            }
        }

        return true;
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private TextView recipeNameTextView;
        private TextView recipeTimeTextView;
        private TextView recipeAuthorTextView; // View for author name
        private ImageButton btnDeleteRecipe;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            
            recipeNameTextView = itemView.findViewById(R.id.recipeName);
            recipeTimeTextView = itemView.findViewById(R.id.recipeTime);
            recipeAuthorTextView = itemView.findViewById(R.id.recipeAuthor);
            btnDeleteRecipe = itemView.findViewById(R.id.btnDeleteRecipe);
        }

        public void bind(Recipe recipe, OnRecipeClickListener listener, FirebaseAuth auth, boolean allowEditing, String currentUserId) {
            // Safe null checks for recipe properties
            if (recipe != null) {
                recipeNameTextView.setText(recipe.getName() != null ? recipe.getName() : "Unknown Recipe");
                recipeTimeTextView.setText(recipe.getCookingTime() > 0 ? recipe.getCookingTime() + " min" : "Time unknown");
                                
                // Show author name if available
                if (recipeAuthorTextView != null && recipe.getUsername() != null) {
                    recipeAuthorTextView.setText("by " + recipe.getUsername());
                    recipeAuthorTextView.setVisibility(View.VISIBLE);
                } else {
                    recipeAuthorTextView.setVisibility(View.GONE);
                }
            }

            // CRITICAL FIX: Show delete button if editing is allowed OR if it's the user's own recipe
            String recipeAuthorId = recipe != null ? recipe.getAuthorId() : null;
            boolean isOwnRecipe = (currentUserId != null && recipeAuthorId != null &&
                                   currentUserId.equals(recipeAuthorId));
                        
            // FIXED: Show delete button if allowEditing is true OR if it's own recipe
            if ((allowEditing || isOwnRecipe) && recipe != null) {
                btnDeleteRecipe.setVisibility(View.VISIBLE);
                btnDeleteRecipe.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteRecipe(recipe);
                    }
                });
            } else {
                // ALWAYS hide delete button for other users' recipes
                btnDeleteRecipe.setVisibility(View.GONE);
                btnDeleteRecipe.setOnClickListener(null); // Clear any click listener
            }

            // Set click listener for recipe item
            itemView.setOnClickListener(v -> {
                if (listener != null && recipe != null) {
                    listener.onRecipeClick(recipe);
                }
            });
        }
    }
}