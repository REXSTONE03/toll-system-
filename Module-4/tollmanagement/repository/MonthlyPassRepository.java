package tollmanagement.repository;

import tollmanagement.model.MonthlyPass;

import java.time.LocalDate;
import java.util.*;

public final class MonthlyPassRepository {
    private final Map<String, MonthlyPass> passesById = new LinkedHashMap<>();

    public synchronized void save(MonthlyPass pass) {
        Objects.requireNonNull(pass, "Monthly pass is required.");
        // Prevent multiple simultaneously valid active passes for the same vehicle and plaza
        boolean existsConflict = passesById.values().stream()
                .filter(MonthlyPass::isActive)
                .filter(p -> p.getVehicleRegistrationNumber().equalsIgnoreCase(pass.getVehicleRegistrationNumber()))
                .filter(p -> p.getPlazaId().equalsIgnoreCase(pass.getPlazaId()))
                .anyMatch(existing -> {
                    // Check date overlap
                    boolean noOverlap = pass.getEndDate().isBefore(existing.getStartDate()) || pass.getStartDate().isAfter(existing.getEndDate());
                    return !noOverlap;
                });

        if (existsConflict) {
            throw new IllegalArgumentException("An active overlapping monthly pass already exists for vehicle "
                    + pass.getVehicleRegistrationNumber() + " at plaza " + pass.getPlazaId());
        }
        passesById.put(pass.getPassId(), pass);
    }

    public synchronized Optional<MonthlyPass> findById(String passId) {
        if (passId == null) return Optional.empty();
        return Optional.ofNullable(passesById.get(passId));
    }

    public synchronized Optional<MonthlyPass> findValidPass(String vehicleReg, String plazaId, LocalDate onDate) {
        if (vehicleReg == null || plazaId == null || onDate == null) return Optional.empty();
        String reg = vehicleReg.trim().toUpperCase();
        String plaza = plazaId.trim();
        return passesById.values().stream()
                .filter(MonthlyPass::isActive)
                .filter(p -> p.getVehicleRegistrationNumber().equalsIgnoreCase(reg))
                .filter(p -> p.getPlazaId().equalsIgnoreCase(plaza))
                .filter(p -> p.isValidOn(onDate))
                .findFirst();
    }

    public synchronized List<MonthlyPass> findAll() {
        return List.copyOf(passesById.values());
    }

    public synchronized List<MonthlyPass> findByVehicle(String vehicleReg) {
        if (vehicleReg == null) return List.of();
        String reg = vehicleReg.trim().toUpperCase();
        return passesById.values().stream()
                .filter(p -> p.getVehicleRegistrationNumber().equalsIgnoreCase(reg))
                .toList();
    }
}
