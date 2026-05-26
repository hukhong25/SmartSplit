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
            csv.append('\ufeff');

            csv.append("TÊN NHÓM (GROUP NAME),").append(group.getName()).append("\n");
            csv.append("MÃ NHÓM (GROUP CODE),").append(group.getJoinCode()).append("\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            csv.append("NGÀY XUẤT BÁO CÁO (EXPORT DATE),").append(sdf.format(new Date())).append("\n\n");

            csv.append("DANH SÁCH CHI TIÊU VÀ TẤT TOÁN (TRANSACTION HISTORY)\n");
            csv.append("Mô tả (Description),Số tiền (Amount),Người trả (Payer),Thời gian (Time),Loại giao dịch (Type),Trạng thái (Status)\n");

            for (Expense exp : expenses) {
                String type = exp.isSettlement() ? "Tất toán (Settlement)" : "Khoản chi (Expense)";
                String status = getStatusLabel(exp.getStatus());

                csv.append(escapeCSV(exp.getDescription())).append(",")
                   .append(exp.getAmount()).append(",")
                   .append(escapeCSV(exp.getPayerName())).append(",")
                   .append(sdf.format(new Date(exp.getTimestamp()))).append(",")
                   .append(type).append(",")
                   .append(status).append("\n");
            }
            csv.append("\n");

            csv.append("CHI TIẾT SỐ DƯ THÀNH VIÊN (MEMBER BALANCES)\n");
            csv.append("Thành viên (Member),Email,Số dư (Balance)\n");

            DecimalFormat df = new DecimalFormat("#,###");
            Map<String, User> userMap = new HashMap<>();
            for (User u : members) userMap.put(u.getUid(), u);

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

            shareFile(context, csv.toString(), "SmartSplit_" + group.getName(), R.string.action_export_report);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.toast_export_failed), Toast.LENGTH_SHORT).show();
        }
    }

    public static void exportPersonalReportToCSV(Context context, String uid, String userName, List<Expense> expenses, int period) {
        if (uid == null || expenses == null) {
            Toast.makeText(context, context.getString(R.string.toast_export_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            DecimalFormat df = new DecimalFormat("#,###");

            long nowMs = System.currentTimeMillis();
            long fromMs = getStartTimeFromPeriod(period);
            String periodLabel = getPeriodLabel(context, period);

            double[] catTotals = new double[5];
            double grandTotal = 0;
            StringBuilder rows = new StringBuilder();

            for (Expense exp : expenses) {
                if (!Expense.STATUS_COMPLETED.equals(exp.getStatus()) || exp.isSettlement()) continue;

                Map<String, Object> split = exp.getSplitDetails();
                if (split == null || !split.containsKey(uid)) continue;

                Object shareObj = split.get(uid);
                if (!(shareObj instanceof Number)) continue;
                double userShare = ((Number) shareObj).doubleValue();

                long ts = exp.getTimestamp();
                if (ts < fromMs || ts > nowMs) continue;

                int catIdx = resolveCategoryIndex(exp);
                catTotals[catIdx] += userShare;
                grandTotal += userShare;

                rows.append(escapeCSV(exp.getDescription())).append(",")
                    .append(escapeCSV(exp.getPayerName())).append(",")
                    .append(df.format(userShare)).append(",")
                    .append(context.getString(getCategoryResId(catIdx))).append(",")
                    .append(sdf.format(new Date(ts))).append("\n");
            }

            StringBuilder csv = new StringBuilder();
            csv.append('\ufeff');
            csv.append(context.getString(R.string.report_personal_header_name)).append(",").append(escapeCSV(userName)).append("\n");
            csv.append(context.getString(R.string.report_personal_header_period)).append(",").append(periodLabel).append("\n");
            csv.append(context.getString(R.string.report_personal_header_export_date)).append(",").append(sdf.format(new Date())).append("\n\n");

            csv.append(context.getString(R.string.report_personal_section_summary)).append("\n");
            csv.append(context.getString(R.string.report_col_category)).append(",").append(context.getString(R.string.report_col_amount)).append(",").append(context.getString(R.string.report_col_percent)).append("\n");
            
            for (int i = 0; i < 5; i++) {
                if (catTotals[i] == 0) continue;
                int pct = grandTotal > 0 ? (int) Math.round(catTotals[i] / grandTotal * 100) : 0;
                csv.append(context.getString(getCategoryResId(i))).append(",")
                   .append(df.format(catTotals[i])).append(" VND,")
                   .append(pct).append("%\n");
            }
            csv.append(context.getString(R.string.report_col_total)).append(",").append(df.format(grandTotal)).append(" VND,100%\n\n");

            csv.append(context.getString(R.string.report_personal_section_detail)).append("\n");
            csv.append(context.getString(R.string.report_col_description)).append(",")
               .append(context.getString(R.string.report_col_payer)).append(",")
               .append(context.getString(R.string.report_col_your_share)).append(",")
               .append(context.getString(R.string.report_col_category)).append(",")
               .append(context.getString(R.string.report_col_time)).append("\n");
            csv.append(rows);

            shareFile(context, csv.toString(), "SmartSplit_Personal_" + userName, R.string.action_export_personal_report);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.toast_export_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private static void shareFile(Context context, String content, String prefix, int chooserTitleRes) throws Exception {
        String fileName = prefix.replaceAll("[\\\\/:*?\"<>|\\s]", "_") + ".csv";
        File cacheDir = new File(context.getCacheDir(), "reports");
        if (!cacheDir.exists()) cacheDir.mkdirs();
        File file = new File(cacheDir, fileName);
        FileOutputStream out = new FileOutputStream(file);
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.close();

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, context.getString(chooserTitleRes)));
    }

    private static String getStatusLabel(String status) {
        if ("PENDING".equals(status)) return "Chờ xác nhận (Pending)";
        if ("REJECTED".equals(status)) return "Bị từ chối (Rejected)";
        return "Hoàn thành (Completed)";
    }

    private static long getStartTimeFromPeriod(int period) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (period == 0) return System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        if (period == 2) cal.set(java.util.Calendar.DAY_OF_YEAR, 1);
        else cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    private static String getPeriodLabel(Context context, int period) {
        if (period == 0) return context.getString(R.string.chip_this_week);
        if (period == 2) return context.getString(R.string.chip_this_year);
        return context.getString(R.string.chip_this_month);
    }

    private static int getCategoryResId(int idx) {
        int[] ids = {R.string.cat_food, R.string.cat_travel, R.string.cat_shopping, R.string.cat_entertainment, R.string.cat_other};
        return ids[idx];
    }

    private static int resolveCategoryIndex(Expense exp) {
        String cat = exp.getCategory();
        if ("FOOD".equalsIgnoreCase(cat)) return 0;
        if ("TRAVEL".equalsIgnoreCase(cat)) return 1;
        if ("SHOPPING".equalsIgnoreCase(cat)) return 2;
        if ("ENTERTAINMENT".equalsIgnoreCase(cat)) return 3;
        
        String desc = exp.getDescription() != null ? exp.getDescription().toLowerCase() : "";
        if (desc.contains("ăn") || desc.contains("uống") || desc.contains("food")) return 0;
        if (desc.contains("xe") || desc.contains("grab") || desc.contains("travel")) return 1;
        if (desc.contains("mua") || desc.contains("shop")) return 2;
        if (desc.contains("phim") || desc.contains("game")) return 3;
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
