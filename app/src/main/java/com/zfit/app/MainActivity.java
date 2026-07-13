package com.zfit.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zfit.app.adapters.FoodLogAdapter;
import com.zfit.app.db.FoodDbHelper;
import com.zfit.app.models.FoodEntry;
import com.zfit.app.models.HealthRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvDate, tvCaloriesBig, tvRemaining, tvBurned, tvNet;
    private TextView tvProteinVal, tvCarbsVal, tvFatVal;
    private TextView tvProteinPct, tvCarbsPct, tvFatPct;
    private TextView tvWaterVal;
    private ProgressBar pbCalorie, pbProtein, pbCarbs, pbFat;
    private RecyclerView rvFoodLog;
    private FoodLogAdapter adapter;
    private List<FoodEntry> foodEntries = new ArrayList<>();
    private FoodDbHelper dbHelper;
    private String currentDate;
    private Calendar currentCal;

    private static final String PREFS = "de_prefs";
    private static final SimpleDateFormat SDF_KEY = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat SDF_DISPLAY = new SimpleDateFormat("EEE, d MMM", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = FoodDbHelper.getInstance(this);
        currentCal = Calendar.getInstance();
        currentDate = SDF_KEY.format(currentCal.getTime());

        bindViews();
        setupFoodLog();
        setupButtons();
        refreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void bindViews() {
        tvDate = findViewById(R.id.tvDate);
        tvCaloriesBig = findViewById(R.id.tvCaloriesBig);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvBurned = findViewById(R.id.tvBurned);
        tvNet = findViewById(R.id.tvNet);
        tvProteinVal = findViewById(R.id.tvProteinVal);
        tvCarbsVal = findViewById(R.id.tvCarbsVal);
        tvFatVal = findViewById(R.id.tvFatVal);
        tvProteinPct = findViewById(R.id.tvProteinPct);
        tvCarbsPct = findViewById(R.id.tvCarbsPct);
        tvFatPct = findViewById(R.id.tvFatPct);
        tvWaterVal = findViewById(R.id.tvWaterVal);
        pbCalorie = findViewById(R.id.pbCalorie);
        pbProtein = findViewById(R.id.pbProtein);
        pbCarbs = findViewById(R.id.pbCarbs);
        pbFat = findViewById(R.id.pbFat);
        rvFoodLog = findViewById(R.id.rvFoodLog);
    }

    private void setupFoodLog() {
        adapter = new FoodLogAdapter(foodEntries, entry -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete entry?")
                .setMessage("Remove " + entry.getFoodName() + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    dbHelper.deleteFoodEntry(entry.getId());
                    refreshUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
        rvFoodLog.setLayoutManager(new LinearLayoutManager(this));
        rvFoodLog.setAdapter(adapter);
    }

    private void setupButtons() {
        findViewById(R.id.btnPrevDay).setOnClickListener(v -> {
            currentCal.add(Calendar.DAY_OF_YEAR, -1);
            currentDate = SDF_KEY.format(currentCal.getTime());
            refreshUI();
        });
        findViewById(R.id.btnNextDay).setOnClickListener(v -> {
            currentCal.add(Calendar.DAY_OF_YEAR, 1);
            currentDate = SDF_KEY.format(currentCal.getTime());
            refreshUI();
        });
        findViewById(R.id.btnAddFood).setOnClickListener(v ->
            startActivity(new Intent(this, AddFoodActivity.class).putExtra("date", currentDate)));
        findViewById(R.id.btnSnapLog).setOnClickListener(v ->
            startActivity(new Intent(this, CameraActivity.class).putExtra("date", currentDate)));
        findViewById(R.id.btnDashboard).setOnClickListener(v ->
            startActivity(new Intent(this, DashboardActivity.class)));
        findViewById(R.id.btnHCImport).setOnClickListener(v ->
            startActivity(new Intent(this, HealthConnectImportActivity.class)));
        findViewById(R.id.btnHealthOverview).setOnClickListener(v ->
            startActivity(new Intent(this, HealthDashboardActivity.class).putExtra("date", currentDate)));
        findViewById(R.id.btnWeeklyReport).setOnClickListener(v ->
            startActivity(new Intent(this, WeeklyReportActivity.class)));
        findViewById(R.id.btnAddFoodFab).setOnClickListener(v ->
            startActivity(new Intent(this, AddFoodActivity.class).putExtra("date", currentDate)));
        findViewById(R.id.btnWater200).setOnClickListener(v -> addWater(200));
        findViewById(R.id.btnWater500).setOnClickListener(v -> addWater(500));
        findViewById(R.id.btnSettings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnIntimacy).setOnClickListener(v ->
            startActivity(new Intent(this, IntimacyActivity.class)));
    }

    private void addWater(int ml) {
        // Quick-add water by updating health record
        new Thread(() -> {
            HealthRecord record = dbHelper.getHealthRecordForDate(currentDate);
            if (record == null) {
                record = new HealthRecord(currentDate);
            }
            record.setWaterMl(record.getWaterMl() + ml);
            dbHelper.upsertHealthRecord(record);
            runOnUiThread(this::refreshUI);
        }).start();
    }

    private void refreshUI() {
        tvDate.setText(SDF_DISPLAY.format(currentCal.getTime()));

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int calorieGoal = prefs.getInt("calorie_goal", 2000);
        int proteinGoal = prefs.getInt("protein_goal", 150);
        int carbsGoal = prefs.getInt("carbs_goal", 200);
        int fatGoal = prefs.getInt("fat_goal", 65);

        new Thread(() -> {
            int totalCal = dbHelper.getTotalCaloriesForDate(currentDate);
            float[] macros = dbHelper.getMacrosForDate(currentDate);
            List<FoodEntry> entries = dbHelper.getFoodEntriesForDate(currentDate);
            HealthRecord healthRecord = dbHelper.getHealthRecordForDate(currentDate);
            int burned = healthRecord != null ? healthRecord.getCaloriesBurned() : 0;
            int waterMl = healthRecord != null ? healthRecord.getWaterMl() : 0;

            runOnUiThread(() -> {
                int remaining = calorieGoal - totalCal + burned;
                int net = totalCal - burned;
                tvCaloriesBig.setText(totalCal + " / " + calorieGoal + " kcal");
                tvRemaining.setText("Remaining\n" + remaining);
                tvBurned.setText("Burned\n" + burned);
                tvNet.setText("Net\n" + net);

                int calPct = calorieGoal > 0 ? Math.min(totalCal * 100 / calorieGoal, 100) : 0;
                pbCalorie.setProgress(calPct);

                tvProteinVal.setText(String.format("%.0fg", macros[0]));
                tvCarbsVal.setText(String.format("%.0fg", macros[1]));
                tvFatVal.setText(String.format("%.0fg", macros[2]));

                int pPct = proteinGoal > 0 ? Math.min((int)(macros[0] * 100 / proteinGoal), 100) : 0;
                int cPct = carbsGoal > 0 ? Math.min((int)(macros[1] * 100 / carbsGoal), 100) : 0;
                int fPct = fatGoal > 0 ? Math.min((int)(macros[2] * 100 / fatGoal), 100) : 0;
                tvProteinPct.setText(pPct + "%");
                tvCarbsPct.setText(cPct + "%");
                tvFatPct.setText(fPct + "%");
                pbProtein.setProgress(pPct);
                pbCarbs.setProgress(cPct);
                pbFat.setProgress(fPct);

                tvWaterVal.setText(waterMl + " ml");

                foodEntries.clear();
                foodEntries.addAll(entries);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }
}
