package com.example.wattway_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etFullName, etEmailLogin, etPasswordLogin;
    private ImageView ivToggleLoginPassword;
    private AppCompatButton btnLogin;
    private TextView tvGoRegister, tvForgotPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Link UI elements
        etFullName = findViewById(R.id.etFullName); // Added full name field
        etEmailLogin = findViewById(R.id.etEmailLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        ivToggleLoginPassword = findViewById(R.id.ivToggleLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoRegister = findViewById(R.id.tvGoRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Toggle password visibility
        ivToggleLoginPassword.setOnClickListener(v -> {
            if (etPasswordLogin.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etPasswordLogin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivToggleLoginPassword.setImageResource(R.drawable.eye);
            } else {
                etPasswordLogin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivToggleLoginPassword.setImageResource(R.drawable.eye);
            }
            etPasswordLogin.setSelection(etPasswordLogin.getText().length());
        });

        // Login button
        btnLogin.setOnClickListener(v -> signIn());

        // Navigation to Register
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> sendResetEmail());
    }

    private void signIn() {
        String fullName = etFullName.getText().toString().trim(); // optional
        String email    = etEmailLogin.getText().toString().trim();
        String pass     = etPasswordLogin.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmailLogin.setError("Email required");
            etEmailLogin.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            etPasswordLogin.setError("Password required");
            etPasswordLogin.requestFocus();
            return;
        }

        // 👇 Make captured vars effectively final
        final String nameToSend  = TextUtils.isEmpty(fullName) ? "User" : fullName;
        final String emailToSend = email;

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in…");

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(LoginActivity.this, task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("LOGIN");

                    if (task.isSuccessful()) {
                        goToHome(nameToSend, emailToSend);
                    } else {
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Login failed";
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }



    private void sendResetEmail() {
        String email = etEmailLogin.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            etEmailLogin.setError("Enter your email to reset");
            etEmailLogin.requestFocus();
            return;
        }
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(t ->
                Toast.makeText(this,
                        t.isSuccessful() ? "Password reset email sent" : "Could not send reset email",
                        Toast.LENGTH_SHORT).show());
    }

    private void goToHome(String fullName, String email) {
        Intent i = new Intent(LoginActivity.this, HomePageActivity.class);
        i.putExtra("fullName", fullName);
        i.putExtra("email", email);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Optional: You can skip name here or use stored name later
            goToHome("User", user.getEmail()); // Fallback name
        }
    }
}
