package tollmanagement.service;

import tollmanagement.model.*;
import tollmanagement.repository.TollTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class RevenueReportService {
    private final TollTransactionRepository transactionRepository;
    private final DisputeAndRefundService refundService;

    public RevenueReportService(TollTransactionRepository transactionRepository, DisputeAndRefundService refundService) {
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "Transaction repository is required.");
        this.refundService = Objects.requireNonNull(refundService, "Refund service is required.");
    }

    /**
     * Generates a daily comprehensive revenue and operational report for a specific toll plaza.
     */
    public DailyReport generateDailyReport(String plazaId, LocalDate date) {
        Objects.requireNonNull(plazaId, "Plaza ID is required.");
        Objects.requireNonNull(date, "Date is required.");

        List<TollTransaction> plazaTxs = transactionRepository.findByPlazaId(plazaId).stream()
                .filter(t -> t.getTimestamp().toLocalDate().equals(date))
                .toList();

        int totalCrossings = plazaTxs.size();
        int successfulCrossings = 0;
        int failedCrossings = 0;
        BigDecimal netRevenue = BigDecimal.ZERO;
        BigDecimal fastagCollection = BigDecimal.ZERO;
        BigDecimal cashCollection = BigDecimal.ZERO;

        Map<Integer, Integer> vehicleCountPerLane = new TreeMap<>();
        Map<VehicleType, BigDecimal> revenueByVehicleType = new EnumMap<>(VehicleType.class);

        for (TollTransaction tx : plazaTxs) {
            vehicleCountPerLane.merge(tx.getLaneNumber(), 1, Integer::sum);

            if (tx.getStatus() == TransactionStatus.SUCCESS) {
                successfulCrossings++;

                // Check if refunded
                boolean isRefunded = refundService.isTransactionRefunded(tx.getTransactionId());
                if (!isRefunded) {
                    BigDecimal amt = tx.getAmountCharged();
                    netRevenue = netRevenue.add(amt);

                    if (tx.getPaymentMode() == PaymentMode.FASTAG) {
                        fastagCollection = fastagCollection.add(amt);
                    } else if (tx.getPaymentMode() == PaymentMode.CASH) {
                        cashCollection = cashCollection.add(amt);
                    }

                    VehicleType vType = tx.getObservedVehicleType();
                    revenueByVehicleType.merge(vType, amt, BigDecimal::add);
                }
            } else if (tx.getStatus() != TransactionStatus.EXEMPTED) {
                failedCrossings++;
            }
        }

        return new DailyReport(
                plazaId, date, netRevenue, successfulCrossings, totalCrossings,
                failedCrossings, fastagCollection, cashCollection, vehicleCountPerLane, revenueByVehicleType);
    }

    public static final class DailyReport {
        private final String plazaId;
        private final LocalDate date;
        private final BigDecimal totalRevenue;
        private final int successfulTransactions;
        private final int totalCrossings;
        private final int failedTransactions;
        private final BigDecimal fastagCollection;
        private final BigDecimal cashCollection;
        private final Map<Integer, Integer> vehicleCountPerLane;
        private final Map<VehicleType, BigDecimal> revenueByVehicleType;

        public DailyReport(String plazaId, LocalDate date, BigDecimal totalRevenue, int successfulTransactions,
                           int totalCrossings, int failedTransactions, BigDecimal fastagCollection,
                           BigDecimal cashCollection, Map<Integer, Integer> vehicleCountPerLane,
                           Map<VehicleType, BigDecimal> revenueByVehicleType) {
            this.plazaId = plazaId;
            this.date = date;
            this.totalRevenue = totalRevenue;
            this.successfulTransactions = successfulTransactions;
            this.totalCrossings = totalCrossings;
            this.failedTransactions = failedTransactions;
            this.fastagCollection = fastagCollection;
            this.cashCollection = cashCollection;
            this.vehicleCountPerLane = vehicleCountPerLane;
            this.revenueByVehicleType = revenueByVehicleType;
        }

        public String getPlazaId() { return plazaId; }
        public LocalDate getDate() { return date; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public int getSuccessfulTransactions() { return successfulTransactions; }
        public int getTotalCrossings() { return totalCrossings; }
        public int getFailedTransactions() { return failedTransactions; }
        public BigDecimal getFastagCollection() { return fastagCollection; }
        public BigDecimal getCashCollection() { return cashCollection; }
        public Map<Integer, Integer> getVehicleCountPerLane() { return vehicleCountPerLane; }
        public Map<VehicleType, BigDecimal> getRevenueByVehicleType() { return revenueByVehicleType; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n======================================================\n");
            sb.append("DAILY REVENUE & OPERATIONAL REPORT\n");
            sb.append("Plaza ID: ").append(plazaId).append(" | Date: ").append(date).append("\n");
            sb.append("------------------------------------------------------\n");
            sb.append("1. Total Net Revenue: Rs. ").append(totalRevenue.toPlainString()).append("\n");
            sb.append("2. Total Successful Transactions: ").append(successfulTransactions).append("\n");
            sb.append("3. Total Vehicle Crossings: ").append(totalCrossings).append("\n");
            sb.append("4. Failed Transactions: ").append(failedTransactions).append("\n");
            sb.append("5. FASTag Collection: Rs. ").append(fastagCollection.toPlainString()).append("\n");
            sb.append("6. Cash Collection: Rs. ").append(cashCollection.toPlainString()).append("\n");
            sb.append("7. Crossings Per Lane: ").append(vehicleCountPerLane).append("\n");
            sb.append("8. Revenue Grouped by Vehicle Type:\n");
            for (Map.Entry<VehicleType, BigDecimal> entry : revenueByVehicleType.entrySet()) {
                sb.append("   - ").append(entry.getKey()).append(": Rs. ").append(entry.getValue().toPlainString()).append("\n");
            }
            sb.append("======================================================\n");
            return sb.toString();
        }
    }
}
