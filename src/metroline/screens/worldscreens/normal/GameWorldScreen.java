package metroline.screens.worldscreens.normal;

import metroline.core.world.GameWorld;
import metroline.objects.gameobjects.*;
import metroline.MainFrame;
import metroline.objects.gameobjects.Label;
import metroline.screens.panel.InfoWindow;
import metroline.screens.render.StationRender;
import metroline.screens.worldscreens.CachedWorldScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class GameWorldScreen extends CachedWorldScreen {
    public static GameWorldScreen INSTANCE;
    public WorldClickController worldClickController;

    public final List<InfoWindow> infoWindows = new ArrayList<>();
    private Timer repaintTimer;
    //Debug
    public boolean debugMode = false;
    //  private BufferedImage worldCache; // Кешированное изображение мира
    private boolean cacheValid = false; // Флаг валидности кеша



    private int fps;
    private long lastFpsTime;
    private int frameCount;
    private long lastRenderTime;
    private int worldUpdates;
    private long lastWorldUpdateTime;
    private long totalRenderTime;
    private int renderCount;

    public GameWorldScreen(MainFrame parent) {
        super(parent,  new GameWorld());
        INSTANCE = this;

        this.worldClickController = new WorldClickController( this);
        initWorldCache(widthWorld, heightWorld);
        setupRepaintTimer();

        // Инициализация таймеров статистики
        lastFpsTime = System.currentTimeMillis();
        lastWorldUpdateTime = System.currentTimeMillis();
    }

    /**
     * Handles mouse click on the world
     * @param x X coordinate in world space
     * @param y Y coordinate in world space
     */
    public void handleWorldClick(int x, int y) {
        if (x < 0 || x >= getWorld().getWidth() || y < 0 || y >= getWorld().getHeight()) {
            return;
        }
        worldClickController.mainClickHandler(x,y);
        repaint();
    }


    public void createNewWorld(int width, int height, boolean hasPassengerCount, boolean hasAbilityPay, boolean hasLandscape, boolean hasRivers, Color worldColor, int money) {
        worldUpdates++;
        stopRepaintTimer();
        widthWorld = width;
        heightWorld = height;
        this.setWorld(new GameWorld(width, height,hasPassengerCount, hasAbilityPay, hasLandscape, hasRivers,worldColor, money));
        this.worldClickController = new WorldClickController( this);
        setupRepaintTimer();
        ((GameWorld)getWorld()).generateRandomGameplayUnits((int) GameConstants.GAMEPLAY_UNITS_COUNT);
        invalidateCache();
        repaint();
    }

    private void setupRepaintTimer() {
        repaintTimer = new Timer(16, e -> {
            worldClickController.checkConstructionProgress();
            // Обновляем все открытые информационные окна
            for (InfoWindow window : infoWindows) {
                window.updateInfo();
            }
            repaint();
        });
        repaintTimer.start();
    }
    public void stopRepaintTimer() {
        if (repaintTimer != null) {
            repaintTimer.stop();
        }
    }
    public void close() {
        stopRepaintTimer();
        // Закрываем все информационные окна
        for (InfoWindow window : new ArrayList<>(infoWindows)) {
            window.dispose();
        }
        infoWindows.clear();
    }
    /**
     *
     * MONEY MONEY MONEY!
     */
    public void updateMoneyDisplay() {
        if(getWorld() instanceof GameWorld) {
            float money = ((GameWorld) getWorld()).getMoney();
            String formatted = String.format("%.2f M", money);
            parent.moneyLabel.setText(formatted);
        }
    }

    public void addMoney(int amount) {
        if(getWorld() instanceof GameWorld) {
            ((GameWorld) getWorld()).addMoney(amount);
            updateMoneyDisplay();
        }
    }


    public static synchronized GameWorldScreen getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("GameWorldScreen not initialized");
        }
        return INSTANCE;
    }
//        private void initWorldCache() {
//            int cacheWidth = widthWorld * 32;
//            int cacheHeight = heightWorld * 32;
//            worldCache = new BufferedImage(cacheWidth, cacheHeight, BufferedImage.TYPE_INT_ARGB);
//        }
        public void invalidateCache() {
            worldUpdates++;
            cacheValid = false;
        }
        @Override
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
//        @Override
//        protected void paintComponent(Graphics gr) {
//            long renderStartTime = System.nanoTime();
//
//            super.paintComponent(gr);
//            Graphics2D g = (Graphics2D)gr;
//
//            updateFpsCounter();
//
//            worldClickController.checkConstructionProgress();
//
//
//            if (!cacheValid) {
//                updateWorldCache(widthWorld, heightWorld, this::drawStaticWorld);
//            }
//
//            // Применяем трансформации
//            AffineTransform oldTransform = g.getTransform();
//            g.scale(zoom, zoom);
//            g.translate(offsetX, offsetY);
//
//            // Рисуем кешированный мир
//            g.drawImage(worldCache, 0, 0, null);
//
//
//            for (Tunnel tunnel : getWorld().getTunnels()) {
//                tunnel.draw(g, 0, 0, 1);
//            }
//
//            if(getWorld().isRoundStationsEnabled()) {
//                for (Station station : getWorld().getStations()) {
//                    StationRender.drawWorldColorRing(station,g, 0, 0, 1);
//                }
//                // 1. Сначала рисуем все соединения всех станций
//                for (Station station : getWorld().getStations()) {
//                    StationRender.drawRoundTransfer(station, g, 0, 0, 1);
//                }
//
//                // 2. Затем рисуем все станции
//                for (Station station : getAllStationsSorted()) {
//                    StationRender.drawRoundStation(station, g, 0, 0, 1);
//                }
//
//                // 3. В конце рисуем выделения
//                for (Station station : getWorld().getStations()) {
//                    if (station.isSelected()) {
//                        StationRender.drawRoundSelection(station, g, 0, 0, 1);
//                    }
//                }
//            } else {
//
//                for (Station station : getWorld().getStations()) {
//                    StationRender.drawWorldColorSquare(station, g, 0, 0, 1);
//                }
//
//                for (Station station : getWorld().getStations()) {
//                    StationRender.drawRoundTransfer(station, g, 0, 0, 1);
//                }
//
//                // 2. Затем рисуем все станции
//                for (Station station : getAllStationsSorted()) {
//                    StationRender.drawSquareStation(station, g, 0, 0, 1);
//                }
//
//                // 3. В конце рисуем выделения
//                for (Station station : getWorld().getStations()) {
//                    if (station.isSelected()) {
//                        StationRender.drawSquareSelection(station, g, 0, 0, 1);
//                    }
//                }
//            }
//            for (GameplayUnits gUnits : ((GameWorld)getWorld()).getGameplayUnits()) {
//                gUnits.draw(g, 0, 0, 1);
//            }
//
//            for (Label label : getWorld().getLabels()) {
//                label.draw(g, 0, 0, 1);
//            }
//
//            // Восстанавливаем трансформации
//            g.setTransform(oldTransform);
//
//            // Рисуем debug-информацию
//            if (debugMode) {
//                drawDebugInfo(g, worldClickController.getSelectedObject());
//            }
//            BufferCapabilities caps = getGraphicsConfiguration().getBufferCapabilities();
//            g.drawString(String.format("VSync: %b", caps.isPageFlipping()), 20, 50);
//            drawPerformanceStats(g);
//        }
@Override
protected void paintComponent(Graphics gr) {
    long renderStartTime = System.nanoTime();
    super.paintComponent(gr);
    Graphics2D g = (Graphics2D)gr;

    updateFpsCounter();

    // Обновляем кэши только при необходимости
    if (!staticCacheValid || worldChanged) {
        updateStaticWorldCache(widthWorld, heightWorld);
    }

    if (!dynamicCacheValid || stationsChanged || tunnelsChanged) {
        updateDynamicElementsCache(widthWorld, heightWorld);
    }

    // Применяем трансформации
    AffineTransform oldTransform = g.getTransform();
    g.scale(zoom, zoom);
    g.translate(offsetX, offsetY);

    // Рисуем статичный мир из кэша
    g.drawImage(staticWorldCache, 0, 0, null);

    // Рисуем динамические элементы из кэша
 //   g.drawImage(dynamicElementsCache, 0, 0, null);
    drawDynamicWorld(g);
    // Рисуем выделения (они всегда динамические)
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
            BufferCapabilities caps = getGraphicsConfiguration().getBufferCapabilities();
           g.drawString(String.format("VSync: %b", caps.isPageFlipping()), 20, 50);
          drawPerformanceStats(g);
    // Восстанавливаем трансформации
    g.setTransform(oldTransform);

    // Отрисовка статистики и debug-информации
    if (debugMode) {
        drawDebugInfo(g, worldClickController.getSelectedObject());
        drawPerformanceStats(g);
    }

    // Обновляем статистику рендеринга
    long renderDuration = System.nanoTime() - renderStartTime;
    totalRenderTime += renderDuration;
    renderCount++;
}
    // Новые методы для сбора и отображения статистики
    private void updateFpsCounter() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsTime >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsTime = currentTime;
        }
    }

    private void drawPerformanceStats(Graphics2D g) {
        // Сохраняем текущие настройки
        Color oldColor = g.getColor();
        Font oldFont = g.getFont();
        FontMetrics metrics = g.getFontMetrics();

        // Устанавливаем стиль для отладочной информации
        g.setColor(new Color(255, 255, 255, 200));
        g.setFont(new Font("Monospaced", Font.BOLD, 12));

        // Получаем статистику памяти
        Runtime runtime = Runtime.getRuntime();
        long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long maxMem = runtime.maxMemory() / (1024 * 1024);

        // Рассчитываем среднее время рендеринга
        long avgRenderTime = renderCount > 0 ? (totalRenderTime / renderCount) / 1000 : 0;

        // Формируем строки статистики
        String[] stats = {
                "=== PERFORMANCE STATS ===",
                String.format("FPS: %d (Target: %.1f)", fps, 1000.0/repaintTimer.getDelay()),
                String.format("Render: %d μs (avg)", avgRenderTime),
                String.format("World updates: %d/s", worldUpdates),
                "",
                "=== MEMORY USAGE ===",
                String.format("Used: %d MB / %d MB", usedMem, totalMem),
                String.format("Max: %d MB", maxMem),
                "",
                "=== RENDER STATS ===",
                String.format("Stations: %d", getWorld().getStations().size()),
                String.format("Tunnels: %d", getWorld().getTunnels().size()),
                String.format("Labels: %d", getWorld().getLabels().size()),
                String.format("Units: %d", ((GameWorld)getWorld()).getGameplayUnits().size()),
                String.format("Cache: %s", cacheValid ? "VALID" : "INVALID"),
                String.format("Zoom: %.2f", zoom)
        };

        // Рисуем фон для текста
        int textHeight = metrics.getHeight() * stats.length;
        int textWidth = 0;
        for (String line : stats) {
            textWidth = Math.max(textWidth, metrics.stringWidth(line));
        }

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(10, 10, textWidth + 20, textHeight + 20);

        // Рисуем текст
        g.setColor(Color.WHITE);
        int yPos = 30;
        for (String line : stats) {
            g.drawString(line, 20, yPos);
            yPos += metrics.getHeight();
        }

        // Восстанавливаем настройки
        g.setColor(oldColor);
        g.setFont(oldFont);

        // Сбрасываем счетчики каждую секунду
        if (System.currentTimeMillis() - lastWorldUpdateTime >= 1000) {
            worldUpdates = 0;
            lastWorldUpdateTime = System.currentTimeMillis();
            totalRenderTime = 0;
            renderCount = 0;
        }
    }
    private List<Station> getAllStationsSorted() {
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
                    getWorld().getWorldGrid()[x][y].draw(g, 0, 0, 1);
                }
            }

            g.setTransform(originalTransform);
        }



        // Добавляем метод для переключения режима отладки
        public void toggleDebugMode() {
            debugMode = !debugMode;
            repaint();
        }


    }
