package tollmanagement;

import tollmanagement.model.*;
import tollmanagement.repository.TollPlazaRepository;
import tollmanagement.service.TollPlazaService;
import tollmanagement.service.TollRateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Scanner;

public final class Main {
    private static final Scanner SCANNER = new Scanner(System.in);
    public static void main(String[] args) {
        Admin admin = new Admin("admin");
        TollPlazaRepository repository = new TollPlazaRepository();
        TollRateService rateService = new TollRateService();
        TollPlazaService plazaService = new TollPlazaService(repository, rateService);
        System.out.println("TOLL MANAGEMENT SYSTEM - MODULE 1: Toll Plaza and Lane Setup");
        System.out.println("Demo admin identity: admin (no password authentication configured)");
        boolean running = true;
        while (running) {
            printMenu();
            try {
                switch (prompt("Choose an option: ")) {
                    case "1" -> createPlaza(admin, plazaService);
                    case "2" -> addLane(admin, plazaService);
                    case "3" -> openLane(admin, plazaService);
                    case "4" -> closeLane(admin, plazaService);
                    case "5" -> setRate(admin, plazaService, rateService);
                    case "6" -> openPlaza(admin, plazaService);
                    case "7" -> listPlazas(plazaService);
                    case "8" -> listRates(plazaService, rateService);
                    case "9" -> simulateProcessing(plazaService);
                    case "0" -> running = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (RuntimeException ex) {
                System.out.println("ERROR: " + ex.getMessage());
            }
            System.out.println();
        }
        System.out.println("Module 1 closed. Goodbye!");
    }
    private static void printMenu() {
        System.out.println("\n--- ADMIN MENU ---");
        System.out.println("1. Create toll plaza");
        System.out.println("2. Add lane to plaza");
        System.out.println("3. Open lane");
        System.out.println("4. Close lane");
        System.out.println("5. Set / update toll rate");
        System.out.println("6. Open toll plaza (validates lanes and rates)");
        System.out.println("7. List toll plazas and lanes");
        System.out.println("8. List rates for a plaza");
        System.out.println("9. Simulate vehicle processing");
        System.out.println("0. Exit");
    }
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
