package tollmanagement.service;

import tollmanagement.model.*;
import tollmanagement.repository.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DisputeAndRefundService {
    public static final Duration DISPUTE_VALID_WINDOW = Duration.ofDays(7);

    private final DisputeRepository disputeRepository;
    private final RefundRepository refundRepository;
    private final TollTransactionRepository transactionRepository;
    private final FASTagAccountRepository accountRepository;

    public DisputeAndRefundService(DisputeRepository disputeRepository,
                                   RefundRepository refundRepository,
                                   TollTransactionRepository transactionRepository,
                                   FASTagAccountRepository accountRepository) {
        this.disputeRepository = Objects.requireNonNull(disputeRepository, "Dispute repository is required.");
        this.refundRepository = Objects.requireNonNull(refundRepository, "Refund repository is required.");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "Transaction repository is required.");
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository is required.");
    }

    /**
     * Vehicle owner raises a dispute within 7 days of the transaction.
     */
    public Dispute raiseDispute(String transactionId, String reason, LocalDateTime disputeTime) {
        Objects.requireNonNull(disputeTime, "Dispute time is required.");
        Objects.requireNonNull(reason, "Reason is required.");

        TollTransaction tx = transactionRepository.requireById(transactionId);

        // Check if dispute already exists for this transaction
        if (disputeRepository.findByTransactionId(transactionId).isPresent()) {
            throw new IllegalArgumentException("A dispute has already been filed for transaction: " + transactionId);
        }

        // Must be raised within 7 days
        Duration elapsed = Duration.between(tx.getTimestamp(), disputeTime);
        if (elapsed.isNegative() || elapsed.compareTo(DISPUTE_VALID_WINDOW) > 0) {
            throw new IllegalArgumentException("Dispute rejected: Disputes must be raised within 7 days of transaction. Elapsed: "
                    + elapsed.toDays() + " days.");
        }

        Dispute dispute = new Dispute(transactionId, tx.getVehicleRegistrationNumber(), tx.getAmountCharged(), reason, disputeTime);
        disputeRepository.save(dispute);
        return dispute;
    }

    /**
     * Admin reviews and approves dispute, issuing an immediate refund back to FASTag balance.
     */
    public RefundRecord approveDispute(Admin admin, String disputeId, String adminReason, LocalDateTime refundTime) {
        TollRateService.requireAdmin(admin);
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        if (dispute.getStatus() != DisputeStatus.PENDING) {
            throw new IllegalStateException("Dispute has already been processed with status: " + dispute.getStatus());
        }

        if (refundRepository.hasRefund(dispute.getTransactionId())) {
            throw new IllegalStateException("A refund has already been granted for transaction: " + dispute.getTransactionId());
        }

        TollTransaction tx = transactionRepository.requireById(dispute.getTransactionId());
        BigDecimal refundAmount = dispute.getDisputedAmount();

        // Never refund more than the original deduction
        if (refundAmount.compareTo(tx.getAmountCharged()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed original charged amount of Rs. " + tx.getAmountCharged());
        }

        // Credit to FASTag account if available
        String tagId = tx.getTagId();
        String accountId = "CASH_TRANSACTION";
        if (tagId != null) {
            Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(tagId);
            if (accountOpt.isPresent()) {
                FASTagAccount account = accountOpt.get();
                accountId = account.getAccountId();
                // Refund adds back to balance
                account.recharge(refundAmount);
            }
        }

        dispute.approve(adminReason);

        RefundRecord refund = new RefundRecord(disputeId, tx.getTransactionId(), accountId, refundAmount, refundTime, adminReason);
        refundRepository.save(refund);
        return refund;
    }

    /**
     * Admin rejects dispute.
     */
    public void rejectDispute(Admin admin, String disputeId, String adminReason) {
        TollRateService.requireAdmin(admin);
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        if (dispute.getStatus() != DisputeStatus.PENDING) {
            throw new IllegalStateException("Dispute has already been processed with status: " + dispute.getStatus());
        }

        dispute.reject(adminReason);
    }

    public List<Dispute> listAllDisputes() {
        return disputeRepository.findAll();
    }

    public List<RefundRecord> listAllRefunds() {
        return refundRepository.findAll();
    }

    public boolean isTransactionRefunded(String transactionId) {
        return refundRepository.hasRefund(transactionId);
    }
}
