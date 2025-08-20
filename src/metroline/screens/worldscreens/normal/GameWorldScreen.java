package metroline.screens.worldscreens.normal;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.core.world.tiles.GameTile;
import metroline.objects.gameobjects.Label;
import metroline.objects.gameobjects.*;
import metroline.screens.GameScreen;
import metroline.screens.panel.InfoWindow;
import metroline.screens.render.StationRender;
import metroline.screens.worldscreens.CachedWorldScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameWorldScreen extends CachedWorldScreen {
    private static final int FPS_UPDATE_INTERVAL = 1000;
    private static final int RENDER_TIMER_DELAY = 16; // ~60 FPS

    public static GameWorldScreen INSTANCE;
    public WorldClickController worldClickController;

    public final List<InfoWindow> infoWindows = new ArrayList<>();
    private Timer repaintTimer;

    // Performance tracking
    private int fps;
    private long lastFpsTime;
    private int frameCount;
    private long totalRenderTime;
    private int renderCount;
    private int worldUpdates;
    private long lastWorldUpdateTime;

    // Debug
    public boolean debugMode = false;

    public GameWorldScreen(MainFrame parent) {
        super(parent, new GameWorld());
        INSTANCE = this;

        this.worldClickController = new WorldClickController(this);
     //   this.updateStaticWorldCache(widthWorld,heightWorld);
        setupRepaintTimer();

        lastFpsTime = System.currentTimeMillis();
        lastWorldUpdateTime = lastFpsTime;
    }


    public void handleWorldClick(int x, int y) {
        if (x < 0 || x >= getWorld().getWidth() || y < 0 || y >= getWorld().getHeight()) {
            return;
        }
        worldClickController.mainClickHandler(x, y);
        repaint();
    }

    public void createNewWorld(short width, short height, boolean hasPassengerCount,
            boolean hasAbilityPay, boolean hasLandscape,
            boolean hasRivers, int worldColor, int money) {
        stopRepaintTimer();

        widthWorld = width;
        heightWorld = height;
        setWorld(new GameWorld(width, height, hasPassengerCount, hasAbilityPay,
                hasLandscape, hasRivers, worldColor, money));

        worldClickController = new WorldClickController(this);
        ((GameWorld)getWorld()).generateRandomGameplayUnits((int)GameConstants.GAMEPLAY_UNITS_COUNT);

        invalidateCache();
        setupRepaintTimer();
        repaint();

    }

    private void setupRepaintTimer() {
        if (repaintTimer != null && repaintTimer.isRunning()) {
            repaintTimer.stop();
        }

        repaintTimer = new Timer(RENDER_TIMER_DELAY, e -> {
            worldClickController.checkConstructionProgress();
            infoWindows.forEach(InfoWindow::updateInfo);
            repaint();
        });
        repaintTimer.start();
    }

    public void stopRepaintTimer() {
        if (repaintTimer != null) {
            repaintTimer.stop();
        }
    }

//    public void close() {
//        stopRepaintTimer();
//        new ArrayList<>(infoWindows).forEach(InfoWindow::dispose);
//        infoWindows.clear();
//
//        // Очищаем контроллеры
//        if (worldClickController != null) {
//       //     worldClickController.cleanup();
//            worldClickController = null;
//        }
//
//        // Очищаем мир
//        if (getWorld() != null) {
//            //      getWorld().cleanup();
//            setWorld(null); // ← Важно!
//        }
//
//        invalidateCache();
//        new ArrayList<>(infoWindows).forEach(InfoWindow::dispose);
//        infoWindows.clear();
//    }

    public void updateMoneyDisplay() {
        if (getWorld() instanceof GameWorld) {
            float money = ((GameWorld)getWorld()).getMoney();
            parent.moneyLabel.setText(String.format("%.2f M", money));
        }
    }

    public void addMoney(int amount) {
        if (getWorld() instanceof GameWorld) {
            ((GameWorld)getWorld()).addMoney(amount);
            updateMoneyDisplay();
        }
    }

    public static synchronized GameWorldScreen getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("GameWorldScreen not initialized");
        }
        return INSTANCE;
    }

    @Override
    protected void paintComponent(Graphics gr) {
        GameScreen currentScreen = MainFrame.getInstance().getCurrentScreen();

        long renderStartTime = System.nanoTime();
        super.paintComponent(gr);
        Graphics2D g = (Graphics2D)gr;

        updatePerformanceCounters();

        AffineTransform oldTransform = g.getTransform();
        g.scale(zoom, zoom);
        g.translate(offsetX, offsetY);

        renderWorld(g);
      //  drawStaticWorld(g);
        drawDynamicWorld(g);

        // Рисуем выделения поверх всего
        drawSelections(g);

        // Restore transformations
        g.setTransform(oldTransform);

        // Draw debug info if enabled
        if (debugMode) {
            drawDebugInfo(g);
        }

        updateRenderStats(renderStartTime);
    }

//    @Override
//    protected void drawDynamicWorld(Graphics2D g) {
//        AffineTransform originalTransform = g.getTransform();
//
//        // Туннели
//        for (Tunnel tunnel : getWorld().getTunnels()) {
//            tunnel.draw(g, 0, 0, 1);
//        }
//
//        // Станции
//        if(getWorld().isRoundStationsEnabled()) {
//            // Сначала рисуем цветные кольца и переходы
//            for (Station station : getWorld().getStations()) {
//                StationRender.drawWorldColorRing(station, g, 0, 0, 1);
//                StationRender.drawRoundTransfer(station, g, 0, 0, 1);
//            }
//            // Затем сами станции в правильном порядке
//            for (Station station : getAllStationsSorted()) {
//                StationRender.drawRoundStation(station, g, 0, 0, 1);
//            }
//        } else {
//            // Квадратные станции
//            for (Station station : getWorld().getStations()) {
//                StationRender.drawWorldColorSquare(station, g, 0, 0, 1);
//                StationRender.drawRoundTransfer(station, g, 0, 0, 1);
//            }
//            for (Station station : getAllStationsSorted()) {
//                StationRender.drawSquareStation(station, g, 0, 0, 1);
//            }
//        }
//
//        // Игровые объекты
//        if (getWorld() instanceof GameWorld) {
//            for (GameplayUnits unit : ((GameWorld)getWorld()).getGameplayUnits()) {
//                unit.draw(g, 0, 0, 1);
//            }
//        }
//
//        // Метки
//        for (Label label : getWorld().getLabels()) {
//            label.draw(g, 0, 0, 1);
//        }
//
//        g.setTransform(originalTransform);
//    }

    private void drawSelections(Graphics2D g) {
        if(getWorld().isRoundStationsEnabled()) {
            for (Station station : getWorld().getStations()) {
                if (station.isSelected()) {
                    StationRender.drawRoundSelection(station, g, 0, 0, 1);
                }
            }
        } else {
            for (Station station : getWorld().getStations()) {
                if (station.isSelected()) {
                    StationRender.drawSquareSelection(station, g, 0, 0, 1);
                }
            }
        }
    }

    public List<Station> getAllStationsSorted() {
        List<Station> stations = new ArrayList<>(getWorld().getStations());
        stations.sort((a, b) -> {
            int yCompare = Integer.compare(a.getY(), b.getY());
            return yCompare != 0 ? yCompare : Integer.compare(a.getX(), b.getX());
        });
        return stations;
    }

    private void drawDebugInfo(Graphics2D g, GameObject selectedObject) {
        if (selectedObject != null) {
            // Save original settings
            Color oldColor = g.getColor();
            Font oldFont = g.getFont();

            // Setup debug font
            g.setColor(new Color(255, 255, 255, 200));
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            FontMetrics metrics = g.getFontMetrics();

            // Determine which grid the object is on
            String gridType = "Unknown";
            if (selectedObject instanceof Station || selectedObject instanceof Tunnel || selectedObject instanceof Label) {
                gridType = "gameGrid (Stations/Tunnels/Labels)";
            } else if (selectedObject instanceof GameplayUnits) {
                gridType = "gameplayGrid (GameplayUnits)";
            }

            // Get object position
            int x = selectedObject.getX();
            int y = selectedObject.getY();

            // Prepare debug info
            String[] debugInfo = {
                    "=== SELECTED OBJECT DEBUG ===",
                    String.format("Type: %s", selectedObject.getClass().getSimpleName()),
                    String.format("Position: %d, %d", x, y),
                    String.format("Grid: %s", gridType),
                    "",
                    "=== OBJECT DETAILS ==="
            };

            // Add object-specific details
            List<String> details = new ArrayList<>(Arrays.asList(debugInfo));

            if (selectedObject instanceof Station) {
                Station station = (Station) selectedObject;
                details.add(String.format("Name: %s", station.getName()));
                details.add(String.format("Type: %s", station.getType()));
                details.add(String.format("Connections: %d", station.getConnections().size()));
                details.add(String.format("Wear level: %.2f", station.getWearLevel()));
            }
            else if (selectedObject instanceof Tunnel) {
                Tunnel tunnel = (Tunnel) selectedObject;
                details.add(String.format("Length: %d", tunnel.getLength()));
                details.add(String.format("Type: %s", tunnel.getType()));
                details.add(String.format("Start: %s", tunnel.getStart().getName()));
                details.add(String.format("End: %s", tunnel.getEnd().getName()));
            }
            else if (selectedObject instanceof GameplayUnits) {
                GameplayUnits unit = (GameplayUnits) selectedObject;
                details.add(String.format("Unit Type: %s", unit.getType()));
                details.add(String.format("Income Multiplier: %.2f", unit.getType().getIncomeMultiplier()));
            }
            else if (selectedObject instanceof Label) {
                Label label = (Label) selectedObject;
                details.add(String.format("Text: %s", label.getText()));
                details.add(String.format("Parent: %s",
                        label.getParentGameObject() != null ?
                                label.getParentGameObject().getClass().getSimpleName() : "None"));
            }

            // Calculate background size
            int textHeight = metrics.getHeight() * details.size();
            int textWidth = 0;
            for (String line : details) {
                textWidth = Math.max(textWidth, metrics.stringWidth(line));
            }

            // Position at bottom right
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int xPos = panelWidth - textWidth - 30;
            int yPos = panelHeight - textHeight - 30;

            // Draw background
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(xPos - 10, yPos - 20, textWidth + 20, textHeight + 30);

            // Draw text
            g.setColor(Color.WHITE);
            int currentY = yPos;
            for (String line : details) {
                g.drawString(line, xPos, currentY);
                currentY += metrics.getHeight();
            }



            // Restore original settings
            g.setColor(oldColor);
            g.setFont(oldFont);
        }
    }

    private void drawDebugInfo(Graphics2D g) {
        drawDebugInfo(g, worldClickController.getSelectedObject());


        drawPerformanceStats(g);


    }

    private void updatePerformanceCounters() {
        frameCount++;
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastFpsTime >= FPS_UPDATE_INTERVAL) {
            fps = frameCount;
            frameCount = 0;
            lastFpsTime = currentTime;

            if (currentTime - lastWorldUpdateTime >= FPS_UPDATE_INTERVAL) {
                worldUpdates = 0;
                lastWorldUpdateTime = currentTime;
                totalRenderTime = 0;
                renderCount = 0;
            }
        }
    }

    private void updateRenderStats(long renderStartTime) {
        long renderDuration = System.nanoTime() - renderStartTime;
        totalRenderTime += renderDuration;
        renderCount++;
    }

    // Вспомогательный метод для подсчета объектов в сетке
    private int countObjectsInGrid(GameTile[] grid) {
        if (grid == null) return 0;

        int count = 0;
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != null && grid[i].getContent() != null) {
                count++;
            }
        }
        return count;
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

        // Подсчитываем объекты в сетках
        int gameGridObjects = countObjectsInGrid(getWorld().getGameGrid());
        int gameplayGridObjects = countObjectsInGrid(getWorld().getGameplayGrid());
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
                String.format("gameGrid objects: %d", gameGridObjects),
                String.format("gameplayGrid objects: %d", gameplayGridObjects),
                String.format("Stations: %d", getWorld().getStations().size()),
                String.format("Tunnels: %d", getWorld().getTunnels().size()),
                String.format("Labels: %d", getWorld().getLabels().size()),
                String.format("Units: %d", getWorld() instanceof GameWorld ?
                        ((GameWorld)getWorld()).getGameplayUnits().size() : 0),
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

    public void toggleDebugMode() {
        debugMode = !debugMode;
        repaint();
    }
}
