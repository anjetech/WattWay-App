package com.example.wattway_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LoadsheddingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loadshedding);

        setupBottomNav();

        // You can set data later like:
        // ((TextView) findViewById(R.id.tvSelectedArea)).setText("Your area");
        // ((Chip) findViewById(R.id.chipStage)).setText("Stage 3");
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_load);

        bottomNav.setOnItemSelectedListener((MenuItem item) -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                startActivity(new Intent(this, HomePageActivity.class));
                overridePendingTransition(0, 0); finish(); return true;
            } else if (id == R.id.nav_stations) {
                startActivity(new Intent(this, StationsActivity.class));
                overridePendingTransition(0, 0); finish(); return true;
            } else if (id == R.id.nav_load) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0); finish(); return true;
            }
            return false;
        });
    }
}
