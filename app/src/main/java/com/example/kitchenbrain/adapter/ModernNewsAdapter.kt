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
import com.example.kitchenbrain.databinding.ItemCulinaryNewsBinding
import com.example.kitchenbrain.models.Article
import java.util.*

/**
 * ModernNewsAdapter - Telegram-Grade News Feed
 */
class ModernNewsAdapter(
    private val onSendToChat: (Article) -> Unit
) : ListAdapter<Article, ModernNewsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCulinaryNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCulinaryNewsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(news: Article) {
            binding.textViewTitle.text = news.title
            binding.textViewDescription.text = news.description
            binding.textViewSource.text = news.source?.name
            
            // Format time (e.g., "2 hours ago")
            binding.textViewTime.text = formatPublishedDate(news.publishedAt)

            Glide.with(binding.imageViewNews)
                .load(news.urlToImage)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.color.background_secondary)
                .centerCrop()
                .into(binding.imageViewNews)

            binding.root.setOnClickListener {
                val articleUrl = news.url ?: return@setOnClickListener
                it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl)))
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

    class DiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article) = oldItem.stableId == newItem.stableId
        override fun areContentsTheSame(oldItem: Article, newItem: Article) = oldItem == newItem
    }
}
