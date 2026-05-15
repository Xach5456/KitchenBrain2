package com.example.kitchenbrain.model;

/**
 * Category model class for Firestore
 * MUST have public no-argument constructor for Firestore deserialization
 */
public class Category {
    private String name;
    private int iconResId;

    /**
     * REQUIRED: Public no-argument constructor for Firebase Firestore
     */
    public Category() {
        this.name = "";
        this.iconResId = 0;
    }

    public Category(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }
}
