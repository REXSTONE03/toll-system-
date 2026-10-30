package tollmanagement.repository;

import tollmanagement.model.TollPlaza;
import java.util.*;

public final class TollPlazaRepository {
    private final Map<String, TollPlaza> plazasById = new LinkedHashMap<>();
    public void save(TollPlaza plaza) {
        Objects.requireNonNull(plaza, "Plaza is required.");
        boolean duplicate = plazasById.values().stream().anyMatch(p ->
            p.getName().equalsIgnoreCase(plaza.getName()) &&
            p.getHighwayName().equalsIgnoreCase(plaza.getHighwayName()));
        if (duplicate) throw new IllegalArgumentException("A plaza with this name already exists on this highway.");
        plazasById.put(plaza.getId(), plaza);
    }
    public Optional<TollPlaza> findById(String id) { return Optional.ofNullable(plazasById.get(id)); }
    public List<TollPlaza> findAll() { return List.copyOf(plazasById.values()); }
    public TollPlaza requireById(String id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Toll plaza not found: " + id));
    }
}
