package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RecipeSuggestionsAdapter extends RecyclerView.Adapter<RecipeSuggestionsAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private List<Recipe> filteredRecipeList;
    private OnRecipeClickListener listener;
    private boolean allowEditing;
    private boolean showOtherUsersRecipes; // Property to control showing other users' recipes
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;
    private Set<String> hiddenRecipeIds; // Track hidden recipes for current user

    public interface OnRecipeClickListener {
        void onDeleteRecipe(Recipe recipe);
        void onHideRecipe(Recipe recipe);
        void onRecipeClick(Recipe recipe);
    }

    public RecipeSuggestionsAdapter(List<Recipe> recipeList, OnRecipeClickListener listener) {
        this(recipeList, listener, true); // Default to allow editing
    }
    
    public RecipeSuggestionsAdapter(List<Recipe> recipeList, OnRecipeClickListener listener, boolean allowEditing) {
        this.recipeList = recipeList != null ? recipeList : new ArrayList<>();
        this.filteredRecipeList = new ArrayList<>(this.recipeList);
        this.listener = listener;
        this.allowEditing = allowEditing;
        this.showOtherUsersRecipes = true; // By default, show other users' recipes
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.hiddenRecipeIds = new HashSet<>();
        this.currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        updateFilteredRecipes(); // Initialize filtered list
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
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
        if (hiddenRecipeIds.contains(recipe.getId())) return false; // Skip hidden recipes

        // If showing other users' recipes is disabled, only show current user's recipes
        if (!showOtherUsersRecipes && currentUserId != null && !currentUserId.equals(recipe.getAuthorId())) {
            return false;
        }

        return true;
    }

    // Method to hide a recipe for the current user only
    public void hideRecipe(String recipeId) {
        if (recipeId != null) {
            hiddenRecipeIds.add(recipeId);
            updateFilteredRecipes(); // Refresh the list
            
            // Update user's hidden recipes in Firestore (so it persists across sessions)
            if (currentUserId != null && db != null) {
                db.collection("users").document(currentUserId)
                    .update("hiddenRecipes", FieldValue.arrayUnion(recipeId))
                    .addOnFailureListener(e -> {
                        // If update fails, remove from local set and retry
                        hiddenRecipeIds.remove(recipeId);
                        if (listener != null) {
                            Toast.makeText(null, "Failed to hide recipe", Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }
    }

    // Method to load user's hidden recipes from Firestore
    public void loadHiddenRecipes() {
        if (currentUserId != null && db != null) {
            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        List<String> userHiddenRecipes = (List<String>) document.get("hiddenRecipes");
                        if (userHiddenRecipes != null) {
                            hiddenRecipeIds.addAll(userHiddenRecipes);
                            updateFilteredRecipes();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Continue without loading hidden recipes
                });
        }
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private TextView recipeNameTextView;
        private TextView recipeTimeTextView;
        private TextView recipeAuthorTextView; // View for author name
        private ImageButton btnDeleteRecipe;
        private ImageButton btnHideRecipe; // Button to hide recipe

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            
            recipeNameTextView = itemView.findViewById(R.id.recipeName);
            recipeTimeTextView = itemView.findViewById(R.id.recipeTime);
            recipeAuthorTextView = itemView.findViewById(R.id.recipeAuthor);
            btnDeleteRecipe = itemView.findViewById(R.id.btnDeleteRecipe);
            btnHideRecipe = itemView.findViewById(R.id.btnHideRecipe);
        }

        public void bind(Recipe recipe, OnRecipeClickListener listener, FirebaseAuth auth, boolean allowEditing, String currentUserId) {
            // Safe null checks for recipe properties
            if (recipe != null) {
                recipeNameTextView.setText(recipe.getName() != null ? recipe.getName() : "Unknown Recipe");
                recipeTimeTextView.setText(recipe.getCookingTime() > 0 ? recipe.getCookingTime() + " min" : "Time unknown");
                                
                // Show author name if available
                if (recipeAuthorTextView != null && recipe.getUsername() != null) {
                    recipeAuthorTextView.setText("by " + recipe.getUsername());
                }
            }

            // Only show delete button if editing is allowed and it's the user's own recipe
            String recipeAuthorId = recipe != null ? recipe.getAuthorId() : null;
            if (allowEditing && recipe != null && currentUserId != null && recipeAuthorId != null && 
                currentUserId.equals(recipeAuthorId)) {
                btnDeleteRecipe.setVisibility(View.VISIBLE);
                btnDeleteRecipe.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteRecipe(recipe);
                    }
                });
            } else {
                btnDeleteRecipe.setVisibility(View.GONE);
            }

            // Show hide button for other users' recipes (but not for current user's own recipes)
            if (recipe != null && currentUserId != null && recipeAuthorId != null &&
                !currentUserId.equals(recipeAuthorId)) { // Only show hide button for other users' recipes
                btnHideRecipe.setVisibility(View.VISIBLE);
                btnHideRecipe.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onHideRecipe(recipe);
                    }
                });
            } else {
                btnHideRecipe.setVisibility(View.GONE);
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