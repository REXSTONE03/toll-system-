package tollmanagement.service;

import tollmanagement.model.*;
import tollmanagement.repository.FASTagAccountRepository;
import tollmanagement.repository.VehicleRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FASTagService {
    private final FASTagAccountRepository accountRepository;
    private final VehicleRepository vehicleRepository;

    public FASTagService(FASTagAccountRepository accountRepository, VehicleRepository vehicleRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository is required.");
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "Vehicle repository is required.");
    }

    public FASTagAccount createAccount(Admin admin, String tagId, String vehicleRegistrationNumber, BigDecimal initialBalance) {
        TollRateService.requireAdmin(admin);

        Vehicle vehicle = vehicleRepository.requireByRegistrationNumber(vehicleRegistrationNumber);

        // One vehicle can have only one active FASTag
        if (vehicle.hasActiveTag()) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber()
                    + " already has an active FASTag: " + vehicle.getActiveTagId());
        }

        FASTagAccount account = new FASTagAccount(tagId, vehicle.getRegistrationNumber(), initialBalance);
        accountRepository.save(account);

        if (account.getStatus() == FASTagStatus.ACTIVE) {
            vehicle.linkActiveTag(account.getTagId());
        }

        return account;
    }

    public FASTagAccount recharge(Admin admin, String tagId, BigDecimal amount) {
        TollRateService.requireAdmin(admin);
        FASTagAccount account = accountRepository.requireByTagId(tagId);
        FASTagStatus statusBefore = account.getStatus();

        account.recharge(amount);

        // If reactivated from BLOCKED to ACTIVE, link vehicle's active tag if not linked
        if (statusBefore == FASTagStatus.BLOCKED && account.getStatus() == FASTagStatus.ACTIVE) {
            vehicleRepository.findByRegistrationNumber(account.getVehicleRegistrationNumber())
                    .ifPresent(v -> v.linkActiveTag(account.getTagId()));
        }

        return account;
    }

    public void blockAccount(Admin admin, String tagId) {
        TollRateService.requireAdmin(admin);
        FASTagAccount account = accountRepository.requireByTagId(tagId);
        account.block();
        vehicleRepository.findByRegistrationNumber(account.getVehicleRegistrationNumber())
                .ifPresent(Vehicle::unlinkActiveTag);
    }

    public void blacklistAccount(Admin admin, String tagId) {
        TollRateService.requireAdmin(admin);
        FASTagAccount account = accountRepository.requireByTagId(tagId);
        account.blacklist();
        vehicleRepository.findByRegistrationNumber(account.getVehicleRegistrationNumber())
                .ifPresent(Vehicle::unlinkActiveTag);
    }

    public void closeAccount(Admin admin, String tagId) {
        TollRateService.requireAdmin(admin);
        FASTagAccount account = accountRepository.requireByTagId(tagId);
        account.close();
        vehicleRepository.findByRegistrationNumber(account.getVehicleRegistrationNumber())
                .ifPresent(Vehicle::unlinkActiveTag);
    }

    public FASTagAccount getAccountByTagId(String tagId) {
        return accountRepository.requireByTagId(tagId);
    }

    public Optional<FASTagAccount> findAccountByTagId(String tagId) {
        return accountRepository.findByTagId(tagId);
    }

    public Optional<FASTagAccount> findAccountByVehicle(String regNumber) {
        return accountRepository.findByVehicleRegistration(regNumber);
    }

    public List<FASTagAccount> listAccounts() {
        return accountRepository.findAll();
    }
}
