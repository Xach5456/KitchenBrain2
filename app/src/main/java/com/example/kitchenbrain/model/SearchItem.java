package com.example.kitchenbrain.model;

import com.example.kitchenbrain.User;

/**
 * 🔥 UNIFIED SEARCH ITEM - Instagram Level Architecture
 * Single data model for all search types
 */
public class SearchItem {
    
    public enum Type {
        RECIPE,
        USER,
        INGREDIENT
    }
    
    private Type type;
    private Object data;
    private String id;
    
    public SearchItem(Type type, Object data, String id) {
        this.type = type;
        this.data = data;
        this.id = id;
    }
    
    public Type getType() {
        return type;
    }
    
    public Object getData() {
        return data;
    }
    
    public String getId() {
        return id;
    }
    
    /**
     * 🔥 TYPE CAST HELPERS
     */
    public Recipe getRecipe() {
        if (type == Type.RECIPE && data instanceof Recipe) {
            return (Recipe) data;
        }
        return null;
    }
    
    public User getUser() {
        if (type == Type.USER && data instanceof User) {
            return (User) data;
        }
        return null;
    }
    
    public FoodProduct getIngredient() {
        if (type == Type.INGREDIENT && data instanceof FoodProduct) {
            return (FoodProduct) data;
        }
        return null;
    }
}
