package com.s23010691.freshconnect.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
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

import com.s23010691.freshconnect.MainActivity;
import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.network.SupabaseClient;

/*
 * Login Activity
 * Handles the login screen UI and user interactions. Provides email/phone and password input, forgot password, and sign-up navigation.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmailPhone;
    private EditText etPassword;
    private ImageView ivPasswordToggle;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private TextView tvSignUp;

    // Tracks whether the password is currently visible
    private boolean isPasswordVisible = false;
    private SupabaseClient supabaseClient;

    /*
     * Initializes the activity, configures edge-to-edge rendering, and sets up UI components.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge rendering for immersive background
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_login);

        // Apply window insets so content doesn't hide behind system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        supabaseClient = new SupabaseClient();
        initViews();
        setupListeners();
    }

    /*
     * Binds all UI views to their respective fields.
     */
    private void initViews() {
        etEmailPhone = findViewById(R.id.etEmailPhone);
        etPassword = findViewById(R.id.etPassword);
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);
    }

    /*
     * Wires up click listeners for interactive elements.
     */
    private void setupListeners() {

        // Password visibility toggle
        ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());

        // Login button
        btnLogin.setOnClickListener(v -> handleLogin());

        // Forgot password navigation
        tvForgotPassword.setOnClickListener(v -> {
            // TODO: Navigate to Forgot Password screen
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
        });

        // Sign Up navigation
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    /*
     * Toggles password field between masked and visible text.
     */
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivPasswordToggle.setImageResource(R.drawable.ic_visibility_off);
        } else {
            // Show password
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivPasswordToggle.setImageResource(R.drawable.ic_visibility_on);
        }
        isPasswordVisible = !isPasswordVisible;

        // Keep cursor at the end after toggling
        etPassword.setSelection(etPassword.getText().length());
    }

    /*
     * Validates input fields and performs login.
     */
    private void handleLogin() {
        String emailPhone = etEmailPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Basic client-side validation
        if (emailPhone.isEmpty()) {
            etEmailPhone.setError("Please enter your email or phone number");
            etEmailPhone.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        supabaseClient.login(emailPhone, password, new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                btnLogin.setEnabled(true);
                btnLogin.setText("Log In");
            }
        });
    }
}
