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
    protected BufferedImage worldCache;
    protected BufferedImage staticWorldCache; // Отдельный кэш для статичных элементов
    protected BufferedImage dynamicElementsCache; // Кэш для динамических элементов
    protected boolean staticCacheValid = false;
    protected boolean dynamicCacheValid = false;
    protected Font debugFont = new Font("Monospaced", Font.PLAIN, 12);
    public LinesLegendWindow legendWindow;

    // Флаги для отслеживания изменений
    protected boolean worldChanged = true;
    protected boolean stationsChanged = true;
    protected boolean tunnelsChanged = true;

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
        worldCache = new BufferedImage(width * 32, height * 32, BufferedImage.TYPE_INT_ARGB);
        staticWorldCache = new BufferedImage(width * 32, height * 32, BufferedImage.TYPE_INT_ARGB);
        dynamicElementsCache = new BufferedImage(width * 32, height * 32, BufferedImage.TYPE_INT_ARGB);
    }

    public void invalidateCache(boolean invalidateStatic) {
        if (invalidateStatic) {
            staticCacheValid = false;
            worldChanged = true;
        }
        dynamicCacheValid = false;
    }

    protected void updateStaticWorldCache(int width, int height) {
        if (staticWorldCache == null || staticWorldCache.getWidth() != width * 32 ||
                staticWorldCache.getHeight() != height * 32) {
            staticWorldCache = new BufferedImage(width * 32, height * 32, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D cacheGraphics = staticWorldCache.createGraphics();
        try {
            // Очищаем полностью
            cacheGraphics.setComposite(AlphaComposite.Clear);
            cacheGraphics.fillRect(0, 0, staticWorldCache.getWidth(), staticWorldCache.getHeight());
            cacheGraphics.setComposite(AlphaComposite.SrcOver);

            // Масштабирование для HQ-рендеринга
            cacheGraphics.scale(2, 2);

            // Отрисовываем только статичный мир (сетку)
            drawStaticWorld(cacheGraphics);
        } finally {
            cacheGraphics.dispose();
        }
        staticCacheValid = true;
        worldChanged = false;
    }

    protected void updateDynamicElementsCache(int width, int height) {
        if (dynamicElementsCache == null || dynamicElementsCache.getWidth() != width * 32 ||
                dynamicElementsCache.getHeight() != height * 32) {
            dynamicElementsCache = new BufferedImage(width * 32, height * 32, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D cacheGraphics = dynamicElementsCache.createGraphics();
        try {
            // Очищаем только альфа-канал
            cacheGraphics.setComposite(AlphaComposite.Clear);
            cacheGraphics.fillRect(0, 0, dynamicElementsCache.getWidth(), dynamicElementsCache.getHeight());
            cacheGraphics.setComposite(AlphaComposite.SrcOver);

            // Масштабирование для HQ-рендеринга
            cacheGraphics.scale(2, 2);

            // Отрисовываем динамические элементы
            drawDynamicWorld(cacheGraphics);
        } finally {
            cacheGraphics.dispose();
        }
        dynamicCacheValid = true;
        stationsChanged = false;
        tunnelsChanged = false;
    }
    protected void drawDynamicWorld(Graphics2D g) {
        AffineTransform originalTransform = g.getTransform();

        // Туннели
        for (Tunnel tunnel : getWorld().getTunnels()) {

            tunnel.draw(g, 0, 0, 1);
        }

        // Станции
        if(getWorld().isRoundStationsEnabled()) {
            for (Station station : getWorld().getStations()) {
                StationRender.drawWorldColorRing(station, g, 0, 0, 1);
                StationRender.drawRoundTransfer(station, g, 0, 0, 1);
            }
            for (Station station : getAllStationsSorted()) {
                StationRender.drawRoundStation(station, g, 0, 0, 1);
            }
        } else {
            for (Station station : getWorld().getStations()) {
                StationRender.drawWorldColorSquare(station, g, 0, 0, 1);
                StationRender.drawRoundTransfer(station, g, 0, 0, 1);
            }
            for (Station station : getAllStationsSorted()) {
                StationRender.drawSquareStation(station, g, 0, 0, 1);
            }
        }

        // Игровые объекты
        if (getWorld() instanceof GameWorld) {
            for (GameplayUnits gUnits : ((GameWorld)getWorld()).getGameplayUnits()) {
                gUnits.draw(g, 0, 0, 1);
            }
        }

        // Метки
        for (Label label : getWorld().getLabels()) {
            label.draw(g, 0, 0, 1);
        }

        g.setTransform(originalTransform);
    }
    private java.util.List<Station> getAllStationsSorted() {
        // Сортируем станции по координатам, чтобы избежать перекрытий
        List<Station> stations = new ArrayList<>(getWorld().getStations());
        stations.sort((a, b) -> {
            if (a.getY() != b.getY()) return Integer.compare(a.getY(), b.getY());
            return Integer.compare(a.getX(), b.getX());
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
        // Общая реализация отладочной информации
        g.setFont(debugFont);
        g.setColor(Color.YELLOW);

        int yPos = 60;
        g.drawString("=== GLOBAL DEBUG INFO ===", 10, yPos);
        yPos += 15;
        g.drawString("Stations: " + getWorld().getStations().size(), 10, yPos);
        yPos += 15;
        g.drawString("Tunnels: " + getWorld().getTunnels().size(), 10, yPos);
        yPos += 15;
        g.drawString("Labels: " + getWorld().getLabels().size(), 10, yPos);
        yPos += 15;
        g.drawString("Zoom: " + String.format("%.2f", zoom), 10, yPos);
        yPos += 15;
        g.drawString("Offset: (" + offsetX + "," + offsetY + ")", 10, yPos);
        yPos += 15;

        // Дополнительная информация о выбранном объекте
        if (selectedObject != null) {
            DebugInfoRenderer.renderDebugInfo(g, selectedObject, getWorld(), yPos);
        }
    }
}
