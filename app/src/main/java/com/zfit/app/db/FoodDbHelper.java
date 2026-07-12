package com.zfit.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.zfit.app.models.FoodEntry;
import com.zfit.app.models.HealthRecord;

import java.util.ArrayList;
import java.util.List;

public class FoodDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "de.db";
    private static final int DB_VERSION = 1;

    private static final String CREATE_FOOD_LOG =
        "CREATE TABLE food_log (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "date TEXT NOT NULL," +
        "meal_type TEXT NOT NULL," +
        "food_name TEXT NOT NULL," +
        "calories INTEGER NOT NULL," +
        "protein REAL DEFAULT 0," +
        "carbs REAL DEFAULT 0," +
        "fat REAL DEFAULT 0," +
        "thumbnail TEXT," +
        "source TEXT DEFAULT 'manual'," +
        "created_at INTEGER NOT NULL)";

    private static final String CREATE_HEALTH_RECORDS =
        "CREATE TABLE health_records (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "date TEXT NOT NULL UNIQUE," +
        "sleep_score INTEGER DEFAULT 0," +
        "sleep_hours REAL DEFAULT 0," +
        "steps INTEGER DEFAULT 0," +
        "calories_burned INTEGER DEFAULT 0," +
        "exercise_minutes INTEGER DEFAULT 0," +
        "exercise_type TEXT DEFAULT ''," +
        "heart_rate INTEGER DEFAULT 0," +
        "weight REAL DEFAULT 0," +
        "body_fat REAL DEFAULT 0," +
        "stress_score INTEGER DEFAULT 0," +
        "water_ml INTEGER DEFAULT 0," +
        "notes TEXT DEFAULT ''," +
        "created_at INTEGER NOT NULL)";

    private static final String CREATE_INTIMACY_LOG =
        "CREATE TABLE intimacy_log (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "date TEXT NOT NULL," +
        "created_at INTEGER NOT NULL)";

    private static FoodDbHelper instance;

    public static synchronized FoodDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FoodDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private FoodDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FOOD_LOG);
        db.execSQL(CREATE_HEALTH_RECORDS);
        db.execSQL(CREATE_INTIMACY_LOG);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS food_log");
        db.execSQL("DROP TABLE IF EXISTS health_records");
        db.execSQL("DROP TABLE IF EXISTS intimacy_log");
        onCreate(db);
    }

    // ---- FOOD LOG ----

    public long insertFoodEntry(FoodEntry entry) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("date", entry.getDate());
        cv.put("meal_type", entry.getMealType());
        cv.put("food_name", entry.getFoodName());
        cv.put("calories", entry.getCalories());
        cv.put("protein", entry.getProtein());
        cv.put("carbs", entry.getCarbs());
        cv.put("fat", entry.getFat());
        cv.put("thumbnail", entry.getThumbnail());
        cv.put("source", entry.getSource() != null ? entry.getSource() : "manual");
        cv.put("created_at", System.currentTimeMillis());
        return db.insert("food_log", null, cv);
    }

    public List<FoodEntry> getFoodEntriesForDate(String date) {
        List<FoodEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("food_log", null, "date=?", new String[]{date},
                null, null, "created_at ASC");
        while (c.moveToNext()) {
            FoodEntry e = cursorToFoodEntry(c);
            list.add(e);
        }
        c.close();
        return list;
    }

    public int getTotalCaloriesForDate(String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(calories) FROM food_log WHERE date=?", new String[]{date});
        int total = 0;
        if (c.moveToFirst()) total = c.getInt(0);
        c.close();
        return total;
    }

    public float[] getMacrosForDate(String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(protein), SUM(carbs), SUM(fat) FROM food_log WHERE date=?", new String[]{date});
        float[] macros = new float[3];
        if (c.moveToFirst()) {
            macros[0] = c.getFloat(0);
            macros[1] = c.getFloat(1);
            macros[2] = c.getFloat(2);
        }
        c.close();
        return macros;
    }

    public void deleteFoodEntry(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("food_log", "id=?", new String[]{String.valueOf(id)});
    }

    public List<FoodEntry> getFoodEntriesForWeek(String startDate, String endDate) {
        List<FoodEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("food_log", null, "date >= ? AND date <= ?",
                new String[]{startDate, endDate}, null, null, "date ASC, created_at ASC");
        while (c.moveToNext()) list.add(cursorToFoodEntry(c));
        c.close();
        return list;
    }

    private FoodEntry cursorToFoodEntry(Cursor c) {
        FoodEntry e = new FoodEntry();
        e.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        e.setDate(c.getString(c.getColumnIndexOrThrow("date")));
        e.setMealType(c.getString(c.getColumnIndexOrThrow("meal_type")));
        e.setFoodName(c.getString(c.getColumnIndexOrThrow("food_name")));
        e.setCalories(c.getInt(c.getColumnIndexOrThrow("calories")));
        e.setProtein(c.getFloat(c.getColumnIndexOrThrow("protein")));
        e.setCarbs(c.getFloat(c.getColumnIndexOrThrow("carbs")));
        e.setFat(c.getFloat(c.getColumnIndexOrThrow("fat")));
        e.setThumbnail(c.getString(c.getColumnIndexOrThrow("thumbnail")));
        e.setSource(c.getString(c.getColumnIndexOrThrow("source")));
        e.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return e;
    }

    // ---- HEALTH RECORDS ----

    public long upsertHealthRecord(HealthRecord r) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("date", r.getDate());
        cv.put("sleep_score", r.getSleepScore());
        cv.put("sleep_hours", r.getSleepHours());
        cv.put("steps", r.getSteps());
        cv.put("calories_burned", r.getCaloriesBurned());
        cv.put("exercise_minutes", r.getExerciseMinutes());
        cv.put("exercise_type", r.getExerciseType() != null ? r.getExerciseType() : "");
        cv.put("heart_rate", r.getHeartRate());
        cv.put("weight", r.getWeight());
        cv.put("body_fat", r.getBodyFat());
        cv.put("stress_score", r.getStressScore());
        cv.put("water_ml", r.getWaterMl());
        cv.put("notes", r.getNotes() != null ? r.getNotes() : "");
        cv.put("created_at", System.currentTimeMillis());
        // Try update first
        int rows = db.update("health_records", cv, "date=?", new String[]{r.getDate()});
        if (rows == 0) return db.insert("health_records", null, cv);
        return rows;
    }

    public HealthRecord getHealthRecordForDate(String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("health_records", null, "date=?", new String[]{date},
                null, null, null);
        HealthRecord r = null;
        if (c.moveToFirst()) r = cursorToHealthRecord(c);
        c.close();
        return r;
    }

    public List<HealthRecord> getHealthRecordsForWeek(String startDate, String endDate) {
        List<HealthRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("health_records", null, "date >= ? AND date <= ?",
                new String[]{startDate, endDate}, null, null, "date ASC");
        while (c.moveToNext()) list.add(cursorToHealthRecord(c));
        c.close();
        return list;
    }

    private HealthRecord cursorToHealthRecord(Cursor c) {
        HealthRecord r = new HealthRecord();
        r.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        r.setDate(c.getString(c.getColumnIndexOrThrow("date")));
        r.setSleepScore(c.getInt(c.getColumnIndexOrThrow("sleep_score")));
        r.setSleepHours(c.getFloat(c.getColumnIndexOrThrow("sleep_hours")));
        r.setSteps(c.getInt(c.getColumnIndexOrThrow("steps")));
        r.setCaloriesBurned(c.getInt(c.getColumnIndexOrThrow("calories_burned")));
        r.setExerciseMinutes(c.getInt(c.getColumnIndexOrThrow("exercise_minutes")));
        r.setExerciseType(c.getString(c.getColumnIndexOrThrow("exercise_type")));
        r.setHeartRate(c.getInt(c.getColumnIndexOrThrow("heart_rate")));
        r.setWeight(c.getFloat(c.getColumnIndexOrThrow("weight")));
        r.setBodyFat(c.getFloat(c.getColumnIndexOrThrow("body_fat")));
        r.setStressScore(c.getInt(c.getColumnIndexOrThrow("stress_score")));
        r.setWaterMl(c.getInt(c.getColumnIndexOrThrow("water_ml")));
        r.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return r;
    }

    // ---- INTIMACY LOG ----

    public long insertIntimacyEntry(String date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("date", date);
        cv.put("created_at", System.currentTimeMillis());
        return db.insert("intimacy_log", null, cv);
    }

    public int getIntimacyCountForMonth(String yearMonth) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM intimacy_log WHERE date LIKE ?",
                new String[]{yearMonth + "%"});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public List<String> getIntimacyDatesForMonth(String yearMonth) {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT date FROM intimacy_log WHERE date LIKE ? ORDER BY date ASC",
                new String[]{yearMonth + "%"});
        while (c.moveToNext()) dates.add(c.getString(0));
        c.close();
        return dates;
    }

    public String getLastIntimacyDate() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT date FROM intimacy_log ORDER BY date DESC LIMIT 1", null);
        String date = null;
        if (c.moveToFirst()) date = c.getString(0);
        c.close();
        return date;
    }

    public int getTotalIntimacyCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM intimacy_log", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
}
