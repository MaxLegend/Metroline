package game.input;

import game.objects.Label;
import game.objects.PathPoint;
import game.objects.Station;
import game.objects.Tunnel;
import game.objects.enums.StationType;
import screens.WorldScreen;
import screens.WorldScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static screens.WorldScreen.*;

public class ClickHandler {
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
    public static void handleEditDrag(int x, int y) {
        if ( selectedObject != null && dragOffset != null) {
            // Проверяем границы
            if (x < 0 || x >= WorldScreen.getInstance().world.getWidth() || y < 0 || y >= WorldScreen.getInstance().world.getHeight()) {
                return;
            }

                if (selectedObject instanceof Label) {
                    Label label = (Label) selectedObject;
                    int newX = x - dragOffset.x;
                    int newY = y - dragOffset.y;

                    if (label.tryMoveTo(newX, newY)) {
                        WorldScreen.getInstance().repaint();
                    }
                    return;
                }

            if ( selectedObject instanceof Station) {
                Station station = (Station)selectedObject;
                int newX = x - dragOffset.x;
                int newY = y - dragOffset.y;

                // Check if new position is valid
                if (newX >= 0 && newX < WorldScreen.getInstance().world.getWidth() &&
                        newY >= 0 && newY < WorldScreen.getInstance().world.getHeight() &&
                        WorldScreen.getInstance().world.getStationAt(newX, newY) == null) {
                    Label label = WorldScreen.getInstance().world.getLabelForStation(station);
                    // Remove from old position
                    WorldScreen.getInstance().world.getGameGrid()[station.getX()][station.getY()].setContent(null);

                    // Update position
                    station.x = newX;
                    station.y = newY;

                    // Add to new position
                    WorldScreen.getInstance().world.getGameGrid()[newX][newY].setContent(station);

                    // Recalculate all connected tunnels
                    for (Tunnel t : WorldScreen.getInstance().world.getTunnels()) {
                        if (t.getStart() == station || t.getEnd() == station) {
                            t.calculatePath();
                        }
                    }
                    if (label != null) {
                        // Находим новую позицию для метки (рядом со станцией)
                        PathPoint newLabelPos = WorldScreen.getInstance().world.findFreePositionNear(station.getX(), station.getY(), station.getName());
                        if (newLabelPos != null) {
                            label.tryMoveTo(newLabelPos.x, newLabelPos.y);
                        }
                    }
                    WorldScreen.getInstance().repaint();
                }
            }
            else if (selectedObject instanceof Tunnel) {
                Tunnel tunnel = (Tunnel)selectedObject;

                // Проверяем, что новая позиция не совпадает со станцией
                Station stationAtPos = WorldScreen.getInstance().world.getStationAt(x, y);
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
    public void handleTunnelClick(int x, int y) {
        Station station = WorldScreen.getInstance().world.getStationAt(x, y);
        if (station == null) return;

        // Если есть уже выбранная станция и это не та же самая
        if (selectedStation != null && selectedStation != station) {
            // Создаём туннель
            Tunnel tunnel = new Tunnel(selectedStation, station);
            WorldScreen.getInstance().world.addTunnel(tunnel);

            // Снимаем выделение
            selectedStation.setSelected(false);
            selectedStation = null;
        }
        else {
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
        WorldScreen.getInstance().repaint();
    }

    public void showColorSelectionPopup(int x, int y) {
        // Создаем прозрачное безрамочное окно
        Window parentWindow = SwingUtilities.getWindowAncestor(WorldScreen.getInstance());
        JDialog colorDialog = new JDialog(parentWindow);
        colorDialog.setUndecorated(true); // Убираем рамку и заголовок
        colorDialog.setBackground(new Color(0, 0, 0, 0)); // Прозрачный фон

        JPanel colorPanel = new JPanel(new GridLayout(2, 2));
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2)); // Белая рамка
        colorPanel.setOpaque(true);
        colorPanel.setBackground(new Color(50, 50, 50)); // Темный фон

        // Конвертируем мировые координаты в экранные
        Point screenPos = WorldScreen.getInstance().worldToScreen(x, y);

        for (Color color : Station.COLORS) {
            JButton colorBtn = new JButton();
            colorBtn.setBackground(color);
            colorBtn.setPreferredSize(new Dimension(30, 30));
            colorBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Убираем стандартную рамку кнопки
            colorBtn.setContentAreaFilled(false); // Прозрачная область кнопки
            colorBtn.setOpaque(true); // Но сам цвет видимый
            colorBtn.setFocusPainted(false); // Убираем эффект фокуса

            colorBtn.addActionListener(e -> {
                ClickHandler.currentStationColor = color;
                Station newStation = new Station(x, y, color, StationType.REGULAR);
                WorldScreen.getInstance().world.addStation(newStation);
                WorldScreen.getInstance().repaint();
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

        Label label = WorldScreen.getInstance().world.getLabelAt(x, y);
        if (label != null) {
            label.setSelected(true);
            selectedObject = label;
            dragOffset = new PathPoint(x - label.getX(), y - label.getY());
            WorldScreen.getInstance().repaint();
            return;
        }

        // Проверяем станции
        Station station = WorldScreen.getInstance().world.getStationAt(x, y);
        if (station != null) {
            station.setSelected(true);
            selectedObject = station;
            dragOffset = new PathPoint(x - station.getX(), y - station.getY());
            WorldScreen.getInstance().repaint();
            return;
        }

        // Проверяем туннели
        Tunnel tunnel = WorldScreen.getInstance().world.getTunnelAt(x, y);
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
            WorldScreen.getInstance().repaint();
            return;
        }

        if (WorldScreen.getInstance().isShiftPressed) {
            // Дополнительная проверка (хотя предыдущие проверки уже гарантируют пустую клетку)
            if (WorldScreen.getInstance().world.getStationAt(x, y) == null) {
                Station newStation = new Station(x, y, ClickHandler.currentStationColor, StationType.REGULAR);
                WorldScreen.getInstance().world.addStation(newStation);
                checkForTransferStation(newStation);
                WorldScreen.getInstance().repaint();
            }
        }
    }

    /**
     * Deselects all game objects
     */
    private static void deselectAll() {
        // Снимаем выделение со всех станций
        for (Station station : WorldScreen.getInstance().world.getStations()) {
            station.setSelected(false);
        }

        // Снимаем выделение со всех меток
        for (Label label : WorldScreen.getInstance().world.getLabels()) {
            label.setSelected(false);
        }

        // Снимаем выделение со всех туннелей
        for (Tunnel tunnel : WorldScreen.getInstance().world.getTunnels()) {
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
    public void handleStationClick(int x, int y) {
        Station existing = WorldScreen.getInstance().world.getStationAt(x, y);
        if (existing != null) {
            if (!existing.selected) {
                existing.setSelected(true);
            }
            return; // Выходим, если станция уже существует
        }
        if (WorldScreen.getInstance().isCPressed) { // Проверяем зажат ли Ctrl+C
            showColorSelectionPopup(x, y);
            return; // Выходим, чтобы не создавать станцию дважды
        }

        // Создаем новую станцию с текущим цветом
        Station station = new Station(x, y, currentStationColor, StationType.REGULAR);
        WorldScreen.getInstance().world.addStation(station);
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
        for (int ny = Math.max(0, y-1); ny < Math.min(WorldScreen.getInstance().world.getHeight(), y+2); ny++) {
            for (int nx = Math.max(0, x-1); nx < Math.min(WorldScreen.getInstance().world.getWidth(), x+2); nx++) {
                if (nx == x && ny == y) continue;

                Station neighbor = WorldScreen.getInstance().world.getStationAt(nx, ny);
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
        dragStartX = x - WorldScreen.getInstance().offsetX;
        dragStartY = y - WorldScreen.getInstance().offsetY;
    }

    /**
     * Updates view during drag
     * @param x Current X coordinate
     * @param y Current Y coordinate
     */
    public static void updateDrag(int x, int y) {
        if (dragging) {
            WorldScreen.getInstance().offsetX = x - dragStartX;
            WorldScreen.getInstance().offsetY = y - dragStartY;
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
