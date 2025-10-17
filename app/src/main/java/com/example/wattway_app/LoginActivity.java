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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmailLogin, etPasswordLogin;
    private AppCompatButton btnLogin;
    private TextView tvGoRegister;
    private ImageView ivToggleLoginPassword;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmailLogin = findViewById(R.id.etEmailLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoRegister = findViewById(R.id.tvGoRegister);
        ivToggleLoginPassword = findViewById(R.id.ivToggleLoginPassword);

        // Toggle password visibility
        ivToggleLoginPassword.setOnClickListener(v -> {
            togglePasswordVisibility(etPasswordLogin);
        });

        btnLogin.setOnClickListener(v -> signIn());
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void togglePasswordVisibility(EditText editText) {
        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        editText.setSelection(editText.getText().length());
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
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    checkUserRole(user.getUid());
                }
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkUserRole(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        if ("admin".equals(role)) {
                            goToAdmin();
                        } else {
                            goToHome();
                        }
                    } else {
                        Toast.makeText(this, "User role not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching role: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void goToHome() {
        Intent i = new Intent(this, HomePageActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private void goToAdmin() {
        Intent i = new Intent(this, AdminActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            checkUserRole(user.getUid());
        }
    }
}
