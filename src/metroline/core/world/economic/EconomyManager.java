package metroline.core.world.economic;

import metroline.core.world.GameWorld;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.enums.Direction;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TrainType;
import metroline.objects.enums.TunnelType;
import metroline.objects.gameobjects.GameConstants;
import metroline.objects.gameobjects.PathPoint;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.util.MetroLogger;

import java.util.HashMap;
import java.util.Map;

import static metroline.objects.gameobjects.GameConstants.*;

/**
 * Централизованный менеджер экономической системы игры
 * Управляет всеми финансовыми операциями, доходами, расходами и содержанием объектов
 */
public class EconomyManager {
    private final GameWorld world;
    public static Map<Station, Float> stationRevenueAccumulator = new HashMap<>();
    private final Map<Station, Long> lastMaintenanceUpdate;
    private final Map<Tunnel, Long> lastTunnelMaintenanceUpdate;

    // Константы экономики
    private static final float BASE_STATION_REVENUE = 10f;
    private static final float BASE_STATION_UPKEEP = 0.001f;
    private static final float BASE_TUNNEL_UPKEEP_PER_SEGMENT = 0.0005f;
    private static final float STATION_BASE_COST = 100f;
    private static final float TUNNEL_COST_PER_SEGMENT = 10f;
    private static final float STATION_REPAIR_BASE_COST = 50f;

    // Интервал обновления содержания (в миллисекундах игрового времени)
    private static final long MAINTENANCE_UPDATE_INTERVAL = 60000; // 1 минута игрового времени

    public EconomyManager(GameWorld world) {
        this.world = world;
        this.stationRevenueAccumulator = new HashMap<>();
        this.lastMaintenanceUpdate = new HashMap<>();
        this.lastTunnelMaintenanceUpdate = new HashMap<>();
    }
    public int getAccumulatorSize() {
        return stationRevenueAccumulator.size();
    }
    public float calculateSimplyStationRevenue(Station station) {
        WorldTile tile = world.getWorldTile(station.getX(), station.getY());
        if (tile == null) {
            return 0f;
        }
        float revenue = BASE_STATION_REVENUE;
        revenue *= getSafeMultiplier(tile.getPassengerCount(), 1.0f); // Пассажиропоток как множитель
        revenue *= getSafeMultiplier(tile.getAbilityPay(), 1.0f);     // Платежеспособность как множитель
        revenue *= getStationTypeMultiplier(station.getType());
        revenue *= (1 - station.getWearLevel()); // Износ снижает доход
        revenue *= getSafeMultiplier(tile.getPerm(), 1.0f); // Множитель местности

        if (tile.isWater() || hasWaterNeighbor(station)) {
            revenue *= 1.8f;
        }
        return revenue;
}
    /**
     * Рассчитывает доход от станции при остановке поезда
     * @param station станция
     * @param trainType тип поезда
     * @return размер дохода
     */
    public float calculateStationRevenue(Station station, TrainType trainType) {
        // Проверка на невалидные станции
        if (isInvalidStation(station)) {
            return 0f;
        }

        WorldTile tile = world.getWorldTile(station.getX(), station.getY());
        if (tile == null) {
            return 0f;
        }

        if(station.hasSameColorNeighbor()) {
            return 0f;
        }
        float revenue = BASE_STATION_REVENUE;

        // Множители дохода (защита от нулевых значений)
        revenue *= trainType.getRevenueMultiplier();
        revenue *= getSafeMultiplier(tile.getPassengerCount(), 1.0f); // Пассажиропоток как множитель
        revenue *= getSafeMultiplier(tile.getAbilityPay(), 1.0f);     // Платежеспособность как множитель
        revenue *= getStationTypeMultiplier(station.getType());
        revenue *= (1 - station.getWearLevel()); // Износ снижает доход
        revenue *= getSafeMultiplier(tile.getPerm(), 1.0f); // Множитель местности

        // Дополнительный множитель для станций на воде
        if (tile.isWater() || hasWaterNeighbor(station)) {
            revenue *= 1.8f; // Снижение дохода на воде
        }

        // Добавляем в аккумулятор станции
        addToRevenueAccumulator(station, revenue);

        return revenue;
    }

    private synchronized void addToRevenueAccumulator(Station station, float revenue) {
        stationRevenueAccumulator.merge(station, revenue, Float::sum);
    }
    // Метод для расчета стоимости ремонта станции
    // Метод для расчета стоимости ремонта станции
    public float calculateRepairCost(Station station) {
        return station.getRepairCost();
    }
    public float calculateAverageStationRevenue(Station station) {
        // Проверка на невалидные станции
        if (isInvalidStation(station)) {
            return 0f;
        }

        WorldTile tile = world.getWorldTile(station.getX(), station.getY());
        if (tile == null) {
            return 0f;
        }

        float revenue = BASE_STATION_REVENUE;

        // Множители дохода (защита от нулевых значений)
        revenue *= 1.5f;
        revenue *= getSafeMultiplier(tile.getPassengerCount(), 1.0f); // Пассажиропоток как множитель
        revenue *= getSafeMultiplier(tile.getAbilityPay(), 1.0f);     // Платежеспособность как множитель
        revenue *= getStationTypeMultiplier(station.getType());
        revenue *= (1 - station.getWearLevel()); // Износ снижает доход
        revenue *= getSafeMultiplier(tile.getPerm(), 1.0f); // Множитель местности

        // Дополнительный множитель для станций на воде
        if (tile.isWater() || hasWaterNeighbor(station)) {
            revenue *= 1.8f; // Снижение дохода на воде
        }

        // Добавляем в аккумулятор станции
        stationRevenueAccumulator.merge(station, revenue, Float::sum);

        return revenue;
    }
    /**
     * Вспомогательный метод для безопасного использования множителей
     * Если значение <= 0, возвращает значение по умолчанию
     */
    private float getSafeMultiplier(float value, float defaultValue) {
        return value > 0 ? value : defaultValue;
    }



    /**
     * Рассчитывает стоимость содержания станции
     * @param station станция
     * @return стоимость содержания в минуту
     */
    public float calculateStationUpkeep(Station station) {
        if (isInvalidStation(station)) {
            return 0f;
        }

        WorldTile tile = world.getWorldTile(station.getX(), station.getY());
        if (tile == null) {
            return BASE_STATION_UPKEEP;
        }

        float upkeep = BASE_STATION_UPKEEP;

        // Базовые множители содержания
        upkeep *= getSafeMultiplier(1 + tile.getPerm(), 1.0f);
        upkeep *= (1 + station.getWearLevel() * 2.5f); // Износ увеличивает содержание

        // Множитель типа станции
        if (station.getType() == StationType.TRANSFER) {
            upkeep *= 1.35f;
        } else if (station.getType() == StationType.TERMINAL) {
            upkeep *= 1.2f;
        }

        // Дополнительные расходы на воде
        if (tile.isWater() || hasWaterNeighbor(station)) {
            upkeep *= 1.25f;
        }

        return upkeep;
    }

    /**
     * Рассчитывает стоимость содержания туннеля
     * @param tunnel туннель
     * @return стоимость содержания в минуту
     */
    public float calculateTunnelUpkeep(Tunnel tunnel) {
        if (tunnel.getType() != TunnelType.ACTIVE) {
            return 0f;
        }

        WorldTile tile = world.getWorldTile(tunnel.getX(), tunnel.getY());
        if (tile == null) {
            return BASE_TUNNEL_UPKEEP_PER_SEGMENT * tunnel.getLength();
        }

        float upkeep = BASE_TUNNEL_UPKEEP_PER_SEGMENT * tunnel.getLength();
        upkeep *= getSafeMultiplier(1 + tile.getPerm() * 0.5f, 1.0f);

        // Учитываем износ станций на концах
        Station start = tunnel.getStart();
        Station end = tunnel.getEnd();
        float avgWear = (start.getWearLevel() + end.getWearLevel()) / 2f;
        upkeep *= (1 + avgWear);

        // Подводные туннели дороже в содержании
        if (isUnderwater(tunnel)) {
            upkeep *= 1.5f;
        }

        return upkeep;
    }

    /**
     * Рассчитывает стоимость строительства станции
     * @param x координата X
     * @param y координата Y
     * @return стоимость строительства
     */
    public float calculateStationConstructionCost(int x, int y) {
        WorldTile tile = world.getWorldTile(x, y);
        if (tile == null) {
            return STATION_BASE_COST;
        }

        float cost = STATION_BASE_COST;
        cost *= getSafeMultiplier(tile.getPerm(), 1.0f);

        // Дополнительная стоимость строительства на воде
        if (tile.isWater() || hasWaterNeighbor(x, y)) {
            cost *= 1.5f;
        }

        return cost;
    }

    /**
     * Проверяет, есть ли вода в соседних клетках (для координат)
     */
    private boolean hasWaterNeighbor(int x, int y) {
        for (Direction dir : Direction.values()) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();

            if (nx >= 0 && nx < world.getWidth() &&
                    ny >= 0 && ny < world.getHeight()) {
                WorldTile neighbor = world.getWorldTile(nx, ny);
                if (neighbor != null && neighbor.isWater()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Рассчитывает стоимость строительства туннеля
     * @param tunnel туннель
     * @return стоимость строительства
     */
    public float calculateTunnelConstructionCost(Tunnel tunnel) {
        WorldTile tile = world.getWorldTile(tunnel.getX(), tunnel.getY());
        float perm = tile != null ? tile.getPerm() : 1f;

        float cost = tunnel.getLength() * TUNNEL_COST_PER_SEGMENT;
        cost *= getSafeMultiplier(perm, 1.0f);

        // Дополнительная стоимость подводных туннелей
        if (isUnderwater(tunnel)) {
            cost *= 2.0f;
        }

        return cost;
    }

    /**
     * Рассчитывает стоимость ремонта станции
     * @param station станция
     * @return стоимость ремонта
     */
    public float calculateStationRepairCost(Station station) {
        float baseCost = STATION_REPAIR_BASE_COST;
        float wearFactor = station.getWearLevel();

        WorldTile tile = world.getWorldTile(station.getX(), station.getY());
        float perm = tile != null ? tile.getPerm() : 1f;

        float cost = baseCost * wearFactor;
        cost *= getSafeMultiplier(1 + perm, 1.0f);

        // Дополнительная стоимость ремонта на воде
        if (tile != null && (tile.isWater() || hasWaterNeighbor(station))) {
            cost *= 1.3f;
        }

        return cost;
    }

    /**
     * Обновляет износ всех станций
     */
    public void updateStationsWear() {
        long currentTime = world.getGameTime().getCurrentTimeMillis();

        for (Station station : world.getStations()) {
            updateStationWear(station, currentTime);
        }
    }

    /**
     * Обновляет износ конкретной станции
     */
    private void updateStationWear(Station station, long currentTime) {
        long age = currentTime - station.getConstructionDate();

        if (station.getType() == StationType.CLOSED) {
            // Закрытые станции изнашиваются быстрее
            float wearLevel = Math.min(1f, (float)age / GameConstants.ABANDONED_THRESHOLD);
            station.setWearLevel(wearLevel);

            if (age >= GameConstants.ABANDONED_THRESHOLD) {
                station.setType(StationType.ABANDONED);
            }
        } else if (!isInvalidStation(station)) {
            // Обычные станции
            float wearLevel = Math.min(1f, (float)age / GameConstants.MAX_LIFETIME);
            station.setWearLevel(wearLevel);

            if (age >= GameConstants.MAX_LIFETIME) {
                station.setType(StationType.RUINED);
            }
        }
    }

    /**
     * Обрабатывает периодические платежи за содержание
     */
    public void processMaintenance() {
        long currentTime = world.getGameTime().getCurrentTimeMillis();
        float totalUpkeep = 0f;

        // Содержание станций
        for (Station station : world.getStations()) {
            Long lastUpdate = lastMaintenanceUpdate.get(station);
            if (lastUpdate == null || currentTime - lastUpdate >= MAINTENANCE_UPDATE_INTERVAL) {
                float upkeep = calculateStationUpkeep(station);
                totalUpkeep += upkeep;
                lastMaintenanceUpdate.put(station, currentTime);
            }
        }

        // Содержание туннелей
        for (Tunnel tunnel : world.getTunnels()) {
            Long lastUpdate = lastTunnelMaintenanceUpdate.get(tunnel);
            if (lastUpdate == null || currentTime - lastUpdate >= MAINTENANCE_UPDATE_INTERVAL) {
                float upkeep = calculateTunnelUpkeep(tunnel);
                totalUpkeep += upkeep;
                lastTunnelMaintenanceUpdate.put(tunnel, currentTime);
            }
        }

        // Списываем общую сумму
        if (totalUpkeep > 0) {
            world.removeMoney(totalUpkeep);
        }
    }

    /**
     * Собирает накопленный доход со всех станций
     * @return общий собранный доход
     */
    public float collectAllRevenue() {
        float totalRevenue = 0f;



        for (Map.Entry<Station, Float> entry : stationRevenueAccumulator.entrySet()) {

            totalRevenue += entry.getValue();
        }

        stationRevenueAccumulator.clear();

        return totalRevenue;
    }

    /**
     * Получает накопленный доход для конкретной станции
     * @param station станция
     * @return накопленный доход
     */
    public float getAccumulatedRevenue(Station station) {
        return stationRevenueAccumulator.getOrDefault(station, 0f);
    }

    /**
     * Проверяет, является ли станция невалидной для получения дохода
     */
    private boolean isInvalidStation(Station station) {
        StationType type = station.getType();
        return type == StationType.DROWNED ||
                type == StationType.ABANDONED ||
                type == StationType.BURNED ||
                type == StationType.RUINED ||
                type == StationType.BUILDING ||
                type == StationType.CLOSED ||
                type == StationType.DESTROYED ||
                type == StationType.PLANNED ||
                type == StationType.DEPO;
    }

    /**
     * Получает множитель дохода для типа станции
     */
    private float getStationTypeMultiplier(StationType type) {
        switch (type) {
            case TRANSFER:
                return 2.5f;
            case TERMINAL:
                return 1.3f;
            case TRANSIT:
                return 1.9f;
            default:
                return 1.0f;
        }
    }

    /**
     * Проверяет, есть ли у станции соседи на воде
     */
    private boolean hasWaterNeighbor(Station station) {
        return hasWaterNeighbor(station.getX(), station.getY());
    }

    /**
     * Проверяет, проходит ли туннель под водой
     */
    private boolean isUnderwater(Tunnel tunnel) {
        // Проверяем точки пути туннеля
        if (tunnel.getPath() != null) {
            for (PathPoint point : tunnel.getPath()) {
                WorldTile tile = world.getWorldTile(point.getX(), point.getY());
                if (tile != null && tile.isWater()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Сброс менеджера при загрузке нового мира
     */
    public void reset() {
        stationRevenueAccumulator.clear();
        lastMaintenanceUpdate.clear();
        lastTunnelMaintenanceUpdate.clear();
    }
}