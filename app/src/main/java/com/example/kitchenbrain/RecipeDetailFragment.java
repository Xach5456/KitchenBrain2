package com.example.kitchenbrain;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.Recipe;

import java.util.List;

/**
 * 🔥 RECIPE DETAIL FRAGMENT - Universal recipe viewer
 * 
 * Works with BOTH:
 * - com.example.kitchenbrain.Recipe (Spoonacular API)
 * - com.example.kitchenbrain.model.Recipe (Firestore)
 */
public class RecipeDetailFragment extends Fragment {

    private static final String TAG = "RecipeDetailFragment";
    private static final String ARG_RECIPE = "recipe";
    
    // UI Components
    private ImageView imgRecipeDetail;
    private TextView txtTitleDetail;
    private TextView txtDescriptionDetail;
    private TextView txtTimeDetail;
    private TextView txtDifficultyDetail;
    private TextView txtServingsDetail;
    private TextView txtIngredientsDetail;
    private TextView txtInstructionsDetail;
    private TextView txtCategoryDetail;
    
    // Data
    private Object recipe; // Use Object to accept both Recipe types

    public RecipeDetailFragment() {
        // Required empty constructor
    }

    /**
     * 🔥 FACTORY METHOD - Create fragment with ANY recipe type
     */
    public static RecipeDetailFragment newInstance(Object recipe) {
        RecipeDetailFragment fragment = new RecipeDetailFragment();
        
        Bundle args = new Bundle();
        args.putSerializable(ARG_RECIPE, (java.io.Serializable) recipe);
        
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
        setupData();
        
        Log.d(TAG, "🔥 ===== UNIVERSAL RECIPE DETAIL FRAGMENT COMPLETE =====");
        return view;
    }
    
    /**
     * 🔥 INITIALIZE VIEWS
     */
    private void initViews(View view) {
        imgRecipeDetail = view.findViewById(R.id.imgRecipeDetail);
        txtTitleDetail = view.findViewById(R.id.txtTitleDetail);
        txtDescriptionDetail = view.findViewById(R.id.txtDescriptionDetail);
        txtTimeDetail = view.findViewById(R.id.txtTimeDetail);
        txtDifficultyDetail = view.findViewById(R.id.txtDifficultyDetail);
        txtServingsDetail = view.findViewById(R.id.txtServingsDetail);
        txtIngredientsDetail = view.findViewById(R.id.txtIngredientsDetail);
        txtInstructionsDetail = view.findViewById(R.id.txtInstructionsDetail);
        txtCategoryDetail = view.findViewById(R.id.txtCategoryDetail);
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
        
        // 🔥 UNIVERSAL DATA EXTRACTION
        String title = getRecipeTitle();
        String description = getRecipeDescription();
        long cookingTime = getRecipeCookingTime();
        String difficulty = getRecipeDifficulty();
        int servings = getRecipeServings();
        String category = getRecipeCategory();
        List<String> ingredients = getRecipeIngredients();
        List<String> instructions = getRecipeInstructions();
        int imageResId = getRecipeImageResId();
        
        // Title
        txtTitleDetail.setText(title != null ? title : "Unknown Recipe");
        
        // Description
        if (description != null && !description.trim().isEmpty()) {
            txtDescriptionDetail.setText(description);
            txtDescriptionDetail.setVisibility(View.VISIBLE);
        } else {
            txtDescriptionDetail.setVisibility(View.GONE);
        }
        
        // Time
        if (cookingTime > 0) {
            txtTimeDetail.setText(cookingTime + " min");
            txtTimeDetail.setVisibility(View.VISIBLE);
        } else {
            txtTimeDetail.setVisibility(View.GONE);
        }
        
        // Difficulty
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            txtDifficultyDetail.setText(difficulty);
            txtDifficultyDetail.setVisibility(View.VISIBLE);
        } else {
            txtDifficultyDetail.setVisibility(View.GONE);
        }
        
        // Servings
        if (servings > 0) {
            txtServingsDetail.setText(servings + " servings");
            txtServingsDetail.setVisibility(View.VISIBLE);
        } else {
            txtServingsDetail.setVisibility(View.GONE);
        }
        
        // Category
        if (category != null && !category.trim().isEmpty()) {
            txtCategoryDetail.setText(category);
            txtCategoryDetail.setVisibility(View.VISIBLE);
        } else {
            txtCategoryDetail.setVisibility(View.GONE);
        }
        
        // Ingredients
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
        
        // Load image
        if (imageResId != 0) {
            Glide.with(requireContext())
                    .load(imageResId)
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(imgRecipeDetail);
        } else {
            imgRecipeDetail.setImageResource(R.drawable.ic_placeholder);
        }
        
        Log.d(TAG, "🔥 Universal recipe data setup complete");
    }
    
    // 🔥 UNIVERSAL GETTER METHODS - Handle both Recipe types
    
    private String getRecipeTitle() {
        try {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                return ((com.example.kitchenbrain.model.Recipe) recipe).getTitle();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                return ((com.example.kitchenbrain.Recipe) recipe).getName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting title", e);
        }
        return "Unknown Recipe";
    }
    
    private String getRecipeDescription() {
        try {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                return ((com.example.kitchenbrain.model.Recipe) recipe).getDescription();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                return ((com.example.kitchenbrain.Recipe) recipe).getDescription();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting description", e);
        }
        return null;
    }
    
    private long getRecipeCookingTime() {
        try {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                return ((com.example.kitchenbrain.model.Recipe) recipe).getCookingTime();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                return ((com.example.kitchenbrain.Recipe) recipe).getCookingTime();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting cooking time", e);
        }
        return 0;
    }
    
    private String getRecipeDifficulty() {
        try {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                return ((com.example.kitchenbrain.model.Recipe) recipe).getDifficulty();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                return ((com.example.kitchenbrain.Recipe) recipe).getDifficulty();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting difficulty", e);
        }
        return null;
    }
    
    private int getRecipeServings() {
        try {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                return ((com.example.kitchenbrain.model.Recipe) recipe).getServings();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                return ((com.example.kitchenbrain.Recipe) recipe).getServings();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting servings", e);
        }
        return 0;
    }
    
    private String getRecipeCategory() {
        try {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                return ((com.example.kitchenbrain.model.Recipe) recipe).getCategory();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                // Spoonacular Recipe doesn't have direct category, use empty
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting category", e);
        }
        return null;
    }
    
    private List<String> getRecipeIngredients() {
        try {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                return ((com.example.kitchenbrain.model.Recipe) recipe).getIngredients();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                return ((com.example.kitchenbrain.Recipe) recipe).getIngredients();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting ingredients", e);
        }
        return null;
    }
    
    private List<String> getRecipeInstructions() {
        try {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                return ((com.example.kitchenbrain.model.Recipe) recipe).getInstructions();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                // Convert cookingInstructions to list
                String cookingInstructions = ((com.example.kitchenbrain.Recipe) recipe).getCookingInstructions();
                if (cookingInstructions != null && !cookingInstructions.trim().isEmpty()) {
                    List<String> instructions = new java.util.ArrayList<>();
                    instructions.add(cookingInstructions);
                    return instructions;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting instructions", e);
        }
        return null;
    }
    
    private int getRecipeImageResId() {
        try {
            if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
                return ((com.example.kitchenbrain.model.Recipe) recipe).getImageResId();
            } else if (recipe instanceof com.example.kitchenbrain.Recipe) {
                // Spoonacular Recipe doesn't have getImageResId(), return 0
                return 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting image", e);
        }
        return 0;
    }
}
