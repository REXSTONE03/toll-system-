package tollmanagement.repository;

import tollmanagement.model.TollTransaction;

import java.util.*;

public final class TollTransactionRepository {
    private final Map<String, TollTransaction> transactionsById = new LinkedHashMap<>();

    public synchronized void save(TollTransaction transaction) {
        Objects.requireNonNull(transaction, "Transaction is required.");
        transactionsById.put(transaction.getTransactionId(), transaction);
    }

    public synchronized Optional<TollTransaction> findById(String transactionId) {
        if (transactionId == null) return Optional.empty();
        return Optional.ofNullable(transactionsById.get(transactionId));
    }

    public synchronized TollTransaction requireById(String transactionId) {
        return findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
    }

    public synchronized List<TollTransaction> findAll() {
        return List.copyOf(transactionsById.values());
    }

    public synchronized List<TollTransaction> findByPlazaId(String plazaId) {
        if (plazaId == null) return List.of();
        return transactionsById.values().stream()
                .filter(t -> t.getPlazaId().equalsIgnoreCase(plazaId.trim()))
                .toList();
    }

    public synchronized List<TollTransaction> findByVehicleRegistration(String regNumber) {
        if (regNumber == null) return List.of();
        String normalized = regNumber.trim().toUpperCase();
        return transactionsById.values().stream()
                .filter(t -> t.getVehicleRegistrationNumber().equalsIgnoreCase(normalized))
                .toList();
    }

    public synchronized List<TollTransaction> findByTagId(String tagId) {
        if (tagId == null) return List.of();
        String normalized = tagId.trim().toUpperCase();
        return transactionsById.values().stream()
                .filter(t -> t.getTagId() != null && t.getTagId().equalsIgnoreCase(normalized))
                .toList();
    }
}
