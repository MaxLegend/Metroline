package metroline.screens.worldscreens;

import metroline.core.world.GameWorld;
import metroline.core.world.tiles.GameTile;
import metroline.core.world.tiles.WorldTile;
import metroline.input.WorldClickController;
import metroline.objects.gameobjects.*;
import metroline.objects.enums.Direction;
import metroline.MainFrame;
import metroline.objects.gameobjects.Label;
import metroline.screens.panel.InfoWindow;
import metroline.screens.panel.LinesLegendWindow;
import metroline.util.PerlinNoise;
import metroline.util.VoronoiNoise;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;


public class WorldGameScreen extends WorldScreen {
    public static WorldGameScreen INSTANCE;
    private Timer repaintTimer;
    public WorldClickController gameClickHandler;


    public transient LinesLegendWindow legendWindow;
    public final List<InfoWindow> infoWindows = new ArrayList<>();

    //Debug
    public boolean debugMode = false;
    private Font debugFont = new Font("Monospaced", Font.PLAIN, 12);

    private BufferedImage worldCache; // Кешированное изображение мира
    private boolean cacheValid = false; // Флаг валидности кеша


    public WorldGameScreen(MainFrame parent) {
        super(parent,  new GameWorld());
        INSTANCE = this;

        this.gameClickHandler = new WorldClickController( this);
        initWorldCache();
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
        gameClickHandler.mainClickHandler(x,y);
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
        this.gameClickHandler = new WorldClickController( this);
        setupRepaintTimer();
        ((GameWorld)getWorld()).generateRandomGameplayUnits((int) GameConstants.GAMEPLAY_UNITS_COUNT);
        invalidateCache();
        repaint();
    }

    private void setupRepaintTimer() {
        repaintTimer = new Timer(1000, e -> {
            gameClickHandler.checkConstructionProgress();
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


    public static WorldGameScreen getInstance() {
        return INSTANCE;
    }
        private void initWorldCache() {
            // Создаем кеш достаточного размера
            int cacheWidth = widthWorld * 32;
            int cacheHeight = heightWorld * 32;
            worldCache = new BufferedImage(cacheWidth, cacheHeight, BufferedImage.TYPE_INT_ARGB);
        }
        public void invalidateCache() {
            cacheValid = false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
      //      updateInfoPanelPosition();
            gameClickHandler.checkConstructionProgress();
            Graphics2D g2d = (Graphics2D)g;

            // Обновляем кеш при необходимости
            if (!cacheValid) {
                updateWorldCache();
            }

            // Применяем трансформации
            AffineTransform oldTransform = g2d.getTransform();
            g2d.scale(zoom, zoom);
            g2d.translate(offsetX, offsetY);

            // Рисуем кешированный мир
            g2d.drawImage(worldCache, 0, 0, null);


            for (Tunnel tunnel : getWorld().getTunnels()) {
                tunnel.draw(g, 0, 0, 1);
            }

            if(getWorld().isRoundStationsEnabled()) {
                for (Station station : getWorld().getStations()) {
                    station.drawWorldColorRing(g, 0, 0, 1);
                }
                // 1. Сначала рисуем все соединения всех станций
                for (Station station : getWorld().getStations()) {
                    station.drawRoundTransfer(g, 0, 0, 1);
                }

                // 2. Затем рисуем все станции
                for (Station station : getAllStationsSorted()) {
                    station.drawRoundStation(g, 0, 0, 1);
                }

                // 3. В конце рисуем выделения
                for (Station station : getWorld().getStations()) {
                    if (station.isSelected()) {
                        station.drawRoundSelection(g, 0, 0, 1);
                    }
                }
            } else {

                for (Station station : getWorld().getStations()) {
                    station.drawWorldColorSquare(g, 0, 0, 1);
                }

                for (Station station : getWorld().getStations()) {
                    station.drawRoundTransfer(g, 0, 0, 1);
                }

                // 2. Затем рисуем все станции
                for (Station station : getAllStationsSorted()) {
                    station.drawSquareStation(g, 0, 0, 1);
                }

                // 3. В конце рисуем выделения
                for (Station station : getWorld().getStations()) {
                    if (station.isSelected()) {
                        station.drawSquareSelection(g, 0, 0, 1);
                    }
                }
            }
            for (GameplayUnits gUnits : ((GameWorld)getWorld()).getGameplayUnits()) {
                gUnits.draw(g, 0, 0, 1);
            }

            for (Label label : getWorld().getLabels()) {
                label.draw(g2d, 0, 0, 1);
            }

            // Восстанавливаем трансформации
            g2d.setTransform(oldTransform);

            // Рисуем debug-информацию
            if (debugMode) {
                drawDebugInfo(g2d);
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
        private void updateWorldCache() {
            if (worldCache == null ||
                    worldCache.getWidth() != widthWorld * 32 ||
                    worldCache.getHeight() != heightWorld * 32) {
                initWorldCache();
            }

            Graphics2D cacheGraphics = worldCache.createGraphics();
            try {
                cacheGraphics.setComposite(AlphaComposite.Clear);
                cacheGraphics.fillRect(0, 0, worldCache.getWidth(), worldCache.getHeight());
                cacheGraphics.setComposite(AlphaComposite.SrcOver);

                // Рисуем статичные элементы в кеш
                cacheGraphics.scale(2,2);
                drawStaticWorld(cacheGraphics);
            } finally {
                cacheGraphics.dispose();
            }
            cacheValid = true;
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




        /**
         * Рисует глобальную отладочную информацию
         */
        private void drawDebugInfo(Graphics2D g) {
            g.setFont(debugFont);
            g.setColor(java.awt.Color.YELLOW);

            int yPos = 60; // Начальная позиция по Y

            // Глобальная информация
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

            // Информация о выбранной станции
            if (gameClickHandler.getSelectedObject() instanceof Station) {
                Station station = (Station) gameClickHandler.getSelectedObject();
                yPos += 15;
                g.drawString("=== SELECTED STATION ===", 10, yPos);
                yPos += 15;
                g.drawString("Hash: " + station.hashCode(), 10, yPos);
                yPos += 15;
                g.drawString("Position: (" + station.getX() + "," + station.getY() + ")", 10, yPos);
                yPos += 15;
                g.drawString("Name: " + station.getName(), 10, yPos);
                yPos += 15;
                g.drawString("Type: " + station.getType(), 10, yPos);
                yPos += 15;
                g.drawString("Color: " + String.format("#%06X", (0xFFFFFF & station.getColor().getRGB())), 10, yPos);
                yPos += 15;

                if (!getWorld().getLabelsForStation(station).isEmpty()) {
                    g.drawString("Labels (" + getWorld().getLabelsForStation(station).size() + "):", 10, yPos);
                    yPos += 15;

                    for (Label label : getWorld().getLabelsForStation(station)) {
                        g.drawString("- '" + label.getText() + "' at (" +
                                label.getX() + "," + label.getY() + ")", 20, yPos);
                        yPos += 15;
                    }
                }
                // Информация о соединениях
                if (!station.getConnections().isEmpty()) {
                    g.drawString("Connections:", 10, yPos);
                    yPos += 15;

                    for (Map.Entry<Direction, Station> entry : station.getConnections().entrySet()) {
                        g.drawString("- " + entry.getKey() + " -> Station " + entry.getValue().getName(), 20, yPos);
                        yPos += 15;
                    }
                }

            }
            else if (gameClickHandler.getSelectedObject() instanceof Label) {
                Label label = (Label) gameClickHandler.getSelectedObject();
                yPos += 15;
                g.drawString("=== SELECTED LABEL ===", 10, yPos);
                yPos += 15;
                g.drawString("Hash: " + label.hashCode(), 10, yPos);
                yPos += 15;
                g.drawString("Text: '" + label.getText() + "'", 10, yPos);
                yPos += 15;
                g.drawString("Position: (" + label.getX() + "," + label.getY() + ")", 10, yPos);
                yPos += 15;
                g.drawString("Parent Station: " + label.getParentGameObject().getName() +
                        " (" + label.getParentGameObject().getX() + "," +
                        label.getParentGameObject().getY() + ")", 10, yPos);
                yPos += 15;
            }
            // Информация о выбранном туннеле
            else if (gameClickHandler.getSelectedObject() instanceof Tunnel) {
                Tunnel tunnel = (Tunnel) gameClickHandler.getSelectedObject();
                yPos += 15;
                g.drawString("=== SELECTED TUNNEL ===", 10, yPos);
                yPos += 15;
                g.drawString("Hash: " + tunnel.hashCode(), 10, yPos);
                yPos += 15;

                // Информация о станциях
                Station start = tunnel.getStart();
                Station end = tunnel.getEnd();
                g.drawString("From: " + start.getName() + " (" + start.getX() + "," + start.getY() + ")", 10, yPos);
                yPos += 15;
                g.drawString("To: " + end.getName() + " (" + end.getX() + "," + end.getY() + ")", 10, yPos);
                yPos += 15;

                // Информация о точках пути
                g.drawString("Path points (" + tunnel.getPath().size() + "):", 10, yPos);
                yPos += 15;

                for (int i = 0; i < tunnel.getPath().size(); i++) {
                    PathPoint p = tunnel.getPath().get(i);
                    String pointType = (i == 0) ? "START" : (i == tunnel.getPath().size()-1) ? "END" : "CTRL";
                    g.drawString(pointType + " " + i + ": (" + p.getX() + "," + p.getY() + ")", 20, yPos);
                    yPos += 15;
                }

            }

            // Убираем старые методы drawTunnelDebugInfo и drawStationDebugInfo
            // так как вся информация теперь выводится в одном месте
        }

        // Добавляем метод для переключения режима отладки
        public void toggleDebugMode() {
            debugMode = !debugMode;
            repaint();
        }

    private String formatDuration(long durationMillis) {
        long minutes = (durationMillis / (1000 * 60)) % 60;
        long seconds = (durationMillis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    }
