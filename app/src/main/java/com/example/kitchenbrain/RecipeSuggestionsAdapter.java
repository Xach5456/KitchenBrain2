package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.kitchenbrain.util.RecipeOwnershipUtils;

import java.util.List;
import java.util.ArrayList;

public class RecipeSuggestionsAdapter extends RecyclerView.Adapter<RecipeSuggestionsAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private List<Recipe> filteredRecipeList;
    private OnRecipeClickListener listener;
    private boolean allowEditing;
    private boolean showOtherUsersRecipes;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;

    public interface OnRecipeClickListener {
        void onDeleteRecipe(Recipe recipe);
        void onRecipeClick(Recipe recipe);
    }

    public RecipeSuggestionsAdapter(List<Recipe> recipeList, OnRecipeClickListener listener) {
        this(recipeList, listener, true);
    }
    
    public RecipeSuggestionsAdapter(List<Recipe> recipeList, OnRecipeClickListener listener, boolean allowEditing) {
        this.recipeList = recipeList != null ? recipeList : new ArrayList<>();
        this.filteredRecipeList = new ArrayList<>(this.recipeList);
        this.listener = listener;
        this.allowEditing = allowEditing;
        this.showOtherUsersRecipes = true;
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        updateFilteredRecipes();
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
        holder.bind(recipe, listener, allowEditing, currentUserId);
    }

    @Override
    public int getItemCount() {
        return filteredRecipeList.size();
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipeList = newRecipes != null ? newRecipes : new ArrayList<>();
        updateFilteredRecipes();
    }

    public void setShowOtherUsersRecipes(boolean show) {
        this.showOtherUsersRecipes = show;
        updateFilteredRecipes();
    }
    
    public boolean areOtherUsersRecipesShown() {
        return this.showOtherUsersRecipes;
    }

    private void updateFilteredRecipes() {
        filteredRecipeList.clear();
        for (Recipe recipe : recipeList) {
            if (shouldIncludeRecipe(recipe)) {
                filteredRecipeList.add(recipe);
            }
        }
        notifyDataSetChanged();
    }

    private boolean shouldIncludeRecipe(Recipe recipe) {
        if (recipe == null) return false;
        if (!showOtherUsersRecipes && currentUserId != null && !currentUserId.equals(recipe.getAuthorId())) {
            return false;
        }
        return true;
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private TextView recipeNameTextView;
        private TextView recipeTimeTextView;
        private TextView recipeAuthorTextView;
        private MaterialButton btnDeleteRecipe;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeNameTextView = itemView.findViewById(R.id.recipeName);
            recipeTimeTextView = itemView.findViewById(R.id.recipeTime);
            recipeAuthorTextView = itemView.findViewById(R.id.recipeAuthor);
            btnDeleteRecipe = itemView.findViewById(R.id.btnDeleteRecipe);
        }

        public void bind(Recipe recipe, OnRecipeClickListener listener, boolean allowEditing, String currentUserId) {
            if (recipe != null) {
                recipeNameTextView.setText(recipe.getName() != null ? recipe.getName() : "Unknown Recipe");
                recipeTimeTextView.setText(recipe.getCookingTime() > 0 ? recipe.getCookingTime() + " min" : "Time unknown");
                if (recipeAuthorTextView != null && recipe.getUsername() != null) {
                    recipeAuthorTextView.setText("by " + recipe.getUsername());
                    recipeAuthorTextView.setVisibility(View.VISIBLE);
                } else if (recipeAuthorTextView != null) {
                    recipeAuthorTextView.setVisibility(View.GONE);
                }
            }

            boolean isOwnRecipe = RecipeOwnershipUtils.isOwner(recipe, currentUserId);

            if (allowEditing && isOwnRecipe && recipe != null) {
                btnDeleteRecipe.setVisibility(View.VISIBLE);
                btnDeleteRecipe.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteRecipe(recipe);
                    }
                });
            } else {
                btnDeleteRecipe.setVisibility(View.GONE);
                btnDeleteRecipe.setOnClickListener(null);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null && recipe != null) {
                    listener.onRecipeClick(recipe);
                }
            });
        }
    }
}
