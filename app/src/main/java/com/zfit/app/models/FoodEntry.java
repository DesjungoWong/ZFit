package com.zfit.app.models;

public class FoodEntry {
    private long id;
    private String date;
    private String mealType;
    private String foodName;
    private int calories;
    private float protein;
    private float carbs;
    private float fat;
    private String thumbnail;
    private String source;
    private long createdAt;

    public FoodEntry() {}

    public FoodEntry(String date, String mealType, String foodName,
                     int calories, float protein, float carbs, float fat, String source) {
        this.date = date;
        this.mealType = mealType;
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.source = source;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }
    public float getProtein() { return protein; }
    public void setProtein(float protein) { this.protein = protein; }
    public float getCarbs() { return carbs; }
    public void setCarbs(float carbs) { this.carbs = carbs; }
    public float getFat() { return fat; }
    public void setFat(float fat) { this.fat = fat; }
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
