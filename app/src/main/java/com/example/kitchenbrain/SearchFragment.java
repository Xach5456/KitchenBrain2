package com.example.kitchenbrain;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.kitchenbrain.ui.UserSearchBottomSheet;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment для поиска рецептов по ингредиентам.
 * Состояние ингредиентов сохраняется во ViewModel (Activity Scope).
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
    private RecyclerView recyclerViewRecipes;
    private TextView textRecipeCount;
    
    private ProductChipAdapter selectedIngredientsAdapter;
    private ProductChipAdapter ingredientAdapter;
    private RecipeAdapter recipeAdapter;
    
    private FirebaseFirestore db;
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
        
        // Получаем ViewModel, привязанную к Activity, чтобы состояние сохранялось при навигации
        viewModel = new ViewModelProvider(requireActivity(), 
                new SavedStateViewModelFactory(requireActivity().getApplication(), requireActivity())).get(SearchViewModel.class);
    }

    private void initExecutor() {
        if (backgroundExecutor == null || backgroundExecutor.isShutdown()) {
            backgroundExecutor = Executors.newFixedThreadPool(4);
            Log.d(TAG, "🚀 Background executor initialized");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isFragmentActive = true;
        initExecutor(); // Ensure executor is ready even if fragment instance was reused
        
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        initViews(view);
        setupAdapters();
        setupListeners();
        initFirebase();
        observeViewModel();
        
        // Загружаем данные только если они еще не в памяти
        if (!viewModel.isDataLoaded()) {
            loadInitialData();
        } else {
            // Если данные есть, просто загружаем список всех продуктов для выбора
            loadIngredients();
        }
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerViewSelectedIngredients = view.findViewById(R.id.recyclerViewSelectedIngredients);
        recyclerViewIngredients = view.findViewById(R.id.recyclerViewProducts);
        textSelectedCount = view.findViewById(R.id.textSelectedCount);
        editTextSearchProducts = view.findViewById(R.id.editTextSearchProducts);
        buttonClearSearch = view.findViewById(R.id.buttonClearSearch);
        buttonNotifications = view.findViewById(R.id.buttonNotifications);
        recyclerViewRecipes = view.findViewById(R.id.recyclerViewRecipes);
        textRecipeCount = view.findViewById(R.id.textRecipeCount);
        
        // Скрываем лишние переключатели, если они есть в XML
        View chipGroup = view.findViewById(R.id.chipGroupSearchType);
        if (chipGroup != null) chipGroup.setVisibility(View.GONE);
        
        View searchHeader = view.findViewById(R.id.buttonUserSearch);
        if (searchHeader != null) {
            searchHeader.setOnClickListener(v -> openUserSearchScreen());
        }

        if (buttonNotifications != null) {
            buttonNotifications.setOnClickListener(v -> openNotificationsScreen());
        }
    }
    
    private void setupAdapters() {
        // Selected ingredients - Horizontal RecyclerView
        selectedIngredientsAdapter = new ProductChipAdapter(getContext(), (product, isSelected) -> {
            if (!isSelected) {
                viewModel.removeIngredient(product.getDisplayName());
            }
        });
        recyclerViewSelectedIngredients.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewSelectedIngredients.setAdapter(selectedIngredientsAdapter);
        
        // All ingredients - Grid RecyclerView
        ingredientAdapter = new ProductChipAdapter(getContext(), this);
        recyclerViewIngredients.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerViewIngredients.setAdapter(ingredientAdapter);
        
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), getContext(), new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(com.example.kitchenbrain.model.Recipe recipe, int position) {
                // TODO: Open recipe detail
            }
            
            @Override
            public void onFavoriteClick(com.example.kitchenbrain.model.Recipe recipe, int position) {
                // Handle favorite
            }
        });
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRecipes.setAdapter(recipeAdapter);
    }
    
    private void setupListeners() {
        editTextSearchProducts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
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
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        if (buttonClearSearch != null) {
            buttonClearSearch.setOnClickListener(v -> {
                viewModel.clearIngredients();
                editTextSearchProducts.setText("");
                Toast.makeText(getContext(), R.string.selection_cleared, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
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
        });
        
        viewModel.getAllRecipes().observe(getViewLifecycleOwner(), recipes -> updateMatchingRecipes());
    }

    @Override
    public void onProductToggle(FoodProduct product, boolean isSelected) {
        if (isSelected) {
            viewModel.addIngredient(product.getDisplayName());
        } else {
            viewModel.removeIngredient(product.getDisplayName());
        }
    }

    /**
     * Выполняет поиск по всей базе, но отображает только ТОП-10 результатов
     * для чистоты интерфейса и лучшего UX.
     * Реализована сортировка по релевантности (Instagram-style).
     */
    private void filterIngredients(String query) {
        if (query.isEmpty()) {
            loadIngredients();
            return;
        }

        initExecutor();
        backgroundExecutor.execute(() -> {
            // 1. Ищем по ВСЕЙ базе данных (Global Scope)
            List<FoodProduct> results = ProductDatabase.searchProducts(query);
            
            // 2. Сортировка по релевантности
            String lowerQuery = query.toLowerCase().trim();
            results.sort((a, b) -> {
                String nameA = a.getName().toLowerCase();
                String nameB = b.getName().toLowerCase();
                
                boolean startsA = nameA.startsWith(lowerQuery);
                boolean startsB = nameB.startsWith(lowerQuery);
                
                if (startsA != startsB) {
                    return startsA ? -1 : 1;
                }
                
                if (startsA) {
                    // Оба начинаются с запроса, тогда более короткое название выше
                    return Integer.compare(nameA.length(), nameB.length());
                }
                
                return nameA.compareTo(nameB);
            });

            // 3. Ограничиваем выдачу в UI до 10 наиболее релевантных элементов
            List<FoodProduct> displayList = results.size() > 10 ? results.subList(0, 10) : results;
            
            mainHandler.post(() -> {
                if (isFragmentActive && ingredientAdapter != null) {
                    ingredientAdapter.setProducts(displayList);
                    // Прокручиваем в начало, чтобы пользователь сразу видел топ совпадений
                    recyclerViewIngredients.scrollToPosition(0);
                }
            });
        });
    }

    private void loadInitialData() {
        loadIngredients();
        loadRecipesFromFirestore();
    }
    
    /**
     * Загружает начальный список ингредиентов (ограничено 10 для чистоты экрана).
     */
    private void loadIngredients() {
        initExecutor();
        backgroundExecutor.execute(() -> {
            List<FoodProduct> products = ProductDatabase.getAllProducts();
            // Показываем только ТОП-10 ингредиентов по умолчанию
            List<FoodProduct> top10 = products.size() > 10 ? products.subList(0, 10) : products;
            
            mainHandler.post(() -> {
                if (isFragmentActive && ingredientAdapter != null) {
                    ingredientAdapter.setProducts(top10);
                }
            });
        });
    }
    
    private void loadRecipesFromFirestore() {
        db.collection("recipes").limit(100).get().addOnSuccessListener(documents -> {
            List<Recipe> recipes = new ArrayList<>();
            for (var doc : documents) {
                Recipe r = doc.toObject(Recipe.class);
                if (r != null) {
                    r.setId(doc.getId());
                    recipes.add(r);
                }
            }
            viewModel.setAllRecipes(recipes);
        }).addOnFailureListener(e -> Log.e(TAG, "❌ Failed to load recipes from Firestore", e));
    }

    /**
     * Улучшенная логика сопоставления рецептов.
     * 1. Очистка выбранных ингредиентов от эмодзи.
     * 2. Подсчет количества совпадений для каждого рецепта.
     * 3. Сортировка по релевантности (чем больше совпадений, тем выше).
     */
    private void updateMatchingRecipes() {
        List<String> selected = viewModel.getSelectedIngredients().getValue();
        List<Recipe> all = viewModel.getAllRecipes().getValue();
        
        if (all == null) return;
        
        if (selected == null || selected.isEmpty()) {
            recipeAdapter.updateData(all);
            textRecipeCount.setText(getString(R.string.recipes_count_format, all.size()));
            return;
        }

        // 1. Очищаем выбранные ингредиенты от эмодзи для точного сравнения
        List<String> cleanedSelected = new ArrayList<>();
        for (String s : selected) {
            String pureName = s.replaceAll("[\\p{So}\\p{Cn}]", "").trim().toLowerCase();
            if (!pureName.isEmpty()) cleanedSelected.add(pureName);
        }

        List<Recipe> matched = new ArrayList<>();
        final Map<String, Integer> matchScores = new HashMap<>();

        for (Recipe r : all) {
            if (r.getIngredients() == null) continue;
            
            int score = 0;
            List<String> recipeIngredients = r.getIngredients();
            
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

        // 2. Сортировка по убыванию количества совпадений
        matched.sort((a, b) -> {
            int scoreA = matchScores.getOrDefault(a.getId(), 0);
            int scoreB = matchScores.getOrDefault(b.getId(), 0);
            return Integer.compare(scoreB, scoreA);
        });

        recipeAdapter.updateData(matched);
        textRecipeCount.setText(getString(R.string.recipes_matching_format, matched.size()));
    }

    private void openUserSearchScreen() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToFragment(
                    new UserSearchBottomSheet(), true);
        }
    }

    private void openNotificationsScreen() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToFragment(
                    new NotificationsFragment(), true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
            Log.d(TAG, "🛑 Background executor shut down");
        }
    }
}
