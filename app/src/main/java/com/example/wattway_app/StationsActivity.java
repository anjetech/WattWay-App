package com.example.wattway_app;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;



public class StationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_stations);
        setupBottomNav();


    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_stations);

        bottomNav.setOnItemSelectedListener((MenuItem item) -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                startActivity(new Intent(this, HomePageActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            } else if (id == R.id.nav_stations) {
                return true;
            } else if (id == R.id.nav_load) {
                startActivity(new Intent(this, LoadsheddingActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            }
            return false;
        });
    }
}