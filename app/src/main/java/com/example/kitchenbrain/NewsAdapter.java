package com.example.kitchenbrain;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.models.Article;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for displaying news articles in RecyclerView
 * Supports like functionality and share
 */
public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<Article> articles = new ArrayList<>();
    private Set<String> likedArticles = new HashSet<>();
    private Context context;

    public NewsAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        Article article = articles.get(position);
        holder.bind(article);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    /**
     * Update articles list
     */
    public void setArticles(List<Article> newArticles) {
        this.articles = newArticles != null ? newArticles : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Add more articles (for pagination)
     */
    public void addArticles(List<Article> moreArticles) {
        if (moreArticles != null) {
            int startPosition = articles.size();
            articles.addAll(moreArticles);
            notifyItemRangeInserted(startPosition, moreArticles.size());
        }
    }

    /**
     * Clear all articles
     */
    public void clear() {
        articles.clear();
        likedArticles.clear();
        notifyDataSetChanged();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewNews;
        TextView textViewTitle;
        TextView textViewDescription;
        TextView textViewSource;
        ImageButton buttonLike;
        ImageButton buttonShare;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewNews = itemView.findViewById(R.id.imageViewNews);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewSource = itemView.findViewById(R.id.textViewSource);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            buttonShare = itemView.findViewById(R.id.buttonShare);
        }

        void bind(Article article) {
            // Set title
            textViewTitle.setText(article.getTitle() != null ? article.getTitle() : "No Title");

            // Set description
            String description = article.getDescription();
            if (description != null && !description.isEmpty()) {
                textViewDescription.setText(description);
                textViewDescription.setVisibility(View.VISIBLE);
            } else {
                textViewDescription.setVisibility(View.GONE);
            }

            // Set source
            if (article.getSource() != null && article.getSource().getName() != null) {
                textViewSource.setText(article.getSource().getName());
            } else {
                textViewSource.setText("Unknown Source");
            }

            // Load image with Glide
            String imageUrl = article.getUrlToImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(imageViewNews);
            } else {
                imageViewNews.setImageResource(R.drawable.ic_profile_placeholder);
            }

            // Update like button state
            String articleUrl = article.getUrl();
            boolean isLiked = articleUrl != null && likedArticles.contains(articleUrl);
            updateLikeButton(isLiked);

            // Like button click
            buttonLike.setOnClickListener(v -> {
                boolean currentlyLiked = likedArticles.contains(articleUrl);
                if (currentlyLiked) {
                    likedArticles.remove(articleUrl);
                    updateLikeButton(false);
                } else {
                    likedArticles.add(articleUrl);
                    updateLikeButton(true);
                }
            });

            // Share button click
            buttonShare.setOnClickListener(v -> {
                if (article.getUrl() != null) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, article.getTitle());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, article.getTitle() + "\n\n" + article.getUrl());
                    itemView.getContext().startActivity(Intent.createChooser(shareIntent, "Share via"));
                }
            });

            // Item click - open article in browser
            itemView.setOnClickListener(v -> {
                if (article.getUrl() != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
                    itemView.getContext().startActivity(intent);
                }
            });
        }

        private void updateLikeButton(boolean isLiked) {
            if (isLiked) {
                buttonLike.setImageResource(R.drawable.ic_heart_filled);
                buttonLike.setColorFilter(itemView.getContext().getColor(R.color.heart_red));
            } else {
                buttonLike.setImageResource(R.drawable.ic_heart_outline);
                buttonLike.setColorFilter(null);
            }
        }
    }
}
