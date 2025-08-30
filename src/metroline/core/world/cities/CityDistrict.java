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
    private final String districtName;

    private List<GameplayUnits> buildings;
    private int developmentLevel;
    private long lastUpdateTime;

    /*******************************
     * CONSTANTS SECTION
     *******************************/
    private static final int BASE_DEVELOPMENT_TIME = 300000;
    private static final int MAX_DEVELOPMENT_LEVEL = 8;
    private static final int MIN_BUILDING_DISTANCE = 1;

    private static final Map<GameplayUnitsType, Integer> BUILDING_WEIGHTS = new HashMap<>();
    public static final Map<GameplayUnitsType, Float> EXPANSION_CHANCES = new HashMap<>();

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

        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY, 0.08f);
        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY2, 0.06f);
        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY3, 0.05f);
        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY4, 0.04f);
        EXPANSION_CHANCES.put(GameplayUnitsType.FACTORY5, 0.03f);
        EXPANSION_CHANCES.put(GameplayUnitsType.SHOP, 0.05f);
        EXPANSION_CHANCES.put(GameplayUnitsType.HOUSE, 0.02f);
        EXPANSION_CHANCES.put(GameplayUnitsType.BIG_HOUSE, 0.03f);
        EXPANSION_CHANCES.put(GameplayUnitsType.SMALL_HOUSE, 0.01f);
        EXPANSION_CHANCES.put(GameplayUnitsType.CITYHALL, 0.10f);
        EXPANSION_CHANCES.put(GameplayUnitsType.CHURCH, 0.04f);
        EXPANSION_CHANCES.put(GameplayUnitsType.HOUSE_CULTURE, 0.03f);
        EXPANSION_CHANCES.put(GameplayUnitsType.MUSEUM, 0.02f);
    }

    /*******************************
     * CONSTRUCTOR SECTION
     *******************************/
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

    /*******************************
     * INITIALIZATION SECTION
     *******************************/
    private void generateInitialBuildings() {
        createDenseCityCenter();
        int initialBuildings = 3 + random.nextInt(4);
        for (int i = 0; i < initialBuildings; i++) {
            addNewBuilding(true);
        }
    }

    private void createDenseCityCenter() {
        int coreBuildings = 2 + random.nextInt(3);
        for (int i = 0; i < coreBuildings; i++) {
            GameplayUnitsType coreType = getCoreBuildingType();
            Point position = findPositionNearCenter(2);
            if (position != null) {
                GameplayUnits building = new GameplayUnits(world, position.x, position.y, coreType);
                buildings.add(building);
                world.addGameplayUnits(building);
            }
        }
    }

    /*******************************
     * UPDATE SECTION
     *******************************/
    public void update(long currentTime) {

            if (currentTime - lastUpdateTime > getDevelopmentTime()) {
                develop();
                lastUpdateTime = currentTime;
            }

            if (random.nextFloat() < 0.7f) {
                tryAutoExpand(currentTime);
                      tryBuildingExpansion();
            }

    }

    private void develop() {
        if (random.nextFloat() < 0.06f && buildings.size() > 5) {
            removeOldestBuilding();
        }

        if (developmentLevel < MAX_DEVELOPMENT_LEVEL && random.nextFloat() < 0.08f) {
            developmentLevel++;
        }

        if (random.nextFloat() < getGrowthProbability() * 0.7f) {
            addNewBuilding(false);
        }
    }

    /*******************************
     * EXPANSION SECTION
     *******************************/
    private void tryAutoExpand(long currentTime) {
        if (!buildings.isEmpty() && random.nextFloat() < 0.3f) {
            GameplayUnits building = buildings.get(random.nextInt(buildings.size()));
            if (shouldBuildingExpand(building, currentTime)) {
                expandFromBuilding(building);
            }
        }
    }

    private boolean shouldBuildingExpand(GameplayUnits building, long currentTime) {
        Float expansionChance = EXPANSION_CHANCES.get(building.getType());
        if (expansionChance == null) return false;

        // Увеличиваем время для максимального шанса (30 минут вместо 10)
        long age = currentTime - building.getCreationTime();
        float ageFactor = Math.min(1.0f, age / 1800000f);

        // Уменьшаем влияние уровня развития
        float developmentFactor = 0.3f + (developmentLevel * 0.07f);

        // Уменьшаем базовый шанс
        float finalChance = expansionChance * 0.5f * ageFactor * developmentFactor;

        return random.nextFloat() < finalChance;
    }


    private void expandFromBuilding(GameplayUnits building) {
        int expansionRadius = 3 + random.nextInt(3);
        Point newPosition = findExpansionPosition(building.getX(), building.getY(), expansionRadius);
        if (newPosition != null) {
            GameplayUnitsType newType = determineExpansionBuildingType(building.getType());
            GameplayUnits newBuilding = new GameplayUnits(world, newPosition.x, newPosition.y, newType);
            buildings.add(newBuilding);
            world.addGameplayUnits(newBuilding);
        }
    }

    private void tryBuildingExpansion() {
        // Создаем копию коллекции для итерации
        List<GameplayUnits> allBuildings = new ArrayList<>(world.getGameplayUnits());

        for (GameplayUnits building : allBuildings) {
            if (shouldTryExpansion(building)) {
                building.tryExpand();
            }
        }
    }

    private boolean shouldTryExpansion(GameplayUnits building) {
        if (building.isAbandoned()) return false;
        if (building.getCondition() < 50) return false;
        return Math.random() < 0.3f;
    }

    /*******************************
     * POSITION FINDING SECTION
     *******************************/
    private Point findExpansionPosition(int sourceX, int sourceY, int maxDistance) {
        for (int distance = 1; distance <= maxDistance; distance++) {
            List<Point> possiblePositions = new ArrayList<>();
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dy = -distance; dy <= distance; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != distance) continue;
                    if (Math.abs(dx) == Math.abs(dy) && distance > 1) continue;

                    int x = sourceX + dx;
                    int y = sourceY + dy;
                    if (isValidPosition(x, y) && hasEnoughSpace(x, y, 1)) {
                        int priority = (distance == 1) ? 5 : 1;
                        for (int i = 0; i < priority; i++) {
                            possiblePositions.add(new Point(x, y));
                        }
                    }
                }
            }
            if (!possiblePositions.isEmpty()) {
                return possiblePositions.get(random.nextInt(possiblePositions.size()));
            }
        }
        return null;
    }

    private Point findValidPosition() {
        int strategy = random.nextInt(10);
        Point position;

        if (strategy < 4) {
            position = findNormalDistributionPosition();
        } else if (strategy < 7) {
            position = findUniformDistributionPosition();
        } else if (strategy < 9) {
            position = findClusterPosition();
        } else {
            position = findRandomPosition();
        }

        return position != null ? findNearbyValidPosition(position.x, position.y, 5) : null;
    }

    private Point findNormalDistributionPosition() {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance;

        if (random.nextDouble() < 0.68) {
            distance = random.nextDouble() * (radius / 3.0);
        } else if (random.nextDouble() < 0.95) {
            distance = (radius / 3.0) + random.nextDouble() * (radius / 3.0);
        } else {
            distance = (2 * radius / 3.0) + random.nextDouble() * (radius / 3.0);
        }

        distance *= (0.8 + 0.4 * random.nextDouble());
        return new Point(centerX + (int)(Math.cos(angle) * distance), centerY + (int)(Math.sin(angle) * distance));
    }

    private Point findUniformDistributionPosition() {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * radius;
        return new Point(centerX + (int)(Math.cos(angle) * distance), centerY + (int)(Math.sin(angle) * distance));
    }

    private Point findClusterPosition() {
        if (!buildings.isEmpty() && random.nextBoolean()) {
            GameplayUnits randomBuilding = buildings.get(random.nextInt(buildings.size()));
            int offsetX = (random.nextInt(7) + 2) * (random.nextBoolean() ? 1 : -1);
            int offsetY = (random.nextInt(7) + 2) * (random.nextBoolean() ? 1 : -1);
            return new Point(randomBuilding.getX() + offsetX, randomBuilding.getY() + offsetY);
        }
        return findUniformDistributionPosition();
    }

    private Point findRandomPosition() {
        int size = radius * 2;
        return new Point(centerX - radius + random.nextInt(size), centerY - radius + random.nextInt(size));
    }

    private Point findPositionNearCenter(int maxDistance) {
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

    private Point findNearbyValidPosition(int x, int y, int searchRadius) {
        if (isValidPosition(x, y)) {
            return new Point(x, y);
        }

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

    /*******************************
     * VALIDATION SECTION
     *******************************/
    private boolean isValidPosition(int x, int y) {
        if (x < 0 || x >= world.getWidth() || y < 0 || y >= world.getHeight()) return false;
        if (world.getWorldTile(x, y).isWater()) return false;
        if (world.getStationAt(x, y) != null || world.getLabelAt(x, y) != null || world.getGameplayUnitsAt(x, y) != null) return false;
        return true;
    }

    private boolean hasEnoughSpace(int x, int y) {
        return hasEnoughSpace(x, y, 1);
    }

    private boolean hasEnoughSpace(int x, int y, int minDistance) {
        for (GameplayUnits existing : buildings) {
            int manhattanDistance = Math.abs(x - existing.getX()) + Math.abs(y - existing.getY());
            if (manhattanDistance < minDistance) return false;

            double euclideanDistance = Math.sqrt(Math.pow(x - existing.getX(), 2) + Math.pow(y - existing.getY(), 2));
            if (euclideanDistance < minDistance - 0.3) return false;
        }
        return true;
    }

    /*******************************
     * BUILDING MANAGEMENT SECTION
     *******************************/
    private boolean addNewBuilding(boolean isInitial) {
        Point position = findValidPosition();
        if (position != null && hasEnoughSpace(position.x, position.y)) {
            GameplayUnitsType type = getBuildingTypeBasedOnNeighbors(position, isInitial);
            GameplayUnits building = new GameplayUnits(world, position.x, position.y, type);
            buildings.add(building);
            world.addGameplayUnits(building);
            return true;
        }
        return false;
    }

    private void removeOldestBuilding() {
        List<GameplayUnits> outskirtsBuildings = new ArrayList<>();
        List<GameplayUnits> centralBuildings = new ArrayList<>();

        // Создаем копию для безопасной работы
        List<GameplayUnits> buildingsCopy = new ArrayList<>(buildings);

        for (GameplayUnits building : buildingsCopy) {
            double distance = Math.sqrt(Math.pow(building.getX() - centerX, 2) + Math.pow(building.getY() - centerY, 2));
            if (distance > (radius * 0.6)) {
                outskirtsBuildings.add(building);
            } else {
                centralBuildings.add(building);
            }
        }

        GameplayUnits buildingToRemove = null;

        if (!outskirtsBuildings.isEmpty() && random.nextFloat() < 0.7f) {
            buildingToRemove = Collections.min(outskirtsBuildings, Comparator.comparingLong(GameplayUnits::getCreationTime));
        } else if (!buildingsCopy.isEmpty() && buildingsCopy.size() > 3) {
            // Сортируем копию, а не оригинальную коллекцию
            buildingsCopy.sort(Comparator.comparingLong(GameplayUnits::getCreationTime));
            int index = Math.min(2, buildingsCopy.size() - 1);
            buildingToRemove = buildingsCopy.get(index);
        }

        if (buildingToRemove != null) {
            removeBuilding(buildingToRemove);
        }
    }

    public void removeBuilding(GameplayUnits building) {
        // Используем итератор для безопасного удаления
        Iterator<GameplayUnits> iterator = buildings.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(building)) {
                iterator.remove();
                world.removeGameplayUnits(building);
                break;
            }
        }
    }
    /*******************************
     * BUILDING TYPE LOGIC SECTION
     *******************************/
    private GameplayUnitsType getCoreBuildingType() {
        GameplayUnitsType[] coreTypes = {
                GameplayUnitsType.CITYHALL, GameplayUnitsType.FACTORY,
                GameplayUnitsType.BIG_HOUSE, GameplayUnitsType.SHOP, GameplayUnitsType.CHURCH
        };
        return coreTypes[random.nextInt(coreTypes.length)];
    }

    private GameplayUnitsType determineExpansionBuildingType(GameplayUnitsType sourceType) {
        switch (sourceType) {
            case FACTORY: case FACTORY2: case FACTORY3: case FACTORY4: case FACTORY5:
                return random.nextBoolean() ? GameplayUnitsType.HOUSE : GameplayUnitsType.SHOP;
            case HOUSE: case BIG_HOUSE: case SMALL_HOUSE:
                return random.nextFloat() < 0.7f ? GameplayUnitsType.SHOP :
                        random.nextBoolean() ? GameplayUnitsType.CHURCH : GameplayUnitsType.HOUSE_CULTURE;
            case SHOP:
                return random.nextBoolean() ? GameplayUnitsType.SHOP : GameplayUnitsType.HOUSE;
            default:
                return getWeightedRandomBuildingType();
        }
    }

    private GameplayUnitsType getBuildingTypeBasedOnNeighbors(Point position, boolean isInitial) {
        if (isInitial || buildings.isEmpty()) return getWeightedRandomBuildingType();

        Map<GameplayUnitsType, Integer> neighborCounts = new HashMap<>();
        int totalNeighbors = 0;

        for (GameplayUnits building : buildings) {
            double distance = Math.sqrt(Math.pow(position.x - building.getX(), 2) + Math.pow(position.y - building.getY(), 2));
            if (distance < 6) {
                neighborCounts.merge(building.getType(), 1, Integer::sum);
                totalNeighbors++;
            }
        }

        if (totalNeighbors == 0) return getWeightedRandomBuildingType();

        if (hasIndustrialNeighbors(neighborCounts)) {
            return getWeightedTypeWithBias(Arrays.asList(GameplayUnitsType.SMALL_HOUSE, GameplayUnitsType.HOUSE, GameplayUnitsType.PERSONAL_HOUSE), 2.5f);
        }

        if (hasResidentialNeighbors(neighborCounts)) {
            return getWeightedTypeWithBias(Arrays.asList(GameplayUnitsType.SHOP, GameplayUnitsType.CHURCH, GameplayUnitsType.HOUSE_CULTURE), 2.0f);
        }

        if (hasCommercialNeighbors(neighborCounts)) {
            return getWeightedTypeWithBias(Arrays.asList(GameplayUnitsType.SHOP, GameplayUnitsType.HOUSE, GameplayUnitsType.BIG_HOUSE), 1.8f);
        }

        return getWeightedRandomBuildingType();
    }

    private boolean hasIndustrialNeighbors(Map<GameplayUnitsType, Integer> neighborCounts) {
        return neighborCounts.containsKey(GameplayUnitsType.FACTORY) || neighborCounts.containsKey(GameplayUnitsType.FACTORY2) ||
                neighborCounts.containsKey(GameplayUnitsType.FACTORY3) || neighborCounts.containsKey(GameplayUnitsType.FACTORY4) ||
                neighborCounts.containsKey(GameplayUnitsType.FACTORY5);
    }

    private boolean hasResidentialNeighbors(Map<GameplayUnitsType, Integer> neighborCounts) {
        return neighborCounts.containsKey(GameplayUnitsType.HOUSE) || neighborCounts.containsKey(GameplayUnitsType.SMALL_HOUSE) ||
                neighborCounts.containsKey(GameplayUnitsType.BIG_HOUSE) || neighborCounts.containsKey(GameplayUnitsType.PERSONAL_HOUSE) ||
                neighborCounts.containsKey(GameplayUnitsType.BIG_PERSONAL_HOUSE);
    }

    private boolean hasCommercialNeighbors(Map<GameplayUnitsType, Integer> neighborCounts) {
        return neighborCounts.containsKey(GameplayUnitsType.SHOP) || neighborCounts.containsKey(GameplayUnitsType.CHURCH) ||
                neighborCounts.containsKey(GameplayUnitsType.HOUSE_CULTURE);
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

    /*******************************
     * UTILITY SECTION
     *******************************/
    private long getDevelopmentTime() {
        return BASE_DEVELOPMENT_TIME * developmentLevel;
    }

    private float getGrowthProbability() {
        float densityFactor = 1.0f - ((float)buildings.size() / (radius * radius));
        densityFactor = Math.max(0.3f, densityFactor);
        return (0.15f + (developmentLevel * 0.04f)) * densityFactor;
    }

    /*******************************
     * GETTERS SECTION
     *******************************/
    public String getName() { return districtName; }
    public int getDevelopmentLevel() { return developmentLevel; }
    public int getBuildingCount() { return buildings.size(); }
    public List<GameplayUnits> getBuildings() { return new ArrayList<>(buildings); }
    public int getCenterX() { return centerX; }
    public int getCenterY() { return centerY; }
    public int getRadius() { return radius; }
}