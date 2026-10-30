# Module 5: Recharge, Disputes, Refunds & Reports

## Responsibilities
- **Recharge Abstraction**: Simulates payment methods (UPI, Card, Net Banking) via `PaymentStrategy` polymorphism while upholding Module 2 financial limits (Min ₹100, Max ₹10,000, Balance cap ₹50,000).
- **Dispute Management**: Allows vehicle owners to dispute deductions within 7 days of transaction occurrence.
- **Refund Processing**: Allows administrators to review, approve, or reject disputes. Approved refunds credit the vehicle's FASTag balance immediately and prevent duplicate refunds.
- **Low-Balance Alerts**: Generates automatic warnings if an account balance drops below ₹200 after a successful transit deduction.
- **Daily Operational & Revenue Reports**: Produces daily metrics per plaza including total crossings, successful vs. failed crossings, crossings per lane, revenue by vehicle type, cash vs. FASTag split, and net revenue strictly excluding refunded transactions.

## Key Classes & Enums
- `PaymentStrategy`, `UpiPaymentStrategy`, `CardPaymentStrategy`, `NetBankingPaymentStrategy`: Polymorphic payment interfaces.
- `PaymentMethod`: `UPI`, `CARD`, `NET_BANKING`.
- `RechargeRecord` & `RechargeStatus`: `PAID`, `FAILED`, `REFUNDED`.
- `Dispute` & `DisputeStatus`: `PENDING`, `APPROVED`, `REJECTED`.
- `RefundRecord`: Immutable refund journal.
- `RechargeRepository`, `DisputeRepository`, `RefundRepository`: Thread-safe repositories.
- `RechargeService`, `DisputeAndRefundService`, `BalanceAlertService`, `RevenueReportService`: Business services.

## Compilation & Verification
```bash
# Compile and run test suite
javac -cp "../Module-1;../Module-2;../Module-3;../Module-4" -d out tollmanagement/model/*.java tollmanagement/repository/*.java tollmanagement/service/*.java Module5Verification.java
java -cp "out;../Module-1;../Module-2;../Module-3;../Module-4" Module5Verification
```
