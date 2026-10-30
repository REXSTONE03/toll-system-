package tollmanagement.service;

import tollmanagement.model.*;
import tollmanagement.repository.TollPlazaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class TollPlazaService {
    private final TollPlazaRepository repository;
    private final TollRateService rateService;
    public TollPlazaService(TollPlazaRepository repository, TollRateService rateService) {
        this.repository = Objects.requireNonNull(repository);
        this.rateService = Objects.requireNonNull(rateService);
    }
    public TollPlaza createPlaza(Admin admin, String name, String highwayName, String location) {
        TollRateService.requireAdmin(admin);
        TollPlaza plaza = new TollPlaza(name, highwayName, location);
        repository.save(plaza);
        return plaza;
    }
    public Lane addLane(Admin admin, String plazaId, int laneNumber, LaneType laneType) {
        TollRateService.requireAdmin(admin);
        Lane lane = new Lane(laneNumber, laneType, LaneStatus.CLOSED);
        repository.requireById(plazaId).addLane(lane);
        return lane;
    }
    public void openLane(Admin admin, String plazaId, int laneNumber) {
        TollRateService.requireAdmin(admin);
        repository.requireById(plazaId).findLane(laneNumber).open();
    }
    public void closeLane(Admin admin, String plazaId, int laneNumber) {
        TollRateService.requireAdmin(admin);
        repository.requireById(plazaId).findLane(laneNumber).close();
    }
    public void startVehicleProcessing(String plazaId, int laneNumber) {
        repository.requireById(plazaId).findLane(laneNumber).startProcessing();
    }
    public void finishVehicleProcessing(String plazaId, int laneNumber) {
        repository.requireById(plazaId).findLane(laneNumber).finishProcessing();
    }
    public void openPlaza(Admin admin, String plazaId, LocalDate onDate) {
        TollRateService.requireAdmin(admin);
        TollPlaza plaza = repository.requireById(plazaId);
        if (plaza.getLanes().size() < 2)
            throw new IllegalStateException("A toll plaza must have at least two lanes before it can open.");
        if (!rateService.hasRatesForEveryVehicleType(plaza, onDate))
            throw new IllegalStateException("Cannot open plaza: configure effective rates for every vehicle type and trip type first.");
        plaza.setStatus(PlazaStatus.OPEN);
    }
    public void closePlaza(Admin admin, String plazaId) {
        TollRateService.requireAdmin(admin);
        repository.requireById(plazaId).setStatus(PlazaStatus.CLOSED);
    }
    public TollPlaza getPlaza(String plazaId) { return repository.requireById(plazaId); }
    public List<TollPlaza> listPlazas() { return repository.findAll(); }
}
