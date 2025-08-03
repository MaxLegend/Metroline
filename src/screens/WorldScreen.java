package screens;


import game.core.GameObject;
import game.core.world.World;

import game.input.ClickHandler;
import game.input.KeyboardController;
import game.input.MouseController;

import game.objects.PathPoint;
import game.objects.Station;
import game.objects.enums.Direction;
import game.objects.enums.StationType;
import game.objects.Tunnel;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Map;


/**
 * World screen that displays and interacts with the game world
 */
public class WorldScreen extends GameScreen {

    public static WorldScreen INSTANCE;

    public static World world;
    private float zoom = 1.0f;
    public static int offsetX = 0;
    public static int offsetY = 0;


    public static int widthWorld = 100, heightWorld = 100;


    // Game modes
    public enum GameMode { NONE, STATION, TUNNEL, EDIT, COLOR }
    public static GameMode currentMode = GameMode.NONE;
    public static Station firstStationForTunnel = null;
    public static GameObject selectedObject = null;


    // Input controllers
    private MouseController mouseController;
    private KeyboardController keyboardController;

    //Debug
    private boolean debugMode = false;
    private Font debugFont = new Font("Monospaced", Font.PLAIN, 12);


    public WorldScreen(MainFrame parent) {
        super(parent);
        world = new World(widthWorld, heightWorld);
        INSTANCE = this;

        // Initialize controllers
        mouseController = new MouseController(this);
        keyboardController = new KeyboardController(this);

        addMouseListener(mouseController);
        addMouseMotionListener(mouseController);
        addMouseWheelListener(mouseController);
        addKeyListener(keyboardController);

        // Setup toolbar
        JButton stationButton = new JButton("Stations (S)");
        stationButton.addActionListener(e -> setMode(GameMode.STATION));

        JButton tunnelButton = new JButton("Tunnels (T)");
        tunnelButton.addActionListener(e -> setMode(GameMode.TUNNEL));

        // Add edit button to toolbar
        JButton editButton = new JButton("Edit (E)");
        editButton.addActionListener(e -> setMode(GameMode.EDIT));

        JButton colorButton = new JButton("Color (C)");
        editButton.addActionListener(e -> setMode(GameMode.COLOR));

        parent.addToolbarButton(stationButton);
        parent.addToolbarButton(tunnelButton);
        parent.addToolbarButton(editButton);
        parent.addToolbarButton(colorButton);
    }
    public static WorldScreen getInstance() {
        return INSTANCE;
    }
    @Override
    public void onActivate() {
        requestFocusInWindow();
    }


    public static GameMode getCurrentMode() {
        return currentMode;
    }
    /**
     * Sets the current game mode
     * @param mode New game mode
     */
    public void setMode(GameMode mode) {
        currentMode = mode;
        firstStationForTunnel = null;

        // Deselect all stations
        for (Station station : world.getStations()) {
            station.setSelected(false);
        }

        // Deselect all tunnels
        for (Tunnel tunnel : world.getTunnels()) {
            tunnel.setSelected(false);
        }

        repaint();
    }

    /**
     * Handles mouse click on the world
     * @param x X coordinate in world space
     * @param y Y coordinate in world space
     */
    public void handleClick(int x, int y) {
        // Check bounds
        if (x < 0 || x >= world.getWidth() || y < 0 || y >= world.getHeight()) {
            return;
        }

        switch (currentMode) {
            case STATION:
                ClickHandler.handleStationClick(x, y);
                break;
            case TUNNEL:
                ClickHandler.handleTunnelClick(x, y);
                break;
            case EDIT:
                ClickHandler.handleEditClick(x, y);
                break;
            default:
                ClickHandler.handleSelectionClick(x, y);
                break;
        }

        repaint();
    }







    /**
     * Converts screen coordinates to world coordinates
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @return PathPoint in world coordinates
     */
    public PathPoint screenToWorld(int screenX, int screenY) {
        int worldX = (int)((screenX / zoom - offsetX) / 32);
        int worldY = (int)((screenY / zoom - offsetY) / 32);
        return new PathPoint(worldX, worldY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D)g;

        // Apply zoom and offset
        AffineTransform oldTransform = g2d.getTransform();
        g2d.scale(zoom, zoom);
        g2d.translate(offsetX, offsetY);

//        // Draw world grid
//        for (int y = 0; y < world.getHeight(); y++) {
//            for (int x = 0; x < world.getWidth(); x++) {
//                world.getBigWorldGrid()[x][y].draw(g2d, 0, 0, 1);
//            }
//        }
        // Draw small world grid
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                world.getWorldGrid()[x][y].draw(g2d, 0, 0, 1);
            }
        }

        // Draw tunnels
        for (Tunnel tunnel : world.getTunnels()) {
            tunnel.draw(g2d, 0, 0, 1);
        }

        // Draw stations
        for (Station station : world.getStations()) {
            station.draw(g2d, 0, 0, 1);
        }

        // Draw mode indicator
        g2d.setTransform(oldTransform);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Mode: " + currentMode, 10, 20);

        if (currentMode == GameMode.TUNNEL && firstStationForTunnel != null) {
            g2d.drawString("Select second station", 10, 40);
        }

        if (debugMode) {
            drawDebugInfo(g2d);
        }
    }

    /**
     * Gets the current zoom level
     * @return Zoom level
     */
    public float getZoom() { return zoom; }

    /**
     * Sets the zoom level
     * @param zoom New zoom level
     */
    public void setZoom(float zoom) {
        this.zoom = Math.max(0.1f, Math.min(3.0f, zoom));
        repaint();
    }

    /**
     * Gets the horizontal offset
     * @return X offset
     */
    public int getOffsetX() { return offsetX; }

    /**
     * Gets the vertical offset
     * @return Y offset
     */
    public int getOffsetY() { return offsetY; }

    /**
     * Sets the view offset
     * @param offsetX New X offset
     * @param offsetY New Y offset
     */
    public void setOffset(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        repaint();
    }




    /**
     * Рисует глобальную отладочную информацию
     */
    private void drawDebugInfo(Graphics2D g) {
        g.setFont(debugFont);
        g.setColor(Color.BLACK);

        int yPos = 60; // Начальная позиция по Y

        // Глобальная информация
        g.drawString("=== DEBUG INFO ===", 10, yPos);
        yPos += 15;
        g.drawString("Stations: " + world.getStations().size(), 10, yPos);
        yPos += 15;
        g.drawString("Tunnels: " + world.getTunnels().size(), 10, yPos);
        yPos += 15;
        g.drawString("Zoom: " + String.format("%.2f", zoom), 10, yPos);
        yPos += 15;
        g.drawString("Offset: (" + offsetX + "," + offsetY + ")", 10, yPos);
        yPos += 15;

        // Информация о выбранной станции
        if (selectedObject instanceof Station) {
            Station station = (Station)selectedObject;
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

            // Информация о соединениях
            if (!station.getConnections().isEmpty()) {
                g.drawString("Connections:", 10, yPos);
                yPos += 15;

                for (Map.Entry<Direction, Station> entry : station.getConnections().entrySet()) {
                    g.drawString("- " + entry.getKey() + " -> Station " + entry.getValue().hashCode(), 20, yPos);
                    yPos += 15;
                }
            }
        }

        // Информация о выбранном туннеле
        else if (selectedObject instanceof Tunnel) {
            Tunnel tunnel = (Tunnel)selectedObject;
            yPos += 15;
            g.drawString("=== SELECTED TUNNEL ===", 10, yPos);
            yPos += 15;
            g.drawString("Hash: " + tunnel.hashCode(), 10, yPos);
            yPos += 15;

            // Информация о станциях
            Station start = tunnel.getStart();
            Station end = tunnel.getEnd();
            g.drawString("From: Station " + start.hashCode() + " (" + start.getX() + "," + start.getY() + ")", 10, yPos);
            yPos += 15;
            g.drawString("To: Station " + end.hashCode() + " (" + end.getX() + "," + end.getY() + ")", 10, yPos);
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