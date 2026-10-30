package tollmanagement.model;

import java.math.BigDecimal;

public final class CardPaymentStrategy implements PaymentStrategy {
    private final boolean simulateSuccess;

    public CardPaymentStrategy() {
        this(true);
    }

    public CardPaymentStrategy(boolean simulateSuccess) {
        this.simulateSuccess = simulateSuccess;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public boolean processPayment(String accountId, BigDecimal amount) {
        // Simulated Card payment gateway
        return simulateSuccess;
    }
}
