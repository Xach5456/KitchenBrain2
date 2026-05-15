package com.example.kitchenbrain.models;

import com.google.gson.annotations.SerializedName;

/**
 * Source model representing the news source/publication
 */
public class Source {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    /**
     * Get the source ID
     * @return Source identifier or null
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the source name
     * @return Name of the news source (e.g., "BBC News", "CNN")
     */
    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
