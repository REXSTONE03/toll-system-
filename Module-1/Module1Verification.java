import tollmanagement.model.*;
import tollmanagement.repository.TollPlazaRepository;
import tollmanagement.service.TollPlazaService;
import tollmanagement.service.TollRateService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class Module1Verification {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== MODULE 1 VERIFICATION TEST SUITE ===");

        test1_AdminCreatesTollPlaza();
        test2_DuplicatePlazaRejected();
        test3_AdminAddsLanes();
        test4_PlazaRequiresAtLeastTwoLanes();
        test5_DuplicateLaneNumbersRejected();
        test6_AdminConfiguresRates();
        test7_PlazaCannotOpenWithIncompleteRateMatrix();
        test8_PlazaOpensWhenRatesAndLanesComplete();
        test9_LaneCannotCloseWhileVehicleBeingProcessed();
        test10_EffectiveDateRateSelection();
        test11_HistoricalRatesUnaffectedByFutureRates();

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

    private static void test1_AdminCreatesTollPlaza() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Electronic City Plaza", "NH-44", "Bengaluru");
        boolean ok = plaza != null && plaza.getId() != null && "Electronic City Plaza".equals(plaza.getName());
        check("1. Admin creates toll plaza", ok, "Plaza ID: " + (plaza != null ? plaza.getId() : "null"));
    }

    private static void test2_DuplicatePlazaRejected() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        service.createPlaza(admin, "Electronic City Plaza", "NH-44", "Bengaluru");
        boolean rejected = false;
        try {
            service.createPlaza(admin, "electronic city plaza", "nh-44", "Another location");
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("2. Duplicate plaza names on same highway rejected", rejected, "Throws IllegalArgumentException on duplicate name+highway");
    }

    private static void test3_AdminAddsLanes() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Plaza 1", "NH-48", "Mumbai");
        Lane lane1 = service.addLane(admin, plaza.getId(), 1, LaneType.FASTAG);
        Lane lane2 = service.addLane(admin, plaza.getId(), 2, LaneType.CASH);
        boolean ok = plaza.getLanes().size() == 2 && lane1.getStatus() == LaneStatus.CLOSED;
        check("3. Admin adds lanes", ok, "Lanes added with default CLOSED status");
    }

    private static void test4_PlazaRequiresAtLeastTwoLanes() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Plaza 1", "NH-48", "Mumbai");
        service.addLane(admin, plaza.getId(), 1, LaneType.FASTAG);
        // Add all rates so failure is solely lane count
        LocalDate today = LocalDate.of(2026, 10, 30);
        for (VehicleType v : VehicleType.values()) {
            for (TripType t : TripType.values()) {
                rateService.setRate(admin, plaza, v, t, new BigDecimal("100"), today);
            }
        }

        boolean failedAsExpected = false;
        try {
            service.openPlaza(admin, plaza.getId(), today);
        } catch (IllegalStateException e) {
            failedAsExpected = e.getMessage().contains("at least two lanes");
        }
        check("4. Plaza requires at least two lanes", failedAsExpected, "Blocked with 1 lane");
    }

    private static void test5_DuplicateLaneNumbersRejected() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Plaza 1", "NH-48", "Mumbai");
        service.addLane(admin, plaza.getId(), 1, LaneType.FASTAG);
        boolean rejected = false;
        try {
            service.addLane(admin, plaza.getId(), 1, LaneType.CASH);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("5. Duplicate lane numbers are rejected", rejected, "Throws IllegalArgumentException on duplicate lane number");
    }

    private static void test6_AdminConfiguresRates() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Plaza 1", "NH-48", "Mumbai");
        LocalDate date = LocalDate.of(2026, 10, 30);
        for (VehicleType v : VehicleType.values()) {
            for (TripType t : TripType.values()) {
                rateService.setRate(admin, plaza, v, t, new BigDecimal("75.00"), date);
            }
        }
        boolean ok = rateService.getRates(plaza).size() == 10;
        check("6. Admin configures all 5 vehicle types and 2 trip types", ok, "Total rates configured: " + rateService.getRates(plaza).size());
    }

    private static void test7_PlazaCannotOpenWithIncompleteRateMatrix() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Plaza 1", "NH-48", "Mumbai");
        service.addLane(admin, plaza.getId(), 1, LaneType.FASTAG);
        service.addLane(admin, plaza.getId(), 2, LaneType.CASH);

        LocalDate date = LocalDate.of(2026, 10, 30);
        // Only set 9 rates instead of 10
        rateService.setRate(admin, plaza, VehicleType.CAR, TripType.SINGLE, new BigDecimal("50"), date);

        boolean rejected = false;
        try {
            service.openPlaza(admin, plaza.getId(), date);
        } catch (IllegalStateException e) {
            rejected = e.getMessage().contains("every vehicle type and trip type");
        }
        check("7. Plaza cannot open with incomplete rate matrix", rejected, "Incomplete rate matrix properly caught");
    }

    private static void test8_PlazaOpensWhenRatesAndLanesComplete() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Plaza 1", "NH-48", "Mumbai");
        service.addLane(admin, plaza.getId(), 1, LaneType.FASTAG);
        service.addLane(admin, plaza.getId(), 2, LaneType.CASH);

        LocalDate date = LocalDate.of(2026, 10, 30);
        for (VehicleType v : VehicleType.values()) {
            for (TripType t : TripType.values()) {
                rateService.setRate(admin, plaza, v, t, new BigDecimal("80.00"), date);
            }
        }

        service.openPlaza(admin, plaza.getId(), date);
        check("8. Plaza opens when all required rates and lanes exist", plaza.getStatus() == PlazaStatus.OPEN, "Plaza status is OPEN");
    }

    private static void test9_LaneCannotCloseWhileVehicleBeingProcessed() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Plaza 1", "NH-48", "Mumbai");
        service.addLane(admin, plaza.getId(), 1, LaneType.FASTAG);
        service.openLane(admin, plaza.getId(), 1);

        service.startVehicleProcessing(plaza.getId(), 1);
        boolean blocked = false;
        try {
            service.closeLane(admin, plaza.getId(), 1);
        } catch (IllegalStateException e) {
            blocked = e.getMessage().contains("vehicle is being processed");
        }

        service.finishVehicleProcessing(plaza.getId(), 1);
        service.closeLane(admin, plaza.getId(), 1);
        boolean closedAfterFinish = plaza.findLane(1).getStatus() == LaneStatus.CLOSED;

        check("9. Lane cannot close while vehicle is being processed", blocked && closedAfterFinish, "Lock prevented close; allowed close after finish");
    }

    private static void test10_EffectiveDateRateSelection() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Plaza 1", "NH-48", "Mumbai");

        LocalDate oct1 = LocalDate.of(2026, 10, 1);
        LocalDate nov1 = LocalDate.of(2026, 11, 1);

        rateService.setRate(admin, plaza, VehicleType.CAR, TripType.SINGLE, new BigDecimal("50.00"), oct1);
        rateService.setRate(admin, plaza, VehicleType.CAR, TripType.SINGLE, new BigDecimal("60.00"), nov1);

        Optional<TollRate> rateOnOct15 = rateService.findRate(plaza, VehicleType.CAR, TripType.SINGLE, LocalDate.of(2026, 10, 15));
        Optional<TollRate> rateOnNov5 = rateService.findRate(plaza, VehicleType.CAR, TripType.SINGLE, LocalDate.of(2026, 11, 5));

        boolean ok = rateOnOct15.isPresent() && rateOnOct15.get().getAmount().compareTo(new BigDecimal("50.00")) == 0
                  && rateOnNov5.isPresent() && rateOnNov5.get().getAmount().compareTo(new BigDecimal("60.00")) == 0;

        check("10. Effective-date rate selection works correctly", ok, "Oct 15 = Rs.50.00, Nov 5 = Rs.60.00");
    }

    private static void test11_HistoricalRatesUnaffectedByFutureRates() {
        Admin admin = new Admin("admin");
        TollPlazaRepository repo = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService service = new TollPlazaService(repo, rateService);

        TollPlaza plaza = service.createPlaza(admin, "Plaza 1", "NH-48", "Mumbai");

        LocalDate jan1 = LocalDate.of(2026, 1, 1);
        rateService.setRate(admin, plaza, VehicleType.CAR, TripType.SINGLE, new BigDecimal("40.00"), jan1);

        // Check before adding future rate
        Optional<TollRate> beforeFuture = rateService.findRate(plaza, VehicleType.CAR, TripType.SINGLE, LocalDate.of(2026, 6, 1));

        // Add future rate effective Dec 1
        LocalDate dec1 = LocalDate.of(2026, 12, 1);
        rateService.setRate(admin, plaza, VehicleType.CAR, TripType.SINGLE, new BigDecimal("100.00"), dec1);

        // Check query in June - must still be 40.00
        Optional<TollRate> afterFuture = rateService.findRate(plaza, VehicleType.CAR, TripType.SINGLE, LocalDate.of(2026, 6, 1));

        // Check query before Jan 1 - must be empty
        Optional<TollRate> beforeJan = rateService.findRate(plaza, VehicleType.CAR, TripType.SINGLE, LocalDate.of(2025, 12, 31));

        boolean ok = beforeFuture.isPresent() && beforeFuture.get().getAmount().compareTo(new BigDecimal("40.00")) == 0
                  && afterFuture.isPresent() && afterFuture.get().getAmount().compareTo(new BigDecimal("40.00")) == 0
                  && beforeJan.isEmpty();

        check("11. Historical rates do not change when future rate is added", ok, "Prior rates intact; dates before effective date return empty");
    }
}
