import tollmanagement.model.*;
import tollmanagement.repository.*;
import tollmanagement.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Module5Verification {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== MODULE 5 VERIFICATION TEST SUITE ===");

        test1_UpiRechargeSuccess();
        test2_CardRechargeSuccess();
        test3_NetBankingRechargeSuccess();
        test4_FailedRechargeDoesNotIncreaseBalance();
        test5_RechargeLimitValidation();
        test6_DisputeWithinSevenDaysAccepted();
        test7_DisputeAfterSevenDaysRejected();
        test8_AdminApprovesRefundAndCreditsBalance();
        test9_AdminRejectsRefund();
        test10_DuplicateRefundPrevention();
        test11_LowBalanceAlertBelow200();
        test12_DailyPlazaRevenueReportExcludesRefunds();
        test13_VehicleCountPerLane();
        test14_FastagVersusCashRevenueSplit();

        System.out.println("\n=== SUMMARY ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }

    private static void check(String testName, boolean condition, String detail) {
        if (condition) {
            System.out.println("[PASS] " + testName + (detail != null ? " - " + detail : ""));
            passed++;
        } else {
            System.out.println("[FAIL] " + testName + (detail != null ? " - " + detail : ""));
            failed++;
        }
    }

    private static Fixture setup() {
        Admin admin = new Admin("admin");
        TollPlazaRepository plazaRepo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService plazaService = new TollPlazaService(plazaRepo, rateService);
        VehicleRepository vehicleRepo = new VehicleRepository();
        FASTagAccountRepository accountRepo = new FASTagAccountRepository();
        TollTransactionRepository txRepo = new TollTransactionRepository();

        VehicleService vehicleService = new VehicleService(vehicleRepo);
        FASTagService fastagService = new FASTagService(accountRepo, vehicleRepo);
        TollProcessingService processingService = new TollProcessingService(
                plazaRepo, rateService, accountRepo, vehicleRepo, txRepo);

        RechargeRepository rechargeRepo = new RechargeRepository();
        RechargeService rechargeService = new RechargeService(rechargeRepo, accountRepo);

        DisputeRepository disputeRepo = new DisputeRepository();
        RefundRepository refundRepo = new RefundRepository();
        DisputeAndRefundService disputeService = new DisputeAndRefundService(
                disputeRepo, refundRepo, txRepo, accountRepo);

        RevenueReportService reportService = new RevenueReportService(txRepo, disputeService);

        TollPlaza plaza = plazaService.createPlaza(admin, "Plaza Central", "NH-44", "Bengaluru");
        plazaService.addLane(admin, plaza.getId(), 1, LaneType.FASTAG);
        plazaService.addLane(admin, plaza.getId(), 2, LaneType.CASH);
        plazaService.openLane(admin, plaza.getId(), 1);
        plazaService.openLane(admin, plaza.getId(), 2);

        LocalDate date = LocalDate.of(2026, 1, 1);
        for (VehicleType v : VehicleType.values()) {
            for (TripType t : TripType.values()) {
                rateService.setRate(admin, plaza, v, t, new BigDecimal("100.00"), date);
            }
        }
        plazaService.openPlaza(admin, plaza.getId(), date);

        return new Fixture(admin, plaza, plazaRepo, rateService, vehicleRepo, accountRepo, txRepo,
                vehicleService, fastagService, processingService, rechargeRepo, rechargeService,
                disputeRepo, refundRepo, disputeService, reportService);
    }

    private static class Fixture {
        Admin admin;
        TollPlaza plaza;
        TollPlazaRepository plazaRepo;
        TollRateService rateService;
        VehicleRepository vehicleRepo;
        FASTagAccountRepository accountRepo;
        TollTransactionRepository txRepo;
        VehicleService vehicleService;
        FASTagService fastagService;
        TollProcessingService processingService;
        RechargeRepository rechargeRepo;
        RechargeService rechargeService;
        DisputeRepository disputeRepo;
        RefundRepository refundRepo;
        DisputeAndRefundService disputeService;
        RevenueReportService reportService;

        Fixture(Admin admin, TollPlaza plaza, TollPlazaRepository plazaRepo, TollRateService rateService,
                VehicleRepository vehicleRepo, FASTagAccountRepository accountRepo, TollTransactionRepository txRepo,
                VehicleService vehicleService, FASTagService fastagService, TollProcessingService processingService,
                RechargeRepository rechargeRepo, RechargeService rechargeService, DisputeRepository disputeRepo,
                RefundRepository refundRepo, DisputeAndRefundService disputeService, RevenueReportService reportService) {
            this.admin = admin;
            this.plaza = plaza;
            this.plazaRepo = plazaRepo;
            this.rateService = rateService;
            this.vehicleRepo = vehicleRepo;
            this.accountRepo = accountRepo;
            this.txRepo = txRepo;
            this.vehicleService = vehicleService;
            this.fastagService = fastagService;
            this.processingService = processingService;
            this.rechargeRepo = rechargeRepo;
            this.rechargeService = rechargeService;
            this.disputeRepo = disputeRepo;
            this.refundRepo = refundRepo;
            this.disputeService = disputeService;
            this.reportService = reportService;
        }
    }

    private static void test1_UpiRechargeSuccess() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01UPI1", "User1", "9876543210", VehicleType.CAR);
        FASTagAccount acc = f.fastagService.createAccount(f.admin, "TAGUPI1", "KA01UPI1", new BigDecimal("500.00"));

        RechargeRecord rec = f.rechargeService.processRecharge(
                "TAGUPI1", new BigDecimal("1000.00"), new UpiPaymentStrategy(true), LocalDateTime.now());

        boolean ok = rec.getStatus() == RechargeStatus.PAID
                && rec.getPaymentMethod() == PaymentMethod.UPI
                && acc.getBalance().compareTo(new BigDecimal("1500.00")) == 0;

        check("1. UPI recharge success", ok, "Balance: Rs.1500, Status: PAID");
    }

    private static void test2_CardRechargeSuccess() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01CARD1", "User2", "9876543210", VehicleType.CAR);
        FASTagAccount acc = f.fastagService.createAccount(f.admin, "TAGCARD1", "KA01CARD1", new BigDecimal("500.00"));

        RechargeRecord rec = f.rechargeService.processRecharge(
                "TAGCARD1", new BigDecimal("2000.00"), new CardPaymentStrategy(true), LocalDateTime.now());

        boolean ok = rec.getStatus() == RechargeStatus.PAID
                && rec.getPaymentMethod() == PaymentMethod.CARD
                && acc.getBalance().compareTo(new BigDecimal("2500.00")) == 0;

        check("2. Card recharge success", ok, "Balance: Rs.2500, Status: PAID");
    }

    private static void test3_NetBankingRechargeSuccess() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01NB1", "User3", "9876543210", VehicleType.CAR);
        FASTagAccount acc = f.fastagService.createAccount(f.admin, "TAGNB1", "KA01NB1", new BigDecimal("500.00"));

        RechargeRecord rec = f.rechargeService.processRecharge(
                "TAGNB1", new BigDecimal("500.00"), new NetBankingPaymentStrategy(true), LocalDateTime.now());

        boolean ok = rec.getStatus() == RechargeStatus.PAID
                && rec.getPaymentMethod() == PaymentMethod.NET_BANKING
                && acc.getBalance().compareTo(new BigDecimal("1000.00")) == 0;

        check("3. Net Banking recharge success", ok, "Balance: Rs.1000, Status: PAID");
    }

    private static void test4_FailedRechargeDoesNotIncreaseBalance() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01FAIL1", "User4", "9876543210", VehicleType.CAR);
        FASTagAccount acc = f.fastagService.createAccount(f.admin, "TAGFAIL1", "KA01FAIL1", new BigDecimal("500.00"));

        RechargeRecord rec = f.rechargeService.processRecharge(
                "TAGFAIL1", new BigDecimal("500.00"), new UpiPaymentStrategy(false), LocalDateTime.now()); // Simulate failure

        boolean ok = rec.getStatus() == RechargeStatus.FAILED
                && acc.getBalance().compareTo(new BigDecimal("500.00")) == 0; // Balance remains 500

        check("4. Failed recharge does not increase balance", ok, "Balance remains Rs.500, Status: FAILED");
    }

    private static void test5_RechargeLimitValidation() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01LIMIT1", "User5", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAGLIMIT1", "KA01LIMIT1", new BigDecimal("500.00"));

        boolean minCaught = false;
        try {
            f.rechargeService.processRecharge("TAGLIMIT1", new BigDecimal("50.00"), new UpiPaymentStrategy(true), LocalDateTime.now());
        } catch (IllegalArgumentException e) {
            minCaught = e.getMessage().contains("Minimum recharge amount is Rs. 100");
        }

        boolean maxCaught = false;
        try {
            f.rechargeService.processRecharge("TAGLIMIT1", new BigDecimal("15000.00"), new UpiPaymentStrategy(true), LocalDateTime.now());
        } catch (IllegalArgumentException e) {
            maxCaught = e.getMessage().contains("Maximum recharge amount per transaction is Rs. 10000");
        }

        check("5. Recharge limit validation (Min Rs.100, Max Rs.10000)", minCaught && maxCaught, "Caught min and max violations");
    }

    private static void test6_DisputeWithinSevenDaysAccepted() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01DISP1", "User6", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAGDISP1", "KA01DISP1", new BigDecimal("500.00"));

        LocalDateTime txTime = LocalDateTime.of(2026, 10, 25, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAGDISP1", VehicleType.CAR, txTime);

        // Dispute filed 3 days later
        LocalDateTime disputeTime = LocalDateTime.of(2026, 10, 28, 10, 0, 0);
        Dispute dispute = f.disputeService.raiseDispute(tx.getTransactionId(), "Double deduction claim", disputeTime);

        boolean ok = dispute.getStatus() == DisputeStatus.PENDING
                && dispute.getTransactionId().equals(tx.getTransactionId());

        check("6. Dispute within 7 days accepted", ok, "Dispute created in PENDING status");
    }

    private static void test7_DisputeAfterSevenDaysRejected() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01DISP2", "User7", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAGDISP2", "KA01DISP2", new BigDecimal("500.00"));

        LocalDateTime txTime = LocalDateTime.of(2026, 10, 10, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAGDISP2", VehicleType.CAR, txTime);

        // Dispute filed 15 days later (> 7 days)
        LocalDateTime disputeTime = LocalDateTime.of(2026, 10, 25, 10, 0, 0);
        boolean rejected = false;
        try {
            f.disputeService.raiseDispute(tx.getTransactionId(), "Late claim", disputeTime);
        } catch (IllegalArgumentException e) {
            rejected = e.getMessage().contains("must be raised within 7 days");
        }

        check("7. Dispute after 7 days rejected", rejected, "Rejected with descriptive exception");
    }

    private static void test8_AdminApprovesRefundAndCreditsBalance() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01REF1", "User8", "9876543210", VehicleType.CAR);
        FASTagAccount acc = f.fastagService.createAccount(f.admin, "TAGREF1", "KA01REF1", new BigDecimal("500.00"));

        LocalDateTime txTime = LocalDateTime.of(2026, 10, 28, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAGREF1", VehicleType.CAR, txTime);
        // Balance after deduction is 400.00

        Dispute dispute = f.disputeService.raiseDispute(tx.getTransactionId(), "Incorrect scanner trigger", txTime.plusHours(1));

        RefundRecord refund = f.disputeService.approveDispute(f.admin, dispute.getDisputeId(), "Valid claim", txTime.plusHours(2));

        boolean ok = dispute.getStatus() == DisputeStatus.APPROVED
                && refund.getRefundAmount().compareTo(new BigDecimal("100.00")) == 0
                && acc.getBalance().compareTo(new BigDecimal("500.00")) == 0; // Balance restored

        check("8. Admin approves refund and credits balance", ok, "Dispute APPROVED, Balance restored to Rs.500");
    }

    private static void test9_AdminRejectsRefund() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01REJ1", "User9", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAGREJ1", "KA01REJ1", new BigDecimal("500.00"));

        LocalDateTime txTime = LocalDateTime.of(2026, 10, 28, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAGREJ1", VehicleType.CAR, txTime);

        Dispute dispute = f.disputeService.raiseDispute(tx.getTransactionId(), "Frivolous claim", txTime.plusHours(1));
        f.disputeService.rejectDispute(f.admin, dispute.getDisputeId(), "Sensor records confirm crossing");

        boolean ok = dispute.getStatus() == DisputeStatus.REJECTED
                && !f.disputeService.isTransactionRefunded(tx.getTransactionId());

        check("9. Admin rejects refund", ok, "Dispute marked REJECTED, no refund record created");
    }

    private static void test10_DuplicateRefundPrevention() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01DUPR1", "User10", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAGDUPR1", "KA01DUPR1", new BigDecimal("500.00"));

        LocalDateTime txTime = LocalDateTime.of(2026, 10, 28, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAGDUPR1", VehicleType.CAR, txTime);

        Dispute dispute = f.disputeService.raiseDispute(tx.getTransactionId(), "First claim", txTime.plusHours(1));
        f.disputeService.approveDispute(f.admin, dispute.getDisputeId(), "Approved", txTime.plusHours(2));

        boolean duplicateBlocked = false;
        try {
            f.disputeService.approveDispute(f.admin, dispute.getDisputeId(), "Try again", txTime.plusHours(3));
        } catch (IllegalStateException e) {
            duplicateBlocked = true;
        }

        check("10. Duplicate refund prevention", duplicateBlocked, "Prevented issuing second refund for same transaction");
    }

    private static void test11_LowBalanceAlertBelow200() {
        FASTagAccount acc1 = new FASTagAccount("TAG1", "KA01A1", new BigDecimal("180.00")); // < 200
        FASTagAccount acc2 = new FASTagAccount("TAG2", "KA01A2", new BigDecimal("350.00")); // >= 200

        boolean alert1 = BalanceAlertService.checkLowBalance(acc1).isPresent();
        boolean alert2 = BalanceAlertService.checkLowBalance(acc2).isEmpty();

        check("11. Low balance alert below Rs.200", alert1 && alert2, "Alert generated for Rs.180, none for Rs.350");
    }

    private static void test12_DailyPlazaRevenueReportExcludesRefunds() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01REP1", "User11", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAGREP1", "KA01REP1", new BigDecimal("500.00"));

        LocalDate today = LocalDate.of(2026, 10, 30);
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 30, 9, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 30, 10, 0, 0);

        // Tx1: FASTag 100
        TollTransaction tx1 = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAGREP1", VehicleType.CAR, t1);
        // Tx2: Cash 100
        TollTransaction tx2 = f.processingService.processCashCrossing(f.plaza.getId(), 2, "KA01CASH", VehicleType.CAR, false, new BigDecimal("100"), t2);

        // Refund Tx1
        Dispute disp = f.disputeService.raiseDispute(tx1.getTransactionId(), "Mistake", t1.plusHours(1));
        f.disputeService.approveDispute(f.admin, disp.getDisputeId(), "Approved", t1.plusHours(2));

        RevenueReportService.DailyReport report = f.reportService.generateDailyReport(f.plaza.getId(), today);

        // Net revenue should be only Rs.100 (from Tx2), since Tx1 was refunded
        boolean ok = report.getTotalRevenue().compareTo(new BigDecimal("100.00")) == 0
                && report.getTotalCrossings() == 2;

        check("12. Daily plaza revenue report excludes refunds", ok, "Net revenue: Rs.100 (Tx1 refunded)");
    }

    private static void test13_VehicleCountPerLane() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01LANE1", "User12", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAGLANE1", "KA01LANE1", new BigDecimal("500.00"));

        LocalDate today = LocalDate.of(2026, 10, 30);
        f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAGLANE1", VehicleType.CAR, LocalDateTime.of(2026, 10, 30, 8, 0, 0));
        f.processingService.processCashCrossing(f.plaza.getId(), 2, "KA01CASH", VehicleType.CAR, false, new BigDecimal("100"), LocalDateTime.of(2026, 10, 30, 8, 30, 0));

        RevenueReportService.DailyReport report = f.reportService.generateDailyReport(f.plaza.getId(), today);

        boolean ok = report.getVehicleCountPerLane().get(1) == 1 && report.getVehicleCountPerLane().get(2) == 1;
        check("13. Vehicle count per lane recorded", ok, "Lane 1: 1, Lane 2: 1");
    }

    private static void test14_FastagVersusCashRevenueSplit() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01SPLIT1", "User13", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAGSPLIT1", "KA01SPLIT1", new BigDecimal("500.00"));

        LocalDate today = LocalDate.of(2026, 10, 30);
        f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAGSPLIT1", VehicleType.CAR, LocalDateTime.of(2026, 10, 30, 9, 0, 0)); // 100
        f.processingService.processCashCrossing(f.plaza.getId(), 2, "KA01CASH", VehicleType.CAR, false, new BigDecimal("100"), LocalDateTime.of(2026, 10, 30, 10, 0, 0)); // 100

        RevenueReportService.DailyReport report = f.reportService.generateDailyReport(f.plaza.getId(), today);

        boolean ok = report.getFastagCollection().compareTo(new BigDecimal("100.00")) == 0
                && report.getCashCollection().compareTo(new BigDecimal("100.00")) == 0;

        check("14. FASTag versus cash revenue split", ok, "FASTag: Rs.100, Cash: Rs.100");
    }
}
