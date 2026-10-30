import tollmanagement.model.*;
import tollmanagement.repository.*;
import tollmanagement.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Module3Verification {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== MODULE 3 VERIFICATION TEST SUITE ===");

        test1_ValidFastagProcessingOpensBarrierAndDeducts();
        test2_VehicleTypeMismatchChargesHigherAndFlags();
        test3_InvalidTagDivertedAndBarrierRemainsClosed();
        test4_BlockedOrBlacklistedTagDivertedAndBarrierClosed();
        test5_InsufficientBalanceLeavesBarrierClosedAndZeroPartialDeduction();
        test6_EmergencyLaneExemptedWithZeroAmount();
        test7_UnauthorizedEmergencyRejectedAndBarrierClosed();
        test8_DuplicateCrossingWithinTwoMinutesPrevented();
        test9_CashCrossingDirectAndPenaltyForRedirected();
        test10_LaneLockReleasedEvenOnFailureOrException();

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

    private static Fixture setupEnvironment() {
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

        // Create plaza with 3 lanes: FASTAG, CASH, EMERGENCY
        TollPlaza plaza = plazaService.createPlaza(admin, "Bangalore Toll Plaza", "NH-44", "Hosur Road");
        plazaService.addLane(admin, plaza.getId(), 1, LaneType.FASTAG);
        plazaService.addLane(admin, plaza.getId(), 2, LaneType.CASH);
        plazaService.addLane(admin, plaza.getId(), 3, LaneType.EMERGENCY);

        // Open lanes
        plazaService.openLane(admin, plaza.getId(), 1);
        plazaService.openLane(admin, plaza.getId(), 2);
        plazaService.openLane(admin, plaza.getId(), 3);

        // Set rates (CAR: 100, BUS: 200, TRUCK: 300, etc.)
        LocalDate date = LocalDate.of(2026, 1, 1);
        rateService.setRate(admin, plaza, VehicleType.CAR, TripType.SINGLE, new BigDecimal("100.00"), date);
        rateService.setRate(admin, plaza, VehicleType.CAR, TripType.RETURN, new BigDecimal("150.00"), date);
        rateService.setRate(admin, plaza, VehicleType.VAN, TripType.SINGLE, new BigDecimal("120.00"), date);
        rateService.setRate(admin, plaza, VehicleType.VAN, TripType.RETURN, new BigDecimal("180.00"), date);
        rateService.setRate(admin, plaza, VehicleType.BUS, TripType.SINGLE, new BigDecimal("200.00"), date);
        rateService.setRate(admin, plaza, VehicleType.BUS, TripType.RETURN, new BigDecimal("300.00"), date);
        rateService.setRate(admin, plaza, VehicleType.TRUCK, TripType.SINGLE, new BigDecimal("300.00"), date);
        rateService.setRate(admin, plaza, VehicleType.TRUCK, TripType.RETURN, new BigDecimal("450.00"), date);
        rateService.setRate(admin, plaza, VehicleType.MULTI_AXLE, TripType.SINGLE, new BigDecimal("400.00"), date);
        rateService.setRate(admin, plaza, VehicleType.MULTI_AXLE, TripType.RETURN, new BigDecimal("600.00"), date);

        plazaService.openPlaza(admin, plaza.getId(), date);

        return new Fixture(admin, plaza, plazaRepo, rateService, vehicleRepo, accountRepo, txRepo,
                vehicleService, fastagService, processingService);
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

        Fixture(Admin admin, TollPlaza plaza, TollPlazaRepository plazaRepo, TollRateService rateService,
                VehicleRepository vehicleRepo, FASTagAccountRepository accountRepo, TollTransactionRepository txRepo,
                VehicleService vehicleService, FASTagService fastagService, TollProcessingService processingService) {
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
        }
    }

    private static void test1_ValidFastagProcessingOpensBarrierAndDeducts() {
        Fixture f = setupEnvironment();
        f.vehicleService.registerVehicle(f.admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAG1001", "KA01AB1111", new BigDecimal("500.00"));

        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAG1001", VehicleType.CAR, now);

        FASTagAccount account = f.fastagService.getAccountByTagId("TAG1001");
        boolean ok = tx.getStatus() == TransactionStatus.SUCCESS
                && tx.isBarrierOpened()
                && tx.getAmountCharged().compareTo(new BigDecimal("100.00")) == 0
                && account.getBalance().compareTo(new BigDecimal("400.00")) == 0;

        check("1. Valid FASTag processing opens barrier & deducts balance", ok, "Charged Rs.100, remaining Rs.400");
    }

    private static void test2_VehicleTypeMismatchChargesHigherAndFlags() {
        Fixture f = setupEnvironment();
        // Registered as CAR (rate 100), but observed as TRUCK (rate 300)
        f.vehicleService.registerVehicle(f.admin, "KA01AB2222", "Bob", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAG1002", "KA01AB2222", new BigDecimal("500.00"));

        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAG1002", VehicleType.TRUCK, now);

        FASTagAccount account = f.fastagService.getAccountByTagId("TAG1002");
        boolean ok = tx.getStatus() == TransactionStatus.SUCCESS
                && tx.isBarrierOpened()
                && tx.getAmountCharged().compareTo(new BigDecimal("300.00")) == 0 // charged TRUCK rate
                && tx.isFlaggedForReview()
                && tx.getRegisteredVehicleType() == VehicleType.CAR
                && tx.getObservedVehicleType() == VehicleType.TRUCK
                && account.getBalance().compareTo(new BigDecimal("200.00")) == 0;

        check("2. Vehicle type mismatch charges higher rate and flags for review", ok, "Charged Rs.300 (TRUCK), flagged review");
    }

    private static void test3_InvalidTagDivertedAndBarrierRemainsClosed() {
        Fixture f = setupEnvironment();
        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "UNKNOWN_TAG", VehicleType.CAR, now);

        boolean ok = tx.getStatus() == TransactionStatus.FAILED_INVALID_TAG
                && !tx.isBarrierOpened()
                && tx.isFlaggedForReview();

        check("3. Invalid/unknown tag diverted to cash & barrier remains closed", ok, "Status: " + tx.getStatus());
    }

    private static void test4_BlockedOrBlacklistedTagDivertedAndBarrierClosed() {
        Fixture f = setupEnvironment();
        f.vehicleService.registerVehicle(f.admin, "KA01AB3333", "Charlie", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAG1003", "KA01AB3333", new BigDecimal("500.00"));
        f.fastagService.blacklistAccount(f.admin, "TAG1003");

        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAG1003", VehicleType.CAR, now);

        boolean ok = tx.getStatus() == TransactionStatus.FAILED_BLACKLISTED_TAG
                && !tx.isBarrierOpened()
                && tx.isFlaggedForReview();

        check("4. Blacklisted tag rejected & barrier remains closed", ok, "Status: " + tx.getStatus());
    }

    private static void test5_InsufficientBalanceLeavesBarrierClosedAndZeroPartialDeduction() {
        Fixture f = setupEnvironment();
        f.vehicleService.registerVehicle(f.admin, "KA01AB4444", "David", "9876543210", VehicleType.TRUCK); // Truck rate = 300
        f.fastagService.createAccount(f.admin, "TAG1004", "KA01AB4444", new BigDecimal("150.00")); // Balance = 150 < 300

        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAG1004", VehicleType.TRUCK, now);

        FASTagAccount account = f.fastagService.getAccountByTagId("TAG1004");
        boolean ok = tx.getStatus() == TransactionStatus.FAILED_INSUFFICIENT_BALANCE
                && !tx.isBarrierOpened()
                && account.getBalance().compareTo(new BigDecimal("150.00")) == 0; // zero partial deduction

        check("5. Insufficient balance leaves barrier closed and deducts 0", ok, "Balance remains Rs.150");
    }

    private static void test6_EmergencyLaneExemptedWithZeroAmount() {
        Fixture f = setupEnvironment();
        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processEmergencyCrossing(
                f.plaza.getId(), 3, "KA01EM108", VehicleType.VAN, true, "Government Ambulance", now);

        boolean ok = tx.getStatus() == TransactionStatus.EXEMPTED
                && tx.isBarrierOpened()
                && tx.getAmountCharged().compareTo(BigDecimal.ZERO) == 0
                && tx.getPaymentMode() == PaymentMode.EXEMPTED;

        check("6. Authorized emergency vehicle exempted with Rs.0 toll & barrier opens", ok, "Amount: Rs.0, Barrier: OPEN");
    }

    private static void test7_UnauthorizedEmergencyRejectedAndBarrierClosed() {
        Fixture f = setupEnvironment();
        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processEmergencyCrossing(
                f.plaza.getId(), 3, "KA01PRIVATE", VehicleType.CAR, false, "Private car claiming hurry", now);

        boolean ok = tx.getStatus() == TransactionStatus.FAILED
                && !tx.isBarrierOpened()
                && tx.isFlaggedForReview();

        check("7. Unauthorized vehicle in emergency lane rejected & barrier closed", ok, "Blocked and flagged");
    }

    private static void test8_DuplicateCrossingWithinTwoMinutesPrevented() {
        Fixture f = setupEnvironment();
        f.vehicleService.registerVehicle(f.admin, "KA01AB5555", "Eve", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAG1005", "KA01AB5555", new BigDecimal("500.00"));

        LocalDateTime t1 = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx1 = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAG1005", VehicleType.CAR, t1);

        // Attempt 2: 1 minute later (< 2 minutes)
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 30, 10, 1, 0);
        TollTransaction tx2 = f.processingService.processFastagCrossing(f.plaza.getId(), 1, "TAG1005", VehicleType.CAR, t2);

        FASTagAccount account = f.fastagService.getAccountByTagId("TAG1005");
        boolean ok = tx1.getStatus() == TransactionStatus.SUCCESS
                && tx2.getStatus() == TransactionStatus.FAILED_DUPLICATE_CROSSING
                && !tx2.isBarrierOpened()
                && account.getBalance().compareTo(new BigDecimal("400.00")) == 0; // only deducted once

        check("8. Duplicate crossing within 2 minutes prevented", ok, "Attempt 2 blocked, balance only deducted once");
    }

    private static void test9_CashCrossingDirectAndPenaltyForRedirected() {
        Fixture f = setupEnvironment();
        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);

        // Direct cash payment for CAR (rate 100)
        TollTransaction txDirect = f.processingService.processCashCrossing(
                f.plaza.getId(), 2, "KA01CASH1", VehicleType.CAR, false, new BigDecimal("100.00"), now);

        // Redirected from FASTag lane (penalty 2x = 200)
        TollTransaction txPenalty = f.processingService.processCashCrossing(
                f.plaza.getId(), 2, "KA01CASH2", VehicleType.CAR, true, new BigDecimal("200.00"), now);

        boolean ok = txDirect.getStatus() == TransactionStatus.SUCCESS
                && txDirect.getAmountCharged().compareTo(new BigDecimal("100.00")) == 0
                && txDirect.isBarrierOpened()
                && txPenalty.getStatus() == TransactionStatus.SUCCESS
                && txPenalty.getAmountCharged().compareTo(new BigDecimal("200.00")) == 0
                && txPenalty.isBarrierOpened()
                && txPenalty.isFlaggedForReview();

        check("9. Cash payment: normal toll and double toll penalty for redirected vehicles", ok, "Normal: Rs.100, Penalty: Rs.200");
    }

    private static void test10_LaneLockReleasedEvenOnFailureOrException() {
        Fixture f = setupEnvironment();
        Lane lane = f.plaza.findLane(1);

        // Failed crossing due to invalid tag
        f.processingService.processFastagCrossing(f.plaza.getId(), 1, "NON_EXISTENT", VehicleType.CAR, LocalDateTime.now());
        boolean releasedAfterFailure = !lane.isVehicleBeingProcessed();

        check("10. Lane processing lock released cleanly in finally block", releasedAfterFailure, "Lane processing flag = false");
    }
}
