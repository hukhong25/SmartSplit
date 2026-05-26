package vn.haui.smartsplit.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.AppNotification;
import vn.haui.smartsplit.utils.NotificationLocalizer;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<AppNotification> notificationList;
    private final OnNotificationClickListener listener;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    public NotificationAdapter(List<AppNotification> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification notification = notificationList.get(position);
        NotificationLocalizer.LocalizedResult localized = NotificationLocalizer.localize(
                holder.itemView.getContext(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent()
        );
        holder.tvTitle.setText(localized.title);
        holder.tvContent.setText(localized.content);
        holder.tvTime.setText(DATE_FORMAT.format(new Date(notification.getTimestamp())));

        if (!notification.isRead()) {
            // Hiển thị chấm xanh và in đậm tiêu đề cho thông báo chưa đọc
            holder.itemView.setBackgroundColor(Color.parseColor("#F1F5F9")); // Nền xám xanh nhẹ
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.tvTitle.setTextColor(Color.parseColor("#1E293B"));
            if (holder.viewUnreadDot != null) {
                holder.viewUnreadDot.setVisibility(View.VISIBLE);
                holder.viewUnreadDot.setBackgroundResource(R.drawable.circle_unread);
            }
        } else {
            // Thông báo đã đọc: Nền trong suốt, chữ bình thường
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.tvTitle.setTextColor(Color.parseColor("#64748B"));
            if (holder.viewUnreadDot != null) {
                holder.viewUnreadDot.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(notification);
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime;
        View viewUnreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvContent = itemView.findViewById(R.id.tvNotifContent);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}
