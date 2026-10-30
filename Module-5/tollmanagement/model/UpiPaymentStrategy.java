package tollmanagement.model;

import java.math.BigDecimal;

public final class UpiPaymentStrategy implements PaymentStrategy {
    private final boolean simulateSuccess;

    public UpiPaymentStrategy() {
        this(true);
    }

    public UpiPaymentStrategy(boolean simulateSuccess) {
        this.simulateSuccess = simulateSuccess;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.UPI;
    }

    @Override
    public boolean processPayment(String accountId, BigDecimal amount) {
        // Simulated UPI gateway integration
        return simulateSuccess;
    }
}
