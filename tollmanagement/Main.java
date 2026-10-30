package tollmanagement;

import tollmanagement.model.*;
import tollmanagement.repository.FASTagAccountRepository;
import tollmanagement.repository.TollPlazaRepository;
import tollmanagement.repository.TollTransactionRepository;
import tollmanagement.repository.VehicleRepository;
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

        // Services
        TollRateService rateService = new TollRateService();
        TollPlazaService plazaService = new TollPlazaService(plazaRepository, rateService);
        VehicleService vehicleService = new VehicleService(vehicleRepository);
        FASTagService fastagService = new FASTagService(accountRepository, vehicleRepository);
        TollProcessingService processingService = new TollProcessingService(
                plazaRepository, rateService, accountRepository, vehicleRepository, transactionRepository);

        System.out.println("TOLL MANAGEMENT SYSTEM - MODULES 1, 2 & 3");
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
        System.out.println("12. Recharge FASTag account");
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
        System.out.println("0. Exit");
    }

    // ----------------- Module 1 Actions -----------------

    private static void createPlaza(Admin admin, TollPlazaService service) {
        TollPlaza plaza = service.createPlaza(admin, prompt("Plaza name: "),
                prompt("Highway name: "), prompt("Location: "));
        System.out.println("Created plaza ID: " + plaza.getId());
        System.out.println("Add at least 2 lanes and configure all 10 vehicle/trip rate combinations before opening.");
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
        String redirected = prompt("Was vehicle redirected from FASTag lane? (y/n): ");
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
