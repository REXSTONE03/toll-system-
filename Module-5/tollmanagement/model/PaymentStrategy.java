package tollmanagement.model;

import java.math.BigDecimal;

public interface PaymentStrategy {
    PaymentMethod getPaymentMethod();
    boolean processPayment(String accountId, BigDecimal amount);
}
