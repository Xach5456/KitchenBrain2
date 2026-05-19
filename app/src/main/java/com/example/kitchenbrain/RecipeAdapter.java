package com.example.kitchenbrain;

import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.databinding.ItemRecipeBinding;
import com.example.kitchenbrain.util.RecipeOwnershipUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.ArrayList;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private List<Recipe> filteredRecipeList;
    private OnRecipeClickListener listener;
    private boolean allowEditing;
    private boolean showOtherUsersRecipes;
    private boolean requireMutualFollow;
    private FirebaseAuth auth;
    private String currentUserId;
    private List<String> myFollowingList;
    private List<String> myFollowersList;
    private final OvershootInterpolator overshootInterpolator = new OvershootInterpolator(2.2f);

    public interface OnRecipeClickListener {
        void onDeleteRecipe(Recipe recipe);
        void onRecipeClick(Recipe recipe);
        default void onEditRecipe(Recipe recipe) {}
    }

    public RecipeAdapter(List<Recipe> recipeList, OnRecipeClickListener listener) {
        this(recipeList, listener, true);
    }
    
    public RecipeAdapter(List<Recipe> recipeList, OnRecipeClickListener listener, boolean allowEditing) {
        this.recipeList = recipeList != null ? recipeList : new ArrayList<>();
        this.filteredRecipeList = new ArrayList<>(this.recipeList);
        this.listener = listener;
        this.allowEditing = allowEditing;
        this.showOtherUsersRecipes = true;
        this.requireMutualFollow = false;
        this.myFollowingList = new ArrayList<>();
        this.myFollowersList = new ArrayList<>();
        this.auth = FirebaseAuth.getInstance();
        this.currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        updateFilteredRecipes();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        if (parent instanceof RecyclerView) {
            RecyclerView.LayoutManager lm = ((RecyclerView) parent).getLayoutManager();
            if (lm instanceof LinearLayoutManager
                    && ((LinearLayoutManager) lm).getOrientation() == LinearLayoutManager.HORIZONTAL) {
                int widthPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 280, parent.getResources().getDisplayMetrics());
                view.setLayoutParams(new RecyclerView.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
        return new RecipeViewHolder(ItemRecipeBinding.bind(view), overshootInterpolator);
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
        this.recipeList = newRecipes != null ? new ArrayList<>(newRecipes) : new ArrayList<>();
        updateFilteredRecipes();
    }

    public void setShowOtherUsersRecipes(boolean show) {
        this.showOtherUsersRecipes = show;
        updateFilteredRecipes();
    }
    
    public void setRequireMutualFollow(boolean require) {
        this.requireMutualFollow = require;
        updateFilteredRecipes();
    }
    
    public void setMyFollowingList(List<String> followingList) {
        this.myFollowingList = followingList != null ? new ArrayList<>(followingList) : new ArrayList<>();
        updateFilteredRecipes();
    }
    
    public void setMyFollowersList(List<String> followersList) {
        this.myFollowersList = followersList != null ? new ArrayList<>(followersList) : new ArrayList<>();
        updateFilteredRecipes();
    }

    private void updateFilteredRecipes() {
        List<Recipe> oldList = new ArrayList<>(filteredRecipeList);
        List<Recipe> newFilteredList = new ArrayList<>();
        filteredRecipeList.clear();
        for (Recipe recipe : recipeList) {
            if (shouldIncludeRecipe(recipe)) {
                newFilteredList.add(recipe);
            }
        }
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new RecipeDiffCallback(oldList, newFilteredList));
        filteredRecipeList.addAll(newFilteredList);
        diffResult.dispatchUpdatesTo(this);
    }

    private boolean shouldIncludeRecipe(Recipe recipe) {
        if (recipe == null) return false;
        if (!showOtherUsersRecipes && currentUserId != null && !currentUserId.equals(recipe.getAuthorId())) {
            return false;
        }
        if (requireMutualFollow && currentUserId != null && !currentUserId.equals(recipe.getAuthorId())) {
            String recipeAuthorId = recipe.getAuthorId();
            boolean iFollowAuthor = myFollowingList != null && myFollowingList.contains(recipeAuthorId);
            boolean authorFollowsMe = myFollowersList != null && myFollowersList.contains(recipeAuthorId);
            if (!iFollowAuthor || !authorFollowsMe) {
                return false;
            }
        }
        return true;
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecipeBinding binding;
        private final OvershootInterpolator overshootInterpolator;

        public RecipeViewHolder(@NonNull ItemRecipeBinding binding, OvershootInterpolator overshootInterpolator) {
            super(binding.getRoot());
            this.binding = binding;
            this.overshootInterpolator = overshootInterpolator;
        }

        public void bind(Recipe recipe, OnRecipeClickListener listener, boolean allowEditing, String currentUserId) {
            if (recipe != null) {
                binding.recipeName.setText(!TextUtils.isEmpty(recipe.getTitle()) ? recipe.getTitle() : "Unknown Recipe");
                binding.recipeDescription.setText(!TextUtils.isEmpty(recipe.getDescription()) ? recipe.getDescription() : "No description yet");
                binding.recipeTime.setText(recipe.getCookingTime() > 0 ? recipe.getCookingTime() + " min" : "Time unknown");
                if (!TextUtils.isEmpty(recipe.getUsername())) {
                    binding.recipeAuthor.setText("by " + recipe.getUsername());
                    binding.recipeAuthor.setVisibility(View.VISIBLE);
                } else {
                    binding.recipeAuthor.setVisibility(View.GONE);
                }
                Glide.with(binding.recipeImage)
                        .load(recipe.getImageUrl())
                        .placeholder(R.drawable.placeholder_recipe)
                        .error(R.drawable.placeholder_recipe)
                        .centerCrop()
                        .into(binding.recipeImage);
            }

            boolean isOwnRecipe = RecipeOwnershipUtils.isOwner(recipe, currentUserId);
            boolean canModify = allowEditing && isOwnRecipe && recipe != null;
            
            if (canModify) {
                binding.recipeActions.setVisibility(View.VISIBLE);
                configurePremiumPress(binding.btnEditRecipe);
                configurePremiumPress(binding.btnDeleteRecipe);
                binding.btnEditRecipe.setOnClickListener(v -> {
                    playGlow(binding.btnEditRecipe);
                    if (listener != null) listener.onEditRecipe(recipe);
                });
                binding.btnDeleteRecipe.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteRecipe(recipe);
                });
            } else {
                binding.recipeActions.setVisibility(View.GONE);
                binding.btnEditRecipe.setOnClickListener(null);
                binding.btnDeleteRecipe.setOnClickListener(null);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null && recipe != null) {
                    animateCardClick(itemView);
                    listener.onRecipeClick(recipe);
                }
            });
        }

        private void configurePremiumPress(MaterialButton button) {
            button.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    v.animate()
                            .scaleX(0.92f)
                            .scaleY(0.92f)
                            .translationZ(14f)
                            .setDuration(110)
                            .start();
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationZ(0f)
                            .setInterpolator(overshootInterpolator)
                            .setDuration(260)
                            .start();
                }
                return false;
            });
        }

        private void playGlow(View view) {
            view.animate()
                    .alpha(0.82f)
                    .translationZ(22f)
                    .setDuration(90)
                    .withEndAction(() -> view.animate()
                            .alpha(1f)
                            .translationZ(0f)
                            .setInterpolator(overshootInterpolator)
                            .setDuration(260)
                            .start())
                    .start();
        }

        private void animateCardClick(View view) {
            if (view instanceof MaterialCardView cardView) {
                cardView.setCardElevation(8f);
                cardView.animate()
                        .scaleX(0.985f)
                        .scaleY(0.985f)
                        .setDuration(90)
                        .withEndAction(() -> {
                            cardView.setCardElevation(3f);
                            cardView.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setInterpolator(overshootInterpolator)
                                    .setDuration(220)
                                    .start();
                        })
                        .start();
            }
        }
    }

    private static class RecipeDiffCallback extends DiffUtil.Callback {
        private final List<Recipe> oldList;
        private final List<Recipe> newList;

        RecipeDiffCallback(List<Recipe> oldList, List<Recipe> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Recipe oldRecipe = oldList.get(oldItemPosition);
            Recipe newRecipe = newList.get(newItemPosition);
            String oldId = oldRecipe != null ? oldRecipe.getId() : null;
            String newId = newRecipe != null ? newRecipe.getId() : null;
            return oldId != null && oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Recipe oldRecipe = oldList.get(oldItemPosition);
            Recipe newRecipe = newList.get(newItemPosition);
            if (oldRecipe == newRecipe) return true;
            if (oldRecipe == null || newRecipe == null) return false;
            return TextUtils.equals(oldRecipe.getTitle(), newRecipe.getTitle())
                    && TextUtils.equals(oldRecipe.getDescription(), newRecipe.getDescription())
                    && TextUtils.equals(oldRecipe.getImageUrl(), newRecipe.getImageUrl())
                    && oldRecipe.getCookingTime() == newRecipe.getCookingTime()
                    && TextUtils.equals(oldRecipe.getUsername(), newRecipe.getUsername())
                    && TextUtils.equals(oldRecipe.getAuthorId(), newRecipe.getAuthorId());
        }
    }
}
