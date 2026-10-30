import tollmanagement.model.*;
import tollmanagement.repository.FASTagAccountRepository;
import tollmanagement.repository.VehicleRepository;
import tollmanagement.service.FASTagService;
import tollmanagement.service.VehicleService;

import java.math.BigDecimal;

public class Module2Verification {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== MODULE 2 VERIFICATION TEST SUITE ===");

        test1_DuplicateVehicleRegistrationRejected();
        test2_DuplicateTagIdRejected();
        test3_OneActiveTagPerVehicle();
        test4_RechargeBelow100Rejected();
        test5_RechargeAbove10000Rejected();
        test6_BalanceExceeding50000Rejected();
        test7_AutomaticBlockingBelow100();
        test8_AutomaticReactivationAt100OrAbove();
        test9_BlacklistedTagCannotReactivate();
        test10_ClosedAccountCannotRecharge();

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

    private static void test1_DuplicateVehicleRegistrationRejected() {
        Admin admin = new Admin("admin");
        VehicleRepository repo = new VehicleRepository();
        VehicleService service = new VehicleService(repo);

        service.registerVehicle(admin, "KA01AB1234", "Ramesh Kumar", "9876543210", VehicleType.CAR);
        boolean rejected = false;
        try {
            service.registerVehicle(admin, "ka01ab1234", "Different Owner", "9876543211", VehicleType.CAR);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("1. Duplicate vehicle registration rejection", rejected, "Throws IllegalArgumentException on duplicate reg number");
    }

    private static void test2_DuplicateTagIdRejected() {
        Admin admin = new Admin("admin");
        VehicleRepository vRepo = new VehicleRepository();
        FASTagAccountRepository aRepo = new FASTagAccountRepository();
        VehicleService vService = new VehicleService(vRepo);
        FASTagService aService = new FASTagService(aRepo, vRepo);

        vService.registerVehicle(admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        vService.registerVehicle(admin, "KA01AB2222", "Bob", "9876543211", VehicleType.CAR);

        aService.createAccount(admin, "TAG1001", "KA01AB1111", new BigDecimal("500"));
        boolean rejected = false;
        try {
            aService.createAccount(admin, "tag1001", "KA01AB2222", new BigDecimal("500"));
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("2. Duplicate tag ID rejection", rejected, "Throws IllegalArgumentException on duplicate tag ID");
    }

    private static void test3_OneActiveTagPerVehicle() {
        Admin admin = new Admin("admin");
        VehicleRepository vRepo = new VehicleRepository();
        FASTagAccountRepository aRepo = new FASTagAccountRepository();
        VehicleService vService = new VehicleService(vRepo);
        FASTagService aService = new FASTagService(aRepo, vRepo);

        vService.registerVehicle(admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        aService.createAccount(admin, "TAG1001", "KA01AB1111", new BigDecimal("500"));

        boolean rejected = false;
        try {
            aService.createAccount(admin, "TAG1002", "KA01AB1111", new BigDecimal("500"));
        } catch (IllegalStateException e) {
            rejected = e.getMessage().contains("already has an active FASTag");
        }
        check("3. One active tag per vehicle", rejected, "Prevented assigning second active tag to vehicle");
    }

    private static void test4_RechargeBelow100Rejected() {
        Admin admin = new Admin("admin");
        VehicleRepository vRepo = new VehicleRepository();
        FASTagAccountRepository aRepo = new FASTagAccountRepository();
        VehicleService vService = new VehicleService(vRepo);
        FASTagService aService = new FASTagService(aRepo, vRepo);

        vService.registerVehicle(admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        aService.createAccount(admin, "TAG1001", "KA01AB1111", new BigDecimal("500"));

        boolean rejected = false;
        try {
            aService.recharge(admin, "TAG1001", new BigDecimal("99.99"));
        } catch (IllegalArgumentException e) {
            rejected = e.getMessage().contains("Minimum recharge amount is Rs. 100");
        }
        check("4. Recharge below Rs.100 rejection", rejected, "Blocked recharge < 100");
    }

    private static void test5_RechargeAbove10000Rejected() {
        Admin admin = new Admin("admin");
        VehicleRepository vRepo = new VehicleRepository();
        FASTagAccountRepository aRepo = new FASTagAccountRepository();
        VehicleService vService = new VehicleService(vRepo);
        FASTagService aService = new FASTagService(aRepo, vRepo);

        vService.registerVehicle(admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        aService.createAccount(admin, "TAG1001", "KA01AB1111", new BigDecimal("500"));

        boolean rejected = false;
        try {
            aService.recharge(admin, "TAG1001", new BigDecimal("10001.00"));
        } catch (IllegalArgumentException e) {
            rejected = e.getMessage().contains("Maximum recharge amount per transaction is Rs. 10000");
        }
        check("5. Recharge above Rs.10,000 rejection", rejected, "Blocked recharge > 10000");
    }

    private static void test6_BalanceExceeding50000Rejected() {
        Admin admin = new Admin("admin");
        VehicleRepository vRepo = new VehicleRepository();
        FASTagAccountRepository aRepo = new FASTagAccountRepository();
        VehicleService vService = new VehicleService(vRepo);
        FASTagService aService = new FASTagService(aRepo, vRepo);

        vService.registerVehicle(admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        aService.createAccount(admin, "TAG1001", "KA01AB1111", new BigDecimal("45000.00"));

        boolean rejected = false;
        try {
            // 45000 + 6000 = 51000 > 50000
            aService.recharge(admin, "TAG1001", new BigDecimal("6000.00"));
        } catch (IllegalArgumentException e) {
            rejected = e.getMessage().contains("maximum allowed balance of Rs. 50000");
        }
        check("6. Balance exceeding Rs.50,000 rejection", rejected, "Blocked balance > 50000");
    }

    private static void test7_AutomaticBlockingBelow100() {
        FASTagAccount account = new FASTagAccount("TAG1001", "KA01AB1111", new BigDecimal("150.00"));
        boolean startActive = account.getStatus() == FASTagStatus.ACTIVE;

        account.deduct(new BigDecimal("60.00")); // Balance now 90.00 < 100
        boolean blockedAfterDeduct = account.getStatus() == FASTagStatus.BLOCKED && account.getBalance().compareTo(new BigDecimal("90.00")) == 0;

        check("7. Automatic blocking below Rs.100", startActive && blockedAfterDeduct, "Account switched from ACTIVE to BLOCKED at balance Rs. 90.00");
    }

    private static void test8_AutomaticReactivationAt100OrAbove() {
        Admin admin = new Admin("admin");
        VehicleRepository vRepo = new VehicleRepository();
        FASTagAccountRepository aRepo = new FASTagAccountRepository();
        VehicleService vService = new VehicleService(vRepo);
        FASTagService aService = new FASTagService(aRepo, vRepo);

        vService.registerVehicle(admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        FASTagAccount account = aService.createAccount(admin, "TAG1001", "KA01AB1111", new BigDecimal("150.00"));

        account.deduct(new BigDecimal("60.00")); // Balance 90 -> BLOCKED
        boolean wasBlocked = account.getStatus() == FASTagStatus.BLOCKED;

        aService.recharge(admin, "TAG1001", new BigDecimal("200.00")); // Balance 290 >= 100 -> ACTIVE
        boolean reactivated = account.getStatus() == FASTagStatus.ACTIVE && account.getBalance().compareTo(new BigDecimal("290.00")) == 0;

        check("8. Automatic reactivation at Rs.100 or above", wasBlocked && reactivated, "BLOCKED account reactivated to ACTIVE upon recharge");
    }

    private static void test9_BlacklistedTagCannotReactivate() {
        Admin admin = new Admin("admin");
        VehicleRepository vRepo = new VehicleRepository();
        FASTagAccountRepository aRepo = new FASTagAccountRepository();
        VehicleService vService = new VehicleService(vRepo);
        FASTagService aService = new FASTagService(aRepo, vRepo);

        vService.registerVehicle(admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        aService.createAccount(admin, "TAG1001", "KA01AB1111", new BigDecimal("500.00"));
        aService.blacklistAccount(admin, "TAG1001");

        boolean rejected = false;
        try {
            aService.recharge(admin, "TAG1001", new BigDecimal("500.00"));
        } catch (IllegalStateException e) {
            rejected = e.getMessage().contains("Cannot recharge a BLACKLISTED");
        }

        FASTagAccount account = aService.getAccountByTagId("TAG1001");
        boolean stillBlacklisted = account.getStatus() == FASTagStatus.BLACKLISTED;

        check("9. Blacklisted tag cannot reactivate", rejected && stillBlacklisted, "Recharge rejected and status remained BLACKLISTED");
    }

    private static void test10_ClosedAccountCannotRecharge() {
        Admin admin = new Admin("admin");
        VehicleRepository vRepo = new VehicleRepository();
        FASTagAccountRepository aRepo = new FASTagAccountRepository();
        VehicleService vService = new VehicleService(vRepo);
        FASTagService aService = new FASTagService(aRepo, vRepo);

        vService.registerVehicle(admin, "KA01AB1111", "Alice", "9876543210", VehicleType.CAR);
        aService.createAccount(admin, "TAG1001", "KA01AB1111", new BigDecimal("500.00"));
        aService.closeAccount(admin, "TAG1001");

        boolean rejected = false;
        try {
            aService.recharge(admin, "TAG1001", new BigDecimal("500.00"));
        } catch (IllegalStateException e) {
            rejected = e.getMessage().contains("Cannot recharge a CLOSED");
        }

        FASTagAccount account = aService.getAccountByTagId("TAG1001");
        boolean stillClosed = account.getStatus() == FASTagStatus.CLOSED;

        check("10. Closed account cannot recharge", rejected && stillClosed, "Recharge rejected and status remained CLOSED");
    }
}
