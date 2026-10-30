# Module 1: Toll Plaza & Lane Management

## Responsibilities
- Toll Plaza lifecycle management (creation, listing, opening validation, closure).
- Lane setup and management (FASTag, Cash, and Emergency lanes).
- Toll Rate configuration and temporal resolution based on effective dates.
- In-memory data store for toll plazas.

## Key Classes & Enums
- `tollmanagement.model.TollPlaza`: Aggregate root for toll plazas.
- `tollmanagement.model.Lane`: Physical lane entity tracking processing status.
- `tollmanagement.model.TollRate`: Rate mapping by vehicle type, trip type, and effective date.
- `tollmanagement.model.LaneType`: `FASTAG`, `CASH`, `EMERGENCY`.
- `tollmanagement.model.LaneStatus`: `OPEN`, `CLOSED`.
- `tollmanagement.model.PlazaStatus`: `OPEN`, `CLOSED`.
- `tollmanagement.model.VehicleType`: `CAR`, `VAN`, `BUS`, `TRUCK`, `MULTI_AXLE`.
- `tollmanagement.model.TripType`: `SINGLE`, `RETURN`.
- `tollmanagement.model.Admin`: Administrative identity.
- `tollmanagement.repository.TollPlazaRepository`: In-memory storage for plazas.
- `tollmanagement.service.TollPlazaService`: Plaza and lane orchestration.
- `tollmanagement.service.TollRateService`: Toll rate storage and matrix evaluation.

## Compilation & Verification
```bash
# Compile Module 1 standalone
javac -d out tollmanagement/Main.java tollmanagement/model/*.java tollmanagement/repository/*.java tollmanagement/service/*.java Module1Verification.java

# Run Module 1 Verification
java -cp out Module1Verification
```
