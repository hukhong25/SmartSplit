package vn.haui.smartsplit.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.CreateGroupActivity;
import vn.haui.smartsplit.GroupDetailsActivity;
import vn.haui.smartsplit.NotificationsActivity;
import vn.haui.smartsplit.R;
import vn.haui.smartsplit.adapters.GroupAdapter;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.viewmodels.DashboardViewModel;

public class DashboardFragment extends Fragment {

    private RecyclerView rvDashboardGroups;
    private GroupAdapter groupAdapter;
    private List<Group> groupList;
    private DashboardViewModel viewModel;
    private TextView tvTotalOwe, tvTotalOwed, tvWelcome, tvSeeAllGroups;
    private ImageView ivNotification;
    private View viewNotifBadge;

    private AlertDialog joinDialog;
    private TextInputLayout tilJoinCodeDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        tvWelcome        = view.findViewById(R.id.tvWelcome);
        tvTotalOwe       = view.findViewById(R.id.tvTotalOwe);
        tvTotalOwed      = view.findViewById(R.id.tvTotalOwed);
        tvSeeAllGroups   = view.findViewById(R.id.tvSeeAllGroups);
        rvDashboardGroups = view.findViewById(R.id.rvDashboardGroups);
        ivNotification   = view.findViewById(R.id.ivNotification);
        viewNotifBadge   = view.findViewById(R.id.viewNotifBadge);

        setupUI();
        observeViewModel();

        viewModel.startListening();
        listenToNotifications();
    }

    private void setupUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            tvWelcome.setText(getString(R.string.dashboard_welcome_format, user.getDisplayName()));
        } else {
            tvWelcome.setText(R.string.default_username);
        }

        rvDashboardGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupList = new ArrayList<>();
        groupAdapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(requireContext(), GroupDetailsActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            startActivity(intent);
        });
        rvDashboardGroups.setAdapter(groupAdapter);

        tvSeeAllGroups.setOnClickListener(v -> {
            if (getActivity() instanceof vn.haui.smartsplit.HomeContainerActivity) {
                ((vn.haui.smartsplit.HomeContainerActivity) getActivity())
                        .navigateTo(R.id.nav_groups);
            }
        });

        ivNotification.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), NotificationsActivity.class));
        });

        View btnCreate = getView().findViewById(R.id.btnQuickCreateGroup);
        if (btnCreate != null) btnCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateGroupActivity.class)));

        View btnJoin = getView().findViewById(R.id.btnQuickJoinGroup);
        if (btnJoin != null) btnJoin.setOnClickListener(v -> showJoinGroupDialog());
    }

    private void observeViewModel() {
        viewModel.getGroups().observe(getViewLifecycleOwner(), groups -> {
            groupList.clear();
            groupList.addAll(groups);
            groupAdapter.notifyDataSetChanged();
        });

        DecimalFormat formatter = new DecimalFormat("#,###");
        viewModel.getTotalOwe().observe(getViewLifecycleOwner(), owe -> 
            tvTotalOwe.setText(getString(R.string.currency_with_unit_format, formatter.format(owe))));

        viewModel.getTotalOwed().observe(getViewLifecycleOwner(), owed -> 
            tvTotalOwed.setText(getString(R.string.currency_with_unit_format, formatter.format(owed))));

        viewModel.getJoinSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success && joinDialog != null && joinDialog.isShowing()) {
                joinDialog.dismiss();
                viewModel.resetJoinState();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errMsg -> {
            if (errMsg != null) {
                if (joinDialog != null && joinDialog.isShowing() && tilJoinCodeDialog != null) {
                    if ("INVALID_CODE".equals(errMsg)) {
                        tilJoinCodeDialog.setError(getString(R.string.toast_join_code_invalid));
                    } else if ("ALREADY_MEMBER".equals(errMsg)) {
                        tilJoinCodeDialog.setError(getString(R.string.toast_already_member));
                    } else {
                        tilJoinCodeDialog.setError(errMsg);
                    }
                } else {
                    Toast.makeText(requireContext(), errMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void listenToNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    viewNotifBadge.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void showJoinGroupDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_group, null);
        tilJoinCodeDialog = dialogView.findViewById(R.id.tilJoinCode);
        EditText etJoinCode = dialogView.findViewById(R.id.etJoinCode);

        joinDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_join_group_title)
                .setView(dialogView)
                .setPositiveButton(R.string.dialog_action_join, null)
                .setNegativeButton(R.string.dialog_action_cancel, (d, w) -> d.dismiss())
                .create();

        joinDialog.setOnShowListener(d -> {
            Button btnJoin = joinDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnJoin.setOnClickListener(v -> {
                String code = etJoinCode.getText().toString().trim().toUpperCase();
                if (code.isEmpty()) {
                    tilJoinCodeDialog.setError(getString(R.string.toast_missing_info));
                } else {
                    viewModel.joinGroup(code);
                }
            });
        });

        joinDialog.show();
    }
}
