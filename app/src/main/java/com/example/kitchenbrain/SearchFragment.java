package com.example.kitchenbrain;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText editTextSearch;
    private RecyclerView recyclerView;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> allRecipes;
    private List<Recipe> filteredRecipes;
    private FirebaseFirestore db;

    // Ingredient checkboxes
    private CheckBox checkboxFlour, checkboxEggs, checkboxSugar, checkboxOil;
    private CheckBox checkboxPotato, checkboxOnion, checkboxCarrot, checkboxPepper;
    private CheckBox checkboxApple, checkboxBanana, checkboxOrange, checkboxPear;
    private CheckBox checkboxMilk, checkboxSourCream, checkboxCottageCheese, checkboxCheese;
    
    // Layout containers to show/hide ingredient categories
    private LinearLayout mainIngredientsContainer, vegetableCategoryCheckBoxContainer;
    private LinearLayout fruitCategoryCheckBoxContainer, dairyCategoryCheckBoxContainer;
    
    // Selected ingredients
    private List<String> selectedIngredients;

    public SearchFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initViews(view);
        setupClickListeners(view);
        
        allRecipes = new ArrayList<>();
        filteredRecipes = new ArrayList<>();
        selectedIngredients = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        loadAllRecipes();

        return view;
    }

    private void initViews(View view) {
        editTextSearch = view.findViewById(R.id.editTextSearch);
        recyclerView = view.findViewById(R.id.recyclerViewRecipes);
        if (recyclerView != null && getContext() != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        // Initialize ingredient checkboxes
        checkboxFlour = view.findViewById(R.id.checkbox_flour);
        checkboxEggs = view.findViewById(R.id.checkbox_eggs);
        checkboxSugar = view.findViewById(R.id.checkbox_sugar);
        checkboxOil = view.findViewById(R.id.checkbox_oil);
        
        checkboxPotato = view.findViewById(R.id.checkbox_potato);
        checkboxOnion = view.findViewById(R.id.checkbox_onion);
        checkboxCarrot = view.findViewById(R.id.checkbox_carrot);
        checkboxPepper = view.findViewById(R.id.checkbox_pepper);
        
        checkboxApple = view.findViewById(R.id.checkbox_apple);
        checkboxBanana = view.findViewById(R.id.checkbox_banana);
        checkboxOrange = view.findViewById(R.id.checkbox_orange);
        checkboxPear = view.findViewById(R.id.checkbox_pear);
        
        checkboxMilk = view.findViewById(R.id.checkbox_milk);
        checkboxSourCream = view.findViewById(R.id.checkbox_sour_cream);
        checkboxCottageCheese = view.findViewById(R.id.checkbox_cottage_cheese);
        checkboxCheese = view.findViewById(R.id.checkbox_cheese);
        
        // Initialize category containers
        mainIngredientsContainer = view.findViewById(R.id.mainIngredientsContainer);
        vegetableCategoryCheckBoxContainer = view.findViewById(R.id.vegetableCategoryCheckBoxContainer);
        fruitCategoryCheckBoxContainer = view.findViewById(R.id.fruitCategoryCheckBoxContainer);
        dairyCategoryCheckBoxContainer = view.findViewById(R.id.dairyCategoryCheckBoxContainer);
    }

    private void setupClickListeners(View view) {
        // Set click listeners for category titles to expand/collapse ingredient lists
        View mainCategoryLayout = view.findViewById(R.id.mainCategoryLayout);
        View vegetableCategoryLayout = view.findViewById(R.id.vegetableCategoryLayout);
        View fruitCategoryLayout = view.findViewById(R.id.fruitCategoryLayout);
        View dairyCategoryLayout = view.findViewById(R.id.dairyCategoryLayout);
        
        if (mainCategoryLayout != null) {
            mainCategoryLayout.setOnClickListener(v -> toggleCategory(mainIngredientsContainer));
        }
        if (vegetableCategoryLayout != null) {
            vegetableCategoryLayout.setOnClickListener(v -> toggleCategory(vegetableCategoryCheckBoxContainer));
        }
        if (fruitCategoryLayout != null) {
            fruitCategoryLayout.setOnClickListener(v -> toggleCategory(fruitCategoryCheckBoxContainer));
        }
        if (dairyCategoryLayout != null) {
            dairyCategoryLayout.setOnClickListener(v -> toggleCategory(dairyCategoryCheckBoxContainer));
        }

        // Set listeners for ingredient checkboxes
        setupCheckboxListener(checkboxFlour, "flour");
        setupCheckboxListener(checkboxEggs, "eggs");
        setupCheckboxListener(checkboxSugar, "sugar");
        setupCheckboxListener(checkboxOil, "oil");
        
        setupCheckboxListener(checkboxPotato, "potato");
        setupCheckboxListener(checkboxOnion, "onion");
        setupCheckboxListener(checkboxCarrot, "carrot");
        setupCheckboxListener(checkboxPepper, "pepper");
        
        setupCheckboxListener(checkboxApple, "apple");
        setupCheckboxListener(checkboxBanana, "banana");
        setupCheckboxListener(checkboxOrange, "orange");
        setupCheckboxListener(checkboxPear, "pear");
        
        setupCheckboxListener(checkboxMilk, "milk");
        setupCheckboxListener(checkboxSourCream, "sour cream");
        setupCheckboxListener(checkboxCottageCheese, "cottage cheese");
        setupCheckboxListener(checkboxCheese, "cheese");

        // Set search text listener
        if (editTextSearch != null) {
            editTextSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterRecipes();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Set up search button click listener
        View searchButton = view.findViewById(R.id.searchRecipeButton);
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> filterRecipes());
        }
    }

    private void setupCheckboxListener(CheckBox checkBox, String ingredientName) {
        if (checkBox != null) {
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedIngredients.contains(ingredientName)) {
                        selectedIngredients.add(ingredientName);
                    }
                } else {
                    selectedIngredients.remove(ingredientName);
                }
                filterRecipes(); // Filter recipes whenever an ingredient is selected/deselected
            });
        }
    }

    private void toggleCategory(LinearLayout container) {
        if (container != null) {
            if (container.getVisibility() == View.GONE) {
                container.setVisibility(View.VISIBLE);
            } else {
                container.setVisibility(View.GONE);
            }
        }
    }

    private void loadAllRecipes() {
        if (db == null) return;
        db.collection("recipes").get().addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) return;
            if (task.isSuccessful()) {
                allRecipes.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Recipe recipe = doc.toObject(Recipe.class);
                    if (recipe != null) {
                        recipe.setId(doc.getId());
                        allRecipes.add(recipe);
                    }
                }
                filterRecipes();
            } else {
                Toast.makeText(getContext(), "Error loading recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterRecipes() {
        if (allRecipes == null) {
            filteredRecipes = new ArrayList<>();
        } else {
            filteredRecipes = new ArrayList<>();
            
            String searchQuery = editTextSearch != null ? editTextSearch.getText().toString().trim().toLowerCase() : "";
            
            for (Recipe recipe : allRecipes) {
                boolean matchesSearch = true;
                boolean matchesIngredients = true;
                
                // Check if recipe matches search query (name or description)
                if (!searchQuery.isEmpty()) {
                    matchesSearch = (recipe.getName() != null && recipe.getName().toLowerCase().contains(searchQuery)) ||
                                   (recipe.getDescription() != null && recipe.getDescription().toLowerCase().contains(searchQuery));
                }
                
                // Check if recipe contains all selected ingredients
                if (!selectedIngredients.isEmpty()) {
                    matchesIngredients = recipe.containsIngredients(selectedIngredients);
                }
                
                if (matchesSearch && matchesIngredients) {
                    filteredRecipes.add(recipe);
                }
            }
        }
        updateRecipeList();
    }

    private void updateRecipeList() {
        if (recyclerView == null || getContext() == null) return;
        if (recipeAdapter == null) {
            recipeAdapter = new RecipeAdapter(filteredRecipes != null ? filteredRecipes : new ArrayList<>(), new RecipeAdapter.OnRecipeClickListener() {
                @Override 
                public void onDeleteRecipe(Recipe recipe) {
                    // Empty implementation for SearchFragment - deletion not allowed from search
                }
                
                @Override 
                public void onHideRecipe(Recipe recipe) {
                    // Empty implementation for SearchFragment - hide functionality not applicable here
                }
                
                @Override 
                public void onRecipeClick(Recipe recipe) {
                    // Handle recipe click from search results - show recipe details
                    showRecipeDetails(recipe);
                }
            });
            recyclerView.setAdapter(recipeAdapter);
        } else {
            recipeAdapter.updateRecipes(filteredRecipes != null ? new ArrayList<>(filteredRecipes) : new ArrayList<>());
        }
    }
    
    private void showRecipeDetails(Recipe recipe) {
        // Navigate to recipe detail fragment
        if (getActivity() != null && isAdded()) {
            RecipeDetailFragment detailFragment = RecipeDetailFragment.newInstance(recipe);
            
            // Validate activity state
            android.app.Activity activity = getActivity();
            if (activity == null || activity.isFinishing() || isActivityDestroyed(activity)) {
                return;
            }
            
            // Validate activity type
            if (!(activity instanceof androidx.fragment.app.FragmentActivity)) {
                return;
            }
            
            androidx.fragment.app.FragmentActivity fragmentActivity = 
                (androidx.fragment.app.FragmentActivity) activity;
            
            fragmentActivity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
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
}