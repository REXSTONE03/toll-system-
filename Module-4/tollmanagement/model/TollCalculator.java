package tollmanagement.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TollCalculator {
    public static final BigDecimal RETURN_DISCOUNT_PERCENT = new BigDecimal("0.50"); // 50% discount

    private TollCalculator() {}

    /**
     * Applies a 50% return-trip discount to the single-trip rate.
     */
    public static BigDecimal applyReturnTripDiscount(BigDecimal singleTripRate) {
        if (singleTripRate == null) return BigDecimal.ZERO;
        return singleTripRate.multiply(RETURN_DISCOUNT_PERCENT);
    }

    /**
     * Rounds toll amount to the nearest 5 rupees.
     * Rounding rule: If amount <= 0, returns 0.
     * Divides by 5, rounds half-up to whole integer, then multiplies by 5.
     */
    public static BigDecimal roundToNearestFive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal divided = amount.divide(new BigDecimal("5"), 0, RoundingMode.HALF_UP);
        return divided.multiply(new BigDecimal("5")).setScale(2, RoundingMode.HALF_UP);
    }
}
