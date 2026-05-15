package com.example.kitchenbrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal strip of compact recipe cards (Tasty / Yummly style).
 */
public class RecipeSimilarAdapter extends RecyclerView.Adapter<RecipeSimilarAdapter.VH> {

    public interface Listener {
        void onSimilarClick(@NonNull Recipe recipe);
    }

    private final List<Recipe> items = new ArrayList<>();
    private final Listener listener;

    public RecipeSimilarAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Recipe> recipes) {
        items.clear();
        if (recipes != null) {
            items.addAll(recipes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_similar_recipe_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Recipe r = items.get(position);
        holder.title.setText(r.getName() != null ? r.getName() : "");
        if (r.getImageUrl() != null && !r.getImageUrl().isEmpty()) {
            Glide.with(holder.image.getContext())
                    .load(r.getImageUrl())
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_placeholder);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSimilarClick(r);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageSimilarThumb);
            title = itemView.findViewById(R.id.textSimilarTitle);
        }
    }
}
