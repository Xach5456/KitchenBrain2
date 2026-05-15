package com.example.kitchenbrain;

/**
 * Model class representing a YouTube Reel (short cooking video)
 */
public class YouTubeReel {
    private String videoId;
    private String title;
    private String channelTitle;
    private String thumbnailUrl;
    private String publishedAt;
    private long viewCount;
    private long likeCount;
    private String description;
    
    // Local interaction states
    private boolean isLiked;
    private boolean isSaved;
    private int commentCount;

    public YouTubeReel() {
        // Required empty constructor for Firestore/JSON parsing
    }

    public YouTubeReel(String videoId, String title, String channelTitle, 
                       String thumbnailUrl, String publishedAt, long viewCount) {
        this.videoId = videoId;
        this.title = title;
        this.channelTitle = channelTitle;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
        this.viewCount = viewCount;
        this.isLiked = false;
        this.isSaved = false;
        this.commentCount = 0;
    }

    // Getters and Setters
    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    /**
     * Format view count for display (e.g., "1.2M views")
     */
    public String getFormattedViewCount() {
        return formatCount(viewCount) + " views";
    }

    /**
     * Format like count for display
     */
    public String getFormattedLikeCount() {
        return formatCount(likeCount);
    }

    /**
     * Helper method to format large numbers
     */
    private String formatCount(long count) {
        if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        } else {
            return String.valueOf(count);
        }
    }
}
