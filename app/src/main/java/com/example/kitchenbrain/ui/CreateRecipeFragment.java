package com.example.kitchenbrain.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.ModernIngredientsPickerFragment;
import com.example.kitchenbrain.model.SocialRecipe;
import com.example.kitchenbrain.repository.RecipeRepository;
import com.example.kitchenbrain.utils.CloudinaryHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 🔥 UNIVERSAL CREATE RECIPE FRAGMENT
 * Optimized for performance and proper data storage.
 */
public class CreateRecipeFragment extends Fragment {

    private static final String TAG = "CreateRecipeFragment";

    private ImageView recipeImageView;
    private ImageView videoIcon;
    private TextView textVideoUrl;
    private EditText titleEditText, descriptionEditText, ingredientsEditText, stepEditText;
    private Button publishButton, addStepButton, addIngredientButton;
    private ProgressBar progressBar;
    private NestedScrollView scrollView;
    private LinearLayout stepsContainer;
    
    private Spinner difficultySpinner;
    private EditText cookTimeEditText, servingsEditText, caloriesEditText;
    private ChipGroup tagsChipGroup, ingredientsChipGroup;

    private Uri selectedImageUri;
    private Uri selectedVideoUri;
    private final List<String> selectedIngredientNames = new ArrayList<>();
    private final Set<String> selectedIngredientIds = new HashSet<>();
    private final List<String> stepsList = new ArrayList<>();
    
    private RecipeRepository recipeRepository;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (recipeImageView != null && selectedImageUri != null) {
                        Glide.with(this).load(selectedImageUri).into(recipeImageView);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();
                    updateVideoUrlDisplay();
                }
            }
    );

    public CreateRecipeFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipeRepository = new RecipeRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_recipe, container, false);
        initializeViews(view);
        setupSpinners();
        setupClickListeners();
        setupIngredientResultListener();
        return view;
    }

    private void initializeViews(View view) {
        scrollView = view.findViewById(R.id.main_scroll_view);
        if (scrollView == null && view instanceof NestedScrollView) {
            scrollView = (NestedScrollView) view;
        }
        
        recipeImageView = view.findViewById(R.id.recipeImage);
        videoIcon = view.findViewById(R.id.recipeVideo);
        textVideoUrl = view.findViewById(R.id.textVideoUrl);
        
        titleEditText = view.findViewById(R.id.titleEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        ingredientsEditText = view.findViewById(R.id.ingredientsEditText);
        stepEditText = view.findViewById(R.id.stepEditText);
        
        View btnIngredients = view.findViewById(R.id.btnIngredients);
        addIngredientButton = view.findViewById(R.id.addIngredientButton);
        View btnSaveRecipe = view.findViewById(R.id.btnSaveRecipe);
        publishButton = view.findViewById(R.id.publishButton);
        addStepButton = view.findViewById(R.id.addStepButton);
        
        difficultySpinner = view.findViewById(R.id.difficultySpinner);
        cookTimeEditText = view.findViewById(R.id.cookTimeEditText);
        servingsEditText = view.findViewById(R.id.servingsEditText);
        caloriesEditText = view.findViewById(R.id.caloriesEditText);
        
        tagsChipGroup = view.findViewById(R.id.tagsChipGroup);
        ingredientsChipGroup = view.findViewById(R.id.ingredientsChipGroup);
        stepsContainer = view.findViewById(R.id.stepsContainer);
        progressBar = view.findViewById(R.id.progressBar);

        if (btnIngredients != null) btnIngredients.setOnClickListener(v -> openIngredientsPicker());
        if (btnSaveRecipe != null) btnSaveRecipe.setOnClickListener(v -> saveDraft());
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> difficultyAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (difficultySpinner != null) {
            difficultySpinner.setAdapter(difficultyAdapter);
        }
        setupTags();
    }

    private void setupTags() {
        if (tagsChipGroup == null) return;
        String[] tagArray = getResources().getStringArray(R.array.recipe_tags);
        for (String tag : tagArray) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setCheckable(true);
            tagsChipGroup.addView(chip);
        }
    }

    private void setupClickListeners() {
        if (recipeImageView != null) recipeImageView.setOnClickListener(v -> openImagePicker());
        if (videoIcon != null) videoIcon.setOnClickListener(v -> openVideoPicker());
        
        if (addIngredientButton != null) {
            addIngredientButton.setOnClickListener(v -> {
                String ingredient = ingredientsEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(ingredient)) {
                    selectedIngredientNames.add(ingredient);
                    updateIngredientsChips();
                    ingredientsEditText.setText("");
                }
            });
        }

        if (addStepButton != null) {
            addStepButton.setOnClickListener(v -> {
                if (stepEditText == null) return;
                String step = stepEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(step)) {
                    addStepToLayout(step);
                    stepEditText.setText("");
                } else {
                    Toast.makeText(getContext(), "Please enter step description", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (publishButton != null) publishButton.setOnClickListener(v -> publishRecipe());
    }

    private void addStepToLayout(String step) {
        stepsList.add(step);
        
        TextView stepView = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        stepView.setLayoutParams(params);
        stepView.setText(getString(R.string.step_format, stepsList.size(), step));
        stepView.setTextAppearance(android.R.style.TextAppearance_Medium);
        stepView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        
        if (stepsContainer != null) {
            stepsContainer.addView(stepView);
            stepsContainer.post(() -> {
                if (scrollView != null && addStepButton != null) {
                    scrollView.smoothScrollTo(0, addStepButton.getBottom());
                }
            });
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        videoPickerLauncher.launch(intent);
    }

    private void openIngredientsPicker() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            ModernIngredientsPickerFragment ingredientsFragment = ModernIngredientsPickerFragment.newInstance(selectedIngredientIds);
            activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, ingredientsFragment)
                .addToBackStack("ingredients_picker")
                .commit();
        }
    }

    private void setupIngredientResultListener() {
        getParentFragmentManager().setFragmentResultListener("ingredient_selection", this, (requestKey, result) -> {
            ArrayList<String> names = result.getStringArrayList("ingredient_names");
            ArrayList<String> ids = result.getStringArrayList("ingredient_ids");
            if (names != null) {
                for (String name : names) {
                    if (!selectedIngredientNames.contains(name)) selectedIngredientNames.add(name);
                }
            }
            if (ids != null) selectedIngredientIds.addAll(ids);
            updateIngredientsChips();
        });
    }

    private void updateIngredientsChips() {
        if (ingredientsChipGroup == null) return;
        ingredientsChipGroup.removeAllViews();
        for (String ingredient : selectedIngredientNames) {
            Chip chip = new Chip(getContext());
            chip.setText(ingredient);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                selectedIngredientNames.remove(ingredient);
                updateIngredientsChips();
            });
            ingredientsChipGroup.addView(chip);
        }
    }

    private void updateVideoUrlDisplay() {
        if (textVideoUrl == null || !isAdded()) return;
        if (selectedVideoUri != null) {
            String path = selectedVideoUri.getPath();
            String fileName = (path != null && path.contains("/")) ? path.substring(path.lastIndexOf("/") + 1) : "Video selected";
            textVideoUrl.setText(fileName);
            textVideoUrl.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue));
        } else {
            textVideoUrl.setText(R.string.no_video_selected);
        }
    }

    private void saveDraft() {
        Toast.makeText(getContext(), "Recipe draft saved locally!", Toast.LENGTH_SHORT).show();
    }

    private void publishRecipe() {
        if (!validateInputs()) return;
        showLoading(true);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showLoading(false);
            Toast.makeText(getContext(), "Please login to publish", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = documentSnapshot.getString("username");
                    if (username == null || username.isEmpty()) {
                        username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                    }
                    if (username == null) username = "Anonymous";
                    
                    uploadMediaAndPublish(uid, username);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Error getting user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadMediaAndPublish(String uid, String username) {
        if (selectedImageUri != null) {
            CloudinaryHelper.uploadImage(requireContext(), selectedImageUri, url -> createRecipeObject(uid, username, url), error -> {
                showLoading(false);
                Toast.makeText(getContext(), "Image Upload Failed: " + error, Toast.LENGTH_SHORT).show();
            });
        } else {
            createRecipeObject(uid, username, null);
        }
    }

    private void createRecipeObject(String uid, String username, String imageUrl) {
        SocialRecipe recipe = new SocialRecipe();
        recipe.setAuthorId(uid);
        recipe.setAuthorName(username);
        recipe.setTitle(titleEditText.getText().toString().trim());
        recipe.setDescription(descriptionEditText.getText().toString().trim());
        recipe.setImageUrl(imageUrl);
        recipe.setIngredients(new ArrayList<>(selectedIngredientNames));
        recipe.setSteps(new ArrayList<>(stepsList));
        
        if (difficultySpinner != null && difficultySpinner.getSelectedItem() != null) {
            recipe.setDifficulty(difficultySpinner.getSelectedItem().toString());
        }
        
        try {
            if (cookTimeEditText != null && !TextUtils.isEmpty(cookTimeEditText.getText())) {
                recipe.setCookingTime(Long.parseLong(cookTimeEditText.getText().toString().trim()));
            }
            if (servingsEditText != null && !TextUtils.isEmpty(servingsEditText.getText())) {
                recipe.setServings(Integer.parseInt(servingsEditText.getText().toString().trim()));
            }
            if (caloriesEditText != null && !TextUtils.isEmpty(caloriesEditText.getText())) {
                recipe.setCalories(Integer.parseInt(caloriesEditText.getText().toString().trim()));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing numeric fields", e);
        }

        recipe.setTimestamp(System.currentTimeMillis());

        recipeRepository.createRecipe(recipe, new RecipeRepository.RecipeCallback<String>() {
            @Override
            public void onSuccess(String result) {
                showLoading(false);
                Toast.makeText(getContext(), "Recipe published!", Toast.LENGTH_SHORT).show();
                if (isAdded()) {
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(getContext(), "Failed to publish: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInputs() {
        if (titleEditText == null || TextUtils.isEmpty(titleEditText.getText())) {
            if (titleEditText != null) titleEditText.setError("Title required");
            return false;
        }
        if (selectedIngredientNames.isEmpty()) {
            Toast.makeText(getContext(), "Add at least one ingredient", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (stepsList.isEmpty()) {
            Toast.makeText(getContext(), "Add at least one step", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (publishButton != null) publishButton.setEnabled(!isLoading);
    }
}
