package vn.haui.smartsplit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.haui.smartsplit.adapters.SplitMemberAdapter;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.utils.ImageUtils;
import vn.haui.smartsplit.viewmodels.AddGroupExpenseViewModel;

public class AddGroupExpenseActivity extends BaseActivity {

    private TextInputLayout tilExpenseDescription, tilExpenseAmount;
    private EditText etExpenseDescription, etExpenseAmount;
    private Spinner spPayer;
    private CheckBox cbSelectAll;
    private ChipGroup cgCategories;
    private RecyclerView rvSplitMembers;
    private TextView tvSplitError;
    private Button btnSaveGroupExpense, btnPickImage;
    private ImageView ivProofPreview;
    private ProgressBar pbLoading;

    private AddGroupExpenseViewModel viewModel;
    private String groupId;
    private String expenseId;
    private boolean isEditMode = false;
    private Uri imageUri;

    private final List<User> memberList = new ArrayList<>();
    private final List<String> selectedUserIds = new ArrayList<>();
    private SplitMemberAdapter splitMemberAdapter;

    private String currentAmount = "";

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivProofPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(imageUri).into(ivProofPreview);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_expense);

        groupId = getIntent().getStringExtra("GROUP_ID");
        expenseId = getIntent().getStringExtra("EXPENSE_ID");
        isEditMode = (expenseId != null);

        viewModel = new ViewModelProvider(this).get(AddGroupExpenseViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbarAddExpense);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String title = isEditMode ? getString(R.string.title_edit_expense) : getString(R.string.title_add_expense);
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tilExpenseDescription = findViewById(R.id.tilExpenseDescription);
        tilExpenseAmount = findViewById(R.id.tilExpenseAmount);
        etExpenseDescription = findViewById(R.id.etExpenseDescription);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        spPayer = findViewById(R.id.spPayer);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        cgCategories = findViewById(R.id.cgCategories);
        rvSplitMembers = findViewById(R.id.rvSplitMembers);
        tvSplitError = findViewById(R.id.tvSplitError);
        btnSaveGroupExpense = findViewById(R.id.btnSaveGroupExpense);
        btnPickImage = findViewById(R.id.btnPickImage);
        ivProofPreview = findViewById(R.id.ivProofPreview);
        pbLoading = findViewById(R.id.progressBar);

        if (isEditMode) {
            btnSaveGroupExpense.setText(getString(R.string.btn_update));
        }

        rvSplitMembers.setLayoutManager(new LinearLayoutManager(this));
        splitMemberAdapter = new SplitMemberAdapter(memberList, selectedUserIds);
        rvSplitMembers.setAdapter(splitMemberAdapter);

        setupListeners();
        observeViewModel();

        viewModel.loadGroupMembers(groupId);
        if (isEditMode) {
            viewModel.loadExpense(expenseId);
        }
    }

    private void setupListeners() {
        cgCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            String currentDesc = etExpenseDescription.getText().toString().trim();
            if (currentDesc.isEmpty()) {
                Chip chip = findViewById(id);
                etExpenseDescription.setText(chip.getText().toString());
            }
        });

        etExpenseDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                tilExpenseDescription.setError(null);
            }
        });

        etExpenseAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tilExpenseAmount.setError(null);
                if (!s.toString().equals(currentAmount)) {
                    etExpenseAmount.removeTextChangedListener(this);
                    String cleanString = s.toString().replaceAll("[.,]", "");
                    if (!cleanString.isEmpty()) {
                        try {
                            double parsed = Double.parseDouble(cleanString);
                            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.GERMANY);
                            formatter.applyPattern("#,###,###,###");
                            String formatted = formatter.format(parsed);
                            currentAmount = formatted;
                            etExpenseAmount.setText(formatted);
                            etExpenseAmount.setSelection(formatted.length());
                        } catch (Exception ignored) {}
                    } else {
                        currentAmount = "";
                    }
                    etExpenseAmount.addTextChangedListener(this);
                }
            }
        });

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        cbSelectAll.setOnClickListener(v -> {
            boolean checked = cbSelectAll.isChecked();
            selectedUserIds.clear();
            if (checked) {
                for (User user : memberList) {
                    selectedUserIds.add(user.getUid());
                }
            }
            tvSplitError.setVisibility(View.GONE);
            splitMemberAdapter.notifyDataSetChanged();
        });

        btnSaveGroupExpense.setOnClickListener(v -> saveExpense());
    }

    private void observeViewModel() {
        viewModel.getMemberList().observe(this, users -> {
            memberList.clear();
            memberList.addAll(users);
            updatePayerSpinner();
            if (!isEditMode && selectedUserIds.isEmpty()) {
                for (User u : users) selectedUserIds.add(u.getUid());
                cbSelectAll.setChecked(true);
            } else if (isEditMode) {
                cbSelectAll.setChecked(selectedUserIds.size() == users.size() && users.size() > 0);
            }
            splitMemberAdapter.notifyDataSetChanged();
        });

        viewModel.getExpenseData().observe(this, expense -> {
            if (expense != null) {
                etExpenseDescription.setText(expense.getDescription());
                DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.GERMANY);
                formatter.applyPattern("#,###,###,###");
                String formatted = formatter.format(expense.getAmount());
                etExpenseAmount.setText(formatted);
                currentAmount = formatted;
                setCategoryChip(expense.getCategory());
                
                if (expense.getProofImageUrl() != null && !expense.getProofImageUrl().isEmpty()) {
                    ivProofPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(expense.getProofImageUrl()).into(ivProofPreview);
                }

                for (int i = 0; i < memberList.size(); i++) {
                    if (memberList.get(i).getUid().equals(expense.getPayerId())) {
                        spPayer.setSelection(i);
                        break;
                    }
                }
                selectedUserIds.clear();
                if (expense.getSplitDetails() != null) {
                    selectedUserIds.addAll(expense.getSplitDetails().keySet());
                }
                if (memberList.size() > 0) {
                    cbSelectAll.setChecked(selectedUserIds.size() == memberList.size());
                }
                splitMemberAdapter.notifyDataSetChanged();
            }
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (success) {
                String msg = isEditMode ? getString(R.string.toast_update_expense_success) : getString(R.string.toast_add_expense_success);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                tilExpenseDescription.setError(getString(R.string.toast_error_prefix, err));
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (pbLoading != null) pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSaveGroupExpense.setEnabled(!loading);
        });
    }

    private void setCategoryChip(String category) {
        if (category == null) return;
        switch (category.toUpperCase()) {
            case "FOOD": cgCategories.check(R.id.chipFood); break;
            case "TRAVEL": cgCategories.check(R.id.chipTravel); break;
            case "SHOPPING": cgCategories.check(R.id.chipShopping); break;
            case "ENTERTAINMENT": cgCategories.check(R.id.chipEntertainment); break;
            default: cgCategories.check(R.id.chipOther); break;
        }
    }

    private String getSelectedCategory() {
        int id = cgCategories.getCheckedChipId();
        if (id == R.id.chipFood) return "FOOD";
        if (id == R.id.chipTravel) return "TRAVEL";
        if (id == R.id.chipShopping) return "SHOPPING";
        if (id == R.id.chipEntertainment) return "ENTERTAINMENT";
        return "OTHER";
    }

    private void updatePayerSpinner() {
        List<String> names = new ArrayList<>();
        for (User u : memberList) names.add(u.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPayer.setAdapter(adapter);

        if (!isEditMode) {
            String currentUid = FirebaseAuth.getInstance().getUid();
            for (int i = 0; i < memberList.size(); i++) {
                if (memberList.get(i).getUid().equals(currentUid)) {
                    spPayer.setSelection(i);
                    break;
                }
            }
        }
    }

    private void saveExpense() {
        String desc = etExpenseDescription.getText().toString().trim();
        String amountStr = etExpenseAmount.getText().toString().trim().replaceAll("[.,]", "");
        boolean isValid = true;

        if (desc.isEmpty()) {
            tilExpenseDescription.setError(getString(R.string.toast_missing_info));
            isValid = false;
        }
        if (amountStr.isEmpty()) {
            tilExpenseAmount.setError(getString(R.string.toast_missing_info));
            isValid = false;
        }
        if (selectedUserIds.isEmpty()) {
            tvSplitError.setVisibility(View.VISIBLE);
            isValid = false;
        }

        if (!isValid) return;

        String base64Image = null;
        if (imageUri != null) {
            base64Image = ImageUtils.convertUriToBase64(getContentResolver(), imageUri, 600);
            if (base64Image == null) {
                Toast.makeText(this, getString(R.string.toast_image_processing_error), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            double amount = Double.parseDouble(amountStr);
            User payer = memberList.get(spPayer.getSelectedItemPosition());
            String id = isEditMode ? expenseId : FirebaseFirestore.getInstance().collection("expenses").document().getId();
            String category = getSelectedCategory();
            
            viewModel.saveExpense(id, desc, amount, payer, groupId, selectedUserIds, FirebaseAuth.getInstance().getUid(), category, base64Image,
                    getString(R.string.notif_title_new_expense),
                    getString(R.string.notif_content_new_expense_format));
        } catch (NumberFormatException e) {
            tilExpenseAmount.setError(getString(R.string.toast_invalid_amount));
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    public void updateSelectAllState() {
        if (cbSelectAll != null) {
            cbSelectAll.setChecked(selectedUserIds.size() == memberList.size() && !memberList.isEmpty());
        }
        if (!selectedUserIds.isEmpty()) {
            tvSplitError.setVisibility(View.GONE);
        }
    }
}
