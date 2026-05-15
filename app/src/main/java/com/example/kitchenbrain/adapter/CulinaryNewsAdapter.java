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
import com.example.kitchenbrain.model.CulinaryNewsItem;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * RecyclerView Adapter for Culinary News Feed
 * Handles display and user interactions (like, comment, send to chat)
 */
public class CulinaryNewsAdapter extends RecyclerView.Adapter<CulinaryNewsAdapter.NewsViewHolder> {

    private List<CulinaryNewsItem> newsList;
    private Context context;
    private OnNewsInteractionListener listener;

    public interface OnNewsInteractionListener {
        void onLikeClick(CulinaryNewsItem news, int position);
        void onCommentClick(CulinaryNewsItem news, int position);
        void onSendToChatClick(CulinaryNewsItem news, int position);
        void onNewsClick(CulinaryNewsItem news, int position);
    }

    public CulinaryNewsAdapter(List<CulinaryNewsItem> newsList, Context context, OnNewsInteractionListener listener) {
        this.newsList = newsList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_culinary_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        CulinaryNewsItem news = newsList.get(position);
        
        holder.textViewTitle.setText(news.getTitle());
        holder.textViewDescription.setText(news.getDescription());
        holder.textViewSource.setText(news.getSource());
        
        if (news.getPublishedAt() != null && !news.getPublishedAt().isEmpty()) {
            holder.textViewTime.setText(formatTimeAgo(news.getPublishedAt()));
        } else {
            holder.textViewTime.setText("Just now");
        }
        
        holder.textLikeCount.setText(news.getLikeCount() + " likes");
        
        if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
            Picasso.get()
                    .load(news.getImageUrl())
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(holder.imageViewNews);
        } else {
            holder.imageViewNews.setImageResource(R.drawable.ic_placeholder);
        }
        
        updateLikeButton(holder.buttonLike, news.isLiked());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNewsClick(news, position);
        });
        
        holder.buttonLike.setOnClickListener(v -> {
            if (listener != null) {
                news.setLiked(!news.isLiked());
                if (news.isLiked()) news.incrementLikeCount();
                else news.decrementLikeCount();
                
                updateLikeButton(holder.buttonLike, news.isLiked());
                holder.textLikeCount.setText(news.getLikeCount() + " likes");
                listener.onLikeClick(news, position);
            }
        });
        
        holder.buttonComment.setOnClickListener(v -> {
            if (listener != null) listener.onCommentClick(news, position);
        });

        // "Comment" button is reused for "Send" in some UIs, or we can add a new one.
        // Assuming we add a send button or use the share icon.
        if (holder.buttonSendToChat != null) {
            holder.buttonSendToChat.setOnClickListener(v -> {
                if (listener != null) listener.onSendToChatClick(news, position);
            });
        }
    }

    private void updateLikeButton(MaterialButton button, boolean isLiked) {
        if (isLiked) {
            button.setTextColor(context.getResources().getColor(R.color.primary_blue));
            button.setIconTintResource(R.color.primary_blue);
        } else {
            button.setTextColor(context.getResources().getColor(R.color.text_secondary));
            button.setIconTintResource(R.color.text_secondary);
        }
    }

    private String formatTimeAgo(String timestamp) {
        try {
            if (timestamp.contains("hours")) return timestamp.split(" ")[0] + "h ago";
            if (timestamp.contains("minutes")) return timestamp.split(" ")[0] + "m ago";
            if (timestamp.contains("days")) return timestamp.split(" ")[0] + "d ago";
            return "Recent";
        } catch (Exception e) {
            return "Recent";
        }
    }

    @Override
    public int getItemCount() {
        return newsList != null ? newsList.size() : 0;
    }

    public void submitList(@NonNull List<CulinaryNewsItem> items) {
        newsList.clear();
        newsList.addAll(items);
        notifyDataSetChanged();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewNews;
        TextView textViewTitle, textViewDescription, textViewSource, textViewTime, textLikeCount;
        MaterialButton buttonLike, buttonComment, buttonSendToChat;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewNews = itemView.findViewById(R.id.imageViewNews);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewSource = itemView.findViewById(R.id.textViewSource);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textLikeCount = itemView.findViewById(R.id.textLikeCount);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            buttonComment = itemView.findViewById(R.id.buttonComment);
            // Link to the send button which I will add to XML
            buttonSendToChat = itemView.findViewById(R.id.buttonSendToChat);
        }
    }
}
