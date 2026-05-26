package vn.haui.smartsplit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.haui.smartsplit.adapters.SplitMemberAdapter;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.viewmodels.AddGroupExpenseViewModel;

public class AddGroupExpenseActivity extends BaseActivity {

    private EditText etExpenseDescription, etExpenseAmount;
    private Spinner spPayer;
    private CheckBox cbSelectAll;
    private ChipGroup cgCategories;
    private RecyclerView rvSplitMembers;
    private Button btnSaveGroupExpense;
    private ProgressBar pbLoading;

    private AddGroupExpenseViewModel viewModel;
    private String groupId;
    private String expenseId;
    private boolean isEditMode = false;

    private final List<User> memberList = new ArrayList<>();
    private final List<String> selectedUserIds = new ArrayList<>();
    private SplitMemberAdapter splitMemberAdapter;

    private String currentAmount = "";

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

        etExpenseDescription = findViewById(R.id.etExpenseDescription);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        spPayer = findViewById(R.id.spPayer);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        cgCategories = findViewById(R.id.cgCategories);
        rvSplitMembers = findViewById(R.id.rvSplitMembers);
        btnSaveGroupExpense = findViewById(R.id.btnSaveGroupExpense);
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
        // Category Selection - Auto fill description if empty
        cgCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            String currentDesc = etExpenseDescription.getText().toString().trim();
            
            // Nếu mô tả đang trống hoặc chỉ chứa tên danh mục cũ, cập nhật theo chip mới chọn
            if (currentDesc.isEmpty()) {
                Chip chip = findViewById(id);
                etExpenseDescription.setText(chip.getText().toString());
            }
        });

        // Currency Formatter
        etExpenseAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
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

        // Quick Split (Select All)
        cbSelectAll.setOnClickListener(v -> {
            boolean checked = cbSelectAll.isChecked();
            selectedUserIds.clear();
            if (checked) {
                for (User user : memberList) {
                    selectedUserIds.add(user.getUid());
                }
            }
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

                // Set Category Chip
                setCategoryChip(expense.getCategory());
                
                // Select Payer
                for (int i = 0; i < memberList.size(); i++) {
                    if (memberList.get(i).getUid().equals(expense.getPayerId())) {
                        spPayer.setSelection(i);
                        break;
                    }
                }

                // Select split members
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
                Toast.makeText(this, getString(R.string.toast_error_prefix, err), Toast.LENGTH_SHORT).show();
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

        if (desc.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_missing_info), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedUserIds.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_missing_split_members), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            User payer = memberList.get(spPayer.getSelectedItemPosition());
            String id = isEditMode ? expenseId : FirebaseFirestore.getInstance().collection("expenses").document().getId();
            String category = getSelectedCategory();
            
            viewModel.saveExpense(id, desc, amount, payer, groupId, selectedUserIds, FirebaseAuth.getInstance().getUid(), category,
                    getString(R.string.notif_title_new_expense),
                    getString(R.string.notif_content_new_expense_format));
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.toast_invalid_amount), Toast.LENGTH_SHORT).show();
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
    }
}
