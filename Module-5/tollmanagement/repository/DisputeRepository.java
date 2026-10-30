package tollmanagement.repository;

import tollmanagement.model.Dispute;

import java.util.*;

public final class DisputeRepository {
    private final Map<String, Dispute> disputesById = new LinkedHashMap<>();

    public synchronized void save(Dispute dispute) {
        Objects.requireNonNull(dispute, "Dispute is required.");
        disputesById.put(dispute.getDisputeId(), dispute);
    }

    public synchronized Optional<Dispute> findById(String disputeId) {
        if (disputeId == null) return Optional.empty();
        return Optional.ofNullable(disputesById.get(disputeId));
    }

    public synchronized Optional<Dispute> findByTransactionId(String transactionId) {
        if (transactionId == null) return Optional.empty();
        return disputesById.values().stream()
                .filter(d -> d.getTransactionId().equalsIgnoreCase(transactionId))
                .findFirst();
    }

    public synchronized List<Dispute> findAll() {
        return List.copyOf(disputesById.values());
    }

    public synchronized List<Dispute> findByVehicle(String vehicleReg) {
        if (vehicleReg == null) return List.of();
        String reg = vehicleReg.trim().toUpperCase();
        return disputesById.values().stream()
                .filter(d -> d.getVehicleRegistrationNumber().equalsIgnoreCase(reg))
                .toList();
    }
}
