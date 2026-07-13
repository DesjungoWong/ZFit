package com.zfit.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zfit.app.db.FoodDbHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class IntimacyActivity extends AppCompatActivity {

    private TextView tvMonthCount, tvLastEvent, tvAvgWeek, tvStreak;
    private LinearLayout llHistory;
    private FoodDbHelper dbHelper;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat SDF_YM = new SimpleDateFormat("yyyy-MM", Locale.US);
    private static final SimpleDateFormat SDF_DISPLAY = new SimpleDateFormat("EEEE, d MMM yyyy", Locale.US);
    private boolean showingHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intimacy);
        dbHelper = FoodDbHelper.getInstance(this);

        tvMonthCount = findViewById(R.id.tvMonthCount);
        tvLastEvent = findViewById(R.id.tvLastEvent);
        tvAvgWeek = findViewById(R.id.tvAvgWeek);
        tvStreak = findViewById(R.id.tvStreak);
        llHistory = findViewById(R.id.llHistory);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLogToday).setOnClickListener(v -> logToday());
        findViewById(R.id.btnViewHistory).setOnClickListener(v -> toggleHistory());

        refreshStats();
    }

    private void logToday() {
        String today = SDF.format(Calendar.getInstance().getTime());
        new Thread(() -> {
            dbHelper.insertIntimacyEntry(today);
            runOnUiThread(() -> {
                Toast.makeText(this, "Logged for today", Toast.LENGTH_SHORT).show();
                refreshStats();
            });
        }).start();
    }

    private void refreshStats() {
        new Thread(() -> {
            String today = SDF.format(Calendar.getInstance().getTime());
            String yearMonth = SDF_YM.format(Calendar.getInstance().getTime());
            int monthCount = dbHelper.getIntimacyCountForMonth(yearMonth);
            String lastDate = dbHelper.getLastIntimacyDate();
            int total = dbHelper.getTotalIntimacyCount();

            // Compute days since last
            int daysSince = -1;
            if (lastDate != null) {
                try {
                    long last = SDF.parse(lastDate).getTime();
                    long now = Calendar.getInstance().getTimeInMillis();
                    daysSince = (int) ((now - last) / (1000 * 60 * 60 * 24));
                } catch (Exception ignored) {}
            }

            // Avg per week (last 4 weeks)
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -28);
            String fourWeeksAgo = SDF.format(cal.getTime());
            int last4Weeks = 0;
            List<String> allDates = dbHelper.getIntimacyDatesForMonth(yearMonth);
            // Simplified: use total/total_weeks
            float avgWeek = total > 0 ? (float)total / Math.max(1, (int)(
                    (Calendar.getInstance().getTimeInMillis() - (System.currentTimeMillis() - 28L * 24 * 60 * 60 * 1000)) / (7L * 24 * 60 * 60 * 1000) + 1)) : 0;

            List<String> monthDates = dbHelper.getIntimacyDatesForMonth(yearMonth);

            final int finalDaysSince = daysSince;
            final int finalMonthCount = monthCount;
            final float finalAvg = monthCount / 4.0f;
            runOnUiThread(() -> {
                tvMonthCount.setText(String.valueOf(finalMonthCount));
                tvLastEvent.setText(finalDaysSince < 0 ? "—" :
                        finalDaysSince == 0 ? "Today" : finalDaysSince + " days ago");
                tvAvgWeek.setText(String.format("%.1f / week", finalAvg));
                tvStreak.setText(finalDaysSince == 0 ? "Active" : "—");

                if (showingHistory) showHistory(monthDates);
            });
        }).start();
    }

    private void toggleHistory() {
        showingHistory = !showingHistory;
        if (showingHistory) {
            String yearMonth = SDF_YM.format(Calendar.getInstance().getTime());
            List<String> dates = dbHelper.getIntimacyDatesForMonth(yearMonth);
            showHistory(dates);
        } else {
            llHistory.removeAllViews();
        }
    }

    private void showHistory(List<String> dates) {
        llHistory.removeAllViews();
        for (String date : dates) {
            TextView tv = new TextView(this);
            try {
                tv.setText(SDF_DISPLAY.format(SDF.parse(date)));
            } catch (Exception e) {
                tv.setText(date);
            }
            tv.setTextColor(0xFF9A9AA3);
            tv.setTextSize(13);
            tv.setPadding(0, 8, 0, 8);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 1);
            tv.setLayoutParams(lp);
            tv.setBackgroundColor(0xFF141419);
            tv.setPadding(14, 12, 14, 12);
            llHistory.addView(tv);
        }
        if (dates.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No entries this month");
            empty.setTextColor(0xFF5C5C66);
            empty.setTextSize(13);
            empty.setPadding(14, 12, 14, 12);
            llHistory.addView(empty);
        }
    }
}
