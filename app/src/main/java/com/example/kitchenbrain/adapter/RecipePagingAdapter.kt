package com.example.kitchenbrain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kitchenbrain.R
import com.example.kitchenbrain.database.CachedRecipe
import com.example.kitchenbrain.databinding.ItemRecipePagingBinding

/**
 * RecipePagingAdapter - Paging3 RecyclerView adapter
 * 
 * 🔥 PAGING ARCHITECTURE:
 * - Efficient diffing with PagingDataAdapter
 * - Loading states handling
 * - Memory-efficient recycling
 * - Social recipe interactions
 */
class RecipePagingAdapter(
    private val onRecipeClick: (CachedRecipe) -> Unit,
    private val onLikeClick: (CachedRecipe) -> Unit,
    private val onCommentClick: (CachedRecipe) -> Unit,
    private val onSaveClick: (CachedRecipe) -> Unit
) : PagingDataAdapter<CachedRecipe, RecipePagingAdapter.RecipeViewHolder>(RecipeDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipePagingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecipeViewHolder(binding, onRecipeClick, onLikeClick, onCommentClick, onSaveClick)
    }
    
    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = getItem(position)
        recipe?.let { holder.bind(it) }
    }
    
    class RecipeViewHolder(
        private val binding: ItemRecipePagingBinding,
        private val onRecipeClick: (CachedRecipe) -> Unit,
        private val onLikeClick: (CachedRecipe) -> Unit,
        private val onCommentClick: (CachedRecipe) -> Unit,
        private val onSaveClick: (CachedRecipe) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(recipe: CachedRecipe) {
            // Basic info
            binding.titleTextView.text = recipe.title
            binding.authorTextView.text = "by ${recipe.authorName}"
            binding.descriptionTextView.text = recipe.description
            
            // Image
            if (!recipe.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(recipe.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(binding.recipeImageView)
            } else {
                binding.recipeImageView.setImageResource(R.drawable.ic_placeholder)
            }
            
            // Author avatar
            if (!recipe.authorAvatar.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(recipe.authorAvatar)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(binding.authorAvatarView)
            } else {
                binding.authorAvatarView.setImageResource(R.drawable.ic_avatar_placeholder)
            }
            
            // Social metrics
            binding.likesCountTextView.text = formatCount(recipe.likes)
            binding.commentsCountTextView.text = formatCount(recipe.comments)
            binding.savesCountTextView.text = formatCount(recipe.saves)
            
            // Social priority indicator
            when (recipe.socialPriority) {
                "mutual" -> {
                    binding.priorityBadgeTextView.text = "🔥 Mutual"
                    binding.priorityBadgeTextView.visibility = View.VISIBLE
                }
                "follower" -> {
                    binding.priorityBadgeTextView.text = "👥 Following"
                    binding.priorityBadgeTextView.visibility = View.VISIBLE
                }
                else -> {
                    binding.priorityBadgeTextView.visibility = View.GONE
                }
            }
            
            // Recipe metadata
            binding.cookTimeTextView.text = "${recipe.cookTime} min"
            binding.difficultyTextView.text = recipe.difficulty
            binding.difficultyTextView.setTextColor(getDifficultyColor(recipe.difficulty))
            
            // Click listeners
            binding.root.setOnClickListener { onRecipeClick(recipe) }
            binding.likeButton.setOnClickListener { onLikeClick(recipe) }
            binding.commentButton.setOnClickListener { onCommentClick(recipe) }
            binding.saveButton.setOnClickListener { onSaveClick(recipe) }
            
            // Update like button state
            updateLikeButton(recipe)
            updateSaveButton(recipe)
        }
        
        private fun updateLikeButton(recipe: CachedRecipe) {
            val isLiked = recipe.likedBy.contains(getCurrentUserId())
            binding.likeButton.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
        }
        
        private fun updateSaveButton(recipe: CachedRecipe) {
            val isSaved = recipe.savedBy.contains(getCurrentUserId())
            binding.saveButton.setImageResource(
                if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
            )
        }
        
        private fun getCurrentUserId(): String {
            // This should get current user ID from Firebase Auth
            return "current_user_id" // Placeholder
        }
        
        private fun formatCount(count: Long): String {
            return when {
                count < 1000 -> count.toString()
                count < 1000000 -> String.format("%.1fK", count / 1000.0)
                else -> String.format("%.1fM", count / 1000000.0)
            }
        }
        
        private fun getDifficultyColor(difficulty: String): Int {
            return when (difficulty) {
                "Easy" -> 0xFF4CAF50.toInt()
                "Medium" -> 0xFFFF9800.toInt()
                "Hard" -> 0xFFF44336.toInt()
                else -> 0xFF757575.toInt()
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates
     */
    class RecipeDiffCallback : DiffUtil.ItemCallback<CachedRecipe>() {
        override fun areItemsTheSame(oldItem: CachedRecipe, newItem: CachedRecipe): Boolean {
            return oldItem.recipeId == newItem.recipeId
        }
        
        override fun areContentsTheSame(oldItem: CachedRecipe, newItem: CachedRecipe): Boolean {
            return oldItem == newItem
        }
    }
}
