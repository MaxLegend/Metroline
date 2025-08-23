package metroline.core.world.cities;

import metroline.core.world.GameWorld;
import metroline.objects.gameobjects.GameplayUnits;
import metroline.util.MetroLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CityManager {
    private final GameWorld world;
    private final List<CityDistrict> districts;
    private final Random random;
    private long lastUpdateTime;

    private static final int MIN_DISTRICTS = 2;
    private static final int MAX_DISTRICTS = 6;
    private static final int DISTRICT_UPDATE_INTERVAL = 6000000;

    public CityManager(GameWorld world) {
        this.world = world;
        this.districts = new ArrayList<>();
        this.random = new Random();
        this.lastUpdateTime = world.getGameTime().getCurrentTimeMillis();

        generateDistricts();
    }

    private void generateDistricts() {
        int districtCount = MIN_DISTRICTS + random.nextInt(MAX_DISTRICTS - MIN_DISTRICTS + 1);
        int attempts = 0;
        int created = 0;

        // Адаптивные размеры относительно мира
        int minDistanceFromEdge = Math.min(world.getWidth(), world.getHeight()) / 10;
        minDistanceFromEdge = Math.max(5, minDistanceFromEdge); // Не менее 5 клеток

        while (created < districtCount && attempts < 100) {
            if (createDistrict(created, minDistanceFromEdge)) {
                created++;
            }
            attempts++;
        }

        MetroLogger.logInfo("Generated " + districts.size() + " city districts with " + getTotalBuildings() + " buildings");
    }

    private boolean createDistrict(int index, int minDistanceFromEdge) {
        // Адаптивные координаты относительно размеров мира
        int centerX = minDistanceFromEdge + random.nextInt(world.getWidth() - 2 * minDistanceFromEdge);
        int centerY = minDistanceFromEdge + random.nextInt(world.getHeight() - 2 * minDistanceFromEdge);

        // Адаптивный радиус относительно размеров мира
        int maxRadius = Math.min(world.getWidth(), world.getHeight()) / 8;
        maxRadius = Math.max(8, Math.min(25, maxRadius)); // Ограничиваем разумными пределами
        int radius = 5 + random.nextInt(maxRadius - 5);

        if (isValidDistrictLocation(centerX, centerY, radius)) {
            String name = generateDistrictName(index);
            CityDistrict district = new CityDistrict(world, centerX, centerY, radius, name);
            districts.add(district);
            return true;
        }
        return false;
    }

    private boolean isValidDistrictLocation(int centerX, int centerY, int radius) {
        // Проверяем границы с учетом адаптивных значений
        int safeMargin = radius + 5;
        if (centerX - safeMargin < 0 || centerX + safeMargin >= world.getWidth() ||
                centerY - safeMargin < 0 || centerY + safeMargin >= world.getHeight()) {
            return false;
        }

        // Проверяем, что центр не в воде
        if (world.getWorldTile(centerX, centerY).isWater()) {
            return false;
        }

        // Проверяем пересечение с существующими районами
        for (CityDistrict existing : districts) {
            double distance = Math.sqrt(Math.pow(centerX - existing.getCenterX(), 2) +
                    Math.pow(centerY - existing.getCenterY(), 2));
            double minDistance = radius + existing.getRadius() + 10;
            if (distance < minDistance) {
                return false;
            }
        }

        return true;
    }

    // Остальные методы без изменений...
    private String generateDistrictName(int index) {
        String[] prefixes = {"Central", "Northern", "Southern", "Eastern", "Western",
                "Old", "New", "Upper", "Lower", "Green", "River", "Hill"};
        String[] suffixes = {"District", "Quarter", "Heights", "Gardens", "Park",
                "Square", "Village", "Hills", "Valley", "Crossing", "View"};

        return prefixes[random.nextInt(prefixes.length)] + " " +
                suffixes[random.nextInt(suffixes.length)];
    }

    public void update() {
        long currentTime = world.getGameTime().getCurrentTimeMillis();

        if (currentTime - lastUpdateTime > DISTRICT_UPDATE_INTERVAL) {
            for (CityDistrict district : districts) {
                try {
                    district.update(currentTime);
                } catch (Exception e) {
                    MetroLogger.logError("Error updating district " + district.getName(), e);
                }
            }
            lastUpdateTime = currentTime;
        }
    }

    public void onBuildingAdded(GameplayUnits building) {
        // Можно добавить логику при необходимости
    }

    public void onBuildingRemoved(GameplayUnits building) {
        for (CityDistrict district : districts) {
            district.removeBuilding(building);
        }
    }

    public List<CityDistrict> getDistricts() { return new ArrayList<>(districts); }
    public int getTotalBuildings() {
        return districts.stream().mapToInt(CityDistrict::getBuildingCount).sum();
    }
}