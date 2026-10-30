package tollmanagement.service;

import tollmanagement.model.*;
import tollmanagement.repository.TollTransactionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ReturnTripService {
    public static final Duration RETURN_TRIP_WINDOW = Duration.ofHours(24);

    private final TollTransactionRepository transactionRepository;

    public ReturnTripService(TollTransactionRepository transactionRepository) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "Transaction repository is required.");
    }

    /**
     * Checks if a vehicle is returning to the same plaza within 24 hours of its previous successful crossing.
     */
    public boolean isEligibleForReturnDiscount(String vehicleReg, String plazaId, LocalDateTime crossingTime) {
        if (vehicleReg == null || plazaId == null || crossingTime == null) return false;
        String reg = vehicleReg.trim().toUpperCase();
        String plaza = plazaId.trim();

        List<TollTransaction> transactions = transactionRepository.findByVehicleRegistration(reg);

        Optional<TollTransaction> lastCrossing = transactions.stream()
                .filter(t -> t.getPlazaId().equalsIgnoreCase(plaza))
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> !t.getTimestamp().isAfter(crossingTime))
                .max(Comparator.comparing(TollTransaction::getTimestamp));

        if (lastCrossing.isEmpty()) return false;

        Duration duration = Duration.between(lastCrossing.get().getTimestamp(), crossingTime);
        return !duration.isNegative() && duration.compareTo(RETURN_TRIP_WINDOW) <= 0;
    }
}
