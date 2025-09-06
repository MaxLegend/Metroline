package metroline.core.world.event;

import metroline.core.world.GameWorld;
import metroline.core.world.event.base.LocalWorldEvent;
import metroline.objects.enums.StationType;
import metroline.objects.gameobjects.Station;
import metroline.util.MetroLogger;

/**
 * Событие затопления станции
 */
public class FloodedStationEvent extends LocalWorldEvent {

    public FloodedStationEvent(GameWorld world, Station targetStation) {
        super(world,
                targetStation,
                "event.station_flood",
                "event.station_flood_desc",
                240000, // 4 минуты длительность
                0.8f,   // высокая серьезность
                true,   // блокирует станцию
                0.0f,   // доходов нет
                3.0f    // износ увеличивается в 3 раза
        );
    }

    @Override
    public void applyEffects() {
        super.applyEffects();
        MetroLogger.logInfo("🌊 ЗАТОПЛЕНИЕ на станции '" + getTargetStation().getName() + "'! Станция закрыта до откачки воды.");

        // Меняем тип станции на специальный для затопления
        getTargetStation().setType(StationType.DROWNED);
    }

    @Override
    public void removeEffects() {
        // После затопления станция становится разрушенной
        getTargetStation().setType(StationType.DROWNED);
        MetroLogger.logInfo("Затопление на станции '" + getTargetStation().getName() + "' ликвидировано. Станция требует восстановления.");

        // Не вызываем super.removeEffects() чтобы не восстанавливать исходное состояние
    }
}
