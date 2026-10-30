package tollmanagement.repository;

import tollmanagement.model.Vehicle;
import java.util.*;

public final class VehicleRepository {
    private final Map<String, Vehicle> vehiclesByRegNumber = new LinkedHashMap<>();

    public void save(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle is required.");
        String key = vehicle.getRegistrationNumber().toUpperCase();
        if (vehiclesByRegNumber.containsKey(key)) {
            throw new IllegalArgumentException("Vehicle with registration number " + vehicle.getRegistrationNumber() + " already exists.");
        }
        vehiclesByRegNumber.put(key, vehicle);
    }

    public Optional<Vehicle> findByRegistrationNumber(String regNumber) {
        if (regNumber == null) return Optional.empty();
        return Optional.ofNullable(vehiclesByRegNumber.get(regNumber.trim().toUpperCase()));
    }

    public Vehicle requireByRegistrationNumber(String regNumber) {
        return findByRegistrationNumber(regNumber)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + regNumber));
    }

    public List<Vehicle> findAll() {
        return List.copyOf(vehiclesByRegNumber.values());
    }
}
