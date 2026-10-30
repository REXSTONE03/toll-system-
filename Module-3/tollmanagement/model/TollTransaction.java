package tollmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class TollTransaction {
    private final String transactionId;
    private final String vehicleRegistrationNumber;
    private final String tagId;
    private final String plazaId;
    private final int laneNumber;
    private final LocalDateTime timestamp;
    private final BigDecimal amountCharged;
    private final PaymentMode paymentMode;
    private final TransactionStatus status;
    private final VehicleType registeredVehicleType;
    private final VehicleType observedVehicleType;
    private final boolean flaggedForReview;
    private final String reviewReason;
    private final boolean barrierOpened;

    public TollTransaction(String vehicleRegistrationNumber,
                           String tagId,
                           String plazaId,
                           int laneNumber,
                           LocalDateTime timestamp,
                           BigDecimal amountCharged,
                           PaymentMode paymentMode,
                           TransactionStatus status,
                           VehicleType registeredVehicleType,
                           VehicleType observedVehicleType,
                           boolean flaggedForReview,
                           String reviewReason,
                           boolean barrierOpened) {
        this.transactionId = UUID.randomUUID().toString();
        this.vehicleRegistrationNumber = vehicleRegistrationNumber != null ? vehicleRegistrationNumber.trim().toUpperCase() : "UNKNOWN";
        this.tagId = tagId != null ? tagId.trim().toUpperCase() : null;
        this.plazaId = Objects.requireNonNull(plazaId, "Plaza ID is required.");
        this.laneNumber = laneNumber;
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp is required.");
        this.amountCharged = Objects.requireNonNull(amountCharged, "Amount charged is required.");
        this.paymentMode = Objects.requireNonNull(paymentMode, "Payment mode is required.");
        this.status = Objects.requireNonNull(status, "Transaction status is required.");
        this.registeredVehicleType = registeredVehicleType;
        this.observedVehicleType = Objects.requireNonNull(observedVehicleType, "Observed vehicle type is required.");
        this.flaggedForReview = flaggedForReview;
        this.reviewReason = reviewReason;
        this.barrierOpened = barrierOpened;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public String getTagId() {
        return tagId;
    }

    public String getPlazaId() {
        return plazaId;
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BigDecimal getAmountCharged() {
        return amountCharged;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public VehicleType getRegisteredVehicleType() {
        return registeredVehicleType;
    }

    public VehicleType getObservedVehicleType() {
        return observedVehicleType;
    }

    public boolean isFlaggedForReview() {
        return flaggedForReview;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public boolean isBarrierOpened() {
        return barrierOpened;
    }

    @Override
    public String toString() {
        return "Transaction [" + transactionId + "] Time: " + timestamp
                + " | Vehicle: " + vehicleRegistrationNumber
                + " | Tag: " + (tagId != null ? tagId : "N/A")
                + " | Plaza: " + plazaId + " | Lane: " + laneNumber
                + " | Mode: " + paymentMode + " | Status: " + status
                + " | Amount: Rs." + amountCharged.toPlainString()
                + " | Barrier: " + (barrierOpened ? "OPENED" : "CLOSED")
                + (flaggedForReview ? " | [FLAGGED: " + reviewReason + "]" : "");
    }
}
