package metroline.core.world.event;

import metroline.core.world.GameWorld;
import metroline.core.world.event.base.GlobalWorldEvent;
import metroline.core.world.event.base.LocalWorldEvent;
import metroline.core.world.event.base.WorldEvent;
import metroline.objects.enums.StationType;
import metroline.objects.gameobjects.Station;
import metroline.util.MetroLogger;
import metroline.util.localizate.LngUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Центральный обработчик генерации, управления и обработки игровых событий.
 * TODO добавить сохранение текущего времени события в файл сохранения.
 */
public class WorldEventManager {
    private final GameWorld world;
    private final List<WorldEvent> activeWorldEvents;
    private final List<WorldEvent> WorldEventHistory;
    private final Random random;

    // Вероятности и интервалы генерации событий (можно вынести в конфиг)
    private final long globalWorldEventIntervalMin = 300000; // 5 мин игрового времени
    private final long globalWorldEventIntervalMax = 1200000; // 20 мин
    private long nextGlobalWorldEventTime;

    private final long localWorldEventIntervalMin = 60000; // 1 мин
    private final long localWorldEventIntervalMax = 300000; // 5 мин
    private long nextLocalWorldEventTime;

    private final float globalWorldEventChance = 0.02f; // 2% шанс при проверке
    private final float localWorldEventChance = 0.05f;  // 5% шанс при проверке

    // Списки возможных событий
    private final List<GlobalWorldEventTemplate> globalWorldEventTemplates;
    private final List<LocalWorldEventTemplate> localWorldEventTemplates;

    public WorldEventManager(GameWorld world) {
        this.world = world;
        this.activeWorldEvents = new ArrayList<>();
        this.WorldEventHistory = new ArrayList<>();
        this.random = new Random();
        this.nextGlobalWorldEventTime = calculateNextGlobalWorldEventTime();
        this.nextLocalWorldEventTime = calculateNextLocalWorldEventTime();

        // Инициализация шаблонов событий
        this.globalWorldEventTemplates = new ArrayList<>();
        this.localWorldEventTemplates = new ArrayList<>();
        initializeWorldEventTemplates();
    }

    private void initializeWorldEventTemplates() {
        // Заполняем шаблоны глобальных событий
//        globalWorldEventTemplates.add(new GlobalWorldEventTemplate(
//                "Экономический кризис",
//                "Падение экономики снижает платежеспособность населения.",
//                480000, // 8 минут
//                0.7f,
//                1.0f, // Пассажиропоток не меняется
//                0.5f, // Платежеспособность падает в 2 раза
//                1.1f  // Содержание немного дороже
//        ));
//        globalWorldEventTemplates.add(new GlobalWorldEventTemplate(
//                "Эпидемия",
//                "Вспышка болезни резко сокращает пассажиропоток.",
//                360000, // 6 минут
//                0.6f,
//                0.3f, // Пассажиропоток сильно падает
//                1.0f, // Платежеспособность не меняется
//                1.0f
//        ));
//        globalWorldEventTemplates.add(new GlobalWorldEventTemplate(
//                "Технический бум",
//                "Новые технологии снижают затраты на содержание.",
//                240000, // 4 минуты
//                0.3f,
//                1.0f,
//                1.0f,
//                0.8f // Содержание дешевле
//        ));
//        localWorldEventTemplates.add(new LocalWorldEventTemplate(
//                "Акция протеста",
//                "Протестующие блокируют вход на станцию. Доходы снижены.",
//                120000, // 2 минуты
//                0.5f,
//                false,  // Не блокирует полностью
//                0.2f,   // Доходы мизерные
//                1.5f    // Износ повышенный
//        ));
        // Заполняем шаблоны локальных событий
        localWorldEventTemplates.add(new LocalWorldEventTemplate(
                "event.station_burn",
                "event.station_burn_desc",
                180000, // 3 минуты
                0.8f,
                true,   // Блокирует станцию
                0.0f,   // Доходов нет
                3.0f    // Износ растет в 3 раза быстрее
        ));
        localWorldEventTemplates.add(new LocalWorldEventTemplate(
                "event.station_flood",
                "event.station_flood_desc",
                240000, // 4 минуты
                0.9f,
                true,
                0.0f,
                5.0f
        ));
        localWorldEventTemplates.add(new LocalWorldEventTemplate(
                "event.station_collapse",
                "event.station_collapse_desc",
                240000, // 4 минуты
                0.9f,
                true,
                0.0f,
                5.0f
        ));
    }

    public void update() {
        long currentTime = world.getGameTime().getCurrentTimeMillis();

        // 1. Проверяем и завершаем активные события
        Iterator<WorldEvent> iterator = activeWorldEvents.iterator();
        while (iterator.hasNext()) {
            WorldEvent WorldEvent = iterator.next();
            if (WorldEvent.checkAndDeactivate()) {
                iterator.remove();
                WorldEventHistory.add(WorldEvent);
            }
        }

        // 2. Проверяем, не настало ли время для проверки нового глобального события
        if (currentTime >= nextGlobalWorldEventTime && activeWorldEvents.stream().noneMatch(e -> e instanceof GlobalWorldEvent)) {
            nextGlobalWorldEventTime = calculateNextGlobalWorldEventTime();
            tryTriggerGlobalWorldEvent();
        }

        // 3. Проверяем, не настало ли время для проверки нового локального события
        if (currentTime >= nextLocalWorldEventTime) {
            nextLocalWorldEventTime = calculateNextLocalWorldEventTime();
            tryTriggerLocalWorldEvent();
        }

        // 4. Применяем эффекты ВСЕХ активных событий
        for (WorldEvent WorldEvent : activeWorldEvents) {
            WorldEvent.applyEffects(); // applyEffects может быть и одноразовым, тогда логику нужно перенести в расчеты доходов/износа
        }
    }

    private void tryTriggerGlobalWorldEvent() {
        if (random.nextFloat() < globalWorldEventChance) {
            triggerGlobalWorldEvent();
        }
    }

    private void tryTriggerLocalWorldEvent() {
        if (random.nextFloat() < localWorldEventChance) {
            // Выбираем случайную станцию, которая не разрушена, не строится и не планируется
            List<Station> eligibleStations = world.getStations().stream()
                                                  .filter(s ->
                                                           s.getType() != StationType.PLANNED
                                                          && s.getType() != StationType.RUINED
                                                          && s.getType() != StationType.BURNED    // исключаем уже горящие
                                                          && s.getType() != StationType.DROWNED)  // исключаем уже затопленные
                                                  .collect(Collectors.toList());

            if (!eligibleStations.isEmpty()) {
                Station target = eligibleStations.get(random.nextInt(eligibleStations.size()));
                triggerLocalWorldEvent(target);
            }
        }
    }

    public void triggerGlobalWorldEvent() {
        if (globalWorldEventTemplates.isEmpty()) return;

        GlobalWorldEventTemplate template = globalWorldEventTemplates.get(random.nextInt(globalWorldEventTemplates.size()));
        GlobalWorldEvent newWorldEvent = new GlobalWorldEvent(
                world,
                template.name,
                template.description,
                (long) (template.duration * (0.8f + 0.4f * random.nextFloat())), // +/- 20% длительность
                template.baseSeverity * (0.7f + 0.6f * random.nextFloat()),      // +/- 30% серьезность
                template.passengerFlowMultiplier,
                template.paymentAbilityMultiplier,
                template.upkeepCostMultiplier
        );

        activeWorldEvents.add(newWorldEvent);
        newWorldEvent.applyEffects(); // Применяем сразу
        MetroLogger.logInfo("!!! Запущено глобальное событие: " + newWorldEvent.getName());
        // TODO: Добавить визуальное оповещение игроку (всплывающее окно, звук)
    }

    public void triggerLocalWorldEvent(Station station) {
        if (localWorldEventTemplates.isEmpty()) return;

        // Не создаем событие для станции, уже затронутой другим локальным событием
        boolean alreadyAffected = activeWorldEvents.stream()
                                                   .filter(e -> e instanceof LocalWorldEvent)
                                                   .map(e -> (LocalWorldEvent) e)
                                                   .anyMatch(e -> e.getAffectedStations().contains(station));

        if (alreadyAffected) {
            return;
        }

        LocalWorldEventTemplate template = localWorldEventTemplates.get(random.nextInt(localWorldEventTemplates.size()));
        LocalWorldEvent newWorldEvent;

        // Создаем конкретный тип события на основе шаблона
        if (template.name.equals("event.station_burn")) {
            newWorldEvent = new BurnedStationEvent(world, station);
        } else if (template.name.equals("event.station_flood")) {
            newWorldEvent = new FloodedStationEvent(world, station);
        }else if (template.name.equals("event.station_collapse")) {
            newWorldEvent = new CollapseStationEvent(world, station);
        }
        else {
            // Для других событий используем общий конструктор
            newWorldEvent = new LocalWorldEvent(
                    world,
                    station,
                    template.name,
                    template.description,
                    (long) (template.duration * (0.8f + 0.4f * random.nextFloat())),
                    template.baseSeverity * (0.7f + 0.6f * random.nextFloat()),
                    template.blocksStation,
                    template.revenueMultiplier,
                    template.wearRateMultiplier
            );
        }

        activeWorldEvents.add(newWorldEvent);
        newWorldEvent.applyEffects();
        MetroLogger.logInfo("!!! Запущено локальное событие на станции " + station.getName() + ": " + newWorldEvent.getName());
    }

    public List<WorldEvent> getActiveWorldEvents() {
        return Collections.unmodifiableList(activeWorldEvents);
    }

    public List<WorldEvent> getWorldEventHistory() {
        return Collections.unmodifiableList(WorldEventHistory);
    }

    public void clearAllWorldEvents() {
        for (WorldEvent WorldEvent : activeWorldEvents) {
            WorldEvent.removeEffects();
        }
        activeWorldEvents.clear();
    }

    private long calculateNextGlobalWorldEventTime() {
        long currentTime = world.getGameTime().getCurrentTimeMillis();
        return currentTime + globalWorldEventIntervalMin + (long) (random.nextFloat() * (globalWorldEventIntervalMax - globalWorldEventIntervalMin));
    }

    private long calculateNextLocalWorldEventTime() {
        long currentTime = world.getGameTime().getCurrentTimeMillis();
        return currentTime + localWorldEventIntervalMin + (long) (random.nextFloat() * (localWorldEventIntervalMax - localWorldEventIntervalMin));
    }

    // Вспомогательные классы-шаблоны
    private static class GlobalWorldEventTemplate {
        final String name;
        final String description;
        final long duration;
        final float baseSeverity;
        final float passengerFlowMultiplier;
        final float paymentAbilityMultiplier;
        final float upkeepCostMultiplier;

        GlobalWorldEventTemplate(String name, String description, long duration, float baseSeverity,
                float passengerFlowMultiplier, float paymentAbilityMultiplier, float upkeepCostMultiplier) {
            this.name = LngUtil.translatable(name);
            this.description = LngUtil.translatable(description);
            this.duration = duration;
            this.baseSeverity = baseSeverity;
            this.passengerFlowMultiplier = passengerFlowMultiplier;
            this.paymentAbilityMultiplier = paymentAbilityMultiplier;
            this.upkeepCostMultiplier = upkeepCostMultiplier;
        }
    }

    private static class LocalWorldEventTemplate {
        final String name;
        final String description;
        final long duration;
        final float baseSeverity;
        final boolean blocksStation;
        final float revenueMultiplier;
        final float wearRateMultiplier;

        LocalWorldEventTemplate(String name, String description, long duration, float baseSeverity,
                boolean blocksStation, float revenueMultiplier, float wearRateMultiplier) {
            this.name = LngUtil.translatable(name);
            this.description = LngUtil.translatable(description);
            this.duration = duration;
            this.baseSeverity = baseSeverity;
            this.blocksStation = blocksStation;
            this.revenueMultiplier = revenueMultiplier;
            this.wearRateMultiplier = wearRateMultiplier;
        }
    }
}
