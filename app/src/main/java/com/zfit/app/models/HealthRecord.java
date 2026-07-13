package com.zfit.app.models;

public class HealthRecord {
    private long id;
    private String date;
    private int sleepScore;
    private float sleepHours;
    private int steps;
    private int caloriesBurned;
    private int exerciseMinutes;
    private String exerciseType;
    private int heartRate;
    private float weight;
    private float bodyFat;
    private int stressScore;
    private int waterMl;
    private String notes;
    private long createdAt;

    public HealthRecord() {}

    public HealthRecord(String date) {
        this.date = date;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public int getSleepScore() { return sleepScore; }
    public void setSleepScore(int sleepScore) { this.sleepScore = sleepScore; }
    public float getSleepHours() { return sleepHours; }
    public void setSleepHours(float sleepHours) { this.sleepHours = sleepHours; }
    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }
    public int getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(int caloriesBurned) { this.caloriesBurned = caloriesBurned; }
    public int getExerciseMinutes() { return exerciseMinutes; }
    public void setExerciseMinutes(int exerciseMinutes) { this.exerciseMinutes = exerciseMinutes; }
    public String getExerciseType() { return exerciseType; }
    public void setExerciseType(String exerciseType) { this.exerciseType = exerciseType; }
    public int getHeartRate() { return heartRate; }
    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }
    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }
    public float getBodyFat() { return bodyFat; }
    public void setBodyFat(float bodyFat) { this.bodyFat = bodyFat; }
    public int getStressScore() { return stressScore; }
    public void setStressScore(int stressScore) { this.stressScore = stressScore; }
    public int getWaterMl() { return waterMl; }
    public void setWaterMl(int waterMl) { this.waterMl = waterMl; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int computeHealthScore(int calorieGoal, int totalCalories) {
        int score = 0;
        if (calorieGoal > 0) {
            float ratio = (float) totalCalories / calorieGoal;
            if (ratio >= 0.8f && ratio <= 1.1f) score += 30;
            else if (ratio >= 0.7f && ratio <= 1.2f) score += 15;
        }
        if (sleepHours >= 7) score += 20;
        else if (sleepHours >= 6) score += 12;
        else if (sleepHours >= 5) score += 6;
        if (steps >= 10000) score += 20;
        else if (steps >= 7500) score += 14;
        else if (steps >= 5000) score += 8;
        if (waterMl >= 2000) score += 15;
        else if (waterMl >= 1500) score += 10;
        else if (waterMl >= 1000) score += 5;
        if (exerciseMinutes >= 30) score += 15;
        else if (exerciseMinutes >= 15) score += 8;
        return Math.min(score, 100);
    }
}
