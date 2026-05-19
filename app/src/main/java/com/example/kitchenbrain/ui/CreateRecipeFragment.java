package com.example.kitchenbrain.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.ModernIngredientsPickerFragment;
import com.example.kitchenbrain.Recipe;
import com.example.kitchenbrain.model.SocialRecipe;
import com.example.kitchenbrain.repository.RecipeRepository;
import com.example.kitchenbrain.util.RecipeOwnershipUtils;
import com.example.kitchenbrain.utils.CloudinaryHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 🔥 UNIVERSAL CREATE/EDIT RECIPE FRAGMENT
 */
public class CreateRecipeFragment extends Fragment {

    private static final String TAG = "CreateRecipeFragment";
    private static final String ARG_RECIPE = "recipe_to_edit";

    private ImageView recipeImageView;
    private ImageView videoIcon;
    private TextView textVideoUrl;
    private EditText titleEditText, descriptionEditText, notesEditText, ingredientsEditText, stepEditText;
    private Button publishButton, addStepButton, deleteStepButton, addIngredientButton;
    private ProgressBar progressBar;
    
    private Spinner difficultySpinner;
    private EditText cookTimeEditText, servingsEditText, caloriesEditText;
    private ChipGroup tagsChipGroup, ingredientsChipGroup;
    private RecyclerView stepsRecyclerView;
    private StepsAdapter stepsAdapter;

    private Uri selectedImageUri;
    private Uri selectedVideoUri;
    private final List<String> selectedIngredientNames = new ArrayList<>();
    private final Set<String> selectedIngredientIds = new HashSet<>();
    private final List<String> stepsList = new ArrayList<>();
    private int editingIngredientIndex = RecyclerView.NO_POSITION;
    
    private RecipeRepository recipeRepository;
    private Object recipeToEdit;
    private boolean isEditMode = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (recipeImageView != null && selectedImageUri != null) {
                        Glide.with(this).load(selectedImageUri).centerCrop().into(recipeImageView);
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
    
    public static CreateRecipeFragment newInstance(Object recipe) {
        CreateRecipeFragment fragment = new CreateRecipeFragment();
        Bundle args = new Bundle();
        if (recipe instanceof Serializable) {
            args.putSerializable(ARG_RECIPE, (Serializable) recipe);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipeRepository = new RecipeRepository();
        if (getArguments() != null) {
            recipeToEdit = getArguments().getSerializable(ARG_RECIPE);
            isEditMode = recipeToEdit != null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_recipe, container, false);
        initializeViews(view);
        setupRecyclerView();
        setupSpinners();
        setupClickListeners();
        setupIngredientResultListener();
        
        if (isEditMode) {
            if (!canEditCurrentRecipe()) {
                Toast.makeText(getContext(), "You can only edit recipes you created", Toast.LENGTH_SHORT).show();
                if (publishButton != null) publishButton.setEnabled(false);
                view.post(() -> {
                    if (isAdded()) getParentFragmentManager().popBackStack();
                });
                return view;
            }
            prefillData();
        }
        
        return view;
    }

    private void initializeViews(View view) {
        recipeImageView = view.findViewById(R.id.recipeImage);
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        videoIcon = view.findViewById(R.id.recipeVideo);
        textVideoUrl = view.findViewById(R.id.textVideoUrl);
        
        titleEditText = view.findViewById(R.id.titleEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        notesEditText = view.findViewById(R.id.notesEditText);
        ingredientsEditText = view.findViewById(R.id.ingredientsEditText);
        stepEditText = view.findViewById(R.id.stepEditText);
        
        View btnIngredients = view.findViewById(R.id.btnIngredients);
        addIngredientButton = view.findViewById(R.id.addIngredientButton);
        View btnSaveRecipe = view.findViewById(R.id.btnSaveRecipe);
        publishButton = view.findViewById(R.id.publishButton);
        addStepButton = view.findViewById(R.id.addStepButton);
        deleteStepButton = view.findViewById(R.id.deleteStepButton);
        
        difficultySpinner = view.findViewById(R.id.difficultySpinner);
        cookTimeEditText = view.findViewById(R.id.cookTimeEditText);
        servingsEditText = view.findViewById(R.id.servingsEditText);
        caloriesEditText = view.findViewById(R.id.caloriesEditText);
        
        tagsChipGroup = view.findViewById(R.id.tagsChipGroup);
        ingredientsChipGroup = view.findViewById(R.id.ingredientsChipGroup);
        stepsRecyclerView = view.findViewById(R.id.stepsRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);

        if (btnIngredients != null) btnIngredients.setOnClickListener(v -> openIngredientsPicker());
        if (btnSaveRecipe != null) btnSaveRecipe.setOnClickListener(v -> saveDraft());
        
        if (toolbar != null) {
            toolbar.setTitle(isEditMode ? "Edit Recipe" : "Create Recipe");
        }
        if (isEditMode && publishButton != null) {
            publishButton.setText(R.string.edit_recipe_button);
        }
    }

    private void setupRecyclerView() {
        stepsAdapter = new StepsAdapter(stepsList);
        stepsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        stepsRecyclerView.setAdapter(stepsAdapter);
        stepsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void prefillData() {
        if (recipeToEdit == null) return;
        
        String title = "";
        String desc = "";
        String diff = "Easy";
        long cookTime = 0;
        int servings = 0;
        int calories = 0;
        String imageUrl = null;
        List<String> ingredients = new ArrayList<>();
        List<String> ingredientIds = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        List<String> tags = new ArrayList<>();

        if (recipeToEdit instanceof Recipe r) {
            title = r.getTitle();
            desc = r.getDescription();
            if (notesEditText != null) notesEditText.setText(r.getNotes());
            diff = r.getDifficulty();
            cookTime = r.getCookingTime();
            servings = r.getServings();
            calories = r.getCalories();
            imageUrl = r.getImageUrl();
            ingredients = r.getIngredients();
            ingredientIds = r.getIngredientIds();
            steps = r.getSteps();
            tags = r.getTags();
        } else if (recipeToEdit instanceof com.example.kitchenbrain.model.Recipe r) {
            title = r.getTitle();
            desc = r.getDescription();
            if (notesEditText != null) notesEditText.setText(r.getNotes());
            diff = r.getDifficulty();
            cookTime = r.getCookingTime();
            servings = r.getServings();
            calories = r.getCalories();
            imageUrl = r.getImageUrl();
            ingredients = r.getIngredients();
            ingredientIds = r.getIngredientIds();
            steps = r.getInstructions();
        } else if (recipeToEdit instanceof SocialRecipe r) {
            title = r.getTitle();
            desc = r.getDescription();
            if (notesEditText != null) notesEditText.setText(r.getNotes());
            diff = r.getDifficulty();
            cookTime = r.getCookTime();
            servings = r.getServings();
            calories = r.getCalories();
            imageUrl = r.getImageUrl();
            ingredients = r.getIngredients();
            ingredientIds = r.getIngredientIds();
            steps = r.getSteps();
            tags = r.getTags();
        }

        if (titleEditText != null) titleEditText.setText(title);
        if (descriptionEditText != null) descriptionEditText.setText(desc);
        if (cookTimeEditText != null) cookTimeEditText.setText(String.valueOf(cookTime));
        if (servingsEditText != null) servingsEditText.setText(String.valueOf(servings));
        if (caloriesEditText != null) caloriesEditText.setText(String.valueOf(calories));
        
        if (imageUrl != null && recipeImageView != null) {
            Glide.with(this).load(imageUrl).centerCrop().into(recipeImageView);
        }

        if (ingredients != null) {
            selectedIngredientNames.addAll(ingredients);
            updateIngredientsChips();
        }
        if (ingredientIds != null) selectedIngredientIds.addAll(ingredientIds);

        if (steps != null) {
            stepsList.addAll(steps);
            stepsAdapter.notifyDataSetChanged();
        }
        
        if (difficultySpinner != null && difficultySpinner.getAdapter() != null) {
            for (int i = 0; i < difficultySpinner.getCount(); i++) {
                if (difficultySpinner.getItemAtPosition(i).toString().equalsIgnoreCase(diff)) {
                    difficultySpinner.setSelection(i);
                    break;
                }
            }
        }
        
        if (tags != null && tagsChipGroup != null) {
            for (int i = 0; i < tagsChipGroup.getChildCount(); i++) {
                View child = tagsChipGroup.getChildAt(i);
                if (child instanceof Chip chip) {
                    if (tags.contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    }
                }
            }
        }
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
        if (tagsChipGroup == null || tagsChipGroup.getChildCount() > 0) return;
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
                if (ingredientsEditText == null) return;
                String ingredient = ingredientsEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(ingredient)) {
                    if (editingIngredientIndex >= 0 && editingIngredientIndex < selectedIngredientNames.size()) {
                        selectedIngredientNames.set(editingIngredientIndex, ingredient);
                    } else {
                        selectedIngredientNames.add(ingredient);
                    }
                    editingIngredientIndex = RecyclerView.NO_POSITION;
                    updateIngredientsChips();
                    ingredientsEditText.setText("");
                    addIngredientButton.setText("Add");
                }
            });
        }

        if (addStepButton != null) {
            addStepButton.setOnClickListener(v -> {
                if (stepEditText == null) return;
                String step = stepEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(step)) {
                    stepsList.add(step);
                    stepsAdapter.notifyItemInserted(stepsList.size() - 1);
                    stepEditText.setText("");
                    stepsRecyclerView.post(() -> stepsRecyclerView.smoothScrollToPosition(stepsList.size() - 1));
                } else {
                    stepEditText.setError("Please enter step description");
                }
            });
        }

        if (deleteStepButton != null) {
            deleteStepButton.setOnClickListener(v -> {
                if (!stepsList.isEmpty()) {
                    int pos = stepsList.size() - 1;
                    stepsList.remove(pos);
                    stepsAdapter.notifyItemRemoved(pos);
                }
            });
        }

        if (publishButton != null) publishButton.setOnClickListener(v -> publishRecipe());
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
        for (int i = 0; i < selectedIngredientNames.size(); i++) {
            String ingredient = selectedIngredientNames.get(i);
            int index = i;
            Chip chip = new Chip(getContext());
            chip.setText(ingredient);
            chip.setCloseIconVisible(true);
            chip.setClickable(true);
            chip.setFocusable(false);
            chip.setOnClickListener(v -> beginIngredientEdit(index));
            chip.setOnCloseIconClickListener(v -> {
                selectedIngredientNames.remove(index);
                if (editingIngredientIndex == index) {
                    editingIngredientIndex = RecyclerView.NO_POSITION;
                    if (ingredientsEditText != null) ingredientsEditText.setText("");
                    if (addIngredientButton != null) addIngredientButton.setText("Add");
                } else if (editingIngredientIndex > index) {
                    editingIngredientIndex--;
                }
                updateIngredientsChips();
            });
            ingredientsChipGroup.addView(chip);
        }
    }

    private void beginIngredientEdit(int index) {
        if (ingredientsEditText == null || index < 0 || index >= selectedIngredientNames.size()) return;
        editingIngredientIndex = index;
        ingredientsEditText.setText(selectedIngredientNames.get(index));
        ingredientsEditText.setSelection(ingredientsEditText.length());
        if (addIngredientButton != null) addIngredientButton.setText("Update");
        showKeyboard(ingredientsEditText);
    }

    private void showKeyboard(EditText editText) {
        editText.requestFocus();
        editText.post(() -> {
            InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
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
            CloudinaryHelper.uploadImage(requireContext(), selectedImageUri, url -> createAndPublishRecipe(uid, username, url), error -> {
                showLoading(false);
                Toast.makeText(getContext(), "Image Upload Failed: " + error, Toast.LENGTH_SHORT).show();
            });
        } else {
            String existingUrl = null;
            if (isEditMode) {
                if (recipeToEdit instanceof Recipe r) existingUrl = r.getImageUrl();
                else if (recipeToEdit instanceof com.example.kitchenbrain.model.Recipe r) existingUrl = r.getImageUrl();
                else if (recipeToEdit instanceof SocialRecipe r) existingUrl = r.getImageUrl();
            }
            createAndPublishRecipe(uid, username, existingUrl);
        }
    }

    private void createAndPublishRecipe(String uid, String username, String imageUrl) {
        if (isEditMode) {
            updateExistingRecipe(imageUrl);
            return;
        }

        SocialRecipe recipe;
        recipe = new SocialRecipe();
        recipe.setAuthorId(uid);
        recipe.setAuthorName(username);
        
        recipe.setTitle(titleEditText.getText().toString().trim());
        recipe.setDescription(descriptionEditText.getText().toString().trim());
        recipe.setNotes(notesEditText != null ? notesEditText.getText().toString().trim() : "");
        recipe.setImageUrl(imageUrl);
        recipe.setIngredients(getCleanTextList(selectedIngredientNames));
        recipe.setSteps(getCleanTextList(stepsList));
        
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

        recipeRepository.createRecipe(recipe, new RecipeRepository.RecipeCallback<>() {
            @Override public void onSuccess(String result) {
                showLoading(false);
                Toast.makeText(getContext(), "Recipe published!", Toast.LENGTH_SHORT).show();
                if (isAdded()) getParentFragmentManager().popBackStack();
            }
            @Override public void onError(String error) {
                showLoading(false);
                Toast.makeText(getContext(), "Failed to publish: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateExistingRecipe(String imageUrl) {
        if (!canEditCurrentRecipe()) {
            showLoading(false);
            Toast.makeText(getContext(), "You can only edit recipes you created", Toast.LENGTH_SHORT).show();
            return;
        }

        String recipeId = getEditableRecipeId();
        Map<String, Object> updates = new HashMap<>();
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        List<String> ingredients = getCleanTextList(selectedIngredientNames);
        List<String> steps = getCleanTextList(stepsList);

        updates.put("title", title);
        updates.put("name", title);
        updates.put("description", description);
        updates.put("notes", notesEditText != null ? notesEditText.getText().toString().trim() : "");
        updates.put("ingredients", ingredients);
        updates.put("ingredientIds", new ArrayList<>(selectedIngredientIds));
        updates.put("steps", steps);
        updates.put("instructions", steps);
        updates.put("cookingInstructions", buildInstructionsText(steps));
        updates.put("updatedAt", FieldValue.serverTimestamp());

        if (imageUrl != null) {
            updates.put("imageUrl", imageUrl);
        }
        if (difficultySpinner != null && difficultySpinner.getSelectedItem() != null) {
            updates.put("difficulty", difficultySpinner.getSelectedItem().toString());
        }

        updates.put("cookingTime", parseLong(cookTimeEditText));
        updates.put("cookTime", parseLong(cookTimeEditText));
        updates.put("servings", parseInt(servingsEditText));
        updates.put("calories", parseInt(caloriesEditText));
        updates.put("tags", getSelectedTags());

        recipeRepository.updateRecipeFields(recipeId, updates, new RecipeRepository.RecipeCallback<>() {
            @Override public void onSuccess(Void result) {
                showLoading(false);
                if (getView() != null) {
                    Snackbar.make(getView(), "Recipe updated", Snackbar.LENGTH_SHORT).show();
                }
                Bundle resultBundle = new Bundle();
                resultBundle.putString("recipe_id", recipeId);
                getParentFragmentManager().setFragmentResult("recipe_updated", resultBundle);
                if (isAdded()) getParentFragmentManager().popBackStack();
            }

            @Override public void onError(String error) {
                showLoading(false);
                Toast.makeText(getContext(), "Failed to update: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getEditableRecipeId() {
        if (recipeToEdit instanceof Recipe r) return r.getId();
        if (recipeToEdit instanceof com.example.kitchenbrain.model.Recipe r) return r.getId();
        if (recipeToEdit instanceof SocialRecipe r) return r.getRecipeId();
        return null;
    }

    private boolean canEditCurrentRecipe() {
        return RecipeOwnershipUtils.isOwner(recipeToEdit, FirebaseAuth.getInstance().getUid());
    }

    private String buildInstructionsText(List<String> steps) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            builder.append(i + 1).append(". ").append(steps.get(i));
            if (i < steps.size() - 1) builder.append('\n');
        }
        return builder.toString();
    }

    private List<String> getSelectedTags() {
        List<String> selectedTags = new ArrayList<>();
        if (tagsChipGroup == null) return selectedTags;
        for (int i = 0; i < tagsChipGroup.getChildCount(); i++) {
            View child = tagsChipGroup.getChildAt(i);
            if (child instanceof Chip chip && chip.isChecked()) {
                selectedTags.add(chip.getText().toString());
            }
        }
        return selectedTags;
    }

    private List<String> getCleanTextList(List<String> source) {
        List<String> clean = new ArrayList<>();
        if (source == null) return clean;
        for (String item : source) {
            if (!TextUtils.isEmpty(item) && !TextUtils.isEmpty(item.trim())) {
                clean.add(item.trim());
            }
        }
        return clean;
    }

    private long parseLong(EditText editText) {
        if (editText == null || TextUtils.isEmpty(editText.getText())) return 0;
        try {
            return Long.parseLong(editText.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseInt(EditText editText) {
        if (editText == null || TextUtils.isEmpty(editText.getText())) return 0;
        try {
            return Integer.parseInt(editText.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean validateInputs() {
        if (titleEditText == null || TextUtils.isEmpty(titleEditText.getText())) {
            if (titleEditText != null) titleEditText.setError("Title required");
            return false;
        }
        if (getCleanTextList(selectedIngredientNames).isEmpty()) {
            Toast.makeText(getContext(), "Add at least one ingredient", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getCleanTextList(stepsList).isEmpty()) {
            Toast.makeText(getContext(), "Add at least one step", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (publishButton != null) publishButton.setEnabled(!isLoading);
    }

    private static class StepsAdapter extends RecyclerView.Adapter<StepsAdapter.StepViewHolder> {
        private final List<String> steps;
        StepsAdapter(List<String> steps) { this.steps = steps; }
        @NonNull @Override public StepViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_editable_cooking_step, p, false);
            return new StepViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull StepViewHolder h, int p) {
            h.bind(steps.get(p), p, steps, this);
        }
        @Override public int getItemCount() { return steps.size(); }
        static class StepViewHolder extends RecyclerView.ViewHolder {
            EditText editStep;
            TextView number;
            Button deleteButton;
            TextWatcher watcher;
            StepViewHolder(View v) { 
                super(v); 
                editStep = v.findViewById(R.id.editStep);
                number = v.findViewById(R.id.textStepNumber);
                deleteButton = v.findViewById(R.id.btnDeleteStep);
            }

            void bind(String step, int position, List<String> steps, RecyclerView.Adapter<?> adapter) {
                if (watcher != null) {
                    editStep.removeTextChangedListener(watcher);
                }
                number.setText(String.valueOf(position + 1));
                editStep.setText(step);
                editStep.setSelection(editStep.length());
                watcher = new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable editable) {
                        int adapterPosition = getBindingAdapterPosition();
                        if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < steps.size()) {
                            steps.set(adapterPosition, editable.toString());
                        }
                    }
                };
                editStep.addTextChangedListener(watcher);
                deleteButton.setOnClickListener(v -> {
                    int adapterPosition = getBindingAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < steps.size()) {
                        steps.remove(adapterPosition);
                        adapter.notifyItemRemoved(adapterPosition);
                        adapter.notifyItemRangeChanged(adapterPosition, steps.size() - adapterPosition);
                    }
                });
            }
        }
    }
}
