package tollmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class RechargeRecord {
    private final String rechargeId;
    private final String accountId;
    private final BigDecimal amount;
    private final PaymentMethod paymentMethod;
    private final LocalDateTime timestamp;
    private RechargeStatus status;

    public RechargeRecord(String accountId, BigDecimal amount, PaymentMethod paymentMethod, LocalDateTime timestamp, RechargeStatus status) {
        this.rechargeId = UUID.randomUUID().toString();
        this.accountId = Objects.requireNonNull(accountId, "Account ID is required.");
        this.amount = Objects.requireNonNull(amount, "Amount is required.");
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method is required.");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp is required.");
        this.status = Objects.requireNonNull(status, "Status is required.");
    }

    public String getRechargeId() { return rechargeId; }
    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public RechargeStatus getStatus() { return status; }

    public void setStatus(RechargeStatus status) {
        this.status = Objects.requireNonNull(status, "Status is required.");
    }

    @Override
    public String toString() {
        return "Recharge [" + rechargeId + "] Account: " + accountId + " | Amount: Rs." + amount.toPlainString()
                + " | Method: " + paymentMethod + " | Status: " + status + " | Time: " + timestamp;
    }
}
