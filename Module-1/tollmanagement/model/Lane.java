package tollmanagement.model;

import java.util.Objects;

public final class Lane {
    private final int number;
    private final LaneType type;
    private LaneStatus status;
    private boolean vehicleBeingProcessed;

    public Lane(int number, LaneType type, LaneStatus status) {
        if (number < 1) throw new IllegalArgumentException("Lane number must be at least 1.");
        this.number = number;
        this.type = Objects.requireNonNull(type, "Lane type is required.");
        this.status = Objects.requireNonNull(status, "Lane status is required.");
    }
    public int getNumber() { return number; }
    public LaneType getType() { return type; }
    public LaneStatus getStatus() { return status; }
    public boolean isVehicleBeingProcessed() { return vehicleBeingProcessed; }

    public void startProcessing() {
        if (status != LaneStatus.OPEN) throw new IllegalStateException("Cannot process a vehicle: lane is closed.");
        if (vehicleBeingProcessed) throw new IllegalStateException("A vehicle is already being processed in this lane.");
        vehicleBeingProcessed = true;
    }
    public void finishProcessing() {
        if (!vehicleBeingProcessed) throw new IllegalStateException("No vehicle is being processed in this lane.");
        vehicleBeingProcessed = false;
    }
    public void close() {
        if (vehicleBeingProcessed) throw new IllegalStateException("Lane cannot be closed while a vehicle is being processed.");
        status = LaneStatus.CLOSED;
    }
    public void open() { status = LaneStatus.OPEN; }
    @Override public String toString() {
        return "Lane " + number + " [" + type + ", " + status + (vehicleBeingProcessed ? ", processing vehicle" : "") + "]";
    }
}
