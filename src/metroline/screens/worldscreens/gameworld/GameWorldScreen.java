package metroline.screens.worldscreens.gameworld;

import metroline.core.world.GameWorld;
import metroline.input.WorldClickController;
import metroline.objects.gameobjects.*;
import metroline.MainFrame;
import metroline.objects.gameobjects.Label;
import metroline.screens.panel.InfoWindow;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.render.StationRender;
import metroline.screens.worldscreens.CachedWorldScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;


public class GameWorldScreen extends CachedWorldScreen {
    public static GameWorldScreen INSTANCE;
    public WorldClickController worldClickController;
    public LinesLegendWindow legendWindow;
    public final List<InfoWindow> infoWindows = new ArrayList<>();
    private Timer repaintTimer;
    //Debug
    public boolean debugMode = false;
    //  private BufferedImage worldCache; // Кешированное изображение мира
    private boolean cacheValid = false; // Флаг валидности кеша


    public GameWorldScreen(MainFrame parent) {
        super(parent,  new GameWorld());
        INSTANCE = this;

        this.worldClickController = new WorldClickController( this);
        initWorldCache(widthWorld, heightWorld);
        setupRepaintTimer();
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
    public void setLegendWindow(LinesLegendWindow legendWindow) {
        this.legendWindow = legendWindow;
        if (getWorld() instanceof GameWorld) {
            ((GameWorld)getWorld()).setLegendWindow(legendWindow);
        }
    }

    public void createNewWorld(int width, int height, boolean hasPassengerCount, boolean hasAbilityPay, boolean hasLandscape, boolean hasRivers, Color worldColor, int money) {
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
        repaintTimer = new Timer(1000, e -> {
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
            cacheValid = false;
        }

        @Override
        protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);

            worldClickController.checkConstructionProgress();
            Graphics2D g = (Graphics2D)gr;

            if (!cacheValid) {
                updateWorldCache(widthWorld, heightWorld, this::drawStaticWorld);
            }

            // Применяем трансформации
            AffineTransform oldTransform = g.getTransform();
            g.scale(zoom, zoom);
            g.translate(offsetX, offsetY);

            // Рисуем кешированный мир
            g.drawImage(worldCache, 0, 0, null);


            for (Tunnel tunnel : getWorld().getTunnels()) {
                tunnel.draw(g, 0, 0, 1);
            }

            if(getWorld().isRoundStationsEnabled()) {
                for (Station station : getWorld().getStations()) {
                    StationRender.drawWorldColorRing(station,g, 0, 0, 1);
                }
                // 1. Сначала рисуем все соединения всех станций
                for (Station station : getWorld().getStations()) {
                    StationRender.drawRoundTransfer(station, g, 0, 0, 1);
                }

                // 2. Затем рисуем все станции
                for (Station station : getAllStationsSorted()) {
                    StationRender.drawRoundStation(station, g, 0, 0, 1);
                }

                // 3. В конце рисуем выделения
                for (Station station : getWorld().getStations()) {
                    if (station.isSelected()) {
                        StationRender.drawRoundSelection(station, g, 0, 0, 1);
                    }
                }
            } else {

                for (Station station : getWorld().getStations()) {
                    StationRender.drawWorldColorSquare(station, g, 0, 0, 1);
                }

                for (Station station : getWorld().getStations()) {
                    StationRender.drawRoundTransfer(station, g, 0, 0, 1);
                }

                // 2. Затем рисуем все станции
                for (Station station : getAllStationsSorted()) {
                    StationRender.drawSquareStation(station, g, 0, 0, 1);
                }

                // 3. В конце рисуем выделения
                for (Station station : getWorld().getStations()) {
                    if (station.isSelected()) {
                        StationRender.drawSquareSelection(station, g, 0, 0, 1);
                    }
                }
            }
            for (GameplayUnits gUnits : ((GameWorld)getWorld()).getGameplayUnits()) {
                gUnits.draw(g, 0, 0, 1);
            }

            for (Label label : getWorld().getLabels()) {
                label.draw(g, 0, 0, 1);
            }

            // Восстанавливаем трансформации
            g.setTransform(oldTransform);

            // Рисуем debug-информацию
            if (debugMode) {
                drawDebugInfo(g, worldClickController.getSelectedObject());
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
