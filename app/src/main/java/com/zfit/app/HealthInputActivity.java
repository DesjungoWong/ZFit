package com.zfit.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zfit.app.db.FoodDbHelper;
import com.zfit.app.models.HealthRecord;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HealthInputActivity extends AppCompatActivity {

    private EditText etSleepScore, etSleepHours, etSteps, etCaloriesBurned,
            etExerciseMin, etExerciseType, etHeartRate, etWeight,
            etBodyFat, etStress, etWater, etNotes;
    private FoodDbHelper dbHelper;
    private String currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_input);
        dbHelper = FoodDbHelper.getInstance(this);
        currentDate = getIntent().getStringExtra("date");
        if (currentDate == null)
            currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

        bindViews();
        loadExistingRecord();

        findViewById(R.id.btnSave).setOnClickListener(v -> saveRecord());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void bindViews() {
        etSleepScore = findViewById(R.id.etSleepScore);
        etSleepHours = findViewById(R.id.etSleepHours);
        etSteps = findViewById(R.id.etSteps);
        etCaloriesBurned = findViewById(R.id.etCaloriesBurned);
        etExerciseMin = findViewById(R.id.etExerciseMin);
        etExerciseType = findViewById(R.id.etExerciseType);
        etHeartRate = findViewById(R.id.etHeartRate);
        etWeight = findViewById(R.id.etWeight);
        etBodyFat = findViewById(R.id.etBodyFat);
        etStress = findViewById(R.id.etStress);
        etWater = findViewById(R.id.etWater);
        etNotes = findViewById(R.id.etNotes);
    }

    private void loadExistingRecord() {
        new Thread(() -> {
            HealthRecord r = dbHelper.getHealthRecordForDate(currentDate);
            if (r != null) {
                runOnUiThread(() -> {
                    if (r.getSleepScore() > 0) etSleepScore.setText(String.valueOf(r.getSleepScore()));
                    if (r.getSleepHours() > 0) etSleepHours.setText(String.valueOf(r.getSleepHours()));
                    if (r.getSteps() > 0) etSteps.setText(String.valueOf(r.getSteps()));
                    if (r.getCaloriesBurned() > 0) etCaloriesBurned.setText(String.valueOf(r.getCaloriesBurned()));
                    if (r.getExerciseMinutes() > 0) etExerciseMin.setText(String.valueOf(r.getExerciseMinutes()));
                    if (r.getExerciseType() != null && !r.getExerciseType().isEmpty())
                        etExerciseType.setText(r.getExerciseType());
                    if (r.getHeartRate() > 0) etHeartRate.setText(String.valueOf(r.getHeartRate()));
                    if (r.getWeight() > 0) etWeight.setText(String.valueOf(r.getWeight()));
                    if (r.getBodyFat() > 0) etBodyFat.setText(String.valueOf(r.getBodyFat()));
                    if (r.getStressScore() > 0) etStress.setText(String.valueOf(r.getStressScore()));
                    if (r.getWaterMl() > 0) etWater.setText(String.valueOf(r.getWaterMl()));
                    if (r.getNotes() != null && !r.getNotes().isEmpty()) etNotes.setText(r.getNotes());
                });
            }
        }).start();
    }

    private void saveRecord() {
        HealthRecord r = new HealthRecord(currentDate);
        r.setSleepScore(parseInt(etSleepScore));
        r.setSleepHours(parseFloat(etSleepHours));
        r.setSteps(parseInt(etSteps));
        r.setCaloriesBurned(parseInt(etCaloriesBurned));
        r.setExerciseMinutes(parseInt(etExerciseMin));
        r.setExerciseType(etExerciseType.getText().toString().trim());
        r.setHeartRate(parseInt(etHeartRate));
        r.setWeight(parseFloat(etWeight));
        r.setBodyFat(parseFloat(etBodyFat));
        r.setStressScore(parseInt(etStress));
        r.setWaterMl(parseInt(etWater));
        r.setNotes(etNotes.getText().toString().trim());

        new Thread(() -> {
            dbHelper.upsertHealthRecord(r);
            runOnUiThread(() -> {
                Toast.makeText(this, "Health data saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private int parseInt(EditText et) {
        try { return Integer.parseInt(et.getText().toString().trim()); }
        catch (Exception e) { return 0; }
    }

    private float parseFloat(EditText et) {
        try { return Float.parseFloat(et.getText().toString().trim()); }
        catch (Exception e) { return 0f; }
    }
}
