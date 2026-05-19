package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.Recipe;
import com.example.kitchenbrain.RecipeDetailFragment;
import com.example.kitchenbrain.util.RecipeOwnershipUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecipeAdapter extends ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder> {

    private Context context;
    private OnRecipeClickListener listener;
    private String currentUserId;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe, int position);
        void onFavoriteClick(Recipe recipe, int position);
        default void onDeleteRecipe(Recipe recipe) {}
        default void onEditRecipe(Recipe recipe) {}
    }

    public RecipeAdapter(List<Recipe> recipeList, Context context, OnRecipeClickListener listener) {
        super(new RecipeDiffCallback());
        this.context = context;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        submitList(recipeList != null ? recipeList : new ArrayList<>());
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
        Recipe recipe = getItem(position);
        
        holder.txtTitle.setText(recipe.getTitle() != null ? recipe.getTitle() : "Unknown Recipe");
        
        if (holder.txtDescription != null) {
            holder.txtDescription.setText(recipe.getDescription() != null ? recipe.getDescription() : "");
        }

        long cookingTime = recipe.getCookingTime();
        if (holder.txtTime != null) {
            if (cookingTime > 0) {
                holder.txtTime.setText(cookingTime + " min");
                holder.txtTime.setVisibility(View.VISIBLE);
            } else {
                holder.txtTime.setVisibility(View.GONE);
            }
        }
        
        if (holder.txtAuthor != null) {
            holder.txtAuthor.setText("by " + (recipe.getUsername() != null ? recipe.getUsername() : "Chef"));
            holder.txtAuthor.setVisibility(View.VISIBLE);
        }

        boolean isOwner = RecipeOwnershipUtils.isOwner(recipe, currentUserId);
        if (holder.layoutActions != null) {
            holder.layoutActions.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        }
        
        if (holder.btnDelete != null) {
            holder.btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            holder.btnDelete.setOnClickListener(v -> {
                if (isOwner && listener != null) listener.onDeleteRecipe(recipe);
            });
        }
        
        if (holder.btnEdit != null) {
            holder.btnEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            holder.btnEdit.setOnClickListener(v -> {
                if (isOwner && listener != null) listener.onEditRecipe(recipe);
            });
        }

        // Fix for Glide crash: Ensuring placeholder and using correct ID
        String imageUrl = recipe.getImageUrl();
        if (holder.imgRecipe != null) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_recipe)
                        .error(R.drawable.placeholder_recipe)
                        .into(holder.imgRecipe);
            } else {
                holder.imgRecipe.setImageResource(R.drawable.placeholder_recipe);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRecipeClick(recipe, position);
            openRecipeDetail(recipe);
        });
    }

    private void openRecipeDetail(Recipe recipe) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, RecipeDetailFragment.newInstance(recipe))
                    .addToBackStack(null)
                    .commit();
        }
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRecipe;
        TextView txtTitle, txtDescription, txtTime, txtAuthor;
        MaterialButton btnDelete, btnEdit;
        View layoutActions;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            // Corrected IDs matching item_recipe.xml
            imgRecipe = itemView.findViewById(R.id.recipeImage);
            txtTitle = itemView.findViewById(R.id.recipeName);
            txtDescription = itemView.findViewById(R.id.recipeDescription);
            txtTime = itemView.findViewById(R.id.recipeTime);
            txtAuthor = itemView.findViewById(R.id.recipeAuthor);
            btnDelete = itemView.findViewById(R.id.btnDeleteRecipe);
            btnEdit = itemView.findViewById(R.id.btnEditRecipe);
            layoutActions = itemView.findViewById(R.id.recipeActions);
        }
    }

    private static class RecipeDiffCallback extends DiffUtil.ItemCallback<Recipe> {
        @Override public boolean areItemsTheSame(@NonNull Recipe o, @NonNull Recipe n) { return Objects.equals(o.getId(), n.getId()); }
        @Override public boolean areContentsTheSame(@NonNull Recipe o, @NonNull Recipe n) { return o.equals(n); }
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        submitList(newRecipes != null ? new ArrayList<>(newRecipes) : new ArrayList<>());
    }
}
