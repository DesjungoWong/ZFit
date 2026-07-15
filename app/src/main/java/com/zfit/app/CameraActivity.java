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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
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

    // Camera tab views
    private LinearLayout panelCamera;
    private ImageView ivCameraPreview;
    private LinearLayout llCameraResult;
    private TextView tvCameraFoodName, tvCameraKcal, tvCameraMacros;

    // Gallery tab views
    private LinearLayout panelGallery;
    private ImageView ivGalleryPreview;
    private LinearLayout llGalleryResult;
    private TextView tvGalleryFoodName, tvGalleryKcal, tvGalleryMacros;

    // Describe tab views
    private LinearLayout panelDescribe;
    private EditText etDescribePrompt;
    private LinearLayout llDescribeResult;
    private TextView tvDescribeFoodName, tvDescribeKcal, tvDescribeMacros;

    private int currentTab = 0; // 0=camera, 1=gallery, 2=describe
    private String currentDate;
    private FoodDbHelper dbHelper;
    private static final String PREFS = "de_prefs";

    // Detected food data per tab
    private String[] foodName = {"", "", ""};
    private int[] foodKcal = {0, 0, 0};
    private float[] foodProtein = {0, 0, 0};
    private float[] foodCarbs = {0, 0, 0};
    private float[] foodFat = {0, 0, 0};

    private static final String[][] SG_FOODS = {
        {"Chicken Rice", "480", "28", "54", "16"},
        {"Char Kway Teow", "744", "16", "96", "32"},
        {"Nasi Lemak with egg", "644", "22", "74", "30"},
        {"Laksa", "589", "22", "68", "26"},
        {"Roti Prata plain", "301", "8", "44", "11"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        dbHelper = FoodDbHelper.getInstance(this);
        currentDate = getIntent().getStringExtra("date");
        if (currentDate == null)
            currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

        panelCamera = findViewById(R.id.panelCamera);
        ivCameraPreview = findViewById(R.id.ivCameraPreview);
        llCameraResult = findViewById(R.id.llCameraResult);
        tvCameraFoodName = findViewById(R.id.tvCameraFoodName);
        tvCameraKcal = findViewById(R.id.tvCameraKcal);
        tvCameraMacros = findViewById(R.id.tvCameraMacros);

        panelGallery = findViewById(R.id.panelGallery);
        ivGalleryPreview = findViewById(R.id.ivGalleryPreview);
        llGalleryResult = findViewById(R.id.llGalleryResult);
        tvGalleryFoodName = findViewById(R.id.tvGalleryFoodName);
        tvGalleryKcal = findViewById(R.id.tvGalleryKcal);
        tvGalleryMacros = findViewById(R.id.tvGalleryMacros);

        panelDescribe = findViewById(R.id.panelDescribe);
        etDescribePrompt = findViewById(R.id.etDescribePrompt);
        llDescribeResult = findViewById(R.id.llDescribeResult);
        tvDescribeFoodName = findViewById(R.id.tvDescribeFoodName);
        tvDescribeKcal = findViewById(R.id.tvDescribeKcal);
        tvDescribeMacros = findViewById(R.id.tvDescribeMacros);

        TabLayout tabLayout = findViewById(R.id.snapTabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { switchTab(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Camera tab buttons
        findViewById(R.id.btnCameraCapture).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERM);
            } else {
                launchCamera();
            }
        });

        // Gallery tab buttons
        findViewById(R.id.btnGalleryPick).setOnClickListener(v -> launchGallery());
        findViewById(R.id.btnGalleryConfirm).setOnClickListener(v -> logFood(1));

        // Describe tab buttons
        findViewById(R.id.btnDescribeAnalyze).setOnClickListener(v -> analyzeDescription());
        findViewById(R.id.btnDescribeConfirm).setOnClickListener(v -> logFood(2));

        // Quick-tag chips append to prompt
        findViewById(R.id.chipHawker).setOnClickListener(v -> appendTag("hawker portion"));
        findViewById(R.id.chipRestaurant).setOnClickListener(v -> appendTag("restaurant serving"));
        findViewById(R.id.chipHomemade).setOnClickListener(v -> appendTag("homemade"));

        setupBottomNav();
        switchTab(0);
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        nav.setSelectedItemId(R.id.nav_snap);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.nav_log) {
                startActivity(new Intent(this, AddFoodActivity.class));
                finish();
            } else if (id == R.id.nav_health) {
                startActivity(new Intent(this, HealthDashboardActivity.class));
                finish();
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(this, WeeklyReportActivity.class));
                finish();
            }
            return false;
        });
    }

    private void switchTab(int tab) {
        currentTab = tab;
        panelCamera.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        panelGallery.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        panelDescribe.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
    }

    private void appendTag(String tag) {
        String current = etDescribePrompt.getText().toString().trim();
        etDescribePrompt.setText(current.isEmpty() ? tag : current + ", " + tag);
        etDescribePrompt.setSelection(etDescribePrompt.getText().length());
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQ_CAMERA);
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        Bitmap bitmap = null;
        if (requestCode == REQ_CAMERA) {
            bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) ivCameraPreview.setImageBitmap(bitmap);
        } else if (requestCode == REQ_GALLERY) {
            try {
                Uri uri = data.getData();
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                if (bitmap != null) {
                    ivGalleryPreview.setImageBitmap(bitmap);
                    TextView hint = (TextView) ((android.view.ViewGroup) ivGalleryPreview.getParent()).getChildAt(1);
                    if (hint != null) hint.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (bitmap != null) {
            final int tabIdx = (requestCode == REQ_CAMERA) ? 0 : 1;
            analyzeImageWithAI(bitmap, tabIdx);
        }
    }

    private void analyzeImageWithAI(Bitmap bmp, final int tabIdx) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String apiKey = prefs.getString("claude_api_key", "");
        if (apiKey.isEmpty()) apiKey = BuildConfig.CLAUDE_API_KEY;

        if (apiKey.isEmpty()) {
            useFallbackFood(tabIdx);
            return;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Bitmap scaled = Bitmap.createScaledBitmap(bmp,
            Math.min(bmp.getWidth(), 512), Math.min(bmp.getHeight(), 512), true);
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, bos);
        final String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        final String finalApiKey = apiKey;

        showResultLoading(tabIdx, "Analyzing...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.anthropic.com/v1/messages");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("x-api-key", finalApiKey);
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
                        "Identify this food and estimate calories and macros. " +
                        "Reply as JSON only: {\"food\":\"name\",\"kcal\":int,\"protein\":int,\"carbs\":int,\"fat\":int,\"confidence\":int}");

                    JSONArray content = new JSONArray();
                    content.put(imageBlock);
                    content.put(textBlock);

                    JSONObject msg = new JSONObject();
                    msg.put("role", "user");
                    msg.put("content", content);

                    JSONObject body = new JSONObject();
                    body.put("model", "claude-haiku-4-5-20251001");
                    body.put("max_tokens", 256);
                    body.put("messages", new JSONArray().put(msg));

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
                    // Extract JSON from response
                    int start = text.indexOf('{');
                    int end = text.lastIndexOf('}');
                    if (start >= 0 && end > start) {
                        JSONObject result = new JSONObject(text.substring(start, end + 1));
                        final String fn = result.optString("food", "Unknown");
                        final int fk = result.optInt("kcal", 300);
                        final int fp = result.optInt("protein", 10);
                        final int fc = result.optInt("carbs", 30);
                        final int ff = result.optInt("fat", 10);
                        runOnUiThread(new Runnable() {
                            @Override public void run() { updateResultCard(tabIdx, fn, fk, fp, fc, ff); }
                        });
                    } else {
                        runOnUiThread(new Runnable() { @Override public void run() { useFallbackFood(tabIdx); } });
                    }
                } catch (Exception e) {
                    runOnUiThread(new Runnable() { @Override public void run() { useFallbackFood(tabIdx); } });
                }
            }
        }).start();
    }

    private void analyzeDescription() {
        String text = etDescribePrompt.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter a food description first", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String apiKey = prefs.getString("claude_api_key", "");
        if (apiKey.isEmpty()) apiKey = BuildConfig.CLAUDE_API_KEY;

        if (apiKey.isEmpty()) {
            useFallbackFood(2);
            return;
        }
        final String finalApiKey = apiKey;
        final String prompt = "Estimate calories and macros for: " + text +
            ". Reply as JSON only: {\"food\":\"name\",\"kcal\":int,\"protein\":int,\"carbs\":int,\"fat\":int,\"confidence\":int}";

        showResultLoading(2, "Estimating...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.anthropic.com/v1/messages");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("x-api-key", finalApiKey);
                    conn.setRequestProperty("anthropic-version", "2023-06-01");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject msg = new JSONObject();
                    msg.put("role", "user");
                    msg.put("content", prompt);

                    JSONObject body = new JSONObject();
                    body.put("model", "claude-haiku-4-5-20251001");
                    body.put("max_tokens", 256);
                    body.put("messages", new JSONArray().put(msg));

                    OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(body.toString());
                    writer.flush();

                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    JSONObject resp = new JSONObject(sb.toString());
                    String responseText = resp.getJSONArray("content").getJSONObject(0).getString("text");
                    int start = responseText.indexOf('{');
                    int end = responseText.lastIndexOf('}');
                    if (start >= 0 && end > start) {
                        JSONObject result = new JSONObject(responseText.substring(start, end + 1));
                        final String fn = result.optString("food", text);
                        final int fk = result.optInt("kcal", 300);
                        final int fp = result.optInt("protein", 10);
                        final int fc = result.optInt("carbs", 30);
                        final int ff = result.optInt("fat", 10);
                        runOnUiThread(new Runnable() {
                            @Override public void run() { updateResultCard(2, fn, fk, fp, fc, ff); }
                        });
                    } else {
                        runOnUiThread(new Runnable() { @Override public void run() { useFallbackFood(2); } });
                    }
                } catch (Exception e) {
                    runOnUiThread(new Runnable() { @Override public void run() { useFallbackFood(2); } });
                }
            }
        }).start();
    }

    private void showResultLoading(int tabIdx, String msg) {
        if (tabIdx == 0) {
            llCameraResult.setVisibility(View.VISIBLE);
            tvCameraFoodName.setText(msg);
            tvCameraKcal.setText("");
            tvCameraMacros.setText("");
        } else if (tabIdx == 1) {
            llGalleryResult.setVisibility(View.VISIBLE);
            tvGalleryFoodName.setText(msg);
            tvGalleryKcal.setText("");
            tvGalleryMacros.setText("");
        } else {
            llDescribeResult.setVisibility(View.VISIBLE);
            tvDescribeFoodName.setText(msg);
            tvDescribeKcal.setText("");
            tvDescribeMacros.setText("");
        }
    }

    private void updateResultCard(int tabIdx, String fn, int fk, int fp, int fc, int ff) {
        foodName[tabIdx] = fn;
        foodKcal[tabIdx] = fk;
        foodProtein[tabIdx] = fp;
        foodCarbs[tabIdx] = fc;
        foodFat[tabIdx] = ff;

        String macros = String.format("P %dg  C %dg  F %dg", fp, fc, ff);
        if (tabIdx == 0) {
            llCameraResult.setVisibility(View.VISIBLE);
            tvCameraFoodName.setText(fn);
            tvCameraKcal.setText(fk + " kcal");
            tvCameraMacros.setText(macros);
        } else if (tabIdx == 1) {
            llGalleryResult.setVisibility(View.VISIBLE);
            tvGalleryFoodName.setText(fn);
            tvGalleryKcal.setText(fk + " kcal");
            tvGalleryMacros.setText(macros);
        } else {
            llDescribeResult.setVisibility(View.VISIBLE);
            tvDescribeFoodName.setText(fn);
            tvDescribeKcal.setText(fk + " kcal");
            tvDescribeMacros.setText(macros);
        }
    }

    private void useFallbackFood(int tabIdx) {
        if (SG_FOODS.length > 0) {
            String fn = SG_FOODS[0][0];
            int fk = Integer.parseInt(SG_FOODS[0][1]);
            int fp = Integer.parseInt(SG_FOODS[0][2]);
            int fc = Integer.parseInt(SG_FOODS[0][3]);
            int ff = Integer.parseInt(SG_FOODS[0][4]);
            updateResultCard(tabIdx, fn, fk, fp, fc, ff);
        }
    }

    private void logFood(int tabIdx) {
        if (foodName[tabIdx].isEmpty()) {
            Toast.makeText(this, "Analyze food first", Toast.LENGTH_SHORT).show();
            return;
        }
        FoodEntry entry = new FoodEntry(currentDate, "Snack", foodName[tabIdx],
            foodKcal[tabIdx], foodProtein[tabIdx], foodCarbs[tabIdx], foodFat[tabIdx], "ai");
        dbHelper.insertFoodEntry(entry);
        Toast.makeText(this, foodName[tabIdx] + " logged!", Toast.LENGTH_SHORT).show();
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