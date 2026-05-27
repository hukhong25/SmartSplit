package vn.haui.smartsplit;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import vn.haui.smartsplit.utils.ImageUtils;
import vn.haui.smartsplit.viewmodels.ProfileViewModel;

public class EditProfileActivity extends BaseActivity {

    private TextInputLayout tilDisplayName;
    private TextInputEditText etDisplayName;
    private ImageView ivAvatar;
    private TextView tvAvatarInitial;
    private MaterialButton btnSave, btnCancel;
    private ProgressBar progressBar;
    private ProfileViewModel viewModel;

    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivAvatar.setImageURI(uri);
                    tvAvatarInitial.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tilDisplayName = findViewById(R.id.tilDisplayName);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        etDisplayName = findViewById(R.id.etDisplayName);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);
        View frameAvatar = findViewById(R.id.frameAvatar);

        setupValidation();
        observeViewModel();
        viewModel.loadUserProfile();

        frameAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void setupValidation() {
        etDisplayName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tilDisplayName.setError(null);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getUser().observe(this, user -> {
            if (user != null) {
                etDisplayName.setText(user.getName());
                if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                    ImageUtils.loadImage(this, user.getPhotoUrl(), ivAvatar, 0);
                    tvAvatarInitial.setVisibility(View.GONE);
                } else {
                    tvAvatarInitial.setVisibility(View.VISIBLE);
                    if (user.getName() != null && !user.getName().isEmpty()) {
                        tvAvatarInitial.setText(String.valueOf(user.getName().charAt(0)).toUpperCase());
                    }
                }
            }
        });

        viewModel.getUpdateSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, getString(R.string.toast_update_profile_success), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                tilDisplayName.setError(getString(R.string.toast_error_prefix, err));
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSave.setEnabled(!loading);
        });
    }

    private void saveProfile() {
        String newName = etDisplayName.getText().toString().trim();

        if (newName.isEmpty()) {
            tilDisplayName.setError(getString(R.string.error_empty_display_name));
            return;
        }

        String base64Image = null;
        if (selectedImageUri != null) {
            base64Image = ImageUtils.convertUriToBase64(getContentResolver(), selectedImageUri, 300);
            if (base64Image == null) {
                Toast.makeText(this, getString(R.string.toast_image_processing_error), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        viewModel.updateProfile(newName, base64Image);
    }
}
