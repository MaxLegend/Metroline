package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.gameobjects.GameplayUnits;
import metroline.objects.gameobjects.Label;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.render.StationRender;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.List;


//TODO Починить белый экран при переключении в полноэкранный и обратно.
//TODO Сделать определенное кэширование для динамических объектов с перерисовкой только при добавлении.
//TODO Починить все что сломалось. Починить слои. Добавить еще слоев. Убрать множество окон для одного объекта
public abstract class CachedWorldScreen extends WorldScreen {
    protected static final int TILE_SIZE = 32;
    protected static final int CACHE_SCALE = 2;

    protected VolatileImage staticWorldCache;
    protected VolatileImage paymentZonesCache;
    protected VolatileImage passengerZonesCache;

    protected boolean staticCacheValid = false;
    protected boolean paymentZonesCacheValid = false;
    protected boolean passengerZonesCacheValid = false;
    protected boolean worldChanged = true;


    private float cachedMaxAbilityPay = -1;
    private int cachedMaxPassengerCount = -1;
    private long lastCacheUpdate = 0;

    protected Font debugFont = new Font("Monospaced", Font.PLAIN, 12);
    public LinesLegendWindow legendWindow;

    private Runtime RUNTIME = Runtime.getRuntime();

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
            worldChanged = true;
    }
    public void invalidateZonesCache() {
        paymentZonesCacheValid = false;
        passengerZonesCacheValid = false;
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

        // Валидация без рекурсивных вызовов

        int validateResult = staticWorldCache.validate(getGraphicsConfiguration());
        if (validateResult == VolatileImage.IMAGE_INCOMPATIBLE) {
            staticWorldCache.flush();
            staticWorldCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
            validateResult = staticWorldCache.validate(getGraphicsConfiguration());
        }

        if (validateResult != VolatileImage.IMAGE_OK) {
            Graphics2D g = staticWorldCache.createGraphics();
            try {
                clearImage(g, staticWorldCache);
                g.scale(CACHE_SCALE, CACHE_SCALE);
                drawStaticWorld(g);
            } finally {
                g.dispose();
            }

        }
        staticCacheValid = true;
        worldChanged = false;
    }
    protected void updatePaymentZonesCache(int width, int height) {
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

        if (validateResult != VolatileImage.IMAGE_OK) {
            Graphics2D g = paymentZonesCache.createGraphics();
            try {
                clearImage(g, paymentZonesCache);
                g.scale(CACHE_SCALE, CACHE_SCALE);
                drawPaymentZonesToCache(g);
            } finally {
                g.dispose();
            }
        }
        paymentZonesCacheValid = true;
    }

    protected void updatePassengerZonesCache(int width, int height) {
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

        if (validateResult != VolatileImage.IMAGE_OK) {
            Graphics2D g = passengerZonesCache.createGraphics();
            try {
                clearImage(g, passengerZonesCache);
                g.scale(CACHE_SCALE, CACHE_SCALE);
                drawPassengerZonesToCache(g);
            } finally {
                g.dispose();
            }
        }
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


    public void renderWorld(Graphics2D g) {
        long frameStartTime = System.nanoTime();
        int width = getWorld().getWidth();
        int height = getWorld().getHeight();

        if (!staticCacheValid) {
            updateStaticWorldCache(width, height);
        }
        drawVolatileImage(g, staticWorldCache, 0, 0, () -> updateStaticWorldCache(width, height));
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
        int tileSize = TILE_SIZE;

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

                    g.setColor(zoneColor.darker());
                    g.drawRect(drawX, drawY, tileSize, tileSize);
                }
            }
        }
    }

    protected void drawPassengerZonesToCache(Graphics2D g) {
        updateZoneCache();
        int tileSize = TILE_SIZE;

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

                    g.setColor(zoneColor.darker());
                    g.drawRect(drawX, drawY, tileSize, tileSize);
                }
            }
        }
    }
    protected void drawDynamicWorld(Graphics2D g) {
        AffineTransform originalTransform = g.getTransform();

        drawAnimatedWater(g);
        drawTunnels(g);
        drawStations(g);
        if(GameWorld.showGameplayUnits) {
            drawGameplayUnits(g);
        }
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

    private int calculateMaxPassengerCount() {
        int max = 0;
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
        for (Label label : getWorld().getLabels()) {
            label.draw(g, 0, 0, 1);
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

    protected void drawPaymentZones(Graphics2D g) {
        if (!GameWorld.showPaymentZones) return;

        int tileSize = TILE_SIZE;
        float maxAbilityPay = getMaxAbilityPay(); // Нужно будет реализовать этот метод

        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null) {
                    int drawX = x * tileSize;
                    int drawY = y * tileSize;

                    // Градиент от синего (низкая) к красному (высокая)
                    float ratio = tile.getAbilityPay() / maxAbilityPay;
                    Color zoneColor = getPaymentZoneColor(ratio);

                    // Полупрозрачная заливка
                    g.setColor(new Color(zoneColor.getRed(), zoneColor.getGreen(),
                            zoneColor.getBlue(), 100)); // 40% прозрачность
                    g.fillRect(drawX, drawY, tileSize, tileSize);

                    // Контур
                    g.setColor(zoneColor.darker());
                    g.drawRect(drawX, drawY, tileSize, tileSize);
                }
            }
        }
    }

    protected void drawPassengerZones(Graphics2D g) {
        if (!GameWorld.showPassengerZones) return;

        int tileSize = TILE_SIZE;
        int maxPassengers = getMaxPassengerCount(); // Нужно будет реализовать этот метод

        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null) {
                    int drawX = x * tileSize;
                    int drawY = y * tileSize;

                    // Градиент от зеленого (мало) к желтому (много)
                    float ratio = (float) tile.getPassengerCount() / maxPassengers;
                    Color zoneColor = getPassengerZoneColor(ratio);

                    // Полупрозрачная заливка
                    g.setColor(new Color(zoneColor.getRed(), zoneColor.getGreen(),
                            zoneColor.getBlue(), 100)); // 40% прозрачность
                    g.fillRect(drawX, drawY, tileSize, tileSize);

                    // Контур
                    g.setColor(zoneColor.darker());
                    g.drawRect(drawX, drawY, tileSize, tileSize);
                }
            }
        }
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

    private float getMaxAbilityPay() {
        float max = 0;
        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null && tile.getAbilityPay() > max) {
                    max = tile.getAbilityPay();
                }
            }
        }
        return max > 0 ? max : 1; // Защита от деления на ноль
    }

    private int getMaxPassengerCount() {
        int max = 0;
        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                WorldTile tile = getWorld().getWorldTile(x, y);
                if (tile != null && tile.getPassengerCount() > max) {
                    max = tile.getPassengerCount();
                }
            }
        }
        return max > 0 ? max : 1; // Защита от деления на ноль
    }


}