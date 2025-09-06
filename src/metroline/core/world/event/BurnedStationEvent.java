package metroline.core.world.event;

import metroline.core.world.GameWorld;
import metroline.core.world.event.base.LocalWorldEvent;
import metroline.objects.enums.StationType;
import metroline.objects.gameobjects.Station;
import metroline.util.MetroLogger;

/**
 * Событие пожара на станции
 */
public class BurnedStationEvent extends LocalWorldEvent {

    public BurnedStationEvent(GameWorld world, Station targetStation) {
        super(world,
                targetStation,
                "event.station_burn",
                "event.station_burn_desc",
                180000, // 3 минуты длительность
                0.9f,   // высокая серьезность
                true,   // блокирует станцию
                0.0f,   // доходов нет
                4.0f    // износ увеличивается в 4 раза
        );
    }

    @Override
    public void applyEffects() {
        super.applyEffects();
        MetroLogger.logInfo("🔥 ПОЖАР на станции '" + getTargetStation().getName() + "'! Станция закрыта до ликвидации.");

        // Меняем тип станции на специальный для пожара
        getTargetStation().setType(StationType.BURNED);
    }

    @Override
    public void removeEffects() {
        // После пожара станция становится разрушенной
        getTargetStation().setType(StationType.BURNED);
        MetroLogger.logInfo("Пожар на станции '" + getTargetStation().getName() + "' ликвидирован. Станция требует восстановления.");

        // Не вызываем super.removeEffects() чтобы не восстанавливать исходное состояние
    }
}