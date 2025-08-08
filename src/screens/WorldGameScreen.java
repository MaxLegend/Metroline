package screens;

import game.core.world.GameWorld;
import game.core.world.SandboxWorld;
import game.input.GameClickHandler;
import game.input.KeyboardController;
import game.input.MouseController;
import game.objects.Label;
import game.objects.PathPoint;
import game.objects.Station;
import game.objects.Tunnel;
import game.objects.enums.Direction;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;

public class WorldGameScreen extends WorldScreen {
    public static WorldGameScreen INSTANCE;


    public GameClickHandler gameClickHandler;
    // Input controllers

    //Debug
    public boolean debugMode = false;
    private Font debugFont = new Font("Monospaced", Font.PLAIN, 12);

    private BufferedImage worldCache; // Кешированное изображение мира
    private boolean cacheValid = false; // Флаг валидности кеша


    public WorldGameScreen(MainFrame parent) {
        super(parent, new GameWorld(widthWorld, heightWorld, false, false, Color.WHITE));

        INSTANCE = this;

        this.gameClickHandler = new GameClickHandler();
        initWorldCache();
    }

    public void createNewWorld(int width, int height, boolean hasOrganicPatches, boolean hasRivers, Color worldColor) {
        widthWorld = width;
        heightWorld = height;
        this.setWorld(new GameWorld(width, height, hasOrganicPatches, hasRivers,worldColor));
        invalidateCache();
        repaint();
    }
    /**
     * Handles mouse click on the world
     * @param x X coordinate in world space
     * @param y Y coordinate in world space
     */
    public void handleClick(int x, int y) {
        if (x < 0 || x >= getWorld().getWidth() || y < 0 || y >= getWorld().getHeight()) {
            return;
        }
        if (isShiftPressed && isCPressed) {
            gameClickHandler.showColorSelectionPopup(x, y);
        }
        else if (isShiftPressed) {
            gameClickHandler.handleStationClick(x, y);
        }
        else if (isCtrlPressed) {

            gameClickHandler.handleTunnelClick(x, y);
        }
        else {
            gameClickHandler.handleDefaultClick(x, y);
        }

        repaint();
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


            for (Station station : getWorld().getStations()) {
                station.draw(g, 0, 0, 1);
            }

            for (game.objects.Label label : getWorld().getLabels()) {
                label.draw(g2d, 0, 0, 1);
            }

            // Восстанавливаем трансформации
            g2d.setTransform(oldTransform);

            // Рисуем debug-информацию
            if (debugMode) {
                drawDebugInfo(g2d);
            }
        }
        private void updateWorldCache() {
            if (worldCache == null ||
                    worldCache.getWidth() != widthWorld * 32 ||
                    worldCache.getHeight() != heightWorld * 32) {
                initWorldCache();
            }

            Graphics2D cacheGraphics = worldCache.createGraphics();
            try {
                // Очищаем кеш
                cacheGraphics.setBackground(new Color(0, 0, 0, 0));
                cacheGraphics.clearRect(0, 0, worldCache.getWidth(), worldCache.getHeight());

                // Рисуем статичные элементы в кеш
                drawStaticWorld(cacheGraphics);
            } finally {
                cacheGraphics.dispose();
            }
            cacheValid = true;
        }
        public void drawStaticWorld(Graphics2D g) {
            // Рисуем сетку
            AffineTransform originalTransform = g.getTransform();
            g.scale(2, 2);
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

                    for (game.objects.Label label : getWorld().getLabelsForStation(station)) {
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
            else if (gameClickHandler.getSelectedObject() instanceof game.objects.Label) {
                game.objects.Label label = (Label) gameClickHandler.getSelectedObject();
                yPos += 15;
                g.drawString("=== SELECTED LABEL ===", 10, yPos);
                yPos += 15;
                g.drawString("Hash: " + label.hashCode(), 10, yPos);
                yPos += 15;
                g.drawString("Text: '" + label.getText() + "'", 10, yPos);
                yPos += 15;
                g.drawString("Position: (" + label.getX() + "," + label.getY() + ")", 10, yPos);
                yPos += 15;
                g.drawString("Parent Station: " + label.getParentStation().getName() +
                        " (" + label.getParentStation().getX() + "," +
                        label.getParentStation().getY() + ")", 10, yPos);
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


    }
