package com.example.kitchenbrain;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Fragment for creating new recipes
 * Implements proper fragment lifecycle management and safe operations
 */
public class CreateRecipeFragment extends Fragment {

    private static final int PICK_IMAGE = 1;
    private static final int PICK_VIDEO = 2;

    // UI components
    private TextInputEditText recipeName, recipeDescription, cookingTime, cookingInstructions;
    private android.widget.ImageView recipeImage;
    private android.widget.VideoView recipeVideo;
    private MaterialButton btnSaveRecipe, btnIngredients;

    // Media URIs
    private Uri selectedImageUri;
    private Uri selectedVideoUri;

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public CreateRecipeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase services in onCreate to survive configuration changes
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                           @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        // Inflate layout safely
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_create_recipe, container, false);
        } catch (Exception e) {
            showError("Failed to inflate layout: " + e.getMessage());
            return null;
        }

        // Initialize UI components
        initializeViews(view);
        
        // Setup click listeners
        setupClickListeners();

        return view;
    }

    /**
     * Initialize all view components with proper null safety
     */
    private void initializeViews(@NonNull View view) {
        try {
            recipeName = view.findViewById(R.id.recipeName);
            recipeDescription = view.findViewById(R.id.recipeDescription);
            cookingTime = view.findViewById(R.id.cookingTime);
            cookingInstructions = view.findViewById(R.id.cookingInstructions);
            recipeImage = view.findViewById(R.id.recipeImage);
            recipeVideo = view.findViewById(R.id.recipeVideo);
            btnSaveRecipe = view.findViewById(R.id.btnSaveRecipe);
            btnIngredients = view.findViewById(R.id.btnIngredients);

            // Validate that all required views were found
            if (recipeName == null || recipeDescription == null || 
                cookingTime == null || cookingInstructions == null ||
                recipeImage == null || recipeVideo == null || 
                btnSaveRecipe == null || btnIngredients == null) {
                showError("Failed to initialize one or more views");
            }

        } catch (Exception e) {
            showError("Error initializing views: " + e.getMessage());
        }
    }

    /**
     * Setup all click listeners with proper error handling
     */
    private void setupClickListeners() {
        if (btnSaveRecipe != null) {
            btnSaveRecipe.setOnClickListener(v -> saveRecipe());
        }
        
        if (btnIngredients != null) {
            btnIngredients.setOnClickListener(v -> openIngredientsPicker());
        }
        
        if (recipeImage != null) {
            recipeImage.setOnClickListener(v -> openImagePicker());
        }
        
        if (recipeVideo != null) {
            recipeVideo.setOnClickListener(v -> openVideoPicker());
        }
    }

    /**
     * Open image picker with proper error handling
     */
    private void openImagePicker() {
        try {
            if (!isAdded()) {
                return;
            }

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);

        } catch (Exception e) {
            showError("Error opening image picker: " + e.getMessage());
        }
    }

    /**
     * Open video picker with proper error handling
     */
    private void openVideoPicker() {
        try {
            if (!isAdded()) {
                return;
            }

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_VIDEO);

        } catch (Exception e) {
            showError("Error opening video picker: " + e.getMessage());
        }
    }

    /**
     * Open ingredients picker fragment with safe navigation
     */
    private void openIngredientsPicker() {
        try {
            // Validate fragment state
            if (!isAdded()) {
                showError("Fragment is not added to activity");
                return;
            }

            // Get activity and validate it
            android.app.Activity activity = getActivity();
            if (activity == null) {
                showError("Activity is null");
                return;
            }

            // Check if activity is finishing
            if (activity.isFinishing()) {
                return;
            }

            // Validate activity type
            if (!(activity instanceof androidx.fragment.app.FragmentActivity)) {
                showError("Invalid activity type for fragment transactions");
                return;
            }

            androidx.fragment.app.FragmentActivity fragmentActivity = 
                (androidx.fragment.app.FragmentActivity) activity;

            // Double-check activity state
            if (fragmentActivity.isDestroyed()) {
                return;
            }

            // Create destination fragment
            IngredientsFragment ingredientsFragment = new IngredientsFragment();

            // Perform safe fragment transaction
            fragmentActivity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, ingredientsFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();

        } catch (Exception e) {
            showError("Error opening ingredients picker: " + e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            // Validate fragment state
            if (!isAdded() || getActivity() == null) {
                return;
            }

            // Validate result
            if (resultCode != android.app.Activity.RESULT_OK || data == null) {
                return;
            }

            Uri selectedUri = data.getData();
            if (selectedUri == null) {
                return;
            }

            // Handle different request codes
            if (requestCode == PICK_IMAGE) {
                selectedImageUri = selectedUri;
                if (recipeImage != null) {
                    recipeImage.setImageURI(selectedUri);
                }
            } else if (requestCode == PICK_VIDEO) {
                selectedVideoUri = selectedUri;
                if (recipeVideo != null) {
                    recipeVideo.setVideoURI(selectedUri);
                    recipeVideo.start();
                }
            }

        } catch (Exception e) {
            showError("Error processing selected media: " + e.getMessage());
        }
    }

    /**
     * Save recipe to Firebase with comprehensive validation and error handling
     */
    private void saveRecipe() {
        try {
            // Check if we're in guest mode
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.isGuestMode()) {
                    // Show message that feature is not available in guest mode
                    showError("Creating recipes requires login. Please sign in to access this feature.");
                    return;
                }
            }
            
            // Validate inputs before saving
            if (!validateInputs()) {
                return;
            }

            // Disable save button during save operation
            if (btnSaveRecipe != null) {
                btnSaveRecipe.setEnabled(false);
                btnSaveRecipe.setText("Saving...");
            }

            // Get input values
            String title = recipeName.getText().toString().trim();
            String description = recipeDescription.getText().toString().trim();
            int cookingTimeInt = Integer.parseInt(cookingTime.getText().toString().trim());
            String instructions = cookingInstructions.getText().toString().trim();

            // Get selected ingredients
            ArrayList<String> selectedIngredients = getSelectedIngredients();

            // Get user information
            String userId = (auth.getCurrentUser() != null) ? 
                auth.getCurrentUser().getUid() : "";
            String username = (auth.getCurrentUser() != null) ? 
                auth.getCurrentUser().getEmail() : "Anonymous";

            // Get media URLs
            String imageUrl = (selectedImageUri != null) ? 
                selectedImageUri.toString() : "";
            String videoUrl = (selectedVideoUri != null) ? 
                selectedVideoUri.toString() : "";

            // Generate recipe ID
            String recipeId = db.collection("recipes").document().getId();

            // Create recipe object with ingredients
            Recipe recipe = new Recipe(
                recipeId,
                title,
                description,
                cookingTimeInt,
                instructions,
                imageUrl,
                videoUrl,
                userId,
                username,
                Timestamp.now(),
                0,
                selectedIngredients
            );

            // Save to Firebase
            saveRecipeToFirestore(recipe);

        } catch (Exception e) {
            showError("Error preparing recipe for save: " + e.getMessage());
            if (btnSaveRecipe != null) {
                btnSaveRecipe.setEnabled(true);
                btnSaveRecipe.setText(R.string.save_recipe_button);
            }
        }
    }

    /**
     * Get selected ingredients from shared preferences
     */
    private ArrayList<String> getSelectedIngredients() {
        try {
            SharedPreferences preferences = requireActivity().getSharedPreferences("SelectedIngredients", Context.MODE_PRIVATE);
            Set<String> selectedIngredientsSet = preferences.getStringSet("ingredients", new HashSet<>());
            return new ArrayList<>(selectedIngredientsSet);
        } catch (Exception e) {
            // Return empty list if there's an error retrieving ingredients
            return new ArrayList<>();
        }
    }

    /**
     * Validate all input fields with proper error handling
     */
    private boolean validateInputs() {
        try {
            // Check if views are initialized
            if (recipeName == null || recipeDescription == null || 
                cookingTime == null || cookingInstructions == null) {
                showError("Form fields not properly initialized");
                return false;
            }

            // Get input values
            String title = recipeName.getText().toString().trim();
            String description = recipeDescription.getText().toString().trim();
            String timeStr = cookingTime.getText().toString().trim();
            String instructions = cookingInstructions.getText().toString().trim();

            // Validate required fields
            if (title.isEmpty()) {
                showError("Please enter a recipe name");
                return false;
            }

            if (description.isEmpty()) {
                showError("Please enter a recipe description");
                return false;
            }

            if (timeStr.isEmpty()) {
                showError("Please enter cooking time");
                return false;
            }

            if (instructions.isEmpty()) {
                showError("Please enter cooking instructions");
                return false;
            }

            // Validate cooking time is a positive integer
            try {
                int time = Integer.parseInt(timeStr);
                if (time <= 0) {
                    showError("Cooking time must be greater than 0");
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("Please enter a valid cooking time (in minutes)");
                return false;
            }

            return true;

        } catch (Exception e) {
            showError("Error validating inputs: " + e.getMessage());
            return false;
        }
    }

    /**
     * Save recipe to Firestore with proper error handling
     */
    private void saveRecipeToFirestore(Recipe recipe) {
        try {
            db.collection("recipes")
                .document(recipe.getId())
                .set(recipe)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && getContext() != null) {
                        showMessage("Recipe saved successfully!");
                        navigateBack();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        showError("Error saving recipe: " + e.getMessage());
                        if (btnSaveRecipe != null) {
                            btnSaveRecipe.setEnabled(true);
                            btnSaveRecipe.setText(R.string.save_recipe_button);
                        }
                    }
                });

        } catch (Exception e) {
            showError("Error initiating save: " + e.getMessage());
            if (btnSaveRecipe != null) {
                btnSaveRecipe.setEnabled(true);
                btnSaveRecipe.setText(R.string.save_recipe_button);
            }
        }
    }

    /**
     * Navigate back to previous fragment with safe navigation
     */
    private void navigateBack() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    if (!isAdded()) return;
                    
                    android.app.Activity activity = getActivity();
                    if (activity == null || activity.isFinishing() || isActivityDestroyed(activity)) {
                        return;
                    }
                    
                    if (!(activity instanceof androidx.fragment.app.FragmentActivity)) {
                        return;
                    }
                    
                    androidx.fragment.app.FragmentActivity fragmentActivity = 
                        (androidx.fragment.app.FragmentActivity) activity;
                    
                    fragmentActivity.getSupportFragmentManager().popBackStack();
                    
                } catch (Exception e) {
                    // Silent catch to prevent crashes
                }
            });
        }
    }
    
    private boolean isActivityDestroyed(android.app.Activity activity) {
        if (activity == null) return true;
        // Check if we're using a newer Android version that has isDestroyed
        try {
            return activity.isDestroyed();
        } catch (NoSuchMethodError e) {
            // For older Android versions, just check isFinishing
            return activity.isFinishing();
        }
    }

    /**
     * Show error message with proper context validation
     */
    private void showError(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show success message with proper context validation
     */
    private void showMessage(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references to prevent memory leaks
        recipeName = null;
        recipeDescription = null;
        cookingTime = null;
        cookingInstructions = null;
        recipeImage = null;
        recipeVideo = null;
        btnSaveRecipe = null;
        btnIngredients = null;
    }
}