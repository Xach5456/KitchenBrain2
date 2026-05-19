package com.example.kitchenbrain;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.kitchenbrain.model.SocialRecipe;
import com.example.kitchenbrain.repository.RecipeRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 🔥 UNIVERSAL RECIPE DETAIL FRAGMENT - Premium Redesign 2026
 * Handles both local and social recipes with a polished, high-end UI.
 */
public class RecipeDetailFragment extends Fragment {

    private static final String TAG = "RecipeDetailFragment";
    private static final String ARG_RECIPE = "recipe";
    
    private Toolbar toolbar;
    private ImageView imgRecipeDetail;
    private TextView txtTitleDetail, txtDescriptionDetail, txtTimeDetail, txtDifficultyDetail;
    private TextView txtServingsDetail, txtIngredientsDetail, txtInstructionsDetail, txtCategoryDetail;
    private TextView txtLikesDetail, txtAuthorName, txtCreatedAt, txtCaloriesDetail;
    
    private TextView labelIngredients, labelInstructions;
    private ImageView imgLikeDetail;
    private LinearLayout layoutLikes, layoutAuthor;
    private ChipGroup chipGroupTags;
    private Button btnWatchVideo;
    
    private Object recipe;
    private boolean isLiked = false;
    private RecipeRepository repository;
    private String currentUserId;
    private int currentLikesCount = 0;

    public RecipeDetailFragment() {}

    public static RecipeDetailFragment newInstance(Object recipe) {
        RecipeDetailFragment fragment = new RecipeDetailFragment();
        Bundle args = new Bundle();
        if (recipe instanceof java.io.Serializable) {
            args.putSerializable(ARG_RECIPE, (java.io.Serializable) recipe);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new RecipeRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (getArguments() != null) {
            recipe = getArguments().getSerializable(ARG_RECIPE);
        }
        Log.d(TAG, "onCreate: Recipe detail opened. User ID: " + currentUserId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_detail, container, false);
        initViews(view);
        setupToolbar();
        setupData();
        setupListeners();
        return view;
    }
    
    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        imgRecipeDetail = view.findViewById(R.id.imgRecipeDetail);
        txtTitleDetail = view.findViewById(R.id.txtTitleDetail);
        txtDescriptionDetail = view.findViewById(R.id.txtDescriptionDetail);
        txtTimeDetail = view.findViewById(R.id.txtTimeDetail);
        txtDifficultyDetail = view.findViewById(R.id.txtDifficultyDetail);
        txtServingsDetail = view.findViewById(R.id.txtServingsDetail);
        txtIngredientsDetail = view.findViewById(R.id.txtIngredientsDetail);
        txtInstructionsDetail = view.findViewById(R.id.txtInstructionsDetail);
        txtCategoryDetail = view.findViewById(R.id.txtCategoryDetail);
        txtCaloriesDetail = view.findViewById(R.id.txtCaloriesDetail);
        
        labelIngredients = view.findViewById(R.id.labelIngredients);
        labelInstructions = view.findViewById(R.id.labelInstructions);
        
        layoutAuthor = view.findViewById(R.id.layoutAuthor);
        txtAuthorName = view.findViewById(R.id.txtAuthorName);
        txtCreatedAt = view.findViewById(R.id.txtCreatedAt);
        
        layoutLikes = view.findViewById(R.id.layoutLikes);
        imgLikeDetail = view.findViewById(R.id.imgLikeDetail);
        txtLikesDetail = view.findViewById(R.id.txtLikesDetail);
        
        chipGroupTags = view.findViewById(R.id.chipGroupTags);
        btnWatchVideo = view.findViewById(R.id.btnWatchVideo);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (isAdded()) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void setupListeners() {
        if (layoutLikes != null) {
            layoutLikes.setOnClickListener(v -> {
                if (currentUserId == null) {
                    Toast.makeText(getContext(), "Please login to like recipes", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String recipeId = getRecipeId();
                if (recipeId == null || recipeId.isEmpty()) {
                    Log.e(TAG, "Cannot like: recipeId is null or empty");
                    return;
                }
                
                // Optimistic UI update
                isLiked = !isLiked;
                if (isLiked) currentLikesCount++;
                else currentLikesCount = Math.max(0, currentLikesCount - 1);
                
                updateLikeUI();
                txtLikesDetail.setText(String.valueOf(currentLikesCount));
                
                Log.d(TAG, "Toggling like for recipe: " + recipeId);
                repository.toggleLike(recipeId, currentUserId, new RecipeRepository.RecipeCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (!isAdded()) return;
                        Log.d(TAG, "Like toggled successfully. New state: " + result);
                        isLiked = result;
                        updateLikeUI();
                        // Refresh full data to ensure sync
                        refreshRecipeData();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Log.e(TAG, "Error toggling like: " + error);
                        // Revert optimistic update
                        isLiked = !isLiked;
                        if (isLiked) currentLikesCount++;
                        else currentLikesCount = Math.max(0, currentLikesCount - 1);
                        
                        updateLikeUI();
                        txtLikesDetail.setText(String.valueOf(currentLikesCount));
                        Toast.makeText(getContext(), "Failed to update like: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (btnWatchVideo != null) {
            btnWatchVideo.setOnClickListener(v -> {
                String url = getRecipeVideoUrl();
                if (url != null && !url.isEmpty()) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error opening video", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (layoutAuthor != null) {
            layoutAuthor.setOnClickListener(v -> {
                String authorId = getRecipeAuthorId();
                if (authorId != null && isAdded()) {
                    OtherUserProfileFragment profileFragment = OtherUserProfileFragment.newInstance(authorId);
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, profileFragment)
                            .addToBackStack(null).commit();
                }
            });
        }
    }

    private void refreshRecipeData() {
        String recipeId = getRecipeId();
        if (recipeId == null) return;
        
        repository.getRecipe(recipeId, new RecipeRepository.RecipeCallback<SocialRecipe>() {
            @Override
            public void onSuccess(SocialRecipe updatedRecipe) {
                if (updatedRecipe != null && isAdded()) {
                    recipe = updatedRecipe;
                    setupData();
                }
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error refreshing recipe: " + error);
            }
        });
    }

    private void updateLikeUI() {
        if (imgLikeDetail != null && isAdded()) {
            if (isLiked) {
                imgLikeDetail.setImageResource(R.drawable.ic_heart_filled);
                imgLikeDetail.setColorFilter(ContextCompat.getColor(requireContext(), R.color.heart_red));
            } else {
                imgLikeDetail.setImageResource(R.drawable.ic_heart_outline);
                imgLikeDetail.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
            
            imgLikeDetail.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction(() -> 
                imgLikeDetail.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
            ).start();
        }
    }
    
    private void setupData() {
        if (recipe == null) return;
        
        txtTitleDetail.setText(getRecipeTitle());
        
        String desc = getRecipeDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            txtDescriptionDetail.setText(Html.fromHtml(desc, Html.FROM_HTML_MODE_COMPACT));
            txtDescriptionDetail.setVisibility(View.VISIBLE);
        } else {
            txtDescriptionDetail.setVisibility(View.GONE);
        }
        
        long cookingTime = getRecipeCookingTime();
        txtTimeDetail.setText(cookingTime > 0 ? cookingTime + " min" : "--");
        
        String diff = getRecipeDifficulty();
        txtDifficultyDetail.setText(diff != null ? diff : "Easy");
        
        int servings = getRecipeServings();
        txtServingsDetail.setText(servings > 0 ? servings + " ppl" : "--");

        int calories = getRecipeCalories();
        if (txtCaloriesDetail != null) {
            txtCaloriesDetail.setText(calories > 0 ? calories + " kcal" : "--");
        }

        txtAuthorName.setText(getRecipeAuthorName());
        txtCreatedAt.setText(getFormattedDate());
        
        currentLikesCount = getRecipeLikesCount();
        txtLikesDetail.setText(String.valueOf(currentLikesCount));
        
        String category = getRecipeCategory();
        if (txtCategoryDetail != null) {
            if (category != null && !category.isEmpty()) {
                txtCategoryDetail.setText(category);
                txtCategoryDetail.setVisibility(View.VISIBLE);
            } else {
                txtCategoryDetail.setVisibility(View.GONE);
            }
        }
        
        String videoUrl = getRecipeVideoUrl();
        if (btnWatchVideo != null) {
            btnWatchVideo.setVisibility(videoUrl != null && !videoUrl.isEmpty() ? View.VISIBLE : View.GONE);
        }
        
        List<String> ingredients = getRecipeIngredients();
        if (ingredients != null && !ingredients.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String ing : ingredients) sb.append("• ").append(ing).append("\n");
            txtIngredientsDetail.setText(sb.toString().trim());
            txtIngredientsDetail.setVisibility(View.VISIBLE);
            if (labelIngredients != null) labelIngredients.setVisibility(View.VISIBLE);
        } else {
            txtIngredientsDetail.setVisibility(View.GONE);
            if (labelIngredients != null) labelIngredients.setVisibility(View.GONE);
        }
        
        List<String> instructions = getRecipeInstructions();
        if (instructions != null && !instructions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < instructions.size(); i++) {
                sb.append(i + 1).append(". ").append(instructions.get(i)).append("\n\n");
            }
            txtInstructionsDetail.setText(sb.toString().trim());
            txtInstructionsDetail.setVisibility(View.VISIBLE);
            if (labelInstructions != null) labelInstructions.setVisibility(View.VISIBLE);
        } else {
            txtInstructionsDetail.setVisibility(View.GONE);
            if (labelInstructions != null) labelInstructions.setVisibility(View.GONE);
        }
        
        String imageUrl = getRecipeImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(imgRecipeDetail);
        } else {
            imgRecipeDetail.setImageResource(R.drawable.ic_placeholder);
        }

        checkIfLiked();
    }

    private void checkIfLiked() {
        if (recipe == null) return;
        
        if (currentUserId == null) {
            isLiked = false;
        } else {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                List<String> likedBy = ((com.example.kitchenbrain.model.Recipe) recipe).getLikedBy();
                isLiked = likedBy != null && likedBy.contains(currentUserId);
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                List<String> likedBy = ((com.example.kitchenbrain.Recipe) recipe).getLikedBy();
                isLiked = likedBy != null && likedBy.contains(currentUserId);
            } else if (recipe instanceof SocialRecipe) {
                Map<String, Boolean> likedBy = ((SocialRecipe) recipe).likedBy;
                isLiked = likedBy != null && likedBy.containsKey(currentUserId);
            }
        }
        updateLikeUI();
    }

    private String getRecipeId() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getId();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getId();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getId();
        return null;
    }

    private String getRecipeTitle() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getTitle();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getTitle();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getTitle();
        return "Recipe";
    }
    
    private String getRecipeDescription() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getDescription();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getDescription();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getDescription();
        return null;
    }
    
    private long getRecipeCookingTime() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getCookingTime();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getCookingTime();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getCookingTime();
        return 0;
    }
    
    private String getRecipeDifficulty() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getDifficulty();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getDifficulty();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getDifficulty();
        return "Easy";
    }
    
    private int getRecipeServings() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getServings();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getServings();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getServings();
        return 0;
    }
    
    private int getRecipeCalories() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getCalories();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getCalories();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getCalories();
        return 0;
    }
    
    private String getRecipeCategory() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getCategory();
        return null;
    }

    private List<String> getRecipeIngredients() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getIngredients();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getIngredients();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getIngredients();
        return null;
    }
    
    private List<String> getRecipeInstructions() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
            List<String> inst = ((com.example.kitchenbrain.model.Recipe) recipe).getInstructions();
            if (inst != null && !inst.isEmpty()) return inst;
            String single = ((com.example.kitchenbrain.model.Recipe) recipe).getCookingInstructions();
            if (single != null) { List<String> l = new ArrayList<>(); l.add(single); return l; }
        }
        if (recipe instanceof com.example.kitchenbrain.Recipe) {
            String single = ((com.example.kitchenbrain.Recipe) recipe).getCookingInstructions();
            if (single != null) { List<String> l = new ArrayList<>(); l.add(single); return l; }
        }
        if (recipe instanceof SocialRecipe) {
            List<String> steps = ((SocialRecipe) recipe).getSteps();
            if (steps != null && !steps.isEmpty()) return steps;
            String single = ((SocialRecipe) recipe).getCookingInstructions();
            if (single != null) { List<String> l = new ArrayList<>(); l.add(single); return l; }
        }
        return null;
    }
    
    private String getRecipeImageUrl() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getImageUrl();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getImageUrl();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getImageUrl();
        return null;
    }
    
    private String getRecipeAuthorName() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getUsername();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getAuthorName();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getAuthorName();
        return "Chef";
    }
    
    private String getRecipeAuthorId() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getAuthorId();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getAuthorId();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getAuthorId();
        return null;
    }
    
    private String getRecipeVideoUrl() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getVideoUrl();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getVideoUrl();
        if (recipe instanceof SocialRecipe) return ((SocialRecipe) recipe).getVideoUrl();
        return null;
    }
    
    private int getRecipeLikesCount() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getLikesCount();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getLikesCount();
        if (recipe instanceof SocialRecipe) return (int)((SocialRecipe) recipe).getLikes();
        return 0;
    }

    private String getFormattedDate() {
        try {
            long ts = 0;
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                ts = ((com.example.kitchenbrain.model.Recipe) recipe).getCreatedAt();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                Object c = ((com.example.kitchenbrain.Recipe) recipe).getCreatedAt();
                if (c instanceof Long) ts = (Long) c;
                else if (c instanceof Timestamp) ts = ((Timestamp) c).toDate().getTime();
                else if (c instanceof Integer) ts = (Integer) c;
            } else if (recipe instanceof SocialRecipe) {
                Date d = ((SocialRecipe) recipe).getCreatedAt();
                if (d != null) ts = d.getTime();
            }
            if (ts > 0) {
                return "Published " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(ts));
            }
        } catch (Exception ignored) {}
        return "Published recently";
    }
}
