package metroline.screens.render;

import metroline.objects.gameobjects.Station;

import java.util.HashMap;
import java.util.Map;

public class StationPositionCache {

    private static final Map<String, CachedPosition> positionCache = new HashMap<>();

    public static class CachedPosition {
        public final int drawX;
        public final int drawY;
        public final int drawSize;
        public final long lastUsed;

        public CachedPosition(int drawX, int drawY, int drawSize) {
            this.drawX = drawX;
            this.drawY = drawY;
            this.drawSize = drawSize;
            this.lastUsed = System.currentTimeMillis();
        }
    }

    /**
     * Получает кэшированную позицию или вычисляет новую
     */
    public static CachedPosition getCachedPosition(Station station, int offsetX, int offsetY, float zoom) {
        String key = getCacheKey(station, offsetX, offsetY, zoom);
        CachedPosition cached = positionCache.get(key);

        if (cached != null) {
            return cached;
        }

        // Вычисляем новую позицию
        cached = calculatePosition(station, offsetX, offsetY, zoom);
        positionCache.put(key, cached);
        return cached;
    }

    /**
     * Вычисляет позицию для отрисовки станции
     */
    private static CachedPosition calculatePosition(Station station, int offsetX, int offsetY, float zoom) {
        int drawSize = (int) (24 * zoom);
        int cellCenterX = (int) ((station.getX() * 32 + offsetX + 16) * zoom);
        int cellCenterY = (int) ((station.getY() * 32 + offsetY + 16) * zoom);
        int drawX = cellCenterX - drawSize / 2;
        int drawY = cellCenterY - drawSize / 2;

        return new CachedPosition(drawX, drawY, drawSize);
    }

    /**
     * Генерирует уникальный ключ для кэша
     */
    private static String getCacheKey(Station station, int offsetX, int offsetY, float zoom) {
        return station.getUniqueId() + "_" + station.getX() + "_" + station.getY() +
                "_" + offsetX + "_" + offsetY + "_" + zoom;
    }

    /**
     * Очищает кэш от старых записей (старше 30 секунд)
     */
    public static void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        positionCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue().lastUsed > 30000
        );
    }

    /**
     * Полностью очищает кэш
     */
    public static void clearCache() {
        positionCache.clear();
    }

    /**
     * Удаляет позиции для конкретной станции
     */
    public static void invalidateStationPositions(Station station) {
        positionCache.entrySet().removeIf(entry ->
                entry.getKey().startsWith(station.getUniqueId() + "_")
        );
    }

    /**
     * Возвращает количество кэшированных позиций
     */
    public static int getCacheSize() {
        return positionCache.size();
    }
}
