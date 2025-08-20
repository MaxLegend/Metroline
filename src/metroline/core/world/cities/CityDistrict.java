package metroline.core.world.cities;

import metroline.core.world.GameWorld;
import metroline.objects.enums.GameplayUnitsType;
import metroline.objects.gameobjects.GameplayUnits;
import metroline.util.MetroLogger;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CityDistrict {
    private final GameWorld world;
    private final int centerX, centerY;
    private final int radius;
    private final Random random;

    private List<GameplayUnits> buildings;
    private int developmentLevel;
    private long lastUpdateTime;
    private String districtName;

    // Константы развития
    private static final int BASE_DEVELOPMENT_TIME = 120000;
    private static final int MAX_DEVELOPMENT_LEVEL = 5;

    // Веса для типов зданий (преобладание жилых домов)
    private static final Map<GameplayUnitsType, Integer> BUILDING_WEIGHTS = new HashMap<>();
    static {
        BUILDING_WEIGHTS.put(GameplayUnitsType.SMALL_HOUSE, 45);
        BUILDING_WEIGHTS.put(GameplayUnitsType.HOUSE, 35);
        BUILDING_WEIGHTS.put(GameplayUnitsType.PERSONAL_HOUSE, 30);
        BUILDING_WEIGHTS.put(GameplayUnitsType.BIG_HOUSE, 20);
        BUILDING_WEIGHTS.put(GameplayUnitsType.BIG_PERSONAL_HOUSE, 15);

        BUILDING_WEIGHTS.put(GameplayUnitsType.SHOP, 25);

        BUILDING_WEIGHTS.put(GameplayUnitsType.FACTORY, 12);
        BUILDING_WEIGHTS.put(GameplayUnitsType.FACTORY2, 10);
        BUILDING_WEIGHTS.put(GameplayUnitsType.FACTORY3, 10);
        BUILDING_WEIGHTS.put(GameplayUnitsType.FACTORY4, 8);
        BUILDING_WEIGHTS.put(GameplayUnitsType.FACTORY5, 8);

        BUILDING_WEIGHTS.put(GameplayUnitsType.CHURCH, 8);
        BUILDING_WEIGHTS.put(GameplayUnitsType.MUSEUM, 6);
        BUILDING_WEIGHTS.put(GameplayUnitsType.HOUSE_CULTURE, 7);
        BUILDING_WEIGHTS.put(GameplayUnitsType.CITYHALL, 5);

        BUILDING_WEIGHTS.put(GameplayUnitsType.AIRPORT, 3);
        BUILDING_WEIGHTS.put(GameplayUnitsType.PORT, 4);
    }

    public CityDistrict(GameWorld world, int centerX, int centerY, int radius, String name) {
        this.world = world;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.districtName = name;
        this.random = new Random();
        this.buildings = new ArrayList<>();
        this.developmentLevel = 1;
        this.lastUpdateTime = world.getGameTime().getCurrentTimeMillis();

        generateInitialBuildings();
    }

    private void generateInitialBuildings() {
        // Создаем плотный центр с ключевыми зданиями
        createDenseCityCenter();

        // Добавляем начальные здания вокруг центра
        int initialBuildings = 5 + random.nextInt(6);
        for (int i = 0; i < initialBuildings; i++) {
            addNewBuilding(true);
        }
    }

    private void createDenseCityCenter() {
        // Создаем 3-5 ключевых зданий в самом центре
        int coreBuildings = 3 + random.nextInt(3);

        for (int i = 0; i < coreBuildings; i++) {
            GameplayUnitsType coreType = getCoreBuildingType();

            // Пытаемся разместить в непосредственной близости от центра
            Point position = findPositionNearCenter(2); // В радиусе 2 клеток от центра

            if (position != null) {
                GameplayUnits building = new GameplayUnits(world, position.x, position.y, coreType);
                buildings.add(building);
                world.addGameplayUnits(building);
            }
        }
    }

    private Point findPositionNearCenter(int maxDistance) {
        // Ищем позицию в непосредственной близости от центра
        for (int distance = 0; distance <= maxDistance; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dy = -distance; dy <= distance; dy++) {
                    if (Math.abs(dx) == distance || Math.abs(dy) == distance) {
                        int testX = centerX + dx;
                        int testY = centerY + dy;

                        if (isValidPosition(testX, testY)) {
                            return new Point(testX, testY);
                        }
                    }
                }
            }
        }
        return null;
    }

    private GameplayUnitsType getCoreBuildingType() {
        // Ядро района - обычно важные здания
        GameplayUnitsType[] coreTypes = {
                GameplayUnitsType.CITYHALL,
                GameplayUnitsType.FACTORY,
                GameplayUnitsType.BIG_HOUSE,
                GameplayUnitsType.SHOP,
                GameplayUnitsType.CHURCH
        };
        return coreTypes[random.nextInt(coreTypes.length)];
    }

    private Point findValidPosition() {
        // Используем нормальное распределение для плотности (больше зданий ближе к центру)
        double angle = random.nextDouble() * 2 * Math.PI;

        // Нормальное распределение расстояния (68% зданий в первой трети радиуса)
        double distance;
        if (random.nextDouble() < 0.68) {
            // 68% зданий в внутренней трети
            distance = random.nextDouble() * (radius / 3.0);
        } else if (random.nextDouble() < 0.95) {
            // 27% зданий в средней трети
            distance = (radius / 3.0) + random.nextDouble() * (radius / 3.0);
        } else {
            // 5% зданий в внешней трети
            distance = (2 * radius / 3.0) + random.nextDouble() * (radius / 3.0);
        }

        // Добавляем небольшой случайный сдвиг для естественности
        distance *= (0.9 + 0.2 * random.nextDouble());

        int x = centerX + (int)(Math.cos(angle) * distance);
        int y = centerY + (int)(Math.sin(angle) * distance);

        return findNearbyValidPosition(x, y, 3); // Увеличиваем радиус поиска
    }

    private Point findNearbyValidPosition(int x, int y, int searchRadius) {
        // Сначала проверяем центральную позицию
        if (isValidPosition(x, y)) {
            return new Point(x, y);
        }

        // Затем ищем по спирали вокруг позиции
        for (int distance = 1; distance <= searchRadius; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dy = -distance; dy <= distance; dy++) {
                    if (Math.abs(dx) == distance || Math.abs(dy) == distance) {
                        int testX = x + dx;
                        int testY = y + dy;

                        if (isValidPosition(testX, testY)) {
                            return new Point(testX, testY);
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isValidPosition(int x, int y) {
        // Проверяем границы мира
        if (x < 0 || x >= world.getWidth() || y < 0 || y >= world.getHeight()) {
            return false;
        }

        // Проверяем, что не вода
        if (world.getWorldTile(x, y).isWater()) {
            return false;
        }

        // Проверяем, что клетка свободна (только от станций и меток)
        if (world.getStationAt(x, y) != null || world.getLabelAt(x, y) != null) {
            return false;
        }

        // УБИРАЕМ проверку расстояния между зданиями - разрешаем строительство вплотную
        // Проверяем только, что в этой клетке нет другого здания
        if (world.getGameplayUnitsAt(x, y) != null) {
            return false;
        }

        return true;
    }

    public void update(long currentTime) {
        if (currentTime - lastUpdateTime > getDevelopmentTime()) {
            develop();
            lastUpdateTime = currentTime;
        }
    }

    private long getDevelopmentTime() {
        return BASE_DEVELOPMENT_TIME * developmentLevel;
    }

    private void develop() {
        // Развитие района
        if (developmentLevel < MAX_DEVELOPMENT_LEVEL && random.nextFloat() < 0.3f) {
            developmentLevel++;
            MetroLogger.logInfo("District '" + districtName + "' reached level " + developmentLevel);
        }

        // Добавление новых зданий с учетом логики роста
        if (random.nextFloat() < getGrowthProbability()) {
            addNewBuilding(false);
        }

        // Удаление старых зданий (очень редко)
        if (random.nextFloat() < 0.05f && buildings.size() > 8) {
            removeOldestBuilding();
        }
    }

    private float getGrowthProbability() {
        // Вероятность роста зависит от уровня развития и плотности застройки
        float densityFactor = 1.0f - ((float)buildings.size() / (radius * radius * 2));
        densityFactor = Math.max(0.3f, densityFactor); // Минимальная вероятность 30%

        return (0.4f + (developmentLevel * 0.1f)) * densityFactor;
    }

    private boolean addNewBuilding(boolean isInitial) {
        Point position = findValidPosition();
        if (position != null) {
            GameplayUnitsType type = getBuildingTypeBasedOnNeighbors(position, isInitial);
            GameplayUnits building = new GameplayUnits(world, position.x, position.y, type);
            buildings.add(building);
            world.addGameplayUnits(building);

            // Логируем плотную застройку
            if (isCloseToCenter(position)) {
                MetroLogger.logInfo("Dense building added at (" + position.x + "," + position.y + ")");
            }

            return true;
        }
        return false;
    }

    private boolean isCloseToCenter(Point position) {
        double distance = Math.sqrt(Math.pow(position.x - centerX, 2) + Math.pow(position.y - centerY, 2));
        return distance < (radius / 3.0);
    }

    private GameplayUnitsType getBuildingTypeBasedOnNeighbors(Point position, boolean isInitial) {
        if (isInitial || buildings.isEmpty()) {
            return getWeightedRandomBuildingType();
        }

        // Анализируем соседние здания для определения типа
        Map<GameplayUnitsType, Integer> neighborCounts = new HashMap<>();
        int totalNeighbors = 0;

        for (GameplayUnits building : buildings) {
            double distance = Math.sqrt(Math.pow(position.x - building.getX(), 2) +
                    Math.pow(position.y - building.getY(), 2));

            if (distance < 6) { // Уменьшаем радиус влияния для более локализованной логики
                neighborCounts.merge(building.getType(), 1, Integer::sum);
                totalNeighbors++;
            }
        }

        if (totalNeighbors == 0) {
            return getWeightedRandomBuildingType();
        }

        // Логика роста в зависимости от соседей
        if (neighborCounts.containsKey(GameplayUnitsType.FACTORY) ||
                neighborCounts.containsKey(GameplayUnitsType.FACTORY2) ||
                neighborCounts.containsKey(GameplayUnitsType.FACTORY3) ||
                neighborCounts.containsKey(GameplayUnitsType.FACTORY4) ||
                neighborCounts.containsKey(GameplayUnitsType.FACTORY5)) {

            // Рядом с промышленностью строим жилье
            return getWeightedTypeWithBias(Arrays.asList(
                    GameplayUnitsType.SMALL_HOUSE,
                    GameplayUnitsType.HOUSE,
                    GameplayUnitsType.PERSONAL_HOUSE
            ), 2.5f);
        }

        if (neighborCounts.containsKey(GameplayUnitsType.HOUSE) ||
                neighborCounts.containsKey(GameplayUnitsType.SMALL_HOUSE) ||
                neighborCounts.containsKey(GameplayUnitsType.BIG_HOUSE) ||
                neighborCounts.containsKey(GameplayUnitsType.PERSONAL_HOUSE) ||
                neighborCounts.containsKey(GameplayUnitsType.BIG_PERSONAL_HOUSE)) {

            // Рядом с жильем строим коммерцию и сервисы
            return getWeightedTypeWithBias(Arrays.asList(
                    GameplayUnitsType.SHOP,
                    GameplayUnitsType.CHURCH,
                    GameplayUnitsType.HOUSE_CULTURE
            ), 2.0f);
        }

        if (neighborCounts.containsKey(GameplayUnitsType.SHOP) ||
                neighborCounts.containsKey(GameplayUnitsType.CHURCH) ||
                neighborCounts.containsKey(GameplayUnitsType.HOUSE_CULTURE)) {

            // Рядом с коммерцией строим больше коммерции и жилья
            return getWeightedTypeWithBias(Arrays.asList(
                    GameplayUnitsType.SHOP,
                    GameplayUnitsType.HOUSE,
                    GameplayUnitsType.BIG_HOUSE
            ), 1.8f);
        }

        // По умолчанию - взвешенный случайный выбор
        return getWeightedRandomBuildingType();
    }

    private GameplayUnitsType getWeightedRandomBuildingType() {
        int totalWeight = BUILDING_WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
        int randomWeight = random.nextInt(totalWeight);

        int currentWeight = 0;
        for (Map.Entry<GameplayUnitsType, Integer> entry : BUILDING_WEIGHTS.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }

        return GameplayUnitsType.HOUSE;
    }

    private GameplayUnitsType getWeightedTypeWithBias(List<GameplayUnitsType> preferredTypes, float biasMultiplier) {
        Map<GameplayUnitsType, Integer> tempWeights = new HashMap<>(BUILDING_WEIGHTS);

        for (GameplayUnitsType type : preferredTypes) {
            tempWeights.put(type, (int)(tempWeights.getOrDefault(type, 10) * biasMultiplier));
        }

        int totalWeight = tempWeights.values().stream().mapToInt(Integer::intValue).sum();
        int randomWeight = random.nextInt(totalWeight);

        int currentWeight = 0;
        for (Map.Entry<GameplayUnitsType, Integer> entry : tempWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }

        return GameplayUnitsType.HOUSE;
    }

    private void removeOldestBuilding() {
        // Удаляем самое старое здание на окраине, а не в центре
        GameplayUnits oldestOnOutskirts = null;
        long oldestTime = Long.MAX_VALUE;

        for (GameplayUnits building : buildings) {
            double distance = Math.sqrt(Math.pow(building.getX() - centerX, 2) +
                    Math.pow(building.getY() - centerY, 2));

            // Предпочитаем удалять здания на окраинах
            if (distance > (radius / 2.0) && building.getCreationTime() < oldestTime) {
                oldestOnOutskirts = building;
                oldestTime = building.getCreationTime();
            }
        }

        if (oldestOnOutskirts != null) {
            removeBuilding(oldestOnOutskirts);
        } else if (!buildings.isEmpty()) {
            // Если все здания в центре, удаляем самое старое
            GameplayUnits oldest = Collections.min(buildings,
                    Comparator.comparingLong(GameplayUnits::getCreationTime));
            removeBuilding(oldest);
        }
    }

    public void removeBuilding(GameplayUnits building) {
        if (buildings.remove(building)) {
            world.removeGameplayUnits(building);
        }
    }

    // Getters
    public String getName() { return districtName; }
    public int getDevelopmentLevel() { return developmentLevel; }
    public int getBuildingCount() { return buildings.size(); }
    public List<GameplayUnits> getBuildings() { return new ArrayList<>(buildings); }
    public int getCenterX() { return centerX; }
    public int getCenterY() { return centerY; }
    public int getRadius() { return radius; }
}