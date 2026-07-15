package com.zfit.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.zfit.app.db.FoodDbHelper;
import com.zfit.app.models.HealthRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HealthDashboardActivity extends AppCompatActivity {

    private TextView tvDate, tvHealthScore, tvCalConsumed, tvCalBurned, tvCalNet;
    private TextView tvSleepScore, tvSleepHours, tvSteps, tvExercise;
    private TextView tvHeartRate, tvWeight, tvBodyFat, tvStress, tvWater;
    private FoodDbHelper dbHelper;
    private String currentDate;
    private Calendar currentCal;
    private static final SimpleDateFormat SDF_KEY = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat SDF_DISPLAY = new SimpleDateFormat("EEE, d MMM", Locale.US);
    private static final String PREFS = "de_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_dashboard);
        dbHelper = FoodDbHelper.getInstance(this);
        currentCal = Calendar.getInstance();
        currentDate = getIntent().getStringExtra("date");
        if (currentDate != null) {
            try { currentCal.setTime(SDF_KEY.parse(currentDate)); }
            catch (Exception ignored) {}
        } else {
            currentDate = SDF_KEY.format(currentCal.getTime());
        }

        bindViews();
        setupNavigation();
        setupBottomNav();
        refreshUI();
    }

    private void bindViews() {
        tvDate = findViewById(R.id.tvDate);
        tvHealthScore = findViewById(R.id.tvHealthScore);
        tvCalConsumed = findViewById(R.id.tvCalConsumed);
        tvCalBurned = findViewById(R.id.tvCalBurned);
        tvCalNet = findViewById(R.id.tvCalNet);
        tvSleepScore = findViewById(R.id.tvSleepScore);
        tvSleepHours = findViewById(R.id.tvSleepHours);
        tvSteps = findViewById(R.id.tvSteps);
        tvExercise = findViewById(R.id.tvExercise);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvWeight = findViewById(R.id.tvWeight);
        tvBodyFat = findViewById(R.id.tvBodyFat);
        tvStress = findViewById(R.id.tvStress);
        tvWater = findViewById(R.id.tvWater);
    }

    private void setupNavigation() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
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
        findViewById(R.id.btnLogHealth).setOnClickListener(v ->
            startActivity(new Intent(this, HealthInputActivity.class).putExtra("date", currentDate)));
        findViewById(R.id.btnAIRating).setOnClickListener(v -> callAIRating());
    }

    private void refreshUI() {
        tvDate.setText(SDF_DISPLAY.format(currentCal.getTime()));
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            int calorieGoal = prefs.getInt("calorie_goal", 2000);
            int totalCal = dbHelper.getTotalCaloriesForDate(currentDate);
            HealthRecord r = dbHelper.getHealthRecordForDate(currentDate);
            if (r == null) r = new HealthRecord(currentDate);
            int score = r.computeHealthScore(calorieGoal, totalCal);
            final HealthRecord finalR = r;
            final int finalCal = totalCal;
            final int finalScore = score;

            runOnUiThread(() -> {
                tvHealthScore.setText(String.valueOf(finalScore));
                tvCalConsumed.setText(finalCal + " kcal");
                tvCalBurned.setText(finalR.getCaloriesBurned() + " kcal");
                tvCalNet.setText((finalCal - finalR.getCaloriesBurned()) + " kcal");
                tvSleepScore.setText(String.valueOf(finalR.getSleepScore()));
                tvSleepHours.setText(String.format("%.1f hrs", finalR.getSleepHours()));
                tvSteps.setText(String.valueOf(finalR.getSteps()));
                tvExercise.setText(finalR.getExerciseMinutes() + " min");
                tvHeartRate.setText(finalR.getHeartRate() > 0 ? finalR.getHeartRate() + " bpm" : "—");
                tvWeight.setText(finalR.getWeight() > 0 ?
                        String.format("%.1f kg", finalR.getWeight()) : "—");
                tvBodyFat.setText(finalR.getBodyFat() > 0 ?
                        String.format("%.1f%%", finalR.getBodyFat()) : "—");
                tvStress.setText(finalR.getStressScore() > 0 ?
                        String.valueOf(finalR.getStressScore()) : "—");
                tvWater.setText(finalR.getWaterMl() + " ml");
            });
        }).start();
    }

    private void callAIRating() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String apiKey = prefs.getString("claude_api_key", "");
        if (apiKey.isEmpty()) {
            showAISheet("No API key. Configure in Settings.");
            return;
        }
        int totalCal = dbHelper.getTotalCaloriesForDate(currentDate);
        HealthRecord r = dbHelper.getHealthRecordForDate(currentDate);
        if (r == null) r = new HealthRecord(currentDate);
        final HealthRecord finalR = r;
        String prompt = "Rate my health today: Calories=" + totalCal +
                " Steps=" + r.getSteps() + " Sleep=" + r.getSleepHours() + "hrs" +
                " Water=" + r.getWaterMl() + "ml Exercise=" + r.getExerciseMinutes() + "min" +
                " HR=" + r.getHeartRate() + "bpm. Score out of 100 and brief tips. 3 sentences max.";
        showAISheet("Getting AI rating...");
        final String finalPrompt = prompt;
        new Thread(() -> {
            try {
                URL url = new URL("https://api.anthropic.com/v1/messages");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("anthropic-version", "2023-06-01");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("model", "claude-haiku-4-5-20251001");
                body.put("max_tokens", 200);
                JSONArray msgs = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", finalPrompt);
                msgs.put(msg);
                body.put("messages", msgs);
                OutputStreamWriter w = new OutputStreamWriter(conn.getOutputStream());
                w.write(body.toString()); w.flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject resp = new JSONObject(sb.toString());
                String text = resp.getJSONArray("content").getJSONObject(0).getString("text");
                runOnUiThread(() -> showAISheet(text));
            } catch (Exception e) {
                runOnUiThread(() -> showAISheet("Error: " + e.getMessage()));
            }
        }).start();
    }

    private BottomSheetDialog bsDialog;

    private void showAISheet(String msg) {
        if (bsDialog != null && bsDialog.isShowing()) bsDialog.dismiss();
        bsDialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_ai, null);
        ((TextView) v.findViewById(R.id.tvAIResponse)).setText(msg);
        v.findViewById(R.id.btnCloseSheet).setOnClickListener(x -> bsDialog.dismiss());
        bsDialog.setContentView(v);
        bsDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        nav.setSelectedItemId(R.id.nav_health);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, MainActivity.class)); finish(); }
            else if (id == R.id.nav_log) { startActivity(new Intent(this, AddFoodActivity.class)); }
            else if (id == R.id.nav_snap) { startActivity(new Intent(this, CameraActivity.class)); }
            else if (id == R.id.nav_report) { startActivity(new Intent(this, WeeklyReportActivity.class)); finish(); }
            return false;
        });
    }
}