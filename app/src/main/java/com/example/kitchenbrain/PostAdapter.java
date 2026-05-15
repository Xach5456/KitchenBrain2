package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adapter for displaying user's recipe posts in profile screen
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    
    private List<Recipe> recipes;
    private Fragment parentFragment;
    
    public PostAdapter(List<Recipe> recipes, Fragment parentFragment) {
        this.recipes = recipes;
        this.parentFragment = parentFragment;
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
            if (holder.textViewPostTitle != null && recipe != null && recipe.getName() != null) {
                holder.textViewPostTitle.setText(recipe.getName());
            }
            
            // Set description
            if (holder.textViewPostDescription != null && recipe != null && recipe.getDescription() != null) {
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
            
            // Click listener to open recipe details
            holder.itemView.setOnClickListener(v -> {
                if (parentFragment.getActivity() != null && parentFragment.isAdded()) {
                    RecipeDetailFragment detailFragment = RecipeDetailFragment.newInstance(recipe);
                    if (detailFragment != null) {
                        try {
                            parentFragment.getActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, detailFragment)
                                    .addToBackStack(null)
                                    .commitAllowingStateLoss();
                        } catch (IllegalStateException e) {
                            // Fallback: post to run after state is stable
                            v.post(() -> {
                                if (!parentFragment.isRemoving() && !parentFragment.isDetached()) {
                                    parentFragment.getActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragment_container, detailFragment)
                                            .addToBackStack(null)
                                            .commitAllowingStateLoss();
                                }
                            });
                        }
                    }
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }
    
    /**
     * ViewHolder for recipe posts
     */
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPost;
        TextView textViewPostTitle;
        TextView textViewPostDescription;
        
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPost = itemView.findViewById(R.id.imageViewPost);
            textViewPostTitle = itemView.findViewById(R.id.textViewPostTitle);
            textViewPostDescription = itemView.findViewById(R.id.textViewPostDescription);
        }
    }
}
