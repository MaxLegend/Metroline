package game.input;

import game.core.GameObject;
import game.objects.Label;
import game.objects.PathPoint;
import game.objects.Station;
import game.objects.Tunnel;
import game.objects.enums.StationType;
import screens.WorldSandboxScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


import static screens.WorldSandboxScreen.*;

/**
 * Main world click handler
 * @author Tesmio
 */
public class SandboxClickHandler {
    public static Station selectedStation = null;
    public GameObject selectedObject = null;
    private static int dragStartX;
    private static int dragStartY;
    private static boolean dragging = false;
    static PathPoint dragOffset = null;

    private static Color currentStationColor = Station.COLORS[0]; // Красный по умолчанию
     boolean colorSelectionEnabled = false;

    /**
     * Handles mouse drag in edit mode
     * @param x X coordinate
     * @param y Y coordinate
     */
    public  void handleEditDrag(int x, int y) {
        if ( selectedObject != null && dragOffset != null) {
            // Проверяем границы
            if (x < 0 || x >= WorldSandboxScreen.getInstance().sandboxWorld.getWidth() || y < 0 || y >= WorldSandboxScreen.getInstance().sandboxWorld.getHeight()) {
                return;
            }

                if (selectedObject instanceof Label) {

                    Label label = (Label) selectedObject;
                    int newX = x - dragOffset.x;
                    int newY = y - dragOffset.y;

                    if (label.tryMoveTo(newX, newY)) {
                        WorldSandboxScreen.getInstance().repaint();
                    }
                    return;
                }

            if ( selectedObject instanceof Station) {
                Station station = (Station)selectedObject;

                int newX = x - dragOffset.x;
                int newY = y - dragOffset.y;

                // Check if new position is valid
                if (newX >= 0 && newX < WorldSandboxScreen.getInstance().sandboxWorld.getWidth() &&
                        newY >= 0 && newY < WorldSandboxScreen.getInstance().sandboxWorld.getHeight() &&
                        WorldSandboxScreen.getInstance().sandboxWorld.getStationAt(newX, newY) == null) {
                    Label label = WorldSandboxScreen.getInstance().sandboxWorld.getLabelForStation(station);
                    // Remove from old position
                    WorldSandboxScreen.getInstance().sandboxWorld.getGameGrid()[station.getX()][station.getY()].setContent(null);

                    // Update position
                    station.x = newX;
                    station.y = newY;

                    // Add to new position
                    WorldSandboxScreen.getInstance().sandboxWorld.getGameGrid()[newX][newY].setContent(station);

                    // Recalculate all connected tunnels
                    for (Tunnel t : WorldSandboxScreen.getInstance().sandboxWorld.getTunnels()) {
                        if (t.getStart() == station || t.getEnd() == station) {
                            t.calculatePath();
                        }
                    }
                    if (label != null) {
                        // Находим новую позицию для метки (рядом со станцией)
                        PathPoint newLabelPos = WorldSandboxScreen.getInstance().sandboxWorld.findFreePositionNear(station.getX(), station.getY(), station.getName());
                        if (newLabelPos != null) {
                            label.tryMoveTo(newLabelPos.x, newLabelPos.y);
                        }
                    }
                    WorldSandboxScreen.getInstance().repaint();
                }
            }
            else if (selectedObject instanceof Tunnel) {

                Tunnel tunnel = (Tunnel)selectedObject;

                // Проверяем, что новая позиция не совпадает со станцией
                Station stationAtPos = WorldSandboxScreen.getInstance().sandboxWorld.getStationAt(x, y);
                if (stationAtPos == null) {
                    tunnel.moveControlPoint(x, y);
                    WorldSandboxScreen.getInstance().repaint();
                }
            }
        }
    }
    /**
     * Handles delete game objects
     */
    public  void deleteSelectedObject() {
        if (selectedObject == null) return;

        if (selectedObject instanceof Station) {
            WorldSandboxScreen.getInstance().sandboxWorld.removeStation((Station)selectedObject);
        }
        else if (selectedObject instanceof Tunnel) {
            WorldSandboxScreen.getInstance().sandboxWorld.removeTunnel((Tunnel)selectedObject);
        }
        else if (selectedObject instanceof Label) {
            WorldSandboxScreen.getInstance().sandboxWorld.removeLabel((Label)selectedObject);
        }

        selectedObject = null;
        WorldSandboxScreen.getInstance().repaint();
    }
    public void handleRemoveTunnel(int worldX, int worldY) {
        Tunnel tunnel = WorldSandboxScreen.getInstance().sandboxWorld.getTunnelAt(worldX, worldY);
        if (tunnel != null) {
            WorldSandboxScreen.getInstance().sandboxWorld.removeTunnel(tunnel);
            WorldSandboxScreen.getInstance().repaint();
        }
    }
    public void handleEditStationName(int x, int y) {
        Station station = WorldSandboxScreen.getInstance().sandboxWorld.getStationAt(x, y);
        Tunnel t = getInstance().sandboxWorld.getTunnelAt(x, y);
        if (station != null) {
            editStationName(station);
        }
    }
    /**
     * Handles tunnel creation
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleTunnelClick(int x, int y) {

        Station station = WorldSandboxScreen.getInstance().sandboxWorld.getStationAt(x, y);
        if (station == null) return;

        // Если есть уже выбранная станция и это не та же самая

           if (selectedStation != null && selectedStation != station) {
               // Создаём туннель
               Tunnel tunnel = new Tunnel(selectedStation, station);
               WorldSandboxScreen.getInstance().sandboxWorld.addTunnel(tunnel);

               // Снимаем выделение
               selectedStation.setSelected(false);
               selectedStation = null;
           } else {
               // Выбираем новую станцию (или снимаем выделение если кликнули ту же)
               if (selectedStation == station) {
                   station.setSelected(false);
                   selectedStation = null;
               } else {
                   deselectAll(); // Снимаем выделение со всех других объектов
                   station.setSelected(true);
                   selectedStation = station;
               }
           }

        WorldSandboxScreen.getInstance().repaint();
    }

    public void showColorSelectionPopup(int x, int y) {
        // Создаем прозрачное безрамочное окно
        Window parentWindow = SwingUtilities.getWindowAncestor(WorldSandboxScreen.getInstance());
        JDialog colorDialog = new JDialog(parentWindow);
        colorDialog.setUndecorated(true); // Убираем рамку и заголовок
        colorDialog.setBackground(new Color(0, 0, 0, 0)); // Прозрачный фон

        JPanel colorPanel = new JPanel(new GridLayout(2, 2));
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2)); // Белая рамка
        colorPanel.setOpaque(true);
        colorPanel.setBackground(new Color(50, 50, 50)); // Темный фон

        // Конвертируем мировые координаты в экранные
        Point screenPos = WorldSandboxScreen.getInstance().worldToScreen(x, y);

        for (Color color : Station.COLORS) {
            JButton colorBtn = new JButton();
            colorBtn.setBackground(color);
            colorBtn.setPreferredSize(new Dimension(30, 30));
            colorBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Убираем стандартную рамку кнопки
            colorBtn.setContentAreaFilled(false); // Прозрачная область кнопки
            colorBtn.setOpaque(true); // Но сам цвет видимый
            colorBtn.setFocusPainted(false); // Убираем эффект фокуса

            colorBtn.addActionListener(e -> {
                SandboxClickHandler.currentStationColor = color;
                Station newStation = new Station(x, y, color, StationType.REGULAR);
                WorldSandboxScreen.getInstance().sandboxWorld.addStation(newStation);
                WorldSandboxScreen.getInstance().repaint();
                colorDialog.dispose();
                colorSelectionEnabled = false;
            });

            // Эффект при наведении
            colorBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    colorBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                }
                public void mouseExited(MouseEvent e) {
                    colorBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                }
            });

            colorPanel.add(colorBtn);
        }

        colorDialog.add(colorPanel);
        colorDialog.pack();

        // Позиционируем окно рядом с курсором
        colorDialog.setLocation(
                screenPos.x + 20, // Смещение от курсора
                screenPos.y + 20
        );

        // Закрытие при клике вне окна
        colorDialog.addWindowFocusListener(new WindowAdapter() {
            public void windowLostFocus(WindowEvent e) {
                colorDialog.dispose();
                colorSelectionEnabled = false;
            }
            @Override
            public void windowClosed(WindowEvent e) {
                colorDialog.dispose();
                colorSelectionEnabled = false; // Сбрасываем флаг при закрытии
            }
        });

        colorDialog.setVisible(true);
    }
    /**
     * Handles selection of stations/tunnels
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleDefaultClick(int x, int y) {
        // Сначала снимаем выделение со всех объектов
        deselectAll();

        Label label = WorldSandboxScreen.getInstance().sandboxWorld.getLabelAt(x, y);
        if (label != null) {
            label.setSelected(true);
            selectedObject = label;
            dragOffset = new PathPoint(x - label.getX(), y - label.getY());
            WorldSandboxScreen.getInstance().repaint();
            return;
        }

        // Проверяем станции
        Station station = WorldSandboxScreen.getInstance().sandboxWorld.getStationAt(x, y);
        if (station != null) {
            station.setSelected(true);
            selectedObject = station;
            dragOffset = new PathPoint(x - station.getX(), y - station.getY());
            WorldSandboxScreen.getInstance().repaint();
            return;
        }

        // Проверяем туннели
        Tunnel tunnel = WorldSandboxScreen.getInstance().sandboxWorld.getTunnelAt(x, y);
        if (tunnel != null) {
            // Ищем конкретную точку туннеля
            for (PathPoint p : tunnel.getPath()) {
                if (p.getX() == x && p.getY() == y) {
                    tunnel.setSelected(true);
                    selectedObject = tunnel;
                    dragOffset = new PathPoint(0, 0);
                    break;
                }
            }
            WorldSandboxScreen.getInstance().repaint();
            return;
        }

        if (WorldSandboxScreen.getInstance().isShiftPressed) {
            // Дополнительная проверка (хотя предыдущие проверки уже гарантируют пустую клетку)
            if (WorldSandboxScreen.getInstance().sandboxWorld.getStationAt(x, y) == null) {
                Station newStation = new Station(x, y, SandboxClickHandler.currentStationColor, StationType.REGULAR);
                WorldSandboxScreen.getInstance().sandboxWorld.addStation(newStation);
                checkForTransferStation(newStation);
                WorldSandboxScreen.getInstance().repaint();
            }
        }
    }
    public GameObject getSelectedObject() {
        return selectedObject;
    }

    /**
     * Deselects all game objects
     */
    private void deselectAll() {
        // Снимаем выделение со всех станций
        for (Station station : WorldSandboxScreen.getInstance().sandboxWorld.getStations()) {
            station.setSelected(false);
        }

        // Снимаем выделение со всех меток
        for (Label label : WorldSandboxScreen.getInstance().sandboxWorld.getLabels()) {
            label.setSelected(false);
        }

        // Снимаем выделение со всех туннелей
        for (Tunnel tunnel : WorldSandboxScreen.getInstance().sandboxWorld.getTunnels()) {
            tunnel.setSelected(false);
        }

        selectedObject = null;
    }

    /**
     * Edit station name
     */
    static void editStationName(Station station) {
        // Создаем панель с текстовым полем
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Station Name:");
        JTextField textField = new JTextField(station.getName(), 20);
        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);

        // Показываем диалоговое окно
        int result = JOptionPane.showConfirmDialog(
                WorldSandboxScreen.getInstance(),
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
                WorldSandboxScreen.getInstance().repaint();
            }
        }
    }
    /**
     * Handles station placement/removal
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleStationClick(int x, int y) {
        Station existing = WorldSandboxScreen.getInstance().sandboxWorld.getStationAt(x, y);
        if (existing != null) {
            if (!existing.selected && !getInstance().isShiftPressed) {
                existing.setSelected(true);
            } else if(getInstance().isShiftPressed) {
                WorldSandboxScreen.getInstance().sandboxWorld.removeStation(existing);
            }
            return; // Выходим, если станция уже существует
        }
        if (WorldSandboxScreen.getInstance().isCPressed) { // Проверяем зажат ли Ctrl+C
            showColorSelectionPopup(x, y);
            return; // Выходим, чтобы не создавать станцию дважды
        }

        // Создаем новую станцию с текущим цветом
        Station station = new Station(x, y, currentStationColor, StationType.REGULAR);
        WorldSandboxScreen.getInstance().sandboxWorld.addStation(station);
        checkForTransferStation(station);
    }

    /**
     * Checks if a station should be converted to a transfer station
     * @param station Station to check
     */
    private static void checkForTransferStation(Station station) {
        int x = station.getX();
        int y = station.getY();

        // Check all 8 directions
        for (int ny = Math.max(0, y-1); ny < Math.min(WorldSandboxScreen.getInstance().sandboxWorld.getHeight(), y+2); ny++) {
            for (int nx = Math.max(0, x-1); nx < Math.min(WorldSandboxScreen.getInstance().sandboxWorld.getWidth(), x+2); nx++) {
                if (nx == x && ny == y) continue;

                Station neighbor = WorldSandboxScreen.getInstance().sandboxWorld.getStationAt(nx, ny);
                if (neighbor != null && !neighbor.getColor().equals(station.getColor())) {
                    station.setType(StationType.TRANSFER);
                    neighbor.setType(StationType.TRANSFER);
                    return;
                }
            }
        }
    }

    /**
     * Starts dragging the view
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     */
    public static void startDrag(int x, int y) {
        dragging = true;
        dragStartX = x - WorldSandboxScreen.getInstance().offsetX;
        dragStartY = y - WorldSandboxScreen.getInstance().offsetY;
    }

    /**
     * Updates view during drag
     * @param x Current X coordinate
     * @param y Current Y coordinate
     */
    public static void updateDrag(int x, int y) {
        if (dragging) {
            WorldSandboxScreen.getInstance().offsetX = x - dragStartX;
            WorldSandboxScreen.getInstance().offsetY = y - dragStartY;
            WorldSandboxScreen.getInstance().repaint();
        }
    }

    /**
     * Stops dragging the view
     */
    public static void stopDrag() {
        dragging = false;
    }
}
