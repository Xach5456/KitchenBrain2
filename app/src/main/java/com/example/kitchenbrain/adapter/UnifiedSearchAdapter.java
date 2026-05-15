package com.example.kitchenbrain.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.FoodProduct;
import com.example.kitchenbrain.model.Recipe;
import com.example.kitchenbrain.model.SearchItem;
import com.example.kitchenbrain.User;
import com.example.kitchenbrain.manager.SocialGraphState;
import com.google.android.material.button.MaterialButton;

/**
 * 🔥 UNIFIED SEARCH ADAPTER - Instagram Level Architecture
 * Single adapter for all search types (recipes, users, ingredients)
 */
public class UnifiedSearchAdapter extends ListAdapter<SearchItem, UnifiedSearchAdapter.ViewHolder> {
    
    public interface OnSearchItemClickListener {
        void onSearchItemClick(SearchItem item);
    }

    public interface OnUserFollowClickListener {
        void onFollowClick(User user, boolean isFollowing);
    }
    
    private final OnSearchItemClickListener listener;
    private OnUserFollowClickListener followClickListener;
    
    public UnifiedSearchAdapter(OnSearchItemClickListener listener) {
        super(new SearchItemDiffCallback());
        this.listener = listener;
    }

    public void setOnUserFollowClickListener(OnUserFollowClickListener followClickListener) {
        this.followClickListener = followClickListener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_unified_search, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchItem item = getItem(position);
        
        switch (item.getType()) {
            case RECIPE:
                bindRecipe(holder, item.getRecipe());
                break;
            case USER:
                bindUser(holder, item.getUser());
                break;
            case INGREDIENT:
                bindIngredient(holder, item.getIngredient());
                break;
        }
    }
    
    /**
     * 🔥 RECIPE BINDING
     */
    private void bindRecipe(ViewHolder holder, Recipe recipe) {
        holder.title.setText(recipe.getTitle());
        holder.subtitle.setText(recipe.getCookingTime() + " min");
        holder.btnFollow.setVisibility(View.GONE);
        
        Glide.with(holder.itemView.getContext())
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.placeholder_recipe)
                .into(holder.image);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSearchItemClick(new SearchItem(SearchItem.Type.RECIPE, recipe, recipe.getId()));
        });
    }
    
    /**
     * 🔥 USER BINDING
     */
    private void bindUser(ViewHolder holder, User user) {
        holder.title.setText(user.getUsername());
        holder.subtitle.setText(user.getEmail() != null ? user.getEmail() : "No email");
        
        boolean isFollowing = SocialGraphState.getInstance().isFollowing(user.getId());
        holder.btnFollow.setVisibility(View.VISIBLE);
        holder.btnFollow.setText(isFollowing ? "Unfollow" : "Follow");
        
        holder.btnFollow.setOnClickListener(v -> {
            if (followClickListener != null) {
                followClickListener.onFollowClick(user, isFollowing);
            }
        });
        
        Glide.with(holder.itemView.getContext())
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.placeholder_user)
                .circleCrop()
                .into(holder.image);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSearchItemClick(new SearchItem(SearchItem.Type.USER, user, user.getId()));
        });
    }
    
    /**
     * 🔥 INGREDIENT BINDING
     */
    private void bindIngredient(ViewHolder holder, FoodProduct ingredient) {
        holder.title.setText(ingredient.getDisplayName());
        holder.subtitle.setText(ingredient.getCategory());
        holder.btnFollow.setVisibility(View.GONE);
        
        // Use icon or placeholder for ingredients
        holder.image.setImageResource(R.drawable.ic_ingredient);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSearchItemClick(new SearchItem(SearchItem.Type.INGREDIENT, ingredient, ingredient.getId()));
        });
    }
    
    /**
     * 🔥 VIEW HOLDER
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView subtitle;
        MaterialButton btnFollow;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.itemImage);
            title = itemView.findViewById(R.id.itemTitle);
            subtitle = itemView.findViewById(R.id.itemSubtitle);
            btnFollow = itemView.findViewById(R.id.btnFollow);
        }
    }
    
    /**
     * 🔥 DIFF UTIL - Instagram Performance
     */
    private static class SearchItemDiffCallback extends DiffUtil.ItemCallback<SearchItem> {
        @Override
        public boolean areItemsTheSame(@NonNull SearchItem oldItem, @NonNull SearchItem newItem) {
            return oldItem.getId().equals(newItem.getId()) && 
                   oldItem.getType() == newItem.getType();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull SearchItem oldItem, @NonNull SearchItem newItem) {
            if (oldItem.getType() != newItem.getType()) {
                return false;
            }
            
            switch (oldItem.getType()) {
                case RECIPE:
                    Recipe oldRecipe = oldItem.getRecipe();
                    Recipe newRecipe = newItem.getRecipe();
                    return oldRecipe != null && newRecipe != null &&
                           oldRecipe.getTitle().equals(newRecipe.getTitle()) &&
                           oldRecipe.getImageUrl().equals(newRecipe.getImageUrl());
                           
                case USER:
                    User oldUser = oldItem.getUser();
                    User newUser = newItem.getUser();
                    boolean followingOld = SocialGraphState.getInstance().isFollowing(oldUser.getId());
                    boolean followingNew = SocialGraphState.getInstance().isFollowing(newUser.getId());
                    return oldUser != null && newUser != null &&
                           oldUser.getUsername().equals(newUser.getUsername()) &&
                           followingOld == followingNew &&
                           (oldUser.getAvatarUrl() == null ? newUser.getAvatarUrl() == null : oldUser.getAvatarUrl().equals(newUser.getAvatarUrl()));
                           
                case INGREDIENT:
                    FoodProduct oldIngredient = oldItem.getIngredient();
                    FoodProduct newIngredient = newItem.getIngredient();
                    return oldIngredient != null && newIngredient != null &&
                           oldIngredient.getDisplayName().equals(newIngredient.getDisplayName());
                           
                default:
                    return false;
            }
        }
    }
}
