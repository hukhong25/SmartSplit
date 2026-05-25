package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.repositories.ExpenseRepository;
import vn.haui.smartsplit.repositories.GroupRepository;

public class DashboardViewModel extends ViewModel {
    private final GroupRepository groupRepository = new GroupRepository();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<Group>> groups = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Double> totalOwe = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalOwed = new MutableLiveData<>(0.0);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private ListenerRegistration groupsListener;
    private ListenerRegistration expensesListener;

    public LiveData<List<Group>> getGroups() { return groups; }
    public LiveData<Double> getTotalOwe() { return totalOwe; }
    public LiveData<Double> getTotalOwed() { return totalOwed; }
    public LiveData<String> getError() { return error; }

    public void startListening() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        if (groupsListener != null) groupsListener.remove();
        groupsListener = groupRepository.getGroupsByUser(uid, 3, new GroupRepository.OnGroupsLoadedListener() {
            @Override
            public void onLoaded(List<Group> loadedGroups) {
                groups.setValue(loadedGroups);
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });

        if (expensesListener != null) expensesListener.remove();
        expensesListener = expenseRepository.getAllExpenses(new ExpenseRepository.OnExpensesLoadedListener() {
            @Override
            public void onLoaded(List<Expense> expenses) {
                calculateBalances(expenses, uid);
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    private void calculateBalances(List<Expense> expenses, String currentUserId) {
        Map<String, Double> groupBalances = new HashMap<>();

        for (Expense exp : expenses) {
            String status = exp.getStatus();
            boolean isSettlement = exp.isSettlement();

            if (!Expense.STATUS_COMPLETED.equals(status)) {
                continue;
            }

            String groupId = exp.getGroupId();
            if (groupId == null) continue;

            double userBalanceChange = 0;
            if (currentUserId.equals(exp.getPayerId())) {
                userBalanceChange += exp.getAmount();
            }

            Map<String, Object> splitDetails = exp.getSplitDetails();
            if (splitDetails != null && splitDetails.containsKey(currentUserId)) {
                Object shareObj = splitDetails.get(currentUserId);
                if (shareObj != null) {
                    try {
                        userBalanceChange -= Double.parseDouble(shareObj.toString());
                    } catch (Exception ignored) {}
                }
            }

            if (userBalanceChange != 0) {
                double current = 0;
                if (groupBalances.containsKey(groupId)) {
                    Double val = groupBalances.get(groupId);
                    if (val != null) current = val;
                }
                groupBalances.put(groupId, current + userBalanceChange);
            }
        }

        double owe = 0;
        double owed = 0;
        for (double bal : groupBalances.values()) {
            if (bal > 1.0) owed += bal;
            else if (bal < -1.0) owe += Math.abs(bal);
        }

        totalOwe.setValue(owe);
        totalOwed.setValue(owed);
    }

    public void joinGroup(String code) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        groupRepository.joinGroupWithCode(code, uid, new GroupRepository.OnActionListener() {
            @Override
            public void onSuccess() {
                // UI updated via listeners
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (groupsListener != null) groupsListener.remove();
        if (expensesListener != null) expensesListener.remove();
    }
}
