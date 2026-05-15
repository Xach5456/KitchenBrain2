package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private Context context;
    private OnCategoryClickListener listener;
    private int selectedPosition = -1;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }

    public CategoryAdapter(Context context, OnCategoryClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.categoryList = new ArrayList<>();
        
        // Add default categories
        loadDefaultCategories();
    }

    private void loadDefaultCategories() {
        categoryList.add(new Category("All", R.drawable.ic_home));
        categoryList.add(new Category("Italian", R.drawable.ic_fast_food));
        categoryList.add(new Category("Asian", R.drawable.ic_vegetables));
        categoryList.add(new Category("Desserts", R.drawable.ic_tip_icon));
        categoryList.add(new Category("Healthy", R.drawable.ic_add_photo));
        categoryList.add(new Category("Quick", R.drawable.ic_time));
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        
        holder.textViewCategoryName.setText(category.getName());
        holder.imageViewCategoryIcon.setImageResource(category.getIconResId());
        
        // Highlight selected category
        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.drawable.bg_category_tag);
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent, null));
        }
        
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            
            if (listener != null) {
                listener.onCategoryClick(category, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public Category getItem(int position) {
        return categoryList.get(position);
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCategoryIcon;
        TextView textViewCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCategoryIcon = itemView.findViewById(R.id.imageViewCategoryIcon);
            textViewCategoryName = itemView.findViewById(R.id.textViewCategoryName);
        }
    }
}
