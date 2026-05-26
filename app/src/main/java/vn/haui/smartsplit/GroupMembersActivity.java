package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.adapters.MemberAdapter;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.viewmodels.GroupMembersViewModel;

public class GroupMembersActivity extends BaseActivity {

    private RecyclerView rvMembers;
    private FloatingActionButton fabAddMember;
    private ProgressBar pbLoading; // Đổi tên biến để tránh trùng lặp/xung đột với BaseActivity
    private String groupId, currentUserId;
    private GroupMembersViewModel viewModel;
    private final List<User> memberList = new ArrayList<>();
    private MemberAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_members);

        currentUserId = FirebaseAuth.getInstance().getUid();
        groupId = getIntent().getStringExtra("GROUP_ID");
        viewModel = new ViewModelProvider(this).get(GroupMembersViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbarMembers);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_members));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvMembers = findViewById(R.id.rvMembers);
        fabAddMember = findViewById(R.id.fabAddMember);
        pbLoading = findViewById(R.id.progressBar); // Sử dụng ID từ XML

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        
        observeViewModel();
        viewModel.init(groupId);

        fabAddMember.setOnClickListener(v -> {
            Group currentGroup = viewModel.getGroup().getValue();
            if (currentGroup != null) {
                if (currentUserId != null && currentUserId.equals(currentGroup.getAdminId())) {
                    showAddMemberByEmailDialog();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.dialog_join_code_title))
                            .setMessage(getString(R.string.dialog_share_join_code_msg_format, currentGroup.getJoinCode()))
                            .setPositiveButton(getString(R.string.dialog_action_ok), null)
                            .show();
                }
            }
        });
    }

    private void observeViewModel() {
        viewModel.getGroup().observe(this, group -> {
            if (group != null) {
                invalidateOptionsMenu();
            }
        });

        viewModel.getMembers().observe(this, users -> {
            memberList.clear();
            memberList.addAll(users);
            updateUI();
        });

        viewModel.getActionSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Thao tác thành công!", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getDissolveSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, getString(R.string.toast_dissolve_group_success), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, HomeContainerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, getString(R.string.toast_error_prefix, err), Toast.LENGTH_SHORT).show();
                if ("Group not found".equals(err)) {
                    finish();
                }
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (pbLoading != null) {
                pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void updateUI() {
        Group currentGroup = viewModel.getGroup().getValue();
        if (currentGroup == null) return;

        adapter = new MemberAdapter(memberList, currentGroup.getAdminId(), currentUserId, new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onEditMember(User user) {
                if (currentUserId != null && currentUserId.equals(currentGroup.getAdminId())) {
                    showEditMemberRoleDialog(user);
                } else {
                    Toast.makeText(GroupMembersActivity.this, getString(R.string.toast_permission_admin_edit_only), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onRemoveMember(User user) {
                if (currentUserId != null && currentUserId.equals(currentGroup.getAdminId())) {
                    confirmRemove(user);
                } else {
                    Toast.makeText(GroupMembersActivity.this, getString(R.string.toast_permission_admin_remove_only), Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvMembers.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Group currentGroup = viewModel.getGroup().getValue();
        if (currentGroup != null && currentUserId != null && currentUserId.equals(currentGroup.getAdminId())) {
            MenuItem item = menu.add(Menu.NONE, 101, Menu.NONE, getString(R.string.menu_dissolve_group));
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 101) {
            confirmDissolveGroup();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDissolveGroup() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.menu_dissolve_group))
                .setMessage(getString(R.string.dialog_dissolve_group_msg))
                .setPositiveButton(getString(R.string.btn_dissolve), (dialog, which) -> viewModel.dissolveGroup())
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showAddMemberByEmailDialog() {
        final EditText etEmail = new EditText(this);
        etEmail.setHint(getString(R.string.hint_enter_gmail));
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etEmail.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_add_member_title))
                .setView(etEmail)
                .setPositiveButton(getString(R.string.dialog_action_add), (dialog, which) -> {
                    String email = etEmail.getText().toString().trim();
                    if (!email.isEmpty()) viewModel.addMember(email);
                })
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .show();
    }

    private void showEditMemberRoleDialog(User user) {
        Group currentGroup = viewModel.getGroup().getValue();
        if (currentGroup == null) return;

        if (user.getUid().equals(currentGroup.getAdminId())) {
            Toast.makeText(this, getString(R.string.toast_user_already_admin), Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_transfer_admin_title))
                .setMessage(getString(R.string.dialog_transfer_admin_msg_format, user.getName()))
                .setPositiveButton(getString(R.string.dialog_action_confirm_transfer), (dialog, which) -> viewModel.transferAdmin(user.getUid()))
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .show();
    }

    private void confirmRemove(User user) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_remove_member_title))
                .setMessage(getString(R.string.dialog_remove_member_msg_format, user.getName()))
                .setPositiveButton(getString(R.string.btn_remove), (dialog, which) -> viewModel.removeMember(user.getUid()))
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
