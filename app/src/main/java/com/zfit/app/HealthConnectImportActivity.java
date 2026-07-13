package com.zfit.app;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zfit.app.db.FoodDbHelper;
import com.zfit.app.models.HealthRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HealthConnectImportActivity extends AppCompatActivity {

    private static final int REQ_ZIP = 301;
    private TextView tvImportResult;
    private FoodDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hc_import);
        dbHelper = FoodDbHelper.getInstance(this);
        tvImportResult = findViewById(R.id.tvImportResult);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSelectZip).setOnClickListener(v -> pickZip());
    }

    private void pickZip() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        startActivityForResult(intent, REQ_ZIP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ZIP && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            processZip(uri);
        }
    }

    private void processZip(Uri uri) {
        tvImportResult.setText("Processing ZIP file...");
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                File cacheDir = getCacheDir();
                File extractDir = new File(cacheDir, "hc_extract");
                extractDir.mkdirs();

                // Extract ZIP
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;
                File dbFile = null;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().endsWith(".db") || entry.getName().endsWith("health_connect.db")) {
                        File f = new File(extractDir, "hc.db");
                        FileOutputStream fos = new FileOutputStream(f);
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                        fos.close();
                        dbFile = f;
                    }
                    zis.closeEntry();
                }
                zis.close();

                if (dbFile == null) {
                    runOnUiThread(() -> tvImportResult.setText("No SQLite database found in ZIP.\n" +
                            "Expected a .db file inside the ZIP archive."));
                    return;
                }

                // Parse the HC database
                Map<String, HealthRecord> records = new HashMap<>();
                SQLiteDatabase hcDb = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(),
                        null, SQLiteDatabase.OPEN_READONLY);

                // Try to read steps
                tryReadTable(hcDb, records, "steps_record", "start_time", "count");
                // Try calories
                tryReadCalories(hcDb, records);
                // Try heart rate
                tryReadHeartRate(hcDb, records);
                // Try sleep
                tryReadSleep(hcDb, records);
                // Try exercise
                tryReadExercise(hcDb, records);
                // Try weight
                tryReadWeight(hcDb, records);
                hcDb.close();

                // Write to local DB
                int imported = 0;
                int totalSteps = 0, totalSleep = 0, totalCalBurned = 0;
                for (HealthRecord r : records.values()) {
                    dbHelper.upsertHealthRecord(r);
                    imported++;
                    totalSteps += r.getSteps();
                    totalSleep += r.getSleepHours();
                    totalCalBurned += r.getCaloriesBurned();
                }

                int avgSteps = imported > 0 ? totalSteps / imported : 0;
                float avgSleep = imported > 0 ? (float) totalSleep / imported : 0;
                int avgCal = imported > 0 ? totalCalBurned / imported : 0;

                final String result = String.format(
                        "Import complete!\n\nDays imported: %d\nAvg steps/day: %d\n" +
                        "Avg sleep: %.1f hrs\nAvg calories burned: %d kcal",
                        imported, avgSteps, avgSleep, avgCal);
                runOnUiThread(() -> tvImportResult.setText(result));

                // Cleanup
                dbFile.delete();

            } catch (Exception e) {
                runOnUiThread(() -> tvImportResult.setText("Error: " + e.getMessage() +
                        "\n\nMake sure you select a valid Health Connect export ZIP file."));
            }
        }).start();
    }

    private void tryReadTable(SQLiteDatabase db, Map<String, HealthRecord> records,
                               String table, String dateCol, String valueCol) {
        try {
            Cursor c = db.rawQuery("SELECT * FROM " + table + " LIMIT 1000", null);
            c.close();
        } catch (Exception ignored) {}
    }

    private void tryReadCalories(SQLiteDatabase db, Map<String, HealthRecord> records) {
        try {
            Cursor c = db.rawQuery(
                    "SELECT date(start_time/1000, 'unixepoch') as day, SUM(energy) FROM active_calories_burned_record GROUP BY day",
                    null);
            while (c.moveToNext()) {
                String date = c.getString(0);
                double joulesOrKcal = c.getDouble(1);
                int kcal = joulesOrKcal > 10000 ? (int)(joulesOrKcal / 4184) : (int) joulesOrKcal;
                HealthRecord r = records.getOrDefault(date, new HealthRecord(date));
                r.setCaloriesBurned(kcal);
                records.put(date, r);
            }
            c.close();
        } catch (Exception ignored) {}
    }

    private void tryReadHeartRate(SQLiteDatabase db, Map<String, HealthRecord> records) {
        try {
            Cursor c = db.rawQuery(
                    "SELECT date(time/1000, 'unixepoch') as day, AVG(bpm) FROM heart_rate_record GROUP BY day",
                    null);
            while (c.moveToNext()) {
                String date = c.getString(0);
                int bpm = (int) c.getDouble(1);
                HealthRecord r = records.getOrDefault(date, new HealthRecord(date));
                r.setHeartRate(bpm);
                records.put(date, r);
            }
            c.close();
        } catch (Exception ignored) {}
    }

    private void tryReadSleep(SQLiteDatabase db, Map<String, HealthRecord> records) {
        try {
            Cursor c = db.rawQuery(
                    "SELECT date(start_time/1000, 'unixepoch') as day, SUM((end_time - start_time)/3600000.0) FROM sleep_session_record GROUP BY day",
                    null);
            while (c.moveToNext()) {
                String date = c.getString(0);
                float hours = (float) c.getDouble(1);
                int sleepScore = hours >= 8 ? 90 : hours >= 7 ? 75 : hours >= 6 ? 55 : 30;
                HealthRecord r = records.getOrDefault(date, new HealthRecord(date));
                r.setSleepHours(hours);
                r.setSleepScore(sleepScore);
                records.put(date, r);
            }
            c.close();
        } catch (Exception ignored) {}
    }

    private void tryReadExercise(SQLiteDatabase db, Map<String, HealthRecord> records) {
        try {
            Cursor c = db.rawQuery(
                    "SELECT date(start_time/1000, 'unixepoch') as day, SUM((end_time - start_time)/60000) FROM exercise_session_record GROUP BY day",
                    null);
            while (c.moveToNext()) {
                String date = c.getString(0);
                int minutes = (int) c.getDouble(1);
                HealthRecord r = records.getOrDefault(date, new HealthRecord(date));
                r.setExerciseMinutes(minutes);
                records.put(date, r);
            }
            c.close();
        } catch (Exception ignored) {}
    }

    private void tryReadWeight(SQLiteDatabase db, Map<String, HealthRecord> records) {
        try {
            Cursor c = db.rawQuery(
                    "SELECT date(time/1000, 'unixepoch') as day, AVG(weight) FROM weight_record GROUP BY day",
                    null);
            while (c.moveToNext()) {
                String date = c.getString(0);
                float weightKg = (float) c.getDouble(1);
                if (weightKg > 500) weightKg = weightKg / 1000; // grams to kg
                HealthRecord r = records.getOrDefault(date, new HealthRecord(date));
                r.setWeight(weightKg);
                records.put(date, r);
            }
            c.close();
        } catch (Exception ignored) {}
    }
}
