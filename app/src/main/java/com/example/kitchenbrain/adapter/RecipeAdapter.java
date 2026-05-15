package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.Recipe;
import com.example.kitchenbrain.RecipeDetailFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RecipeAdapter extends ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder> {

    private Context context;
    private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe, int position);
        void onFavoriteClick(Recipe recipe, int position);
    }

    public RecipeAdapter(List<Recipe> recipeList, Context context, OnRecipeClickListener listener) {
        super(new RecipeDiffCallback());
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);
        submitList(recipeList != null ? recipeList : new ArrayList<>());
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // COMPACT LAYOUT FOR MATCHING RECIPES
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_compact, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = getItem(position);
        
        // 🔥 MODERN COMPACT PREVIEW - clean info
        holder.txtTitle.setText(recipe.getTitle() != null ? recipe.getTitle() : "Unknown Recipe");
        
        // Time info with icon
        long cookingTime = recipe.getCookingTime();
        if (cookingTime > 0) {
            holder.txtTime.setText("⏱ " + cookingTime + " min");
            holder.txtTime.setVisibility(View.VISIBLE);
        } else {
            holder.txtTime.setVisibility(View.GONE);
        }
        
        // 🔥 ACTUAL MATCH DATA FROM METADATA
        Map<String, Object> metadata = recipe.getMetadata();
        
        // Ingredients match count
        if (metadata != null && metadata.containsKey("matchScore")) {
            Object score = metadata.get("matchScore");
            holder.txtIngredients.setText("🥬 " + score + " matching");
            holder.txtIngredients.setVisibility(View.VISIBLE);
        } else {
            // Fallback to total count if no match score
            List<String> ingredients = recipe.getIngredients();
            if (ingredients != null && !ingredients.isEmpty()) {
                holder.txtIngredients.setText("🥬 " + ingredients.size() + " items");
                holder.txtIngredients.setVisibility(View.VISIBLE);
            } else {
                holder.txtIngredients.setVisibility(View.GONE);
            }
        }
        
        // Match percentage
        if (metadata != null && metadata.containsKey("matchPercentage")) {
            Object percentage = metadata.get("matchPercentage");
            holder.txtMatch.setText("⭐ " + percentage + "% match");
            holder.txtMatch.setVisibility(View.VISIBLE);
        } else {
            holder.txtMatch.setVisibility(View.GONE);
        }
        
        // Load image using Glide
        int imageResId = recipe.getImageResId();
        if (imageResId != 0) {
            Glide.with(context)
                    .load(imageResId)
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(holder.imgRecipe);
        } else {
            // Try imageUrl if resId is 0
            String imageUrl = recipe.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(holder.imgRecipe);
            } else {
                holder.imgRecipe.setImageResource(R.drawable.ic_placeholder);
            }
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe, position);
            }
            
            // 🔥 OPEN RECIPE DETAIL FRAGMENT
            openRecipeDetail(recipe);
        });
    }

    private void openRecipeDetail(Recipe recipe) {
        try {
            RecipeDetailFragment detailFragment = RecipeDetailFragment.newInstance(recipe);
            if (context instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailFragment, "recipe_detail")
                        .addToBackStack(null)
                        .commit();
                android.util.Log.d("RecipeAdapter", "🔥 Opening recipe detail: " + recipe.getTitle());
            }
        } catch (Exception e) {
            android.util.Log.e("RecipeAdapter", "Error opening recipe detail", e);
        }
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRecipe;
        TextView txtTitle;
        TextView txtTime;
        TextView txtIngredients;
        TextView txtMatch;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRecipe = itemView.findViewById(R.id.imgRecipe);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtIngredients = itemView.findViewById(R.id.txtIngredients);
            txtMatch = itemView.findViewById(R.id.txtMatch);
        }
    }

    private static class RecipeDiffCallback extends DiffUtil.ItemCallback<Recipe> {
        @Override
        public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                   Objects.equals(oldItem.getCookingTime(), newItem.getCookingTime()) &&
                   Objects.equals(oldItem.getMetadata(), newItem.getMetadata()) &&
                   Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl());
        }
    }

    public void updateData(List<Recipe> newRecipes) {
        submitList(newRecipes != null ? new ArrayList<>(newRecipes) : new ArrayList<>());
    }
}
