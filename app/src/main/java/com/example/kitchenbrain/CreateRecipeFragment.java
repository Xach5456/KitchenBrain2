package com.example.kitchenbrain;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment for creating new recipes.
 * Handles image/video selection and ingredient picking using modern Android APIs.
 */
public class CreateRecipeFragment extends Fragment {

    private static final String TAG = "CreateRecipeFragment";

    // UI components
    private TextInputEditText recipeName, recipeDescription, cookingTime, cookingInstructions;
    private ImageView recipeImage;
    private ImageView recipeVideo;
    private TextView textVideoUrl;
    private MaterialButton btnSaveRecipe, btnIngredients;
    private TextView textSelectedIngredients;

    // Media URIs
    private Uri selectedImageUri;
    private Uri selectedVideoUri;
    
    // Selected ingredients
    private final List<String> selectedIngredientNames = new ArrayList<>();
    private final Set<String> selectedIngredientIds = new HashSet<>();

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Modern Activity Result Launchers
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (recipeImage != null && selectedImageUri != null) {
                        recipeImage.setImageURI(selectedImageUri);
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

    public CreateRecipeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                           @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_recipe, container, false);

        initializeViews(view);
        setupClickListeners();
        setupIngredientResultListener();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateSelectedIngredientsDisplay();
    }

    private void initializeViews(@NonNull View view) {
        recipeName = view.findViewById(R.id.recipeName);
        recipeDescription = view.findViewById(R.id.recipeDescription);
        cookingTime = view.findViewById(R.id.cookingTime);
        cookingInstructions = view.findViewById(R.id.cookingInstructions);
        recipeImage = view.findViewById(R.id.recipeImage);
        recipeVideo = view.findViewById(R.id.recipeVideo);
        textVideoUrl = view.findViewById(R.id.textVideoUrl);
        btnSaveRecipe = view.findViewById(R.id.btnSaveRecipe);
        btnIngredients = view.findViewById(R.id.btnIngredients);
        textSelectedIngredients = view.findViewById(R.id.textSelectedIngredients);
    }

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
            // Using the specific ModernIngredientsPickerFragment
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
                selectedIngredientNames.clear();
                selectedIngredientNames.addAll(names);
            }
            
            if (ids != null) {
                selectedIngredientIds.clear();
                selectedIngredientIds.addAll(ids);
            }
            
            updateSelectedIngredientsDisplay();
        });
    }

    private void updateVideoUrlDisplay() {
        if (textVideoUrl == null || !isAdded()) return;
        
        if (selectedVideoUri != null) {
            textVideoUrl.setText(getFileNameFromUri(selectedVideoUri));
            textVideoUrl.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue));
        } else {
            textVideoUrl.setText("No video selected");
            textVideoUrl.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }
    
    private String getFileNameFromUri(Uri uri) {
        if (uri == null) return "Unknown";
        String path = uri.getPath();
        if (path == null) return "Unknown";
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
    }
    
    private void updateSelectedIngredientsDisplay() {
        if (textSelectedIngredients == null || !isAdded()) return;
        
        if (selectedIngredientNames.isEmpty()) {
            textSelectedIngredients.setText("No ingredients selected");
            textSelectedIngredients.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            textSelectedIngredients.setText(String.join(", ", selectedIngredientNames));
            textSelectedIngredients.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }
    }

    private void saveRecipe() {
        String name = recipeName != null && recipeName.getText() != null ? recipeName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter recipe name", Toast.LENGTH_SHORT).show();
            return;
        }
        // Save logic here
        Toast.makeText(getContext(), "Recipe saved successfully!", Toast.LENGTH_SHORT).show();
    }
}
