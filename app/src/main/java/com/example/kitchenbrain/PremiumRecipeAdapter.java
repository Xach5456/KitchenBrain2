package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * Premium Recipe Adapter with DiffUtil
 * High-performance adapter with smooth animations and efficient image loading
 */
public class PremiumRecipeAdapter extends ListAdapter<Recipe, PremiumRecipeAdapter.PremiumViewHolder> {

    private OnRecipeInteractionListener listener;
    private String currentUserId;

    public interface OnRecipeInteractionListener {
        void onRecipeClick(Recipe recipe, int position);
        void onLikeClick(Recipe recipe, int position);
        void onSaveClick(Recipe recipe, int position);
        void onAuthorClick(String authorId);
    }

    public PremiumRecipeAdapter(OnRecipeInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    @NonNull
    @Override
    public PremiumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_premium, parent, false);
        return new PremiumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PremiumViewHolder holder, int position) {
        Recipe recipe = getItem(position);
        holder.bind(recipe, currentUserId, listener);
    }

    static class PremiumViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageRecipe;
        private ImageButton buttonSave;
        private TextView textRecipeTitle;
        private TextView textRecipeDescription;
        private TextView textCookingTime;
        private ShapeableImageView imageAuthorAvatar;
        private TextView textAuthor;
        private ImageButton buttonLike;
        private TextView textLikesCount;

        public PremiumViewHolder(@NonNull View itemView) {
            super(itemView);
            imageRecipe = itemView.findViewById(R.id.imageRecipe);
            buttonSave = itemView.findViewById(R.id.buttonSave);
            textRecipeTitle = itemView.findViewById(R.id.textRecipeTitle);
            textRecipeDescription = itemView.findViewById(R.id.textRecipeDescription);
            textCookingTime = itemView.findViewById(R.id.textCookingTime);
            imageAuthorAvatar = itemView.findViewById(R.id.imageAuthorAvatar);
            textAuthor = itemView.findViewById(R.id.textAuthor);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            textLikesCount = itemView.findViewById(R.id.textLikesCount);
        }

        public void bind(Recipe recipe, String currentUserId, OnRecipeInteractionListener listener) {
            // Load recipe image with Glide (optimized)
            if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(recipe.getImageUrl())
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(imageRecipe);
            } else {
                imageRecipe.setImageResource(R.drawable.ic_profile_placeholder);
            }

            // Bind data
            textRecipeTitle.setText(recipe.getName());
            textRecipeDescription.setText(recipe.getDescription());
            textCookingTime.setText(formatCookingTime(recipe.getCookingTime()));
            textAuthor.setText(recipe.getUsername() != null ? recipe.getUsername() : "Anonymous Chef");
            textLikesCount.setText(formatLikesCount(recipe.getLikesCount()));

            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onRecipeClick(recipe, position);
                }
            });

            buttonSave.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onSaveClick(recipe, position);
                }
            });

            buttonLike.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onLikeClick(recipe, position);
                }
            });

            // Author avatar click - navigate to profile
            imageAuthorAvatar.setOnClickListener(v -> {
                if (listener != null && recipe.getAuthorId() != null) {
                    listener.onAuthorClick(recipe.getAuthorId());
                }
            });
        }

        private String formatCookingTime(long minutes) {
            if (minutes < 60) {
                return minutes + " min";
            } else {
                long hours = minutes / 60;
                long mins = minutes % 60;
                return hours + "h " + (mins > 0 ? mins + "m" : "");
            }
        }

        private String formatLikesCount(int count) {
            if (count >= 1000000) {
                return String.format("%.1fM", count / 1000000.0);
            } else if (count >= 1000) {
                return String.format("%.1fk", count / 1000.0);
            } else {
                return count + " likes";
            }
        }
    }

    // DiffUtil callback for efficient list updates
    private static final DiffUtil.ItemCallback<Recipe> DIFF_CALLBACK = new DiffUtil.ItemCallback<Recipe>() {
        @Override
        public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                   oldItem.getDescription().equals(newItem.getDescription()) &&
                   oldItem.getLikesCount() == newItem.getLikesCount() &&
                   oldItem.getCookingTime() == newItem.getCookingTime();
        }

        @Override
        public Object getChangePayload(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            // Return specific fields that changed for partial updates
            return newItem;
        }
    };
}
