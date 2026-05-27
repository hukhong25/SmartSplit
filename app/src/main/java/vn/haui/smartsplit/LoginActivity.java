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

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import vn.haui.smartsplit.viewmodels.AuthViewModel;

public class LoginActivity extends BaseActivity {

    private TextInputLayout tilEmail, tilPassword;
    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister, tvGeneralError;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvGeneralError = findViewById(R.id.tvGeneralError);
        progressBar = findViewById(R.id.progressBar);

        setupValidation();
        observeViewModel();

        btnLogin.setOnClickListener(v -> {
            if (validateAll()) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                viewModel.login(email, password);
            }
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void setupValidation() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tilEmail.setError(null);
                tilPassword.setError(null);
                tvGeneralError.setVisibility(View.GONE);
            }
        };

        etEmail.addTextChangedListener(watcher);
        etPassword.addTextChangedListener(watcher);
    }

    private boolean validateAll() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean isValid = true;

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.toast_missing_info));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.toast_missing_info));
            isValid = false;
        }

        return isValid;
    }

    private void observeViewModel() {
        viewModel.getFirebaseUser().observe(this, user -> {
            if (user != null) {
                startActivity(new Intent(LoginActivity.this, HomeContainerActivity.class));
                finish();
            }
        });

        viewModel.getError().observe(this, errMsg -> {
            if (errMsg != null) {
                // Hiển thị thông báo lỗi chung ở dưới cùng
                tvGeneralError.setText(R.string.error_invalid_credentials);
                tvGeneralError.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnLogin.setEnabled(!loading);
        });
    }
}
