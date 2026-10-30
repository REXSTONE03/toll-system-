package tollmanagement.service;

import tollmanagement.model.FASTagAccount;

import java.math.BigDecimal;
import java.util.Optional;

public final class BalanceAlertService {
    public static final BigDecimal LOW_BALANCE_ALERT_THRESHOLD = new BigDecimal("200.00");

    private BalanceAlertService() {}

    /**
     * Checks whether an alert should be raised after a successful transaction.
     * Returns alert message if balance < 200, or Optional.empty().
     */
    public static Optional<String> checkLowBalance(FASTagAccount account) {
        if (account == null) return Optional.empty();
        if (account.getBalance().compareTo(LOW_BALANCE_ALERT_THRESHOLD) < 0) {
            return Optional.of("LOW BALANCE WARNING: Account balance is Rs. "
                    + account.getBalance().toPlainString() + " (below threshold Rs. " + LOW_BALANCE_ALERT_THRESHOLD + "). Please recharge promptly.");
        }
        return Optional.empty();
    }
}
