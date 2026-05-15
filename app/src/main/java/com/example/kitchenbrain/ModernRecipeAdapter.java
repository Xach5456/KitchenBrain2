package com.example.kitchenbrain;

import android.content.Context;
import android.util.Log;
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
import com.google.android.material.imageview.ShapeableImageView;

/**
 * Modern Recipe Adapter with DiffUtil for smooth updates
 * Supports like/save functionality and image loading
 */
public class ModernRecipeAdapter extends ListAdapter<Recipe, ModernRecipeAdapter.RecipeViewHolder> {

    private OnRecipeInteractionListener listener;
    private String currentUserId;

    public interface OnRecipeInteractionListener {
        void onRecipeClick(Recipe recipe);
        void onLikeClick(Recipe recipe, int position, boolean isLiked);
        void onSaveClick(Recipe recipe, int position, boolean isSaved);
        void onAuthorClick(String authorId);
    }

    public ModernRecipeAdapter(OnRecipeInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = getItem(position);
        
        // SAFETY: Ensure adapter never passes null item
        if (recipe == null) {
            Log.e("ModernRecipeAdapter", "ERROR: Null recipe at position " + position);
            return;
        }
        
        // SANITIZE: Ensure all fields are non-null before binding
        recipe = RecipeSanitizer.sanitize(recipe);
        
        holder.bind(recipe, currentUserId, listener);
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageRecipe;
        private ImageButton buttonSave;
        private TextView textRecipeTitle;
        private TextView textRecipeDescription;
        private TextView textCookingTime;
        private ShapeableImageView imageAuthorAvatar;
        private TextView textAuthor;
        private ImageButton buttonLike;
        private TextView textLikesCount;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // Initialize views with null checks
            imageRecipe = itemView.findViewById(R.id.imageRecipe);
            buttonSave = itemView.findViewById(R.id.buttonSave);
            textRecipeTitle = itemView.findViewById(R.id.textRecipeTitle);
            textRecipeDescription = itemView.findViewById(R.id.textRecipeDescription);
            textCookingTime = itemView.findViewById(R.id.textCookingTime);
            imageAuthorAvatar = itemView.findViewById(R.id.imageAuthorAvatar);
            textAuthor = itemView.findViewById(R.id.textAuthor);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            textLikesCount = itemView.findViewById(R.id.textLikesCount);
            
            // Validate critical views
            if (imageRecipe == null) {
                Log.e("RecipeViewHolder", "CRITICAL: imageRecipe ImageView not found in layout!");
            }
        }

        public void bind(Recipe recipe, String currentUserId, OnRecipeInteractionListener listener) {
            // EARLY EXIT: If recipe is null, return immediately
            if (recipe == null) {
                Log.e("ADAPTER_DEBUG", "bind() called with null recipe - returning early");
                return;
            }
            
            // DEBUG LOGGING: Find real cause of any issues
            Log.d("ADAPTER_DEBUG", "=== Binding Recipe ===");
            Log.d("ADAPTER_DEBUG", "recipe=" + recipe);
            Log.d("ADAPTER_DEBUG", "imageRecipe=" + imageRecipe);
            Log.d("ADAPTER_DEBUG", "context=" + itemView.getContext());
            Log.d("ADAPTER_DEBUG", "imageUrl=" + recipe.getImageUrl());
            
            // ========================================
            // ULTIMATE GLIDE NULL-SAFETY
            // ========================================
            if (imageRecipe != null) {
                Context context = itemView.getContext();
                if (context != null) {
                    String imageUrl = recipe.getImageUrl();
                    
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        // ✅ FIXED: Remove trailing dot from Spoonacular URLs
                        if (imageUrl.endsWith(".")) {
                            imageUrl = imageUrl.substring(0, imageUrl.length() - 1);
                        }
                        
                        // Valid URL - load with Glide (no try-catch needed - Glide handles errors asynchronously)
                        Glide.with(context)
                                .load(imageUrl)
                                .centerCrop()
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .into(imageRecipe);
                    } else {
                        // URL is null or empty - use placeholder
                        imageRecipe.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                } else {
                    Log.e("ADAPTER_DEBUG", "Context is null - cannot load image");
                }
            } else {
                Log.e("ADAPTER_DEBUG", "imageRecipe ImageView is null - cannot load image");
            }

            // ========================================
            // BIND DATA WITH NULL CHECKS
            // ========================================
            if (textRecipeTitle != null) {
                textRecipeTitle.setText(recipe.getName() != null ? recipe.getName() : "Untitled Recipe");
            }
            
            if (textRecipeDescription != null) {
                textRecipeDescription.setText(recipe.getDescription() != null ? recipe.getDescription() : "No description");
            }
            
            if (textCookingTime != null) {
                textCookingTime.setText(recipe.getCookingTime() > 0 ? recipe.getCookingTime() + " min" : "-- min");
            }
            
            if (textAuthor != null) {
                textAuthor.setText(recipe.getUsername() != null ? recipe.getUsername() : "Unknown Chef");
            }
            
            if (textLikesCount != null) {
                textLikesCount.setText(formatLikesCount(recipe.getLikesCount()));
            }

            // ========================================
            // CLICK LISTENERS WITH SAFETY CHECKS
            // ========================================
            if (itemView != null) {
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        // OPEN RECIPE IN BOTTOM SHEET MODAL
                        RecipeBottomSheetFragment bottomSheet = 
                                RecipeBottomSheetFragment.newInstance(recipe);
                        
                        android.app.Activity activity = (android.app.Activity) itemView.getContext();
                        if (activity instanceof androidx.fragment.app.FragmentActivity) {
                            androidx.fragment.app.FragmentManager fragmentManager = 
                                    ((androidx.fragment.app.FragmentActivity) activity).getSupportFragmentManager();
                            bottomSheet.show(fragmentManager, "RecipeBottomSheet");
                        }
                    }
                });
            }

            if (buttonSave != null) {
                buttonSave.setOnClickListener(v -> {
                    if (listener != null) {
                        int position = getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onSaveClick(recipe, position, false);
                        }
                    }
                });
            }

            if (buttonLike != null) {
                buttonLike.setOnClickListener(v -> {
                    if (listener != null) {
                        int position = getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onLikeClick(recipe, position, false);
                        }
                    }
                });
            }

            // Author avatar click - with null safety
            if (imageAuthorAvatar != null) {
                imageAuthorAvatar.setOnClickListener(v -> {
                    if (listener != null && recipe.getAuthorId() != null) {
                        listener.onAuthorClick(recipe.getAuthorId());
                    }
                });
            }
        }

        private String formatLikesCount(int count) {
            if (count >= 1000) {
                return String.format("%.1fk", count / 1000.0);
            }
            return count + " likes";
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
                   oldItem.getLikesCount() == newItem.getLikesCount();
        }

        @Override
        public Object getChangePayload(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            // Return specific fields that changed for partial updates
            return newItem;
        }
    };
}
