package com.zfit.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
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

public class DashboardFragment extends Fragment {

    private CalorieRingView calorieRingView;
    private TextView tvDate, tvRemaining, tvBurned, tvNet;
    private TextView tvProteinVal, tvCarbsVal, tvFatVal;
    private ProgressBar pbProtein, pbCarbs, pbFat;
    private TextView tvWaterVal;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(View root, Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        dbHelper = FoodDbHelper.getInstance(requireContext());
        currentCal = Calendar.getInstance();
        currentDate = SDF_KEY.format(currentCal.getTime());

        calorieRingView = root.findViewById(R.id.calorieRingView);
        tvDate = root.findViewById(R.id.tvDate);
        tvRemaining = root.findViewById(R.id.tvRemaining);
        tvBurned = root.findViewById(R.id.tvBurned);
        tvNet = root.findViewById(R.id.tvNet);
        tvProteinVal = root.findViewById(R.id.tvProteinVal);
        tvCarbsVal = root.findViewById(R.id.tvCarbsVal);
        tvFatVal = root.findViewById(R.id.tvFatVal);
        pbProtein = root.findViewById(R.id.pbProtein);
        pbCarbs = root.findViewById(R.id.pbCarbs);
        pbFat = root.findViewById(R.id.pbFat);
        tvWaterVal = root.findViewById(R.id.tvWaterVal);
        rvFoodLog = root.findViewById(R.id.rvFoodLog);

        adapter = new FoodLogAdapter(foodEntries, entry -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Delete entry?")
                .setMessage("Remove " + entry.getFoodName() + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    dbHelper.deleteFoodEntry(entry.getId());
                    refreshUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
        rvFoodLog.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFoodLog.setAdapter(adapter);

        root.findViewById(R.id.btnPrevDay).setOnClickListener(v -> {
            currentCal.add(Calendar.DAY_OF_YEAR, -1);
            currentDate = SDF_KEY.format(currentCal.getTime());
            refreshUI();
        });
        root.findViewById(R.id.btnNextDay).setOnClickListener(v -> {
            currentCal.add(Calendar.DAY_OF_YEAR, 1);
            currentDate = SDF_KEY.format(currentCal.getTime());
            refreshUI();
        });
        root.findViewById(R.id.btnAddFoodFab).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), AddFoodActivity.class).putExtra("date", currentDate)));
        root.findViewById(R.id.btnWater200).setOnClickListener(v -> addWater(200));
        root.findViewById(R.id.btnWater500).setOnClickListener(v -> addWater(500));
        root.findViewById(R.id.btnSettings).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), SettingsActivity.class)));

        refreshUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUI();
    }

    private void addWater(int ml) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HealthRecord record = dbHelper.getHealthRecordForDate(currentDate);
                if (record == null) {
                    record = new HealthRecord(currentDate);
                }
                record.setWaterMl(record.getWaterMl() + ml);
                dbHelper.upsertHealthRecord(record);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshUI();
                        }
                    });
                }
            }
        }).start();
    }

    private void refreshUI() {
        if (tvDate == null) return;
        tvDate.setText(SDF_DISPLAY.format(currentCal.getTime()));
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
        final int calorieGoal = prefs.getInt("calorie_goal", 2000);
        final int proteinGoal = prefs.getInt("protein_goal", 150);
        final int carbsGoal = prefs.getInt("carbs_goal", 200);
        final int fatGoal = prefs.getInt("fat_goal", 65);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final int totalCal = dbHelper.getTotalCaloriesForDate(currentDate);
                final float[] macros = dbHelper.getMacrosForDate(currentDate);
                final List<FoodEntry> entries = dbHelper.getFoodEntriesForDate(currentDate);
                final HealthRecord healthRecord = dbHelper.getHealthRecordForDate(currentDate);
                final int burned = healthRecord != null ? healthRecord.getCaloriesBurned() : 0;
                final int waterMl = healthRecord != null ? healthRecord.getWaterMl() : 0;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            calorieRingView.setProgress(totalCal, calorieGoal);

                            int remaining = calorieGoal - totalCal + burned;
                            tvRemaining.setText(String.valueOf(remaining));
                            tvBurned.setText(String.valueOf(burned));
                            tvNet.setText(String.valueOf(totalCal - burned));

                            tvProteinVal.setText(String.format("%.0fg", macros[0]));
                            tvCarbsVal.setText(String.format("%.0fg", macros[1]));
                            tvFatVal.setText(String.format("%.0fg", macros[2]));

                            int pPct = proteinGoal > 0 ? Math.min((int)(macros[0] * 100 / proteinGoal), 100) : 0;
                            int cPct = carbsGoal > 0 ? Math.min((int)(macros[1] * 100 / carbsGoal), 100) : 0;
                            int fPct = fatGoal > 0 ? Math.min((int)(macros[2] * 100 / fatGoal), 100) : 0;
                            pbProtein.setProgress(pPct);
                            pbCarbs.setProgress(cPct);
                            pbFat.setProgress(fPct);

                            tvWaterVal.setText(waterMl + " ml");

                            foodEntries.clear();
                            foodEntries.addAll(entries);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        }).start();
    }
}