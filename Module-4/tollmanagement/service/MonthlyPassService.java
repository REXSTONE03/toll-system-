package tollmanagement.service;

import tollmanagement.model.Admin;
import tollmanagement.model.MonthlyPass;
import tollmanagement.model.VehicleType;
import tollmanagement.repository.MonthlyPassRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class MonthlyPassService {
    private final MonthlyPassRepository passRepository;
    private final Map<VehicleType, BigDecimal> passFeesByVehicleType = new EnumMap<>(VehicleType.class);

    public MonthlyPassService(MonthlyPassRepository passRepository) {
        this.passRepository = Objects.requireNonNull(passRepository, "Pass repository is required.");
        // Set default pass fees
        passFeesByVehicleType.put(VehicleType.CAR, new BigDecimal("3000.00"));
        passFeesByVehicleType.put(VehicleType.VAN, new BigDecimal("4000.00"));
        passFeesByVehicleType.put(VehicleType.BUS, new BigDecimal("6000.00"));
        passFeesByVehicleType.put(VehicleType.TRUCK, new BigDecimal("8000.00"));
        passFeesByVehicleType.put(VehicleType.MULTI_AXLE, new BigDecimal("12000.00"));
    }

    public void setPassFee(Admin admin, VehicleType vehicleType, BigDecimal fee) {
        TollRateService.requireAdmin(admin);
        Objects.requireNonNull(vehicleType, "Vehicle type is required.");
        Objects.requireNonNull(fee, "Fee is required.");
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Pass fee cannot be negative.");
        }
        passFeesByVehicleType.put(vehicleType, fee);
    }

    public BigDecimal getPassFee(VehicleType vehicleType) {
        return passFeesByVehicleType.getOrDefault(vehicleType, new BigDecimal("3000.00"));
    }

    public MonthlyPass createMonthlyPass(Admin admin, String vehicleReg, String plazaId, LocalDate startDate, LocalDate endDate) {
        TollRateService.requireAdmin(admin);
        MonthlyPass pass = new MonthlyPass(vehicleReg, plazaId, startDate, endDate);
        passRepository.save(pass);
        return pass;
    }

    public Optional<MonthlyPass> findValidPass(String vehicleReg, String plazaId, LocalDate onDate) {
        return passRepository.findValidPass(vehicleReg, plazaId, onDate);
    }

    public List<MonthlyPass> listPasses() {
        return passRepository.findAll();
    }

    public List<MonthlyPass> listPassesForVehicle(String vehicleReg) {
        return passRepository.findByVehicle(vehicleReg);
    }
}
