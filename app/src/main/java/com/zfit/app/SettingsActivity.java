package com.zfit.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText etApiKey, etHeight, etWeight, etCalGoal, etProteinGoal, etCarbsGoal, etFatGoal;
    private static final String PREFS = "zfit_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etApiKey = findViewById(R.id.etApiKey);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        etCalGoal = findViewById(R.id.etCalGoal);
        etProteinGoal = findViewById(R.id.etProteinGoal);
        etCarbsGoal = findViewById(R.id.etCarbsGoal);
        etFatGoal = findViewById(R.id.etFatGoal);

        // Mask API key
        etApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        loadSettings();

        findViewById(R.id.btnSave).setOnClickListener(v -> saveSettings());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Toggle API key visibility
        findViewById(R.id.btnToggleApiKey).setOnClickListener(v -> {
            int type = etApiKey.getInputType();
            if ((type & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0) {
                etApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                etApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            }
            etApiKey.setSelection(etApiKey.getText().length());
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String apiKey = prefs.getString("claude_api_key", "");
        if (!apiKey.isEmpty()) etApiKey.setText(apiKey);
        int height = prefs.getInt("height_cm", 0);
        float weight = prefs.getFloat("weight_kg", 0);
        int calGoal = prefs.getInt("calorie_goal", 2000);
        int proteinGoal = prefs.getInt("protein_goal", 150);
        int carbsGoal = prefs.getInt("carbs_goal", 200);
        int fatGoal = prefs.getInt("fat_goal", 65);

        if (height > 0) etHeight.setText(String.valueOf(height));
        if (weight > 0) etWeight.setText(String.valueOf(weight));
        etCalGoal.setText(String.valueOf(calGoal));
        etProteinGoal.setText(String.valueOf(proteinGoal));
        etCarbsGoal.setText(String.valueOf(carbsGoal));
        etFatGoal.setText(String.valueOf(fatGoal));
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        String apiKey = etApiKey.getText().toString().trim();
        if (!apiKey.isEmpty()) editor.putString("claude_api_key", apiKey);
        try { editor.putInt("height_cm", Integer.parseInt(etHeight.getText().toString())); } catch (Exception ignored) {}
        try { editor.putFloat("weight_kg", Float.parseFloat(etWeight.getText().toString())); } catch (Exception ignored) {}
        try { editor.putInt("calorie_goal", Integer.parseInt(etCalGoal.getText().toString())); } catch (Exception ignored) {}
        try { editor.putInt("protein_goal", Integer.parseInt(etProteinGoal.getText().toString())); } catch (Exception ignored) {}
        try { editor.putInt("carbs_goal", Integer.parseInt(etCarbsGoal.getText().toString())); } catch (Exception ignored) {}
        try { editor.putInt("fat_goal", Integer.parseInt(etFatGoal.getText().toString())); } catch (Exception ignored) {}
        editor.apply();
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
