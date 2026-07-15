package com.zfit.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private static final String PREFS = "de_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        setupBottomNav();

        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragmentContainer, fragment);
        ft.commit();
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new DashboardFragment());
                return true;
            } else if (id == R.id.nav_log) {
                startActivity(new Intent(this, AddFoodActivity.class));
                return false;
            } else if (id == R.id.nav_snap) {
                startActivity(new Intent(this, CameraActivity.class));
                return false;
            } else if (id == R.id.nav_health) {
                startActivity(new Intent(this, HealthDashboardActivity.class));
                return false;
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(this, WeeklyReportActivity.class));
                return false;
            }
            return false;
        });
    }
}