package game.input;

import game.core.world.World;
import game.objects.PathPoint;
import game.objects.Station;
import game.objects.Tunnel;
import game.objects.enums.StationType;
import screens.WorldScreen;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

import static screens.WorldScreen.*;

public class ClickHandler {
    private static int dragStartX;
    private static int dragStartY;
    private static boolean dragging = false;
    private static PathPoint dragOffset = null;

    private static Color currentStationColor = Station.COLORS[0]; // Красный по умолчанию
    private static boolean colorSelectionEnabled = false;
    /**
     * Handles edit mode click
     * @param x X coordinate
     * @param y Y coordinate
     */
    public static void handleEditClick(int x, int y) {
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
    public static void handleEditDrag(int x, int y) {
        if (currentMode == WorldScreen.GameMode.EDIT && selectedObject != null && dragOffset != null) {
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
                    WorldScreen.getInstance().repaint();
                }
            }
            else if (selectedObject instanceof Tunnel) {
                Tunnel tunnel = (Tunnel)selectedObject;

                // Проверяем, что новая позиция не совпадает со станцией
                Station stationAtPos = world.getStationAt(x, y);
                if (stationAtPos == null) {
                    tunnel.moveControlPoint(x, y);
                    WorldScreen.getInstance().repaint();
                }
            }
        }
    }
    /**
     * Handles tunnel creation
     * @param x X coordinate
     * @param y Y coordinate
     */
    public static void handleTunnelClick(int x, int y) {
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
    public static void handleSelectionClick(int x, int y) {
        // Check for station first
        Station station = world.getStationAt(x, y);
        if (station != null) {
            // Если станция уже выбрана - редактируем название
            if (station.isSelected()) {
                editStationName(station);
            } else {
                // Иначе просто выбираем станцию
                station.setSelected(true);
            }
            WorldScreen.getInstance().repaint();
            return;
        }


        // Then check for tunnel
        Tunnel tunnel = world.getTunnelAt(x, y);
        if (tunnel != null) {
            tunnel.setSelected(!tunnel.isSelected());
        }
    }
    /**
     * Edit station name
     */
    private static void editStationName(Station station) {
        // Создаем панель с текстовым полем
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Station Name:");
        JTextField textField = new JTextField(station.getName(), 20);
        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);

        // Показываем диалоговое окно
        int result = JOptionPane.showConfirmDialog(
                WorldScreen.getInstance(),
                panel,
                "Edit name",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Обрабатываем результат
        if (result == JOptionPane.OK_OPTION) {
            String newName = textField.getText().trim();
            if (!newName.isEmpty()) {
                station.setName(newName);
                WorldScreen.getInstance().repaint();
            }
        }
    }
    /**
     * Handles station placement/removal
     * @param x X coordinate
     * @param y Y coordinate
     */
    public static void handleStationClick(int x, int y) {
        Station existing = world.getStationAt(x, y);

        if (existing != null) {
            // Remove existing station
            world.removeStation(existing);
        } else {
            if (colorSelectionEnabled) {
                // Show color chooser dialog only when color selection is enabled
                String[] colors = {"Red", "Green", "Blue", "Orange"};
                String choice = (String) JOptionPane.showInputDialog(
                        WorldScreen.getInstance(),
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
     * Checks if a station should be converted to a transfer station
     * @param station Station to check
     */
    private static void checkForTransferStation(Station station) {
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
     * Toggles color selection mode
     */
    public static void toggleColorSelection() {
        colorSelectionEnabled = !colorSelectionEnabled;
        JOptionPane.showMessageDialog(WorldScreen.getInstance(),
                "Color selection mode: " + (colorSelectionEnabled ? "ON" : "OFF"));
    }
    /**
     * Starts dragging the view
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     */
    public static void startDrag(int x, int y) {
        dragging = true;
        dragStartX = x - offsetX;
        dragStartY = y - offsetY;
    }

    /**
     * Updates view during drag
     * @param x Current X coordinate
     * @param y Current Y coordinate
     */
    public static void updateDrag(int x, int y) {
        if (dragging) {
            offsetX = x - dragStartX;
            offsetY = y - dragStartY;
            WorldScreen.getInstance().repaint();
        }
    }

    /**
     * Stops dragging the view
     */
    public static void stopDrag() {
        dragging = false;
    }
}
