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
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.Recipe;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 🔥 RECIPE DETAIL FRAGMENT - Universal recipe viewer
 * 
 * Works with BOTH:
 * - com.example.kitchenbrain.Recipe (Legacy / AddRecipe)
 * - com.example.kitchenbrain.model.Recipe (Firestore)
 */
public class RecipeDetailFragment extends Fragment {

    private static final String TAG = "RecipeDetailFragment";
    private static final String ARG_RECIPE = "recipe";
    
    // UI Components
    private Toolbar toolbar;
    private ImageView imgRecipeDetail;
    private TextView txtTitleDetail;
    private TextView txtDescriptionDetail;
    private TextView txtTimeDetail;
    private TextView txtDifficultyDetail;
    private TextView txtServingsDetail;
    private TextView txtIngredientsDetail;
    private TextView txtInstructionsDetail;
    private TextView txtCategoryDetail;
    private TextView txtLikesDetail;
    private TextView txtAuthorName;
    private TextView txtCreatedAt;
    
    private ImageView imgAuthorAvatar;
    private ImageView imgLikeDetail;
    private LinearLayout layoutLikes;
    private LinearLayout layoutAuthor;
    private ChipGroup chipGroupTags;
    private Button btnWatchVideo;
    
    // Data
    private Object recipe; // Use Object to accept both Recipe types
    private boolean isLiked = false;

    public RecipeDetailFragment() {
        // Required empty constructor
    }

    /**
     * 🔥 FACTORY METHOD - Create fragment with ANY recipe type
     */
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
        
        // 🔥 RESTORE RECIPE FROM ARGUMENTS
        if (getArguments() != null) {
            recipe = getArguments().getSerializable(ARG_RECIPE);
            Log.d(TAG, "🔥 Recipe loaded: " + (recipe != null ? recipe.getClass().getSimpleName() : "null"));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "🔥 ===== UNIVERSAL RECIPE DETAIL FRAGMENT START =====");
        
        View view = inflater.inflate(R.layout.fragment_recipe_detail, container, false);
        
        initViews(view);
        setupToolbar();
        setupData();
        setupListeners();
        
        Log.d(TAG, "🔥 ===== UNIVERSAL RECIPE DETAIL FRAGMENT COMPLETE =====");
        return view;
    }
    
    /**
     * 🔥 INITIALIZE VIEWS
     */
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
        
        // Author & Engagement
        layoutAuthor = view.findViewById(R.id.layoutAuthor);
        imgAuthorAvatar = view.findViewById(R.id.imgAuthorAvatar);
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
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void setupListeners() {
        // Like button toggle logic
        if (layoutLikes != null) {
            layoutLikes.setOnClickListener(v -> {
                isLiked = !isLiked;
                updateLikeUI();
                String msg = isLiked ? "Added to favorites" : "Removed from favorites";
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                // Note: Real persistence (Firestore/Local DB) should be added here
            });
        }

        // Watch Video logic
        if (btnWatchVideo != null) {
            btnWatchVideo.setOnClickListener(v -> {
                String videoUrl = getRecipeVideoUrl();
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Could not open video URL", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "No video tutorial available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Author click - Open Profile
        if (layoutAuthor != null) {
            layoutAuthor.setOnClickListener(v -> {
                String authorId = getRecipeAuthorId();
                if (authorId != null && !authorId.isEmpty()) {
                    openAuthorProfile(authorId);
                }
            });
        }
    }

    private void updateLikeUI() {
        if (imgLikeDetail != null) {
            // Check if R.drawable.ic_heart_filled exists, fallback to filled with tint if not
            imgLikeDetail.setColorFilter(isLiked ? 
                    getResources().getColor(R.color.primary_blue) : 
                    getResources().getColor(R.color.text_secondary));
        }
    }

    private void openAuthorProfile(String authorId) {
        try {
            OtherUserProfileFragment profileFragment = OtherUserProfileFragment.newInstance(authorId);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Log.e(TAG, "Error opening profile fragment", e);
        }
    }
    
    /**
     * 🔥 SETUP RECIPE DATA - Universal handler for both Recipe types
     */
    private void setupData() {
        if (recipe == null) {
            Log.e(TAG, "🔥 ERROR: Recipe is null");
            return;
        }
        
        Log.d(TAG, "🔥 Setting up recipe data for: " + recipe.getClass().getSimpleName());
        
        // Basic Info
        txtTitleDetail.setText(getRecipeTitle());
        
        String desc = getRecipeDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            txtDescriptionDetail.setText(Html.fromHtml(desc, Html.FROM_HTML_MODE_COMPACT));
            txtDescriptionDetail.setVisibility(View.VISIBLE);
        } else {
            txtDescriptionDetail.setVisibility(View.GONE);
        }
        
        long cookingTime = getRecipeCookingTime();
        if (cookingTime > 0) {
            txtTimeDetail.setText(cookingTime + " min");
            txtTimeDetail.setVisibility(View.VISIBLE);
        } else {
            txtTimeDetail.setVisibility(View.GONE);
        }
        
        String difficulty = getRecipeDifficulty();
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            txtDifficultyDetail.setText(difficulty);
            txtDifficultyDetail.setVisibility(View.VISIBLE);
        } else {
            txtDifficultyDetail.setVisibility(View.GONE);
        }
        
        int servings = getRecipeServings();
        if (servings > 0) {
            txtServingsDetail.setText(servings + " ppl");
            txtServingsDetail.setVisibility(View.VISIBLE);
        } else {
            txtServingsDetail.setVisibility(View.GONE);
        }
        
        String category = getRecipeCategory();
        if (category != null && !category.trim().isEmpty()) {
            txtCategoryDetail.setText(category);
            txtCategoryDetail.setVisibility(View.VISIBLE);
            addTagChip(category);
        } else {
            txtCategoryDetail.setVisibility(View.GONE);
        }

        // Author & Engagement
        txtAuthorName.setText(getRecipeAuthorName());
        txtCreatedAt.setText(getFormattedDate());
        txtLikesDetail.setText(String.valueOf(getRecipeLikesCount()));
        
        // Video Button visibility
        String videoUrl = getRecipeVideoUrl();
        btnWatchVideo.setVisibility(videoUrl != null && !videoUrl.isEmpty() ? View.VISIBLE : View.GONE);
        
        // Ingredients
        List<String> ingredients = getRecipeIngredients();
        if (ingredients != null && !ingredients.isEmpty()) {
            StringBuilder ingredientsText = new StringBuilder();
            for (int i = 0; i < ingredients.size(); i++) {
                String ingredient = ingredients.get(i);
                if (ingredient != null && !ingredient.trim().isEmpty()) {
                    ingredientsText.append("• ").append(ingredient.trim());
                    if (i < ingredients.size() - 1) {
                        ingredientsText.append("\n");
                    }
                }
            }
            txtIngredientsDetail.setText(ingredientsText.toString());
            txtIngredientsDetail.setVisibility(View.VISIBLE);
        } else {
            txtIngredientsDetail.setText("No ingredients listed");
        }
        
        // Instructions
        List<String> instructions = getRecipeInstructions();
        if (instructions != null && !instructions.isEmpty()) {
            StringBuilder instructionsText = new StringBuilder();
            for (int i = 0; i < instructions.size(); i++) {
                String instruction = instructions.get(i);
                if (instruction != null && !instruction.trim().isEmpty()) {
                    instructionsText.append(i + 1).append(". ").append(instruction.trim());
                    if (i < instructions.size() - 1) {
                        instructionsText.append("\n\n");
                    }
                }
            }
            txtInstructionsDetail.setText(instructionsText.toString());
            txtInstructionsDetail.setVisibility(View.VISIBLE);
        } else {
            txtInstructionsDetail.setText("No instructions available");
        }
        
        // Load image (URL has priority, then Resource ID)
        String imageUrl = getRecipeImageUrl();
        int imageResId = getRecipeImageResId();
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(imgRecipeDetail);
        } else if (imageResId != 0) {
            Glide.with(requireContext())
                    .load(imageResId)
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder)
                    .into(imgRecipeDetail);
        } else {
            imgRecipeDetail.setImageResource(R.drawable.ic_placeholder);
        }
    }

    private void addTagChip(String tag) {
        if (chipGroupTags != null) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setChipBackgroundColorResource(R.color.bg_input_field);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setChipStrokeWidth(0);
            chipGroupTags.addView(chip);
        }
    }
    
    // 🔥 UNIVERSAL GETTER METHODS - Extract data from multiple model types
    
    private String getRecipeTitle() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getTitle();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getName();
        return "Untitled Recipe";
    }

    private String getRecipeDescription() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getDescription();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getDescription();
        return null;
    }

    private long getRecipeCookingTime() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getCookingTime();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getCookingTime();
        return 0;
    }

    private String getRecipeDifficulty() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getDifficulty();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getDifficulty();
        return null;
    }

    private int getRecipeServings() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getServings();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getServings();
        return 0;
    }

    private String getRecipeCategory() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getCategory();
        return null;
    }

    private List<String> getRecipeIngredients() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getIngredients();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getIngredients();
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
        return null;
    }

    private String getRecipeImageUrl() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getImageUrl();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getImageUrl();
        return null;
    }

    private int getRecipeImageResId() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getImageResId();
        return 0;
    }

    private String getRecipeAuthorName() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
            String name = ((com.example.kitchenbrain.model.Recipe) recipe).getUsername();
            return name != null ? name : ((com.example.kitchenbrain.model.Recipe) recipe).getCreatedBy();
        }
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getUsername();
        return "Anonymous Chef";
    }

    private String getRecipeAuthorId() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getAuthorId();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getAuthorId();
        return null;
    }

    private String getRecipeVideoUrl() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getVideoUrl();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getVideoUrl();
        return null;
    }

    private int getRecipeLikesCount() {
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) return ((com.example.kitchenbrain.model.Recipe) recipe).getLikesCount();
        if (recipe instanceof com.example.kitchenbrain.Recipe) return ((com.example.kitchenbrain.Recipe) recipe).getLikesCount();
        return 0;
    }

    private String getFormattedDate() {
        try {
            long timestamp = 0;
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                timestamp = ((com.example.kitchenbrain.model.Recipe) recipe).getCreatedAt();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                Object created = ((com.example.kitchenbrain.Recipe) recipe).getCreatedAt();
                if (created instanceof Long) timestamp = (Long) created;
                else if (created instanceof Timestamp) timestamp = ((Timestamp) created).toDate().getTime();
            }
            
            if (timestamp > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return "Published " + sdf.format(new Date(timestamp));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date", e);
        }
        return "Recently published";
    }
}
