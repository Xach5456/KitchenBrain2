package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.util.RecipeOwnershipUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying user's recipe posts in profile screen.
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    
    private List<Recipe> recipes;
    private Fragment parentFragment;
    private String currentUserId;
    private OnPostActionListener actionListener;

    public interface OnPostActionListener {
        void onViewRecipe(Recipe recipe);
        void onEditRecipe(Recipe recipe);
        void onDeleteRecipe(Recipe recipe);
    }

    public PostAdapter(List<Recipe> recipes, Fragment parentFragment, String currentUserId, OnPostActionListener actionListener) {
        this.recipes = recipes;
        this.parentFragment = parentFragment;
        this.currentUserId = currentUserId;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        if (recipes != null && position >= 0 && position < recipes.size()) {
            Recipe recipe = recipes.get(position);
            
            // Set title
            if (holder.textViewPostTitle != null && recipe != null) {
                holder.textViewPostTitle.setText(recipe.getTitle());
            }
            
            // Set description
            if (holder.textViewPostDescription != null && recipe != null) {
                holder.textViewPostDescription.setText(recipe.getDescription());
            }
            
            // Set image with Glide
            if (holder.imageViewPost != null && recipe != null) {
                if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                    try {
                        Glide.with(parentFragment)
                                .load(recipe.getImageUrl())
                                .placeholder(R.drawable.ic_placeholder)
                                .into(holder.imageViewPost);
                    } catch (Exception e) {
                        holder.imageViewPost.setImageResource(R.drawable.ic_placeholder);
                    }
                } else {
                    holder.imageViewPost.setImageResource(R.drawable.ic_placeholder);
                }
            }
            
            boolean isOwner = RecipeOwnershipUtils.isOwner(recipe, currentUserId);
            if (holder.layoutRecipeOwnerActions != null) {
                holder.layoutRecipeOwnerActions.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            }

            View.OnClickListener openClickListener = v -> {
                if (actionListener != null) actionListener.onViewRecipe(recipe);
            };
            holder.itemView.setOnClickListener(openClickListener);
            if (holder.buttonViewRecipe != null) {
                holder.buttonViewRecipe.setOnClickListener(openClickListener);
            }
            if (holder.buttonEditRecipe != null) {
                holder.buttonEditRecipe.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                holder.buttonEditRecipe.setOnClickListener(isOwner && actionListener != null
                        ? v -> actionListener.onEditRecipe(recipe)
                        : null);
            }
            if (holder.buttonDeleteRecipe != null) {
                holder.buttonDeleteRecipe.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                holder.buttonDeleteRecipe.setOnClickListener(isOwner && actionListener != null
                        ? v -> actionListener.onDeleteRecipe(recipe)
                        : null);
            }
        }
    }
    
    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        List<Recipe> oldRecipes = recipes != null ? new ArrayList<>(recipes) : new ArrayList<>();
        List<Recipe> nextRecipes = newRecipes != null ? new ArrayList<>(newRecipes) : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new RecipeDiffCallback(oldRecipes, nextRecipes));
        recipes = nextRecipes;
        diffResult.dispatchUpdatesTo(this);
    }

    private static class RecipeDiffCallback extends DiffUtil.Callback {
        private final List<Recipe> oldRecipes;
        private final List<Recipe> newRecipes;

        RecipeDiffCallback(List<Recipe> oldRecipes, List<Recipe> newRecipes) {
            this.oldRecipes = oldRecipes;
            this.newRecipes = newRecipes;
        }

        @Override
        public int getOldListSize() {
            return oldRecipes.size();
        }

        @Override
        public int getNewListSize() {
            return newRecipes.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Recipe oldRecipe = oldRecipes.get(oldItemPosition);
            Recipe newRecipe = newRecipes.get(newItemPosition);
            if (oldRecipe == null || newRecipe == null) return oldRecipe == newRecipe;
            String oldId = oldRecipe.getId();
            String newId = newRecipe.getId();
            return oldId != null && oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Recipe oldRecipe = oldRecipes.get(oldItemPosition);
            Recipe newRecipe = newRecipes.get(newItemPosition);
            if (oldRecipe == null || newRecipe == null) return oldRecipe == newRecipe;
            return String.valueOf(oldRecipe.getTitle()).equals(String.valueOf(newRecipe.getTitle()))
                    && String.valueOf(oldRecipe.getDescription()).equals(String.valueOf(newRecipe.getDescription()))
                    && String.valueOf(oldRecipe.getImageUrl()).equals(String.valueOf(newRecipe.getImageUrl()));
        }
    }
    
    /**
     * ViewHolder for recipe posts
     */
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPost;
        TextView textViewPostTitle;
        TextView textViewPostDescription;
        MaterialButton buttonViewRecipe;
        MaterialButton buttonEditRecipe;
        MaterialButton buttonDeleteRecipe;
        View layoutRecipeOwnerActions;
        
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPost = itemView.findViewById(R.id.imageViewPost);
            textViewPostTitle = itemView.findViewById(R.id.textViewPostTitle);
            textViewPostDescription = itemView.findViewById(R.id.textViewPostDescription);
            buttonViewRecipe = itemView.findViewById(R.id.buttonViewRecipe);
            buttonEditRecipe = itemView.findViewById(R.id.buttonEditRecipe);
            buttonDeleteRecipe = itemView.findViewById(R.id.buttonDeleteRecipe);
            layoutRecipeOwnerActions = itemView.findViewById(R.id.layoutRecipeOwnerActions);
        }
    }
}
