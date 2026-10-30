package tollmanagement.repository;

import tollmanagement.model.RefundRecord;

import java.util.*;

public final class RefundRepository {
    private final Map<String, RefundRecord> refundsById = new LinkedHashMap<>();

    public synchronized void save(RefundRecord refund) {
        Objects.requireNonNull(refund, "Refund record is required.");
        // Prevent duplicate refunds for the same transaction
        boolean alreadyRefunded = refundsById.values().stream()
                .anyMatch(r -> r.getTransactionId().equalsIgnoreCase(refund.getTransactionId()));
        if (alreadyRefunded) {
            throw new IllegalArgumentException("A refund has already been processed for transaction: " + refund.getTransactionId());
        }
        refundsById.put(refund.getRefundId(), refund);
    }

    public synchronized Optional<RefundRecord> findById(String refundId) {
        if (refundId == null) return Optional.empty();
        return Optional.ofNullable(refundsById.get(refundId));
    }

    public synchronized Optional<RefundRecord> findByTransactionId(String transactionId) {
        if (transactionId == null) return Optional.empty();
        return refundsById.values().stream()
                .filter(r -> r.getTransactionId().equalsIgnoreCase(transactionId))
                .findFirst();
    }

    public synchronized boolean hasRefund(String transactionId) {
        if (transactionId == null) return false;
        return refundsById.values().stream()
                .anyMatch(r -> r.getTransactionId().equalsIgnoreCase(transactionId));
    }

    public synchronized List<RefundRecord> findAll() {
        return List.copyOf(refundsById.values());
    }
}
