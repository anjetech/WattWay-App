package com.example.wattway_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private TextView tvLogin;
    private ImageView ivTogglePassword, ivToggleConfirm;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private String fullName, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // Link UI elements
        fullNameEditText = findViewById(R.id.etFullName);
        emailEditText = findViewById(R.id.etEmail);
        passwordEditText = findViewById(R.id.etPassword);
        confirmPasswordEditText = findViewById(R.id.etConfirmPassword);
        registerButton = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirm = findViewById(R.id.ivToggleConfirm);

        // Toggle password visibility
        ivTogglePassword.setOnClickListener(v -> {
            togglePasswordVisibility(passwordEditText);
        });

        ivToggleConfirm.setOnClickListener(v -> {
            togglePasswordVisibility(confirmPasswordEditText);
        });

        // Handle "Already have an account? Sign In" click
        tvLogin.setOnClickListener(v -> {
            Toast.makeText(RegisterActivity.this, "Navigating to Login", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });

        // Register logic
        registerButton.setOnClickListener(v -> {
            fullName = fullNameEditText.getText().toString().trim();
            email    = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirm  = confirmPasswordEditText.getText().toString().trim();

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            registerButton.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        registerButton.setEnabled(true);

                        if (!task.isSuccessful()) {
                            Toast.makeText(this,
                                    "Registration failed: " +
                                            (task.getException() != null ? task.getException().getMessage() : ""),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Save user profile to Firebase Database
                        String userId = mAuth.getCurrentUser().getUid();
                        User user = new User(fullName, email);

                        mDatabase.child(userId).setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                    goToHome(fullName, email);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Saved auth, but failed to save profile: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    goToHome(fullName, email); // Still go to home
                                });
                    });
        });
    }

    private void togglePasswordVisibility(EditText editText) {
        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        editText.setSelection(editText.getText().length());
    }

    private void goToHome(String fullName, String email) {
        Intent i = new Intent(this, HomePageActivity.class);
        i.putExtra("fullName", fullName);
        i.putExtra("email", email);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        Toast.makeText(this, "Navigating to HomePageActivity", Toast.LENGTH_SHORT).show();
        startActivity(i);
        finish();
    }
}
