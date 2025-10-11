package com.example.wattway_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etOldPassword, etNewPassword, etConfirmPassword;
    private AppCompatButton btnResetPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnForgotPassword);

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(oldPass)) {
            etOldPassword.setError("Current password required");
            etOldPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPass)) {
            etNewPassword.setError("New password required");
            etNewPassword.requestFocus();
            return;
        }

        if (newPass.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords don't match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Resetting...");

        // First sign in the user to verify credentials
        mAuth.signInWithEmailAndPassword(email, oldPass).addOnCompleteListener(signInTask -> {
            if (signInTask.isSuccessful()) {
                // Now update the password
                AuthCredential credential = EmailAuthProvider.getCredential(email, oldPass);

                mAuth.getCurrentUser().reauthenticate(credential).addOnCompleteListener(reAuthTask -> {
                    if (reAuthTask.isSuccessful()) {
                        mAuth.getCurrentUser().updatePassword(newPass).addOnCompleteListener(updateTask -> {
                            btnResetPassword.setEnabled(true);
                            btnResetPassword.setText("RESET PASSWORD");

                            if (updateTask.isSuccessful()) {
                                Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Failed to change password", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        btnResetPassword.setEnabled(true);
                        btnResetPassword.setText("RESET PASSWORD");
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("RESET PASSWORD");
                Toast.makeText(this, "Email or password is incorrect", Toast.LENGTH_LONG).show();
            }
        });
    }
}