package vn.haui.smartsplit.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.CreateGroupActivity;
import vn.haui.smartsplit.GroupDetailsActivity;
import vn.haui.smartsplit.R;
import vn.haui.smartsplit.adapters.GroupAdapter;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.viewmodels.GroupsViewModel;

public class GroupsFragment extends Fragment {

    private RecyclerView rvGroups;
    private GroupAdapter adapter;
    private List<Group> groupList;
    private GroupsViewModel viewModel;
    private EditText etSearch;
    private View layoutEmpty;
    private TextView tvGroupCount;

    private AlertDialog joinDialog;
    private TextInputLayout tilJoinCodeDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(GroupsViewModel.class);

        rvGroups     = view.findViewById(R.id.rvGroups);
        etSearch     = view.findViewById(R.id.etSearchGroups);
        layoutEmpty  = view.findViewById(R.id.layoutEmpty);
        tvGroupCount = view.findViewById(R.id.tvGroupCount);

        MaterialButton btnCreate = view.findViewById(R.id.btnCreateGroup);
        MaterialButton btnJoin   = view.findViewById(R.id.btnJoinGroup);

        setupUI(btnCreate, btnJoin);
        observeViewModel();

        viewModel.loadGroups();
    }

    private void setupUI(MaterialButton btnCreate, MaterialButton btnJoin) {
        rvGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupList = new ArrayList<>();
        adapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(requireContext(), GroupDetailsActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            startActivity(intent);
        });
        rvGroups.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
                updateEmptyState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (btnCreate != null) btnCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateGroupActivity.class)));
        if (btnJoin != null) btnJoin.setOnClickListener(v -> showJoinGroupDialog());
    }

    private void observeViewModel() {
        viewModel.getGroups().observe(getViewLifecycleOwner(), groups -> {
            groupList.clear();
            groupList.addAll(groups);
            adapter.updateFullList(groupList);
            
            String query = etSearch != null ? etSearch.getText().toString() : "";
            adapter.getFilter().filter(query);
            
            updateGroupCount();
            updateEmptyState();
        });

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

    private void updateGroupCount() {
        if (tvGroupCount != null) {
            tvGroupCount.setText(getString(R.string.group_count_format, groupList.size()));
        }
    }

    private void updateEmptyState() {
        if (rvGroups == null || layoutEmpty == null) return;
        rvGroups.post(() -> {
            boolean empty = adapter.getItemCount() == 0;
            layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvGroups.setVisibility(empty ? View.GONE : View.VISIBLE);
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
