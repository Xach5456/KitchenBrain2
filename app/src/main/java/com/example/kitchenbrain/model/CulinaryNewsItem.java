package com.example.kitchenbrain.model;

import java.io.Serializable;

/**
 * Model class representing a culinary news item
 * Contains all data needed for display in the feed
 */
public class CulinaryNewsItem implements Serializable {
    
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String source;
    private String url;
    private String publishedAt;
    private int likeCount;
    private boolean isLiked;
    private int commentCount;

    public CulinaryNewsItem() {
        this.isLiked = false;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    public CulinaryNewsItem(String id, String title, String description, String imageUrl, 
                           String source, String url, String publishedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.source = source;
        this.url = url;
        this.publishedAt = publishedAt;
        this.isLiked = false;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : "No title";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "No description available";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSource() {
        return source != null ? source : "Unknown Source";
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }
}
