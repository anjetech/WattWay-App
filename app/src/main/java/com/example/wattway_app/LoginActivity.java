package com.example.wattway_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmailLogin, etPasswordLogin;
    private AppCompatButton btnLogin;
    private TextView tvGoRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmailLogin = findViewById(R.id.etEmailLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoRegister = findViewById(R.id.tvGoRegister);

        btnLogin.setOnClickListener(v -> signIn());
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void signIn() {
        String email = etEmailLogin.getText().toString().trim();
        String pass  = etPasswordLogin.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmailLogin.setError("Email required");
            etEmailLogin.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(pass))  {
            etPasswordLogin.setError("Password required");
            etPasswordLogin.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in…");

        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this, task -> {
            btnLogin.setEnabled(true);
            btnLogin.setText("Sign in");

            if (task.isSuccessful()) {
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                goToHome();
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToHome() {
        Intent i = new Intent(this, HomePageActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    // Optional: keep users signed in
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            goToHome();
        }
    }
}