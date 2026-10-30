package tollmanagement.repository;

import tollmanagement.model.FASTagAccount;
import java.util.*;

public final class FASTagAccountRepository {
    private final Map<String, FASTagAccount> accountsById = new LinkedHashMap<>();
    private final Map<String, String> accountIdByTagId = new HashMap<>();

    public void save(FASTagAccount account) {
        Objects.requireNonNull(account, "FASTag account is required.");
        String tagKey = account.getTagId().toUpperCase();
        if (accountIdByTagId.containsKey(tagKey)) {
            throw new IllegalArgumentException("FASTag with Tag ID " + account.getTagId() + " already exists.");
        }
        accountsById.put(account.getAccountId(), account);
        accountIdByTagId.put(tagKey, account.getAccountId());
    }

    public Optional<FASTagAccount> findById(String accountId) {
        if (accountId == null) return Optional.empty();
        return Optional.ofNullable(accountsById.get(accountId));
    }

    public Optional<FASTagAccount> findByTagId(String tagId) {
        if (tagId == null) return Optional.empty();
        String accountId = accountIdByTagId.get(tagId.trim().toUpperCase());
        if (accountId == null) return Optional.empty();
        return Optional.ofNullable(accountsById.get(accountId));
    }

    public FASTagAccount requireByTagId(String tagId) {
        return findByTagId(tagId)
                .orElseThrow(() -> new IllegalArgumentException("FASTag account not found for Tag ID: " + tagId));
    }

    public Optional<FASTagAccount> findByVehicleRegistration(String regNumber) {
        if (regNumber == null) return Optional.empty();
        String normalized = regNumber.trim().toUpperCase();
        return accountsById.values().stream()
                .filter(a -> a.getVehicleRegistrationNumber().equalsIgnoreCase(normalized))
                .findFirst();
    }

    public List<FASTagAccount> findAll() {
        return List.copyOf(accountsById.values());
    }
}
