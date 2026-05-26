package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.repositories.ExpenseRepository;
import vn.haui.smartsplit.repositories.GroupRepository;
import vn.haui.smartsplit.repositories.UserRepository;

public class GroupMembersViewModel extends ViewModel {
    private final GroupRepository groupRepository = new GroupRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<Group> group = new MutableLiveData<>();
    private final MutableLiveData<List<User>> members = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> dissolveSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private ListenerRegistration groupListener;

    public LiveData<Group> getGroup() { return group; }
    public LiveData<List<User>> getMembers() { return members; }
    public LiveData<Boolean> getActionSuccess() { return actionSuccess; }
    public LiveData<Boolean> getDissolveSuccess() { return dissolveSuccess; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void init(String groupId) {
        if (groupId == null) return;
        isLoading.setValue(true);
        if (groupListener != null) groupListener.remove();
        groupListener = groupRepository.getGroupById(groupId, new GroupRepository.OnGroupLoadedListener() {
            @Override
            public void onLoaded(Group loadedGroup) {
                group.setValue(loadedGroup);
                fetchMembers(loadedGroup.getMemberIds());
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                error.setValue(e.getMessage());
            }
        });
    }

    private void fetchMembers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            members.setValue(new ArrayList<>());
            isLoading.setValue(false);
            return;
        }

        List<User> userList = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        for (String uid : userIds) {
            userRepository.getUserById(uid, new UserRepository.OnUserLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    userList.add(user);
                    if (count.incrementAndGet() == userIds.size()) {
                        members.setValue(userList);
                        isLoading.setValue(false);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (count.incrementAndGet() == userIds.size()) {
                        members.setValue(userList);
                        isLoading.setValue(false);
                    }
                }
            });
        }
    }

    public void addMember(String email) {
        Group currentGroup = group.getValue();
        if (currentGroup == null || email.isEmpty()) return;

        isLoading.setValue(true);
        userRepository.getUserByEmail(email).addOnSuccessListener(querySnapshot -> {
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                String uid = querySnapshot.getDocuments().get(0).getId();
                if (currentGroup.getMemberIds().contains(uid)) {
                    isLoading.setValue(false);
                    error.setValue("Người dùng đã ở trong nhóm");
                    return;
                }
                groupRepository.addMember(currentGroup.getId(), uid)
                        .addOnSuccessListener(aVoid -> {
                            isLoading.setValue(false);
                            actionSuccess.setValue(true);
                        })
                        .addOnFailureListener(e -> {
                            isLoading.setValue(false);
                            error.setValue(e.getMessage());
                        });
            } else {
                isLoading.setValue(false);
                error.setValue("Không tìm thấy người dùng");
            }
        }).addOnFailureListener(e -> {
            isLoading.setValue(false);
            error.setValue(e.getMessage());
        });
    }

    public void removeMember(String uid) {
        Group currentGroup = group.getValue();
        if (currentGroup == null) return;

        isLoading.setValue(true);
        groupRepository.removeMember(currentGroup.getId(), uid)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    actionSuccess.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    error.setValue(e.getMessage());
                });
    }

    public void transferAdmin(String newAdminId) {
        Group currentGroup = group.getValue();
        if (currentGroup == null) return;

        isLoading.setValue(true);
        groupRepository.transferAdmin(currentGroup.getId(), newAdminId)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    actionSuccess.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    error.setValue(e.getMessage());
                });
    }

    public void dissolveGroup() {
        Group currentGroup = group.getValue();
        if (currentGroup == null) return;

        isLoading.setValue(true);
        groupRepository.dissolveGroup(currentGroup.getId())
                .addOnSuccessListener(aVoid -> {
                    expenseRepository.deleteExpensesByGroup(currentGroup.getId());
                    isLoading.setValue(false);
                    dissolveSuccess.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    error.setValue(e.getMessage());
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (groupListener != null) groupListener.remove();
    }
}
