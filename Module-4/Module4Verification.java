import tollmanagement.model.*;
import tollmanagement.repository.*;
import tollmanagement.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Module4Verification {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== MODULE 4 VERIFICATION TEST SUITE ===");

        test1_ReturnTripWithin24HoursApplies50PercentDiscount();
        test2_ReturnTripAfter24HoursChargesNormalToll();
        test3_ReturnTripAtDifferentPlazaDoesNotApplyDiscount();
        test4_ValidMonthlyPassChargesZeroToll();
        test5_ExpiredMonthlyPassChargesNormalToll();
        test6_DuplicateActiveMonthlyPassRejected();
        test7_PassFeeByVehicleTypeConfiguration();
        test8_TollRoundingToNearestFive();
        test9_ReturnTripDiscountCombinedWithFastagProcessing();
        test10_EmergencyExemptionRemainsUnaffected();

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

        MonthlyPassRepository passRepo = new MonthlyPassRepository();
        MonthlyPassService passService = new MonthlyPassService(passRepo);
        ReturnTripService returnTripService = new ReturnTripService(txRepo);

        processingService.setMonthlyPassService(passService);
        processingService.setReturnTripService(returnTripService);

        // Setup Plaza 1 (Plaza A)
        TollPlaza plaza1 = plazaService.createPlaza(admin, "Plaza A", "NH-44", "Bengaluru");
        plazaService.addLane(admin, plaza1.getId(), 1, LaneType.FASTAG);
        plazaService.addLane(admin, plaza1.getId(), 2, LaneType.CASH);
        plazaService.addLane(admin, plaza1.getId(), 3, LaneType.EMERGENCY);
        plazaService.openLane(admin, plaza1.getId(), 1);
        plazaService.openLane(admin, plaza1.getId(), 2);
        plazaService.openLane(admin, plaza1.getId(), 3);

        // Setup Plaza 2 (Plaza B)
        TollPlaza plaza2 = plazaService.createPlaza(admin, "Plaza B", "NH-48", "Tumakuru");
        plazaService.addLane(admin, plaza2.getId(), 1, LaneType.FASTAG);
        plazaService.addLane(admin, plaza2.getId(), 2, LaneType.CASH);
        plazaService.openLane(admin, plaza2.getId(), 1);
        plazaService.openLane(admin, plaza2.getId(), 2);

        LocalDate date = LocalDate.of(2026, 1, 1);
        for (VehicleType v : VehicleType.values()) {
            for (TripType t : TripType.values()) {
                rateService.setRate(admin, plaza1, v, t, new BigDecimal("100.00"), date);
                rateService.setRate(admin, plaza2, v, t, new BigDecimal("100.00"), date);
            }
        }
        plazaService.openPlaza(admin, plaza1.getId(), date);
        plazaService.openPlaza(admin, plaza2.getId(), date);

        return new Fixture(admin, plaza1, plaza2, plazaRepo, rateService, vehicleRepo, accountRepo, txRepo,
                vehicleService, fastagService, processingService, passRepo, passService, returnTripService);
    }

    private static class Fixture {
        Admin admin;
        TollPlaza plaza1;
        TollPlaza plaza2;
        TollPlazaRepository plazaRepo;
        TollRateService rateService;
        VehicleRepository vehicleRepo;
        FASTagAccountRepository accountRepo;
        TollTransactionRepository txRepo;
        VehicleService vehicleService;
        FASTagService fastagService;
        TollProcessingService processingService;
        MonthlyPassRepository passRepo;
        MonthlyPassService passService;
        ReturnTripService returnTripService;

        Fixture(Admin admin, TollPlaza plaza1, TollPlaza plaza2, TollPlazaRepository plazaRepo,
                TollRateService rateService, VehicleRepository vehicleRepo, FASTagAccountRepository accountRepo,
                TollTransactionRepository txRepo, VehicleService vehicleService, FASTagService fastagService,
                TollProcessingService processingService, MonthlyPassRepository passRepo,
                MonthlyPassService passService, ReturnTripService returnTripService) {
            this.admin = admin;
            this.plaza1 = plaza1;
            this.plaza2 = plaza2;
            this.plazaRepo = plazaRepo;
            this.rateService = rateService;
            this.vehicleRepo = vehicleRepo;
            this.accountRepo = accountRepo;
            this.txRepo = txRepo;
            this.vehicleService = vehicleService;
            this.fastagService = fastagService;
            this.processingService = processingService;
            this.passRepo = passRepo;
            this.passService = passService;
            this.returnTripService = returnTripService;
        }
    }

    private static void test1_ReturnTripWithin24HoursApplies50PercentDiscount() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAG1001", "KA01AB1111", new BigDecimal("500.00"));

        LocalDateTime t1 = LocalDateTime.of(2026, 10, 30, 8, 0, 0);
        TollTransaction tx1 = f.processingService.processFastagCrossing(f.plaza1.getId(), 1, "TAG1001", VehicleType.CAR, t1);

        // Return crossing 5 hours later at same plaza (100 * 0.5 = 50)
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 30, 13, 0, 0);
        TollTransaction tx2 = f.processingService.processFastagCrossing(f.plaza1.getId(), 1, "TAG1001", VehicleType.CAR, t2);

        boolean ok = tx1.getAmountCharged().compareTo(new BigDecimal("100.00")) == 0
                && tx2.getAmountCharged().compareTo(new BigDecimal("50.00")) == 0
                && tx2.getStatus() == TransactionStatus.SUCCESS;

        check("1. Return trip within 24 hours applies 50% discount", ok, "Tx1: Rs.100, Tx2: Rs.50");
    }

    private static void test2_ReturnTripAfter24HoursChargesNormalToll() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01AB2222", "Bob", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAG1002", "KA01AB2222", new BigDecimal("500.00"));

        LocalDateTime t1 = LocalDateTime.of(2026, 10, 30, 8, 0, 0);
        f.processingService.processFastagCrossing(f.plaza1.getId(), 1, "TAG1002", VehicleType.CAR, t1);

        // Return crossing 26 hours later (> 24 hours)
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 31, 10, 0, 0);
        TollTransaction tx2 = f.processingService.processFastagCrossing(f.plaza1.getId(), 1, "TAG1002", VehicleType.CAR, t2);

        boolean ok = tx2.getAmountCharged().compareTo(new BigDecimal("100.00")) == 0;
        check("2. Return trip after 24 hours charges normal toll", ok, "Charged full toll: Rs.100");
    }

    private static void test3_ReturnTripAtDifferentPlazaDoesNotApplyDiscount() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01AB3333", "Charlie", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAG1003", "KA01AB3333", new BigDecimal("500.00"));

        LocalDateTime t1 = LocalDateTime.of(2026, 10, 30, 8, 0, 0);
        f.processingService.processFastagCrossing(f.plaza1.getId(), 1, "TAG1003", VehicleType.CAR, t1);

        // Crossing at Plaza B (different plaza) 2 hours later
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction txPlazaB = f.processingService.processFastagCrossing(f.plaza2.getId(), 1, "TAG1003", VehicleType.CAR, t2);

        boolean ok = txPlazaB.getAmountCharged().compareTo(new BigDecimal("100.00")) == 0;
        check("3. Return trip at different plaza does not apply discount", ok, "Plaza B charged full rate: Rs.100");
    }

    private static void test4_ValidMonthlyPassChargesZeroToll() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01AB4444", "David", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAG1004", "KA01AB4444", new BigDecimal("100.00"));

        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        f.passService.createMonthlyPass(f.admin, "KA01AB4444", f.plaza1.getId(), start, end);

        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza1.getId(), 1, "TAG1004", VehicleType.CAR, now);

        FASTagAccount account = f.fastagService.getAccountByTagId("TAG1004");
        boolean ok = tx.getStatus() == TransactionStatus.SUCCESS
                && tx.getAmountCharged().compareTo(BigDecimal.ZERO) == 0
                && tx.isBarrierOpened()
                && account.getBalance().compareTo(new BigDecimal("100.00")) == 0;

        check("4. Valid monthly pass charges Rs.0 toll and opens barrier", ok, "Toll: Rs.0, Balance unchanged");
    }

    private static void test5_ExpiredMonthlyPassChargesNormalToll() {
        Fixture f = setup();
        f.vehicleService.registerVehicle(f.admin, "KA01AB5555", "Eve", "9876543210", VehicleType.CAR);
        f.fastagService.createAccount(f.admin, "TAG1005", "KA01AB5555", new BigDecimal("500.00"));

        // Pass expired on Oct 15
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 15);
        f.passService.createMonthlyPass(f.admin, "KA01AB5555", f.plaza1.getId(), start, end);

        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processFastagCrossing(f.plaza1.getId(), 1, "TAG1005", VehicleType.CAR, now);

        boolean ok = tx.getStatus() == TransactionStatus.SUCCESS
                && tx.getAmountCharged().compareTo(new BigDecimal("100.00")) == 0;

        check("5. Expired monthly pass falls back to normal toll", ok, "Charged Rs.100");
    }

    private static void test6_DuplicateActiveMonthlyPassRejected() {
        Fixture f = setup();
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        f.passService.createMonthlyPass(f.admin, "KA01DUP1", f.plaza1.getId(), start, end);

        boolean rejected = false;
        try {
            // Overlapping pass
            f.passService.createMonthlyPass(f.admin, "KA01DUP1", f.plaza1.getId(), LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15));
        } catch (IllegalArgumentException e) {
            rejected = e.getMessage().contains("overlapping monthly pass already exists");
        }

        check("6. Duplicate active monthly pass rejected", rejected, "Blocked overlapping pass");
    }

    private static void test7_PassFeeByVehicleTypeConfiguration() {
        Fixture f = setup();
        f.passService.setPassFee(f.admin, VehicleType.CAR, new BigDecimal("3500.00"));
        f.passService.setPassFee(f.admin, VehicleType.TRUCK, new BigDecimal("9000.00"));

        boolean ok = f.passService.getPassFee(VehicleType.CAR).compareTo(new BigDecimal("3500.00")) == 0
                && f.passService.getPassFee(VehicleType.TRUCK).compareTo(new BigDecimal("9000.00")) == 0;

        check("7. Pass fee configuration by vehicle type", ok, "Car: Rs.3500, Truck: Rs.9000");
    }

    private static void test8_TollRoundingToNearestFive() {
        BigDecimal r1 = TollCalculator.roundToNearestFive(new BigDecimal("72.00")); // -> 70.00
        BigDecimal r2 = TollCalculator.roundToNearestFive(new BigDecimal("73.00")); // -> 75.00
        BigDecimal r3 = TollCalculator.roundToNearestFive(new BigDecimal("77.50")); // -> 80.00
        BigDecimal r4 = TollCalculator.roundToNearestFive(BigDecimal.ZERO);          // -> 0.00

        boolean ok = r1.compareTo(new BigDecimal("70.00")) == 0
                && r2.compareTo(new BigDecimal("75.00")) == 0
                && r3.compareTo(new BigDecimal("80.00")) == 0
                && r4.compareTo(BigDecimal.ZERO) == 0;

        check("8. Toll rounding to nearest Rs.5", ok, "72->70, 73->75, 77.5->80, 0->0");
    }

    private static void test9_ReturnTripDiscountCombinedWithFastagProcessing() {
        Fixture f = setup();
        // Setup rate of Rs.125
        LocalDate date = LocalDate.of(2026, 1, 1);
        f.rateService.setRate(f.admin, f.plaza1, VehicleType.VAN, TripType.SINGLE, new BigDecimal("125.00"), date);

        f.vehicleService.registerVehicle(f.admin, "KA01VAN1", "George", "9876543210", VehicleType.VAN);
        f.fastagService.createAccount(f.admin, "TAGVAN1", "KA01VAN1", new BigDecimal("500.00"));

        LocalDateTime t1 = LocalDateTime.of(2026, 10, 30, 8, 0, 0);
        f.processingService.processFastagCrossing(f.plaza1.getId(), 1, "TAGVAN1", VehicleType.VAN, t1); // 125 rounded to 125

        // Return trip within 24h: 125 * 0.5 = 62.50 -> rounded to nearest 5 = 65.00
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 30, 14, 0, 0);
        TollTransaction txReturn = f.processingService.processFastagCrossing(f.plaza1.getId(), 1, "TAGVAN1", VehicleType.VAN, t2);

        boolean ok = txReturn.getAmountCharged().compareTo(new BigDecimal("65.00")) == 0;
        check("9. Return-trip discount combined with rounding & FASTag deduction", ok, "125 -> 50% discount = 62.50 -> rounded to Rs.65.00");
    }

    private static void test10_EmergencyExemptionRemainsUnaffected() {
        Fixture f = setup();
        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 10, 0, 0);
        TollTransaction tx = f.processingService.processEmergencyCrossing(
                f.plaza1.getId(), 3, "AMB108", VehicleType.VAN, true, "State Ambulance Service", now);

        boolean ok = tx.getStatus() == TransactionStatus.EXEMPTED
                && tx.getAmountCharged().compareTo(BigDecimal.ZERO) == 0
                && tx.isBarrierOpened();

        check("10. Emergency exemption remains unaffected with Rs.0 toll", ok, "Status: EXEMPTED, Rs.0");
    }
}
