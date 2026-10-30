# Module 3: Toll Processing

## Responsibilities
- Automated FASTag lane transit processing with balance deduction and barrier triggering.
- Vehicle type mismatch detection (charges higher rate and flags for review).
- Cash lane processing with 2x penalty for vehicles redirected from FASTag lanes without valid tags.
- Emergency lane exemption (₹0 charge for authorized emergency vehicles).
- Duplicate transaction suppression (rejects duplicate crossings within 2 minutes at the same plaza).
- Complete immutable transaction logging.

## Key Classes & Enums
- `tollmanagement.model.TollTransaction`: Immutable audit record of crossings.
- `tollmanagement.model.PaymentMode`: `FASTAG`, `CASH`, `EXEMPTED`.
- `tollmanagement.model.TransactionStatus`: Detailed status outcomes (e.g., `SUCCESS`, `FAILED_INSUFFICIENT_BALANCE`, `FAILED_DUPLICATE_CROSSING`, `EXEMPTED`).
- `tollmanagement.repository.TollTransactionRepository`: Thread-safe transaction journal.
- `tollmanagement.service.TollProcessingService`: Toll processing orchestrator.

## Dependencies on Other Modules
- **Module 1**: Reuses `TollPlaza`, `Lane`, `LaneType`, `LaneStatus`, `VehicleType`, `TripType`, `TollRate`, `TollPlazaRepository`, and `TollRateService`.
- **Module 2**: Reuses `Vehicle`, `FASTagAccount`, `FASTagStatus`, `VehicleRepository`, and `FASTagAccountRepository`.

## Compilation & Verification
```bash
# Compile with Module-1 and Module-2 on classpath
javac -cp "../Module-1;../Module-2" -d out tollmanagement/model/*.java tollmanagement/repository/*.java tollmanagement/service/*.java Module3Verification.java

# Run Module 3 Verification
java -cp "out;../Module-1;../Module-2" Module3Verification
```
