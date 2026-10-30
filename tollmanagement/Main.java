package tollmanagement;

import tollmanagement.model.*;
import tollmanagement.repository.*;
import tollmanagement.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Scanner;

public final class Main {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        Admin admin = new Admin("admin");

        // Repositories
        TollPlazaRepository plazaRepository = new TollPlazaRepository();
        VehicleRepository vehicleRepository = new VehicleRepository();
        FASTagAccountRepository accountRepository = new FASTagAccountRepository();
        TollTransactionRepository transactionRepository = new TollTransactionRepository();
        MonthlyPassRepository passRepository = new MonthlyPassRepository();
        RechargeRepository rechargeRepository = new RechargeRepository();
        DisputeRepository disputeRepository = new DisputeRepository();
        RefundRepository refundRepository = new RefundRepository();

        // Services
        TollRateService rateService = new TollRateService();
        TollPlazaService plazaService = new TollPlazaService(plazaRepository, rateService);
        VehicleService vehicleService = new VehicleService(vehicleRepository);
        FASTagService fastagService = new FASTagService(accountRepository, vehicleRepository);

        MonthlyPassService passService = new MonthlyPassService(passRepository);
        ReturnTripService returnTripService = new ReturnTripService(transactionRepository);

        TollProcessingService processingService = new TollProcessingService(
                plazaRepository, rateService, accountRepository, vehicleRepository, transactionRepository);
        processingService.setMonthlyPassService(passService);
        processingService.setReturnTripService(returnTripService);

        RechargeService rechargeService = new RechargeService(rechargeRepository, accountRepository);
        DisputeAndRefundService disputeService = new DisputeAndRefundService(
                disputeRepository, refundRepository, transactionRepository, accountRepository);
        RevenueReportService reportService = new RevenueReportService(transactionRepository, disputeService);

        System.out.println("TOLL MANAGEMENT SYSTEM - COMPLETE (MODULES 1 TO 5)");
        System.out.println("Demo admin identity: admin (no password authentication configured)");

        boolean running = true;
        while (running) {
            printMenu();
            try {
                switch (prompt("Choose an option: ")) {
                    // Module 1 options
                    case "1" -> createPlaza(admin, plazaService);
                    case "2" -> addLane(admin, plazaService);
                    case "3" -> openLane(admin, plazaService);
                    case "4" -> closeLane(admin, plazaService);
                    case "5" -> setRate(admin, plazaService, rateService);
                    case "6" -> openPlaza(admin, plazaService);
                    case "7" -> listPlazas(plazaService);
                    case "8" -> listRates(plazaService, rateService);
                    case "9" -> simulateProcessing(plazaService);

                    // Module 2 options
                    case "10" -> registerVehicle(admin, vehicleService);
                    case "11" -> createFASTagAccount(admin, fastagService);
                    case "12" -> rechargeAccount(admin, fastagService);
                    case "13" -> blockAccount(admin, fastagService);
                    case "14" -> blacklistAccount(admin, fastagService);
                    case "15" -> viewVehicleAndAccount(vehicleService, fastagService);
                    case "16" -> listVehicles(vehicleService);
                    case "17" -> listAccounts(fastagService);

                    // Module 3 options
                    case "18" -> processFastagCrossing(processingService);
                    case "19" -> processCashCrossing(processingService);
                    case "20" -> processEmergencyCrossing(processingService);
                    case "21" -> viewTransactionHistory(processingService);
                    case "22" -> searchTransactionById(processingService);

                    // Module 4 options
                    case "23" -> createMonthlyPass(admin, passService);
                    case "24" -> setPassFee(admin, passService);
                    case "25" -> listMonthlyPasses(passService);

                    // Module 5 options
                    case "26" -> rechargeWithPaymentMethod(rechargeService);
                    case "27" -> raiseDispute(disputeService);
                    case "28" -> reviewDispute(admin, disputeService);
                    case "29" -> listDisputes(disputeService);
                    case "30" -> generateDailyRevenueReport(reportService);

                    case "0" -> running = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (RuntimeException ex) {
                System.out.println("ERROR: " + ex.getMessage());
            }
            System.out.println();
        }
        System.out.println("System closed. Goodbye!");
    }

    private static void printMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("== Module 1: Toll Plaza & Lanes ==");
        System.out.println("1. Create toll plaza");
        System.out.println("2. Add lane to plaza");
        System.out.println("3. Open lane");
        System.out.println("4. Close lane");
        System.out.println("5. Set / update toll rate");
        System.out.println("6. Open toll plaza (validates lanes and rates)");
        System.out.println("7. List toll plazas and lanes");
        System.out.println("8. List rates for a plaza");
        System.out.println("9. Simulate vehicle processing");
        System.out.println("== Module 2: Vehicles & FASTag Accounts ==");
        System.out.println("10. Register vehicle");
        System.out.println("11. Create FASTag account");
        System.out.println("12. Recharge FASTag account (direct)");
        System.out.println("13. Block FASTag account");
        System.out.println("14. Blacklist FASTag account");
        System.out.println("15. View vehicle and account details");
        System.out.println("16. List all vehicles");
        System.out.println("17. List all FASTag accounts");
        System.out.println("== Module 3: Toll Processing ==");
        System.out.println("18. Process FASTAG vehicle crossing");
        System.out.println("19. Process CASH vehicle crossing");
        System.out.println("20. Process EMERGENCY vehicle crossing");
        System.out.println("21. View transaction history");
        System.out.println("22. Search transaction by ID");
        System.out.println("== Module 4: Return Trips & Monthly Passes ==");
        System.out.println("23. Create monthly pass for vehicle");
        System.out.println("24. Configure monthly pass fee");
        System.out.println("25. List all monthly passes");
        System.out.println("== Module 5: Recharge, Disputes, Refunds & Reports ==");
        System.out.println("26. Recharge with payment gateway (UPI/Card/NetBanking)");
        System.out.println("27. Raise dispute for transaction (within 7 days)");
        System.out.println("28. Review dispute (Approve refund / Reject)");
        System.out.println("29. List all disputes and refund records");
        System.out.println("30. Generate daily plaza revenue report");
        System.out.println("0. Exit");
    }

    // ----------------- Module 1 Actions -----------------

    private static void createPlaza(Admin admin, TollPlazaService service) {
        TollPlaza plaza = service.createPlaza(admin, prompt("Plaza name: "),
                prompt("Highway name: "), prompt("Location: "));
        System.out.println("Created plaza ID: " + plaza.getId());
    }

    private static void addLane(Admin admin, TollPlazaService service) {
        Lane lane = service.addLane(admin, prompt("Plaza ID: "), readInt("Lane number: "),
                readEnum("Lane type", LaneType.class));
        System.out.println("Added: " + lane);
    }

    private static void openLane(Admin admin, TollPlazaService service) {
        service.openLane(admin, prompt("Plaza ID: "), readInt("Lane number: "));
        System.out.println("Lane opened.");
    }

    private static void closeLane(Admin admin, TollPlazaService service) {
        service.closeLane(admin, prompt("Plaza ID: "), readInt("Lane number: "));
        System.out.println("Lane closed.");
    }

    private static void setRate(Admin admin, TollPlazaService plazaService, TollRateService rateService) {
        TollPlaza plaza = plazaService.getPlaza(prompt("Plaza ID: "));
        VehicleType vehicle = readEnum("Vehicle type", VehicleType.class);
        TripType trip = readEnum("Trip type", TripType.class);
        BigDecimal amount = new BigDecimal(prompt("Rate amount (Rs.): "));
        LocalDate date = LocalDate.parse(prompt("Effective date (YYYY-MM-DD): "));
        System.out.println("Saved rate: " + rateService.setRate(admin, plaza, vehicle, trip, amount, date));
    }

    private static void openPlaza(Admin admin, TollPlazaService service) {
        service.openPlaza(admin, prompt("Plaza ID: "), LocalDate.parse(prompt("Opening/evaluation date (YYYY-MM-DD): ")));
        System.out.println("Toll plaza opened successfully.");
    }

    private static void listPlazas(TollPlazaService service) {
        if (service.listPlazas().isEmpty()) { System.out.println("No plazas created."); return; }
        for (TollPlaza plaza : service.listPlazas()) {
            System.out.println(plaza);
            plaza.getLanes().forEach(lane -> System.out.println("  " + lane));
        }
    }

    private static void listRates(TollPlazaService service, TollRateService rates) {
        TollPlaza plaza = service.getPlaza(prompt("Plaza ID: "));
        var list = rates.getRates(plaza);
        if (list.isEmpty()) System.out.println("No rates configured.");
        else list.stream().sorted(Comparator.comparing(TollRate::getVehicleType)
                .thenComparing(TollRate::getTripType).thenComparing(TollRate::getEffectiveDate))
                .forEach(System.out::println);
    }

    private static void simulateProcessing(TollPlazaService service) {
        String id = prompt("Plaza ID: ");
        int lane = readInt("Lane number: ");
        service.startVehicleProcessing(id, lane);
        System.out.println("Processing started. Press Enter to finish.");
        SCANNER.nextLine();
        service.finishVehicleProcessing(id, lane);
        System.out.println("Processing finished.");
    }

    // ----------------- Module 2 Actions -----------------

    private static void registerVehicle(Admin admin, VehicleService service) {
        String reg = prompt("Vehicle registration number (e.g., KA01AB1234): ");
        String owner = prompt("Owner name: ");
        String phone = prompt("Phone number: ");
        VehicleType type = readEnum("Vehicle type", VehicleType.class);
        Vehicle vehicle = service.registerVehicle(admin, reg, owner, phone, type);
        System.out.println("Registered vehicle: " + vehicle);
    }

    private static void createFASTagAccount(Admin admin, FASTagService service) {
        String tagId = prompt("Tag ID (e.g., TAG1001): ");
        String reg = prompt("Vehicle registration number: ");
        BigDecimal balance = new BigDecimal(prompt("Initial balance (Rs.): "));
        FASTagAccount account = service.createAccount(admin, tagId, reg, balance);
        System.out.println("Created FASTag account: " + account);
    }

    private static void rechargeAccount(Admin admin, FASTagService service) {
        String tagId = prompt("Tag ID: ");
        BigDecimal amount = new BigDecimal(prompt("Recharge amount (Rs.): "));
        FASTagAccount account = service.recharge(admin, tagId, amount);
        System.out.println("Recharge successful! " + account);
    }

    private static void blockAccount(Admin admin, FASTagService service) {
        String tagId = prompt("Tag ID: ");
        service.blockAccount(admin, tagId);
        System.out.println("Account blocked successfully.");
    }

    private static void blacklistAccount(Admin admin, FASTagService service) {
        String tagId = prompt("Tag ID: ");
        service.blacklistAccount(admin, tagId);
        System.out.println("Account blacklisted successfully.");
    }

    private static void viewVehicleAndAccount(VehicleService vehicleService, FASTagService fastagService) {
        String reg = prompt("Vehicle registration number: ");
        Vehicle vehicle = vehicleService.getVehicle(reg);
        System.out.println(vehicle);
        fastagService.findAccountByVehicle(reg).ifPresentOrElse(
                acc -> System.out.println("Linked FASTag: " + acc),
                () -> System.out.println("No FASTag account linked to this vehicle.")
        );
    }

    private static void listVehicles(VehicleService service) {
        var list = service.listVehicles();
        if (list.isEmpty()) {
            System.out.println("No vehicles registered.");
        } else {
            list.forEach(System.out::println);
        }
    }

    private static void listAccounts(FASTagService service) {
        var list = service.listAccounts();
        if (list.isEmpty()) {
            System.out.println("No FASTag accounts found.");
        } else {
            list.forEach(System.out::println);
        }
    }

    // ----------------- Module 3 Actions -----------------

    private static void processFastagCrossing(TollProcessingService service) {
        String plazaId = prompt("Plaza ID: ");
        int laneNumber = readInt("Lane number: ");
        String tagId = prompt("FASTag Tag ID: ");
        VehicleType observed = readEnum("Observed vehicle type", VehicleType.class);

        System.out.println("Processing FASTag crossing...");
        TollTransaction tx = service.processFastagCrossing(plazaId, laneNumber, tagId, observed, LocalDateTime.now());
        System.out.println("RESULT: " + tx);
        if (tx.isBarrierOpened()) {
            System.out.println(">>> BARRIER OPENED: Vehicle cleared to pass. <<<");
        } else {
            System.out.println(">>> BARRIER REMAINED CLOSED: " + tx.getStatus() + " <<<");
        }
    }

    private static void processCashCrossing(TollProcessingService service) {
        String plazaId = prompt("Plaza ID: ");
        int laneNumber = readInt("Lane number: ");
        String regNumber = prompt("Vehicle registration number: ");
        VehicleType observed = readEnum("Observed vehicle type", VehicleType.class);
        String redirected = prompt("Was vehicle redirected from FASTag lane without valid tag? (y/n): ");
        boolean isPenalty = "y".equalsIgnoreCase(redirected);
        BigDecimal tendered = new BigDecimal(prompt("Cash amount tendered (Rs.): "));

        System.out.println("Processing Cash crossing...");
        TollTransaction tx = service.processCashCrossing(plazaId, laneNumber, regNumber, observed, isPenalty, tendered, LocalDateTime.now());
        System.out.println("RESULT: " + tx);
        if (tx.isBarrierOpened()) {
            System.out.println(">>> BARRIER OPENED: Cash received. Vehicle cleared to pass. <<<");
        } else {
            System.out.println(">>> BARRIER REMAINED CLOSED: " + tx.getStatus() + " <<<");
        }
    }

    private static void processEmergencyCrossing(TollProcessingService service) {
        String plazaId = prompt("Plaza ID: ");
        int laneNumber = readInt("Lane number: ");
        String regNumber = prompt("Vehicle registration number: ");
        VehicleType observed = readEnum("Observed vehicle type", VehicleType.class);
        String authStr = prompt("Is this an authorized emergency vehicle (e.g. Ambulance/Fire)? (y/n): ");
        boolean isAuth = "y".equalsIgnoreCase(authStr);
        String details = prompt("Emergency service details / mission description: ");

        System.out.println("Processing Emergency crossing...");
        TollTransaction tx = service.processEmergencyCrossing(plazaId, laneNumber, regNumber, observed, isAuth, details, LocalDateTime.now());
        System.out.println("RESULT: " + tx);
        if (tx.isBarrierOpened()) {
            System.out.println(">>> BARRIER OPENED: Emergency vehicle exempted (Rs.0.00). Cleared to pass. <<<");
        } else {
            System.out.println(">>> BARRIER REMAINED CLOSED: " + tx.getStatus() + " <<<");
        }
    }

    private static void viewTransactionHistory(TollProcessingService service) {
        var list = service.getTransactionHistory();
        if (list.isEmpty()) {
            System.out.println("No transactions recorded.");
        } else {
            System.out.println("=== TRANSACTION LOG (" + list.size() + " records) ===");
            list.forEach(System.out::println);
        }
    }

    private static void searchTransactionById(TollProcessingService service) {
        String txId = prompt("Transaction ID: ");
        service.getTransactionById(txId).ifPresentOrElse(
                tx -> System.out.println("Found: " + tx),
                () -> System.out.println("Transaction not found for ID: " + txId)
        );
    }

    // ----------------- Module 4 Actions -----------------

    private static void createMonthlyPass(Admin admin, MonthlyPassService service) {
        String reg = prompt("Vehicle registration number: ");
        String plazaId = prompt("Plaza ID: ");
        LocalDate start = LocalDate.parse(prompt("Start date (YYYY-MM-DD): "));
        LocalDate end = LocalDate.parse(prompt("End date (YYYY-MM-DD): "));
        MonthlyPass pass = service.createMonthlyPass(admin, reg, plazaId, start, end);
        System.out.println("Created Monthly Pass: " + pass);
    }

    private static void setPassFee(Admin admin, MonthlyPassService service) {
        VehicleType type = readEnum("Vehicle type", VehicleType.class);
        BigDecimal fee = new BigDecimal(prompt("Monthly pass fee (Rs.): "));
        service.setPassFee(admin, type, fee);
        System.out.println("Updated pass fee for " + type + " to Rs." + fee);
    }

    private static void listMonthlyPasses(MonthlyPassService service) {
        var list = service.listPasses();
        if (list.isEmpty()) System.out.println("No monthly passes found.");
        else list.forEach(System.out::println);
    }

    // ----------------- Module 5 Actions -----------------

    private static void rechargeWithPaymentMethod(RechargeService service) {
        String tagId = prompt("FASTag Tag ID: ");
        BigDecimal amount = new BigDecimal(prompt("Recharge amount (Rs.): "));
        System.out.println("Select Payment Method:\n1. UPI\n2. Credit/Debit Card\n3. Net Banking");
        String choice = prompt("Choice (1-3): ");
        PaymentStrategy strategy = switch (choice) {
            case "2" -> new CardPaymentStrategy();
            case "3" -> new NetBankingPaymentStrategy();
            default -> new UpiPaymentStrategy();
        };

        RechargeRecord record = service.processRecharge(tagId, amount, strategy, LocalDateTime.now());
        System.out.println("Recharge Record: " + record);
        if (record.getStatus() == RechargeStatus.PAID) {
            System.out.println("Payment SUCCESS! Amount credited.");
        } else {
            System.out.println("Payment FAILED. No balance change.");
        }
    }

    private static void raiseDispute(DisputeAndRefundService service) {
        String txId = prompt("Transaction ID to dispute: ");
        String reason = prompt("Reason for dispute: ");
        Dispute dispute = service.raiseDispute(txId, reason, LocalDateTime.now());
        System.out.println("Filed Dispute: " + dispute);
    }

    private static void reviewDispute(Admin admin, DisputeAndRefundService service) {
        String dispId = prompt("Dispute ID to review: ");
        String decision = prompt("Decision (A to Approve / R to Reject): ");
        String adminReason = prompt("Admin reason/notes: ");
        if ("A".equalsIgnoreCase(decision)) {
            RefundRecord refund = service.approveDispute(admin, dispId, adminReason, LocalDateTime.now());
            System.out.println("Dispute APPROVED. Refund processed: " + refund);
        } else {
            service.rejectDispute(admin, dispId, adminReason);
            System.out.println("Dispute REJECTED.");
        }
    }

    private static void listDisputes(DisputeAndRefundService service) {
        var disputes = service.listAllDisputes();
        System.out.println("=== DISPUTES (" + disputes.size() + ") ===");
        disputes.forEach(System.out::println);

        var refunds = service.listAllRefunds();
        System.out.println("=== REFUND RECORDS (" + refunds.size() + ") ===");
        refunds.forEach(System.out::println);
    }

    private static void generateDailyRevenueReport(RevenueReportService service) {
        String plazaId = prompt("Plaza ID: ");
        LocalDate date = LocalDate.parse(prompt("Report date (YYYY-MM-DD): "));
        RevenueReportService.DailyReport report = service.generateDailyReport(plazaId, date);
        System.out.println(report);
    }

    // ----------------- Helpers -----------------

    private static String prompt(String text) { System.out.print(text); return SCANNER.nextLine().trim(); }
    private static int readInt(String label) {
        try { return Integer.parseInt(prompt(label)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a valid whole number."); }
    }
    private static <E extends Enum<E>> E readEnum(String label, Class<E> type) {
        System.out.println(label + " options:");
        E[] values = type.getEnumConstants();
        for (int i = 0; i < values.length; i++) System.out.println((i + 1) + ". " + values[i]);
        int selected = readInt("Choose number: ");
        if (selected < 1 || selected > values.length) throw new IllegalArgumentException("Invalid " + label + ".");
        return values[selected - 1];
    }
}
