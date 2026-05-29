package com.example.kitchenbrain;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/**
 * Adapter for YouTube Reels with ExoPlayer integration
 * Handles video playback, user interactions, and lifecycle management
 */
public class ReelsAdapter extends RecyclerView.Adapter<ReelsAdapter.ReelViewHolder> {

    private List<YouTubeReel> reels;
    private Context context;
    private ReelInteractionListener listener;
    
    // Currently playing video position
    private int currentlyPlayingPosition = -1;
    
    // ExoPlayer instance (shared across view holders for efficiency)
    private ExoPlayer exoPlayer;
    private boolean isPlayerInitialized = false;

    public interface ReelInteractionListener {
        void onLikeClick(YouTubeReel reel, int position);
        void onCommentClick(YouTubeReel reel, int position);
        void onShareClick(YouTubeReel reel, int position);
        void onSaveClick(YouTubeReel reel, int position);
    }

    public ReelsAdapter(Context context, List<YouTubeReel> reels, ReelInteractionListener listener) {
        this.context = context;
        this.reels = reels;
        this.listener = listener;
        initializePlayer();
    }

    /**
     * Initialize ExoPlayer instance
     */
    private void initializePlayer() {
        if (!isPlayerInitialized) {
            exoPlayer = new ExoPlayer.Builder(context).build();
            isPlayerInitialized = true;
        }
    }

    /**
     * Release ExoPlayer resources
     */
    public void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
            isPlayerInitialized = false;
        }
    }

    /**
     * Pause video playback
     */
    public void pausePlayback() {
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
        }
    }

    /**
     * Resume video playback
     */
    public void resumePlayback() {
        if (exoPlayer != null && currentlyPlayingPosition >= 0) {
            exoPlayer.play();
        }
    }

    @NonNull
    @Override
    public ReelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reel, parent, false);
        return new ReelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReelViewHolder holder, int position) {
        YouTubeReel reel = reels.get(position);
        holder.bind(reel, position);
    }

    @Override
    public int getItemCount() {
        return reels.size();
    }

    /**
     * Update reels list
     */
    public void updateReels(List<YouTubeReel> newReels) {
        this.reels = newReels;
        notifyDataSetChanged();
    }

    /**
     * Update single reel (for like/save interactions)
     */
    public void updateReel(int position, YouTubeReel reel) {
        if (position >= 0 && position < reels.size()) {
            reels.set(position, reel);
            notifyItemChanged(position);
        }
    }

    /**
     * ViewHolder for Reel items
     */
    public class ReelViewHolder extends RecyclerView.ViewHolder {
        
        private PlayerView playerView;
        private ProgressBar progressBar;
        private ImageView imageThumbnail;
        private TextView textChannelName;
        private TextView textVideoTitle;
        private TextView textViewCount;
        private TextView textLikeCount;
        private TextView textCommentCount;
        private FloatingActionButton buttonLike;
        private FloatingActionButton buttonComment;
        private FloatingActionButton buttonShare;
        private FloatingActionButton buttonSave;

        private int adapterPosition;
        private YouTubeReel currentReel;

        public ReelViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.playerView);
            progressBar = itemView.findViewById(R.id.progressBar);
            imageThumbnail = itemView.findViewById(R.id.imageThumbnail);
            textChannelName = itemView.findViewById(R.id.textChannelName);
            textVideoTitle = itemView.findViewById(R.id.textVideoTitle);
            textViewCount = itemView.findViewById(R.id.textViewCount);
            textLikeCount = itemView.findViewById(R.id.textLikeCount);
            textCommentCount = itemView.findViewById(R.id.textCommentCount);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            buttonComment = itemView.findViewById(R.id.buttonComment);
            buttonShare = itemView.findViewById(R.id.buttonShare);
            buttonSave = itemView.findViewById(R.id.buttonSave);

            // Connect player to view
            playerView.setPlayer(exoPlayer);
        }

        /**
         * Bind reel data to views
         */
        public void bind(YouTubeReel reel, int position) {
            this.currentReel = reel;
            this.adapterPosition = position;

            // Set text data
            textChannelName.setText(reel.getChannelTitle());
            textVideoTitle.setText(reel.getTitle());
            textViewCount.setText(reel.getFormattedViewCount());
            textLikeCount.setText(reel.getFormattedLikeCount());
            textCommentCount.setText(String.valueOf(reel.getCommentCount()));

            // Load thumbnail
            Glide.with(context)
                    .load(reel.getThumbnailUrl())
                    .centerCrop()
                    .into(imageThumbnail);

            // Update like/save button states
            updateLikeButton(reel.isLiked());
            updateSaveButton(reel.isSaved());

            // Setup click listeners
            setupClickListeners();

            // Handle video playback based on visibility
            setupVideoPlayback(position);
        }

        /**
         * Setup click listeners for interaction buttons
         */
        private void setupClickListeners() {
            // Like button with animation
            buttonLike.setOnClickListener(v -> {
                boolean newLikeState = !currentReel.isLiked();
                currentReel.setLiked(newLikeState);
                updateLikeButton(newLikeState);
                
                if (listener != null) {
                    listener.onLikeClick(currentReel, adapterPosition);
                }
            });

            // Comment button
            buttonComment.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentClick(currentReel, adapterPosition);
                }
            });

            // Share button
            buttonShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(currentReel, adapterPosition);
                } else {
                    shareReel(currentReel);
                }
            });

            // Save button
            buttonSave.setOnClickListener(v -> {
                boolean newSaveState = !currentReel.isSaved();
                currentReel.setSaved(newSaveState);
                updateSaveButton(newSaveState);
                
                if (listener != null) {
                    listener.onSaveClick(currentReel, adapterPosition);
                }
            });
        }

        /**
         * Setup video playback - play if this is the current visible item
         */
        private void setupVideoPlayback(int position) {
            if (position == currentlyPlayingPosition) {
                playVideo(currentReel);
            } else {
                // Show thumbnail for non-visible items
                playerView.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }

        /**
         * Play video using ExoPlayer
         */
        private void playVideo(YouTubeReel reel) {
            if (exoPlayer == null) return;

            // Show loading state
            progressBar.setVisibility(View.VISIBLE);
            imageThumbnail.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);

            // Construct YouTube video URL
            // Note: For production, you'd use YouTube IFrame API or extract actual video URL
            // This is a simplified version - in production use YouTube Android Player API
            String videoUrl = "https://www.youtube.com/watch?v=" + reel.getVideoId();
            
            // For ExoPlayer, we need direct video URLs
            // Since YouTube doesn't provide direct URLs easily, we'll open in browser/YouTube app
            // Alternative: Use YouTube Android Player API (requires YouTube app)
            
            // TEMPORARY: Show thumbnail and open YouTube on tap
            progressBar.setVisibility(View.GONE);
            playerView.setVisibility(View.GONE);
            imageThumbnail.setVisibility(View.VISIBLE);
            
            imageThumbnail.setOnClickListener(v -> openYouTubeVideo(reel));
        }

        /**
         * Open video in YouTube app or browser
         */
        private void openYouTubeVideo(YouTubeReel reel) {
            String videoUrl = "https://www.youtube.com/watch?v=" + reel.getVideoId();
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(videoUrl));
            intent.setPackage("com.google.android.youtube");
            
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                // YouTube app not installed, open in browser
                intent.setPackage(null);
                context.startActivity(intent);
            }
        }

        /**
         * Update like button appearance
         */
        private void updateLikeButton(boolean isLiked) {
            if (isLiked) {
                buttonLike.setImageResource(R.drawable.ic_heart_filled);
                buttonLike.setColorFilter(
                    context.getResources().getColor(R.color.error_red)
                );
            } else {
                buttonLike.setImageResource(R.drawable.ic_heart_outlined);
                buttonLike.setColorFilter(
                    context.getResources().getColor(R.color.white)
                );
            }
        }

        /**
         * Update save button appearance
         */
        private void updateSaveButton(boolean isSaved) {
            if (isSaved) {
                buttonSave.setImageResource(R.drawable.ic_bookmark_filled);
            } else {
                buttonSave.setImageResource(R.drawable.ic_bookmark_outlined);
            }
        }

        /**
         * Share reel via Intent
         */
        private void shareReel(YouTubeReel reel) {
            String shareText = "Check out this cooking video: " + reel.getTitle() + 
                    "\nhttps://www.youtube.com/watch?v=" + reel.getVideoId();
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            
            context.startActivity(Intent.createChooser(shareIntent, "Share via"));
        }
    }

    /**
     * Called when a new position becomes visible
     */
    public void onScrollToPosition(int position) {
        int oldPosition = currentlyPlayingPosition;
        currentlyPlayingPosition = position;
        
        // Stop old video
        if (oldPosition >= 0 && oldPosition < getItemCount()) {
            notifyItemChanged(oldPosition);
        }
        
        // Play new video
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position);
        }
    }

    /**
     * Show comment dialog
     */
    public void showCommentDialog(YouTubeReel reel, int position) {
        new MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)
                .setTitle("Add Comment")
                .setMessage("Comment feature coming soon!\n\nVideo: " + reel.getTitle())
                .setPositiveButton("OK", null)
                .show();
    }
}
