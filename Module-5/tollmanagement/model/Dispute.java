package tollmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Dispute {
    private final String disputeId;
    private final String transactionId;
    private final String vehicleRegistrationNumber;
    private final BigDecimal disputedAmount;
    private final String reason;
    private final LocalDateTime date;
    private DisputeStatus status;
    private String adminDecisionReason;

    public Dispute(String transactionId, String vehicleRegistrationNumber, BigDecimal disputedAmount, String reason, LocalDateTime date) {
        this.disputeId = UUID.randomUUID().toString();
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction ID is required.");
        this.vehicleRegistrationNumber = Objects.requireNonNull(vehicleRegistrationNumber, "Vehicle registration is required.").trim().toUpperCase();
        this.disputedAmount = Objects.requireNonNull(disputedAmount, "Disputed amount is required.");
        this.reason = Objects.requireNonNull(reason, "Reason is required.");
        this.date = Objects.requireNonNull(date, "Dispute date is required.");
        this.status = DisputeStatus.PENDING;
    }

    public String getDisputeId() { return disputeId; }
    public String getTransactionId() { return transactionId; }
    public String getVehicleRegistrationNumber() { return vehicleRegistrationNumber; }
    public BigDecimal getDisputedAmount() { return disputedAmount; }
    public String getReason() { return reason; }
    public LocalDateTime getDate() { return date; }
    public DisputeStatus getStatus() { return status; }
    public String getAdminDecisionReason() { return adminDecisionReason; }

    public void approve(String decisionReason) {
        this.status = DisputeStatus.APPROVED;
        this.adminDecisionReason = decisionReason;
    }

    public void reject(String decisionReason) {
        this.status = DisputeStatus.REJECTED;
        this.adminDecisionReason = decisionReason;
    }

    @Override
    public String toString() {
        return "Dispute [" + disputeId + "] Tx: " + transactionId + " | Vehicle: " + vehicleRegistrationNumber
                + " | Amount: Rs." + disputedAmount.toPlainString() + " | Reason: " + reason
                + " | Status: " + status + " | Date: " + date;
    }
}
