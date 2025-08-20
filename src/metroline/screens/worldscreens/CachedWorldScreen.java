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
import metroline.util.debug.DebugInfoRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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


    protected boolean staticCacheValid = false;

    protected boolean worldChanged = true;

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
        System.out.println("CREATED VOLATILE IMAGE " + width + " " + height);
        return gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
    }

    protected BufferedImage createCompatibleImage(int width, int height) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                                      .getDefaultScreenDevice().getDefaultConfiguration();
        return gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
    }

    public void invalidateCache() {

            staticCacheValid = false;
            worldChanged = true;


    }

    //    protected void updateStaticWorldCache(int width, int height) {
//        if (needToRecreateCache(staticWorldCache, width, height)) {
//            staticWorldCache = createCompatibleVolatileImage(width * TILE_SIZE, height * TILE_SIZE);
//        }
//
//        // Проверяем и восстанавливаем изображение если необходимо
//        validateVolatileImage(staticWorldCache);
//
//        Graphics2D g = staticWorldCache.createGraphics();
//        try {
//            clearImage(g, staticWorldCache);
//            g.scale(CACHE_SCALE, CACHE_SCALE);
//            drawStaticWorld(g);
//        } finally {
//            g.dispose();
//        }
//
//        staticCacheValid = true;
//        worldChanged = false;
//    }
    protected void updateStaticWorldCache(int width, int height) {
        boolean needsRecreation = needToRecreateCache(staticWorldCache, width, height);

        if (needsRecreation) {
            if (staticWorldCache != null) {
                staticWorldCache.flush();
            }
            if(width >0 && height >0)
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
        if (!staticCacheValid || !validateVolatileImage(staticWorldCache, () -> updateStaticWorldCache(getWorld().getWidth(), getWorld().getHeight()))) {
            updateStaticWorldCache(getWorld().getWidth(), getWorld().getHeight());
        }
        drawVolatileImage(g, staticWorldCache, 0, 0, () -> updateStaticWorldCache(getWorld().getWidth(), getWorld().getHeight()));
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


    protected void drawDynamicWorld(Graphics2D g) {
        AffineTransform originalTransform = g.getTransform();

        drawTunnels(g);
        drawStations(g);
        drawGameplayUnits(g);
        drawLabels(g);

        g.setTransform(originalTransform);
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



    protected void drawDebugInfo(Graphics2D g, Object selectedObject) {
        g.setFont(debugFont);
        g.setColor(Color.YELLOW);

        int yPos = 60;
        String[] debugInfo = {
                "=== GLOBAL DEBUG INFO ===",
                "Stations: " + getWorld().getStations().size(),
                "Tunnels: " + getWorld().getTunnels().size(),
                "Labels: " + getWorld().getLabels().size(),
                String.format("Zoom: %.2f", zoom),
                "Offset: (" + offsetX + "," + offsetY + ")"
        };

        for (String line : debugInfo) {
            g.drawString(line, 10, yPos);
            yPos += 15;
        }

        if (selectedObject != null) {
            DebugInfoRenderer.renderDebugInfo(g, selectedObject, getWorld(), yPos);
        }
    }
}