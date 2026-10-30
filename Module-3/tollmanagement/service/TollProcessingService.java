package tollmanagement.service;

import tollmanagement.model.*;
import tollmanagement.repository.FASTagAccountRepository;
import tollmanagement.repository.TollPlazaRepository;
import tollmanagement.repository.TollTransactionRepository;
import tollmanagement.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TollProcessingService {
    public static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(2);
    public static final BigDecimal DOUBLE_TOLL_MULTIPLIER = new BigDecimal("2.00");

    private final TollPlazaRepository plazaRepository;
    private final TollRateService rateService;
    private final FASTagAccountRepository accountRepository;
    private final VehicleRepository vehicleRepository;
    private final TollTransactionRepository transactionRepository;

    // Optional Module 4 extensions
    private MonthlyPassService monthlyPassService;
    private ReturnTripService returnTripService;

    public TollProcessingService(TollPlazaRepository plazaRepository,
                                 TollRateService rateService,
                                 FASTagAccountRepository accountRepository,
                                 VehicleRepository vehicleRepository,
                                 TollTransactionRepository transactionRepository) {
        this.plazaRepository = Objects.requireNonNull(plazaRepository, "Plaza repository is required.");
        this.rateService = Objects.requireNonNull(rateService, "Rate service is required.");
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository is required.");
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "Vehicle repository is required.");
        this.transactionRepository = Objects.requireNonNull(transactionRepository, "Transaction repository is required.");
    }

    public void setMonthlyPassService(MonthlyPassService monthlyPassService) {
        this.monthlyPassService = monthlyPassService;
    }

    public void setReturnTripService(ReturnTripService returnTripService) {
        this.returnTripService = returnTripService;
    }

    /**
     * Process vehicle in a FASTAG lane.
     */
    public TollTransaction processFastagCrossing(String plazaId,
                                                 int laneNumber,
                                                 String tagId,
                                                 VehicleType observedType,
                                                 LocalDateTime crossingTime) {
        Objects.requireNonNull(crossingTime, "Crossing time is required.");
        Objects.requireNonNull(observedType, "Observed vehicle type is required.");

        TollPlaza plaza = plazaRepository.requireById(plazaId);
        Lane lane = plaza.findLane(laneNumber);

        // Lane validation
        if (lane.getStatus() != LaneStatus.OPEN) {
            TollTransaction failed = new TollTransaction(
                    null, tagId, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                    PaymentMode.FASTAG, TransactionStatus.FAILED_LANE_CLOSED, null, observedType,
                    false, "Lane " + laneNumber + " is CLOSED", false);
            transactionRepository.save(failed);
            return failed;
        }

        if (lane.getType() != LaneType.FASTAG) {
            TollTransaction failed = new TollTransaction(
                    null, tagId, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                    PaymentMode.FASTAG, TransactionStatus.FAILED_LANE_MISMATCH, null, observedType,
                    false, "Lane " + laneNumber + " is not a dedicated FASTAG lane", false);
            transactionRepository.save(failed);
            return failed;
        }

        lane.startProcessing();
        try {
            // 1. Tag & Account lookup
            Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(tagId);
            if (accountOpt.isEmpty()) {
                TollTransaction failed = new TollTransaction(
                        null, tagId, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                        PaymentMode.FASTAG, TransactionStatus.FAILED_INVALID_TAG, null, observedType,
                        true, "Unknown FASTag tag ID: " + tagId + ". Divert to CASH lane.", false);
                transactionRepository.save(failed);
                return failed;
            }

            FASTagAccount account = accountOpt.get();
            String vehicleReg = account.getVehicleRegistrationNumber();
            Optional<Vehicle> vehicleOpt = vehicleRepository.findByRegistrationNumber(vehicleReg);
            VehicleType registeredType = vehicleOpt.map(Vehicle::getVehicleType).orElse(observedType);

            // 2. Account Status Check
            if (account.getStatus() == FASTagStatus.BLOCKED) {
                TollTransaction failed = new TollTransaction(
                        vehicleReg, tagId, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                        PaymentMode.FASTAG, TransactionStatus.FAILED_BLOCKED_TAG, registeredType, observedType,
                        true, "FASTag account is BLOCKED (low balance or admin blocked). Divert to CASH lane.", false);
                transactionRepository.save(failed);
                return failed;
            }
            if (account.getStatus() == FASTagStatus.BLACKLISTED) {
                TollTransaction failed = new TollTransaction(
                        vehicleReg, tagId, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                        PaymentMode.FASTAG, TransactionStatus.FAILED_BLACKLISTED_TAG, registeredType, observedType,
                        true, "FASTag account is BLACKLISTED. Divert to CASH lane.", false);
                transactionRepository.save(failed);
                return failed;
            }
            if (account.getStatus() == FASTagStatus.CLOSED) {
                TollTransaction failed = new TollTransaction(
                        vehicleReg, tagId, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                        PaymentMode.FASTAG, TransactionStatus.FAILED_CLOSED_ACCOUNT, registeredType, observedType,
                        true, "FASTag account is CLOSED. Divert to CASH lane.", false);
                transactionRepository.save(failed);
                return failed;
            }
            if (account.getStatus() != FASTagStatus.ACTIVE) {
                TollTransaction failed = new TollTransaction(
                        vehicleReg, tagId, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                        PaymentMode.FASTAG, TransactionStatus.FAILED_INVALID_TAG, registeredType, observedType,
                        true, "FASTag account is not ACTIVE. Divert to CASH lane.", false);
                transactionRepository.save(failed);
                return failed;
            }

            // 3. Duplicate crossing check (within 2 minutes at the same plaza)
            if (isDuplicateCrossing(plazaId, tagId, crossingTime)) {
                TollTransaction duplicate = new TollTransaction(
                        vehicleReg, tagId, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                        PaymentMode.FASTAG, TransactionStatus.FAILED_DUPLICATE_CROSSING, registeredType, observedType,
                        true, "Duplicate crossing attempt within 2 minutes at plaza " + plazaId, false);
                transactionRepository.save(duplicate);
                return duplicate;
            }

            // 4. Monthly Pass Check (Module 4) - Valid pass pays Rs.0
            if (monthlyPassService != null) {
                Optional<MonthlyPass> passOpt = monthlyPassService.findValidPass(vehicleReg, plazaId, crossingTime.toLocalDate());
                if (passOpt.isPresent()) {
                    TollTransaction passSuccess = new TollTransaction(
                            vehicleReg, tagId, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                            PaymentMode.FASTAG, TransactionStatus.SUCCESS, registeredType, observedType,
                            false, "Monthly Pass applied: " + passOpt.get().getPassId(), true);
                    transactionRepository.save(passSuccess);
                    return passSuccess;
                }
            }

            // 5. Rate Calculation & Type Mismatch Check
            BigDecimal applicableRate;
            boolean flaggedForReview = false;
            String reviewReason = null;

            if (registeredType != observedType) {
                flaggedForReview = true;
                BigDecimal regRate = getRate(plaza, registeredType, crossingTime);
                BigDecimal obsRate = getRate(plaza, observedType, crossingTime);
                if (obsRate.compareTo(regRate) > 0) {
                    applicableRate = obsRate;
                    reviewReason = "Vehicle type mismatch: registered=" + registeredType
                            + ", observed=" + observedType + ". Charged higher observed rate: Rs." + obsRate;
                } else {
                    applicableRate = regRate;
                    reviewReason = "Vehicle type mismatch: registered=" + registeredType
                            + ", observed=" + observedType + ". Charged registered rate: Rs." + regRate;
                }
            } else {
                applicableRate = getRate(plaza, observedType, crossingTime);
            }

            // 6. Return-Trip Discount Check (Module 4) - 50% discount if returning within 24h
            if (returnTripService != null && returnTripService.isEligibleForReturnDiscount(vehicleReg, plazaId, crossingTime)) {
                applicableRate = TollCalculator.applyReturnTripDiscount(applicableRate);
                reviewReason = (reviewReason != null ? reviewReason + " | " : "") + "50% Return Trip discount applied";
            }

            // 7. Toll Rounding to nearest Rs.5 (Module 4)
            applicableRate = TollCalculator.roundToNearestFive(applicableRate);

            // 8. Balance Check & Deduction
            if (account.getBalance().compareTo(applicableRate) < 0) {
                TollTransaction failed = new TollTransaction(
                        vehicleReg, tagId, plazaId, laneNumber, crossingTime, applicableRate,
                        PaymentMode.FASTAG, TransactionStatus.FAILED_INSUFFICIENT_BALANCE, registeredType, observedType,
                        flaggedForReview, (reviewReason != null ? reviewReason + " | " : "")
                        + "Insufficient balance (current: Rs." + account.getBalance() + ", needed: Rs." + applicableRate + "). Divert to CASH lane.", false);
                transactionRepository.save(failed);
                return failed;
            }

            // Deduct balance
            account.deduct(applicableRate);

            // Low-balance alert check (Module 5)
            Optional<String> lowBalanceAlert = BalanceAlertService.checkLowBalance(account);
            if (lowBalanceAlert.isPresent()) {
                reviewReason = (reviewReason != null ? reviewReason + " | " : "") + lowBalanceAlert.get();
            }

            // Barrier opens after successful payment
            TollTransaction success = new TollTransaction(
                    vehicleReg, tagId, plazaId, laneNumber, crossingTime, applicableRate,
                    PaymentMode.FASTAG, TransactionStatus.SUCCESS, registeredType, observedType,
                    flaggedForReview, reviewReason, true);
            transactionRepository.save(success);
            return success;

        } finally {
            lane.finishProcessing();
        }
    }

    /**
     * Process vehicle in a CASH lane.
     */
    public TollTransaction processCashCrossing(String plazaId,
                                               int laneNumber,
                                               String vehicleRegistrationNumber,
                                               VehicleType observedType,
                                               boolean hasInvalidFastagEntry,
                                               BigDecimal cashTendered,
                                               LocalDateTime crossingTime) {
        Objects.requireNonNull(crossingTime, "Crossing time is required.");
        Objects.requireNonNull(observedType, "Observed vehicle type is required.");
        Objects.requireNonNull(cashTendered, "Cash tendered is required.");

        TollPlaza plaza = plazaRepository.requireById(plazaId);
        Lane lane = plaza.findLane(laneNumber);

        if (lane.getStatus() != LaneStatus.OPEN) {
            TollTransaction failed = new TollTransaction(
                    vehicleRegistrationNumber, null, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                    PaymentMode.CASH, TransactionStatus.FAILED_LANE_CLOSED, null, observedType,
                    false, "Lane " + laneNumber + " is CLOSED", false);
            transactionRepository.save(failed);
            return failed;
        }

        if (lane.getType() != LaneType.CASH) {
            TollTransaction failed = new TollTransaction(
                    vehicleRegistrationNumber, null, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                    PaymentMode.CASH, TransactionStatus.FAILED_LANE_MISMATCH, null, observedType,
                    false, "Lane " + laneNumber + " is not a CASH lane", false);
            transactionRepository.save(failed);
            return failed;
        }

        lane.startProcessing();
        try {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findByRegistrationNumber(vehicleRegistrationNumber);
            VehicleType registeredType = vehicleOpt.map(Vehicle::getVehicleType).orElse(null);

            BigDecimal baseRate = getRate(plaza, observedType, crossingTime);
            BigDecimal finalCharge = baseRate;
            boolean flagged = false;
            String reviewReason = null;

            if (hasInvalidFastagEntry) {
                finalCharge = baseRate.multiply(DOUBLE_TOLL_MULTIPLIER);
                flagged = true;
                reviewReason = "Double toll penalty applied: vehicle entered without valid FASTag";
            } else if (returnTripService != null && returnTripService.isEligibleForReturnDiscount(vehicleRegistrationNumber, plazaId, crossingTime)) {
                // Return trip discount on normal cash crossing
                finalCharge = TollCalculator.applyReturnTripDiscount(finalCharge);
                reviewReason = "50% Return Trip discount applied";
            }

            if (registeredType != null && registeredType != observedType) {
                flagged = true;
                BigDecimal regRate = getRate(plaza, registeredType, crossingTime);
                if (hasInvalidFastagEntry) regRate = regRate.multiply(DOUBLE_TOLL_MULTIPLIER);
                if (regRate.compareTo(finalCharge) > 0) {
                    finalCharge = regRate;
                }
                reviewReason = (reviewReason != null ? reviewReason + " | " : "")
                        + "Vehicle type mismatch: registered=" + registeredType + ", observed=" + observedType;
            }

            // Apply rounding
            finalCharge = TollCalculator.roundToNearestFive(finalCharge);

            if (cashTendered.compareTo(finalCharge) < 0) {
                TollTransaction failed = new TollTransaction(
                        vehicleRegistrationNumber, null, plazaId, laneNumber, crossingTime, finalCharge,
                        PaymentMode.CASH, TransactionStatus.FAILED, registeredType, observedType,
                        flagged, "Insufficient cash tendered (tendered: Rs." + cashTendered + ", required: Rs." + finalCharge + ")", false);
                transactionRepository.save(failed);
                return failed;
            }

            TollTransaction success = new TollTransaction(
                    vehicleRegistrationNumber, null, plazaId, laneNumber, crossingTime, finalCharge,
                    PaymentMode.CASH, TransactionStatus.SUCCESS, registeredType, observedType,
                    flagged, reviewReason, true);
            transactionRepository.save(success);
            return success;

        } finally {
            lane.finishProcessing();
        }
    }

    /**
     * Process vehicle in an EMERGENCY lane.
     */
    public TollTransaction processEmergencyCrossing(String plazaId,
                                                    int laneNumber,
                                                    String vehicleRegistrationNumber,
                                                    VehicleType observedType,
                                                    boolean isAuthorizedEmergency,
                                                    String emergencyServiceDetails,
                                                    LocalDateTime crossingTime) {
        Objects.requireNonNull(crossingTime, "Crossing time is required.");
        Objects.requireNonNull(observedType, "Observed vehicle type is required.");

        TollPlaza plaza = plazaRepository.requireById(plazaId);
        Lane lane = plaza.findLane(laneNumber);

        if (lane.getStatus() != LaneStatus.OPEN) {
            TollTransaction failed = new TollTransaction(
                    vehicleRegistrationNumber, null, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                    PaymentMode.EXEMPTED, TransactionStatus.FAILED_LANE_CLOSED, null, observedType,
                    false, "Lane " + laneNumber + " is CLOSED", false);
            transactionRepository.save(failed);
            return failed;
        }

        if (lane.getType() != LaneType.EMERGENCY) {
            TollTransaction failed = new TollTransaction(
                    vehicleRegistrationNumber, null, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                    PaymentMode.EXEMPTED, TransactionStatus.FAILED_LANE_MISMATCH, null, observedType,
                    false, "Lane " + laneNumber + " is not an EMERGENCY lane", false);
            transactionRepository.save(failed);
            return failed;
        }

        lane.startProcessing();
        try {
            if (!isAuthorizedEmergency) {
                TollTransaction failed = new TollTransaction(
                        vehicleRegistrationNumber, null, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                        PaymentMode.EXEMPTED, TransactionStatus.FAILED, null, observedType,
                        true, "Unauthorized emergency lane attempt: " + emergencyServiceDetails, false);
                transactionRepository.save(failed);
                return failed;
            }

            // Authorized emergency vehicle: 0 charge, barrier opened, EXEMPTED status
            TollTransaction exempted = new TollTransaction(
                    vehicleRegistrationNumber, null, plazaId, laneNumber, crossingTime, BigDecimal.ZERO,
                    PaymentMode.EXEMPTED, TransactionStatus.EXEMPTED, null, observedType,
                    false, "Emergency crossing: " + (emergencyServiceDetails != null ? emergencyServiceDetails : "Authorized"), true);
            transactionRepository.save(exempted);
            return exempted;

        } finally {
            lane.finishProcessing();
        }
    }

    private BigDecimal getRate(TollPlaza plaza, VehicleType vehicleType, LocalDateTime crossingTime) {
        return rateService.findRate(plaza, vehicleType, TripType.SINGLE, crossingTime.toLocalDate())
                .map(TollRate::getAmount)
                .orElseThrow(() -> new IllegalStateException("No active toll rate found for vehicle type "
                        + vehicleType + " at plaza " + plaza.getId() + " on " + crossingTime.toLocalDate()));
    }

    private boolean isDuplicateCrossing(String plazaId, String tagId, LocalDateTime crossingTime) {
        if (tagId == null) return false;
        List<TollTransaction> recent = transactionRepository.findByTagId(tagId);
        return recent.stream()
                .filter(t -> t.getPlazaId().equalsIgnoreCase(plazaId))
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .anyMatch(t -> {
                    Duration duration = Duration.between(t.getTimestamp(), crossingTime).abs();
                    return duration.compareTo(DUPLICATE_WINDOW) < 0;
                });
    }

    public List<TollTransaction> getTransactionHistory() {
        return transactionRepository.findAll();
    }

    public Optional<TollTransaction> getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    public List<TollTransaction> getTransactionsByPlaza(String plazaId) {
        return transactionRepository.findByPlazaId(plazaId);
    }

    public List<TollTransaction> getTransactionsByVehicle(String regNumber) {
        return transactionRepository.findByVehicleRegistration(regNumber);
    }
}
