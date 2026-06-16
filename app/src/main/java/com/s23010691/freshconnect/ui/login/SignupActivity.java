package com.s23010691.freshconnect.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;
import android.widget.EditText;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.network.SupabaseClient;

/*
 * Signup Activity
 * Handles new user registration, capturing name, email, password, and terms agreement.
 */
public class SignupActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPassword;
    private ImageView ivPasswordToggle;
    private CheckBox cbTerms;
    private Button btnSignUp;
    private boolean isPasswordVisible = false;
    private SupabaseClient supabaseClient;

    /*
     * Initializes the activity, configures UI components, and sets up click listeners.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        supabaseClient = new SupabaseClient();

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle);
        cbTerms = findViewById(R.id.cbTerms);
        btnSignUp = findViewById(R.id.btnSignUp);

        // Password visibility toggle
        ivPasswordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivPasswordToggle.setImageResource(R.drawable.ic_visibility_off);
            } else {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivPasswordToggle.setImageResource(R.drawable.ic_visibility_on);
            }
            isPasswordVisible = !isPasswordVisible;
            etPassword.setSelection(etPassword.getText().length());
        });

        // "Log In" link -> go to LoginActivity
        findViewById(R.id.tvLogIn).setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });

        btnSignUp.setOnClickListener(v -> handleSignup());
    }

    /*
     * Validates registration fields and creates a new user account via Supabase.
     */
    private void handleSignup() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to Terms and Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignUp.setEnabled(false);
        btnSignUp.setText("Signing up...");

        supabaseClient.signUp(email, password, name, new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(SignupActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SignupActivity.this, error, Toast.LENGTH_LONG).show();
                btnSignUp.setEnabled(true);
                btnSignUp.setText("Sign Up");
            }
        });
    }
}
