package com.zfit.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zfit.app.db.FoodDbHelper;
import com.zfit.app.models.FoodEntry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {

    private static final int REQ_CAMERA = 101;
    private static final int REQ_GALLERY = 102;
    private static final int REQ_CAMERA_PERM = 201;

    private ImageView ivPreview;
    private TextView tvAIStatus, tvFoodName, tvCalResult, tvMacroResult;
    private LinearLayout llResult;
    private Button btnAddToLog, btnAIToggle;
    private Bitmap capturedBitmap;
    private String currentDate;
    private FoodDbHelper dbHelper;
    private boolean aiEnabled = true;
    private static final String PREFS = "zfit_prefs";

    // Detected food
    private String detectedFoodName = "";
    private int detectedCalories = 0;
    private float detectedProtein = 0, detectedCarbs = 0, detectedFat = 0;

    // SG Food fallback list
    private static final String[][] SG_FOODS = {
        {"Chicken Rice", "480", "28", "54", "16"},
        {"Char Kway Teow", "744", "16", "96", "32"},
        {"Nasi Lemak with egg", "644", "22", "74", "30"},
        {"Laksa", "589", "22", "68", "26"},
        {"Roti Prata plain", "301", "8", "44", "11"},
        {"Mee Goreng", "660", "18", "92", "24"},
        {"Wanton Mee", "451", "20", "62", "14"},
        {"Bak Chor Mee", "467", "22", "60", "16"},
        {"Hokkien Mee", "560", "24", "72", "20"},
        {"Kaya Toast set", "370", "12", "52", "14"},
        {"Chicken Biryani", "680", "32", "86", "22"},
        {"Fish Head Curry", "420", "38", "12", "24"},
        {"Prawn Noodle Soup", "380", "24", "48", "10"},
        {"Bak Kut Teh", "310", "28", "6", "18"},
        {"Dim Sum Har Gow 3pc", "120", "8", "14", "4"},
        {"Char Siu Bao steamed", "150", "7", "22", "4"},
        {"Ice Kachang", "240", "3", "56", "2"},
        {"Chendol", "310", "3", "62", "6"},
        {"Teh Tarik", "140", "4", "24", "4"},
        {"Milo", "163", "5", "28", "4"},
        {"Kopi O", "15", "0", "3", "0"},
        {"Kopi C", "80", "3", "10", "3"},
        {"Satay chicken", "62", "7", "2", "3"},
        {"Satay beef", "68", "6", "3", "4"},
        {"Rojak", "280", "6", "38", "12"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        dbHelper = FoodDbHelper.getInstance(this);
        currentDate = getIntent().getStringExtra("date");
        if (currentDate == null)
            currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

        ivPreview = findViewById(R.id.ivPreview);
        tvAIStatus = findViewById(R.id.tvAIStatus);
        tvFoodName = findViewById(R.id.tvFoodName);
        tvCalResult = findViewById(R.id.tvCalResult);
        tvMacroResult = findViewById(R.id.tvMacroResult);
        llResult = findViewById(R.id.llResult);
        btnAddToLog = findViewById(R.id.btnAddToLog);
        btnAIToggle = findViewById(R.id.btnAIToggle);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String apiKey = prefs.getString("claude_api_key", "");
        aiEnabled = !apiKey.isEmpty();
        updateAIToggle();

        btnAIToggle.setOnClickListener(v -> {
            aiEnabled = !aiEnabled;
            updateAIToggle();
        });

        findViewById(R.id.btnCamera).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERM);
            } else {
                launchCamera();
            }
        });

        findViewById(R.id.btnGallery).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_GALLERY);
        });

        btnAddToLog.setOnClickListener(v -> addToLog());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void updateAIToggle() {
        btnAIToggle.setText(aiEnabled ? "AI: ON" : "AI: OFF");
        btnAIToggle.setBackgroundColor(aiEnabled ? 0xFF1F0A33 : 0xFF141419);
        tvAIStatus.setText(aiEnabled ? "AI will identify food from photo" : "Manual food selection active");
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQ_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQ_CAMERA) {
            capturedBitmap = (Bitmap) data.getExtras().get("data");
        } else if (requestCode == REQ_GALLERY) {
            try {
                Uri uri = data.getData();
                capturedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (capturedBitmap != null) {
            ivPreview.setImageBitmap(capturedBitmap);
            if (aiEnabled) analyzeWithAI();
            else showManualFoodList();
        }
    }

    private void analyzeWithAI() {
        tvFoodName.setText("Analyzing...");
        llResult.setVisibility(View.VISIBLE);
        btnAddToLog.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String apiKey = prefs.getString("claude_api_key", "");

        // Convert bitmap to base64
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Bitmap scaled = Bitmap.createScaledBitmap(capturedBitmap,
                Math.min(capturedBitmap.getWidth(), 512),
                Math.min(capturedBitmap.getHeight(), 512), true);
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, bos);
        String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);

        new Thread(() -> {
            try {
                URL url = new URL("https://api.anthropic.com/v1/messages");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("anthropic-version", "2023-06-01");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject imageSource = new JSONObject();
                imageSource.put("type", "base64");
                imageSource.put("media_type", "image/jpeg");
                imageSource.put("data", b64);

                JSONObject imageBlock = new JSONObject();
                imageBlock.put("type", "image");
                imageBlock.put("source", imageSource);

                JSONObject textBlock = new JSONObject();
                textBlock.put("type", "text");
                textBlock.put("text",
                    "Identify this food and estimate its nutritional content. " +
                    "Reply in EXACTLY this format:\n" +
                    "FOOD: [food name]\n" +
                    "CALORIES: [number]\n" +
                    "PROTEIN: [number]g\n" +
                    "CARBS: [number]g\n" +
                    "FAT: [number]g\n" +
                    "Only reply with these 5 lines, nothing else.");

                JSONArray content = new JSONArray();
                content.put(imageBlock);
                content.put(textBlock);

                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", content);

                JSONArray messages = new JSONArray();
                messages.put(msg);

                JSONObject body = new JSONObject();
                body.put("model", "claude-haiku-4-5-20251001");
                body.put("max_tokens", 128);
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
                parseAIResponse(text);

            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvFoodName.setText("AI error - try manual");
                    showManualFoodList();
                });
            }
        }).start();
    }

    private void parseAIResponse(String text) {
        String foodName = "Unknown Food";
        int cal = 300; float p = 10, c = 30, f = 10;
        for (String line : text.split("\n")) {
            if (line.startsWith("FOOD:")) foodName = line.substring(5).trim();
            else if (line.startsWith("CALORIES:")) {
                try { cal = Integer.parseInt(line.substring(9).trim()); } catch (Exception ignored) {}
            }
            else if (line.startsWith("PROTEIN:")) {
                try { p = Float.parseFloat(line.substring(8).replace("g","").trim()); } catch (Exception ignored) {}
            }
            else if (line.startsWith("CARBS:")) {
                try { c = Float.parseFloat(line.substring(6).replace("g","").trim()); } catch (Exception ignored) {}
            }
            else if (line.startsWith("FAT:")) {
                try { f = Float.parseFloat(line.substring(4).replace("g","").trim()); } catch (Exception ignored) {}
            }
        }
        detectedFoodName = foodName; detectedCalories = cal;
        detectedProtein = p; detectedCarbs = c; detectedFat = f;
        final String fn = foodName; final int fc = cal;
        final float fp = p, fca = c, ff = f;
        runOnUiThread(() -> {
            tvFoodName.setText(fn);
            tvCalResult.setText(fc + " kcal");
            tvMacroResult.setText(String.format("P %.0fg  C %.0fg  F %.0fg", fp, fca, ff));
            llResult.setVisibility(View.VISIBLE);
            btnAddToLog.setVisibility(View.VISIBLE);
        });
    }

    private void showManualFoodList() {
        // Show first SG food as default
        if (SG_FOODS.length > 0) {
            detectedFoodName = SG_FOODS[0][0];
            detectedCalories = Integer.parseInt(SG_FOODS[0][1]);
            detectedProtein = Float.parseFloat(SG_FOODS[0][2]);
            detectedCarbs = Float.parseFloat(SG_FOODS[0][3]);
            detectedFat = Float.parseFloat(SG_FOODS[0][4]);
        }
        runOnUiThread(() -> {
            tvFoodName.setText(detectedFoodName + " (manual)");
            tvCalResult.setText(detectedCalories + " kcal");
            tvMacroResult.setText(String.format("P %.0fg  C %.0fg  F %.0fg",
                    detectedProtein, detectedCarbs, detectedFat));
            llResult.setVisibility(View.VISIBLE);
            btnAddToLog.setVisibility(View.VISIBLE);
        });
    }

    private void addToLog() {
        FoodEntry entry = new FoodEntry(currentDate, "Snack", detectedFoodName,
                detectedCalories, detectedProtein, detectedCarbs, detectedFat, "ai");
        dbHelper.insertFoodEntry(entry);
        Toast.makeText(this, detectedFoodName + " added!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERM && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        }
    }
}
