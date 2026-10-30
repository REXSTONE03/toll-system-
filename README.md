# FASTag Toll Management System

A modular in-memory Java FASTag Toll Management System developed collaboratively across 5 specialized modules.

## Architecture & Modules

```
Toll System/
├── Module-1/           # Toll Plaza and Lane Setup
├── Module-2/           # Vehicle and FASTag Account Management
├── Module-3/           # Toll Processing (FASTag, Cash, Emergency)
├── Module-4/           # Return Trips, Monthly Passes & Toll Rounding
├── Module-5/           # Recharges, Disputes, Refunds & Daily Analytics Reports
└── tollmanagement/     # Root unified CLI entry point
    └── Main.java
```

### Module Breakdown
- **Module 1**: Toll plaza lifecycle, lane configuration (FASTag, Cash, Emergency), rate schedules with temporal effective-date logic.
- **Module 2**: Vehicle registration, FASTag accounts (Active, Blocked, Closed, Blacklisted), balance rules (₹100 min recharge, ₹50,000 max balance, auto-blocking < ₹100).
- **Module 3**: Automated lane crossing processing, vehicle type mismatch resolution, duplicate transaction suppression (2-min window), cash lane 2x penalty, and emergency exemptions.
- **Module 4**: Return-trip discount (50% within 24h at same plaza), monthly pass management (₹0 toll), and toll rounding to nearest ₹5.
- **Module 5**: Payment gateways (UPI, Card, Net Banking), 7-day dispute filing, admin refund approval, balance alerts (< ₹200), and daily plaza revenue reports excluding refunded transactions.

## Compilation & Execution

### Compile Full Project
```bash
javac -cp "Module-1;Module-2;Module-3;Module-4;Module-5" -d out `
  tollmanagement/Main.java `
  Module-1/tollmanagement/model/*.java Module-1/tollmanagement/repository/*.java Module-1/tollmanagement/service/*.java `
  Module-2/tollmanagement/model/*.java Module-2/tollmanagement/repository/*.java Module-2/tollmanagement/service/*.java `
  Module-3/tollmanagement/model/*.java Module-3/tollmanagement/repository/*.java Module-3/tollmanagement/service/*.java `
  Module-4/tollmanagement/model/*.java Module-4/tollmanagement/repository/*.java Module-4/tollmanagement/service/*.java `
  Module-5/tollmanagement/model/*.java Module-5/tollmanagement/repository/*.java Module-5/tollmanagement/service/*.java
```

### Run Root Interactive CLI
```bash
java -cp "out;Module-1;Module-2;Module-3;Module-4;Module-5" tollmanagement.Main
```

### Run All Module Test Suites
```bash
# Module 1 (11 tests)
java -cp out Module1Verification

# Module 2 (10 tests)
java -cp out Module2Verification

# Module 3 (10 tests)
java -cp out Module3Verification

# Module 4 (10 tests)
java -cp out Module4Verification

# Module 5 (14 tests)
java -cp out Module5Verification
```
