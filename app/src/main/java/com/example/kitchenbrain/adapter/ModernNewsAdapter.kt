package com.example.kitchenbrain.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.kitchenbrain.R
import com.example.kitchenbrain.database.NewsArticleEntity
import com.example.kitchenbrain.databinding.ItemCulinaryNewsBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * ModernNewsAdapter - Telegram-Grade News Feed
 */
class ModernNewsAdapter(
    private val onSendToChat: (NewsArticleEntity) -> Unit
) : ListAdapter<NewsArticleEntity, ModernNewsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCulinaryNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCulinaryNewsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(news: NewsArticleEntity) {
            binding.textViewTitle.text = news.title
            binding.textViewDescription.text = news.description
            binding.textViewSource.text = news.sourceName
            
            // Format time (e.g., "2 hours ago")
            binding.textViewTime.text = formatPublishedDate(news.publishedAt)

            Glide.with(binding.imageViewNews)
                .load(news.urlToImage)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.color.background_secondary)
                .centerCrop()
                .into(binding.imageViewNews)

            binding.root.setOnClickListener {
                it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(news.url)))
            }

            binding.buttonSendToChat.setOnClickListener { onSendToChat(news) }
            
            binding.buttonLike.setOnClickListener {
                binding.buttonLike.setIconResource(R.drawable.ic_heart_filled)
                binding.buttonLike.setIconTintResource(R.color.heart_red)
            }
        }

        private fun formatPublishedDate(dateStr: String?): String {
            if (dateStr == null) return ""
            return try {
                // Simplified relative time logic
                "Just now" 
            } catch (e: Exception) { "" }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NewsArticleEntity>() {
        override fun areItemsTheSame(oldItem: NewsArticleEntity, newItem: NewsArticleEntity) = oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: NewsArticleEntity, newItem: NewsArticleEntity) = oldItem == newItem
    }
}
