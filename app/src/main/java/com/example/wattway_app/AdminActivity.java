package com.example.wattway_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    private TextView welcome;
    private AppCompatButton signOutButton, manageUsersButton, viewReportsButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        getWindow().setStatusBarColor(Color.parseColor("#1A8BE4"));

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        welcome = findViewById(R.id.welcome);
        signOutButton = findViewById(R.id.signOutButton);
        manageUsersButton = findViewById(R.id.manageUsersButton);
        viewReportsButton = findViewById(R.id.viewReportsButton);
        bottomNav = findViewById(R.id.bottomNav);

        welcome.setText("Welcome 👋");

        signOutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(AdminActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });


        loadBottomNavByRole();
    }

    private void loadBottomNavByRole() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String role = document.getString("role");
                bottomNav.getMenu().clear();

                if ("admin".equals(role)) {
                    bottomNav.inflateMenu(R.menu.menu_bottom_nav_admin);
                    bottomNav.setSelectedItemId(R.id.nav_admin);
                } else {
                    bottomNav.inflateMenu(R.menu.menu_bottom_nav_user);
                    bottomNav.setSelectedItemId(R.id.nav_profile);
                }

                setupBottomNav();
            }
        });
    }

    private void setupBottomNav() {
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
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_admin) {
                if (!(this instanceof AdminActivity)) {
                    startActivity(new Intent(this, AdminActivity.class));
                    overridePendingTransition(0, 0);
                }
                return true;
            }
            return false;
        });
    }
}
