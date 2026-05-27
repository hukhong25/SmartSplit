package vn.haui.smartsplit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.adapters.MemberAdapter;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.viewmodels.CreateGroupViewModel;

public class CreateGroupActivity extends BaseActivity {

    private TextInputLayout tilGroupName, tilMemberEmail;
    private EditText etGroupName, etMemberEmail;
    private Button btnCreateGroup, btnAddMember;
    private RecyclerView rvAddedMembers;
    private ProgressBar progressBar;
    private MemberAdapter memberAdapter;
    private List<User> addedMembersList = new ArrayList<>();
    private CreateGroupViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        viewModel = new ViewModelProvider(this).get(CreateGroupViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.create_group_title);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        tilGroupName = findViewById(R.id.tilGroupName);
        tilMemberEmail = findViewById(R.id.tilMemberEmail);
        etGroupName = findViewById(R.id.etGroupName);
        etMemberEmail = findViewById(R.id.etMemberEmail);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnAddMember = findViewById(R.id.btnAddMember);
        rvAddedMembers = findViewById(R.id.rvAddedMembers);
        progressBar = findViewById(R.id.progressBar);

        setupRecyclerView();
        setupValidation();
        observeViewModel();

        btnAddMember.setOnClickListener(v -> {
            String email = etMemberEmail.getText().toString().trim();
            if (validateMemberEmail(email)) {
                viewModel.addMemberByEmail(email);
            }
        });

        btnCreateGroup.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) {
                tilGroupName.setError(getString(R.string.toast_missing_group_name));
            } else {
                viewModel.createGroup(groupName);
            }
        });
    }

    private void setupValidation() {
        etGroupName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tilGroupName.setError(null);
            }
        });

        etMemberEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tilMemberEmail.setError(null);
            }
        });
    }

    private boolean validateMemberEmail(String email) {
        if (email.isEmpty()) {
            tilMemberEmail.setError(getString(R.string.toast_missing_email));
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilMemberEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        
        // Kiểm tra xem đã có trong danh sách chưa
        for (User u : addedMembersList) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                tilMemberEmail.setError(getString(R.string.toast_user_already_in_list));
                return false;
            }
        }

        // Kiểm tra có phải chính mình không
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (myEmail != null && myEmail.equalsIgnoreCase(email)) {
            tilMemberEmail.setError(getString(R.string.toast_already_member));
            return false;
        }

        return true;
    }

    private void setupRecyclerView() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        memberAdapter = new MemberAdapter(addedMembersList, currentUserId, currentUserId, new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onEditMember(User user) {
                Toast.makeText(CreateGroupActivity.this, R.string.toast_cannot_edit_role_on_create, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemoveMember(User user) {
                viewModel.removeMember(user);
            }
        });
        
        rvAddedMembers.setLayoutManager(new LinearLayoutManager(this));
        rvAddedMembers.setAdapter(memberAdapter);
    }

    private void observeViewModel() {
        viewModel.getAddedMembers().observe(this, users -> {
            addedMembersList.clear();
            addedMembersList.addAll(users);
            memberAdapter.notifyDataSetChanged();
            etMemberEmail.setText("");
            tilMemberEmail.setError(null);
        });

        viewModel.getCreateSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, R.string.toast_create_group_success, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                if (err.contains("email") || err.contains("user")) {
                    tilMemberEmail.setError(err);
                } else {
                    tilGroupName.setError(err);
                }
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnCreateGroup.setEnabled(!loading);
            btnAddMember.setEnabled(!loading);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
