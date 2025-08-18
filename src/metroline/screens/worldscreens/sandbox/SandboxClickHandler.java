package metroline.screens.worldscreens.sandbox;

import metroline.objects.enums.StationColors;
import metroline.objects.gameobjects.GameObject;
import metroline.core.time.GameTime;
import metroline.objects.gameobjects.*;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TunnelType;
import metroline.objects.gameobjects.Label;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.LngUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;


import static metroline.screens.worldscreens.sandbox.SandboxWorldScreen.*;

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
    public static PathPoint dragOffset = null;

    private static StationColors currentStationColor = StationColors.RED;
     boolean colorSelectionEnabled = false;

    /**
     * Handles mouse drag in edit mode
     * @param x X coordinate
     * @param y Y coordinate
     */
    public  void handleEditDrag(int x, int y) {
        if ( selectedObject != null && dragOffset != null) {
            // Проверяем границы
            if (x < 0 || x >= SandboxWorldScreen.getInstance().getWorld().getWidth() || y < 0 || y >= SandboxWorldScreen.getInstance().getWorld().getHeight()) {
                return;
            }

                if (selectedObject instanceof Label) {

                    Label label = (Label) selectedObject;
                    int newX = x - dragOffset.x;
                    int newY = y - dragOffset.y;

                    if (label.tryMoveTo(newX, newY)) {
                        SandboxWorldScreen.getInstance().repaint();
                    }
                    return;
                }

            if ( selectedObject instanceof Station) {
                Station station = (Station)selectedObject;

                int newX = x - dragOffset.x;
                int newY = y - dragOffset.y;

                // Check if new position is valid
                if (newX >= 0 && newX < SandboxWorldScreen.getInstance().getWorld().getWidth() &&
                        newY >= 0 && newY < SandboxWorldScreen.getInstance().getWorld().getHeight() &&
                        SandboxWorldScreen.getInstance().getWorld().getStationAt(newX, newY) == null) {
                    Label label = SandboxWorldScreen.getInstance().getWorld().getLabelForStation(station);
                    // Remove from old position
                    SandboxWorldScreen.getInstance().getWorld().getGameGrid()[station.getX()][station.getY()].setContent(null);

                    // Update position
                    station.x = newX;
                    station.y = newY;

                    // Add to new position
                    SandboxWorldScreen.getInstance().getWorld().getGameGrid()[newX][newY].setContent(station);

                    // Recalculate all connected tunnels
                    for (Tunnel t : SandboxWorldScreen.getInstance().getWorld().getTunnels()) {
                        if (t.getStart() == station || t.getEnd() == station) {
                            t.calculatePath();
                        }
                    }
                    if (label != null) {
                        // Находим новую позицию для метки (рядом со станцией)
                        PathPoint newLabelPos = SandboxWorldScreen.getInstance().getWorld().findFreePositionNear(station.getX(), station.getY(), station.getName());
                        if (newLabelPos != null) {
                            label.tryMoveTo(newLabelPos.x, newLabelPos.y);
                        }
                    }
                    SandboxWorldScreen.getInstance().repaint();
                }
            }
            else if (selectedObject instanceof Tunnel) {

                Tunnel tunnel = (Tunnel)selectedObject;

                // Проверяем, что новая позиция не совпадает со станцией
                Station stationAtPos = SandboxWorldScreen.getInstance().getWorld().getStationAt(x, y);
                if (stationAtPos == null) {
                    tunnel.moveControlPoint(x, y);
                    SandboxWorldScreen.getInstance().repaint();
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
            SandboxWorldScreen.getInstance().getWorld().removeStation((Station)selectedObject);
        }
        else if (selectedObject instanceof Tunnel) {
            SandboxWorldScreen.getInstance().getWorld().removeTunnel((Tunnel)selectedObject);
        }
        else if (selectedObject instanceof Label) {
            SandboxWorldScreen.getInstance().getWorld().removeLabel((Label)selectedObject);
        }

        selectedObject = null;
        SandboxWorldScreen.getInstance().repaint();
    }
    public void handleRemoveTunnel(int worldX, int worldY) {
        Tunnel tunnel = SandboxWorldScreen.getInstance().getWorld().getTunnelAt(worldX, worldY);
        if (tunnel != null) {
            SandboxWorldScreen.getInstance().getWorld().removeTunnel(tunnel);
            SandboxWorldScreen.getInstance().repaint();
        }
    }
    public void handleEditStationName(int x, int y) {
        Station station = SandboxWorldScreen.getInstance().getWorld().getStationAt(x, y);
        Tunnel t = SandboxWorldScreen.getInstance().getWorld().getTunnelAt(x, y);
        if (station != null) {
            Point mouseWorldPos = new Point(station.getX(), station.getY());
            editStationName(station, mouseWorldPos );
        }
    }
    /**
     * Handles tunnel creation
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleTunnelClick(int x, int y) {

        Station station = SandboxWorldScreen.getInstance().getWorld().getStationAt(x, y);
        if (station == null) return;

        // Если есть уже выбранная станция и это не та же самая

           if (selectedStation != null && selectedStation != station) {
               // Создаём туннель
               Tunnel tunnel = new Tunnel(SandboxWorldScreen.getInstance().getWorld(), selectedStation, station, TunnelType.ACTIVE);
               SandboxWorldScreen.getInstance().getWorld().addTunnel(tunnel);

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

        SandboxWorldScreen.getInstance().repaint();
    }

    /**
     * Показ выбора цвета
     */
    public void showColorSelectionPopup(int x, int y) {
        Window parentWindow = SwingUtilities.getWindowAncestor(GameWorldScreen.getInstance());
        JDialog colorDialog = new JDialog(parentWindow);
        colorDialog.setUndecorated(true);
        colorDialog.setModal(false);

        // Создаем панель с темной подложкой и скругленными углами
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 0));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2d.setColor(new Color(80, 80, 80, 70));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2d.dispose();
            }
        };

        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setOpaque(false);

        // Панель для цветов
        JPanel colorPanel = new JPanel(new GridLayout(4, 4, 5, 5));
        colorPanel.setOpaque(false);

        for (StationColors color : StationColors.values()) {
            JButton colorBtn = createColorButton(color.getColor(), x, y, colorDialog);
            colorPanel.add(colorBtn);
        }

        mainPanel.add(colorPanel, BorderLayout.CENTER);
        colorDialog.add(mainPanel);

        // Рассчитываем позицию как для InfoWindow
        Point screenPoint = GameWorldScreen.getInstance().worldToScreen(x, y);
        Point windowPoint = new Point(screenPoint);
        SwingUtilities.convertPointToScreen(windowPoint, GameWorldScreen.getInstance());
        colorDialog.setLocation(windowPoint.x + 20, windowPoint.y + 20);

        // Обработчики закрытия
        colorDialog.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                colorDialog.dispose();
            }
        });

        colorDialog.pack();
        colorDialog.setVisible(true);
    }

    private JButton createColorButton(Color colorButton, int x, int y, JDialog dialog) {
        JButton colorBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Рисуем скругленную кнопку
                g2d.setColor(colorButton);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                // Обводка при наведении
                if (getModel().isRollover()) {
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 20, 20);
                }
                g2d.dispose();
            }
        };

        colorBtn.setPreferredSize(new Dimension(30, 30));
        colorBtn.setContentAreaFilled(false);
        colorBtn.setOpaque(false);
        colorBtn.setFocusPainted(false);
        colorBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        colorBtn.addActionListener(e -> {
            currentStationColor = StationColors.fromColor(colorButton);
            Station newStation = new Station(
                    SandboxWorldScreen.getInstance().getWorld(),
                    x, y, StationColors.fromColor(colorButton), StationType.REGULAR
            );
            SandboxWorldScreen.getInstance().getWorld().addStation(newStation);
            SandboxWorldScreen.getInstance().repaint();
            dialog.dispose();
        });

        return colorBtn;
    }
    /**
     * Handles selection of stations/tunnels
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleDefaultClick(int x, int y) {
        // Сначала снимаем выделение со всех объектов
        deselectAll();

        Label label = SandboxWorldScreen.getInstance().getWorld().getLabelAt(x, y);
        if (label != null) {
            label.setSelected(true);
            selectedObject = label;
            dragOffset = new PathPoint(x - label.getX(), y - label.getY());
            SandboxWorldScreen.getInstance().repaint();
            return;
        }

        // Проверяем станции
        Station station = SandboxWorldScreen.getInstance().getWorld().getStationAt(x, y);
        if (station != null) {
            station.setSelected(true);
            selectedObject = station;
            dragOffset = new PathPoint(x - station.getX(), y - station.getY());
            SandboxWorldScreen.getInstance().repaint();
            return;
        }

        // Проверяем туннели
        Tunnel tunnel = SandboxWorldScreen.getInstance().getWorld().getTunnelAt(x, y);
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
            SandboxWorldScreen.getInstance().repaint();
            return;
        }

        if (SandboxWorldScreen.getInstance().isShiftPressed) {
            // Дополнительная проверка (хотя предыдущие проверки уже гарантируют пустую клетку)
            if (SandboxWorldScreen.getInstance().getWorld().getStationAt(x, y) == null) {
                Station newStation = new Station(SandboxWorldScreen.getInstance().getWorld(), x, y, SandboxClickHandler.currentStationColor, StationType.REGULAR);
                SandboxWorldScreen.getInstance().getWorld().addStation(newStation);
                checkForTransferStation(newStation);
                SandboxWorldScreen.getInstance().repaint();
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
        for (Station station : SandboxWorldScreen.getInstance().getWorld().getStations()) {
            station.setSelected(false);
        }

        // Снимаем выделение со всех меток
        for (Label label : SandboxWorldScreen.getInstance().getWorld().getLabels()) {
            label.setSelected(false);
        }

        // Снимаем выделение со всех туннелей
        for (Tunnel tunnel : SandboxWorldScreen.getInstance().getWorld().getTunnels()) {
            tunnel.setSelected(false);
        }

        selectedObject = null;
    }

    /**
     * Edit station name
     */
    static void editStationName(Station station, Point mouseWorldPos) {
        // Получаем экранные координаты из мировых
        Point screenPos = SandboxWorldScreen.getInstance().worldToScreen(
                mouseWorldPos.x,
                mouseWorldPos.y
        );

        // Конвертируем в координаты окна
        Point windowPos = new Point(screenPos);
        SwingUtilities.convertPointToScreen(windowPos, SandboxWorldScreen.getInstance());

        // Создаем кастомное окно
        JDialog dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        // Основная панель
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 45));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Текстовое поле
        JTextField textField = new JTextField(station.getName(), 15);
        textField.setForeground(Color.WHITE);
        textField.setBackground(new Color(60, 60, 60));
        textField.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        textField.setFont(new Font("Sans Serif", Font.PLAIN, 14));
        textField.setCaretColor(Color.WHITE);
        textField.selectAll();

        // Обработчики событий
        textField.addActionListener(e -> {
            applyNameChange(station, textField.getText().trim());
            dialog.dispose();
        });

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyNameChange(station, textField.getText().trim());
                dialog.dispose();
            }
        });

        panel.add(textField, BorderLayout.CENTER);
        dialog.add(panel);
        dialog.pack();

        // Позиционируем окно рядом с курсором (смещение 20px вправо и вниз)
        dialog.setLocation(
                windowPos.x + 20,
                windowPos.y + 20
        );

        // Закрытие при клике вне окна
        dialog.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!panel.contains(e.getPoint())) {
                    dialog.dispose();
                }
            }
        });

        dialog.setVisible(true);
        textField.requestFocusInWindow();
    }

    private static void applyNameChange(Station station, String newName) {
        if (!newName.isEmpty() && !newName.equals(station.getName())) {
            station.setName(newName);
            SandboxWorldScreen.getInstance().repaint();
        }
    }
    /**
     * Handles station placement/removal
     * @param x X coordinate
     * @param y Y coordinate
     */
    public void handleStationClick(int x, int y) {
        Station existing = SandboxWorldScreen.getInstance().getWorld().getStationAt(x, y);
        if (existing != null) {
            if (!existing.selected && !getInstance().isShiftPressed) {
                existing.setSelected(true);
            } else if(getInstance().isShiftPressed) {
                SandboxWorldScreen.getInstance().getWorld().removeStation(existing);
            }
            return; // Выходим, если станция уже существует
        }
        if (SandboxWorldScreen.getInstance().isCPressed) { // Проверяем зажат ли Ctrl+C
            showColorSelectionPopup(x, y);
            return; // Выходим, чтобы не создавать станцию дважды
        }

        // Создаем новую станцию с текущим цветом
        Station station = new Station(SandboxWorldScreen.getInstance().getWorld(), x, y, currentStationColor, StationType.REGULAR);
        SandboxWorldScreen.getInstance().getWorld().addStation(station);
        checkForTransferStation(station);
    }

    /**
     * Checks if a station should be converted to a transfer station
     * @param station Station to check
     */
    private static void checkForTransferStation(Station station) {
        int x = station.getX();
        int y = station.getY();
        GameTime gametime = SandboxWorldScreen.getInstance().getWorld().getGameTime();
        // Check all 8 directions
        for (int ny = Math.max(0, y-1); ny < Math.min(SandboxWorldScreen.getInstance().getWorld().getHeight(), y+2); ny++) {
            for (int nx = Math.max(0, x-1); nx < Math.min(SandboxWorldScreen.getInstance().getWorld().getWidth(), x+2); nx++) {
                if (nx == x && ny == y) continue;

                Station neighbor = SandboxWorldScreen.getInstance().getWorld().getStationAt(nx, ny);
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
        dragStartX = x - SandboxWorldScreen.getInstance().offsetX;
        dragStartY = y - SandboxWorldScreen.getInstance().offsetY;
    }

    /**
     * Updates view during drag
     * @param x Current X coordinate
     * @param y Current Y coordinate
     */
    public static void updateDrag(int x, int y) {
        if (dragging) {
            SandboxWorldScreen.getInstance().offsetX = x - dragStartX;
            SandboxWorldScreen.getInstance().offsetY = y - dragStartY;
            SandboxWorldScreen.getInstance().repaint();
        }
    }

    /**
     * Stops dragging the view
     */
    public static void stopDrag() {
        dragging = false;
    }
}
