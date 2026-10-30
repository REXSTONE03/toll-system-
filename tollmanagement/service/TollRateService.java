package tollmanagement.service;

import tollmanagement.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class TollRateService {
    private final Map<String, List<TollRate>> ratesByPlazaId = new HashMap<>();

    public TollRate setRate(Admin admin, TollPlaza plaza, VehicleType vehicleType,
                            TripType tripType, BigDecimal amount, LocalDate effectiveDate) {
        requireAdmin(admin);
        Objects.requireNonNull(plaza, "Plaza is required.");
        TollRate rate = new TollRate(vehicleType, tripType, amount, effectiveDate);
        List<TollRate> rates = ratesByPlazaId.computeIfAbsent(plaza.getId(), ignored -> new ArrayList<>());
        rates.removeIf(existing -> existing.getVehicleType() == vehicleType
            && existing.getTripType() == tripType && existing.getEffectiveDate().equals(effectiveDate));
        rates.add(rate);
        return rate;
    }

    public Optional<TollRate> findRate(TollPlaza plaza, VehicleType vehicleType, TripType tripType, LocalDate onDate) {
        Objects.requireNonNull(plaza);
        return ratesByPlazaId.getOrDefault(plaza.getId(), List.of()).stream()
            .filter(r -> r.getVehicleType() == vehicleType && r.getTripType() == tripType)
            .filter(r -> !r.getEffectiveDate().isAfter(onDate))
            .max(Comparator.comparing(TollRate::getEffectiveDate));
    }
    public List<TollRate> getRates(TollPlaza plaza) {
        return List.copyOf(ratesByPlazaId.getOrDefault(plaza.getId(), List.of()));
    }
    public boolean hasRatesForEveryVehicleType(TollPlaza plaza, LocalDate onDate) {
        for (VehicleType vehicle : VehicleType.values())
            for (TripType trip : TripType.values())
                if (findRate(plaza, vehicle, trip, onDate).isEmpty()) return false;
        return true;
    }
    public static void requireAdmin(Admin admin) {
        if (admin == null) throw new SecurityException("Only an Admin can perform this action.");
    }
}
