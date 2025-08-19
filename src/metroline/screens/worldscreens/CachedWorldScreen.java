package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.core.world.World;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class CachedWorldScreen extends WorldScreen {
    protected static final int TILE_SIZE = 32;
    protected static final int CACHE_SCALE = 2;

    protected BufferedImage staticWorldCache;
    protected BufferedImage dynamicElementsCache;

    protected boolean staticCacheValid = false;
    protected boolean dynamicCacheValid = false;

    protected boolean worldChanged = true;
    protected boolean stationsChanged = true;
    protected boolean tunnelsChanged = true;

    protected Font debugFont = new Font("Monospaced", Font.PLAIN, 12);
    public LinesLegendWindow legendWindow;

    public CachedWorldScreen(MainFrame parent, World world) {
        super(parent, world);
    }

    public void setLegendWindow(LinesLegendWindow legendWindow) {
        this.legendWindow = legendWindow;
        if (getWorld() instanceof GameWorld) {
            ((GameWorld)getWorld()).setLegendWindow(legendWindow);
        }
    }

    protected void initWorldCache(int width, int height) {
        int cacheWidth = width * TILE_SIZE;
        int cacheHeight = height * TILE_SIZE;

        staticWorldCache = createCompatibleImage(cacheWidth, cacheHeight);
        dynamicElementsCache = createCompatibleImage(cacheWidth, cacheHeight);
    }

    protected BufferedImage createCompatibleImage(int width, int height) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                                      .getDefaultScreenDevice().getDefaultConfiguration();
        return gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
    }

    public void invalidateCache(boolean invalidateStatic) {
        if (invalidateStatic) {
            staticCacheValid = false;
            worldChanged = true;
        }
        dynamicCacheValid = false;
    }

    protected void updateStaticWorldCache(int width, int height) {
        if (needToRecreateCache(staticWorldCache, width, height)) {
            staticWorldCache = createCompatibleImage(width * TILE_SIZE, height * TILE_SIZE);
        }

        Graphics2D g = staticWorldCache.createGraphics();
        try {
            clearImage(g, staticWorldCache);
            g.scale(CACHE_SCALE, CACHE_SCALE);
            drawStaticWorld(g);
        } finally {
            g.dispose();
        }

        staticCacheValid = true;
        worldChanged = false;
    }

    protected void updateDynamicElementsCache(int width, int height) {
        if (needToRecreateCache(dynamicElementsCache, width, height)) {
            dynamicElementsCache = createCompatibleImage(width * TILE_SIZE, height * TILE_SIZE);
        }

        Graphics2D g = dynamicElementsCache.createGraphics();
        try {
            clearImage(g, dynamicElementsCache);
            g.scale(CACHE_SCALE, CACHE_SCALE);
            drawDynamicWorld(g);
        } finally {
            g.dispose();
        }

        dynamicCacheValid = true;
        stationsChanged = false;
        tunnelsChanged = false;
    }

    private boolean needToRecreateCache(BufferedImage cache, int width, int height) {
        return cache == null ||
                cache.getWidth() != width * TILE_SIZE ||
                cache.getHeight() != height * TILE_SIZE;
    }

    private void clearImage(Graphics2D g, BufferedImage image) {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setComposite(AlphaComposite.SrcOver);
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
            for (GameplayUnits unit : ((GameWorld)getWorld()).getGameplayUnits()) {
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
                getWorld().getWorldGrid()[x][y].draw(g, 0, 0, 1);
            }
        }

        g.setTransform(originalTransform);
    }

    public void notifyWorldChanged() {
        worldChanged = true;
        invalidateCache(true);
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