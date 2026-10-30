package tollmanagement.service;

import tollmanagement.model.*;
import tollmanagement.repository.FASTagAccountRepository;
import tollmanagement.repository.RechargeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class RechargeService {
    private final RechargeRepository rechargeRepository;
    private final FASTagAccountRepository accountRepository;

    public RechargeService(RechargeRepository rechargeRepository, FASTagAccountRepository accountRepository) {
        this.rechargeRepository = Objects.requireNonNull(rechargeRepository, "Recharge repository is required.");
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository is required.");
    }

    /**
     * Executes account recharge using the specified payment strategy.
     */
    public RechargeRecord processRecharge(String tagId, BigDecimal amount, PaymentStrategy paymentStrategy, LocalDateTime timestamp) {
        Objects.requireNonNull(paymentStrategy, "Payment strategy is required.");
        Objects.requireNonNull(timestamp, "Timestamp is required.");
        Objects.requireNonNull(amount, "Amount is required.");

        FASTagAccount account = accountRepository.requireByTagId(tagId);

        // Pre-validate limits before attempting payment
        if (amount.compareTo(FASTagAccount.MIN_RECHARGE) < 0) {
            throw new IllegalArgumentException("Minimum recharge amount is Rs. " + FASTagAccount.MIN_RECHARGE);
        }
        if (amount.compareTo(FASTagAccount.MAX_RECHARGE) > 0) {
            throw new IllegalArgumentException("Maximum recharge amount per transaction is Rs. " + FASTagAccount.MAX_RECHARGE);
        }
        if (account.getBalance().add(amount).compareTo(FASTagAccount.MAX_BALANCE) > 0) {
            throw new IllegalArgumentException("Recharge would exceed maximum allowed balance of Rs. " + FASTagAccount.MAX_BALANCE);
        }
        if (account.getStatus() == FASTagStatus.CLOSED) {
            throw new IllegalStateException("Cannot recharge a CLOSED FASTag account.");
        }
        if (account.getStatus() == FASTagStatus.BLACKLISTED) {
            throw new IllegalStateException("Cannot recharge a BLACKLISTED FASTag account.");
        }

        // Process through payment gateway abstraction
        boolean success = paymentStrategy.processPayment(account.getAccountId(), amount);

        if (success) {
            account.recharge(amount);
            RechargeRecord record = new RechargeRecord(
                    account.getAccountId(), amount, paymentStrategy.getPaymentMethod(), timestamp, RechargeStatus.PAID);
            rechargeRepository.save(record);
            return record;
        } else {
            // Failed payment: balance does NOT increase
            RechargeRecord failedRecord = new RechargeRecord(
                    account.getAccountId(), amount, paymentStrategy.getPaymentMethod(), timestamp, RechargeStatus.FAILED);
            rechargeRepository.save(failedRecord);
            return failedRecord;
        }
    }

    public List<RechargeRecord> getRechargesForAccount(String accountId) {
        return rechargeRepository.findByAccountId(accountId);
    }

    public List<RechargeRecord> getAllRecharges() {
        return rechargeRepository.findAll();
    }
}
