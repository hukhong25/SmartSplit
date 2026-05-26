package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import vn.haui.smartsplit.adapters.BalanceAdapter;
import vn.haui.smartsplit.adapters.GroupExpenseAdapter;
import vn.haui.smartsplit.models.AppNotification;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.utils.ReportUtils;
import vn.haui.smartsplit.viewmodels.GroupDetailsViewModel;

public class GroupDetailsActivity extends BaseActivity implements
        BalanceAdapter.OnActionClickListener,
        GroupExpenseAdapter.OnExpenseActionListener {

    private RecyclerView rvGroupContent;
    private TabLayout tabLayoutGroup;
    private FloatingActionButton fabAddGroupExpense;
    private String groupId, groupName;
    private GroupDetailsViewModel viewModel;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        groupId = getIntent().getStringExtra("GROUP_ID");
        groupName = getIntent().getStringExtra("GROUP_NAME");

        viewModel = new ViewModelProvider(this).get(GroupDetailsViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbarGroupDetails);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(groupName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvGroupContent = findViewById(R.id.rvGroupContent);
        tabLayoutGroup = findViewById(R.id.tabLayoutGroup);
        fabAddGroupExpense = findViewById(R.id.fabAddGroupExpense);

        rvGroupContent.setLayoutManager(new LinearLayoutManager(this));

        setupTabs();
        observeViewModel();

        viewModel.init(groupId);
    }

    private void setupTabs() {
        tabLayoutGroup.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateTabUI();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        updateTabUI();
    }

    private void updateTabUI() {
        if (currentTab == 0) {
            fabAddGroupExpense.setImageResource(android.R.drawable.ic_input_add);
            fabAddGroupExpense.setOnClickListener(v -> openAddExpense());
            renderExpenses(viewModel.getExpenses().getValue());
        } else {
            fabAddGroupExpense.setImageResource(android.R.drawable.ic_menu_send);
            fabAddGroupExpense.setOnClickListener(v -> openSettleUp());
            renderBalances(viewModel.getMembers().getValue(), viewModel.getBalances().getValue());
        }
    }

    private void observeViewModel() {
        viewModel.getGroup().observe(this, group -> {
            if (group != null && getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(getString(R.string.group_subtitle_code_prefix, group.getJoinCode()));
            }
        });

        viewModel.getExpenses().observe(this, expenses -> {
            if (currentTab == 0) renderExpenses(expenses);
        });

        viewModel.getBalances().observe(this, balMap -> {
            if (currentTab == 1) renderBalances(viewModel.getMembers().getValue(), balMap);
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                if ("Group not found".equals(error)) {
                    finish();
                }
            }
        });
    }

    private void renderExpenses(List<Expense> expenses) {
        if (expenses == null) return;
        GroupExpenseAdapter adapter = new GroupExpenseAdapter(new ArrayList<>(expenses), this);
        rvGroupContent.setAdapter(adapter);
    }

    private void renderBalances(List<User> members, Map<String, Double> balances) {
        if (members == null || balances == null) return;
        BalanceAdapter adapter = new BalanceAdapter(members, balances);
        adapter.setActionListener(this);
        rvGroupContent.setAdapter(adapter);
    }

    @Override
    public void onExpenseClick(Expense expense) {
        Intent intent = new Intent(this, SettleDetailActivity.class);
        intent.putExtra("EXPENSE_ID", expense.getId());
        startActivity(intent);
    }

    @Override
    public void onExpenseEdit(Expense expense) {
        if (expense.isSettlement()) {
            Toast.makeText(this, getString(R.string.toast_cannot_edit_settlement), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, AddGroupExpenseActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        intent.putExtra("EXPENSE_ID", expense.getId());
        startActivity(intent);
    }

    @Override
    public void onExpenseDelete(Expense expense) {
        if (expense.isSettlement()) {
            Toast.makeText(this, getString(R.string.toast_cannot_delete_settlement), Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_confirm_delete_title))
                .setMessage(getString(R.string.dialog_confirm_delete_expense_msg))
                .setPositiveButton(getString(R.string.dialog_action_agree), (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("expenses").document(expense.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, getString(R.string.toast_delete_success), Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.toast_error_prefix, e.getMessage()), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .show();
    }

    private void openAddExpense() {
        Intent intent = new Intent(this, AddGroupExpenseActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        startActivity(intent);
    }

    private void openSettleUp() {
        Intent intent = new Intent(this, SettleUpActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        startActivity(intent);
    }

    @Override
    public void onSettleUp(User user, double balance) {
        Intent intent = new Intent(this, SettleUpActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        intent.putExtra("TARGET_USER_ID", user.getUid());
        intent.putExtra("AMOUNT", Math.abs(balance));
        startActivity(intent);
    }

    @Override
    public void onRemind(User user, double balance) {
        String currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (currentUserName == null || currentUserName.isEmpty()) {
            currentUserName = getString(R.string.default_member_name);
        }

        String title = getString(R.string.notif_title_debt_remind);
        String content = getString(R.string.notif_content_debt_remind_format, currentUserName, (long) Math.abs(balance), groupName);

        String notifId = FirebaseFirestore.getInstance().collection("notifications").document().getId();
        AppNotification notif = new AppNotification(notifId, user.getUid(), title, content, "REMIND", groupId);
        
        FirebaseFirestore.getInstance().collection("notifications").document(notifId).set(notif)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, getString(R.string.toast_remind_sent_success, user.getName()), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSettleTransaction(BalanceAdapter.Transaction transaction) {
        onSettleUp(new User(transaction.toId, transaction.toName, ""), transaction.amount);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) { finish(); return true; }
        if (id == R.id.action_invite) { shareInviteCode(); return true; }
        if (id == R.id.action_manage_members) {
            Intent intent = new Intent(this, GroupMembersActivity.class);
            intent.putExtra("GROUP_ID", groupId);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_export_report) {
            exportReport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareInviteCode() {
        if (viewModel.getGroup().getValue() == null) return;
        String code = viewModel.getGroup().getValue().getJoinCode();
        String message = getString(R.string.share_invite_msg_format, groupName, code);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)));
    }

    private void exportReport() {
        ReportUtils.exportGroupReportToCSV(
                this,
                viewModel.getGroup().getValue(),
                viewModel.getExpenses().getValue(),
                viewModel.getMembers().getValue(),
                viewModel.getBalances().getValue()
        );
    }
}
