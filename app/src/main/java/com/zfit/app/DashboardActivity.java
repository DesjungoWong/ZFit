package com.zfit.app;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.zfit.app.db.FoodDbHelper;
import com.zfit.app.models.HealthRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvHealthScore, tvScoreLabel, tvTabDaily, tvTabWeekly, tvTabMonthly, tvTabYearly;
    private TextView tvCalVal, tvStepsVal, tvSleepVal, tvWaterVal, tvHRVal, tvExerciseVal;
    private ProgressBar pbScore;
    private LinearLayout llBarChart;
    private FoodDbHelper dbHelper;
    private int activeTab = 0;
    private static final String PREFS = "zfit_prefs";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        dbHelper = FoodDbHelper.getInstance(this);

        tvHealthScore = findViewById(R.id.tvHealthScore);
        tvScoreLabel = findViewById(R.id.tvScoreLabel);
        pbScore = findViewById(R.id.pbScore);
        tvCalVal = findViewById(R.id.tvCalVal);
        tvStepsVal = findViewById(R.id.tvStepsVal);
        tvSleepVal = findViewById(R.id.tvSleepVal);
        tvWaterVal = findViewById(R.id.tvWaterVal);
        tvHRVal = findViewById(R.id.tvHRVal);
        tvExerciseVal = findViewById(R.id.tvExerciseVal);
        llBarChart = findViewById(R.id.llBarChart);

        tvTabDaily = findViewById(R.id.tvTabDaily);
        tvTabWeekly = findViewById(R.id.tvTabWeekly);
        tvTabMonthly = findViewById(R.id.tvTabMonthly);
        tvTabYearly = findViewById(R.id.tvTabYearly);

        tvTabDaily.setOnClickListener(v -> selectTab(0));
        tvTabWeekly.setOnClickListener(v -> selectTab(1));
        tvTabMonthly.setOnClickListener(v -> selectTab(2));
        tvTabYearly.setOnClickListener(v -> selectTab(3));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAIAdvisory).setOnClickListener(v -> callAIAdvisory());

        selectTab(0);
    }

    private void selectTab(int tab) {
        activeTab = tab;
        int active = 0xFFA100FF;
        int inactive = 0xFF5C5C66;
        tvTabDaily.setTextColor(tab == 0 ? active : inactive);
        tvTabWeekly.setTextColor(tab == 1 ? active : inactive);
        tvTabMonthly.setTextColor(tab == 2 ? active : inactive);
        tvTabYearly.setTextColor(tab == 3 ? active : inactive);
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            String today = SDF.format(Calendar.getInstance().getTime());
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            int calorieGoal = prefs.getInt("calorie_goal", 2000);

            // Get today's data
            int totalCal = dbHelper.getTotalCaloriesForDate(today);
            HealthRecord hr = dbHelper.getHealthRecordForDate(today);
            if (hr == null) hr = new HealthRecord(today);

            int score = hr.computeHealthScore(calorieGoal, totalCal);
            final int finalScore = score;
            final HealthRecord finalHr = hr;
            final int finalCal = totalCal;

            // Get weekly data for chart
            Calendar cal = Calendar.getInstance();
            String endDate = SDF.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -6);
            String startDate = SDF.format(cal.getTime());
            List<HealthRecord> weekRecords = dbHelper.getHealthRecordsForWeek(startDate, endDate);

            runOnUiThread(() -> {
                tvHealthScore.setText(String.valueOf(finalScore));
                pbScore.setProgress(finalScore);

                if (finalScore >= 80) {
                    tvScoreLabel.setText("Excellent");
                    tvScoreLabel.setTextColor(0xFF4FD1A0);
                } else if (finalScore >= 60) {
                    tvScoreLabel.setText("Good");
                    tvScoreLabel.setTextColor(0xFFD9B96A);
                } else if (finalScore >= 40) {
                    tvScoreLabel.setText("Needs Work");
                    tvScoreLabel.setTextColor(0xFFE8A838);
                } else {
                    tvScoreLabel.setText("Poor");
                    tvScoreLabel.setTextColor(0xFFFF5C7A);
                }

                tvCalVal.setText(finalCal + " / " + calorieGoal + " kcal");
                tvStepsVal.setText(String.valueOf(finalHr.getSteps()));
                tvSleepVal.setText(String.format("%.1f hrs", finalHr.getSleepHours()));
                tvWaterVal.setText(finalHr.getWaterMl() + " ml");
                tvHRVal.setText(finalHr.getHeartRate() > 0 ? finalHr.getHeartRate() + " bpm" : "—");
                tvExerciseVal.setText(finalHr.getExerciseMinutes() + " min");

                drawBarChart(weekRecords, calorieGoal);
            });
        }).start();
    }

    private void drawBarChart(List<HealthRecord> records, int calorieGoal) {
        llBarChart.removeAllViews();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int maxVal = calorieGoal > 0 ? calorieGoal : 2000;

        for (int i = 0; i < 7; i++) {
            LinearLayout colLayout = new LinearLayout(this);
            colLayout.setOrientation(LinearLayout.VERTICAL);
            colLayout.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            colLayout.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            colLayout.setPadding(2, 0, 2, 0);

            int cal = 0;
            if (i < records.size()) {
                String date = records.get(i).getDate();
                cal = dbHelper.getTotalCaloriesForDate(date);
            }
            int barH = maxVal > 0 ? (int)(cal * 100f / maxVal) : 0;
            barH = Math.max(4, Math.min(barH, 100));

            View bar = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, barH * 2);
            bar.setLayoutParams(lp);
            bar.setBackgroundColor(0xFFA100FF);

            TextView label = new TextView(this);
            label.setText(i < days.length ? days[i] : "");
            label.setTextSize(9);
            label.setTextColor(0xFF5C5C66);
            label.setGravity(android.view.Gravity.CENTER);

            colLayout.addView(bar);
            colLayout.addView(label);
            llBarChart.addView(colLayout);
        }
    }

    private void callAIAdvisory() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String apiKey = prefs.getString("claude_api_key", "");
        if (apiKey.isEmpty()) {
            showBottomSheet("No API key configured. Go to Settings to add your Claude API key.");
            return;
        }
        String today = SDF.format(Calendar.getInstance().getTime());
        int totalCal = dbHelper.getTotalCaloriesForDate(today);
        HealthRecord hr = dbHelper.getHealthRecordForDate(today);
        String prompt = "Give me a brief health advisory based on today's data: " +
                "Calories consumed: " + totalCal + " kcal, " +
                "Steps: " + (hr != null ? hr.getSteps() : 0) + ", " +
                "Sleep: " + (hr != null ? hr.getSleepHours() : 0) + " hours, " +
                "Water: " + (hr != null ? hr.getWaterMl() : 0) + " ml, " +
                "Exercise: " + (hr != null ? hr.getExerciseMinutes() : 0) + " minutes. " +
                "Be concise, 3-4 sentences.";

        showBottomSheet("Getting AI advisory...");

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
                body.put("max_tokens", 256);
                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", prompt);
                messages.put(msg);
                body.put("messages", messages);

                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(body.toString());
                writer.flush();

                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONObject resp = new JSONObject(sb.toString());
                String text = resp.getJSONArray("content").getJSONObject(0).getString("text");
                runOnUiThread(() -> showBottomSheet(text));
            } catch (Exception e) {
                runOnUiThread(() -> showBottomSheet("Error: " + e.getMessage()));
            }
        }).start();
    }

    private BottomSheetDialog bsDialog;
    private void showBottomSheet(String message) {
        if (bsDialog != null && bsDialog.isShowing()) bsDialog.dismiss();
        bsDialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_ai, null);
        ((TextView) v.findViewById(R.id.tvAIResponse)).setText(message);
        v.findViewById(R.id.btnCloseSheet).setOnClickListener(x -> bsDialog.dismiss());
        bsDialog.setContentView(v);
        bsDialog.show();
    }
}
