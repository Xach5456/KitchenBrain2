package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.SocialRecipe;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {
    private List<SocialRecipe> favoritesList;
    private Context context;

    public FavoritesAdapter(List<SocialRecipe> favoritesList, Context context) {
        this.favoritesList = favoritesList;
        this.context = context;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_recipe, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        SocialRecipe recipe = favoritesList.get(position);
        
        holder.titleTextView.setText(recipe.getTitle());
        holder.authorTextView.setText("by " + recipe.getAuthorName());
        
        // Load recipe image
        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.recipeImageView);
        } else {
            holder.recipeImageView.setImageResource(R.drawable.ic_placeholder);
        }
        
        // Set other details
        holder.cookTimeTextView.setText(recipe.getFormattedCookTime());
        holder.difficultyTextView.setText(recipe.getDifficulty());
        
        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            // TODO: Navigate to recipe detail
        });
    }

    @Override
    public int getItemCount() {
        return favoritesList.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView recipeImageView;
        TextView titleTextView;
        TextView authorTextView;
        TextView cookTimeTextView;
        TextView difficultyTextView;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImageView = itemView.findViewById(R.id.recipeImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            cookTimeTextView = itemView.findViewById(R.id.cookTimeTextView);
            difficultyTextView = itemView.findViewById(R.id.difficultyTextView);
        }
    }
}
