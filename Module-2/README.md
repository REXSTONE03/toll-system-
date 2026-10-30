# Module 2: Vehicle & FASTag Account Management

## Responsibilities
- Vehicle registration with unique license numbers, owner details, and vehicle type.
- FASTag account management (creation, balance checks, recharge limits).
- Account status lifecycle: `ACTIVE`, `BLOCKED` (< ₹100 balance), `CLOSED`, `BLACKLISTED`.
- Strict 1:1 active tag association per vehicle.

## Key Classes & Enums
- `tollmanagement.model.Vehicle`: Represents registered vehicle and active FASTag association.
- `tollmanagement.model.FASTagAccount`: Account model enforcing financial limits (Min ₹100 recharge, Max ₹10,000 recharge, Max ₹50,000 balance).
- `tollmanagement.model.FASTagStatus`: `ACTIVE`, `BLOCKED`, `CLOSED`, `BLACKLISTED`.
- `tollmanagement.repository.VehicleRepository`: In-memory storage for vehicles.
- `tollmanagement.repository.FASTagAccountRepository`: In-memory storage for FASTag accounts.
- `tollmanagement.service.VehicleService`: Vehicle registration and queries.
- `tollmanagement.service.FASTagService`: Account creation, recharge, blocking, and blacklisting.

## Dependencies on Other Modules
- **Module 1**: Reuses `Admin` and `VehicleType` from `Module-1/tollmanagement/model/`.

## Compilation & Verification
```bash
# Compile with Module-1 on classpath
javac -cp "../Module-1" -d out tollmanagement/model/*.java tollmanagement/repository/*.java tollmanagement/service/*.java Module2Verification.java

# Run Module 2 Verification
java -cp "out;../Module-1" Module2Verification
```
