package metroline.core.world.event.base;

import metroline.core.world.GameWorld;
import metroline.objects.enums.StationType;
import metroline.objects.gameobjects.Station;
import metroline.util.MetroLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для локальных событий, влияющих на конкретные станции.
 */
public class LocalWorldEvent extends WorldEvent {

    private Station targetStation;
    private List<Station> affectedStations; // Для событий, которые могут затронуть несколько станций (например, замыкание на линии)
 //   private final Station originalState; // Сохранение состояния для отката


    public LocalWorldEvent(GameWorld world, Station targetStation, String name, String description, long duration, float severity,
            boolean blocksStation, float revenueMultiplier, float wearRateMultiplier) {
        super(world, name, description, duration, severity);
        this.targetStation = targetStation;

        this.affectedStations = new ArrayList<>();
        this.affectedStations.add(targetStation);
    }

    public Station getTargetStation() { return targetStation; }
    public List<Station> getAffectedStations() { return affectedStations; }

    @Override
    public void applyEffects() {}

    @Override
    public void removeEffects() {
        MetroLogger.logInfo("Локальное событие на станции '" + targetStation.getName() + "' ликвидировано.");

        if (targetStation.getType() != StationType.BURNED &&
                targetStation.getType() != StationType.DROWNED) {

                targetStation.setType(StationType.REGULAR);
                targetStation.updateType();

        }
    }
}
