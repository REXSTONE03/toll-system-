package tollmanagement.service;

import tollmanagement.model.*;
import tollmanagement.repository.FASTagAccountRepository;
import tollmanagement.repository.VehicleRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class VehicleService {
    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "Vehicle repository is required.");
    }

    public Vehicle registerVehicle(Admin admin, String registrationNumber, String ownerName, String phoneNumber, VehicleType vehicleType) {
        TollRateService.requireAdmin(admin);
        Vehicle vehicle = new Vehicle(registrationNumber, ownerName, phoneNumber, vehicleType);
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    public Vehicle getVehicle(String registrationNumber) {
        return vehicleRepository.requireByRegistrationNumber(registrationNumber);
    }

    public Optional<Vehicle> findVehicle(String registrationNumber) {
        return vehicleRepository.findByRegistrationNumber(registrationNumber);
    }

    public List<Vehicle> listVehicles() {
        return vehicleRepository.findAll();
    }
}
