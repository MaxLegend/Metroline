package metroline.screens.worldscreens.sandbox;

import metroline.core.world.SandboxWorld;
import metroline.core.world.SandboxWorld;
import metroline.input.KeyboardController;
import metroline.input.selection.GameSelectionListener;
import metroline.input.selection.Selectable;
import metroline.input.selection.SelectionManager;
import metroline.objects.enums.StationColors;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TunnelType;
import metroline.objects.gameobjects.*;
import metroline.screens.render.StationPositionCache;
import metroline.util.MetroLogger;
import metroline.util.ui.MetrolinePopupMenu;
import metroline.util.ui.MetrolineButton;
import metroline.util.ui.tooltip.CursorTooltip;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static metroline.objects.gameobjects.GameConstants.COST_STANDART_TRAIN;

/**
 * Main world click handler for Sandbox mode
 * @author Tesmio
 */
public class SandboxClickHandler {
    public static PathPoint dragOffset = null;
    private static int dragStartX;
    private static int dragStartY;
    private static boolean dragging = false;
    private static StationColors currentStationColor = StationColors.RED;

    public SandboxWorldScreen screen;

    public SandboxClickHandler(SandboxWorldScreen screen) {
        this.screen = screen;
        SelectionManager.getInstance().addListener(new GameSelectionListener(screen.parent));
    }

    /*********************************
     * ОСНОВНЫЕ МЕТОДЫ ОБРАБОТКИ КЛИКОВ
     *********************************/

    /**
     * Основной обработчик кликов
     */
    public void mainClickHandler(int x, int y) {
        KeyboardController keyboard = KeyboardController.getInstance();
        boolean isShiftPressed = keyboard.isKeyPressed(KeyEvent.VK_SHIFT);
        boolean isCtrlPressed = keyboard.isKeyPressed(KeyEvent.VK_CONTROL);
        boolean isAltPressed = keyboard.isKeyPressed(KeyEvent.VK_ALT);
        boolean isCPressed = keyboard.isKeyPressed(KeyEvent.VK_C);

        if (isAltPressed || screen.isAltPressed) {
            handleAltClick(x, y);
        } else if (isShiftPressed && isCPressed || screen.isShiftPressed && screen.isCPressed) {
            showColorSelectionPopup(x, y);
        } else if (isShiftPressed || screen.isShiftPressed) {
            handleShiftClick(x, y);
        } else if (isCtrlPressed || screen.isCtrlPressed) {
            handleCtrlClick(x, y);
        } else {
            handleDefaultLeftClick(x, y);
        }
    }

    /*********************************
     * ОБРАБОТЧИКИ КОНКРЕТНЫХ ДЕЙСТВИЙ
     *********************************/

    /**
     * Alt+Click - удаление объектов
     */
    public void handleAltClick(int x, int y) {
        SandboxWorld world = (SandboxWorld) SandboxWorldScreen.getInstance().getWorld();

        // Проверяем объекты в порядке приоритета
        Station station = world.getStationAt(x, y);
        if (station != null) {
            world.removeStation(station);
            SandboxWorldScreen.getInstance().repaint();
            return;
        }

        Tunnel tunnel = world.getTunnelAt(x, y);
        if (tunnel != null) {
            world.removeTunnel(tunnel);
            SandboxWorldScreen.getInstance().repaint();
            return;
        }

        StationLabel label = world.getLabelAt(x, y);
        if (label != null) {
            world.removeLabel(label);
            SandboxWorldScreen.getInstance().repaint();
            return;
        }
    }

    /**
     * Ctrl+Click - создание туннелей
     */
    public void handleCtrlClick(int x, int y) {
        SandboxWorld world = (SandboxWorld) SandboxWorldScreen.getInstance().getWorld();
        Station station = world.getStationAt(x, y);

        if (station == null) return;

        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();

        if (selected != null && !(selected instanceof Station)) {
            selectionManager.deselect();
            return;
        }

        if (selected != null && selected != station) {
            Station selectedStation = (Station) selected;

            // Создаем туннель между двумя станциями
            Tunnel tunnel = new Tunnel(world, selectedStation, station, TunnelType.ACTIVE);
            world.addTunnel(tunnel);

            // Снимаем выделение
            selectionManager.deselect();
        } else {
            if (selectionManager.isSelected(station)) {
                selectionManager.deselect();
            } else {
                selectionManager.select(station);
            }
        }

        SandboxWorldScreen.getInstance().repaint();
    }

    /**
     * Shift+Click - строительство станций
     */
    public void handleShiftClick(int x, int y) {
        Station existing = SandboxWorldScreen.getInstance().getWorld().getStationAt(x, y);

        if (existing != null) {
            handleExistingStation(existing);
        } else {
            handleNewStation(x, y);
        }
    }

    /**
     * Обычный клик - выбор объектов
     */
    public void handleDefaultLeftClick(int x, int y) {
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();

        // Проверяем объекты в порядке приоритета
        if (trySelectLabel(x, y)) return;
        if (trySelectStation(x, y)) return;
        if (trySelectTunnel(x, y)) return;

        // Если кликнули на пустое место - снимаем выделение
        selectionManager.deselect();
    }

    /**
     * Обработка правого клика
     */
    public void handleRightClick(int x, int y) {
        KeyboardController keyboard = KeyboardController.getInstance();
        boolean isShiftPressed = keyboard.isKeyPressed(KeyEvent.VK_SHIFT);

        SandboxWorld world = (SandboxWorld) SandboxWorldScreen.getInstance().getWorld();
        GameObject clickedObject = world.getGameObjectAt(x, y);

        if (clickedObject instanceof Station) {
            Station station = (Station) clickedObject;

            // Shift+ПКМ - превращение в депо
            if (isShiftPressed || screen.isShiftPressed) {
                if (station.getType() != StationType.DEPO) {
                    station.setType(StationType.DEPO);
                    SandboxWorldScreen.getInstance().repaint();
                }
            } else {
                    editStationName(station, new Point(x, y));

            }
        }
    }

    /*********************************
     * ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
     *********************************/

    /**
     * Обработка существующей станции
     */
    private void handleExistingStation(Station station) {
        SelectionManager selectionManager = SelectionManager.getInstance();

        if (!selectionManager.isSelected(station)) {
            selectionManager.select(station);
        } else {
            selectionManager.deselect();
        }

        SandboxWorldScreen.getInstance().repaint();
    }

    /**
     * Создание новой станции
     */
    private void handleNewStation(int x, int y) {
        if (!isPositionValidForStation(x, y)) {
            return;
        }

        if (SandboxWorldScreen.getInstance().isCPressed) {
            showColorSelectionPopup(x, y);
        } else {
            Station station = new Station(
                    SandboxWorldScreen.getInstance().getWorld(),
                    x, y,
                    currentStationColor,
                    StationType.REGULAR
            );
            SandboxWorldScreen.getInstance().getWorld().addStation(station);
            checkForTransferStation(station);
            SandboxWorldScreen.getInstance().repaint();
        }
    }

    /**
     * Попытка выбрать метку
     */
    private boolean trySelectLabel(int x, int y) {
        StationLabel stationLabel = SandboxWorldScreen.getInstance().getWorld().getLabelAt(x, y);
        if (stationLabel != null) {
            selectObject(stationLabel, x, y);
            return true;
        }
        return false;
    }

    /**
     * Попытка выбрать станцию
     */
    private boolean trySelectStation(int x, int y) {
        Station station = SandboxWorldScreen.getInstance().getWorld().getStationAt(x, y);
        if (station != null) {
            selectObject(station, x, y);
            return true;
        }
        return false;
    }

    /**
     * Попытка выбрать туннель
     */
    private boolean trySelectTunnel(int x, int y) {
        Tunnel tunnel = SandboxWorldScreen.getInstance().getWorld().getTunnelAt(x, y);
        if (tunnel != null) {
            for (PathPoint p : tunnel.getPath()) {
                if (p.getX() == x && p.getY() == y) {
                    selectObject(tunnel, x, y);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Выбор объекта
     */
    private void selectObject(GameObject obj, int clickX, int clickY) {
        SelectionManager.getInstance().select(obj);
        dragOffset = new PathPoint(clickX - obj.getX(), clickY - obj.getY());
        SandboxWorldScreen.getInstance().repaint();
    }

    /**
     * Обработка перетаскивания
     */
    public void handleEditDrag(int x, int y) {
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();
        if (dragOffset == null) return;

        // Проверяем границы мира
        if (!isWithinWorldBounds(x, y)) return;

        if (selected instanceof StationLabel) {
            handleLabelDrag((StationLabel) selected, x, y);
        } else if (selected instanceof Station) {
            handleStationDrag((Station) selected, x, y);
        } else if (selected instanceof Tunnel) {
            handleTunnelDrag((Tunnel) selected, x, y);
        }
    }

    /**
     * Перетаскивание метки
     */
    private void handleLabelDrag(StationLabel stationLabel, int x, int y) {
        int newX = x - dragOffset.x;
        int newY = y - dragOffset.y;

        if (stationLabel.tryMoveTo(newX, newY)) {
            SandboxWorldScreen.getInstance().repaint();
        }
    }

    /**
     * Перетаскивание станции
     */
    private void handleStationDrag(Station station, int x, int y) {
        int newX = x - dragOffset.x;
        int newY = y - dragOffset.y;

        if (isPositionValidForStation(newX, newY)) {
            moveStationTo(station, newX, newY);
        }
    }

    /**
     * Перетаскивание туннеля
     */
    private void handleTunnelDrag(Tunnel tunnel, int x, int y) {
        // Проверяем, что новая позиция не совпадает со станцией
        Station stationAtPos = SandboxWorldScreen.getInstance().getWorld().getStationAt(x, y);
        if (stationAtPos == null) {
            tunnel.moveControlPoint(x, y);
            SandboxWorldScreen.getInstance().repaint();
        }
    }

    /**
     * Перемещение станции
     */
    private void moveStationTo(Station station, int newX, int newY) {
        SandboxWorld world = (SandboxWorld) SandboxWorldScreen.getInstance().getWorld();

        // Удаляем из старой позиции
        world.getGameTile(station.getX(), station.getY()).setContent(null);

        // Обновляем позицию
        station.x = newX;
        station.y = newY;

        // Добавляем в новую позицию
        world.getGameTile(newX, newY).setContent(station);

        // Пересчитываем все связанные туннели
        for (Tunnel t : world.getTunnels()) {
            if (t.getStart() == station || t.getEnd() == station) {
                t.calculatePath();
            }
        }

        // Перемещаем метку станции
        StationLabel stationLabel = world.getLabelForStation(station);
        if (stationLabel != null) {
            PathPoint newLabelPos = world.findFreePositionNear(station.getX(), station.getY(), station.getName());
            if (newLabelPos != null) {
                stationLabel.tryMoveTo(newLabelPos.x, newLabelPos.y);
            }
        }

        SandboxWorldScreen.getInstance().repaint();
    }

    /**
     * Проверка валидности позиции для станции
     */
    private boolean isPositionValidForStation(int x, int y) {
        SandboxWorld world = (SandboxWorld) SandboxWorldScreen.getInstance().getWorld();

        // Проверяем границы мира
        if (x < 0 || x >= world.getWidth() || y < 0 || y >= world.getHeight()) {
            return false;
        }

        // Проверяем, что клетка свободна
        return world.getStationAt(x, y) == null;
    }

    /**
     * Проверка границ мира
     */
    private boolean isWithinWorldBounds(int x, int y) {
        return x >= 0 && x < SandboxWorldScreen.getInstance().getWorld().getWidth() &&
                y >= 0 && y < SandboxWorldScreen.getInstance().getWorld().getHeight();
    }

    /**
     * Проверка на трансферную станцию
     */
    private static void checkForTransferStation(Station station) {
        int x = station.getX();
        int y = station.getY();

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
     * Показ выбора цвета
     */
    public void showColorSelectionPopup(int x, int y) {
        Window parentWindow = SwingUtilities.getWindowAncestor(SandboxWorldScreen.getInstance());
        JDialog colorDialog = new JDialog(parentWindow);
        colorDialog.setUndecorated(true);
        colorDialog.setModal(false);

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

        JPanel colorPanel = new JPanel(new GridLayout(4, 4, 5, 5));
        colorPanel.setOpaque(false);

        for (StationColors color : StationColors.values()) {
            JButton colorBtn = createColorButton(color.getColor(), x, y, colorDialog);
            colorPanel.add(colorBtn);
        }

        mainPanel.add(colorPanel, BorderLayout.CENTER);
        colorDialog.add(mainPanel);

        Point screenPoint = SandboxWorldScreen.getInstance().worldToScreen(x, y);
        Point windowPoint = new Point(screenPoint);
        SwingUtilities.convertPointToScreen(windowPoint, SandboxWorldScreen.getInstance());
        colorDialog.setLocation(windowPoint.x + 20, windowPoint.y + 20);

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
                g2d.setColor(colorButton);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

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
     * Редактирование имени станции
     */
    static void editStationName(Station station, Point mouseWorldPos) {
        Point screenPos = SandboxWorldScreen.getInstance().worldToScreen(
                mouseWorldPos.x,
                mouseWorldPos.y
        );

        Point windowPos = new Point(screenPos);
        SwingUtilities.convertPointToScreen(windowPos, SandboxWorldScreen.getInstance());

        JDialog dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 45));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JTextField textField = new JTextField(station.getName(), 15);
        textField.setForeground(Color.WHITE);
        textField.setBackground(new Color(60, 60, 60));
        textField.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        textField.setFont(new Font("Sans Serif", Font.PLAIN, 14));
        textField.setCaretColor(Color.WHITE);
        textField.selectAll();

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
        dialog.setLocation(windowPos.x + 20, windowPos.y + 20);

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
     * Удаление выбранного объекта
     */
    public void deleteSelectedObject() {
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();

        if (selected == null) return;

        if (selected instanceof Station) {
            SandboxWorldScreen.getInstance().getWorld().removeStation((Station)selected);
        } else if (selected instanceof Tunnel) {
            SandboxWorldScreen.getInstance().getWorld().removeTunnel((Tunnel)selected);
        } else if (selected instanceof StationLabel) {
            SandboxWorldScreen.getInstance().getWorld().removeLabel((StationLabel)selected);
        }

        selectionManager.deselect();
        SandboxWorldScreen.getInstance().repaint();
    }

    /**
     * Получение выбранного объекта
     */
    public Selectable getSelectedObject() {
        return SelectionManager.getInstance().getSelected();
    }

    /**
     * Снятие выделения со всех объектов
     */
    public void deselectAll() {
        SelectionManager.getInstance().deselect();
    }

    /**
     * Начало перетаскивания вида
     */
    public static void startDrag(int x, int y) {
        dragging = true;
        dragStartX = x - SandboxWorldScreen.getInstance().offsetX;
        dragStartY = y - SandboxWorldScreen.getInstance().offsetY;
    }

    /**
     * Обновление вида при перетаскивании
     */
    public static void updateDrag(int x, int y) {
        if (dragging) {
            SandboxWorldScreen.getInstance().offsetX = x - dragStartX;
            SandboxWorldScreen.getInstance().offsetY = y - dragStartY;
            SandboxWorldScreen.getInstance().repaint();
        }
    }

    /**
     * Остановка перетаскивания вида
     */
    public static void stopDrag() {
        dragging = false;
    }
}