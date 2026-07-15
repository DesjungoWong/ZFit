package com.zfit.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.zfit.app.db.FoodDbHelper;
import com.zfit.app.models.FoodEntry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeeklyReportActivity extends AppCompatActivity {

    private static final int REQ_IMPORT = 401;
    private TextView tvTotalDays, tvTotalMeals, tvAvgCal, tvTotalCal;
    private LinearLayout llBarChart, llDayCards;
    private FoodDbHelper dbHelper;
    private static final SimpleDateFormat SDF_KEY = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat SDF_DISPLAY = new SimpleDateFormat("EEE d", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_report);
        dbHelper = FoodDbHelper.getInstance(this);
        tvTotalDays = findViewById(R.id.tvTotalDays);
        tvTotalMeals = findViewById(R.id.tvTotalMeals);
        tvAvgCal = findViewById(R.id.tvAvgCal);
        tvTotalCal = findViewById(R.id.tvTotalCal);
        llBarChart = findViewById(R.id.llBarChart);
        llDayCards = findViewById(R.id.llDayCards);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnImportReport).setOnClickListener(v -> pickFile());
        setupBottomNav();
        loadWeeklyData();
    }

    private void loadWeeklyData() {
        new Thread(() -> {
            Calendar cal = Calendar.getInstance();
            String endDate = SDF_KEY.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -6);
            String startDate = SDF_KEY.format(cal.getTime());

            List<FoodEntry> allEntries = dbHelper.getFoodEntriesForWeek(startDate, endDate);

            // Group by date
            Map<String, List<FoodEntry>> byDate = new LinkedHashMap<>();
            Calendar iterCal = Calendar.getInstance();
            iterCal.add(Calendar.DAY_OF_YEAR, -6);
            for (int i = 0; i < 7; i++) {
                String date = SDF_KEY.format(iterCal.getTime());
                byDate.put(date, new ArrayList<>());
                iterCal.add(Calendar.DAY_OF_YEAR, 1);
            }
            for (FoodEntry e : allEntries) {
                List<FoodEntry> list = byDate.get(e.getDate());
                if (list != null) list.add(e);
            }

            int totalMeals = allEntries.size();
            int totalCal = 0;
            int daysWithData = 0;
            for (List<FoodEntry> dayEntries : byDate.values()) {
                if (!dayEntries.isEmpty()) {
                    daysWithData++;
                    for (FoodEntry e : dayEntries) totalCal += e.getCalories();
                }
            }
            int avgCal = daysWithData > 0 ? totalCal / daysWithData : 0;
            final int finalTotal = totalCal, finalAvg = avgCal, finalDays = daysWithData, finalMeals = totalMeals;
            final Map<String, List<FoodEntry>> finalMap = byDate;
            final int maxCal = byDate.values().stream().mapToInt(
                    list -> list.stream().mapToInt(FoodEntry::getCalories).sum()).max().orElse(2000);

            runOnUiThread(() -> {
                tvTotalDays.setText(String.valueOf(finalDays));
                tvTotalMeals.setText(String.valueOf(finalMeals));
                tvAvgCal.setText(String.valueOf(finalAvg));
                tvTotalCal.setText(String.valueOf(finalTotal));
                drawBarChart(finalMap, maxCal);
                drawDayCards(finalMap);
            });
        }).start();
    }

    private void drawBarChart(Map<String, List<FoodEntry>> byDate, int maxCal) {
        llBarChart.removeAllViews();
        if (maxCal == 0) maxCal = 2000;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        for (Map.Entry<String, List<FoodEntry>> entry : byDate.entrySet()) {
            int dayTotal = entry.getValue().stream().mapToInt(FoodEntry::getCalories).sum();
            int barH = Math.max(4, Math.min(dayTotal * 160 / maxCal, 160));

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            col.setPadding(2, 0, 2, 0);

            View bar = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, barH);
            bar.setLayoutParams(lp);
            boolean isToday = entry.getKey().equals(SDF_KEY.format(Calendar.getInstance().getTime()));
            bar.setBackgroundColor(isToday ? 0xFFC86BFF : 0xFFA100FF);

            TextView label = new TextView(this);
            try {
                label.setText(SDF_DISPLAY.format(SDF_KEY.parse(entry.getKey())));
            } catch (Exception e) { label.setText(""); }
            label.setTextSize(8);
            label.setTextColor(0xFF5C5C66);
            label.setGravity(android.view.Gravity.CENTER);

            col.addView(bar);
            col.addView(label);
            llBarChart.addView(col);
        }
    }

    private void drawDayCards(Map<String, List<FoodEntry>> byDate) {
        llDayCards.removeAllViews();
        for (Map.Entry<String, List<FoodEntry>> entry : byDate.entrySet()) {
            List<FoodEntry> entries = entry.getValue();
            if (entries.isEmpty()) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(new android.graphics.drawable.GradientDrawable());
            ((android.graphics.drawable.GradientDrawable)card.getBackground()).setColor(0xFF141419);
            card.setPadding(14, 14, 14, 14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 4);
            card.setLayoutParams(lp);

            int totalCal = entries.stream().mapToInt(FoodEntry::getCalories).sum();
            float totalP = 0, totalC = 0, totalF = 0;
            for (FoodEntry e : entries) { totalP += e.getProtein(); totalC += e.getCarbs(); totalF += e.getFat(); }

            TextView tvDate2 = new TextView(this);
            try { tvDate2.setText(new SimpleDateFormat("EEEE, d MMM", Locale.US).format(SDF_KEY.parse(entry.getKey()))); }
            catch (Exception e) { tvDate2.setText(entry.getKey()); }
            tvDate2.setTextColor(0xFFF4F4F6);
            tvDate2.setTextSize(13);
            tvDate2.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvCalDay = new TextView(this);
            tvCalDay.setText(totalCal + " kcal");
            tvCalDay.setTextColor(0xFFC86BFF);
            tvCalDay.setTextSize(13);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tvDate2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tvDate2);
            row.addView(tvCalDay);

            TextView tvMacroDay = new TextView(this);
            tvMacroDay.setText(String.format("P %.0fg  C %.0fg  F %.0fg", totalP, totalC, totalF));
            tvMacroDay.setTextColor(0xFF5C5C66);
            tvMacroDay.setTextSize(11);

            card.addView(row);
            card.addView(tvMacroDay);

            for (FoodEntry e : entries) {
                TextView tv = new TextView(this);
                tv.setText("  · " + e.getMealType() + "  " + e.getFoodName() + "  " + e.getCalories() + " kcal");
                tv.setTextColor(0xFF9A9AA3);
                tv.setTextSize(11);
                LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tvLp.setMargins(0, 4, 0, 0);
                tv.setLayoutParams(tvLp);
                card.addView(tv);
            }

            llDayCards.addView(card);
        }
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMPORT && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            parseImportFile(uri);
        }
    }

    private void parseImportFile(Uri uri) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        getContentResolver().openInputStream(uri)));
                String line;
                int imported = 0;
                while ((line = reader.readLine()) != null) {
                    // Try CSV: date,meal_type,food_name,calories,protein,carbs,fat
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        try {
                            String date = parts[0].trim();
                            String meal = parts.length > 1 ? parts[1].trim() : "Snack";
                            String name = parts.length > 2 ? parts[2].trim() : "Unknown";
                            int cal = Integer.parseInt(parts[3].trim());
                            float p = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0;
                            float c = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0;
                            float f = parts.length > 6 ? Float.parseFloat(parts[6].trim()) : 0;
                            FoodEntry entry = new FoodEntry(date, meal, name, cal, p, c, f, "import");
                            dbHelper.insertFoodEntry(entry);
                            imported++;
                        } catch (Exception ignored) {}
                    }
                }
                final int finalImported = imported;
                runOnUiThread(() -> {
                    Toast.makeText(this, finalImported + " entries imported", Toast.LENGTH_SHORT).show();
                    loadWeeklyData();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        nav.setSelectedItemId(R.id.nav_report);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, MainActivity.class)); finish(); }
            else if (id == R.id.nav_log) { startActivity(new Intent(this, AddFoodActivity.class)); }
            else if (id == R.id.nav_snap) { startActivity(new Intent(this, CameraActivity.class)); }
            else if (id == R.id.nav_health) { startActivity(new Intent(this, HealthDashboardActivity.class)); finish(); }
            return false;
        });
    }
}