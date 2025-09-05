package metroline.screens.worldscreens.normal;

import metroline.core.world.GameWorld;
import metroline.input.KeyboardController;
import metroline.input.selection.GameSelectionListener;
import metroline.input.selection.Selectable;
import metroline.input.selection.SelectionManager;
import metroline.objects.enums.StationColors;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TrainType;
import metroline.objects.enums.TunnelType;
import metroline.objects.gameobjects.StationLabel;
import metroline.objects.gameobjects.*;
import metroline.screens.render.StationPositionCache;
import metroline.util.MetroLogger;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.MetrolineButton;
import metroline.util.ui.MetrolinePopupMenu;
import metroline.util.ui.tooltip.CursorTooltip;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static metroline.objects.gameobjects.GameConstants.COST_STANDART_TRAIN;

public class WorldClickController {

    public static PathPoint dragOffset = null;

    // Ссылки
    public GameWorldScreen screen;
    private static StationColors currentStationColor = StationColors.RED;


    public WorldClickController(GameWorldScreen screen) {
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
        boolean isAPressed = keyboard.isKeyPressed(KeyEvent.VK_A);
        if (isAltPressed && isShiftPressed || screen.isAltPressed && screen.isShiftPressed) {
            handleAltShiftClick(x, y);
        } else if (isAltPressed || screen.isAltPressed) {
            handleAltClick(x, y);
        } else if (isShiftPressed && isCPressed || screen.isShiftPressed && screen.isCPressed) {
            ((GameWorld)screen.getWorld()).updateLegendWindow();
            showColorSelectionPopup(x, y);
        }
        else if (isShiftPressed || screen.isShiftPressed) {
            ((GameWorld)screen.getWorld()).updateLegendWindow();
            handleShiftClick(x, y);
        } else if (isCtrlPressed || screen.isCtrlPressed) {
            handleCtrlClick(x, y);
        } else {
            handleDefaultLeftClick(x, y);
        }

    }
    public void handleAltShiftClick(int x, int y) {
        Station station = GameWorldScreen.getInstance().getWorld().getStationAt(x, y);
        if (station != null && (station.getType() != StationType.DESTROYED ||station.getType() != StationType.PLANNED || station.getType() != StationType.BUILDING) ) {
            GameWorld gameWorld = (GameWorld) GameWorldScreen.getInstance().getWorld();
            gameWorld.startDestroyingStation(station);
            GameWorldScreen.getInstance().repaint();
        }
    }


    /*********************************
     * ОБРАБОТЧИКИ КОНКРЕТНЫХ ДЕЙСТВИЙ
     *********************************/

    /**
     * Alt+Click - переключение между PLANNED и BUILDING
     */
    public void handleAltClick(int x, int y) {
        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
        Station station = world.getStationAt(x, y);

        if (station != null && station.getType() == StationType.PLANNED) {
            float stationCost = world.getEconomyManager().calculateStationConstructionCost(station.getX(), station.getY());
            if (world.canAfford(stationCost)) {
                world.addMoney(-stationCost);
                station.setType(StationType.BUILDING);
                world.addStation(station);
                station.setConstructionDate(world.getGameTime().getCurrentTimeMillis());
            }
        }
        GameWorldScreen.getInstance().repaint();
    }
    /**
     * Ctrl+Click - создание туннелей
     */
    public void handleCtrlClick(int x, int y) {
        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
        Station station = world.getStationAt(x, y);

        if (station == null) return;

        // Проверяем, что станция не в "неактивном" состоянии
        if (station.getType() == StationType.DESTROYED ||
                station.getType() == StationType.CLOSED ||
                station.getType() == StationType.ABANDONED) {
            return;
        }
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();
        if (selected != null && !(selected instanceof Station)) {
            selectionManager.deselect();
            return;
        }
        if (selected != null && selected != station) {
            Station selectedStation = (Station) selected;
            if (!selectedStation.getColor().equals(station.getColor())) {
                selectionManager.deselect();
                return;
            }

            int selectedStationConnections = countActiveTunnels(selectedStation, world);
            int targetStationConnections = countActiveTunnels(station, world);

            if (selectedStationConnections >= 2 || targetStationConnections >= 2) {
                selectionManager.deselect();
                return;
            }

            // Проверяем, что обе станции построены
            boolean bothBuilt =
                    selectedStation.getType() == StationType.TRANSIT ||
                    selectedStation.getType() == StationType.TERMINAL ||
                    selectedStation.getType() == StationType.REGULAR ||
                    selectedStation.getType() == StationType.TRANSFER;
            boolean targetBuilt =
                    station.getType() == StationType.TRANSIT ||
                            station.getType() == StationType.TERMINAL ||
                    station.getType() == StationType.REGULAR ||
                    station.getType() == StationType.TRANSFER;

            // Создаем туннель между двумя станциями
            Tunnel tunnel = new Tunnel(world, selectedStation, station,
                    bothBuilt && targetBuilt ? TunnelType.BUILDING : TunnelType.PLANNED);

            world.addTunnel(tunnel);
            tunnel.setTunnelColor(station.getColor());
            // Снимаем выделение
            selectionManager.deselect();
        } else {
            if (selectionManager.isSelected(station)) {
                selectionManager.deselect();
            } else {
                selectionManager.select(station);
            }
        }

        GameWorldScreen.getInstance().repaint();
    }
    // Вспомогательный метод для подсчета активных туннелей станции
    private int countActiveTunnels(Station station, GameWorld world) {
        int count = 0;
        for (Tunnel tunnel : world.getTunnels()) {
            if (tunnel.getType() == TunnelType.ACTIVE ||
                    tunnel.getType() == TunnelType.BUILDING ||
                    tunnel.getType() == TunnelType.PLANNED) {
                if (tunnel.getStart() == station || tunnel.getEnd() == station) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Shift+Click - строительство/удаление станций
     */
    public void handleShiftClick(int x, int y) {
        Station existing = GameWorldScreen.getInstance().getWorld().getStationAt(x, y);

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

        if (selected instanceof Train) {
            Train selectedTrain = (Train) selected;
            if (!isClickOnTrain(selectedTrain, x, y)) {
                selectionManager.deselect();
            }
            return; // Важно: выходим после обработки поезда
        }

        if (selected != null) {
            selectionManager.deselect();
        }

        // Проверяем объекты в порядке приоритета
        if (trySelectTrain(x, y)) return;
        if (trySelectLabel(x, y)) return;
        if (trySelectStation(x, y)) return;

        if (trySelectTunnel(x, y)) return;

        if(tryGameplayObject(x, y)) return;



    }


    /**
     * Обработка перетаскивания
     */
    public void handleEditDrag(int x, int y) {
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();
        if (selected == null || dragOffset == null) return;

        // Проверяем границы мира
        if (!isWithinWorldBounds(x, y)) return;

        if (selected instanceof StationLabel) {
            handleLabelDrag((StationLabel) selected, x, y);
        } else if (selected instanceof Station) {
            handleStationDrag((Station)selected, x, y);
            StationPositionCache.invalidateStationPositions((Station)selected);
        } else if (selected instanceof Tunnel) {
            handleTunnelDrag((Tunnel)selected, x, y);

        }
    }

    /*********************************
     * ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
     *********************************/

    /**
     * Обработка существующей станции
     */
    private void handleExistingStation(Station station) {
        // Запрещаем действия с разрушающейся/строящейся/закрытой станцией
        if (station.getType() == StationType.DEPO ||station.getType() == StationType.DESTROYED ||
                station.getType() == StationType.BUILDING) {
            return;
        }

        if (screen.isAltPressed) {
            // Alt+Shift - начало разрушения станции
            GameWorld gameWorld = (GameWorld) GameWorldScreen.getInstance().getWorld();
            gameWorld.startDestroyingStation(station);
        } else {
            // Обычный Shift - изменение типа станции
            if (station.getType() == StationType.PLANNED) {
                GameWorldScreen.getInstance().getWorld().removeStation(station);
            } else if (station.getType() == StationType.CLOSED) {
                station.updateType();
            } else {
                station.setType(StationType.CLOSED);
            }
        }
        GameWorldScreen.getInstance().repaint();
    }


    /**
     * Создание новой станции
     */
    private void handleNewStation(int x, int y) {
        if (!isPositionValidForStation(x, y)) {
            return; // Нельзя строить - выходим
        }

        if (GameWorldScreen.getInstance().isCPressed) {

            showColorSelectionPopup(x, y);

        }
        else {
            // Создаем новую станцию
            Station station = new Station(
                    GameWorldScreen.getInstance().getWorld(),
                    x, y,
                    currentStationColor,
                    StationType.PLANNED
            );
            GameWorldScreen.getInstance().getWorld().addStation(station);
            checkForTransferStation(station);
        }
    }

    /**
     * Перетаскивание метки
     */
    private void handleLabelDrag(StationLabel stationLabel, int x, int y) {
        int newX = x - dragOffset.x;
        int newY = y - dragOffset.y;

        if (stationLabel.tryMoveTo(newX, newY)) {
            GameWorldScreen.getInstance().repaint();
        }
    }

    /**
     * Перетаскивание станции
     */
    private void handleStationDrag(Station station,int x, int y) {

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
    private void handleTunnelDrag(Tunnel tunnel, int x, int y) {

        if (tunnel.getType() != TunnelType.PLANNED) return;

        // Проверяем, что новая позиция не совпадает со станцией
        Station stationAtPos = GameWorldScreen.getInstance().getWorld().getStationAt(x, y);
        if (stationAtPos == null) {
            tunnel.moveControlPoint(x, y);
            GameWorldScreen.getInstance().repaint();
        }
    }

    /**
     * Перемещение станции
     */
    private void moveStationTo(Station station, int newX, int newY) {
        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();

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
     //   System.out.println("stationLabel " + station.getLabel());
        if (stationLabel != null) {

            PathPoint newLabelPos = world.findFreePositionNear(station.getX(), station.getY(), station.getName());
            if (newLabelPos != null) {
                stationLabel.tryMoveTo(newLabelPos.x, newLabelPos.y);

            }
        }


        GameWorldScreen.getInstance().repaint();
    }
    private boolean tryGameplayObject(int x, int y) {
        GameplayUnits gameplayUnits = ((GameWorld) GameWorldScreen.getInstance().getWorld()).getGameplayUnitsAt(x, y);
        if (gameplayUnits != null) {
            selectObject(gameplayUnits, x, y);
            return true;
        }
        return false;
    }
    /**
     * Попытка выбрать метку
     * TODO Скрытие меток надо сделать физическим, а не визуальным
     */
    private boolean trySelectLabel(int x, int y) {
        // Сначала точное совпадение
        StationLabel stationLabel = GameWorldScreen.getInstance().getWorld().getLabelAt(x, y);
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

        Station station = GameWorldScreen.getInstance().getWorld().getStationAt(x, y);
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
        Tunnel tunnel = GameWorldScreen.getInstance().getWorld().getTunnelAt(x, y);
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
        GameWorldScreen.getInstance().repaint();
    }




    /**
     * Попытка выбрать поезд
     */
    private boolean trySelectTrain(int cellX, int cellY) {
        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();

        // Сначала проверяем, не выбран ли уже поезд
        if (selected instanceof Train) {
            Train selectedTrain = (Train) selected;
            if (isClickOnTrain(selectedTrain, cellX, cellY)) {

                return true;
            }
        }

        // Проверяем все поезда в мире
        for (Train train : world.getTrains()) {
            if (isClickOnTrain(train, cellX, cellY)) {
                System.out.println("Train selected: " + train.getName());
                selectObject(train, cellX, cellY);
                return true;
            }
        }

        return false;
    }

    public void handleRightClick(int x, int y) {
        KeyboardController keyboard = KeyboardController.getInstance();
        boolean isShiftPressed = keyboard.isKeyPressed(KeyEvent.VK_SHIFT);

        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
        GameObject clickedObject = world.getGameObjectAt(x, y);

        if (clickedObject instanceof Station) {
            Station station = (Station) clickedObject;

            // Shift+ПКМ - превращение в депо
            if (isShiftPressed || screen.isShiftPressed) {
                // Проверяем, что станция может быть превращена в депо
           //     world.addTrainToStation(station, TrainType.EXPRESS);

                if (station.getType() != StationType.DEPO &&
                        station.getType() != StationType.PLANNED &&
                        station.getType() != StationType.BUILDING &&
                        station.getType() != StationType.DESTROYED) {

                    station.setType(StationType.DEPO);
                    GameWorldScreen.getInstance().repaint();
                }
            } else {
                // Обычный ПКМ - добавление поезда (только не для депо)
                if(station.getType() == StationType.DEPO) {
                    showDepotMenu(station, x, y);

                }
            }
        }
    }


    /*********************************
     * СЛУЖЕБНЫЕ МЕТОДЫ
     *********************************/

    /**
     * Проверка визуальной области метки
     */
    private boolean isClickOnLabelVisualArea(StationLabel stationLabel, int clickX, int clickY) {
        if (stationLabel.getParentGameObject() == null) return false;

        GameObject parent = stationLabel.getParentGameObject();
        int relX = stationLabel.getX() - parent.getX();
        int relY = stationLabel.getY() - parent.getY();

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
     * Проверка валидности позиции для станции
     */
    private boolean isPositionValidForStation(int x, int y) {
        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();

        // Проверяем границы мира
        if (x < 0 || x >= world.getWidth() || y < 0 || y >= world.getHeight()) {
            return false;
        }

        // Проверяем, что клетка свободна (нет станции и нет GameplayUnits)
        return world.getStationAt(x, y) == null;
    }

    /**
     * Проверка границ мира
     */
    private boolean isWithinWorldBounds(int x, int y) {
        return x >= 0 && x < GameWorldScreen.getInstance().getWorld().getWidth() &&
                y >= 0 && y < GameWorldScreen.getInstance().getWorld().getHeight();
    }


    /**
     * Снятие выделения со всех объектов
     */
    public void deselectAll() {
        SelectionManager.getInstance().deselect();
    }
    /**
     * Получение выбранного объекта
     */
    public Selectable getSelectedObject() {
        return SelectionManager.getInstance().getSelected();
    }

    /**
     * Проверка на трансферную станцию
     */
    private static void checkForTransferStation(Station station) {
        int x = station.getX();
        int y = station.getY();

        for (int ny = Math.max(0, y-1); ny < Math.min(GameWorldScreen.getInstance().getWorld().getHeight(), y+2); ny++) {
            for (int nx = Math.max(0, x-1); nx < Math.min(GameWorldScreen.getInstance().getWorld().getWidth(), x+2); nx++) {
                if (nx == x && ny == y) continue;

                Station neighbor = GameWorldScreen.getInstance().getWorld().getStationAt(nx, ny);
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

     //   validateSelection();

        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
        SelectionManager selectionManager = SelectionManager.getInstance();
        // Создаем копии списков для безопасной итерации
        List<Station> stationsToCheck = new ArrayList<>(world.getStations());
        List<Tunnel> tunnelsToCheck = new ArrayList<>(world.getTunnels());

        // Для станций
        for (Station station : stationsToCheck) {
            try {
                if (station.getType() == StationType.BUILDING || station.getType() == StationType.DESTROYED) {
                    float progress = world.getConstructionProcessor().getStationConstructionProgress(station);
                    if (station.getType() == StationType.BUILDING && progress >= 1.0f) {
                        completeConstruction(station);
                    } else if (station.getType() == StationType.DESTROYED && progress <= 0f) {
                        if (!selectionManager.isSelected(station)) {
                            world.removeStation(station);
                            GameWorldScreen.getInstance().repaint();
                        }
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
                    float progress = world.getConstructionProcessor().getTunnelConstructionProgress(tunnel);
                    if (tunnel.getType() == TunnelType.BUILDING && progress >= 1.0f) {
                        completeConstruction(tunnel);
                    } else if (tunnel.getType() == TunnelType.DESTROYED && progress <= 0f) {
                        if (!selectionManager.isSelected(tunnel)) {
                            world.removeTunnel(tunnel);
                            GameWorldScreen.getInstance().repaint();
                        }
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
            if (GameWorldScreen.getInstance().getWorld() instanceof GameWorld world) {
                world.updateConnectedTunnels(station);
            }

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

            }
        }
    }

//    /**
//     * Расчет стоимости станции
//     */
//    private float calculateStationCost(Station station) {
//        float totalCost = (GameConstants.STATION_BASE_COST * 100) * GameWorldScreen.getInstance().getWorld().getWorldTile(station.getX(), station.getY()).getPerm();
//
//        for (Tunnel tunnel : GameWorldScreen.getInstance().getWorld().getTunnels()) {
//            if ((tunnel.getStart() == station || tunnel.getEnd() == station) &&
//                    tunnel.getType() == TunnelType.PLANNED) {
//                totalCost += tunnel.getLength() * GameConstants.TUNNEL_COST_PER_SEGMENT;
//            }
//        }
//
//        return totalCost;
//    }

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
                    GameWorldScreen.getInstance().getWorld(),
                    x, y, StationColors.fromColor(colorButton), StationType.PLANNED
            );
            GameWorldScreen.getInstance().getWorld().addStation(newStation);
            GameWorldScreen.getInstance().repaint();
            dialog.dispose();
        });

        return colorBtn;
    }



    /**
     * Проверка попадания клика на поезд
     */
    /**
     * Проверка попадания клика на поезд (в мировых координатах)
     */
    private boolean isClickOnTrain(Train train, int worldX, int worldY) {
        // Поезд имеет точные координаты с плавающей точкой
        float trainX = train.getCurrentX();
        float trainY = train.getCurrentY();

        // Размеры поезда в мировых координатах (примерно 1x1 клетка)
        float trainWidth = 1.0f;
        float trainHeight = 1.0f;

        // Проверяем попадание в прямоугольную область поезда
        return worldX >= trainX - trainWidth/2 &&
                worldX <= trainX + trainWidth/2 &&
                worldY >= trainY - trainHeight/2 &&
                worldY <= trainY + trainHeight/2;
    }
    /**
     * Показ меню депо с выбором поездов используя MetrolinePopupMenu
     */
    private void showDepotMenu(Station station, int x, int y) {
        MetrolinePopupMenu depotMenu = new MetrolinePopupMenu();

        // Устанавливаем специальный layout для сетки 4x2
        depotMenu.setLayout(new GridLayout(4, 2, 2, 2));

        // Добавляем кнопки для каждого типа поезда
        for (TrainType trainType : TrainType.values()) {
            MetrolineButton trainButton = createTrainButton(trainType, station, depotMenu);
            depotMenu.add(trainButton);
        }

        // Получаем компонент для отображения
        Component parentComponent = GameWorldScreen.getInstance();
        Point screenPoint = GameWorldScreen.getInstance().worldToScreen(x, y);

        // Показываем меню в нужной позиции
        depotMenu.show(parentComponent, screenPoint.x + 20, screenPoint.y + 20);
    }

    /**
     * Создание кнопки для типа поезда для MetrolinePopupMenu
     */
    private MetrolineButton createTrainButton(TrainType trainType, Station station, MetrolinePopupMenu menu) {
        MetrolineButton trainButton = new MetrolineButton("") {
            @Override
            protected void paintComponent(Graphics g) {
                // Прозрачный фон
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(0, 0, 0, 0));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();

                super.paintComponent(g);
            }
        };

        trainButton.setPreferredSize(new Dimension(60, 60));
        trainButton.setContentAreaFilled(false);
        trainButton.setOpaque(false);
        trainButton.setFocusPainted(false);
        trainButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Загружаем иконку поезда
        ImageIcon trainIcon = loadTrainIcon(trainType);
        if (trainIcon != null) {
            trainButton.setIcon(trainIcon);
        }
        trainButton.setToolTipText(LngUtil.translatable("train.cost." + trainType.name().toLowerCase()));
        trainButton.addActionListener(e -> {
            GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
            float cost = getTrainCost(trainType);

            // Проверяем, хватает ли денег
            if (world.canAfford(cost)) {
                world.addMoney(-cost);
                world.addTrainToStation(station, trainType);
                GameWorldScreen.getInstance().repaint();
                menu.setVisible(false); // Закрываем меню
                CursorTooltip.hideTooltip();
            } else {
                // Показываем сообщение о недостатке денег
                JOptionPane.showMessageDialog(menu,
                        "Недостаточно денег для покупки этого поезда!",
                        "Ошибка",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        return trainButton;
    }

    /**
     * Загрузка иконки поезда из ресурсов
     */
    private ImageIcon loadTrainIcon(TrainType trainType) {
        String imagePath = "";
        switch (trainType) {
            case FIRST:
                imagePath = "/trains/first_train.png";
                break;
            case OLD:
                imagePath = "/trains/old_train.png";
                break;
            case CLASSIC:
                imagePath = "/trains/classic_train.png";
                break;
            case MODERN:
                imagePath = "/trains/modern_train.png";
                break;
            case NEW:
                imagePath = "/trains/new_train.png";
                break;
            case NEWEST:
                imagePath = "/trains/newest_train.png";
                break;
            case FUTURISTIC:
                imagePath = "/trains/futuristic_train.png";
                break;
            case FAR_FUTURISTIC:
                imagePath = "/trains/far_futuristic_train.png";
                break;
        }

        try {
            Image image = loadImage(imagePath);
            if (image != null) {
                // Масштабируем изображение
                Image scaledImage = image.getScaledInstance(55, 55, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            MetroLogger.logError("Error loading train icon: " + imagePath, e);
        }

        // Fallback - простая цветная иконка
        return createFallbackIcon(trainType);
    }

    /**
     * Создание fallback иконки (упрощенная)
     */
    private ImageIcon createFallbackIcon(TrainType trainType) {
        BufferedImage image = new BufferedImage(35, 35, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Простой цветной квадрат с номером типа
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
                Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.PINK};
        int index = trainType.ordinal() % colors.length;

        g2d.setColor(colors[index]);
        g2d.fillRect(5, 5, 25, 25);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(String.valueOf(trainType.ordinal() + 1), 15, 20);

        g2d.dispose();
        return new ImageIcon(image);
    }

    public static Image loadImage(String path) {
        try {
            return ImageIO.read(WorldClickController.class.getResource(path));
        } catch (IOException | IllegalArgumentException e) {
            MetroLogger.logError("Failed to load image: " + path, e);
            return null;
        }
    }
    /**
     * Создание fallback иконки
     */

    /**
     * Получение отображаемого имени типа поезда
     */
    private String getTrainTypeName(TrainType trainType) {
        switch (trainType) {
            case FIRST: return "train.name.first";
            case OLD: return "train.name.old";
            case CLASSIC: return "train.name.classic";
            case MODERN: return "train.name.modern";
            case NEW: return "train.name.new";
            case NEWEST: return "train.name.newest";
            case FUTURISTIC: return "train.name.futuristic";
            case FAR_FUTURISTIC: return "train.name.far_futuristic";
            default: return trainType.name();
        }
    }

    /**
     * Получение стоимости поезда
     */
    private float getTrainCost(TrainType trainType) {
        switch (trainType) {
            case FIRST: return COST_STANDART_TRAIN;
            case OLD: return COST_STANDART_TRAIN*1.1f;
            case CLASSIC: return COST_STANDART_TRAIN*1.2f;
            case MODERN: return COST_STANDART_TRAIN*1.4f;
            case NEW: return COST_STANDART_TRAIN*1.6f;
            case NEWEST: return COST_STANDART_TRAIN*1.9f;
            case FUTURISTIC: return COST_STANDART_TRAIN*2f;
            case FAR_FUTURISTIC: return COST_STANDART_TRAIN*4f;
            default: return COST_STANDART_TRAIN;
        }
    }


}
