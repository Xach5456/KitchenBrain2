package com.example.kitchenbrain;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.model.Recipe;
import com.example.kitchenbrain.model.SocialRecipe;
import com.example.kitchenbrain.repository.RecipeRepository;
import com.example.kitchenbrain.util.RecipeOwnershipUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 🔥 UNIVERSAL CREATE/EDIT RECIPE FRAGMENT
 * Optimized for stability and fixed deployment/build issues.
 */
public class CreateRecipeFragment extends Fragment {

    private static final String ARG_RECIPE = "recipe_to_edit";

    // UI Components
    private ImageView recipeImageView;
    private ImageView videoIcon;
    private TextView textVideoUrl;
    private EditText titleEditText, descriptionEditText;
    private TextInputEditText ingredientsEditText, stepEditText;
    private MaterialButton btnIngredients, btnSaveRecipe, addIngredientButton;
    private Button publishButton, addStepButton, deleteStepButton;
    private ProgressBar progressBar;
    
    private Spinner difficultySpinner;
    private EditText cookTimeEditText, servingsEditText, caloriesEditText;
    private ChipGroup tagsChipGroup, ingredientsChipGroup;
    private RecyclerView stepsRecyclerView;

    // Data
    private Uri selectedImageUri;
    private Uri selectedVideoUri;
    private final List<String> selectedIngredientNames = new ArrayList<>();
    private final Set<String> selectedIngredientIds = new HashSet<>();
    private final List<String> stepsList = new ArrayList<>();
    private int editingIngredientIndex = RecyclerView.NO_POSITION;
    
    private StepsAdapter stepsAdapter;
    private RecipeRepository recipeRepository;
    private StorageReference storageReference;
    
    private Object recipeToEdit;
    private boolean isEditMode = false;

    // Activity Result Launchers
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (recipeImageView != null && selectedImageUri != null) {
                            Glide.with(CreateRecipeFragment.this)
                                    .load(selectedImageUri)
                                    .centerCrop()
                                    .into(recipeImageView);
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedVideoUri = result.getData().getData();
                        updateVideoUrlDisplay();
                    }
                }
            }
    );

    public CreateRecipeFragment() {
        // Required empty public constructor
    }
    
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
        storageReference = FirebaseStorage.getInstance().getReference();
        if (getArguments() != null) {
            recipeToEdit = getArguments().getSerializable(ARG_RECIPE);
            isEditMode = recipeToEdit != null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_recipe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupSpinners();
        setupClickListeners(view);
        setupIngredientResultListener();
        
        if (isEditMode) {
            if (!canEditCurrentRecipe()) {
                Toast.makeText(getContext(), "You can only edit recipes you created", Toast.LENGTH_SHORT).show();
                if (publishButton != null) publishButton.setEnabled(false);
                getParentFragmentManager().popBackStack();
                return;
            }
            prefillData();
        }
    }

    private void initializeViews(View view) {
        recipeImageView = view.findViewById(R.id.recipeImage);
        videoIcon = view.findViewById(R.id.recipeVideo);
        textVideoUrl = view.findViewById(R.id.textVideoUrl);
        
        titleEditText = view.findViewById(R.id.titleEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        ingredientsEditText = view.findViewById(R.id.ingredientsEditText);
        stepEditText = view.findViewById(R.id.stepEditText);
        
        btnIngredients = view.findViewById(R.id.btnIngredients);
        btnSaveRecipe = view.findViewById(R.id.btnSaveRecipe);
        publishButton = view.findViewById(R.id.publishButton);
        addStepButton = view.findViewById(R.id.addStepButton);
        deleteStepButton = view.findViewById(R.id.deleteStepButton);
        addIngredientButton = view.findViewById(R.id.addIngredientButton);
        
        difficultySpinner = view.findViewById(R.id.difficultySpinner);
        cookTimeEditText = view.findViewById(R.id.cookTimeEditText);
        servingsEditText = view.findViewById(R.id.servingsEditText);
        caloriesEditText = view.findViewById(R.id.caloriesEditText);
        
        tagsChipGroup = view.findViewById(R.id.tagsChipGroup);
        ingredientsChipGroup = view.findViewById(R.id.ingredientsChipGroup);
        stepsRecyclerView = view.findViewById(R.id.stepsRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        
        if (isEditMode && publishButton != null) {
            publishButton.setText("Update Recipe");
        }
    }

    private void prefillData() {
        if (recipeToEdit == null) return;
        
        String title = "";
        String desc = "";
        String diff = "Easy";
        int cookTime = 0;
        int servingsVal = 0;
        int caloriesVal = 0;
        String imageUrl = null;
        List<String> ingredients = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        List<String> tagsList = new ArrayList<>();

        if (recipeToEdit instanceof Recipe) {
            Recipe r = (Recipe) recipeToEdit;
            title = r.getTitle();
            desc = r.getDescription();
            diff = r.getDifficulty();
            cookTime = (int) r.getCookingTime();
            servingsVal = r.getServings();
            caloriesVal = r.getCalories();
            imageUrl = r.getImageUrl();
            ingredients = r.getIngredients();
            selectedIngredientIds.clear();
            if (r.getIngredientIds() != null) selectedIngredientIds.addAll(r.getIngredientIds());
            steps = getExistingSteps(r);
        } else if (recipeToEdit instanceof SocialRecipe) {
            SocialRecipe r = (SocialRecipe) recipeToEdit;
            title = r.getTitle();
            desc = r.getDescription();
            diff = r.getDifficulty();
            cookTime = (int) r.getCookTime();
            servingsVal = r.getServings();
            caloriesVal = r.getCalories();
            imageUrl = r.getImageUrl();
            ingredients = r.getIngredients();
            steps = r.getSteps();
            selectedIngredientIds.clear();
            if (r.getIngredientIds() != null) selectedIngredientIds.addAll(r.getIngredientIds());
            tagsList = r.getTags();
        }

        if (titleEditText != null) titleEditText.setText(title);
        if (descriptionEditText != null) descriptionEditText.setText(desc);
        if (cookTimeEditText != null) cookTimeEditText.setText(String.valueOf(cookTime));
        if (servingsEditText != null) servingsEditText.setText(String.valueOf(servingsVal));
        if (caloriesEditText != null) caloriesEditText.setText(String.valueOf(caloriesVal));
        
        if (imageUrl != null && recipeImageView != null) {
            Glide.with(this).load(imageUrl).centerCrop().into(recipeImageView);
        }

        if (ingredients != null) {
            selectedIngredientNames.clear();
            selectedIngredientNames.addAll(ingredients);
            updateIngredientsChips();
        }

        if (steps != null) {
            stepsList.clear();
            stepsList.addAll(getCleanTextList(steps));
            if (stepsAdapter != null) stepsAdapter.notifyDataSetChanged();
        }
        
        if (difficultySpinner != null && difficultySpinner.getAdapter() != null) {
            for (int i = 0; i < difficultySpinner.getCount(); i++) {
                if (difficultySpinner.getItemAtPosition(i).toString().equalsIgnoreCase(diff)) {
                    difficultySpinner.setSelection(i);
                    break;
                }
            }
        }
        
        if (tagsList != null && tagsChipGroup != null) {
            for (int i = 0; i < tagsChipGroup.getChildCount(); i++) {
                View child = tagsChipGroup.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (tagsList.contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    }
                }
            }
        }
    }

    private void setupRecyclerView() {
        if (stepsRecyclerView == null) return;
        stepsAdapter = new StepsAdapter(stepsList);
        stepsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        stepsRecyclerView.setAdapter(stepsAdapter);
        stepsRecyclerView.setNestedScrollingEnabled(false);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false;
                Collections.swap(stepsList, from, to);
                stepsAdapter.notifyItemMoved(from, to);
                stepsAdapter.notifyItemRangeChanged(Math.min(from, to), Math.abs(from - to) + 1);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        }).attachToRecyclerView(stepsRecyclerView);
    }

    private void setupSpinners() {
        if (difficultySpinner == null) return;
        ArrayAdapter<CharSequence> difficultyAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(difficultyAdapter);
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

    private void setupClickListeners(View view) {
        if (recipeImageView != null) {
            recipeImageView.setOnClickListener(v -> openImagePicker());
        }
        
        View videoContainer = view.findViewById(R.id.videoContainer);
        if (videoContainer != null) {
            videoContainer.setOnClickListener(v -> openVideoPicker());
        } else if (videoIcon != null) {
            videoIcon.setOnClickListener(v -> openVideoPicker());
        }

        if (btnIngredients != null) {
            btnIngredients.setOnClickListener(v -> openIngredientsPicker());
        }
        
        if (addStepButton != null) {
            addStepButton.setOnClickListener(v -> addStep());
        }

        if (deleteStepButton != null) {
            deleteStepButton.setOnClickListener(v -> deleteStep());
        }

        if (addIngredientButton != null) {
            addIngredientButton.setOnClickListener(v -> {
                if (ingredientsEditText != null) {
                    String name = ingredientsEditText.getText().toString().trim();
                    if (!name.isEmpty()) {
                        if (editingIngredientIndex >= 0 && editingIngredientIndex < selectedIngredientNames.size()) {
                            selectedIngredientNames.set(editingIngredientIndex, name);
                        } else {
                            selectedIngredientNames.add(name);
                        }
                        editingIngredientIndex = RecyclerView.NO_POSITION;
                        updateIngredientsChips();
                        ingredientsEditText.setText("");
                        addIngredientButton.setText("Add");
                    }
                }
            });
        }

        if (btnSaveRecipe != null) {
            btnSaveRecipe.setOnClickListener(v -> Toast.makeText(getContext(), "Draft saved!", Toast.LENGTH_SHORT).show());
        }

        if (publishButton != null) {
            publishButton.setOnClickListener(v -> publishRecipe());
        }

        View toolbarView = view.findViewById(R.id.toolbar);
        if (toolbarView instanceof Toolbar) {
            Toolbar toolbar = (Toolbar) toolbarView;
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationOnClickListener(v -> {
                if (isAdded()) getParentFragmentManager().popBackStack();
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
        getParentFragmentManager().setFragmentResultListener("ingredient_selection", getViewLifecycleOwner(), new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                ArrayList<String> names = result.getStringArrayList("ingredient_names");
                ArrayList<String> ids = result.getStringArrayList("ingredient_ids");
                
                if (names != null) {
                    selectedIngredientNames.clear();
                    selectedIngredientNames.addAll(names);
                }
                
                if (ids != null) {
                    selectedIngredientIds.clear();
                    selectedIngredientIds.addAll(ids);
                }
                
                updateIngredientsChips();
            }
        });
    }

    private void updateIngredientsChips() {
        if (ingredientsChipGroup == null) return;
        ingredientsChipGroup.removeAllViews();
        for (int i = 0; i < selectedIngredientNames.size(); i++) {
            String ingredient = selectedIngredientNames.get(i);
            int index = i;
            Chip chip = new Chip(requireContext());
            chip.setText(ingredient);
            chip.setCloseIconVisible(true);
            chip.setClickable(true);
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
    }

    private void updateVideoUrlDisplay() {
        if (textVideoUrl == null || !isAdded()) return;
        if (selectedVideoUri != null) {
            String path = selectedVideoUri.getPath();
            String fileName = (path != null && path.contains("/")) ? path.substring(path.lastIndexOf("/") + 1) : "Video selected";
            textVideoUrl.setText(fileName);
            textVideoUrl.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue));
        } else {
            textVideoUrl.setText("Add Video");
        }
    }

    private void addStep() {
        if (stepEditText == null || stepEditText.getText() == null) return;
        String text = stepEditText.getText().toString().trim();
        if (!text.isEmpty()) {
            stepsList.add(text);
            if (stepsAdapter != null) stepsAdapter.notifyItemInserted(stepsList.size() - 1);
            stepEditText.setText("");
            stepEditText.requestFocus();
        } else {
            stepEditText.setError("Please enter step description");
        }
    }

    private void deleteStep() {
        if (!stepsList.isEmpty()) {
            int lastIndex = stepsList.size() - 1;
            stepsList.remove(lastIndex);
            if (stepsAdapter != null) stepsAdapter.notifyItemRemoved(lastIndex);
        } else {
            Toast.makeText(getContext(), "No steps to delete", Toast.LENGTH_SHORT).show();
        }
    }

    private void publishRecipe() {
        if (!validateInputs()) return;
        if (isEditMode && !canEditCurrentRecipe()) {
            Toast.makeText(getContext(), "You can only edit recipes you created", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        if (selectedImageUri != null) {
            uploadImageAndPublish();
        } else {
            String existingUrl = null;
            if (isEditMode) {
                if (recipeToEdit instanceof Recipe) existingUrl = ((Recipe) recipeToEdit).getImageUrl();
                else if (recipeToEdit instanceof SocialRecipe) existingUrl = ((SocialRecipe) recipeToEdit).getImageUrl();
            }
            createAndPublishRecipe(existingUrl);
        }
    }

    private boolean validateInputs() {
        if (titleEditText == null || titleEditText.getText() == null || TextUtils.isEmpty(titleEditText.getText().toString().trim())) {
            if (titleEditText != null) titleEditText.setError("Title is required");
            return false;
        }
        if (selectedIngredientNames.isEmpty()) {
            Toast.makeText(getContext(), "Add at least one ingredient", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadImageAndPublish() {
        if (selectedImageUri == null) return;
        String fileName = "recipe_images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storageReference.child(fileName);
        ref.putFile(selectedImageUri)
            .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> createAndPublishRecipe(uri.toString())))
            .addOnFailureListener(e -> {
                showLoading(false);
                Toast.makeText(getContext(), "Image upload failed", Toast.LENGTH_SHORT).show();
            });
    }

    private void createAndPublishRecipe(String imageUrl) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        
        SocialRecipe recipe;
        if (isEditMode && recipeToEdit instanceof SocialRecipe) {
            recipe = (SocialRecipe) recipeToEdit;
        } else if (isEditMode && recipeToEdit instanceof Recipe) {
            recipe = new SocialRecipe(uid, name != null ? name : "User", 
                titleEditText.getText().toString().trim(), 
                descriptionEditText.getText().toString().trim());
            recipe.setId(((Recipe) recipeToEdit).getId());
        } else {
            recipe = new SocialRecipe(uid, name != null ? name : "User", 
                titleEditText.getText().toString().trim(), 
                descriptionEditText.getText().toString().trim());
        }
        
        if (titleEditText != null && titleEditText.getText() != null) recipe.setTitle(titleEditText.getText().toString().trim());
        if (descriptionEditText != null && descriptionEditText.getText() != null) recipe.setDescription(descriptionEditText.getText().toString().trim());
        recipe.setImageUrl(imageUrl);
        recipe.setIngredients(new ArrayList<>(selectedIngredientNames));
        recipe.setSteps(new ArrayList<>(stepsList));
        if (difficultySpinner != null && difficultySpinner.getSelectedItem() != null) {
            recipe.setDifficulty(difficultySpinner.getSelectedItem().toString());
        }
        
        try {
            if (cookTimeEditText != null && cookTimeEditText.getText() != null) recipe.setCookTime(Integer.parseInt(cookTimeEditText.getText().toString()));
            if (servingsEditText != null && servingsEditText.getText() != null) recipe.setServings(Integer.parseInt(servingsEditText.getText().toString()));
            if (caloriesEditText != null && caloriesEditText.getText() != null) recipe.setCalories(Integer.parseInt(caloriesEditText.getText().toString()));
        } catch (Exception ignored) {}

        List<String> tags = new ArrayList<>();
        if (tagsChipGroup != null) {
            for (int i = 0; i < tagsChipGroup.getChildCount(); i++) {
                View child = tagsChipGroup.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (chip.isChecked()) tags.add(chip.getText().toString());
                }
            }
        }
        recipe.setTags(tags);

        if (isEditMode) {
             updateExistingRecipe(recipe, imageUrl);
        } else {
            recipeRepository.createRecipe(recipe, new RecipeRepository.RecipeCallback<String>() {
                @Override public void onSuccess(String id) {
                    showLoading(false);
                    Toast.makeText(getContext(), "Recipe Published!", Toast.LENGTH_SHORT).show();
                    if (isAdded()) getParentFragmentManager().popBackStack();
                }
                @Override public void onError(String error) {
                    showLoading(false);
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateExistingRecipe(SocialRecipe recipe, String imageUrl) {
        String recipeId = recipe.getRecipeId();
        if (TextUtils.isEmpty(recipeId)) {
            showLoading(false);
            Toast.makeText(getContext(), "Recipe ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> cleanIngredients = getCleanTextList(selectedIngredientNames);
        List<String> cleanSteps = getCleanTextList(stepsList);
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", recipe.getTitle());
        updates.put("name", recipe.getTitle());
        updates.put("description", recipe.getDescription());
        updates.put("ingredients", cleanIngredients);
        updates.put("ingredientIds", new ArrayList<>(selectedIngredientIds));
        updates.put("steps", cleanSteps);
        updates.put("instructions", cleanSteps);
        updates.put("cookingInstructions", buildInstructionsText(cleanSteps));
        updates.put("difficulty", recipe.getDifficulty());
        updates.put("cookingTime", recipe.getCookingTime());
        updates.put("cookTime", recipe.getCookTime());
        updates.put("servings", recipe.getServings());
        updates.put("calories", recipe.getCalories());
        updates.put("tags", getSelectedTags());
        updates.put("updatedAt", FieldValue.serverTimestamp());
        if (imageUrl != null) {
            updates.put("imageUrl", imageUrl);
        }

        recipeRepository.updateRecipeFields(recipeId, updates, new RecipeRepository.RecipeCallback<Void>() {
            @Override public void onSuccess(Void result) {
                showLoading(false);
                Toast.makeText(getContext(), "Recipe Updated!", Toast.LENGTH_SHORT).show();
                Bundle resultBundle = new Bundle();
                resultBundle.putString("recipe_id", recipeId);
                getParentFragmentManager().setFragmentResult("recipe_updated", resultBundle);
                if (isAdded()) getParentFragmentManager().popBackStack();
            }

            @Override public void onError(String error) {
                showLoading(false);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (publishButton != null) publishButton.setEnabled(!show);
    }

    private boolean canEditCurrentRecipe() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        return RecipeOwnershipUtils.isOwner(recipeToEdit, currentUserId);
    }

    private List<String> getExistingSteps(Recipe recipe) {
        List<String> steps = recipe.getSteps();
        if (steps != null && !steps.isEmpty()) return steps;
        List<String> instructions = recipe.getInstructions();
        if (instructions != null && !instructions.isEmpty()) return instructions;
        return splitInstructions(recipe.getCookingInstructions());
    }

    private List<String> splitInstructions(String instructions) {
        List<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(instructions)) return result;
        String[] lines = instructions.split("\\r?\\n");
        for (String line : lines) {
            String clean = line.replaceFirst("^\\s*\\d+[.)]\\s*", "").trim();
            if (!TextUtils.isEmpty(clean)) result.add(clean);
        }
        return result;
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

    private String buildInstructionsText(List<String> steps) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            builder.append(i + 1).append(". ").append(steps.get(i));
            if (i < steps.size() - 1) builder.append('\n');
        }
        return builder.toString();
    }

    private List<String> getSelectedTags() {
        List<String> tags = new ArrayList<>();
        if (tagsChipGroup == null) return tags;
        for (int i = 0; i < tagsChipGroup.getChildCount(); i++) {
            View child = tagsChipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) tags.add(chip.getText().toString());
            }
        }
        return tags;
    }

    private static class StepsAdapter extends RecyclerView.Adapter<StepsAdapter.StepViewHolder> {
        private final List<String> steps;
        StepsAdapter(List<String> steps) { this.steps = steps; }
        @NonNull @Override public StepViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_editable_cooking_step, p, false);
            return new StepViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull StepViewHolder h, int p) { 
            h.bind(steps.get(p), steps, this);
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

            void bind(String step, List<String> steps, RecyclerView.Adapter<?> adapter) {
                if (watcher != null) editStep.removeTextChangedListener(watcher);

                int adapterPosition = getBindingAdapterPosition();
                number.setText(adapterPosition != RecyclerView.NO_POSITION ? String.valueOf(adapterPosition + 1) : "");
                editStep.setText(step);
                editStep.setSelection(editStep.length());

                watcher = new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable editable) {
                        int position = getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && position < steps.size()) {
                            steps.set(position, editable.toString());
                        }
                    }
                };
                editStep.addTextChangedListener(watcher);

                deleteButton.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && position < steps.size()) {
                        steps.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, steps.size() - position);
                    }
                });
            }
        }
    }
}
