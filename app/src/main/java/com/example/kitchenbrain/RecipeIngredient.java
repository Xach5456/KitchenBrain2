package com.example.kitchenbrain;

public class RecipeIngredient {
    private String ingredientName;
    private String category;
    private String quantity;
    private String unit;

    // Default constructor required for Firebase
    public RecipeIngredient() {
    }

    public RecipeIngredient(String ingredientName, String category, String quantity, String unit) {
        this.ingredientName = ingredientName;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
