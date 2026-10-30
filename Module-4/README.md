# Module 4: Return Trips & Monthly Passes

## Responsibilities
- **Return Trip Management**: Identifies vehicles returning to the same toll plaza within 24 hours of their previous successful crossing and automatically applies a 50% discount on the single-trip toll rate.
- **Monthly Pass Management**: Enables administrators to issue and configure monthly passes tied to specific vehicles and toll plazas. Valid passes grant a ₹0 toll crossing during their active date range. Overlapping active passes for the same vehicle/plaza are rejected.
- **Toll Rounding**: Implements financial rounding of toll charges to the nearest ₹5 after applying applicable discounts, leaving ₹0/exempt transactions untouched.

## Key Classes & Enums
- `tollmanagement.model.MonthlyPass`: Entity defining vehicle, plaza, date validity, and active flag.
- `tollmanagement.model.TollCalculator`: Pure utility for 50% return-trip discount and rounding to the nearest ₹5.
- `tollmanagement.repository.MonthlyPassRepository`: Thread-safe in-memory store enforcing overlap constraints.
- `tollmanagement.service.MonthlyPassService`: Domain service for pass generation and fee configuration.
- `tollmanagement.service.ReturnTripService`: Queries transaction history to evaluate return-trip eligibility within 24 hours.

## Compilation & Verification
```bash
# Compile and run test suite
javac -cp "../Module-1;../Module-2;../Module-3" -d out tollmanagement/model/*.java tollmanagement/repository/*.java tollmanagement/service/*.java Module4Verification.java
java -cp "out;../Module-1;../Module-2;../Module-3" Module4Verification
```
