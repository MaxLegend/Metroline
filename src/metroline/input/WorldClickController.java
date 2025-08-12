package metroline.input;

import metroline.core.world.GameWorld;
import metroline.objects.enums.StationColors;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TunnelType;
import metroline.objects.gameobjects.*;
import metroline.objects.gameobjects.Label;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.WorldGameScreen;
import metroline.util.MetroLogger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class WorldClickController {
    // Выбранные объекты
    public static Station selectedStation = null;
    public GameObject selectedObject = null;
    public static PathPoint dragOffset = null;

    // Ссылки
    public WorldGameScreen screen;
    private static StationColors currentStationColor = StationColors.RED;

    // Константы стоимости
    public static final int STATION_BASE_COST = 100;
    public static final int TUNNEL_COST_PER_SEGMENT = 10;

    public WorldClickController(WorldGameScreen screen) {
        this.screen = screen;
    }
    /*********************************
     * ОСНОВНЫЕ МЕТОДЫ ОБРАБОТКИ КЛИКОВ
     *********************************/

    /**
     * Основной обработчик кликов
     */
    public void mainClickHandler(int x, int y) {
        if (screen.isAltPressed && screen.isShiftPressed) {
            handleAltShiftClick(x, y); // Alt+Shift - разрушение станции
        } else if (screen.isAltPressed) {
            handleAltClick(x, y); // Alt - альтернативные действия
        } else if (screen.isShiftPressed && screen.isCPressed) {
            ((GameWorld)screen.getWorld()).updateLegendWindow();
            showColorSelectionPopup(x, y); // Shift+C - выбор цвета
        } else if (screen.isShiftPressed) {
            ((GameWorld)screen.getWorld()).updateLegendWindow();
            handleShiftClick(x, y); // Shift - строительство/удаление
        } else if (screen.isCtrlPressed) {
            handleCtrlClick(x, y); // Ctrl - создание туннелей
        } else {

            handleDefaultLeftClick(x, y); // Обычный клик - выбор объектов

        }
    }
    public void handleAltShiftClick(int x, int y) {
        Station station = WorldGameScreen.getInstance().getWorld().getStationAt(x, y);
        if (station != null && (station.getType() != StationType.PLANNED || station.getType() != StationType.BUILDING) ) {
            GameWorld gameWorld = (GameWorld) WorldGameScreen.getInstance().getWorld();
            gameWorld.startDestroyingStation(station);
            WorldGameScreen.getInstance().repaint();
        }
    }


    /*********************************
     * ОБРАБОТЧИКИ КОНКРЕТНЫХ ДЕЙСТВИЙ
     *********************************/

    /**
     * Alt+Click - переключение между PLANNED и BUILDING
     */
    public void handleAltClick(int x, int y) {
        GameWorld world = (GameWorld) WorldGameScreen.getInstance().getWorld();
        Station station = world.getStationAt(x, y);

        if (station != null && station.getType() == StationType.PLANNED) {
            int stationCost = calculateStationCost(station);
            if (world.canAfford(stationCost)) {
                world.addMoney(-stationCost);
                station.setType(StationType.BUILDING);
                world.addStation(station);
            }
        }
        WorldGameScreen.getInstance().repaint();
    }

    /**
     * Ctrl+Click - создание туннелей
     */
    public void handleCtrlClick(int x, int y) {
        GameWorld world = (GameWorld) WorldGameScreen.getInstance().getWorld();
        Station station = world.getStationAt(x, y);

        if (station == null) return;

        if (selectedStation != null && selectedStation != station) {
            // Создаем туннель между двумя станциями
            Tunnel tunnel = new Tunnel(world, selectedStation, station, TunnelType.PLANNED);
            world.addTunnel(tunnel);

            // Снимаем выделение
            selectedStation.setSelected(false);
            selectedStation = null;
        } else {
            // Выбираем/снимаем выделение со станции
            if (selectedStation == station) {
                station.setSelected(false);
                selectedStation = null;
            } else {
                deselectAll();
                station.setSelected(true);
                selectedStation = station;
            }
        }

        WorldGameScreen.getInstance().repaint();
    }

    /**
     * Shift+Click - строительство/удаление станций
     */
    public void handleShiftClick(int x, int y) {
        Station existing = WorldGameScreen.getInstance().getWorld().getStationAt(x, y);

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
        deselectAll();

        // Проверяем объекты в порядке приоритета
        if (trySelectLabel(x, y)) return;
        if (trySelectStation(x, y)) return;
        if (trySelectTunnel(x, y)) return;
    }

    /**
     * Обработка перетаскивания
     */
    public void handleEditDrag(int x, int y) {
        if (selectedObject == null || dragOffset == null) return;

        // Проверяем границы мира
        if (!isWithinWorldBounds(x, y)) return;

        if (selectedObject instanceof Label) {
            handleLabelDrag(x, y);
        } else if (selectedObject instanceof Station) {
            handleStationDrag(x, y);
        } else if (selectedObject instanceof Tunnel) {
            handleTunnelDrag(x, y);
        }
    }

    /*********************************
     * ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
     *********************************/

    /**
     * Обработка существующей станции
     */
    private void handleExistingStation(Station station) {
        if (screen.isAltPressed) {
            // Alt+Shift - начало разрушения станции
            GameWorld gameWorld = (GameWorld) WorldGameScreen.getInstance().getWorld();
            gameWorld.startDestroyingStation(station);
        } else {
            // Обычный Shift - изменение типа станции
            if (station.getType() == StationType.PLANNED) {
                WorldGameScreen.getInstance().getWorld().removeStation(station);
            } else if (station.getType() == StationType.CLOSED) {
            //    station.setType(StationType.REGULAR);
                station.updateType();
            } else {
                station.setType(StationType.CLOSED);
            }
        }
        WorldGameScreen.getInstance().repaint();
    }

    /**
     * Создание новой станции
     */
    private void handleNewStation(int x, int y) {
        if (WorldGameScreen.getInstance().isCPressed) {
            // Ctrl+C - выбор цвета
            showColorSelectionPopup(x, y);
        } else {
            // Создаем новую станцию
            Station station = new Station(
                    WorldGameScreen.getInstance().getWorld(),
                    x, y,
                    currentStationColor,
                    StationType.PLANNED
            );
            WorldGameScreen.getInstance().getWorld().addStation(station);
            checkForTransferStation(station);
        }
    }

    /**
     * Перетаскивание метки
     */
    private void handleLabelDrag(int x, int y) {
        Label label = (Label) selectedObject;
        int newX = x - dragOffset.x;
        int newY = y - dragOffset.y;

        if (label.tryMoveTo(newX, newY)) {
            WorldGameScreen.getInstance().repaint();
        }
    }

    /**
     * Перетаскивание станции
     */
    private void handleStationDrag(int x, int y) {
        Station station = (Station) selectedObject;
        if (station.getType() != StationType.PLANNED) return;

        int newX = x - dragOffset.x;
        int newY = y - dragOffset.y;

        if (isPositionValidForStation(newX, newY)) {
            moveStationTo(station, newX, newY);
        }
    }

    /**
     * Перетаскивание туннеля
     */
    private void handleTunnelDrag(int x, int y) {
        Tunnel tunnel = (Tunnel) selectedObject;
        if (tunnel.getType() != TunnelType.PLANNED) return;

        // Проверяем, что новая позиция не совпадает со станцией
        Station stationAtPos = WorldGameScreen.getInstance().getWorld().getStationAt(x, y);
        if (stationAtPos == null) {
            tunnel.moveControlPoint(x, y);
            WorldGameScreen.getInstance().repaint();
        }
    }

    /**
     * Перемещение станции
     */
    private void moveStationTo(Station station, int newX, int newY) {
        GameWorld world = (GameWorld) WorldGameScreen.getInstance().getWorld();

        // Удаляем из старой позиции
        world.getGameGrid()[station.getX()][station.getY()].setContent(null);

        // Обновляем позицию
        station.x = newX;
        station.y = newY;

        // Добавляем в новую позицию
        world.getGameGrid()[newX][newY].setContent(station);

        // Пересчитываем все связанные туннели
        for (Tunnel t : world.getTunnels()) {
            if (t.getStart() == station || t.getEnd() == station) {
                t.calculatePath();
            }
        }

        // Перемещаем метку станции
        Label label = world.getLabelForStation(station);
        if (label != null) {
            PathPoint newLabelPos = world.findFreePositionNear(station.getX(), station.getY(), station.getName());
            if (newLabelPos != null) {
                label.tryMoveTo(newLabelPos.x, newLabelPos.y);
            }
        }

        WorldGameScreen.getInstance().repaint();
    }

    /**
     * Попытка выбрать метку
     */
    private boolean trySelectLabel(int x, int y) {
        // Сначала точное совпадение
        Label label = WorldGameScreen.getInstance().getWorld().getLabelAt(x, y);
        if (label != null) {
            selectObject(label, x, y);
            return true;
        }

        // Затем проверяем визуальную область
        for (Label l : WorldGameScreen.getInstance().getWorld().getLabels()) {
            if (isClickOnLabelVisualArea(l, x, y)) {
                selectObject(l, x, y);
                return true;
            }
        }

        return false;
    }

    /**
     * Попытка выбрать станцию
     */
    private boolean trySelectStation(int x, int y) {
        Station station = WorldGameScreen.getInstance().getWorld().getStationAt(x, y);
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
        Tunnel tunnel = WorldGameScreen.getInstance().getWorld().getTunnelAt(x, y);
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
        obj.setSelected(true);
        selectedObject = obj;
        dragOffset = new PathPoint(clickX - obj.getX(), clickY - obj.getY());
        WorldGameScreen.getInstance().repaint();
    }

    /**
     * Проверка визуальной области метки
     */
    private boolean isClickOnLabelVisualArea(Label label, int clickX, int clickY) {
        if (label.getParentStation() == null) return false;

        Station parent = label.getParentStation();
        int relX = label.getX() - parent.getX();
        int relY = label.getY() - parent.getY();

        // Рассчитываем визуальную позицию (упрощенная версия)
        int baseOffsetX = 32 + 8;
        int baseOffsetY = 20;

        if (relX < 0) {
            baseOffsetX = -40; // Приблизительная ширина текста
        } else if (relX == 0) {
            baseOffsetX = 0;
            if (relY < 0) {
                baseOffsetY = -30;
            } else if (relY > 0) {
                baseOffsetY = 50;
            }
        }

        // Проверяем попадание в область текста (с запасом)
        int visualX = parent.getX() * 32 + baseOffsetX;
        int visualY = parent.getY() * 32 + baseOffsetY;
        int margin = 20;

        return Math.abs(clickX * 32 - visualX) <= margin &&
                Math.abs(clickY * 32 - visualY) <= margin;
    }

    /**
     * Проверка границ мира
     */
    private boolean isWithinWorldBounds(int x, int y) {
        return x >= 0 && x < WorldGameScreen.getInstance().getWorld().getWidth() &&
                y >= 0 && y < WorldGameScreen.getInstance().getWorld().getHeight();
    }

    /**
     * Проверка валидности позиции для станции
     */
    private boolean isPositionValidForStation(int x, int y) {
        return x >= 0 && x < WorldGameScreen.getInstance().getWorld().getWidth() &&
                y >= 0 && y < WorldGameScreen.getInstance().getWorld().getHeight() &&
                WorldGameScreen.getInstance().getWorld().getStationAt(x, y) == null;
    }

    /*********************************
     * СЛУЖЕБНЫЕ МЕТОДЫ
     *********************************/

    /**
     * Снятие выделения со всех объектов
     */
    private void deselectAll() {
        GameWorld world = (GameWorld) WorldGameScreen.getInstance().getWorld();

        for (Station station : world.getStations()) {
            station.setSelected(false);
        }

        for (Label label : world.getLabels()) {
            label.setSelected(false);
        }

        for (Tunnel tunnel : world.getTunnels()) {
            tunnel.setSelected(false);
        }

        selectedObject = null;
        selectedStation = null;
    }

    /**
     * Получение выбранного объекта
     */
    public GameObject getSelectedObject() {
        return selectedObject;
    }

    /**
     * Редактирование имени станции
     */
    public void handleEditStationName(int x, int y) {
        Station station = WorldGameScreen.getInstance().getWorld().getStationAt(x, y);
        Tunnel t = WorldGameScreen.getInstance().getWorld().getTunnelAt(x, y);
        if (station != null) {
            editStationName(station);
        }
    }
    public static void editStationName(Station station) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Station Name:");
        JTextField textField = new JTextField(station.getName(), 20);
        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                WorldGameScreen.getInstance(),
                panel,
                "Edit name",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String newName = textField.getText().trim();
            if (!newName.isEmpty()) {
                station.setName(newName);
                WorldGameScreen.getInstance().repaint();
            }
        }
    }

    /**
     * Проверка на трансферную станцию
     */
    private static void checkForTransferStation(Station station) {
        int x = station.getX();
        int y = station.getY();

        for (int ny = Math.max(0, y-1); ny < Math.min(WorldGameScreen.getInstance().getWorld().getHeight(), y+2); ny++) {
            for (int nx = Math.max(0, x-1); nx < Math.min(WorldGameScreen.getInstance().getWorld().getWidth(), x+2); nx++) {
                if (nx == x && ny == y) continue;

                Station neighbor = WorldGameScreen.getInstance().getWorld().getStationAt(nx, ny);
                if (neighbor != null && !neighbor.getColor().equals(station.getColor())) {
                    station.setType(StationType.TRANSFER);
                    neighbor.setType(StationType.TRANSFER);
                    return;
                }
            }
        }
    }

    /**
     * Проверка прогресса строительства
     */
    public void checkConstructionProgress() {
        GameWorld world = (GameWorld) WorldGameScreen.getInstance().getWorld();

        // Создаем копии списков для безопасной итерации
        List<Station> stationsToCheck = new ArrayList<>(world.getStations());
        List<Tunnel> tunnelsToCheck = new ArrayList<>(world.getTunnels());

        // Для станций
        for (Station station : stationsToCheck) {
            try {
                if (station.getType() == StationType.BUILDING || station.getType() == StationType.DESTROYED) {
                    float progress = world.getStationConstructionProgress(station);
                    if (station.getType() == StationType.BUILDING && progress >= 1.0f) {
                        completeConstruction(station);
                    } else if (station.getType() == StationType.DESTROYED && progress <= 0f) {
                        world.removeStation(station);
                        WorldGameScreen.getInstance().repaint();
                    }
                }
            } catch (Exception e) {
                MetroLogger.logError("Error processing station " + station.getName(), e);
            }
        }

        // Для туннелей
        for (Tunnel tunnel : tunnelsToCheck) {
            try {
                if (tunnel.getType() == TunnelType.BUILDING || tunnel.getType() == TunnelType.DESTROYED) {
                    float progress = world.getTunnelConstructionProgress(tunnel);
                    if (tunnel.getType() == TunnelType.BUILDING && progress >= 1.0f) {
                        completeConstruction(tunnel);
                    } else if (tunnel.getType() == TunnelType.DESTROYED && progress <= 0f) {
                        world.removeTunnel(tunnel);
                        WorldGameScreen.getInstance().repaint();
                    }
                }
            } catch (Exception e) {
                MetroLogger.logError("Error processing tunnel", e);
            }
        }
    }

    /**
     * Завершение строительства станции
     */
    public void completeConstruction(Station station) {
        if (station.getType() == StationType.BUILDING) {
            station.updateType();
            if (WorldGameScreen.getInstance().getWorld() instanceof GameWorld world) {
                world.updateConnectedTunnels(station);
            }
            WorldGameScreen.getInstance().repaint();
        }
    }

    /**
     * Завершение строительства туннеля
     */
    public void completeConstruction(Tunnel tunnel) {
        if (tunnel.getType() == TunnelType.BUILDING) {
            if (tunnel.getStart().getType() != StationType.PLANNED &&
                    tunnel.getStart().getType() != StationType.BUILDING &&
                    tunnel.getEnd().getType() != StationType.PLANNED &&
                    tunnel.getEnd().getType() != StationType.BUILDING) {
                tunnel.setType(TunnelType.ACTIVE);
                WorldGameScreen.getInstance().repaint();
            }
        }
    }

    /**
     * Расчет стоимости станции
     */
    private int calculateStationCost(Station station) {
        int totalCost = STATION_BASE_COST;

        for (Tunnel tunnel : WorldGameScreen.getInstance().getWorld().getTunnels()) {
            if ((tunnel.getStart() == station || tunnel.getEnd() == station) &&
                    tunnel.getType() == TunnelType.PLANNED) {
                totalCost += tunnel.getLength() * TUNNEL_COST_PER_SEGMENT;
            }
        }

        return totalCost;
    }

    /**
     * Показ выбора цвета
     */
    public void showColorSelectionPopup(int x, int y) {
        Window parentWindow = SwingUtilities.getWindowAncestor(WorldGameScreen.getInstance());
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
        Point screenPoint = WorldGameScreen.getInstance().worldToScreen(x, y);
        Point windowPoint = new Point(screenPoint);
        SwingUtilities.convertPointToScreen(windowPoint, WorldGameScreen.getInstance());
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
                    WorldGameScreen.getInstance().getWorld(),
                    x, y, StationColors.fromColor(colorButton), StationType.PLANNED
            );
            WorldGameScreen.getInstance().getWorld().addStation(newStation);
            WorldGameScreen.getInstance().repaint();
            dialog.dispose();
        });

        return colorBtn;
    }

    /**
     * Создание кнопки цвета
     */
//    private JButton createColorButton(Color colorButton, int x, int y, JDialog dialog) {
//        JButton colorBtn = new JButton();
//        colorBtn.setBackground(colorButton);
//        colorBtn.setPreferredSize(new Dimension(30, 30));
//        colorBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//        colorBtn.setContentAreaFilled(false);
//        colorBtn.setOpaque(true);
//        colorBtn.setFocusPainted(false);
//
//        colorBtn.addActionListener(e -> {
//            currentStationColor = StationColors.fromColor(colorButton);
//            Station newStation = new Station(
//                    WorldGameScreen.getInstance().getWorld(),
//                    x, y, StationColors.fromColor(colorButton), StationType.PLANNED
//            );
//            WorldGameScreen.getInstance().getWorld().addStation(newStation);
//            WorldGameScreen.getInstance().repaint();
//            dialog.dispose();
//        });
//
//        colorBtn.addMouseListener(new MouseAdapter() {
//            public void mouseEntered(MouseEvent e) {
//                colorBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
//            }
//            public void mouseExited(MouseEvent e) {
//                colorBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//            }
//        });
//
//        return colorBtn;
//    }

}
