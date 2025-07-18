package screens;


import game.core.GameObject;
import game.core.World;

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
    private World world;
    private float zoom = 1.0f;
    private int offsetX = 0;
    private int offsetY = 0;
    private int dragStartX, dragStartY;
    private boolean dragging = false;

    private int widthWorld = 100, heightWorld = 100;

    // Game modes
    public enum GameMode { NONE, STATION, TUNNEL, EDIT, COLOR }
    private GameMode currentMode = GameMode.NONE;
    private Station firstStationForTunnel = null;
    private GameObject selectedObject = null;
    private PathPoint dragOffset = null;

    private Color currentStationColor = Station.COLORS[0]; // Красный по умолчанию
    private boolean colorSelectionEnabled = false;

    // Input controllers
    private MouseController mouseController;
    private KeyboardController keyboardController;

    //Debug
    private boolean debugMode = false;
    private Font debugFont = new Font("Monospaced", Font.PLAIN, 12);


    public WorldScreen(MainFrame parent) {
        super(parent);
        world = new World(widthWorld, heightWorld);


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

    @Override
    public void onActivate() {
        requestFocusInWindow();
    }


    public GameMode getCurrentMode() {
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
                handleStationClick(x, y);
                break;
            case TUNNEL:
                handleTunnelClick(x, y);
                break;
            case EDIT:
                handleEditClick(x, y);
                break;
            default:
                handleSelectionClick(x, y);
                break;
        }

        repaint();
    }
    /**
     * Handles edit mode click
     * @param x X coordinate
     * @param y Y coordinate
     */
    private void handleEditClick(int x, int y) {
        // First check if we're already dragging something
        if (selectedObject != null) {
            selectedObject.setSelected(false);
            selectedObject = null;
            dragOffset = null;
            return;
        }

        // Check for station first
        Station station = world.getStationAt(x, y);
        if (station != null) {
            selectedObject = station;
            station.setSelected(true);
            dragOffset = new PathPoint(x - station.getX(), y - station.getY());
            return;
        }

        // Then check for tunnel control PathPoint
        Tunnel tunnel = world.getTunnelAt(x, y);
        if (tunnel != null) {
            selectedObject = tunnel;
            tunnel.setSelected(true);

            // Find which path PathPoint was clicked
            for (PathPoint p : tunnel.getPath()) {
                if (p.getX() == x && p.getY() == y && !p.equals(tunnel.getStart()) && !p.equals(tunnel.getEnd())) {
                    dragOffset = new PathPoint(0, 0); // No offset for control PathPoints
                    return;
                }
            }
        }
    }
    /**
     * Handles mouse drag in edit mode
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleEditDrag(int x, int y) {
        if (currentMode == GameMode.EDIT && selectedObject != null && dragOffset != null) {
            // Проверяем границы
            if (x < 0 || x >= world.getWidth() || y < 0 || y >= world.getHeight()) {
                return;
            }
            if (selectedObject instanceof Station) {
                Station station = (Station)selectedObject;
                int newX = x - dragOffset.x;
                int newY = y - dragOffset.y;

                // Check if new position is valid
                if (newX >= 0 && newX < world.getWidth() &&
                        newY >= 0 && newY < world.getHeight() &&
                        world.getStationAt(newX, newY) == null) {

                    // Remove from old position
                    world.getGameGrid()[station.getX()][station.getY()].setContent(null);

                    // Update position
                    station.x = newX;
                    station.y = newY;

                    // Add to new position
                    world.getGameGrid()[newX][newY].setContent(station);

                    // Recalculate all connected tunnels
                    for (Tunnel t : world.getTunnels()) {
                        if (t.getStart() == station || t.getEnd() == station) {
                            t.calculatePath();
                        }
                    }
                }
            }
            else if (selectedObject instanceof Tunnel) {
                Tunnel tunnel = (Tunnel)selectedObject;

                // Проверяем, что новая позиция не совпадает со станцией
                Station stationAtPos = world.getStationAt(x, y);
                if (stationAtPos == null) {
                    tunnel.moveControlPoint(x, y);
                    repaint();
                }
            }
        }
    }

    /**
     * Handles station placement/removal
     * @param x X coordinate
     * @param y Y coordinate
     */
    private void handleStationClick(int x, int y) {
        Station existing = world.getStationAt(x, y);

        if (existing != null) {
            // Remove existing station
            world.removeStation(existing);
        } else {
            if (colorSelectionEnabled) {
                // Show color chooser dialog only when color selection is enabled
                String[] colors = {"Red", "Green", "Blue", "Orange"};
                String choice = (String)JOptionPane.showInputDialog(
                        this,
                        "Select station color:",
                        "New Station",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        colors,
                        colors[0]);

                if (choice != null) {
                    currentStationColor = Station.COLORS[Arrays.asList(colors).indexOf(choice)];
                } else {
                    return; // User canceled
                }
            }

            // Create new station with current color (red by default or last selected)
            Station station = new Station(x, y, currentStationColor, StationType.REGULAR);
            world.addStation(station);

            // Check if this should be a transfer station
            checkForTransferStation(station);
        }
    }

    /**
     * Toggles color selection mode
     */
    public void toggleColorSelection() {
        colorSelectionEnabled = !colorSelectionEnabled;
        JOptionPane.showMessageDialog(this,
                "Color selection mode: " + (colorSelectionEnabled ? "ON" : "OFF"));
    }

    /**
     * Gets current station color
     */
    public Color getCurrentStationColor() {
        return currentStationColor;
    }

    /**
     * Checks if a station should be converted to a transfer station
     * @param station Station to check
     */
    private void checkForTransferStation(Station station) {
        int x = station.getX();
        int y = station.getY();

        // Check all 8 directions
        for (int ny = Math.max(0, y-1); ny < Math.min(world.getHeight(), y+2); ny++) {
            for (int nx = Math.max(0, x-1); nx < Math.min(world.getWidth(), x+2); nx++) {
                if (nx == x && ny == y) continue;

                Station neighbor = world.getStationAt(nx, ny);
                if (neighbor != null && !neighbor.getColor().equals(station.getColor())) {
                    station.setType(StationType.TRANSFER);
                    neighbor.setType(StationType.TRANSFER);
                    return;
                }
            }
        }
    }

    /**
     * Handles tunnel creation
     * @param x X coordinate
     * @param y Y coordinate
     */
    private void handleTunnelClick(int x, int y) {
        Station station = world.getStationAt(x, y);

        if (station == null) return;

        if (firstStationForTunnel == null) {
            // First station selection
            firstStationForTunnel = station;
            station.setSelected(true);
        } else if (firstStationForTunnel == station) {
            // Clicked same station - cancel
            firstStationForTunnel.setSelected(false);
            firstStationForTunnel = null;
        } else {
            // Second station - create tunnel
            Tunnel tunnel = new Tunnel(firstStationForTunnel, station);
            world.addTunnel(tunnel);

            firstStationForTunnel.setSelected(false);
            firstStationForTunnel = null;
        }
    }

    /**
     * Handles selection of stations/tunnels
     * @param x X coordinate
     * @param y Y coordinate
     */
    private void handleSelectionClick(int x, int y) {
        // Check for station first
        Station station = world.getStationAt(x, y);
        if (station != null) {
            station.setSelected(!station.isSelected());
            return;
        }

        // Then check for tunnel
        Tunnel tunnel = world.getTunnelAt(x, y);
        if (tunnel != null) {
            tunnel.setSelected(!tunnel.isSelected());
        }
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
            if (debugMode) {
                drawTunnelDebugInfo(g2d, tunnel);
            }
        }

        // Draw stations
        for (Station station : world.getStations()) {
            station.draw(g2d, 0, 0, 1);
            if (debugMode) {
                drawStationDebugInfo(g2d, station);
            }
        }

        // Draw mode indicator
        g2d.setTransform(oldTransform);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Mode: " + currentMode, 10, 20);

        if (currentMode == GameMode.TUNNEL && firstStationForTunnel != null) {
            g2d.drawString("Select second station", 10, 40);
        }

        if (debugMode) {
            drawGlobalDebugInfo(g2d);
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
     * Starts dragging the view
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     */
    public void startDrag(int x, int y) {
        dragging = true;
        dragStartX = x - offsetX;
        dragStartY = y - offsetY;
    }

    /**
     * Updates view during drag
     * @param x Current X coordinate
     * @param y Current Y coordinate
     */
    public void updateDrag(int x, int y) {
        if (dragging) {
            offsetX = x - dragStartX;
            offsetY = y - dragStartY;
            repaint();
        }
    }

    /**
     * Stops dragging the view
     */
    public void stopDrag() {
        dragging = false;
    }

    /**
     * Рисует отладочную информацию о туннеле
     */
    private void drawTunnelDebugInfo(Graphics2D g, Tunnel tunnel) {
        g.setFont(debugFont);
        g.setColor(Color.RED);

        // Информация о начальной и конечной точках
        PathPoint start = tunnel.getPath().get(0);
        PathPoint end = tunnel.getPath().get(tunnel.getPath().size() - 1);

        int startX = (int)start.getX() * 32 + 16;
        int startY = (int)start.getY() * 32 + 16;
        int endX =(int) end.getX() * 32 + 16;
        int endY = (int)end.getY() * 32 + 16;

        g.drawString("Tunnel " + tunnel.hashCode(), startX - 30, startY - 10);
        g.drawString("From: (" + start.getX() + "," + start.getY() + ")", startX - 30, startY + 25);
        g.drawString("To: (" + end.getX() + "," + end.getY() + ")", endX - 30, endY + 25);

        // Информация о контрольных точках
        if (tunnel.getPath().size() > 2) {
            PathPoint control = tunnel.getPath().get(1);
            int cX = (int)control.getX() * 32 + 16;
            int cY = (int)control.getY() * 32 + 16;
        //    g.drawString("Ctrl: (" + control.x + "," + control.y + ")", cX - 50, cY - 15);
        }

        // Рисуем номера всех точек пути
        g.setColor(Color.RED);
        for (int i = 0; i < tunnel.getPath().size(); i++) {
            PathPoint p = tunnel.getPath().get(i);
            int px = (int)p.getX() * 32 + 16;
            int py = (int)p.getY() * 32 + 16;
            g.drawString(Integer.toString(i), px - 3, py - 5);
        }
    }

    /**
     * Рисует отладочную информацию о станции
     */
    private void drawStationDebugInfo(Graphics2D g, Station station) {
        g.setFont(debugFont);
        g.setColor(Color.BLACK);

        int x = station.getX() * 32 + 16;
        int y = station.getY() * 32 + 16;

        // Основная информация
        g.drawString("Station " + station.hashCode(), x - 30, y - 25);
        g.drawString("Pos: (" + station.getX() + "," + station.getY() + ")", x - 30, y - 10);
        g.drawString("Type: " + station.getType(), x - 30, y + 25);

        // Информация о соединениях
        int connY = y + 40;
        for (Map.Entry<Direction, Station> entry : station.getConnections().entrySet()) {
            g.drawString(entry.getKey() + " -> " + entry.getValue().hashCode(),
                    x - 50, connY);
            connY += 15;
        }
    }

    /**
     * Рисует глобальную отладочную информацию
     */
    private void drawGlobalDebugInfo(Graphics2D g) {
        g.setFont(debugFont);
        g.setColor(Color.BLACK);

        int yPos = 60;
        g.drawString("=== DEBUG INFO ===", 10, yPos);
        yPos += 15;
        g.drawString("Stations: " + world.getStations().size(), 10, yPos);
        yPos += 15;
        g.drawString("Tunnels: " + world.getTunnels().size(), 10, yPos);
        yPos += 15;
        g.drawString("Zoom: " + String.format("%.2f", zoom), 10, yPos);
        yPos += 15;
        g.drawString("Offset: (" + offsetX + "," + offsetY + ")", 10, yPos);
    }

    // Добавляем метод для переключения режима отладки
    public void toggleDebugMode() {
        debugMode = !debugMode;
        repaint();
    }
}