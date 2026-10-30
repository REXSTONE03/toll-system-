package tollmanagement.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class MonthlyPass {
    private final String passId;
    private final String vehicleRegistrationNumber;
    private final String plazaId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private boolean active;

    public MonthlyPass(String vehicleRegistrationNumber, String plazaId, LocalDate startDate, LocalDate endDate) {
        this.passId = UUID.randomUUID().toString();
        this.vehicleRegistrationNumber = Objects.requireNonNull(vehicleRegistrationNumber, "Vehicle registration is required.").trim().toUpperCase();
        this.plazaId = Objects.requireNonNull(plazaId, "Plaza ID is required.").trim();
        this.startDate = Objects.requireNonNull(startDate, "Start date is required.");
        this.endDate = Objects.requireNonNull(endDate, "End date is required.");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Pass end date cannot be before start date.");
        }
        this.active = true;
    }

    public String getPassId() { return passId; }
    public String getVehicleRegistrationNumber() { return vehicleRegistrationNumber; }
    public String getPlazaId() { return plazaId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isActive() { return active; }

    public void deactivate() { this.active = false; }

    public boolean isValidOn(LocalDate date) {
        if (!active || date == null) return false;
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    @Override
    public String toString() {
        return "MonthlyPass [" + passId + "] Vehicle: " + vehicleRegistrationNumber + " | Plaza: " + plazaId
                + " | Valid: " + startDate + " to " + endDate + " | Status: " + (active ? "ACTIVE" : "INACTIVE");
    }
}
