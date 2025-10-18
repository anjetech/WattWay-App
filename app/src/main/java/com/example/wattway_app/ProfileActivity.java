package com.example.wattway_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView welcome, userEmail;
    private AppCompatButton signOutButton, favoriteStations, changePassword;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize views - MAKE SURE bottomNav is initialized
        welcome = findViewById(R.id.welcome);
        userEmail = findViewById(R.id.userEmail);
        signOutButton = findViewById(R.id.signOutButton);
        favoriteStations = findViewById(R.id.favoriteStations);
        changePassword = findViewById(R.id.changePassword);
        bottomNav = findViewById(R.id.bottomNav);

        // Set welcome text
        welcome.setText("Welcome 👋");

        // Set user email
        if (currentUser != null) {
            String email = currentUser.getEmail();
            userEmail.setText(email != null ? email : "Email not available");
        } else {
            userEmail.setText("Not signed in");
        }

        // Sign out button listener
        signOutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Favorite stations button listener
        favoriteStations.setOnClickListener(v -> {
            Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Change password button listener
        changePassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Setup bottom navigation AFTER initializing bottomNav
        setupBottomNav();
    }

    private void setupBottomNav() {
        if (bottomNav != null) {
            // Set the selected item FIRST
            bottomNav.setSelectedItemId(R.id.nav_profile);

            // Then set the listener
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_map) {
                    startActivity(new Intent(this, HomePageActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_stations) {
                    startActivity(new Intent(this, StationsActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_load) {
                    startActivity(new Intent(this, LoadsheddingActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_profile) {
                    // Already on profile, do nothing
                    return true;
                }
                return false;
            });
        } else {
            // Debug log to see if bottomNav is null
            android.util.Log.e("ProfileActivity", "bottomNav is null!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure the correct item is selected when returning to this activity
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        } else {
            // Try to find it again if it was null
            bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_profile);
            }
        }
    }
}