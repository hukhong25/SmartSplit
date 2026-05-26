package vn.haui.smartsplit.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;

public class ReportUtils {

    public static void exportGroupReportToCSV(Context context, Group group, List<Expense> expenses, List<User> members, Map<String, Double> balances) {
        if (group == null || expenses == null || members == null || balances == null) {
            Toast.makeText(context, context.getString(R.string.toast_export_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder csv = new StringBuilder();
            // UTF-8 BOM to make Excel open Vietnamese characters properly
            csv.append('\ufeff');

            // Header information
            csv.append("TÊN NHÓM (GROUP NAME),").append(group.getName()).append("\n");
            csv.append("MÃ NHÓM (GROUP CODE),").append(group.getJoinCode()).append("\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            csv.append("NGÀY XUẤT BÁO CÁO (EXPORT DATE),").append(sdf.format(new Date())).append("\n\n");

            // Expenses history
            csv.append("DANH SÁCH CHI TIÊU VÀ TẤT TOÁN (TRANSACTION HISTORY)\n");
            csv.append("Mô tả (Description),Số tiền (Amount),Người trả (Payer),Thời gian (Time),Loại giao dịch (Type),Trạng thái (Status)\n");

            DecimalFormat df = new DecimalFormat("#,###");
            for (Expense exp : expenses) {
                String type = exp.isSettlement() ? "Tất toán (Settlement)" : "Khoản chi (Expense)";
                String status = exp.getStatus() != null ? exp.getStatus() : "";
                if ("PENDING".equals(status)) {
                    status = "Chờ xác nhận (Pending)";
                } else if ("REJECTED".equals(status)) {
                    status = "Bị từ chối (Rejected)";
                } else {
                    status = "Hoàn thành (Completed)";
                }

                csv.append(escapeCSV(exp.getDescription())).append(",")
                   .append(exp.getAmount()).append(",")
                   .append(escapeCSV(exp.getPayerName())).append(",")
                   .append(sdf.format(new Date(exp.getTimestamp()))).append(",")
                   .append(type).append(",")
                   .append(status).append("\n");
            }
            csv.append("\n");

            // Balances
            csv.append("CHI TIẾT SỐ DƯ THÀNH VIÊN (MEMBER BALANCES)\n");
            csv.append("Thành viên (Member),Email,Số dư (Balance)\n");

            Map<String, User> userMap = new HashMap<>();
            for (User u : members) {
                userMap.put(u.getUid(), u);
            }

            for (Map.Entry<String, Double> entry : balances.entrySet()) {
                User user = userMap.get(entry.getKey());
                if (user != null) {
                    double bal = entry.getValue();
                    String balStr = bal > 0 ? "+" + df.format(bal) : df.format(bal);
                    csv.append(escapeCSV(user.getName())).append(",")
                       .append(escapeCSV(user.getEmail())).append(",")
                       .append(balStr).append(" VND\n");
                }
            }

            // Save to cache file
            String fileName = "SmartSplit_" + group.getName().replaceAll("[\\\\/:*?\"<>|\\s]", "_") + "_BaoCao.csv";
            File cacheDir = new File(context.getCacheDir(), "reports");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File file = new File(cacheDir, fileName);

            FileOutputStream out = new FileOutputStream(file);
            out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            out.close();

            // Share file via Uri
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.action_export_report)));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.toast_export_failed), Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Personal Report ────────────────────────────────────────────────────────

    /**
     * Builds and shares a personal spending CSV for the given user.
     *
     * @param context  Activity / Fragment context
     * @param uid      Firebase UID of the current user
     * @param userName Display name shown in the report header
     * @param expenses Full list of expenses returned by ExpenseRepository.getAllExpenses()
     * @param period   0 = this week, 1 = this month, 2 = this year, -1 = all time
     */
    public static void exportPersonalReportToCSV(Context context,
                                                 String uid,
                                                 String userName,
                                                 List<Expense> expenses,
                                                 int period) {
        if (uid == null || expenses == null) {
            Toast.makeText(context, context.getString(R.string.toast_export_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            DecimalFormat df = new DecimalFormat("#,###");

            // Determine period boundaries
            long nowMs = System.currentTimeMillis();
            long fromMs;
            String periodLabel;
            switch (period) {
                case 0:
                    fromMs = nowMs - 7L * 24 * 3600 * 1000;
                    periodLabel = context.getString(R.string.chip_this_week);
                    break;
                case 2:
                    java.util.Calendar cal2 = java.util.Calendar.getInstance();
                    cal2.set(java.util.Calendar.DAY_OF_YEAR, 1);
                    cal2.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal2.set(java.util.Calendar.MINUTE, 0);
                    cal2.set(java.util.Calendar.SECOND, 0);
                    fromMs = cal2.getTimeInMillis();
                    periodLabel = context.getString(R.string.chip_this_year);
                    break;
                default: // month
                    java.util.Calendar cal1 = java.util.Calendar.getInstance();
                    cal1.set(java.util.Calendar.DAY_OF_MONTH, 1);
                    cal1.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal1.set(java.util.Calendar.MINUTE, 0);
                    cal1.set(java.util.Calendar.SECOND, 0);
                    fromMs = cal1.getTimeInMillis();
                    periodLabel = context.getString(R.string.chip_this_month);
                    break;
            }

            // Category totals & per-expense rows
            String[] catKeys = {"FOOD", "TRAVEL", "SHOPPING", "ENTERTAINMENT", "OTHER"};
            int[] catResIds = {
                R.string.cat_food, R.string.cat_travel,
                R.string.cat_shopping, R.string.cat_entertainment, R.string.cat_other
            };
            double[] catTotals = new double[5];
            double grandTotal = 0;

            StringBuilder rows = new StringBuilder();
            for (Expense exp : expenses) {
                if (!Expense.STATUS_COMPLETED.equals(exp.getStatus())) continue;
                if (exp.isSettlement()) continue;

                Map<String, Object> split = exp.getSplitDetails();
                if (split == null || !split.containsKey(uid)) continue;

                Object shareObj = split.get(uid);
                if (!(shareObj instanceof Number)) continue;
                double userShare = ((Number) shareObj).doubleValue();

                long ts = exp.getTimestamp();
                if (ts < fromMs || ts > nowMs) continue;

                // Resolve category index
                int catIdx = resolveCategoryIndex(exp);
                catTotals[catIdx] += userShare;
                grandTotal += userShare;

                String catLabel = context.getString(catResIds[catIdx]);
                rows.append(escapeCSV(exp.getDescription())).append(",")
                    .append(escapeCSV(exp.getPayerName())).append(",")
                    .append(df.format(userShare)).append(",")
                    .append(catLabel).append(",")
                    .append(sdf.format(new Date(ts))).append("\n");
            }

            // Build CSV
            StringBuilder csv = new StringBuilder();
            csv.append('\ufeff'); // UTF-8 BOM

            csv.append(context.getString(R.string.report_personal_header_name)).append(",")
               .append(escapeCSV(userName)).append("\n");
            csv.append(context.getString(R.string.report_personal_header_period)).append(",")
               .append(periodLabel).append("\n");
            csv.append(context.getString(R.string.report_personal_header_export_date)).append(",")
               .append(sdf.format(new Date())).append("\n\n");

            // Summary by category
            csv.append(context.getString(R.string.report_personal_section_summary)).append("\n");
            csv.append(context.getString(R.string.report_col_category)).append(",")
               .append(context.getString(R.string.report_col_amount)).append(",")
               .append(context.getString(R.string.report_col_percent)).append("\n");
            for (int i = 0; i < catKeys.length; i++) {
                if (catTotals[i] == 0) continue;
                int pct = grandTotal > 0 ? (int) Math.round(catTotals[i] / grandTotal * 100) : 0;
                csv.append(context.getString(catResIds[i])).append(",")
                   .append(df.format(catTotals[i])).append(" VND,")
                   .append(pct).append("%\n");
            }
            csv.append(context.getString(R.string.report_col_total)).append(",")
               .append(df.format(grandTotal)).append(" VND,100%\n\n");

            // Detail rows
            csv.append(context.getString(R.string.report_personal_section_detail)).append("\n");
            csv.append(context.getString(R.string.report_col_description)).append(",")
               .append(context.getString(R.string.report_col_payer)).append(",")
               .append(context.getString(R.string.report_col_your_share)).append(",")
               .append(context.getString(R.string.report_col_category)).append(",")
               .append(context.getString(R.string.report_col_time)).append("\n");
            csv.append(rows);

            // Save
            String fileName = "SmartSplit_CaNhan_" + userName.replaceAll("[\\\\/:*?\"<>|\\s]", "_") + ".csv";
            File cacheDir = new File(context.getCacheDir(), "reports");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File file = new File(cacheDir, fileName);
            FileOutputStream out = new FileOutputStream(file);
            out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            out.close();

            // Share
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent,
                    context.getString(R.string.action_export_personal_report)));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.toast_export_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private static int resolveCategoryIndex(Expense exp) {
        String category = exp.getCategory();
        if (category != null) {
            switch (category.toUpperCase()) {
                case "FOOD": return 0;
                case "TRAVEL": return 1;
                case "SHOPPING": return 2;
                case "ENTERTAINMENT": return 3;
            }
        }
        String desc = exp.getDescription() != null ? exp.getDescription().toLowerCase() : "";
        if (desc.contains("ăn") || desc.contains("uống") || desc.contains("cà phê") || desc.contains("food")) return 0;
        if (desc.contains("xe") || desc.contains("grab") || desc.contains("vé") || desc.contains("travel")) return 1;
        if (desc.contains("mua") || desc.contains("shop") || desc.contains("quần") || desc.contains("áo")) return 2;
        if (desc.contains("phim") || desc.contains("game") || desc.contains("giải trí")) return 3;
        return 4;
    }

    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
