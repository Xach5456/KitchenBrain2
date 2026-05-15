package com.example.kitchenbrain;

import android.content.Context;
import android.os.Bundle;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.adapter.ProductChipAdapter;
import com.example.kitchenbrain.data.ProductDatabase;
import com.example.kitchenbrain.model.FoodProduct;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Modern ingredient picker with category-based selection and chip UI
 */
public class ModernIngredientsPickerFragment extends Fragment implements ProductChipAdapter.OnProductSelectionListener {

    private static final String TAG = "ModernIngredientsPicker";
    private static final String ARG_SELECTED_INGREDIENTS = "selected_ingredients";
    
    // UI Components
    private EditText editTextSearchIngredients;
    private ImageButton buttonClearSearch;
    private TextView textSelectedCount;
    private RecyclerView recyclerViewIngredients;
    private TabLayout tabLayoutCategories;
    private MaterialButton buttonCancel;
    private MaterialButton buttonConfirm;
    
    // Adapter
    private ProductChipAdapter productChipAdapter;
    
    // Data
    private List<FoodProduct> allProducts;
    private List<FoodProduct> selectedProducts;
    private Set<String> selectedProductIds;
    
    // Listener for ingredient selection
    private OnIngredientsSelectedListener listener;
    
    public interface OnIngredientsSelectedListener {
        void onIngredientsSelected(List<String> ingredientNames);
    }
    
    // Initial selected ingredients (passed from CreateRecipeFragment)
    private Set<String> initialSelectedIngredients;

    public ModernIngredientsPickerFragment() {
        // Required empty public constructor
    }

    public static ModernIngredientsPickerFragment newInstance(Set<String> selectedIngredients) {
        ModernIngredientsPickerFragment fragment = new ModernIngredientsPickerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SELECTED_INGREDIENTS, new HashSet<>(selectedIngredients));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize collections BEFORE any operations
        selectedProducts = new ArrayList<>();
        selectedProductIds = new HashSet<>();
        
        if (getArguments() != null) {
            initialSelectedIngredients = (Set<String>) getArguments().getSerializable(ARG_SELECTED_INGREDIENTS);
        }
        if (initialSelectedIngredients == null) {
            initialSelectedIngredients = new HashSet<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modern_ingredients_picker, container, false);
        
        initViews(view);
        setupAdapters();
        setupListeners();
        loadProducts();
        restoreInitialState();
        
        return view;
    }

    private void initViews(View view) {
        editTextSearchIngredients = view.findViewById(R.id.editTextSearchIngredients);
        buttonClearSearch = view.findViewById(R.id.buttonClearSearch);
        textSelectedCount = view.findViewById(R.id.textSelectedCount);
        recyclerViewIngredients = view.findViewById(R.id.recyclerViewIngredients);
        tabLayoutCategories = view.findViewById(R.id.tabLayoutCategories);
        buttonCancel = view.findViewById(R.id.buttonCancel);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);
        
        if (recyclerViewIngredients != null && getContext() != null) {
            // Grid layout for product chips (3 columns)
            recyclerViewIngredients.setLayoutManager(new GridLayoutManager(getContext(), 3));
        }
    }

    private void setupAdapters() {
        productChipAdapter = new ProductChipAdapter(getContext(), this);
        if (recyclerViewIngredients != null) {
            recyclerViewIngredients.setAdapter(productChipAdapter);
        }
    }

    private void setupListeners() {
        // Search functionality
        editTextSearchIngredients.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (productChipAdapter != null) {
                    productChipAdapter.getFilter().filter(s.toString());
                }
                
                // Show/hide clear button
                if (buttonClearSearch != null) {
                    buttonClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Clear search button
        if (buttonClearSearch != null) {
            buttonClearSearch.setOnClickListener(v -> {
                editTextSearchIngredients.setText("");
                editTextSearchIngredients.requestFocus();
            });
        }
        
        // Category tabs
        if (tabLayoutCategories != null) {
            setupCategoryTabs();
        }
        
        // Cancel button
        if (buttonCancel != null) {
            buttonCancel.setOnClickListener(v -> {
                if (getActivity() != null && isAdded()) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
        
        // Confirm button
        if (buttonConfirm != null) {
            buttonConfirm.setOnClickListener(v -> confirmSelection());
        }
    }

    private void setupCategoryTabs() {
        // Add category tabs
        String[] categories = {
            ProductDatabase.CATEGORY_VEGETABLES,
            ProductDatabase.CATEGORY_FRUITS,
            ProductDatabase.CATEGORY_MEAT,
            ProductDatabase.CATEGORY_SEAFOOD,
            ProductDatabase.CATEGORY_DAIRY,
            ProductDatabase.CATEGORY_GRAINS,
            ProductDatabase.CATEGORY_SPICES,
            ProductDatabase.CATEGORY_OTHER
        };
        
        for (String category : categories) {
            TabLayout.Tab tab = tabLayoutCategories.newTab();
            tab.setText(getCategoryDisplayName(category));
            tab.setTag(category);
            tabLayoutCategories.addTab(tab);
        }
        
        // Tab selection listener
        tabLayoutCategories.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterByCategory((String) tab.getTag());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        // Select first tab by default
        if (tabLayoutCategories.getTabCount() > 0) {
            tabLayoutCategories.getTabAt(0).select();
        }
    }

    private String getCategoryDisplayName(String category) {
        switch (category) {
            case ProductDatabase.CATEGORY_VEGETABLES: return "🥦 Vegetables";
            case ProductDatabase.CATEGORY_FRUITS: return "🍎 Fruits";
            case ProductDatabase.CATEGORY_MEAT: return "🍗 Meat";
            case ProductDatabase.CATEGORY_SEAFOOD: return "🐟 Seafood";
            case ProductDatabase.CATEGORY_DAIRY: return "🧀 Dairy";
            case ProductDatabase.CATEGORY_GRAINS: return "🍚 Grains";
            case ProductDatabase.CATEGORY_SPICES: return "🌿 Spices";
            case ProductDatabase.CATEGORY_OTHER: return "🥚 Other";
            default: return category;
        }
    }

    private void filterByCategory(String category) {
        List<FoodProduct> categoryProducts = ProductDatabase.getProductsByCategory(category);
        productChipAdapter.setProducts(categoryProducts);
        updateAdapterSelection();
    }

    private void loadProducts() {
        allProducts = ProductDatabase.getAllProducts();
        if (productChipAdapter != null) {
            productChipAdapter.setProducts(allProducts);
        }
    }

    private void restoreInitialState() {
        // Pre-select ingredients that were already chosen
        if (allProducts != null && !allProducts.isEmpty()) {
            for (FoodProduct product : allProducts) {
                if (initialSelectedIngredients != null) {
                    if (initialSelectedIngredients.contains(product.getName()) ||
                        initialSelectedIngredients.contains(product.getId())) {
                        selectedProductIds.add(product.getId());
                        selectedProducts.add(product);
                    }
                }
            }
        }
        updateAdapterSelection();
        updateSelectedCount();
    }

    private void updateAdapterSelection() {
        if (productChipAdapter != null && selectedProducts != null) {
            List<String> selectedNames = new ArrayList<>();
            for (FoodProduct p : selectedProducts) {
                selectedNames.add(p.getDisplayName());
            }
            productChipAdapter.setSelectedNames(selectedNames);
        }
    }

    private void confirmSelection() {
        // Return BOTH ingredient names and IDs
        List<String> ingredientNames = new ArrayList<>();
        List<String> ingredientIds = new ArrayList<>();
        
        for (FoodProduct product : selectedProducts) {
            ingredientNames.add(product.getName());
            ingredientIds.add(product.getId());
        }
        
        Log.d(TAG, "Confirming selection: " + ingredientNames.size() + " names");
        
        // Use Fragment Result API to send names and IDs back
        Bundle result = new Bundle();
        result.putStringArrayList("ingredient_names", new ArrayList<>(ingredientNames));
        result.putStringArrayList("ingredient_ids", new ArrayList<>(ingredientIds));
        
        getParentFragmentManager().setFragmentResult("ingredient_selection", result);
        
        // Navigate back
        if (getActivity() != null && isAdded()) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void updateSelectedCount() {
        if (textSelectedCount != null && selectedProducts != null) {
            int count = selectedProducts.size();
            if (count == 0) {
                textSelectedCount.setText("0 ingredients selected");
            } else {
                textSelectedCount.setText(count + " ingredient" + (count == 1 ? "" : "s") + " selected");
            }
        }
    }

    // ===== ProductChipAdapter.OnProductSelectionListener =====

    @Override
    public void onProductToggle(FoodProduct product, boolean isSelected) {
        if (isSelected) {
            if (selectedProductIds != null && !selectedProductIds.contains(product.getId())) {
                selectedProductIds.add(product.getId());
                if (selectedProducts != null) {
                    selectedProducts.add(product);
                }
                
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Added: " + product.getDisplayName(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            if (selectedProductIds != null && selectedProductIds.contains(product.getId())) {
                selectedProductIds.remove(product.getId());
                if (selectedProducts != null) {
                    selectedProducts.remove(product);
                }
            }
        }
        updateSelectedCount();
        updateAdapterSelection();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnIngredientsSelectedListener) {
            listener = (OnIngredientsSelectedListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
