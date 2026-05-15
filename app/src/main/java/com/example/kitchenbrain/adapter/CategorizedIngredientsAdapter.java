package com.example.kitchenbrain.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.R;
import com.example.kitchenbrain.data.ProductDatabase;
import com.example.kitchenbrain.model.FoodProduct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Professional categorized ingredients adapter with expandable sections
 * Similar to Instagram-style organized lists
 */
public class CategorizedIngredientsAdapter extends RecyclerView.Adapter<CategorizedIngredientsAdapter.CategoryViewHolder> {

    private List<CategoryItem> categoryItems;
    private OnIngredientSelectedListener listener;
    private Set<String> selectedIngredientIds;
    private Map<String, Boolean> expandedCategories;
    
    public interface OnIngredientSelectedListener {
        void onIngredientSelected(FoodProduct product);
        void onIngredientDeselected(FoodProduct product);
    }
    
    public CategorizedIngredientsAdapter(OnIngredientSelectedListener listener) {
        this.listener = listener;
        this.selectedIngredientIds = new HashSet<>();
        this.expandedCategories = new HashMap<>();
        loadCategories();
    }
    
    private void loadCategories() {
        categoryItems = new ArrayList<>();
        
        // Add all categories from ProductDatabase
        addCategory(ProductDatabase.CATEGORY_VEGETABLES, "🥬", ProductDatabase.getVegetables());
        addCategory(ProductDatabase.CATEGORY_FRUITS, "🍎", ProductDatabase.getFruits());
        addCategory(ProductDatabase.CATEGORY_MEAT, "🍗", ProductDatabase.getMeatAndPoultry());
        addCategory(ProductDatabase.CATEGORY_SEAFOOD, "🐟", ProductDatabase.getFishAndSeafood());
        addCategory(ProductDatabase.CATEGORY_DAIRY, "🧀", ProductDatabase.getDairyProducts());
        addCategory(ProductDatabase.CATEGORY_GRAINS, "🍞", ProductDatabase.getGrainsAndPasta());
        addCategory(ProductDatabase.CATEGORY_SPICES, "🌿", ProductDatabase.getSpicesAndHerbs());
        addCategory(ProductDatabase.CATEGORY_OTHER, "🥚", ProductDatabase.getOtherIngredients());
    }
    
    private void addCategory(String name, String icon, List<FoodProduct> products) {
        CategoryItem categoryItem = new CategoryItem(name, icon, products);
        categoryItems.add(categoryItem);
        // All categories expanded by default
        expandedCategories.put(name, true);
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem categoryItem = categoryItems.get(position);
        holder.bind(categoryItem);
    }
    
    @Override
    public int getItemCount() {
        return categoryItems != null ? categoryItems.size() : 0;
    }
    
    /**
     * Toggle category expansion
     */
    public void toggleCategory(int position) {
        if (position < 0 || position >= categoryItems.size()) return;
        
        CategoryItem item = categoryItems.get(position);
        boolean isExpanded = expandedCategories.getOrDefault(item.categoryName, true);
        expandedCategories.put(item.categoryName, !isExpanded);
        notifyItemChanged(position);
    }
    
    /**
     * Set selected ingredients from external source
     */
    public void setSelectedIngredients(Set<String> ingredientIds) {
        if (ingredientIds == null) {
            selectedIngredientIds.clear();
        } else {
            selectedIngredientIds = new HashSet<>(ingredientIds);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Get count of selected ingredients
     */
    public int getSelectedCount() {
        return selectedIngredientIds != null ? selectedIngredientIds.size() : 0;
    }
    
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView textCategoryTitle;
        private TextView textCategoryIcon;
        private RecyclerView recyclerViewIngredients;
        private TextView textExpandIndicator;
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategoryTitle = itemView.findViewById(R.id.textCategoryTitle);
            textCategoryIcon = itemView.findViewById(R.id.textCategoryIcon);
            recyclerViewIngredients = itemView.findViewById(R.id.recyclerViewIngredients);
            textExpandIndicator = itemView.findViewById(R.id.textExpandIndicator);
        }
        
        public void bind(CategoryItem categoryItem) {
            // Set category info
            textCategoryTitle.setText(categoryItem.categoryName);
            textCategoryIcon.setText(categoryItem.icon);
            
            // Setup ingredients RecyclerView
            boolean isExpanded = expandedCategories.getOrDefault(categoryItem.categoryName, true);
            textExpandIndicator.setText(isExpanded ? "▼" : "▶");
            recyclerViewIngredients.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            
            if (isExpanded && categoryItem.products != null) {
                // CRITICAL FIX: Reuse existing adapter instead of creating new one
                IngredientChipAdapter adapter = (IngredientChipAdapter) recyclerViewIngredients.getAdapter();
                if (adapter == null) {
                    adapter = new IngredientChipAdapter(
                        categoryItem.products, 
                        selectedIngredientIds,
                        listener
                    );
                    recyclerViewIngredients.setLayoutManager(
                        new LinearLayoutManager(itemView.getContext())
                    );
                    recyclerViewIngredients.setAdapter(adapter);
                    recyclerViewIngredients.setHasFixedSize(true); // Performance optimization
                } else {
                    // Update existing adapter data
                    adapter.updateProducts(categoryItem.products);
                }
            }
            
            // Click to expand/collapse
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    toggleCategory(pos);
                }
            });
        }
    }
    
    /**
     * Simple data class for category with products
     */
    static class CategoryItem {
        String categoryName;
        String icon;
        List<FoodProduct> products;
        
        CategoryItem(String categoryName, String icon, List<FoodProduct> products) {
            this.categoryName = categoryName;
            this.icon = icon;
            this.products = products;
        }
    }
}
