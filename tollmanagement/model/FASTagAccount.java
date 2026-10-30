package tollmanagement.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public final class FASTagAccount {
    public static final BigDecimal MIN_RECHARGE = new BigDecimal("100.00");
    public static final BigDecimal MAX_RECHARGE = new BigDecimal("10000.00");
    public static final BigDecimal MAX_BALANCE = new BigDecimal("50000.00");
    public static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("100.00");

    private final String accountId;
    private final String tagId;
    private final String vehicleRegistrationNumber;
    private BigDecimal balance;
    private FASTagStatus status;

    public FASTagAccount(String tagId, String vehicleRegistrationNumber, BigDecimal initialBalance) {
        this.accountId = UUID.randomUUID().toString();
        this.tagId = sanitizeTag(tagId);
        this.vehicleRegistrationNumber = sanitizeVehicleNumber(vehicleRegistrationNumber);

        Objects.requireNonNull(initialBalance, "Initial balance is required.");
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        if (initialBalance.compareTo(MAX_BALANCE) > 0) {
            throw new IllegalArgumentException("Initial balance cannot exceed maximum balance of Rs. " + MAX_BALANCE);
        }
        this.balance = initialBalance;

        // If initial balance is at least threshold, ACTIVE; otherwise BLOCKED
        if (this.balance.compareTo(LOW_BALANCE_THRESHOLD) >= 0) {
            this.status = FASTagStatus.ACTIVE;
        } else {
            this.status = FASTagStatus.BLOCKED;
        }
    }

    private static String sanitizeTag(String tagId) {
        if (tagId == null || tagId.isBlank()) {
            throw new IllegalArgumentException("Tag ID cannot be blank.");
        }
        return tagId.trim().toUpperCase();
    }

    private static String sanitizeVehicleNumber(String vehicleNumber) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("Vehicle registration number cannot be blank.");
        }
        return vehicleNumber.trim().toUpperCase();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getTagId() {
        return tagId;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public FASTagStatus getStatus() {
        return status;
    }

    public void setStatus(FASTagStatus status) {
        this.status = Objects.requireNonNull(status, "Status is required.");
    }

    public void recharge(BigDecimal amount) {
        if (status == FASTagStatus.CLOSED) {
            throw new IllegalStateException("Cannot recharge a CLOSED FASTag account.");
        }
        if (status == FASTagStatus.BLACKLISTED) {
            throw new IllegalStateException("Cannot recharge a BLACKLISTED FASTag account.");
        }
        Objects.requireNonNull(amount, "Recharge amount is required.");
        if (amount.compareTo(MIN_RECHARGE) < 0) {
            throw new IllegalArgumentException("Minimum recharge amount is Rs. " + MIN_RECHARGE);
        }
        if (amount.compareTo(MAX_RECHARGE) > 0) {
            throw new IllegalArgumentException("Maximum recharge amount per transaction is Rs. " + MAX_RECHARGE);
        }
        BigDecimal newBalance = this.balance.add(amount);
        if (newBalance.compareTo(MAX_BALANCE) > 0) {
            throw new IllegalArgumentException("Recharge would exceed maximum allowed balance of Rs. " + MAX_BALANCE
                    + ". Current balance: Rs. " + balance);
        }
        this.balance = newBalance;

        // Automatically reactivate a BLOCKED account if balance reaches threshold
        if (this.status == FASTagStatus.BLOCKED && this.balance.compareTo(LOW_BALANCE_THRESHOLD) >= 0) {
            this.status = FASTagStatus.ACTIVE;
        }
    }

    public void deduct(BigDecimal amount) {
        Objects.requireNonNull(amount, "Deduction amount is required.");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Deduction amount cannot be negative.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance. Current balance: Rs. " + balance + ", needed: Rs. " + amount);
        }
        this.balance = this.balance.subtract(amount);

        // Automatically block account if balance falls below threshold
        if (this.status == FASTagStatus.ACTIVE && this.balance.compareTo(LOW_BALANCE_THRESHOLD) < 0) {
            this.status = FASTagStatus.BLOCKED;
        }
    }

    public void block() {
        if (this.status == FASTagStatus.CLOSED) {
            throw new IllegalStateException("Cannot block a closed account.");
        }
        this.status = FASTagStatus.BLOCKED;
    }

    public void blacklist() {
        this.status = FASTagStatus.BLACKLISTED;
    }

    public void close() {
        this.status = FASTagStatus.CLOSED;
    }

    @Override
    public String toString() {
        return "Account ID: " + accountId + " | Tag: " + tagId + " | Vehicle: " + vehicleRegistrationNumber
                + " | Balance: Rs. " + balance.toPlainString() + " | Status: " + status;
    }
}
