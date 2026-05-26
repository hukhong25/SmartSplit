package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import vn.haui.smartsplit.models.AppNotification;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.repositories.ExpenseRepository;
import vn.haui.smartsplit.repositories.GroupRepository;
import vn.haui.smartsplit.repositories.NotificationRepository;
import vn.haui.smartsplit.repositories.UserRepository;
import vn.haui.smartsplit.utils.CategoryUtils;

public class AddGroupExpenseViewModel extends ViewModel {
    private final GroupRepository groupRepository = new GroupRepository();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final UserRepository userRepository = new UserRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository();

    private final MutableLiveData<List<User>> memberList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Expense> expenseData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<User>> getMemberList() { return memberList; }
    public LiveData<Expense> getExpenseData() { return expenseData; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadGroupMembers(String groupId) {
        if (groupId == null) return;
        isLoading.setValue(true);
        groupRepository.getGroupById(groupId, new GroupRepository.OnGroupLoadedListener() {
            @Override
            public void onLoaded(vn.haui.smartsplit.models.Group group) {
                fetchUsers(group.getMemberIds());
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                error.setValue(e.getMessage());
            }
        });
    }

    private void fetchUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            isLoading.setValue(false);
            return;
        }

        List<User> users = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        for (String uid : userIds) {
            userRepository.getUserById(uid, new UserRepository.OnUserLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    users.add(user);
                    if (count.incrementAndGet() == userIds.size()) {
                        memberList.setValue(users);
                        isLoading.setValue(false);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (count.incrementAndGet() == userIds.size()) {
                        memberList.setValue(users);
                        isLoading.setValue(false);
                    }
                }
            });
        }
    }

    public void loadExpense(String expenseId) {
        if (expenseId == null) return;
        isLoading.setValue(true);
        expenseRepository.getExpenseById(expenseId, new ExpenseRepository.OnExpenseLoadedListener() {
            @Override
            public void onLoaded(Expense expense) {
                expenseData.setValue(expense);
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                error.setValue(e.getMessage());
            }
        });
    }

    public void saveExpense(String id, String desc, double amount, User payer, String groupId, List<String> selectedUserIds, String currentUid, String category, String notifTitle, String notifContentFormat) {
        isLoading.setValue(true);
        double share = amount / selectedUserIds.size();
        
        Map<String, Object> splitDetails = new HashMap<>();
        for (String uid : selectedUserIds) splitDetails.put(uid, share);

        Expense expense = new Expense();
        expense.setId(id);
        expense.setDescription(desc);
        expense.setAmount(amount);
        expense.setPayerId(payer.getUid());
        expense.setPayerName(payer.getName());
        expense.setGroupId(groupId);
        expense.setTimestamp(System.currentTimeMillis());
        expense.setSplitDetails(splitDetails);
        expense.setStatus(Expense.STATUS_COMPLETED);
        expense.setSettlement(false);
        expense.setCategory(category != null ? category : CategoryUtils.deduceCategory(desc));

        expenseRepository.saveExpense(expense)
                .addOnSuccessListener(aVoid -> {
                    notifyMembers(expense, currentUid, notifTitle, notifContentFormat);
                    saveSuccess.setValue(true);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    error.setValue(e.getMessage());
                });
    }

    private void notifyMembers(Expense expense, String currentUid, String title, String contentFormat) {
        String content = String.format(contentFormat, expense.getPayerName(), expense.getDescription(), (long) expense.getAmount());

        if (expense.getSplitDetails() != null) {
            for (String uid : expense.getSplitDetails().keySet()) {
                if (uid.equals(currentUid)) continue;

                String notifId = notificationRepository.generateId();
                AppNotification notif = new AppNotification(
                        notifId, uid, title, content, "EXPENSE_ADDED", expense.getId()
                );
                notificationRepository.sendNotification(notif);
            }
        }
    }
}
