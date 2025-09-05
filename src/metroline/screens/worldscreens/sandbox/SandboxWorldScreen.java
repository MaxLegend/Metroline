package metroline.screens.worldscreens.sandbox;


import metroline.MainFrame;
import metroline.core.world.SandboxWorld;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.gameobjects.StationLabel;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.screens.render.StationRender;
import metroline.screens.worldscreens.CachedWorldScreen;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;



/**
 * FIX Привести класс в соответствие с GameWorld - сейчас краш из за меток
 * World screen that displays and interacts with the game world
 */
public class SandboxWorldScreen extends CachedWorldScreen {

    // Performance tracking
    private int fps;
    private long lastFpsTime;
    private int frameCount;
    private long totalRenderTime;
    private int renderCount;
    private int worldUpdates;
    private long lastWorldUpdateTime;

    private static final int FPS_UPDATE_INTERVAL = 1000;
    private static final int RENDER_TIMER_DELAY = 16; // ~60 FPS

    public static SandboxWorldScreen INSTANCE;
    public SandboxClickHandler sandboxClickHandler;
    private boolean cacheValid = false; // Флаг валидности кеша

    public SandboxWorldScreen(MainFrame parent) {
        super(parent, new SandboxWorld(widthWorld, heightWorld, 0xFFFFFF));
        INSTANCE = this;
        this.sandboxClickHandler = new SandboxClickHandler(this);

    }


    public void createNewWorld(short width, short height, int worldColor) {
        widthWorld = width;
        heightWorld = height;

        this.setWorld(new SandboxWorld(width, height, worldColor));
        invalidateCache();

        repaint();

    }


    /**
     * Handles mouse click on the world
     * @param x X coordinate in world space
     * @param y Y coordinate in world space
     */
//    public void handleClick(int x, int y) {
//        if (x < 0 || x >= getWorld().getWidth() || y < 0 || y >= getWorld().getHeight()) {
//            return;
//        }
//        if (isShiftPressed && isCPressed) {
//            sandboxClickHandler.showColorSelectionPopup(x, y);
//        }
//        else if (isShiftPressed) {
//            sandboxClickHandler.handleStationClick(x, y);
//        }
//        else if (isCtrlPressed) {
//
//            sandboxClickHandler.handleTunnelClick(x, y);
//        }
//        else {
//            sandboxClickHandler.handleDefaultClick(x, y);
//        }
//
//        repaint();
//    }
    public void handleWorldClick(int x, int y) {
        if (x < 0 || x >= getWorld().getWidth() || y < 0 || y >= getWorld().getHeight()) {
            return;
        }
        sandboxClickHandler.mainClickHandler(x, y);
        repaint();
    }
    public static synchronized SandboxWorldScreen getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SandboxWorldScreen not initialized");
        }
        return INSTANCE;
    }
    public void close() {
        // Останавливаем все таймеры, очищаем ресурсы
        if (sandboxClickHandler != null) {
       //     sandboxClickHandler.cleanup();
            sandboxClickHandler = null;
        }
        invalidateCache();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;



        // Применяем трансформации
        AffineTransform oldTransform = g2d.getTransform();
        g2d.scale(zoom, zoom);
        g2d.translate(offsetX, offsetY);

        renderWorld(g2d);

        for (Tunnel tunnel : getWorld().getTunnels()) {
            tunnel.draw(g2d, 0, 0, 1);
        }


        if(getWorld().isRoundStationsEnabled()) {
            for (Station station : getWorld().getStations()) {
                StationRender.drawWorldColorRing(station,g2d, 0, 0, 1);
            }
            // 1. Сначала рисуем все соединения всех станций
            for (Station station : getWorld().getStations()) {
                StationRender.drawRoundTransfer(station, g2d, 0, 0, 1);
            }

            // 2. Затем рисуем все станции
            for (Station station : getAllStationsSorted()) {
                StationRender.drawRoundStation(station, g2d, 0, 0, 1);
            }

            // 3. В конце рисуем выделения
            for (Station station : getWorld().getStations()) {
                if (station.isSelected()) {
                    StationRender.drawRoundSelection(station, g2d, 0, 0, 1);
                }
            }
        } else {

            for (Station station : getWorld().getStations()) {
                StationRender.drawWorldColorSquare(station, g2d, 0, 0, 1);
            }

            for (Station station : getWorld().getStations()) {
                StationRender.drawRoundTransfer(station, g2d, 0, 0, 1);
            }

            // 2. Затем рисуем все станции
            for (Station station : getAllStationsSorted()) {
                StationRender.drawSquareStation(station, g2d, 0, 0, 1);
            }

            // 3. В конце рисуем выделения
            for (Station station : getWorld().getStations()) {
                if (station.isSelected()) {
                    StationRender.drawSquareSelection(station, g2d, 0, 0, 1);
                }
            }
        }

        for (StationLabel stationLabel : getWorld().getLabels()) {
            stationLabel.draw(g2d, 0, 0, 1);
        }

        // Восстанавливаем трансформации
        g2d.setTransform(oldTransform);

        // Рисуем debug-информацию
        if (debugMode) {
              drawPerformanceStats(g2d);
        }
    }
    public java.util.List<Station> getAllStationsSorted() {
        // Сортируем станции по координатам, чтобы избежать перекрытий
        List<Station> stations = new ArrayList<>(getWorld().getStations());
        stations.sort((a, b) -> {
            if (a.getY() != b.getY()) return Integer.compare(a.getY(), b.getY());
            return Integer.compare(a.getX(), b.getX());
        });
        return stations;
    }

    public void drawStaticWorld(Graphics2D g) {
        // Рисуем сетку
        AffineTransform originalTransform = g.getTransform();

            for (int y = 0; y < getWorld().getHeight(); y++) {
                for (int x = 0; x < getWorld().getWidth(); x++) {
                    WorldTile tile = getWorld().getWorldTile(x, y);
                    tile.draw(g, 0, 0, 1);
            //        getWorld().getWorldGrid()[x*y].draw(g, 0, 0, 1);
                }
            }

        g.setTransform(originalTransform);
    }

    private void drawPerformanceStats(Graphics2D g) {
        // Save original settings
        Color oldColor = g.getColor();
        Font oldFont = g.getFont();

        // Setup debug font
        g.setColor(new Color(255, 255, 255, 200));
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        FontMetrics metrics = g.getFontMetrics();

        // Prepare performance data
        Runtime runtime = Runtime.getRuntime();
        long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long avgRenderTime = renderCount > 0 ? (totalRenderTime / renderCount) / 1000 : 0;


        BufferCapabilities caps = getGraphicsConfiguration().getBufferCapabilities();

        String[] stats = {
                "=== PERFORMANCE STATS ===",
                String.format("FPS: %d (Target: %.1f)", fps, 1000.0/RENDER_TIMER_DELAY),
                String.format("Render: %d μs (avg)", avgRenderTime),
                String.format("World updates: %d/s", worldUpdates),
                "",
                "=== MEMORY USAGE ===",
                String.format("Used: %d MB / %d MB", usedMem, totalMem),
                String.format("Max: %d MB", runtime.maxMemory() / (1024 * 1024)),
                "",
                "=== WORLD STATS ===",
                String.format("World size: %dx%d", getWorld().getWidth(), getWorld().getHeight()),
                String.format("Stations: %d", getWorld().getStations().size()),
                String.format("Tunnels: %d", getWorld().getTunnels().size()),
                String.format("Labels: %d", getWorld().getLabels().size()),
                String.format("Zoom: %.2f", zoom),
                String.format("VSync: %b", caps.isPageFlipping())
        };

        // Calculate background size
        int textHeight = metrics.getHeight() * stats.length;
        int textWidth = 0;
        for (String line : stats) {
            textWidth = Math.max(textWidth, metrics.stringWidth(line));
        }
        //   g.drawString(String.format("VSync: %b", caps.isPageFlipping()), 20, 50);
        // Draw background
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(10, 10, textWidth + 20, textHeight + 20);

        // Draw text
        g.setColor(Color.WHITE);
        int yPos = 30;
        for (String line : stats) {
            g.drawString(line, 20, yPos);
            yPos += metrics.getHeight();
        }

        // Restore original settings
        g.setColor(oldColor);
        g.setFont(oldFont);
    }



}