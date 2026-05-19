package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Filter;
import android.widget.Filterable;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.kitchenbrain.model.FoodProduct;
import com.example.kitchenbrain.R;

public class ProductChipAdapter extends RecyclerView.Adapter<ProductChipAdapter.ProductViewHolder> implements Filterable {
    
    private Context context;
    private List<FoodProduct> allProducts = new ArrayList<>();
    private List<FoodProduct> filteredProducts = new ArrayList<>();
    private Set<String> selectedNames = new HashSet<>(); 
    private OnProductSelectionListener listener;
    private boolean onlyShowSelected = false;
    private int lastPosition = -1;
    
    public interface OnProductSelectionListener {
        void onProductToggle(FoodProduct product, boolean isSelected);
    }
    
    public ProductChipAdapter(Context context, OnProductSelectionListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    public void setProducts(List<FoodProduct> products) {
        this.allProducts = new ArrayList<>(products);
        this.filteredProducts = new ArrayList<>(products);
        notifyDataSetChanged();
    }

    public void setSelectedNames(List<String> names) {
        this.selectedNames = new HashSet<>(names);
        if (onlyShowSelected) {
            filterSelectedOnly();
        }
        notifyDataSetChanged();
    }
    
    public void setOnlyShowSelected(boolean onlyShowSelected) {
        this.onlyShowSelected = onlyShowSelected;
        if (onlyShowSelected) {
            filterSelectedOnly();
        } else {
            filteredProducts = new ArrayList<>(allProducts);
        }
        notifyDataSetChanged();
    }
    
    private void filterSelectedOnly() {
        filteredProducts.clear();
        for (FoodProduct product : allProducts) {
            if (selectedNames.contains(product.getDisplayName())) {
                filteredProducts.add(product);
            }
        }
    }
    
    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_chip, parent, false);
        return new ProductViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        FoodProduct product = filteredProducts.get(position);
        boolean isSelected = selectedNames.contains(product.getDisplayName());
        holder.bind(product, isSelected);
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            animation.setDuration(400);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }
    
    @Override
    public int getItemCount() {
        return filteredProducts.size();
    }
    
    @Override
    public Filter getFilter() {
        return productFilter;
    }
    
    private Filter productFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<FoodProduct> filteredList = new ArrayList<>();
            if (constraint == null || constraint.toString().trim().isEmpty()) {
                filteredList.addAll(allProducts);
            } else {
                String query = constraint.toString().toLowerCase().trim();
                for (FoodProduct product : allProducts) {
                    if (product.matchesQuery(query)) {
                        filteredList.add(product);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }
        
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredProducts = (List<FoodProduct>) results.values;
            notifyDataSetChanged();
        }
    };
    
    class ProductViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private MaterialTextView textViewName;
        
        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.productChipCard);
            textViewName = itemView.findViewById(R.id.productChipText);
        }
        
        public void bind(FoodProduct product, boolean isSelected) {
            textViewName.setText(product.getDisplayName());
            updateUI(isSelected);
            
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductToggle(product, !isSelected);
                }
            });
        }
        
        private void updateUI(boolean isSelected) {
            if (isSelected) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_primary_light));
                textViewName.setTextColor(Color.WHITE);
                cardView.setStrokeWidth(0);
                cardView.setCardElevation(4f);
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_surface_container_low_light));
                textViewName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                cardView.setStrokeWidth(1);
                cardView.setStrokeColor(ContextCompat.getColor(context, R.color.divider));
                cardView.setCardElevation(0f);
            }
        }
    }
}
