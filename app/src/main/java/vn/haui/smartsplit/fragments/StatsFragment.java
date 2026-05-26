package vn.haui.smartsplit.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.utils.ReportUtils;
import vn.haui.smartsplit.viewmodels.StatsViewModel;
import vn.haui.smartsplit.views.DonutChartView;

public class StatsFragment extends Fragment {

    private DonutChartView donutChartView;
    private LinearLayout layoutCategories;
    private TextView tvTrendPercent, tvTrendDesc;
    private TextView tvBarLastMonthAmount, tvBarThisMonthAmount;
    private View barLastMonth, barThisMonth;
    private ChipGroup chipGroupPeriod;
    private View btnExport;
    private StatsViewModel viewModel;

    private static final int[] CAT_COLORS = {
            Color.parseColor("#1CC29F"),  // Food
            Color.parseColor("#3B82F6"),  // Travel
            Color.parseColor("#8B5CF6"),  // Shopping
            Color.parseColor("#F59E0B"),  // Entertainment
            Color.parseColor("#9CA3AF")   // Other
    };
    
    private static final int[] CAT_LABEL_RES = {
            R.string.cat_food, R.string.cat_travel, R.string.cat_shopping, R.string.cat_entertainment, R.string.cat_other
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        donutChartView      = view.findViewById(R.id.donutChartView);
        layoutCategories    = view.findViewById(R.id.layoutCategories);
        tvTrendPercent      = view.findViewById(R.id.tvTrendPercent);
        tvTrendDesc         = view.findViewById(R.id.tvTrendDesc);
        tvBarLastMonthAmount = view.findViewById(R.id.tvBarLastMonthAmount);
        tvBarThisMonthAmount = view.findViewById(R.id.tvBarThisMonthAmount);
        barLastMonth        = view.findViewById(R.id.barLastMonth);
        barThisMonth        = view.findViewById(R.id.barThisMonth);
        chipGroupPeriod     = view.findViewById(R.id.chipGroupPeriod);
        btnExport           = view.findViewById(R.id.btnExportPersonalReport);

        setupUI();
        observeViewModel();

        viewModel.loadStats(1); // Default month
    }

    private void setupUI() {
        chipGroupPeriod.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            int period = 1;
            if (id == R.id.chipWeek)        period = 0;
            else if (id == R.id.chipMonth)  period = 1;
            else if (id == R.id.chipYear)   period = 2;
            viewModel.loadStats(period);
        });

        btnExport.setOnClickListener(v -> {
            String uid = viewModel.getCurrentUid();
            if (uid == null) return;

            String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (name == null || name.isEmpty()) name = getString(R.string.default_username);

            List<Expense> expenses = viewModel.getRawExpenses().getValue();
            Integer period = viewModel.getSelectedPeriod().getValue();

            if (expenses != null && period != null) {
                ReportUtils.exportPersonalReportToCSV(requireContext(), uid, name, expenses, period);
            } else {
                Toast.makeText(requireContext(), getString(R.string.no_notifications_msg), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getStatsData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                updateUI(data.catTotals, data.totalThisMonth, data.totalLastMonth);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errMsg -> {
            if (errMsg != null) {
                Toast.makeText(requireContext(), errMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(double[] catTotals, double totalThisMonth, double totalLastMonth) {
        DecimalFormat fmt = new DecimalFormat("#,###");
        double total = 0;
        for (double v : catTotals) total += v;

        // Build donut segments
        List<DonutChartView.Segment> segments = new ArrayList<>();
        for (int i = 0; i < CAT_LABEL_RES.length; i++) {
            if (catTotals[i] > 0) {
                segments.add(new DonutChartView.Segment(getString(CAT_LABEL_RES[i]), (float) catTotals[i], CAT_COLORS[i]));
            }
        }

        String centerValue = "₫" + formatShort(total);
        donutChartView.setData(segments, getString(R.string.donut_center_title), centerValue);

        // Category rows
        layoutCategories.removeAllViews();
        for (int i = 0; i < CAT_LABEL_RES.length; i++) {
            if (catTotals[i] == 0) continue;
            int pct = total > 0 ? (int) Math.round(catTotals[i] / total * 100) : 0;
            layoutCategories.addView(buildCategoryRow(getString(CAT_LABEL_RES[i]), pct, catTotals[i], CAT_COLORS[i], fmt));
        }

        // Monthly trend
        double diff = totalLastMonth > 0 ? ((totalThisMonth - totalLastMonth) / totalLastMonth) * 100 : 0;
        String arrow = diff <= 0 ? "↓" : "↑";
        int trendColor = diff <= 0 ? Color.parseColor("#1CC29F") : Color.parseColor("#E53935");
        
        tvTrendPercent.setText(getString(R.string.trend_percent_format, arrow, (int) Math.abs(diff)));
        tvTrendPercent.setTextColor(trendColor);

        if (diff <= 0) {
            tvTrendDesc.setText(getString(R.string.trend_desc_less_format, (int) Math.abs(diff)));
        } else {
            tvTrendDesc.setText(getString(R.string.trend_desc_more_format, (int) Math.abs(diff)));
        }

        tvBarLastMonthAmount.setText("₫" + formatShort(totalLastMonth));
        tvBarThisMonthAmount.setText("₫" + formatShort(totalThisMonth));

        updateBarHeights(totalThisMonth, totalLastMonth);
    }

    private void updateBarHeights(double thisMonth, double lastMonth) {
        if (barThisMonth == null || barLastMonth == null) return;

        float density = getResources().getDisplayMetrics().density;
        double max = Math.max(thisMonth, lastMonth);
        int maxHeightPx = (int) (80 * density); // 80dp max height

        ViewGroup.LayoutParams lpThis = barThisMonth.getLayoutParams();
        lpThis.height = max > 0 ? (int) ((thisMonth / max) * maxHeightPx) : 0;
        if (thisMonth > 0 && lpThis.height < (int)(4 * density)) lpThis.height = (int)(4 * density);
        barThisMonth.setLayoutParams(lpThis);

        ViewGroup.LayoutParams lpLast = barLastMonth.getLayoutParams();
        lpLast.height = max > 0 ? (int) ((lastMonth / max) * maxHeightPx) : 0;
        if (lastMonth > 0 && lpLast.height < (int)(4 * density)) lpLast.height = (int)(4 * density);
        barLastMonth.setLayoutParams(lpLast);
    }

    private View buildCategoryRow(String label, int pct, double amount, int color, DecimalFormat fmt) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, (int) (12 * density));
        row.setLayoutParams(lp);

        View dot = new View(requireContext());
        dot.setLayoutParams(new LinearLayout.LayoutParams((int)(8 * density), (int)(8 * density)));
        dot.setBackground(createCircleDrawable(color));
        row.addView(dot);

        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart((int)(12 * density));
        textCol.setLayoutParams(textLp);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setTextColor(Color.parseColor("#1E293B"));
        textCol.addView(tvLabel);

        TextView tvPct = new TextView(requireContext());
        tvPct.setText(pct + "%");
        tvPct.setTextSize(12);
        tvPct.setTextColor(Color.parseColor("#64748B"));
        textCol.addView(tvPct);

        row.addView(textCol);

        TextView tvAmt = new TextView(requireContext());
        tvAmt.setText(getString(R.string.currency_prefix_format, fmt.format(amount)));
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAmt.setTextColor(Color.parseColor("#1E293B"));
        row.addView(tvAmt);

        return row;
    }

    private android.graphics.drawable.ShapeDrawable createCircleDrawable(int color) {
        android.graphics.drawable.ShapeDrawable d = new android.graphics.drawable.ShapeDrawable(new android.graphics.drawable.shapes.OvalShape());
        d.getPaint().setColor(color);
        return d;
    }

    private String formatShort(double value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
        if (value >= 1_000)     return String.format("%.0fk", value / 1_000);
        return String.valueOf((long) value);
    }
}
