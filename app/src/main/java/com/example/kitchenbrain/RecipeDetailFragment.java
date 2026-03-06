package com.example.kitchenbrain;

import android.animation.ObjectAnimator;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class RecipeDetailFragment extends Fragment {

    private Recipe recipe;

    private TextView recipeNameTextView;
    private TextView recipeAuthorTextView;
    private TextView recipeTimeTextView;
    private TextView recipeDescriptionTextView;
    private TextView recipeIngredientsTextView;
    private TextView recipeInstructionsTextView;
    private ImageView recipeImageView;
    private VideoView recipeVideoView;
    private Toolbar toolbar;

    // For expandable sections
    private LinearLayout ingredientsHeader;
    private LinearLayout instructionsHeader;
    private LinearLayout ingredientsContent;
    private LinearLayout instructionsContent;
    private ImageView ingredientsArrow;
    private ImageView instructionsArrow;

    public RecipeDetailFragment() {
        // Required empty public constructor
    }

    public static RecipeDetailFragment newInstance(Recipe recipe) {
        RecipeDetailFragment fragment = new RecipeDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("recipe", recipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            recipe = (Recipe) getArguments().getSerializable("recipe");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_detail_modern, container, false);

        initViews(view);
        setupClickListeners();
        populateRecipeDetails();

        return view;
    }

    private void initViews(View view) {
        recipeNameTextView = view.findViewById(R.id.recipeName);
        recipeAuthorTextView = view.findViewById(R.id.recipeAuthor);
        recipeTimeTextView = view.findViewById(R.id.recipeTime);
        recipeDescriptionTextView = view.findViewById(R.id.recipeDescription);
        recipeIngredientsTextView = view.findViewById(R.id.recipeIngredients);
        recipeInstructionsTextView = view.findViewById(R.id.recipeInstructions);
        recipeImageView = view.findViewById(R.id.recipeImage);
        recipeVideoView = view.findViewById(R.id.recipeVideo);
        toolbar = view.findViewById(R.id.toolbar);

        // Initialize expandable sections
        ingredientsHeader = view.findViewById(R.id.ingredientsHeader);
        instructionsHeader = view.findViewById(R.id.instructionsHeader);
        ingredientsContent = view.findViewById(R.id.ingredientsContent);
        instructionsContent = view.findViewById(R.id.instructionsContent);
        ingredientsArrow = view.findViewById(R.id.ingredientsArrow);
        instructionsArrow = view.findViewById(R.id.instructionsArrow);

        // Set up toolbar
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void setupClickListeners() {
        // Set up expandable sections
        if (ingredientsHeader != null) {
            ingredientsHeader.setOnClickListener(v -> toggleSection(ingredientsContent, ingredientsArrow));
        }

        if (instructionsHeader != null) {
            instructionsHeader.setOnClickListener(v -> toggleSection(instructionsContent, instructionsArrow));
        }
    }

    private void toggleSection(LinearLayout content, ImageView arrow) {
        boolean isVisible = content.getVisibility() == View.VISIBLE;
        
        if (isVisible) {
            content.setVisibility(View.GONE);
            animateArrow(arrow, 180f, 0f);
        } else {
            content.setVisibility(View.VISIBLE);
            animateArrow(arrow, 0f, 180f);
        }
    }

    private void animateArrow(ImageView arrow, float fromAngle, float toAngle) {
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(arrow, "rotation", fromAngle, toAngle);
        rotateAnimator.setDuration(300);
        rotateAnimator.start();
    }

    private void populateRecipeDetails() {
        if (recipe == null) {
            Toast.makeText(getContext(), "Error: Recipe data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Populate basic information
        if (recipeNameTextView != null && recipe.getName() != null) {
            recipeNameTextView.setText(recipe.getName());
        }

        if (recipeAuthorTextView != null && recipe.getUsername() != null) {
            recipeAuthorTextView.setText("Created by: " + recipe.getUsername());
        }

        if (recipeTimeTextView != null) {
            recipeTimeTextView.setText(recipe.getCookingTime() + "");
        }

        if (recipeDescriptionTextView != null && recipe.getDescription() != null) {
            recipeDescriptionTextView.setText(recipe.getDescription());
        }

        // Populate ingredients section
        populateIngredientsSection();

        // Populate cooking instructions section
        populateInstructionsSection();

        // Load image if available
        if (recipeImageView != null && recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            try {
                Glide.with(this)
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.ic_placeholder) // Placeholder while loading
                    .error(R.drawable.ic_placeholder) // Default image if load fails
                    .into(recipeImageView);
            } catch (Exception e) {
                // Handle image loading error
                recipeImageView.setImageResource(R.drawable.ic_placeholder);
            }
        } else if (recipeImageView != null) {
            // Hide image view if no image URL
            recipeImageView.setVisibility(View.GONE);
        }

        // Load video if available
        if (recipeVideoView != null && recipe.getVideoUrl() != null && !recipe.getVideoUrl().isEmpty()) {
            try {
                Uri videoUri = Uri.parse(recipe.getVideoUrl());
                recipeVideoView.setVideoURI(videoUri);
                
                // Set up media controller for video playback
                MediaController mediaController = new MediaController(getContext());
                recipeVideoView.setMediaController(mediaController);
                
                // Start playing when ready
                recipeVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        recipeVideoView.start();
                    }
                });
                
                // Handle errors during video playback
                recipeVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Toast.makeText(getContext(), "Error playing video", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
            } catch (Exception e) {
                // Hide video view if there's an error loading the video
                recipeVideoView.setVisibility(View.GONE);
            }
        } else if (recipeVideoView != null) {
            // Hide video view if no video URL
            recipeVideoView.setVisibility(View.GONE);
        }
    }

    private void populateIngredientsSection() {
        if (recipeIngredientsTextView != null && ingredientsContent != null) {
            List<String> ingredients = recipe.getIngredients();
            if (ingredients != null && !ingredients.isEmpty()) {
                ingredientsContent.removeAllViews(); // Clear existing views
                
                for (int i = 0; i < ingredients.size(); i++) {
                    String ingredient = ingredients.get(i);
                    
                    TextView ingredientTextView = new TextView(getContext());
                    ingredientTextView.setText("• " + ingredient);
                    ingredientTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    ingredientTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
                    ingredientTextView.setTypeface(Typeface.SANS_SERIF);
                    ingredientTextView.setPadding(0, 0, 0, 12); // Add bottom padding
                    
                    ingredientsContent.addView(ingredientTextView);
                }
                
                ingredientsContent.setVisibility(View.VISIBLE);
            } else {
                TextView noIngredientsTextView = new TextView(getContext());
                noIngredientsTextView.setText("No ingredients specified");
                noIngredientsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                noIngredientsTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
                
                ingredientsContent.removeAllViews();
                ingredientsContent.addView(noIngredientsTextView);
                ingredientsContent.setVisibility(View.VISIBLE);
            }
        }
    }

    private void populateInstructionsSection() {
        if (recipeInstructionsTextView != null && instructionsContent != null) {
            String instructions = recipe.getCookingInstructions();
            if (instructions != null && !instructions.isEmpty()) {
                instructionsContent.removeAllViews(); // Clear existing views
                
                // Split instructions by new lines or steps if possible
                String[] steps = instructions.split("\\n|\\r\\n|\\.");
                
                for (int i = 0; i < steps.length; i++) {
                    String step = steps[i].trim();
                    if (!step.isEmpty()) {
                        LinearLayout stepLayout = new LinearLayout(getContext());
                        stepLayout.setOrientation(LinearLayout.HORIZONTAL);
                        
                        // Step number
                        TextView stepNumber = new TextView(getContext());
                        stepNumber.setText(String.valueOf(i + 1));
                        stepNumber.setBackground(getResources().getDrawable(R.drawable.step_number_background, null));
                        stepNumber.setGravity(android.view.Gravity.CENTER);
                        stepNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        stepNumber.setTextColor(getResources().getColor(android.R.color.white, null));
                        stepNumber.setWidth(36);
                        stepNumber.setHeight(36);
                        stepNumber.setPadding(0, 4, 0, 4);
                        
                        // Step text
                        TextView stepText = new TextView(getContext());
                        stepText.setText(step);
                        stepText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        stepText.setTextColor(getResources().getColor(R.color.text_secondary, null));
                        stepText.setTypeface(Typeface.SANS_SERIF);
                        stepText.setPadding(24, 0, 0, 24); // Left padding to match spacing
                        
                        stepLayout.addView(stepNumber);
                        stepLayout.addView(stepText);
                        
                        instructionsContent.addView(stepLayout);
                    }
                }
                
                instructionsContent.setVisibility(View.VISIBLE);
            } else {
                TextView noInstructionsTextView = new TextView(getContext());
                noInstructionsTextView.setText("No cooking instructions available");
                noInstructionsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                noInstructionsTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
                
                instructionsContent.removeAllViews();
                instructionsContent.addView(noInstructionsTextView);
                instructionsContent.setVisibility(View.VISIBLE);
            }
        }
    }
}