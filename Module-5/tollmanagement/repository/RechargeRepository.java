package tollmanagement.repository;

import tollmanagement.model.RechargeRecord;

import java.util.*;

public final class RechargeRepository {
    private final Map<String, RechargeRecord> rechargesById = new LinkedHashMap<>();

    public synchronized void save(RechargeRecord record) {
        Objects.requireNonNull(record, "Recharge record is required.");
        rechargesById.put(record.getRechargeId(), record);
    }

    public synchronized Optional<RechargeRecord> findById(String rechargeId) {
        if (rechargeId == null) return Optional.empty();
        return Optional.ofNullable(rechargesById.get(rechargeId));
    }

    public synchronized List<RechargeRecord> findAll() {
        return List.copyOf(rechargesById.values());
    }

    public synchronized List<RechargeRecord> findByAccountId(String accountId) {
        if (accountId == null) return List.of();
        return rechargesById.values().stream()
                .filter(r -> r.getAccountId().equalsIgnoreCase(accountId))
                .toList();
    }
}
