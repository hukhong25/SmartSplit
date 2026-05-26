package vn.haui.smartsplit.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.models.Group;

public class GroupRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnGroupsLoadedListener {
        void onLoaded(List<Group> groups);
        void onError(Exception e);
    }

    public interface OnGroupLoadedListener {
        void onLoaded(Group group);
        void onError(Exception e);
    }

    public ListenerRegistration getGroupsByUser(String userId, int limit, OnGroupsLoadedListener listener) {
        return db.collection("groups")
                .whereArrayContains("memberIds", userId)
                .limit(limit)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<Group> groups = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            groups.add(doc.toObject(Group.class));
                        }
                    }
                    listener.onLoaded(groups);
                });
    }

    public ListenerRegistration getGroupById(String groupId, OnGroupLoadedListener listener) {
        return db.collection("groups").document(groupId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        listener.onLoaded(doc.toObject(Group.class));
                    } else {
                        listener.onError(new Exception("Group not found"));
                    }
                });
    }

    public void joinGroupWithCode(String code, String userId, OnActionListener listener) {
        db.collection("groups").whereEqualTo("joinCode", code).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String groupId = snapshots.getDocuments().get(0).getId();
                        db.collection("groups").document(groupId)
                                .update("memberIds", FieldValue.arrayUnion(userId))
                                .addOnSuccessListener(v -> listener.onSuccess())
                                .addOnFailureListener(listener::onError);
                    } else {
                        listener.onError(new Exception("Invalid code"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public Task<Void> createGroup(Group group) {
        return db.collection("groups").document(group.getId()).set(group);
    }

    public String generateGroupId() {
        return db.collection("groups").document().getId();
    }

    public Task<Void> dissolveGroup(String groupId) {
        return db.collection("groups").document(groupId).delete();
    }

    public Task<Void> addMember(String groupId, String uid) {
        return db.collection("groups").document(groupId).update("memberIds", FieldValue.arrayUnion(uid));
    }

    public Task<Void> removeMember(String groupId, String uid) {
        return db.collection("groups").document(groupId).update("memberIds", FieldValue.arrayRemove(uid));
    }

    public Task<Void> transferAdmin(String groupId, String newAdminId) {
        return db.collection("groups").document(groupId).update("adminId", newAdminId);
    }

    public interface OnActionListener {
        void onSuccess();
        void onError(Exception e);
    }
}
