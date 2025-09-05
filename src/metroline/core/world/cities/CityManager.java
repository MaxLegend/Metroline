package metroline.core.world.cities;

import metroline.core.world.GameWorld;
import metroline.objects.gameobjects.GameplayUnits;
import metroline.util.MetroLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static metroline.objects.gameobjects.GameConstants.COUNT_DISTRICTS;

public class CityManager {
    private final GameWorld world;
    private final List<CityDistrict> districts;
    private final Random random;
    private long lastUpdateTime;

    /*******************************
     * CONSTANTS SECTION
     *******************************/
    private static final double DISTRICTS_PER_10000_CELLS = 2.5;

    private static final int DISTRICT_UPDATE_INTERVAL = 8000000;
    private static final int EXPANSION_CHECK_INTERVAL = 120000;

    private long lastZoneUpdateTime;
    private static final int ZONE_UPDATE_INTERVAL = 60000; // Обновлять зоны каждые 30 секунд
    /*******************************
     * CONSTRUCTOR SECTION
     *******************************/
    public CityManager(GameWorld world) {
        this.world = world;
        this.districts = new ArrayList<>();
        this.random = new Random();
        this.lastUpdateTime = world.getGameTime().getCurrentTimeMillis();
        generateDistricts();
    }

    /*******************************
     * INITIALIZATION SECTION
     *******************************/
    private void generateDistricts() {
        int districtCount = calculateDistrictCount();
        int attempts = 0;
        int created = 0;

        int minDistanceFromEdge = Math.min(world.getWidth(), world.getHeight()) / 10;
        minDistanceFromEdge = Math.max(5, minDistanceFromEdge);

        // Увеличиваем максимальное количество попыток
        while (created < districtCount && attempts < districtCount * 5) {
            if (createDistrict(created, minDistanceFromEdge)) {
                created++;
            }
            attempts++;
        }

        // Логируем результат для отладки
        MetroLogger.logInfo("Generated " + created + " districts out of " + districtCount + " requested");
    }

    private int calculateDistrictCount() {
        // Если COUNT_DISTRICTS задан (больше 0), используем его с небольшим разбросом
        if (COUNT_DISTRICTS > 0) {
            int variation = (int) (COUNT_DISTRICTS * 0.15); // 15% разброс
            variation = Math.max(1, variation); // Минимум 1 для вариативности
            return (int) (COUNT_DISTRICTS - variation + random.nextInt(variation * 2 + 1));
        }

        // Автоматический расчет только если COUNT_DISTRICTS не задан
        double area = world.getWidth() * world.getHeight();
        int calculatedCount = (int) Math.round(area / 10000.0 * DISTRICTS_PER_10000_CELLS);

        // Добавляем вариативность к автоматическому расчету
        int variation = (int) (calculatedCount * 0.25);
        variation = Math.max(1, variation);
        return calculatedCount - variation + random.nextInt(variation * 2 + 1);
    }

    private boolean createDistrict(int index, int minDistanceFromEdge) {
        int centerX = minDistanceFromEdge + random.nextInt(world.getWidth() - 2 * minDistanceFromEdge);
        int centerY = minDistanceFromEdge + random.nextInt(world.getHeight() - 2 * minDistanceFromEdge);

        int maxRadius = Math.min(world.getWidth(), world.getHeight()) / 6;
        maxRadius = Math.max(10, Math.min(30, maxRadius));
        int radius = 8 + random.nextInt(maxRadius - 8);

        if (isValidDistrictLocation(centerX, centerY, radius)) {
            String name = generateDistrictName(index);
            CityDistrict district = new CityDistrict(world, centerX, centerY, radius, name);
            districts.add(district);
            return true;
        }
        return false;
    }

    private boolean isValidDistrictLocation(int centerX, int centerY, int radius) {
        // Базовая проверка границ
        int safeMargin = radius + 3; // Уменьшаем запас
        if (centerX - safeMargin < 0 || centerX + safeMargin >= world.getWidth() ||
                centerY - safeMargin < 0 || centerY + safeMargin >= world.getHeight()) {
            return false;
        }

        // Проверка воды
        if (world.getWorldTile(centerX, centerY).isWater()) {
            return false;
        }

        // Проверка расстояния до других районов (можно уменьшить минимальное расстояние)
        for (CityDistrict existing : districts) {
            double distance = Math.sqrt(Math.pow(centerX - existing.getCenterX(), 2) + Math.pow(centerY - existing.getCenterY(), 2));
            double minDistance = radius + existing.getRadius() + 5; // Уменьшаем с 10 до 5
            if (distance < minDistance) {
                return false;
            }
        }

        return true;
    }

    private String generateDistrictName(int index) {
        String[] prefixes = {"Central", "Northern", "Southern", "Eastern", "Western",
                "Old", "New", "Upper", "Lower", "Green", "River", "Hill"};
        String[] suffixes = {"District", "Quarter", "Heights", "Gardens", "Park",
                "Square", "Village", "Hills", "Valley", "Crossing", "View"};

        return prefixes[random.nextInt(prefixes.length)] + " " + suffixes[random.nextInt(suffixes.length)];
    }

    /*******************************
     * UPDATE SECTION
     *******************************/
    public void update() {
        long currentTime = world.getGameTime().getCurrentTimeMillis();

        if (currentTime - lastUpdateTime > DISTRICT_UPDATE_INTERVAL) {
            for (CityDistrict district : districts) {
                    district.update(currentTime);
            }
            lastUpdateTime = currentTime;
        }
        // Автоматическое обновление зон
        if (currentTime - lastZoneUpdateTime > ZONE_UPDATE_INTERVAL) {
            world.updatePaymentAndPassengerZones();
            lastZoneUpdateTime = currentTime;
        }
    }

    /*******************************
     * BUILDING MANAGEMENT SECTION
     *******************************/
    public void onBuildingAdded(GameplayUnits building) {
        for (CityDistrict district : districts) {
            double distance = Math.sqrt(Math.pow(building.getX() - district.getCenterX(), 2) +
                    Math.pow(building.getY() - district.getCenterY(), 2));
            if (distance <= district.getRadius() + 5) {
                district.getBuildings().add(building);

                // НЕМЕДЛЕННО обновляем зоны при добавлении здания
                world.updatePaymentAndPassengerZones();
                break;
            }
        }
    }

    public void onBuildingRemoved(GameplayUnits building) {
        boolean removed = false;
        for (CityDistrict district : districts) {
            if (district.removeBuilding(building)) {
                removed = true;
            }
        }

        // НЕМЕДЛЕННО обновляем зоны при удалении здания
        if (removed) {
            world.updatePaymentAndPassengerZones();
        }
    }

    /*******************************
     * GETTERS SECTION
     *******************************/
    public List<CityDistrict> getDistricts() {
        return new ArrayList<>(districts);
    }

    public int getTotalBuildings() {
        return districts.stream().mapToInt(CityDistrict::getBuildingCount).sum();
    }
}