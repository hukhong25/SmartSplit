package vn.haui.smartsplit.utils;

import android.content.Context;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vn.haui.smartsplit.R;

public class NotificationLocalizer {

    public static class LocalizedResult {
        public final String title;
        public final String content;

        public LocalizedResult(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    public static LocalizedResult localize(Context context, String type, String originalTitle, String originalContent) {
        if (type == null) {
            return new LocalizedResult(originalTitle, originalContent);
        }

        try {
            switch (type) {
                case "EXPENSE_ADDED":
                    return localizeExpenseAdded(context, originalContent);
                case "PAYMENT_REQUEST":
                    return localizePaymentRequest(context, originalContent);
                case "PAYMENT_RESPONSE":
                    return localizePaymentResponse(context, originalTitle, originalContent);
                case "REMIND":
                    return localizeRemind(context, originalContent);
                default:
                    return new LocalizedResult(originalTitle, originalContent);
            }
        } catch (Exception e) {
            // Fallback to original content in case of parsing errors
            return new LocalizedResult(originalTitle, originalContent);
        }
    }

    private static LocalizedResult localizeExpenseAdded(Context context, String content) {
        // VI: [PayerName] đã thêm: [Description] ([Amount] VND)
        Pattern viPattern = Pattern.compile("^(.*?) đã thêm: (.*?) \\((\\d+) VND\\)$");
        // EN: [PayerName] added: [Description] ([Amount] VND)
        Pattern enPattern = Pattern.compile("^(.*?) added: (.*?) \\((\\d+) VND\\)$");

        Matcher matcher = viPattern.matcher(content);
        if (!matcher.matches()) {
            matcher = enPattern.matcher(content);
        }

        if (matcher.matches()) {
            String payerName = matcher.group(1);
            String description = matcher.group(2);
            long amount = Long.parseLong(matcher.group(3));

            String title = context.getString(R.string.notif_title_new_expense);
            String localizedContent = context.getString(R.string.notif_content_new_expense_format, payerName, description, amount);
            return new LocalizedResult(title, localizedContent);
        }

        return new LocalizedResult(context.getString(R.string.notif_title_new_expense), content);
    }

    private static LocalizedResult localizePaymentRequest(Context context, String content) {
        // VI: [SenderName] đã gửi xác nhận thanh toán [Amount] VND. Vui lòng xác nhận.
        Pattern viPattern = Pattern.compile("^(.*?) đã gửi xác nhận thanh toán (\\d+) VND\\. Vui lòng xác nhận\\.$");
        // EN: [SenderName] has sent a payment confirmation of [Amount] VND. Please confirm.
        Pattern enPattern = Pattern.compile("^(.*?) has sent a payment confirmation of (\\d+) VND\\. Please confirm\\.$");

        Matcher matcher = viPattern.matcher(content);
        if (!matcher.matches()) {
            matcher = enPattern.matcher(content);
        }

        if (matcher.matches()) {
            String senderName = matcher.group(1);
            long amount = Long.parseLong(matcher.group(2));

            String title = context.getString(R.string.notif_title_new_payment_request);
            String localizedContent = context.getString(R.string.notif_content_payment_request_format, senderName, amount);
            return new LocalizedResult(title, localizedContent);
        }

        return new LocalizedResult(context.getString(R.string.notif_title_new_payment_request), content);
    }

    private static LocalizedResult localizePaymentResponse(Context context, String title, String content) {
        // VI Accepted: [ReceiverName] đã xác nhận khoản thanh toán [Amount] VND của bạn.
        Pattern viAcceptPattern = Pattern.compile("^(.*?) đã xác nhận khoản thanh toán (\\d+) VND của bạn\\.$");
        // EN Accepted: [ReceiverName] confirmed your payment of [Amount] VND.
        Pattern enAcceptPattern = Pattern.compile("^(.*?) confirmed your payment of (\\d+) VND\\.$");

        // VI Rejected: [ReceiverName] đã từ chối khoản thanh toán [Amount] VND của bạn.
        Pattern viRejectPattern = Pattern.compile("^(.*?) đã từ chối khoản thanh toán (\\d+) VND của bạn\\.$");
        // EN Rejected: [ReceiverName] rejected your payment of [Amount] VND.
        Pattern enRejectPattern = Pattern.compile("^(.*?) rejected your payment of (\\d+) VND\\.$");

        // Determine if it was accepted or rejected from title/content
        boolean isAccepted = true;
        Matcher matcher = viAcceptPattern.matcher(content);
        if (!matcher.matches()) {
            matcher = enAcceptPattern.matcher(content);
        }

        if (!matcher.matches()) {
            isAccepted = false;
            matcher = viRejectPattern.matcher(content);
            if (!matcher.matches()) {
                matcher = enRejectPattern.matcher(content);
            }
        }

        if (matcher.matches()) {
            String receiverName = matcher.group(1);
            long amount = Long.parseLong(matcher.group(2));

            String localizedTitle = isAccepted 
                ? context.getString(R.string.notif_title_payment_accepted)
                : context.getString(R.string.notif_title_payment_rejected);
            String localizedContent = isAccepted
                ? context.getString(R.string.notif_content_payment_accepted_format, receiverName, amount)
                : context.getString(R.string.notif_content_payment_rejected_format, receiverName, amount);

            return new LocalizedResult(localizedTitle, localizedContent);
        }

        // Fallback title translation based on original title keywords if regex matches failed
        String fallbackTitle = title;
        if (title != null) {
            if (title.contains("xác nhận") || title.contains("Confirmed")) {
                fallbackTitle = context.getString(R.string.notif_title_payment_accepted);
            } else if (title.contains("từ chối") || title.contains("Rejected")) {
                fallbackTitle = context.getString(R.string.notif_title_payment_rejected);
            }
        }

        return new LocalizedResult(fallbackTitle, content);
    }

    private static LocalizedResult localizeRemind(Context context, String content) {
        // VI: [SenderName] nhắc bạn thanh toán khoản nợ [Amount] VND trong nhóm [GroupName]
        Pattern viPattern = Pattern.compile("^(.*?) nhắc bạn thanh toán khoản nợ (\\d+) VND trong nhóm (.*)$");
        // EN: [SenderName] reminded you to settle a debt of [Amount] VND in group [GroupName]
        Pattern enPattern = Pattern.compile("^(.*?) reminded you to settle a debt of (\\d+) VND in group (.*)$");

        Matcher matcher = viPattern.matcher(content);
        if (!matcher.matches()) {
            matcher = enPattern.matcher(content);
        }

        if (matcher.matches()) {
            String senderName = matcher.group(1);
            long amount = Long.parseLong(matcher.group(2));
            String groupName = matcher.group(3);

            String title = context.getString(R.string.notif_title_debt_remind);
            String localizedContent = context.getString(R.string.notif_content_debt_remind_format, senderName, amount, groupName);
            return new LocalizedResult(title, localizedContent);
        }

        return new LocalizedResult(context.getString(R.string.notif_title_debt_remind), content);
    }
}
