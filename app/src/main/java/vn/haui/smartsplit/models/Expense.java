package vn.haui.smartsplit.models;

import java.util.Map;

public class Expense {
    // Status Constants
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REJECTED = "REJECTED";

    // Category Constants
    public static final String CAT_FOOD = "FOOD";
    public static final String CAT_TRAVEL = "TRAVEL";
    public static final String CAT_SHOPPING = "SHOPPING";
    public static final String CAT_ENTERTAINMENT = "ENTERTAINMENT";
    public static final String CAT_OTHER = "OTHER";

    private String id;
    private String description;
    private double amount;
    private String payerId;
    private String payerName;
    private String groupId;
    private long timestamp;
    private Map<String, Object> splitDetails; 
    private String proofImageUrl;
    private String status; 
    private boolean isSettlement;
    private String category;

    public Expense() {
        this.status = STATUS_COMPLETED;
        this.isSettlement = false;
        this.category = CAT_OTHER;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Map<String, Object> getSplitDetails() { return splitDetails; }
    public void setSplitDetails(Map<String, Object> splitDetails) { this.splitDetails = splitDetails; }
    public String getProofImageUrl() { return proofImageUrl; }
    public void setProofImageUrl(String proofImageUrl) { this.proofImageUrl = proofImageUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSettlement() { return isSettlement; }
    public void setSettlement(boolean settlement) { isSettlement = settlement; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
