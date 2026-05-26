package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import vn.haui.smartsplit.viewmodels.AuthViewModel;

public class RegisterActivity extends BaseActivity {

    private TextInputLayout tilDisplayName, tilEmail, tilPassword, tilConfirmPassword;
    private EditText etDisplayName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        tilDisplayName = findViewById(R.id.tilDisplayName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etDisplayName = findViewById(R.id.etDisplayName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        setupValidation();
        observeViewModel();

        btnRegister.setOnClickListener(v -> {
            if (validateAll()) {
                String name = etDisplayName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                viewModel.register(email, password, name);
            }
        });

        tvLogin.setOnClickListener(v -> finish());
    }

    private void setupValidation() {
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                validateEmail(s.toString().trim());
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                validatePassword(s.toString().trim());
            }
        });

        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                validateConfirmPassword(s.toString().trim());
            }
        });
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.toast_missing_info));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            return false;
        } else {
            tilEmail.setError(null);
            return true;
        }
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.toast_missing_info));
            return false;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_too_short));
            return false;
        } else {
            tilPassword.setError(null);
            return true;
        }
    }

    private boolean validateConfirmPassword(String confirmPassword) {
        String password = etPassword.getText().toString().trim();
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError(getString(R.string.toast_missing_info));
            return false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return false;
        } else {
            tilConfirmPassword.setError(null);
            return true;
        }
    }

    private boolean validateAll() {
        boolean isEmailValid = validateEmail(etEmail.getText().toString().trim());
        boolean isPasswordValid = validatePassword(etPassword.getText().toString().trim());
        boolean isConfirmValid = validateConfirmPassword(etConfirmPassword.getText().toString().trim());
        boolean isNameValid = !etDisplayName.getText().toString().trim().isEmpty();
        
        if (!isNameValid) tilDisplayName.setError(getString(R.string.toast_missing_info));
        else tilDisplayName.setError(null);

        return isEmailValid && isPasswordValid && isConfirmValid && isNameValid;
    }

    private void observeViewModel() {
        viewModel.getFirebaseUser().observe(this, user -> {
            if (user != null) {
                Toast.makeText(RegisterActivity.this, getString(R.string.toast_register_success), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, HomeContainerActivity.class));
                finishAffinity();
            }
        });

        viewModel.getError().observe(this, errMsg -> {
            if (errMsg != null) {
                Toast.makeText(RegisterActivity.this, getString(R.string.toast_register_failed_prefix, errMsg), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnRegister.setEnabled(!loading);
        });
    }
}
