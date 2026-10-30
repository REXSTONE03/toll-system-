package tollmanagement.model;

import java.math.BigDecimal;

public final class NetBankingPaymentStrategy implements PaymentStrategy {
    private final boolean simulateSuccess;

    public NetBankingPaymentStrategy() {
        this(true);
    }

    public NetBankingPaymentStrategy(boolean simulateSuccess) {
        this.simulateSuccess = simulateSuccess;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.NET_BANKING;
    }

    @Override
    public boolean processPayment(String accountId, BigDecimal amount) {
        // Simulated Net Banking gateway
        return simulateSuccess;
    }
}
