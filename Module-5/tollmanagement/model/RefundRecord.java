package tollmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class RefundRecord {
    private final String refundId;
    private final String disputeId;
    private final String transactionId;
    private final String accountId;
    private final BigDecimal refundAmount;
    private final LocalDateTime timestamp;
    private final String reason;

    public RefundRecord(String disputeId, String transactionId, String accountId, BigDecimal refundAmount, LocalDateTime timestamp, String reason) {
        this.refundId = UUID.randomUUID().toString();
        this.disputeId = disputeId;
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction ID is required.");
        this.accountId = Objects.requireNonNull(accountId, "Account ID is required.");
        this.refundAmount = Objects.requireNonNull(refundAmount, "Refund amount is required.");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp is required.");
        this.reason = reason;
    }

    public String getRefundId() { return refundId; }
    public String getDisputeId() { return disputeId; }
    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return "Refund [" + refundId + "] Tx: " + transactionId + " | Account: " + accountId
                + " | Amount: Rs." + refundAmount.toPlainString() + " | Time: " + timestamp;
    }
}
