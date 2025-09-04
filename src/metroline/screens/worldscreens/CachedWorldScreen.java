package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.gameobjects.GameplayUnits;
import metroline.objects.gameobjects.StationLabel;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.render.StationRender;
import metroline.util.compressor.WorldThreadImageCompressor;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.List;



// TODO Добавить еще слоев. Убрать множество окон для одного объекта
public class CachedWorldScreen extends WorldScreen {
    protected static final int WORLD_TILE_SIZE = 16;
    protected static final int TILE_SIZE = 32;
    protected static final int CACHE_SCALE = 2;

    protected VolatileImage staticWorldCache;
    public VolatileImage paymentZonesCache;
    public VolatileImage passengerZonesCache;
    protected VolatileImage grassZonesCache;

    protected boolean staticCacheValid = false;
    protected boolean paymentZonesCacheValid = false;
    protected boolean passengerZonesCacheValid = false;
    protected boolean grassZonesCacheValid = false; // Флаг валидности кэша травы
    protected boolean worldChanged = true;


    private float cachedMaxAbilityPay = -1;
    private float cachedMaxPassengerCount = -1;
    private long lastCacheUpdate = 0;

    protected Font debugFont = new Font("Monospaced", Font.PLAIN, 12);
    public LinesLegendWindow legendWindow;

    protected BufferedImage compressedStaticCache;
    protected BufferedImage compressedPaymentZonesCache;
    protected BufferedImage compressedPassengerZonesCache;
    protected BufferedImage compressedGrassZonesCache;

    protected boolean compressedCacheValid = false;

    /**
     * Обновляет сжатый кэш статического мира
     */
    protected void updateCompressedStaticCache() {
        if (staticWorldCache == null || compressedCacheValid || compressedStaticCache != null) {
            return; // Уже сжато
        }
        if (staticWorldCache == null) {
            compressedStaticCache = null;
            return;
        }

        int worldWidth = getWorld().getWidth();
        int worldHeight = getWorld().getHeight();

        compressedStaticCache = WorldThreadImageCompressor.compressWorldImageOptimized(
                staticWorldCache,
                worldWidth,
                worldHeight,
                TILE_SIZE
        );

        compressedCacheValid = true;
    }

    /**
     * Восстанавливает VolatileImage из сжатого кэша
     */
    protected void restoreFromCompressedCache() {
        if (compressedStaticCache == null) return;

        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc == null) return;

        // Распаковываем и создаем VolatileImage
        BufferedImage decompressed = WorldThreadImageCompressor.decompressWorldImage(
                compressedStaticCache,
                TILE_SIZE
        );

        staticWorldCache = WorldThreadImageCompressor.convertBufferedImageToVolatileImage(
                decompressed,
                gc
        );

        // Освобождаем память от временных объектов
        decompressed.flush();
    }

    /**
     * Освобождает память, занятую сжатыми кэшами
     */
    public void flushCompressedCaches() {
        if (compressedStaticCache != null) {
            compressedStaticCache.flush();
            compressedStaticCache = null;
        }
        if (compressedPaymentZonesCache != null) {
            compressedPaymentZonesCache.flush();
            compressedPaymentZonesCache = null;
        }
        if (compressedPassengerZonesCache != null) {
            compressedPassengerZonesCache.flush();
            compressedPassengerZonesCache = null;
        }
        if (compressedGrassZonesCache != null) {
            compressedGrassZonesCache.flush();
            compressedGrassZonesCache = null;
        }
        compressedCacheValid = false;
    }

    public CachedWorldScreen(MainFrame parent, World world) {
        super(parent, world);
    }

    public void setLegendWindow(LinesLegendWindow legendWindow) {
        this.legendWindow = legendWindow;
        if (getWorld() instanceof GameWorld) {
            ((GameWorld) getWorld()).setLegendWindow(legendWindow);
        }
    }

    protected boolean validateVolatileImage(VolatileImage image, Runnable redrawContent) {
        if (image == null) return false;

        int validateResult = image.validate(getGraphicsConfiguration());

        switch (validateResult) {
            case VolatileImage.IMAGE_OK:
                return true;

            case VolatileImage.IMAGE_RESTORED:
                // Содержимое потеряно — перерисовать
                if (redrawContent != null) redrawContent.run();
                return true;

            case VolatileImage.IMAGE_INCOMPATIBLE:
                // Изображение несовместимо — пересоздать
                if (redrawContent != null) redrawContent.run();
                return true;

            default:
                return false;
        }
    }

    protected VolatileImage createCompatibleVolatileImage(int width, int height) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                                      .getDefaultScreenDevice().getDefaultConfiguration();
        return gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
    }

    public void invalidateCache() {
        staticCacheValid = false;
        paymentZonesCacheValid = false;
        passengerZonesCacheValid = false;
        grassZonesCacheValid = false; // Инвалидируем кэш травы
        compressedCacheValid = false;
        worldChanged = true;

        // Освобождаем сжатые кэши при инвалидации
        flushCompressedCaches();
    }

    public void invalidateZonesCache() {
        paymentZonesCacheValid = false;
        passengerZonesCacheValid = false;
        grassZonesCacheValid = false;
        // Сбрасываем кэш максимумов при изменении мира
        cachedMaxAbilityPay = -1;
        cachedMaxPassengerCount = -1;
    }
    protected void updateStaticWorldCache(int width, int height) {
        if (width <= 0 || height <= 0) {
            staticCacheValid = false;
            return;
        }

        boolean needsRecreation = needToRecreateCache(staticWorldCache, width, height);

        if (needsRecreation) {
            if (staticWorldCache != null) {
                staticWorldCache.flush();
            }
            staticWorldCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
        }

        int validateResult = staticWorldCache.validate(getGraphicsConfiguration());
        boolean needsRedraw = (validateResult != VolatileImage.IMAGE_OK);

        if (validateResult == VolatileImage.IMAGE_INCOMPATIBLE) {
            staticWorldCache.flush();
            staticWorldCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
            needsRedraw = true;
        }

        if (needsRedraw) {
            Graphics2D g = staticWorldCache.createGraphics();
            try {
                clearImage(g, staticWorldCache);
                g.scale(CACHE_SCALE, CACHE_SCALE);
                drawStaticWorld(g);
            } finally {
                g.dispose();
            }

            updateCompressedStaticCache();
        }

        staticCacheValid = true;
        worldChanged = false;
    }
    protected void updateGrassZonesCache(int width, int height) {
        if (width <= 0 || height <= 0 || !GameWorld.showGrassZones) {
            grassZonesCacheValid = false;
            return;
        }

        boolean needsRecreation = needToRecreateCache(grassZonesCache, width, height);
        if (needsRecreation) {
            if (grassZonesCache != null) grassZonesCache.flush();
            grassZonesCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
        }

        int validateResult = grassZonesCache.validate(getGraphicsConfiguration());
        if (validateResult == VolatileImage.IMAGE_INCOMPATIBLE) {
            grassZonesCache.flush();
            grassZonesCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
        }


            Graphics2D g = grassZonesCache.createGraphics();
            try {
                clearImage(g, grassZonesCache);
                g.scale(CACHE_SCALE, CACHE_SCALE);
                drawGrassZonesToCache(g);
            } finally {
                g.dispose();
            }

        grassZonesCacheValid = true;
    }
    // Метод для отрисовки зон травы в кэш
    protected void drawGrassZonesToCache(Graphics2D g) {
        int tileSize = TILE_SIZE / 2;

        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null && !tile.isWater()) {
                    int drawX = x * tileSize;
                    int drawY = y * tileSize;

                    // Получаем значение травы для тайла
                    float grassValue = tile.getGrassValue();
                    Color grassColor = getGrassZoneColor(grassValue);

                    // Применяем затемнение если есть соседняя вода
                    grassColor = applyWaterDarkening(grassColor, x, y);

                    // Мягкая полупрозрачная заливка
                    g.setColor(new Color(grassColor.getRed(), grassColor.getGreen(),
                            grassColor.getBlue(), 180));
                    g.fillRect(drawX, drawY, tileSize, tileSize);
                }
            }
        }
    }
    private Color applyWaterDarkening(Color originalColor, int x, int y) {
        float minDistanceToWater = getMinDistanceToWater(x, y);
        if (minDistanceToWater > 3) {
            return originalColor; // Слишком далеко от воды
        }

        // Затемнение зависит от расстояния до воды
        float darkenFactor = 0.8f + (minDistanceToWater * 0.06f); // 0.8-0.98
        float blueTint = 1.0f + (0.1f / (minDistanceToWater + 1)); // +10%-0% синего

        int r = (int) (originalColor.getRed() * darkenFactor);
        int g = (int) (originalColor.getGreen() * darkenFactor);
        int b = (int) (originalColor.getBlue() * blueTint);

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new Color(r, g, b);
    }
    private float getMinDistanceToWater(int x, int y) {
        float minDistance = Float.MAX_VALUE;

        // Проверяем область 5x5 вокруг клетки
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < getWorld().getWidth() &&
                        ny >= 0 && ny < getWorld().getHeight()) {
                    WorldTile neighbor = getWorld().getWorldTile(nx, ny);
                    if (neighbor != null && neighbor.isWater()) {
                        float distance = (float) Math.sqrt(dx * dx + dy * dy);
                        minDistance = Math.min(minDistance, distance);
                    }
                }
            }
        }

        return minDistance == Float.MAX_VALUE ? 999 : minDistance;
    }
    private int countWaterNeighbors(int x, int y) {
        int waterCount = 0;

        // Проверяем все 8 соседних клеток (включая диагонали)
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue; // Пропускаем центральную клетку

                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < getWorld().getWidth() &&
                        ny >= 0 && ny < getWorld().getHeight()) {
                    WorldTile neighbor = getWorld().getWorldTile(nx, ny);
                    if (neighbor != null && neighbor.isWater()) {
                        waterCount++;
                    }
                }
            }
        }

        return waterCount;
    }
    private Color getGrassZoneColor(float grassValue) {
        // Мягкая палитра от светло-салатового к насыщенному зеленому
        // Убраны темные тона, только светлые и средние оттенки

        if (grassValue < 0.3f) {
            // Очень светлые салатовые тона
            float t = grassValue / 0.3f;
            int r = (int) (220 - t * 30);   // 220 -> 190
            int g = (int) (245 - t * 25);   // 245 -> 220
            int b = (int) (200 - t * 40);   // 200 -> 160
            return new Color(r, g, b);
        } else if (grassValue < 0.6f) {
            // Плавный переход к зеленому
            float t = (grassValue - 0.3f) / 0.3f;
            int r = (int) (190 - t * 50);   // 190 -> 140
            int g = (int) (220 + t * 15);   // 220 -> 235
            int b = (int) (160 - t * 40);   // 160 -> 120
            return new Color(r, g, b);
        } else if (grassValue < 0.9f) {
            // Насыщенные зеленые тона
            float t = (grassValue - 0.6f) / 0.3f;
            int r = (int) (140 - t * 30);   // 140 -> 110
            int g = (int) (235 - t * 25);   // 235 -> 210
            int b = (int) (120 - t * 30);   // 120 -> 90
            return new Color(r, g, b);
        } else {
            // Самые насыщенные зеленые (но не темные)
            float t = (grassValue - 0.9f) / 0.1f;
            int r = (int) (110 - t * 20);   // 110 -> 90
            int g = (int) (210 - t * 10);   // 210 -> 200
            int b = (int) (90 - t * 20);    // 90 -> 70
            return new Color(r, g, b);
        }
    }
    public void updatePaymentZonesCache(int width, int height) {
        if (width <= 0 || height <= 0 || !GameWorld.showPaymentZones) {
            paymentZonesCacheValid = false;
            return;
        }

        boolean needsRecreation = needToRecreateCache(paymentZonesCache, width, height);
        if (needsRecreation) {
            if (paymentZonesCache != null) paymentZonesCache.flush();
            paymentZonesCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
        }

        int validateResult = paymentZonesCache.validate(getGraphicsConfiguration());
        if (validateResult == VolatileImage.IMAGE_INCOMPATIBLE) {
            paymentZonesCache.flush();
            paymentZonesCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
        }

    //    if (validateResult != VolatileImage.IMAGE_OK) {
            Graphics2D g = paymentZonesCache.createGraphics();
            try {
                clearImage(g, paymentZonesCache);
                g.scale(CACHE_SCALE, CACHE_SCALE);
                drawPaymentZonesToCache(g);
            } finally {
                g.dispose();
  //          }
        }
        paymentZonesCacheValid = true;
    }

    public void updatePassengerZonesCache(int width, int height) {
        if (width <= 0 || height <= 0 || !GameWorld.showPassengerZones) {
            passengerZonesCacheValid = false;
            return;
        }

        boolean needsRecreation = needToRecreateCache(passengerZonesCache, width, height);
        if (needsRecreation) {
            if (passengerZonesCache != null) passengerZonesCache.flush();
            passengerZonesCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
        }

        int validateResult = passengerZonesCache.validate(getGraphicsConfiguration());
        if (validateResult == VolatileImage.IMAGE_INCOMPATIBLE) {
            passengerZonesCache.flush();
            passengerZonesCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
        }

   //     if (validateResult != VolatileImage.IMAGE_OK) {
            Graphics2D g = passengerZonesCache.createGraphics();
            try {
                clearImage(g, passengerZonesCache);
                g.scale(CACHE_SCALE, CACHE_SCALE);
                drawPassengerZonesToCache(g);
            } finally {
                g.dispose();
            }
   //     }
        passengerZonesCacheValid = true;
    }
    private boolean needToRecreateCache(VolatileImage cache, int width, int height) {
        return cache == null ||
                cache.getWidth() != width * TILE_SIZE ||
                cache.getHeight() != height * TILE_SIZE;
    }

    private void clearImage(Graphics2D g, VolatileImage image) {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setComposite(AlphaComposite.SrcOver);
    }

    // Добавляем финализатор для очистки ресурсов
    @Override
    protected void finalize() throws Throwable {
        try {
            flushCompressedCaches();
        } finally {
            super.finalize();
        }
    }
    public void renderWorld(Graphics2D g) {
        long frameStartTime = System.nanoTime();
        int width = getWorld().getWidth();
        int height = getWorld().getHeight();

        if (compressedCacheValid && staticCacheValid && staticWorldCache == null) {
            restoreFromCompressedCache();
            if (staticWorldCache != null) {
                System.out.println("[RENDER] Restored from compressed cache");
            }
        }

        // Если всё ещё нет кэша — пересоздаём
        if (!staticCacheValid || staticWorldCache == null) {
            updateStaticWorldCache(width, height);
        }
        if (!GameWorld.showGrassZones) {
            drawVolatileImage(g, staticWorldCache, 0, 0, () -> updateStaticWorldCache(width, height));
            // Обновляем и рисуем кэш зон травы
        } else {
            if (!grassZonesCacheValid || !validateVolatileImage(grassZonesCache,
                    () -> updateGrassZonesCache(width, height))) {
                updateGrassZonesCache(width, height);
            }
            drawVolatileImage(g, grassZonesCache, 0, 0,
                    () -> updateGrassZonesCache(width, height));
        }
        // Обновляем и рисуем кэш зон платежеспособности
        if (GameWorld.showPaymentZones) {
            if (!paymentZonesCacheValid || !validateVolatileImage(paymentZonesCache,
                    () -> updatePaymentZonesCache(width, height))) {
                updatePaymentZonesCache(width, height);
            }
            drawVolatileImage(g, paymentZonesCache, 0, 0,
                    () -> updatePaymentZonesCache(width, height));
        }

        // Обновляем и рисуем кэш зон пассажиропотока
        if (GameWorld.showPassengerZones) {
            if (!passengerZonesCacheValid || !validateVolatileImage(passengerZonesCache,
                    () -> updatePassengerZonesCache(width, height))) {
                updatePassengerZonesCache(width, height);
            }
            drawVolatileImage(g, passengerZonesCache, 0, 0,
                    () -> updatePassengerZonesCache(width, height));
        }
        // Инкрементальная сборка мусора
        incrementalGarbageCollection();

        // Контроль FPS
        controlFrameRate(frameStartTime);
    }

    protected void drawVolatileImage(Graphics2D g, VolatileImage image, int x, int y, Runnable redrawContent) {
        if (image == null) return;

        int maxAttempts = 3; // Защита от бесконечного цикла
        int attempts = 0;

        while (attempts < maxAttempts) {
            int result = image.validate(getGraphicsConfiguration());

            switch (result) {
                case VolatileImage.IMAGE_OK:
                    g.drawImage(image, x, y, null);
                    return; // Успешно нарисовали

                case VolatileImage.IMAGE_RESTORED:
                    if (redrawContent != null) {
                        redrawContent.run();
                    }
                    g.drawImage(image, x, y, null);
                    return;

                case VolatileImage.IMAGE_INCOMPATIBLE:
                    // Пересоздаём изображение
                    if (staticWorldCache != null) {
                        staticWorldCache.flush();
                    }
                    staticWorldCache = createCompatibleVolatileImage(
                            getWorld().getWidth() * TILE_SIZE,
                            getWorld().getHeight() * TILE_SIZE
                    );
                    if (redrawContent != null) {
                        redrawContent.run();
                    }
                    g.drawImage(staticWorldCache, x, y, null);
                    return;
            }

            attempts++;
            try {
                Thread.sleep(10); // Небольшая задержка
            } catch (InterruptedException ignored) {}
        }

        // Если не удалось нарисовать после нескольких попыток
        System.err.println("Failed to draw volatile image after " + maxAttempts + " attempts");
    }

    // Методы для отрисовки в кэш
    protected void drawPaymentZonesToCache(Graphics2D g) {
        updateZoneCache();
        int tileSize = TILE_SIZE/2;

        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null) {
                    int drawX = x * tileSize;
                    int drawY = y * tileSize;

                    float ratio = tile.getAbilityPay() / cachedMaxAbilityPay;
                    Color zoneColor = getPaymentZoneColor(ratio);

                    g.setColor(new Color(zoneColor.getRed(), zoneColor.getGreen(),
                            zoneColor.getBlue(), 100));
                    g.fillRect(drawX, drawY, tileSize, tileSize);

                }
            }
        }
    }

    protected void drawPassengerZonesToCache(Graphics2D g) {
        updateZoneCache();
        int tileSize = TILE_SIZE/2;

        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null) {
                    int drawX = x * tileSize;
                    int drawY = y * tileSize;

                    float ratio = (float) tile.getPassengerCount() / cachedMaxPassengerCount;
                    Color zoneColor = getPassengerZoneColor(ratio);

                    g.setColor(new Color(zoneColor.getRed(), zoneColor.getGreen(),
                            zoneColor.getBlue(), 100));
                    g.fillRect(drawX, drawY, tileSize, tileSize);

                }
            }
        }
    }
    protected void drawDynamicWorld(Graphics2D g) {
        AffineTransform originalTransform = g.getTransform();


        drawTunnels(g);
        drawStations(g);

        drawLabels(g);

        g.setTransform(originalTransform);
    }
    private void updateZoneCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > 5000 || cachedMaxAbilityPay == -1 || cachedMaxPassengerCount == -1) {
            cachedMaxAbilityPay = calculateMaxAbilityPay();
            cachedMaxPassengerCount = calculateMaxPassengerCount();
            lastCacheUpdate = currentTime;
        }
    }

    private float calculateMaxAbilityPay() {
        float max = 0;
        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null && tile.getAbilityPay() > max) {
                    max = tile.getAbilityPay();
                }
            }
        }
        return max > 0 ? max : 1;
    }

    private float calculateMaxPassengerCount() {
        float max = 0;
        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null && tile.getPassengerCount() > max) {
                    max = tile.getPassengerCount();
                }
            }
        }
        return max > 0 ? max : 1;
    }
    protected void drawTunnels(Graphics2D g) {
        for (Tunnel tunnel : getWorld().getTunnels()) {
            tunnel.draw(g, 0, 0, 1);
        }
    }

    protected void drawStations(Graphics2D g) {
        boolean roundStations = getWorld().isRoundStationsEnabled();

        // Draw station bases
        for (Station station : getWorld().getStations()) {
            if (roundStations) {
                StationRender.drawWorldColorRing(station, g, 0, 0, 1);
           //     StationRender.isRoundRingRendered = true;

            } else {
                StationRender.drawWorldColorSquare(station, g, 0, 0, 1);
            }
            StationRender.drawRoundTransfer(station, g, 0, 0, 1);
        }

        // Draw station details in sorted order
        List<Station> sortedStations = getAllStationsSorted();
        for (Station station : sortedStations) {
            if (roundStations) {
                StationRender.drawRoundStation(station, g, 0, 0, 1);

            } else {
                StationRender.drawSquareStation(station, g, 0, 0, 1);
            }
        }
    }

    protected void drawGameplayUnits(Graphics2D g) {
        if (getWorld() instanceof GameWorld) {
            for (GameplayUnits unit : ((GameWorld) getWorld()).getGameplayUnits()) {
                unit.draw(g, 0, 0, 1);
            }
        }
    }

    protected void drawLabels(Graphics2D g) {
        for (StationLabel stationLabel : getWorld().getLabels()) {
            stationLabel.draw(g, 0, 0, 1);
        }
    }
    protected void drawAnimatedWater(Graphics2D g) {
        int tileSize = 32; // или твоя константа размера

        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null && tile.isWater()) {
                    int drawX = x * tileSize;
                    int drawY = y * tileSize;

                    g.setColor(tile.getAnimatedWaterColor());
                    g.fillRect(drawX, drawY, tileSize, tileSize);
                }
            }
        }
    }

    protected List<Station> getAllStationsSorted() {
        List<Station> stations = new ArrayList<>(getWorld().getStations());
        stations.sort((a, b) -> {
            int yCompare = Integer.compare(a.getY(), b.getY());
            return yCompare != 0 ? yCompare : Integer.compare(a.getX(), b.getX());
        });
        return stations;
    }

    public void drawStaticWorld(Graphics2D g) {
        AffineTransform originalTransform = g.getTransform();

        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                tile.draw(g, 0, 0, 1);
            }
        }

        g.setTransform(originalTransform);
    }


    private Color getPaymentZoneColor(float ratio) {
        // Градиент: синий (0.0) -> фиолетовый (0.5) -> красный (1.0)
        if (ratio < 0.5f) {
            // Синий -> Фиолетовый
            int r = (int) (ratio * 2 * 255);
            int g = 0;
            int b = 255;
            return new Color(r, g, b);
        } else {
            // Фиолетовый -> Красный
            int r = 255;
            int g = 0;
            int b = (int) ((1 - (ratio - 0.5f) * 2) * 255);
            return new Color(r, g, b);
        }
    }

    private Color getPassengerZoneColor(float ratio) {
        // Градиент: зеленый (0.0) -> желтый (0.5) -> оранжевый (1.0)
        if (ratio < 0.5f) {
            // Зеленый -> Желтый
            int r = (int) (ratio * 2 * 255);
            int g = 255;
            int b = 0;
            return new Color(r, g, b);
        } else {
            // Желтый -> Оранжевый
            int r = 255;
            int g = (int) ((1 - (ratio - 0.5f)) * 255);
            int b = 0;
            return new Color(r, g, b);
        }
    }


}