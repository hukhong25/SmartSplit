package vn.haui.smartsplit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import vn.haui.smartsplit.viewmodels.ChangePasswordViewModel;

public class ChangePasswordActivity extends BaseActivity {

    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmNewPassword;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private MaterialButton btnChangePassword, btnCancel;
    private ProgressBar progressBar;
    private ChangePasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        viewModel = new ViewModelProvider(this).get(ChangePasswordViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmNewPassword = findViewById(R.id.tilConfirmNewPassword);
        
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);

        setupValidation();
        observeViewModel();

        btnChangePassword.setOnClickListener(v -> {
            if (validateAll()) {
                String currentPass = etCurrentPassword.getText().toString().trim();
                String newPass = etNewPassword.getText().toString().trim();
                viewModel.changePassword(currentPass, newPass);
            }
        });

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    private void setupValidation() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tilCurrentPassword.setError(null);
                tilNewPassword.setError(null);
                tilConfirmNewPassword.setError(null);
            }
        };

        etCurrentPassword.addTextChangedListener(watcher);
        etNewPassword.addTextChangedListener(watcher);
        etConfirmNewPassword.addTextChangedListener(watcher);
    }

    private boolean validateAll() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmNewPassword.getText().toString().trim();
        boolean isValid = true;

        if (currentPass.isEmpty()) {
            tilCurrentPassword.setError(getString(R.string.toast_missing_info));
            isValid = false;
        }

        if (newPass.isEmpty()) {
            tilNewPassword.setError(getString(R.string.toast_missing_info));
            isValid = false;
        } else if (newPass.length() < 6) {
            tilNewPassword.setError(getString(R.string.error_password_too_short));
            isValid = false;
        }

        if (confirmPass.isEmpty()) {
            tilConfirmNewPassword.setError(getString(R.string.toast_missing_info));
            isValid = false;
        } else if (!newPass.equals(confirmPass)) {
            tilConfirmNewPassword.setError(getString(R.string.error_password_mismatch));
            isValid = false;
        }

        return isValid;
    }

    private void observeViewModel() {
        viewModel.getSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, getString(R.string.toast_change_password_success), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                if (err.equals("REAUTH_FAILED")) {
                    tilCurrentPassword.setError(getString(R.string.error_current_password_incorrect));
                } else {
                    tilNewPassword.setError(getString(R.string.toast_error_prefix, err));
                }
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnChangePassword.setEnabled(!loading);
        });
    }
}
