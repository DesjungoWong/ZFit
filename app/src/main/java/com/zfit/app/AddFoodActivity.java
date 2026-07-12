package com.zfit.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zfit.app.db.FoodDbHelper;
import com.zfit.app.models.FoodEntry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddFoodActivity extends AppCompatActivity {

    private EditText etSearch, etManualName, etManualCal, etManualProtein, etManualCarbs, etManualFat;
    private LinearLayout llSuggestions, llSelectedCard, llManualSection;
    private TextView tvSelFoodName, tvSelCal, tvSelMacro;
    private Button btnBreakfast, btnLunch, btnDinner, btnSnack;
    private String selectedMealType = "Breakfast";
    private String currentDate;
    private FoodDbHelper dbHelper;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Selected food
    private String selName = ""; private int selCal = 0;
    private float selP = 0, selC = 0, selF = 0;
    private int servingMultiplier = 1;

    private static final String[][] SG_FOODS = {
        {"Chicken Rice", "480", "28", "54", "16", "SG"},
        {"Char Kway Teow", "744", "16", "96", "32", "SG"},
        {"Nasi Lemak with egg", "644", "22", "74", "30", "SG"},
        {"Laksa", "589", "22", "68", "26", "SG"},
        {"Roti Prata plain", "301", "8", "44", "11", "SG"},
        {"Mee Goreng", "660", "18", "92", "24", "SG"},
        {"Wanton Mee", "451", "20", "62", "14", "SG"},
        {"Bak Chor Mee", "467", "22", "60", "16", "SG"},
        {"Hokkien Mee", "560", "24", "72", "20", "SG"},
        {"Satay beef per stick", "68", "6", "3", "4", "SG"},
        {"Satay chicken per stick", "62", "7", "2", "3", "SG"},
        {"Kaya Toast set", "370", "12", "52", "14", "SG"},
        {"Kopi O", "15", "0", "3", "0", "SG"},
        {"Kopi C", "80", "3", "10", "3", "SG"},
        {"Teh Tarik", "140", "4", "24", "4", "SG"},
        {"Milo", "163", "5", "28", "4", "SG"},
        {"Chicken Biryani", "680", "32", "86", "22", "SG"},
        {"Fish Head Curry", "420", "38", "12", "24", "SG"},
        {"Prawn Noodle Soup", "380", "24", "48", "10", "SG"},
        {"Bak Kut Teh", "310", "28", "6", "18", "SG"},
        {"Dim Sum Har Gow 3pc", "120", "8", "14", "4", "SG"},
        {"Char Siu Bao steamed", "150", "7", "22", "4", "SG"},
        {"Rojak", "280", "6", "38", "12", "SG"},
        {"Ice Kachang", "240", "3", "56", "2", "SG"},
        {"Chendol", "310", "3", "62", "6", "SG"},
        {"Caesar Salad", "320", "14", "18", "22", "Global"},
        {"Grilled Salmon 100g", "208", "20", "0", "13", "Global"},
        {"Brown Rice 100g", "111", "3", "23", "1", "Global"},
        {"White Rice 100g", "130", "3", "28", "0", "Global"},
        {"Egg white omelette", "120", "18", "2", "4", "Global"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);
        dbHelper = FoodDbHelper.getInstance(this);
        currentDate = getIntent().getStringExtra("date");
        if (currentDate == null)
            currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

        etSearch = findViewById(R.id.etSearch);
        llSuggestions = findViewById(R.id.llSuggestions);
        llSelectedCard = findViewById(R.id.llSelectedCard);
        llManualSection = findViewById(R.id.llManualSection);
        tvSelFoodName = findViewById(R.id.tvSelFoodName);
        tvSelCal = findViewById(R.id.tvSelCal);
        tvSelMacro = findViewById(R.id.tvSelMacro);
        etManualName = findViewById(R.id.etManualName);
        etManualCal = findViewById(R.id.etManualCal);
        etManualProtein = findViewById(R.id.etManualProtein);
        etManualCarbs = findViewById(R.id.etManualCarbs);
        etManualFat = findViewById(R.id.etManualFat);
        btnBreakfast = findViewById(R.id.btnBreakfast);
        btnLunch = findViewById(R.id.btnLunch);
        btnDinner = findViewById(R.id.btnDinner);
        btnSnack = findViewById(R.id.btnSnack);

        setupMealChips();
        setupSearch();
        setupStepper();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddToLog).setOnClickListener(v -> addToLog());
        findViewById(R.id.btnAddManual).setOnClickListener(v -> addManual());

        // Show SG foods by default
        showSuggestions(filterSGFoods(""));
    }

    private void setupMealChips() {
        View.OnClickListener chipClick = v -> {
            if (v == btnBreakfast) selectedMealType = "Breakfast";
            else if (v == btnLunch) selectedMealType = "Lunch";
            else if (v == btnDinner) selectedMealType = "Dinner";
            else selectedMealType = "Snack";
            updateChips();
        };
        btnBreakfast.setOnClickListener(chipClick);
        btnLunch.setOnClickListener(chipClick);
        btnDinner.setOnClickListener(chipClick);
        btnSnack.setOnClickListener(chipClick);
        updateChips();
    }

    private void updateChips() {
        int active = 0xFF1F0A33, inactive = 0xFF141419;
        int aText = 0xFFA100FF, iText = 0xFF9A9AA3;
        btnBreakfast.setBackgroundColor(selectedMealType.equals("Breakfast") ? active : inactive);
        btnBreakfast.setTextColor(selectedMealType.equals("Breakfast") ? aText : iText);
        btnLunch.setBackgroundColor(selectedMealType.equals("Lunch") ? active : inactive);
        btnLunch.setTextColor(selectedMealType.equals("Lunch") ? aText : iText);
        btnDinner.setBackgroundColor(selectedMealType.equals("Dinner") ? active : inactive);
        btnDinner.setTextColor(selectedMealType.equals("Dinner") ? aText : iText);
        btnSnack.setBackgroundColor(selectedMealType.equals("Snack") ? active : inactive);
        btnSnack.setTextColor(selectedMealType.equals("Snack") ? aText : iText);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch(s.toString().trim());
                handler.postDelayed(searchRunnable, 400);
            }
        });
    }

    private void performSearch(String query) {
        List<String[]> sgResults = filterSGFoods(query);
        showSuggestions(sgResults);
        if (!query.isEmpty()) fetchOpenFoodFacts(query, sgResults.size());
    }

    private List<String[]> filterSGFoods(String query) {
        List<String[]> results = new ArrayList<>();
        for (String[] food : SG_FOODS) {
            if (query.isEmpty() || food[0].toLowerCase().contains(query.toLowerCase()))
                results.add(food);
        }
        return results;
    }

    private void fetchOpenFoodFacts(String query, int existingCount) {
        new Thread(() -> {
            try {
                String urlStr = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=" +
                        java.net.URLEncoder.encode(query, "UTF-8") +
                        "&search_simple=1&action=process&json=1&page_size=8" +
                        "&fields=product_name,nutriments,countries_tags";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONObject json = new JSONObject(sb.toString());
                JSONArray products = json.optJSONArray("products");
                List<String[]> apiResults = new ArrayList<>();
                if (products != null) {
                    for (int i = 0; i < products.length(); i++) {
                        JSONObject p = products.getJSONObject(i);
                        String name = p.optString("product_name", "");
                        if (name.isEmpty()) continue;
                        JSONObject n = p.optJSONObject("nutriments");
                        if (n == null) continue;
                        int kcal = (int) n.optDouble("energy-kcal_100g", 0);
                        float prot = (float) n.optDouble("proteins_100g", 0);
                        float carb = (float) n.optDouble("carbohydrates_100g", 0);
                        float fat = (float) n.optDouble("fat_100g", 0);
                        apiResults.add(new String[]{name, String.valueOf(kcal),
                                String.valueOf(prot), String.valueOf(carb),
                                String.valueOf(fat), "Global", "api"});
                    }
                }
                runOnUiThread(() -> appendApiSuggestions(apiResults));
            } catch (Exception ignored) {}
        }).start();
    }

    private void appendApiSuggestions(List<String[]> results) {
        for (String[] food : results) {
            addSuggestionRow(food, true);
        }
    }

    private void showSuggestions(List<String[]> foods) {
        llSuggestions.removeAllViews();
        for (String[] food : foods) addSuggestionRow(food, false);
    }

    private void addSuggestionRow(String[] food, boolean isApi) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        row.setBackgroundColor(0xFF101013);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(food[0]);
        tvName.setTextColor(0xFFF4F4F6);
        tvName.setTextSize(14);

        LinearLayout tagRow = new LinearLayout(this);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.setPadding(0, 2, 0, 0);

        TextView tvOrigin = new TextView(this);
        tvOrigin.setText(food.length > 5 ? food[5] : "SG");
        tvOrigin.setTextColor(food.length > 5 && food[5].equals("SG") ? 0xFFD9B96A : 0xFF5C7AFF);
        tvOrigin.setTextSize(10);
        tvOrigin.setPadding(0, 0, 8, 0);

        if (isApi) {
            TextView tvApi = new TextView(this);
            tvApi.setText("Auto-fetched");
            tvApi.setTextColor(0xFF4FD1A0);
            tvApi.setTextSize(9);
            tagRow.addView(tvApi);
        }
        tagRow.addView(tvOrigin, 0);

        info.addView(tvName);
        info.addView(tagRow);

        TextView tvCal = new TextView(this);
        tvCal.setText(food[1] + " kcal");
        tvCal.setTextColor(0xFFC86BFF);
        tvCal.setTextSize(14);
        tvCal.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(info);
        row.addView(tvCal);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFF1A1A20);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 0, 0, 0);
        wrapper.addView(row);
        wrapper.addView(divider);

        wrapper.setOnClickListener(v -> selectFood(food));
        llSuggestions.addView(wrapper);
    }

    private void selectFood(String[] food) {
        selName = food[0];
        selCal = Integer.parseInt(food[1]);
        try { selP = Float.parseFloat(food[2]); } catch (Exception e) { selP = 0; }
        try { selC = Float.parseFloat(food[3]); } catch (Exception e) { selC = 0; }
        try { selF = Float.parseFloat(food[4]); } catch (Exception e) { selF = 0; }
        servingMultiplier = 1;
        updateSelectedCard();
    }

    private void updateSelectedCard() {
        tvSelFoodName.setText(selName);
        tvSelCal.setText((selCal * servingMultiplier) + " kcal");
        tvSelMacro.setText(String.format("P %.0fg  C %.0fg  F %.0fg",
                selP * servingMultiplier, selC * servingMultiplier, selF * servingMultiplier));
        llSelectedCard.setVisibility(View.VISIBLE);
    }

    private void setupStepper() {
        TextView tvServing = findViewById(R.id.tvServing);
        findViewById(R.id.btnServingMinus).setOnClickListener(v -> {
            if (servingMultiplier > 1) {
                servingMultiplier--;
                tvServing.setText(String.valueOf(servingMultiplier));
                if (!selName.isEmpty()) updateSelectedCard();
            }
        });
        findViewById(R.id.btnServingPlus).setOnClickListener(v -> {
            servingMultiplier++;
            tvServing.setText(String.valueOf(servingMultiplier));
            if (!selName.isEmpty()) updateSelectedCard();
        });
    }

    private void addToLog() {
        if (selName.isEmpty()) {
            Toast.makeText(this, "Select a food first", Toast.LENGTH_SHORT).show();
            return;
        }
        FoodEntry entry = new FoodEntry(currentDate, selectedMealType, selName,
                selCal * servingMultiplier, selP * servingMultiplier,
                selC * servingMultiplier, selF * servingMultiplier, "api");
        dbHelper.insertFoodEntry(entry);
        Toast.makeText(this, selName + " added!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void addManual() {
        String name = etManualName.getText().toString().trim();
        String calStr = etManualCal.getText().toString().trim();
        if (name.isEmpty() || calStr.isEmpty()) {
            Toast.makeText(this, "Name and calories required", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int cal = Integer.parseInt(calStr);
            float p = etManualProtein.getText().toString().isEmpty() ? 0 :
                    Float.parseFloat(etManualProtein.getText().toString());
            float c = etManualCarbs.getText().toString().isEmpty() ? 0 :
                    Float.parseFloat(etManualCarbs.getText().toString());
            float f = etManualFat.getText().toString().isEmpty() ? 0 :
                    Float.parseFloat(etManualFat.getText().toString());
            FoodEntry entry = new FoodEntry(currentDate, selectedMealType, name, cal, p, c, f, "manual");
            dbHelper.insertFoodEntry(entry);
            Toast.makeText(this, name + " added!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
}

