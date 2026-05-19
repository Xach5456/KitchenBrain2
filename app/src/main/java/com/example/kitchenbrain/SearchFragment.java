package com.example.kitchenbrain;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.adapter.ProductChipAdapter;
import com.example.kitchenbrain.adapter.RecipeAdapter;
import com.example.kitchenbrain.data.ProductDatabase;
import com.example.kitchenbrain.model.FoodProduct;
import com.example.kitchenbrain.viewmodel.SearchViewModel;
import com.example.kitchenbrain.model.Recipe;
import com.example.kitchenbrain.repository.RecipeRepository;
import com.example.kitchenbrain.ui.UserSearchBottomSheet;
import com.example.kitchenbrain.util.RecipeOwnershipUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment для поиска рецептов по ингредиентам.
 * Premium Redesign 2026 version.
 * Исправлено: список рецептов пуст до момента выбора ингредиента для лучшего UX.
 */
public class SearchFragment extends Fragment implements ProductChipAdapter.OnProductSelectionListener {

    private static final String TAG = "SearchFragment";
    
    private SearchViewModel viewModel;
    
    private RecyclerView recyclerViewSelectedIngredients;
    private RecyclerView recyclerViewIngredients;
    private TextView textSelectedCount;
    private EditText editTextSearchProducts;
    private ImageButton buttonClearSearch;
    private ImageButton buttonNotifications;
    private ImageButton buttonUserSearch;
    private RecyclerView recyclerViewRecipes;
    private TextView textRecipeCount;
    private View searchBarContainer;
    private View shimmerRecipes;
    private View layoutEmpty;
    
    private ProductChipAdapter selectedIngredientsAdapter;
    private ProductChipAdapter ingredientAdapter;
    private RecipeAdapter recipeAdapter;
    
    private ExecutorService backgroundExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isFragmentActive = true;
    
    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 300;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initExecutor();
        viewModel = new ViewModelProvider(requireActivity(), 
                new SavedStateViewModelFactory(requireActivity().getApplication(), requireActivity())).get(SearchViewModel.class);
    }

    private void initExecutor() {
        if (backgroundExecutor == null || backgroundExecutor.isShutdown()) {
            backgroundExecutor = Executors.newFixedThreadPool(4);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isFragmentActive = true;
        initExecutor();
        
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        initViews(view);
        setupAdapters();
        setupListeners();
        observeViewModel();
        
        // 🔥 НЕ загружаем рецепты автоматически при открытии, если ничего не выбрано
        List<String> currentSelected = viewModel.getSelectedIngredients().getValue();
        if (currentSelected != null && !currentSelected.isEmpty()) {
            viewModel.startRecipeListener();
            if (!viewModel.isDataLoaded()) {
                showLoading(true);
            }
        } else {
            // Гарантируем чистый список при старте
            recipeAdapter.updateRecipes(new ArrayList<>());
            textRecipeCount.setText(getString(R.string.recipes_count_format, 0));
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
        }
        
        // Загружаем ингредиенты (локальная БД для выбора)
        loadIngredients();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerViewSelectedIngredients = view.findViewById(R.id.recyclerViewSelectedIngredients);
        recyclerViewIngredients = view.findViewById(R.id.recyclerViewProducts);
        textSelectedCount = view.findViewById(R.id.textSelectedCount);
        editTextSearchProducts = view.findViewById(R.id.editTextSearchProducts);
        buttonClearSearch = view.findViewById(R.id.buttonClearSearch);
        buttonNotifications = view.findViewById(R.id.buttonNotifications);
        buttonUserSearch = view.findViewById(R.id.buttonUserSearch);
        recyclerViewRecipes = view.findViewById(R.id.recyclerViewRecipes);
        textRecipeCount = view.findViewById(R.id.textRecipeCount);
        searchBarContainer = view.findViewById(R.id.searchBarContainer);
        shimmerRecipes = view.findViewById(R.id.shimmerRecipes);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
    }
    
    private void setupAdapters() {
        selectedIngredientsAdapter = new ProductChipAdapter(getContext(), (product, isSelected) -> {
            if (!isSelected) viewModel.removeIngredient(product.getDisplayName());
        });
        recyclerViewSelectedIngredients.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewSelectedIngredients.setAdapter(selectedIngredientsAdapter);
        
        ingredientAdapter = new ProductChipAdapter(getContext(), this);
        recyclerViewIngredients.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerViewIngredients.setAdapter(ingredientAdapter);
        
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), getContext(), new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe, int position) {
                openRecipeDetail(recipe);
            }
            @Override
            public void onFavoriteClick(Recipe recipe, int position) {
                Toast.makeText(getContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onEditRecipe(Recipe recipe) {
                editRecipe(recipe);
            }
            @Override
            public void onDeleteRecipe(Recipe recipe) {
                confirmDeleteRecipe(recipe);
            }
        });
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRecipes.setAdapter(recipeAdapter);
    }
    
    private void setupListeners() {
        editTextSearchProducts.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (searchRunnable != null) searchDebounceHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> filterIngredients(query);
                searchDebounceHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY_MS);
                
                if (buttonClearSearch != null) {
                    buttonClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        editTextSearchProducts.setOnFocusChangeListener((v, hasFocus) -> {
            if (searchBarContainer != null) {
                searchBarContainer.setBackgroundResource(hasFocus ? 
                        R.drawable.bg_input_field_focused : R.drawable.bg_input_field);
            }
        });
        
        if (buttonClearSearch != null) {
            buttonClearSearch.setOnClickListener(v -> {
                editTextSearchProducts.setText("");
                if (viewModel.getSelectedIngredients().getValue() != null && !viewModel.getSelectedIngredients().getValue().isEmpty()) {
                    viewModel.clearIngredients();
                    Toast.makeText(getContext(), R.string.selection_cleared, Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (buttonUserSearch != null) {
            buttonUserSearch.setOnClickListener(v -> openUserSearchScreen());
        }

        if (buttonNotifications != null) {
            buttonNotifications.setOnClickListener(v -> openNotificationsScreen());
        }
    }
    
    private void observeViewModel() {
        viewModel.getSelectedIngredients().observe(getViewLifecycleOwner(), ingredients -> {
            if (selectedIngredientsAdapter != null) {
                selectedIngredientsAdapter.setSelectedNames(ingredients);
                selectedIngredientsAdapter.setOnlyShowSelected(true);
            }
            if (ingredientAdapter != null) {
                ingredientAdapter.setSelectedNames(ingredients);
            }
            textSelectedCount.setText(getString(R.string.ingredients_selected_format, ingredients.size()));
            updateMatchingRecipes();
            
            if (!ingredients.isEmpty()) {
                recyclerViewSelectedIngredients.smoothScrollToPosition(ingredients.size() - 1);
            }
        });
        
        viewModel.getAllRecipes().observe(getViewLifecycleOwner(), recipes -> {
            Log.d(TAG, "Observe all recipes update: " + (recipes != null ? recipes.size() : 0));
            showLoading(false);
            updateMatchingRecipes();
        });
    }

    @Override
    public void onProductToggle(FoodProduct product, boolean isSelected) {
        if (isSelected) {
            viewModel.addIngredient(product.getDisplayName());
            // 🔥 Запускаем загрузку рецептов только при выборе первого ингредиента
            viewModel.startRecipeListener();
            if (!viewModel.isDataLoaded()) {
                showLoading(true);
            }
        } else {
            viewModel.removeIngredient(product.getDisplayName());
        }
    }

    private void filterIngredients(String query) {
        if (query.isEmpty()) {
            loadIngredients();
            return;
        }

        initExecutor();
        backgroundExecutor.execute(() -> {
            List<FoodProduct> results = ProductDatabase.searchProducts(query);
            
            String lowerQuery = query.toLowerCase().trim();
            results.sort((a, b) -> {
                String nameA = a.getName().toLowerCase();
                String nameB = b.getName().toLowerCase();
                boolean startsA = nameA.startsWith(lowerQuery);
                boolean startsB = nameB.startsWith(lowerQuery);
                if (startsA != startsB) return startsA ? -1 : 1;
                if (startsA) return Integer.compare(nameA.length(), nameB.length());
                return nameA.compareTo(nameB);
            });

            List<FoodProduct> displayList = results.size() > 10 ? results.subList(0, 10) : results;
            
            mainHandler.post(() -> {
                if (isFragmentActive && ingredientAdapter != null) {
                    ingredientAdapter.setProducts(displayList);
                }
            });
        });
    }
    
    private void loadIngredients() {
        initExecutor();
        backgroundExecutor.execute(() -> {
            List<FoodProduct> products = ProductDatabase.getAllProducts();
            List<FoodProduct> top10 = products.size() > 10 ? products.subList(0, 10) : products;
            
            mainHandler.post(() -> {
                if (isFragmentActive && ingredientAdapter != null) {
                    ingredientAdapter.setProducts(top10);
                }
            });
        });
    }

    private void updateMatchingRecipes() {
        List<String> selected = viewModel.getSelectedIngredients().getValue();
        List<Recipe> all = viewModel.getAllRecipes().getValue();
        
        // 🔥 Если ничего не выбрано - очищаем список и выходим
        if (selected == null || selected.isEmpty()) {
            recipeAdapter.updateRecipes(new ArrayList<>());
            textRecipeCount.setText(getString(R.string.recipes_count_format, 0));
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }
        
        if (all == null || all.isEmpty()) {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        // Логика фильтрации
        List<String> cleanedSelected = new ArrayList<>();
        for (String s : selected) {
            String pureName = s.replaceAll("[\\p{So}\\p{Cn}]", "").trim().toLowerCase();
            if (!pureName.isEmpty()) cleanedSelected.add(pureName);
        }

        List<Recipe> matched = new ArrayList<>();
        final Map<String, Integer> matchScores = new HashMap<>();

        for (Recipe r : all) {
            List<String> recipeIngredients = r.getIngredients();
            if (recipeIngredients == null) continue;
            
            int score = 0;
            for (String sel : cleanedSelected) {
                for (String ri : recipeIngredients) {
                    if (ri.toLowerCase().contains(sel)) {
                        score++;
                        break;
                    }
                }
            }
            if (score > 0) {
                matchScores.put(r.getId(), score);
                matched.add(r);
            }
        }

        matched.sort((a, b) -> {
            int scoreA = matchScores.getOrDefault(a.getId(), 0);
            int scoreB = matchScores.getOrDefault(b.getId(), 0);
            return Integer.compare(scoreB, scoreA);
        });

        recipeAdapter.updateRecipes(matched);
        textRecipeCount.setText(getString(R.string.recipes_matching_format, matched.size()));
        
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(matched.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (shimmerRecipes != null) {
            shimmerRecipes.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewRecipes != null) {
            recyclerViewRecipes.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
        if (layoutEmpty != null && isLoading) {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void openRecipeDetail(Recipe recipe) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, RecipeDetailFragment.newInstance(recipe))
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void editRecipe(Recipe recipe) {
        if (!RecipeOwnershipUtils.isOwner(recipe, FirebaseAuth.getInstance().getUid())) {
            Toast.makeText(getContext(), "You can only edit recipes you created", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getActivity() != null) {
            ((MainActivity) getActivity()).showFragment(com.example.kitchenbrain.CreateRecipeFragment.newInstance(recipe), "edit_recipe");
        }
    }

    private void confirmDeleteRecipe(Recipe recipe) {
        if (!RecipeOwnershipUtils.isOwner(recipe, FirebaseAuth.getInstance().getUid())) {
            Toast.makeText(getContext(), "You can only delete recipes you created", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete this recipe?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteRecipeFromFirestore(recipe);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecipeFromFirestore(Recipe recipe) {
        if (recipe.getId() == null) return;
        new RecipeRepository().deleteRecipe(recipe.getId(), new RecipeRepository.RecipeCallback<>() {
            @Override
            public void onSuccess(Void result) {
                    Toast.makeText(getContext(), "Recipe deleted", Toast.LENGTH_SHORT).show();
                    // ViewModel will automatically update via listener
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error deleting recipe: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openUserSearchScreen() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToFragment(new UserSearchBottomSheet(), true);
        }
    }

    private void openNotificationsScreen() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToFragment(new NotificationsFragment(), true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        hideKeyboard();
    }

    private void hideKeyboard() {
        if (getView() != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
    }
}
