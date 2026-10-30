package tollmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class TollRate {
    private final String id;
    private final VehicleType vehicleType;
    private final TripType tripType;
    private final BigDecimal amount;
    private final LocalDate effectiveDate;

    public TollRate(VehicleType vehicleType, TripType tripType, BigDecimal amount, LocalDate effectiveDate) {
        this.id = UUID.randomUUID().toString();
        this.vehicleType = Objects.requireNonNull(vehicleType, "Vehicle type is required.");
        this.tripType = Objects.requireNonNull(tripType, "Trip type is required.");
        this.amount = Objects.requireNonNull(amount, "Rate amount is required.");
        this.effectiveDate = Objects.requireNonNull(effectiveDate, "Effective date is required.");
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Toll rate cannot be negative.");
    }
    public String getId() { return id; }
    public VehicleType getVehicleType() { return vehicleType; }
    public TripType getTripType() { return tripType; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    @Override public String toString() {
        return vehicleType + " / " + tripType + " = Rs." + amount.toPlainString() + " (from " + effectiveDate + ")";
    }
}
