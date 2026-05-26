package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.repositories.ExpenseRepository;

public class StatsViewModel extends ViewModel {
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<StatsData> statsData = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<Expense>> rawExpenses = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedPeriod = new MutableLiveData<>(1);
    private ListenerRegistration expensesListener;

    public static class StatsData {
        public double[] catTotals;
        public double totalThisMonth;
        public double totalLastMonth;

        public StatsData(double[] catTotals, double totalThisMonth, double totalLastMonth) {
            this.catTotals = catTotals;
            this.totalThisMonth = totalThisMonth;
            this.totalLastMonth = totalLastMonth;
        }
    }

    public LiveData<StatsData> getStatsData() { return statsData; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<Expense>> getRawExpenses() { return rawExpenses; }
    public LiveData<Integer> getSelectedPeriod() { return selectedPeriod; }
    public String getCurrentUid() { return mAuth.getUid(); }

    public void loadStats(int period) {
        selectedPeriod.setValue(period);
        String uid = mAuth.getUid();
        if (uid == null) return;

        if (expensesListener != null) expensesListener.remove();

        expensesListener = expenseRepository.getAllExpenses(new ExpenseRepository.OnExpensesLoadedListener() {
            @Override
            public void onLoaded(List<Expense> expenses) {
                rawExpenses.setValue(expenses);
                processStats(expenses, uid, period);
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    private void processStats(List<Expense> expenses, String uid, int selectedPeriod) {
        double[] catTotals = new double[5]; // Ăn uống, Di chuyển, Mua sắm, Giải trí, Khác
        double totalThisMonth = 0;
        double totalLastMonth = 0;

        Calendar now = Calendar.getInstance();
        int thisMonth = now.get(Calendar.MONTH);
        int thisYear  = now.get(Calendar.YEAR);

        for (Expense exp : expenses) {
            if (!Expense.STATUS_COMPLETED.equals(exp.getStatus())) continue;
            if (exp.isSettlement()) continue;

            Map<String, Object> splitDetails = exp.getSplitDetails();
            if (splitDetails == null || !splitDetails.containsKey(uid)) continue;

            Object shareObj = splitDetails.get(uid);
            if (!(shareObj instanceof Number)) continue;
            double userShare = ((Number) shareObj).doubleValue();

            long timestamp = exp.getTimestamp();
            
            Calendar expCal = Calendar.getInstance();
            expCal.setTimeInMillis(timestamp);

            int expMonth = expCal.get(Calendar.MONTH);
            int expYear  = expCal.get(Calendar.YEAR);

            // Monthly comparison
            if (expMonth == thisMonth && expYear == thisYear) {
                totalThisMonth += userShare;
            }
            int lastMonthVal = thisMonth == 0 ? 11 : thisMonth - 1;
            int lastMonthYear = thisMonth == 0 ? thisYear - 1 : thisYear;
            if (expMonth == lastMonthVal && expYear == lastMonthYear) {
                totalLastMonth += userShare;
            }

            // Filter by period
            boolean inPeriod;
            if (selectedPeriod == 0) { // week
                long diffMs = now.getTimeInMillis() - timestamp;
                inPeriod = diffMs >= 0 && diffMs <= 7L * 24 * 3600 * 1000;
            } else if (selectedPeriod == 1) { // month
                inPeriod = (expMonth == thisMonth && expYear == thisYear);
            } else { // year
                inPeriod = (expYear == thisYear);
            }

            if (!inPeriod) continue;

            // Categorize
            int catIdx = getCategoryIndex(exp);
            catTotals[catIdx] += userShare;
        }

        statsData.setValue(new StatsData(catTotals, totalThisMonth, totalLastMonth));
    }

    private int getCategoryIndex(Expense exp) {
        String category = exp.getCategory();
        if (category != null) {
            switch (category.toUpperCase()) {
                case "FOOD": return 0;
                case "TRAVEL": return 1;
                case "SHOPPING": return 2;
                case "ENTERTAINMENT": return 3;
                case "OTHER":
                    break;
            }
        }

        // Fallback for legacy database records
        String desc = exp.getDescription() != null ? exp.getDescription().toLowerCase() : "";
        if (desc.contains("ăn") || desc.contains("uống") || desc.contains("cà phê") || desc.contains("food")) return 0;
        if (desc.contains("xe") || desc.contains("grab") || desc.contains("vé") || desc.contains("travel")) return 1;
        if (desc.contains("mua") || desc.contains("shop") || desc.contains("quần") || desc.contains("áo")) return 2;
        if (desc.contains("phim") || desc.contains("game") || desc.contains("giải trí")) return 3;
        return 4; // Other
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (expensesListener != null) expensesListener.remove();
    }
}
