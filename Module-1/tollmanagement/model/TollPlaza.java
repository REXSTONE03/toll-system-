package tollmanagement.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TollPlaza {
    private final String id;
    private final String name;
    private final String highwayName;
    private final String location;
    private final List<Lane> lanes = new ArrayList<>();
    private PlazaStatus status = PlazaStatus.CLOSED;

    public TollPlaza(String name, String highwayName, String location) {
        this.id = UUID.randomUUID().toString();
        this.name = requireText(name, "Plaza name");
        this.highwayName = requireText(highwayName, "Highway name");
        this.location = requireText(location, "Location");
    }
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank.");
        return value.trim();
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getHighwayName() { return highwayName; }
    public String getLocation() { return location; }
    public PlazaStatus getStatus() { return status; }
    public List<Lane> getLanes() { return Collections.unmodifiableList(lanes); }
    public void addLane(Lane lane) {
        Objects.requireNonNull(lane, "Lane is required.");
        if (lanes.stream().anyMatch(existing -> existing.getNumber() == lane.getNumber()))
            throw new IllegalArgumentException("Lane number " + lane.getNumber() + " already exists at this plaza.");
        lanes.add(lane);
    }
    public Lane findLane(int laneNumber) {
        return lanes.stream().filter(l -> l.getNumber() == laneNumber).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Lane " + laneNumber + " was not found."));
    }
    public void setStatus(PlazaStatus status) { this.status = Objects.requireNonNull(status); }
    @Override public String toString() {
        return name + " | Highway: " + highwayName + " | Location: " + location
            + " | Status: " + status + " | Lanes: " + lanes.size() + " | ID: " + id;
    }
}
