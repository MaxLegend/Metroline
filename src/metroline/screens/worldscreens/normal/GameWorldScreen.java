package metroline.screens.worldscreens.normal;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.core.world.tiles.GameTile;
import metroline.input.KeyboardController;
import metroline.input.selection.Selectable;
import metroline.input.selection.SelectionManager;
import metroline.objects.gameobjects.StationLabel;
import metroline.objects.gameobjects.*;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.render.StationPositionCache;
import metroline.screens.render.StationRender;
import metroline.screens.worldscreens.CachedWorldScreen;




import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.TimerTask;


//TODO доделать плавный перемещатор с учетом зума (ща без него)
public class GameWorldScreen extends CachedWorldScreen {
    private static final int FPS_UPDATE_INTERVAL = 1000;
    private static final int TARGET_FPS = 500;
    private static final int RENDER_DELAY_MS = 1000 / TARGET_FPS; // ~7ms для 144 FPS

    public static GameWorldScreen INSTANCE;
    public GameClickController worldClickController;

    //    public final List<InfoWindow> infoWindows = new ArrayList<>();
    private java.util.Timer gameTimer;
    private TimerTask renderTask;

    // Performance tracking
    private int fps;
    private long lastFpsTime;
    private int frameCount;
    private long totalRenderTime;
    private int renderCount;
    private int worldUpdates;
    private long lastWorldUpdateTime;

    private long lastUpdateTime;
    private double deltaTime;


    public long lastUpdateTime2 = System.nanoTime();
    public GameWorldScreen(MainFrame parent) {
        super(parent, new GameWorld());
        INSTANCE = this;

        this.worldClickController = new GameClickController(this);
       // setupRepaintTimer();
        setupGameTimer();
        lastFpsTime = System.currentTimeMillis();
        lastWorldUpdateTime = lastFpsTime;
        lastUpdateTime = System.nanoTime();

        parent.updateLanguage();
    }


    public void handleWorldClick(int x, int y) {
        if (x < 0 || x >= getWorld().getWidth() || y < 0 || y >= getWorld().getHeight()) {
            return;
        }
        worldClickController.mainClickHandler(x, y);
        repaint();
    }

    public void createNewWorld(short width, short height,  int worldColor) {

        setupGameTimer();
        widthWorld = width;
        heightWorld = height;
        setWorld(new GameWorld(width, height, worldColor));

        worldClickController = new GameClickController(this);
        setupGameTimer();
        invalidateCache();
        repaint();

    }

    private void setupGameTimer() {
        stopGameTimer();

        gameTimer = new java.util.Timer("GameLoopTimer", true);
        renderTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (GameWorldScreen.this) {
                    long currentTime = System.nanoTime();
                    deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0; // в секундах
                    lastUpdateTime = currentTime;

                    // Обновление игровой логики
                    updateGameLogic(deltaTime);

                    // Отрисовка в EDT
                    SwingUtilities.invokeLater(() -> {
                        repaint();
                    });
                }
            }
        };

        // Запускаем с фиксированной частотой
        gameTimer.scheduleAtFixedRate(renderTask, 0, RENDER_DELAY_MS);
    }
    private void updateGameLogic(double deltaTime) {

        List<Station> stationsToCheck;
        List<Tunnel> tunnelsToCheck;

        synchronized (this) {
            stationsToCheck = new ArrayList<>(((GameWorld)getWorld()).getStations());
            tunnelsToCheck = new ArrayList<>(((GameWorld)getWorld()).getTunnels());
        }


        updatePanByArrowKeys();

        StationPositionCache.cleanupCache();
        worldUpdates++;
    }

    public void stopGameTimer() {
        if (renderTask != null) {
            renderTask.cancel();
        }
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer.purge();
        }
    }
    /**
     * Обновляет панорамирование на основе зажатых клавиш-стрелок.
     * Вызывать каждый кадр в игровом цикле.
     */
    public void updatePanByArrowKeys() {
        if (!(this instanceof GameWorldScreen)) return;

        KeyboardController kb = KeyboardController.getInstance();
        if (kb == null) return;
        kb.updatePanArroyKeys(this);
    }




    public static synchronized GameWorldScreen getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("GameWorldScreen not initialized");
        }
        return INSTANCE;
    }

    @Override
    protected void paintComponent(Graphics gr) {

        long renderStartTime = System.nanoTime();
        super.paintComponent(gr);
        Graphics2D g = (Graphics2D)gr;

        updatePerformanceCounters();

        AffineTransform oldTransform = g.getTransform();
        g.scale(zoom, zoom);
        g.translate(offsetX, offsetY);


        renderWorld(g);
        drawAnimatedWater(g);

        drawDynamicWorld(g);
        drawSelections(g);
        drawLabels(g);

        g.setTransform(oldTransform);

        // Draw debug info if enabled
        if (debugMode) {
            drawDebugInfo(g);
        }

        updateRenderStats(renderStartTime);

    }


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
    public void setLegendWindow(LinesLegendWindow legendWindow) {
        this.legendWindow = legendWindow;
        if (getWorld() instanceof GameWorld) {
            ((GameWorld) getWorld()).setLegendWindow(legendWindow);
        }
    }
    public ArrayList<Station> getAllStationsSorted() {
        ArrayList<Station> stations = new ArrayList<>(getWorld().getStations());
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
            if (selectedObject instanceof Station || selectedObject instanceof Tunnel || selectedObject instanceof StationLabel) {
                gridType = "gameGrid (Stations/Tunnels/Labels)";
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

            }
            else if (selectedObject instanceof Tunnel) {
                Tunnel tunnel = (Tunnel) selectedObject;
                details.add(String.format("Length: %d", tunnel.getLength()));
                details.add(String.format("Type: %s", tunnel.getType()));
                details.add(String.format("Start: %s", tunnel.getStart().getName()));
                details.add(String.format("End: %s", tunnel.getEnd().getName()));
            }

            else if (selectedObject instanceof StationLabel) {
                StationLabel stationLabel = (StationLabel) selectedObject;
                details.add(String.format("Text: %s", stationLabel.getText()));
                details.add(String.format("Parent: %s",
                        stationLabel.getParentGameObject() != null ?
                                stationLabel.getParentGameObject().getClass().getSimpleName() : "None"));
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
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();
        if(selected instanceof GameObject) {
            drawDebugInfo(g, (GameObject) selected);
        }

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
    Color oldColor = g.getColor();
    Font oldFont = g.getFont();

    g.setColor(new Color(255, 255, 255, 200));
    g.setFont(new Font("Monospaced", Font.BOLD, 12));
    FontMetrics metrics = g.getFontMetrics();

    Runtime runtime = Runtime.getRuntime();
    long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    long totalMem = runtime.totalMemory() / (1024 * 1024);
    long avgRenderTime = renderCount > 0 ? (totalRenderTime / renderCount) / 1000 : 0;

    int gameGridObjects = countObjectsInGrid(getWorld().getGameGrid());

    String[] stats = {
            "=== PERFORMANCE STATS ===",
            String.format("FPS: %d (Target: %d)", fps, TARGET_FPS),
            String.format("Render: %d μs (avg)", avgRenderTime),
            String.format("World updates: %d/s", worldUpdates),
            String.format("Delta time: %.4f", deltaTime),
            "",
            "=== MEMORY USAGE ===",
            String.format("Used: %d MB / %d MB", usedMem, totalMem),
            String.format("Max: %d MB", runtime.maxMemory() / (1024 * 1024)),
            "",
            "=== WORLD STATS ===",
            String.format("World size: %dx%d", getWorld().getWidth(), getWorld().getHeight()),
            String.format("gameGrid objects: %d", gameGridObjects),
            String.format("Stations: %d", getWorld().getStations().size()),
            String.format("Tunnels: %d", getWorld().getTunnels().size()),
            String.format("Labels: %d", getWorld().getLabels().size()),

            String.format("Zoom: %.2f", zoom)
    };

    int textHeight = metrics.getHeight() * stats.length;
    int textWidth = 0;
    for (String line : stats) {
        textWidth = Math.max(textWidth, metrics.stringWidth(line));
    }

    g.setColor(new Color(0, 0, 0, 150));
    g.fillRect(10, 10, textWidth + 20, textHeight + 20);

    g.setColor(Color.WHITE);
    int yPos = 30;
    for (String line : stats) {
        g.drawString(line, 20, yPos);
        yPos += metrics.getHeight();
    }

    g.setColor(oldColor);
    g.setFont(oldFont);
}

    public void toggleDebugMode() {
        debugMode = !debugMode;
        repaint();
    }

    @Override
    public void onActivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(this);
        requestFocusInWindow();

        super.onActivate();

    }

    @Override
    public void onDeactivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(null);
        super.onDeactivate();
    }
}
