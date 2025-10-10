package com.example.wattway_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView userName, userEmail;
    private AppCompatButton signOutButton, favoriteStations, changePassword, editEmail;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getWindow().setStatusBarColor(Color.parseColor("#1A8BE4")); // Match your background

        // Firebase setup
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // UI references
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        signOutButton = findViewById(R.id.signOutButton);
        favoriteStations = findViewById(R.id.favoriteStations);
        changePassword = findViewById(R.id.changePassword);
        editEmail = findViewById(R.id.editEmail);

        // Display user info
        if (currentUser != null) {
            String name = currentUser.getDisplayName(); // May be null if not set
            String email = currentUser.getEmail();

            userName.setText(name != null ? name : "Username");
            userEmail.setText(email != null ? email : "Email not available");
        } else {
            userName.setText("Guest");
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
}
