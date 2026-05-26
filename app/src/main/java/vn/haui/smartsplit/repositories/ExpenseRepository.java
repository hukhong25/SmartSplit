package vn.haui.smartsplit.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.haui.smartsplit.models.Expense;

public class ExpenseRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnExpensesLoadedListener {
        void onLoaded(List<Expense> expenses);
        void onError(Exception e);
    }

    public interface OnExpenseLoadedListener {
        void onLoaded(Expense expense);
        void onError(Exception e);
    }

    public ListenerRegistration getAllExpenses(OnExpensesLoadedListener listener) {
        // Bỏ .orderBy() để tránh yêu cầu Index phức tạp
        return db.collection("expenses")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<Expense> expenses = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                expenses.add(doc.toObject(Expense.class));
                            } catch (Exception e) {
                                // Bỏ qua
                            }
                        }
                    }
                    // Sắp xếp thủ công: mới nhất lên đầu
                    Collections.sort(expenses, (e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));
                    listener.onLoaded(expenses);
                });
    }

    public ListenerRegistration getExpensesByGroup(String groupId, OnExpensesLoadedListener listener) {
        // Bỏ .orderBy() ở đây để tránh lỗi Index
        return db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<Expense> expenses = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                expenses.add(doc.toObject(Expense.class));
                            } catch (Exception e) {
                                // Bỏ qua
                            }
                        }
                    }
                    // Sắp xếp thủ công: mới nhất lên đầu
                    Collections.sort(expenses, (e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));
                    listener.onLoaded(expenses);
                });
    }

    public ListenerRegistration observeExpenseById(String expenseId, OnExpenseLoadedListener listener) {
        return db.collection("expenses")
                .document(expenseId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        listener.onLoaded(doc.toObject(Expense.class));
                    } else {
                        listener.onError(new Exception("Expense not found"));
                    }
                });
    }

    public void getExpenseById(String expenseId, OnExpenseLoadedListener listener) {
        db.collection("expenses")
                .document(expenseId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        listener.onLoaded(doc.toObject(Expense.class));
                    } else {
                        listener.onError(new Exception("Expense not found"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public Task<Void> saveExpense(Expense expense) {
        return db.collection("expenses").document(expense.getId()).set(expense);
    }

    public Task<Void> updateExpenseStatus(String expenseId, String status) {
        return db.collection("expenses").document(expenseId).update("status", status);
    }

    public Task<Void> deleteExpense(String expenseId) {
        return db.collection("expenses").document(expenseId).delete();
    }

    public void deleteExpensesByGroup(String groupId) {
        db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        com.google.firebase.firestore.WriteBatch batch = db.batch();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            batch.delete(doc.getReference());
                        }
                        batch.commit();
                    }
                });
    }
}