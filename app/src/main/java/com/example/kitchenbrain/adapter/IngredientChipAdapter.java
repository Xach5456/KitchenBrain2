package com.example.kitchenbrain.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.FoodProduct;

import java.util.List;
import java.util.Set;

/**
 * Adapter for displaying ingredient chips within a category
 */
public class IngredientChipAdapter extends RecyclerView.Adapter<IngredientChipAdapter.IngredientViewHolder> {

    private List<FoodProduct> ingredients;
    private Set<String> selectedIngredientIds;
    private CategorizedIngredientsAdapter.OnIngredientSelectedListener listener;
    
    public IngredientChipAdapter(List<FoodProduct> ingredients, 
                                 Set<String> selectedIngredientIds,
                                 CategorizedIngredientsAdapter.OnIngredientSelectedListener listener) {
        this.ingredients = ingredients != null ? ingredients : new java.util.ArrayList<>();
        this.selectedIngredientIds = selectedIngredientIds != null ? selectedIngredientIds : new java.util.HashSet<>();
        this.listener = listener;
    }
    
    /**
     * Update ingredients list without creating new adapter
     */
    public void updateProducts(List<FoodProduct> newProducts) {
        this.ingredients = newProducts != null ? newProducts : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient_chip, parent, false);
        return new IngredientViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        FoodProduct product = ingredients.get(position);
        holder.bind(product);
    }
    
    @Override
    public int getItemCount() {
        return ingredients != null ? ingredients.size() : 0;
    }
    
    class IngredientViewHolder extends RecyclerView.ViewHolder {
        private TextView textIngredientName;
        private TextView textIngredientIcon;
        private View selectionIndicator;
        
        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            textIngredientName = itemView.findViewById(R.id.textIngredientName);
            textIngredientIcon = itemView.findViewById(R.id.textIngredientIcon);
            selectionIndicator = itemView.findViewById(R.id.selectionIndicator);
        }
        
        public void bind(FoodProduct product) {
            // Display name and icon
            textIngredientName.setText(product.getDisplayName());
            textIngredientIcon.setText(product.getEmoji());
            
            // Show selection state
            boolean isSelected = selectedIngredientIds.contains(product.getId());
            if (isSelected) {
                selectionIndicator.setVisibility(View.VISIBLE);
                itemView.setBackgroundResource(R.drawable.bg_ingredient_selected);
            } else {
                selectionIndicator.setVisibility(View.GONE);
                itemView.setBackgroundResource(R.drawable.bg_ingredient_unselected);
            }
            
            // Handle click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    if (isSelected) {
                        listener.onIngredientDeselected(product);
                    } else {
                        listener.onIngredientSelected(product);
                    }
                }
            });
        }
    }
}
