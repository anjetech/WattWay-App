package com.example.wattway_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView welcome, userEmail;
    private AppCompatButton signOutButton, favoriteStations, changePassword, editEmail;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.getMenu().clear();                 // ensure a clean slate
        bottomNav.inflateMenu(R.menu.menu_bottom_nav);  // force the correct menu file
        getWindow().setStatusBarColor(Color.parseColor("#1A8BE4")); // Match your background
        setupBottomNav();
        // Firebase setup
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // UI references
        welcome = findViewById(R.id.welcome); // updated ID
        userEmail = findViewById(R.id.userEmail);
        signOutButton = findViewById(R.id.signOutButton);
        favoriteStations = findViewById(R.id.favoriteStations);
        changePassword = findViewById(R.id.changePassword);
        editEmail = findViewById(R.id.editEmail);

        // Display static welcome message and email
        welcome.setText("Welcome 👋");
        if (currentUser != null) {
            String email = currentUser.getEmail();
            userEmail.setText(email != null ? email : "Email not available");
        } else {
            userEmail.setText("Not signed in");
        }

        // Sign out logic
        signOutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Stub buttons (inactive for now)
        favoriteStations.setOnClickListener(v -> {
            Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        changePassword.setOnClickListener(v -> {
            Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        editEmail.setOnClickListener(v -> {
            Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener((MenuItem item) -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                startActivity(new Intent(this, HomePageActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_stations) {
                startActivity(new Intent(this, StationsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_load) {
                startActivity(new Intent(this, LoadsheddingActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}
