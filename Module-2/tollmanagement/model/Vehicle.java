package tollmanagement.model;

import java.util.Objects;

public final class Vehicle {
    private final String registrationNumber;
    private final String ownerName;
    private final String phoneNumber;
    private final VehicleType vehicleType;
    private String activeTagId;

    public Vehicle(String registrationNumber, String ownerName, String phoneNumber, VehicleType vehicleType) {
        this.registrationNumber = sanitize(registrationNumber, "Vehicle registration number");
        this.ownerName = sanitize(ownerName, "Owner name");
        this.phoneNumber = sanitizePhone(phoneNumber);
        this.vehicleType = Objects.requireNonNull(vehicleType, "Vehicle type is required.");
    }

    private static String sanitize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank.");
        }
        return value.trim().toUpperCase();
    }

    private static String sanitizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be blank.");
        }
        String cleaned = phone.trim();
        if (!cleaned.matches("^[0-9+()\\-\\s]{7,15}$")) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }
        return cleaned;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getActiveTagId() {
        return activeTagId;
    }

    public boolean hasActiveTag() {
        return activeTagId != null && !activeTagId.isBlank();
    }

    public void linkActiveTag(String tagId) {
        if (hasActiveTag() && !this.activeTagId.equalsIgnoreCase(tagId)) {
            throw new IllegalStateException("Vehicle " + registrationNumber + " already has an active FASTag: " + this.activeTagId);
        }
        this.activeTagId = tagId.trim().toUpperCase();
    }

    public void unlinkActiveTag() {
        this.activeTagId = null;
    }

    @Override
    public String toString() {
        return "Vehicle [" + registrationNumber + "] Type: " + vehicleType + ", Owner: " + ownerName
                + " (" + phoneNumber + "), Active Tag: " + (activeTagId != null ? activeTagId : "None");
    }
}
