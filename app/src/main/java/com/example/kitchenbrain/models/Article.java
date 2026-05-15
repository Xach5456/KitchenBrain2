package com.example.kitchenbrain.models;

import com.google.gson.annotations.SerializedName;

/**
 * Article model representing a single news article
 */
public class Article {
    
    @SerializedName("source")
    private Source source;
    
    @SerializedName("author")
    private String author;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("url")
    private String url;
    
    @SerializedName("urlToImage")
    private String urlToImage;
    
    @SerializedName("publishedAt")
    private String publishedAt;
    
    @SerializedName("content")
    private String content;
    
    // Getters
    public Source getSource() { return source; }
    public String getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUrl() { return url; }
    public String getUrlToImage() { return urlToImage; }
    public String getPublishedAt() { return publishedAt; }
    public String getContent() { return content; }

    // Setters (Required for Room-to-Model mapping)
    public void setSource(Source source) { this.source = source; }
    public void setAuthor(String author) { this.author = author; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setUrl(String url) { this.url = url; }
    public void setUrlToImage(String urlToImage) { this.urlToImage = urlToImage; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    public void setContent(String content) { this.content = content; }
    
    public String getStableId() {
        if (url != null && !url.isEmpty()) return url;
        if (title != null) return title;
        return "article_" + hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Article)) return false;
        Article other = (Article) obj;
        return getStableId().equals(other.getStableId());
    }
    
    @Override
    public int hashCode() {
        return getStableId().hashCode();
    }
}
