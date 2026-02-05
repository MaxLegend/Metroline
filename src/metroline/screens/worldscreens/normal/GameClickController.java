package metroline.screens.worldscreens.normal;

import metroline.core.world.GameWorld;
import metroline.core.world.tiles.GameTile;
import metroline.input.KeyboardController;
import metroline.input.selection.GameSelectionListener;
import metroline.input.selection.Selectable;
import metroline.input.selection.SelectionManager;
import metroline.objects.enums.StationColors;
import metroline.objects.enums.StationType;

import metroline.objects.enums.TunnelType;
import metroline.objects.gameobjects.StationLabel;
import metroline.objects.gameobjects.*;

import metroline.screens.panel.StationTypePopupMenu;
import metroline.screens.panel.TunnelTypePopupMenu;
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

import static metroline.MainFrame.SOUND_ENGINE;


// FIX Убрать возможность выбирать скрытые объекты
// TODO Добавить клики и обработку станций пострадавших от событий. Пока за деньги.
public class GameClickController  {

    public static PathPoint dragOffset = null;

    // Ссылки
    public GameWorldScreen screen;
    private static StationColors currentStationColor = StationColors.RED;


    public GameClickController(GameWorldScreen screen) {
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

        // Check both keyboard keys AND button modes
        boolean stationMode = isShiftPressed || screen.isShiftPressed || screen.stationButtonModeActive;
        boolean tunnelMode = isCtrlPressed || screen.isCtrlPressed || screen.tunnelButtonModeActive;
        boolean destroyMode = isAltPressed || screen.isAltPressed || screen.destroyButtonModeActive;
        boolean colorMode = isCPressed || screen.isCPressed || screen.colorButtonModeActive;

        // River tools (button-only)
        if (screen.isRiverBrushToolActive) {
            handleRiverBrush3x3(x, y);
            SOUND_ENGINE.playUISound("mouse", 1);
            return;
        }
        if (screen.isRiverToolActive) {
            handleRiverTool(x, y);
            SOUND_ENGINE.playUISound("mouse", 1);
            return;
        }

        if (screen.isRiverLineToolActive) {
            if (isAltPressed || screen.isAltPressed) {
                // Alt+LMB = finish current river
                handleRiverLineFinish(x, y);
            } else if (isShiftPressed || screen.isShiftPressed) {
                // Shift+LMB = delete point or river
                handleRiverLineDelete(x, y);
            } else {
                // LMB = add point
                handleRiverLineAddPoint(x, y);
            }
            SOUND_ENGINE.playUISound("mouse", 1);
            return;
        }

        if (isAltPressed && isShiftPressed || screen.isAltPressed && screen.isShiftPressed) {
            handleAltShiftClick(x, y);
            SOUND_ENGINE.playUISound("mouse", 1);
        } else if (destroyMode) {
            handleAltClick(x, y);
            SOUND_ENGINE.playUISound("mouse", 1);
        } else if (colorMode) {
            ((GameWorld)screen.getWorld()).updateLegendWindow();
            handleColorButtonMode(x, y);
            SOUND_ENGINE.playUISound("mouse", 1);
        } else if (stationMode) {
            ((GameWorld)screen.getWorld()).updateLegendWindow();
            handleShiftClick(x, y);
            SOUND_ENGINE.playUISound("mouse", 1);
        } else if (tunnelMode) {
            handleCtrlClick(x, y);
            SOUND_ENGINE.playUISound("mouse", 1);
        } else {
            handleDefaultLeftClick(x, y);
        }
    }

    private void handleColorButtonMode(int x, int y) {
        // [GameClickController::handleColorButtonMode] Color button active - show popup when placing station
        Station existing = GameWorldScreen.getInstance().getWorld().getStationAt(x, y);

        if (existing == null && isTileFreeFor(null, x, y)) {
            // Empty cell - show color popup
            showColorSelectionPopup(x, y);
        }
    }
    public void handleAltShiftClick(int x, int y) {
//        Station station = GameWorldScreen.getInstance().getWorld().getStationAt(x, y);
//        if (station != null && (station.getType() != StationType.DESTROYED ||station.getType() != StationType.PLANNED || station.getType() != StationType.BUILDING) ) {
//            GameWorld gameWorld = (GameWorld) GameWorldScreen.getInstance().getWorld();
//
//            GameWorldScreen.getInstance().repaint();
//        }
        // TODO - free function
    }


    /*********************************
     * ОБРАБОТЧИКИ КОНКРЕТНЫХ ДЕЙСТВИЙ
     *********************************/

    /**
     * Alt+Click - переключение между PLANNED и BUILDING
     */
    public void handleAltClick(int x, int y) {
        // [GameClickController::handleAltClick] Destroy tool or place RiverPoint
        GameWorld gameWorld = (GameWorld) GameWorldScreen.getInstance().getWorld();

        // Check if cell has any content
        GameTile tile = gameWorld.getGameTile(x, y);
        if (tile == null) return;

        GameObject content = tile.getContent();

        // If cell is EMPTY - place new RiverPoint (like Shift+LMB for stations)
        if (content == null) {
            placeRiverPoint(x, y);
            return;
        }

        // If cell has RiverPoint - remove it
        if (content instanceof RiverPoint) {
            RiverPoint riverPoint = (RiverPoint) content;
            River parentRiver = riverPoint.getParentRiver();
            gameWorld.removeRiverPoint(riverPoint);

            // Also remove from River's internal list
            if (parentRiver != null) {
                parentRiver.removePoint(riverPoint);

                // If river now has < 2 points, remove entire river
                if (parentRiver.getPoints().size() < 2) {
                    for (RiverPoint p : new ArrayList<>(parentRiver.getPoints())) {
                        gameWorld.removeRiverPoint(p);
                    }
                    gameWorld.removeRiver(parentRiver);
                    MetroLogger.logInfo("[GameClickController::handleAltClick] River removed (< 2 points)");
                }
            }

            screen.invalidateCache();
            screen.repaint();
            return;
        }

        // If cell has Station - remove it
        if (content instanceof Station) {
            Station station = (Station) content;
            // Existing station removal logic...
            gameWorld.removeStation(station);
            screen.invalidateCache();
            screen.repaint();
            return;
        }

        // Other object types removal...
        MetroLogger.logInfo("[GameClickController::handleAltClick] Unknown content at: " + x + ", " + y);
    }
    /**
     * Places a new RiverPoint at coordinates, auto-connecting to nearest existing river
     * or creating a new river if needed.
     */
    private void placeRiverPoint(int x, int y) {
        GameWorld world = (GameWorld) screen.getWorld();

        if (x < 0 || x >= world.getWidth() || y < 0 || y >= world.getHeight()) {
            return;
        }

        River targetRiver = null;
        Selectable selected = getSelectedObject();

        // 1. Пытаемся взять реку у текущего выделения
        if (selected instanceof RiverPoint) {
            targetRiver = ((RiverPoint) selected).getParentRiver();
        }

        // 2. КРИТИЧЕСКИЙ ФИКС: Если река всё еще null (ничего не выбрано
        // или у точки удалена река) — создаем новую
        if (targetRiver == null) {
            targetRiver = new River(world);
            world.addRiver(targetRiver);
            MetroLogger.logInfo("[GameClickController::placeRiverPoint] Creating a fresh river (targetRiver was null)");
        }

        // 3. Теперь targetRiver гарантированно не null, создаем точку
        RiverPoint newPoint = new RiverPoint(world, x, y, targetRiver);
        world.addRiverPoint(newPoint);
        targetRiver.addPoint(newPoint);

        // 4. Делаем новую точку активной для продолжения линии
        SelectionManager.getInstance().select(newPoint);

        MetroLogger.logInfo(" Point added at: " + x + ", " + y);

        screen.invalidateCache();
        screen.repaint();
    }

    /**
     * Finds nearest RiverPoint within maxDistance tiles
     */
    private RiverPoint findNearestRiverPoint(int x, int y, int maxDistance) {
        GameWorld world = (GameWorld) screen.getWorld();
        RiverPoint nearest = null;
        double minDist = maxDistance + 1;

        for (River river : world.getRivers()) {
            for (RiverPoint point : river.getPoints()) {
                double dist = Math.sqrt(Math.pow(point.getX() - x, 2) + Math.pow(point.getY() - y, 2));
                if (dist < minDist && dist <= maxDistance) {
                    minDist = dist;
                    nearest = point;
                }
            }
        }
        return nearest;
    }
    /**
     * Ctrl+Click - создание туннелей
     */
    public void handleCtrlClick(int x, int y) {
        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
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


            // Создаем туннель между двумя станциями
            Tunnel tunnel = new Tunnel(world, selectedStation, station, TunnelType.ACTIVE);

            world.addTunnel(tunnel);
            tunnel.setTunnelColor(station.getColor());
            // Снимаем выделение
         //   selectionManager.deselect();
            selectionManager.select(station);
        } else {
            if (selectionManager.isSelected(station)) {
                selectionManager.deselect();
            } else {
                selectionManager.select(station);
                SOUND_ENGINE.playUISound("set", 1);
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
        Tunnel existingTunnel = GameWorldScreen.getInstance().getWorld().getTunnelAt(x, y);

        if (existing != null) {
            handleExistingStation(existing);
        }
        else if (existingTunnel != null) {
                // Remove tunnel
                handleExistingTunnel(existingTunnel);
            } else {
            handleNewStation(x, y);
            SOUND_ENGINE.playUISound("click", 1);
        }
    }
    private void handleExistingTunnel(Tunnel tunnel) {
        // [GameClickController::handleExistingTunnel] Removing tunnel
        GameWorld gameWorld = (GameWorld) GameWorldScreen.getInstance().getWorld();
        gameWorld.removeTunnel(tunnel);

        // Deselect if was selected
        SelectionManager.getInstance().deselect();

        MetroLogger.logInfo("[GameClickController::handleExistingTunnel] Removed tunnel");
        GameWorldScreen.getInstance().repaint();
    }
    /**
     * Обычный клик - выбор объектов
     */

    public void handleDefaultLeftClick(int x, int y) {
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();

        if (selected != null) {
            selectionManager.deselect();
        }

        // Проверяем объекты в порядке приоритета

        if (trySelectLabel(x, y)) {
            SOUND_ENGINE.playUISound("mouse", 1);
            return;
        }
        if (trySelectStation(x, y)) {
            SOUND_ENGINE.playUISound("mouse", 1);
            return;
        }

        if (trySelectTunnel(x, y)) {
            SOUND_ENGINE.playUISound("mouse", 1);
            return;
        }
        if (trySelectRiverPoint(x, y)) {
            SOUND_ENGINE.playUISound("mouse", 1);
            return;
        }



    }
    /**
     * Try to select RiverPoint at coordinates
     */
    private boolean trySelectRiverPoint(int x, int y) {
        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
        RiverPoint riverPoint = world.getRiverPointAt(x, y);

        if (riverPoint != null) {
            selectObject(riverPoint, x, y);
            return true;
        }
        return false;
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

        }else if (selected instanceof RiverPoint) {
            handleRiverPointDrag((RiverPoint)selected, x, y);
        }
    }

    /*********************************
     * ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
     *********************************/
    /**
     * Handle RiverPoint drag - move point and update river path
     */
    private void handleRiverPointDrag(RiverPoint riverPoint, int x, int y) {
        int newX = x - dragOffset.x;
        int newY = y - dragOffset.y;

        if (performMove(riverPoint, newX, newY)) {
            screen.invalidateCache();
            screen.repaint();
        }
    }
    /**
     * Обработка существующей станции
     */
    private void handleExistingStation(Station station) {
        GameWorld gameWorld = (GameWorld) GameWorldScreen.getInstance().getWorld();

        List<Tunnel> tunnelsToRemove = new ArrayList<>();
        for (Tunnel tunnel : gameWorld.getTunnels()) {
            if (tunnel.getStart() == station || tunnel.getEnd() == station) {
                tunnelsToRemove.add(tunnel);
            }
        }
        for (Tunnel tunnel : tunnelsToRemove) {
            gameWorld.removeTunnel(tunnel);
        }

        // Remove station label if exists
        StationLabel label = gameWorld.getLabelForStation(station);
        if (label != null) {
            gameWorld.removeLabel(label);
        }

        // Remove station itself
        gameWorld.removeStation(station);

        // Deselect if was selected
        SelectionManager.getInstance().deselect();

        MetroLogger.logInfo("[GameClickController::handleExistingStation] Removed station: " + station.getName());
        GameWorldScreen.getInstance().repaint();
    }


    /**
     * Создание новой станции
     */
    private void handleNewStation(int x, int y) {
        if (!isTileFreeFor(null, x, y)) {
            return;
        }

        // Check both keyboard C and color button mode
        if (GameWorldScreen.getInstance().isCPressed || GameWorldScreen.getInstance().colorButtonModeActive) {
            showColorSelectionPopup(x, y);
        } else {
            Station station = new Station(
                    GameWorldScreen.getInstance().getWorld(),
                    x, y,
                    currentStationColor,
                    StationType.REGULAR
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

        // performMove внутри проверит isTileFreeFor и спец-условие для меток (радиус 1 от родителя)
        if (performMove(stationLabel, newX, newY)) {
            GameWorldScreen.getInstance().repaint();
        }
    }

    /**
     * Перетаскивание станции
     */
//    private void handleStationDrag(Station station,int x, int y) {
//
//        int newX = x - dragOffset.x;
//        int newY = y - dragOffset.y;
//
//        if (isPositionValidForStation(newX, newY)) {
//            moveStationTo(station, newX, newY);
//        }
//    }
    private void handleStationDrag(Station station, int x, int y) {
        int newX = x - dragOffset.x;
        int newY = y - dragOffset.y;

        // Используем централизованную проверку
        if (performMove(station, newX, newY)) {
            // Если станция переместилась, нужно пересчитать туннели
            GameWorld world = (GameWorld) screen.getWorld();
            for (Tunnel t : world.getTunnels()) {
                if (t.getStart() == station || t.getEnd() == station) {
                    t.calculatePath();
                }
            }

            // АВТОМАТИЧЕСКОЕ ПЕРЕМЕЩЕНИЕ МЕТКИ
            updateLabelPositionForStation(station);

            GameWorldScreen.getInstance().repaint();
        }
    }
    /**
     * Логика привязки метки: "Всегда справа по умолчанию", иначе ищем место.
     */
    private void updateLabelPositionForStation(Station station) {
        GameWorld world = (GameWorld) screen.getWorld();
        StationLabel label = world.getLabelForStation(station);

        if (label == null) return;

        // 1. Пытаемся поставить СПРАВА (x+1, y)
        int defaultX = station.getX() + 1;
        int defaultY = station.getY();

        // Проверяем, свободна ли дефолтная позиция (игнорируя саму метку, т.к. мы её сейчас двигаем)
        if (isTileFreeFor(label, defaultX, defaultY)) {
            performMove(label, defaultX, defaultY);
            return;
        }

        // 2. Если справа занято, ищем свободную клетку вокруг (по часовой стрелке или как угодно)
        // Порядок проверки: Снизу, Слева, Сверху, потом диагонали
        int[][] offsets = {
                {0, 1}, {-1, 0}, {0, -1}, // Ортогональные
                {1, 1}, {-1, 1}, {-1, -1}, {1, -1} // Диагональные
        };

        for (int[] offset : offsets) {
            int checkX = station.getX() + offset[0];
            int checkY = station.getY() + offset[1];

            if (isTileFreeFor(label, checkX, checkY)) {
                performMove(label, checkX, checkY);
                return;
            }
        }

        // Если места вообще нет - можно скрыть метку или оставить на месте (наезд)
        // В данном случае оставляем как есть или прячем
        // label.setVisible(false);
    }
    /**
     * Перетаскивание туннеля
     */
    private void handleTunnelDrag(Tunnel tunnel, int x, int y) {

        // Проверяем, что новая позиция не совпадает со станцией
        Station stationAtPos = GameWorldScreen.getInstance().getWorld().getStationAt(x, y);
        if (stationAtPos == null) {
            tunnel.moveControlPoint(x, y);
            GameWorldScreen.getInstance().repaint();
        }
    }

//    /**
//     * Перемещение станции
//     */
//    private void moveStationTo(Station station, int newX, int newY) {
//        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
//
//        // Удаляем из старой позиции
//        world.getGameTile(station.getX(), station.getY()).setContent(null);
//        if(world.getRiverPointAt(newX,newY) != null) return;
//        // Обновляем позицию
//        station.x = newX;
//        station.y = newY;
//
//
//        // Добавляем в новую позицию
//        world.getGameTile(newX, newY).setContent(station);
//
//        // Пересчитываем все связанные туннели
//        for (Tunnel t : world.getTunnels()) {
//            if (t.getStart() == station || t.getEnd() == station) {
//                t.calculatePath();
//            }
//        }
//
//        // Перемещаем метку станции
//        StationLabel stationLabel = world.getLabelForStation(station);
//     //   System.out.println("stationLabel " + station.getLabel());
//        if (stationLabel != null) {
//
//            PathPoint newLabelPos = world.findFreePositionNear(station.getX(), station.getY(), station.getName());
//            if (newLabelPos != null) {
//                if(world.getRiverPointAt(newX,newY) != null) return;
//                stationLabel.tryMoveTo(newLabelPos.x, newLabelPos.y);
//
//            }
//        }
//
//
//        GameWorldScreen.getInstance().repaint();
//    }

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
     * ЦЕНТРАЛИЗОВАННАЯ ВАЛИДАЦИЯ
     * Проверяет, может ли объект занять указанную клетку.
     *
     * @param obj Объект, который мы хотим переместить (может быть null, если это новый объект)
     * @param targetX Целевая координата X
     * @param targetY Целевая координата Y
     * @return true, если клетка валидна для размещения
     */
    private boolean isTileFreeFor(GameObject obj, int targetX, int targetY) {
        GameWorld world = (GameWorld) screen.getWorld();

        // 1. Проверка границ мира
        if (targetX < 0 || targetX >= world.getWidth() ||
                targetY < 0 || targetY >= world.getHeight()) {
            return false;
        }

        // 2. Получаем тайл
        GameTile tile = world.getGameTile(targetX, targetY);
        if (tile == null) return false;

        GameObject content = tile.getContent();

        // 3. Клетка свободна, если там пусто ИЛИ там лежит сам перемещаемый объект
        if (content == null) {
            return true;
        }

        // Разрешаем "перемещение" в ту же самую клетку (drag на месте)
        return content == obj;
    }

    /**
     * ЦЕНТРАЛИЗОВАННОЕ ПЕРЕМЕЩЕНИЕ
     * Перемещает объект в новую точку с обновлением сетки мира.
     */
    private boolean performMove(GameObject obj, int newX, int newY) {
        if (!isTileFreeFor(obj, newX, newY)) {
            return false;
        }

        GameWorld world = (GameWorld) screen.getWorld();

        // Дополнительная валидация специфичная для StationLabel
        if (obj instanceof StationLabel) {
            StationLabel label = (StationLabel) obj;
            GameObject parent = label.getParentGameObject();
            if (parent != null) {
                int dx = Math.abs(newX - parent.getX());
                int dy = Math.abs(newY - parent.getY());
                // Метка должна быть строго вокруг станции (радиус 1)
                if (dx > 1 || dy > 1 || (dx == 0 && dy == 0)) {
                    return false;
                }
            }
        }

        // 1. Очищаем старую клетку
        world.getGameTile(obj.getX(), obj.getY()).setContent(null);

        // 2. Обновляем координаты объекта
        // (предполагается наличие метода setPosition или доступ к полям x,y)
        obj.x = newX;
        obj.y = newY;

        // Для RiverPoint нужно обновить родительский путь
        if (obj instanceof RiverPoint) {
            ((RiverPoint) obj).moveTo(newX, newY); // Этот метод внутри вызывает пересчет пути реки
        }

        // 3. Занимаем новую клетку
        world.getGameTile(newX, newY).setContent(obj);

        return true;
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

    public void handleRightClick(int x, int y) {
        KeyboardController keyboard = KeyboardController.getInstance();
        boolean isShiftPressed = keyboard.isKeyPressed(KeyEvent.VK_SHIFT);

        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
        GameObject clickedObject = world.getGameObjectAt(x, y);

        if (clickedObject instanceof Station) {
            Station station = (Station) clickedObject;
            Point screenPoint = screen.worldToScreen(x, y);
            Point windowPoint = SwingUtilities.convertPoint(screen, screenPoint.x, screenPoint.y, null);

            StationTypePopupMenu menu = new StationTypePopupMenu(station, windowPoint.x, windowPoint.y);
            menu.setVisible(true);
        }
        if (clickedObject instanceof Tunnel) {
            Tunnel tunnel = (Tunnel) clickedObject;

            Point screenPoint = screen.worldToScreen(x, y);
            Point windowPoint = SwingUtilities.convertPoint(screen, screenPoint.x, screenPoint.y, null);

            TunnelTypePopupMenu menu = new TunnelTypePopupMenu(tunnel, windowPoint.x, windowPoint.y);
            menu.setVisible(true);
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
//    private boolean isPositionValidForStation(int x, int y) {
//        GameWorld world = (GameWorld) GameWorldScreen.getInstance().getWorld();
//
//        // Проверяем границы мира
//        if (x < 0 || x >= world.getWidth() || y < 0 || y >= world.getHeight()) {
//            return false;
//        }
//
//        // Проверяем, что клетка свободна (нет станции и нет GameplayUnits)
//        return world.getStationAt(x, y) == null || world.getRiverPointAt(x, y) == null;
//    }

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
                    x, y, StationColors.fromColor(colorButton), StationType.REGULAR
            );
            GameWorldScreen.getInstance().getWorld().addStation(newStation);
            GameWorldScreen.getInstance().repaint();
            dialog.dispose();
        });

        return colorBtn;
    }
    /**
     * River brush 3x3 - paints 9 tiles (center + 8 around)
     * Shift inverts (removes water)
     */
    public void handleRiverBrush3x3(int centerX, int centerY) {
        // [GameClickController::handleRiverBrush3x3] Processing river brush at coordinates
        GameWorld world = (GameWorld) screen.getWorld();
        KeyboardController keyboard = KeyboardController.getInstance();
        boolean isShiftPressed = keyboard.isKeyPressed(KeyEvent.VK_SHIFT);
        boolean setWater = !isShiftPressed; // Shift = remove, no shift = add

        // Paint 3x3 area centered on click
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int x = centerX + dx;
                int y = centerY + dy;

                // Check bounds
                if (x >= 0 && x < world.getWidth() && y >= 0 && y < world.getHeight()) {
                    metroline.core.world.tiles.WorldTile tile = world.getWorldTile(x, y);
                    if (tile != null) {
                        tile.setWater(setWater);
                    }
                }
            }
        }

        screen.invalidateCache();
        screen.repaint();
    }

    /**
     * River tool - toggle isWater for tiles. Shift inverts (removes water)
     */
    public void handleRiverTool(int x, int y) {
        // [GameClickController::handleRiverTool] Processing river tool at coordinates
        if (x < 0 || x >= screen.getWorld().getWidth() || y < 0 || y >= screen.getWorld().getHeight()) {
            return;
        }

        GameWorld world = (GameWorld) screen.getWorld();
        metroline.core.world.tiles.WorldTile tile = world.getWorldTile(x, y);

        if (tile != null) {
            KeyboardController keyboard = KeyboardController.getInstance();
            boolean isShiftPressed = keyboard.isKeyPressed(KeyEvent.VK_SHIFT);

            // Shift = remove water (false), no shift = add water (true)
            tile.setWater(!isShiftPressed);
            screen.invalidateCache(); // Force re-render
            screen.repaint();
        }
    }

    /**
     * Add RiverPoint to current river (LMB when river line tool active)
     */
    public void handleRiverLineAddPoint(int x, int y) {
        //  Adding river point
        if (x < 0 || x >= screen.getWorld().getWidth() || y < 0 || y >= screen.getWorld().getHeight()) {
            return;
        }

        GameWorld world = (GameWorld) screen.getWorld();

        // Check if cell is free
        if (world.getGameTile(x, y).getContent() != null) {
            MetroLogger.logInfo(" Cell occupied at: " + x + ", " + y);
            return;
        }

        // Create new river if none active
        if (screen.currentRiver == null) {
            screen.currentRiver = new River(world);
            world.addRiver(screen.currentRiver);
      //      MetroLogger.logInfo(" New river started");
        }

        // Create and add RiverPoint
        RiverPoint point = new RiverPoint(world, x, y, screen.currentRiver);
        world.addRiverPoint(point);
        screen.currentRiver.addPoint(point);

//        MetroLogger.logInfo(" Point added at: " + x + ", " + y +
//                " (total: " + screen.currentRiver.getPoints().size() + ")");

        screen.invalidateCache();
        screen.repaint();
    }

    /**
     * Finish current river and start new one (Alt+LMB)
     */
    public void handleRiverLineFinish(int x, int y) {
        // [GameClickController::handleRiverLineFinish] Finishing river
        if (screen.currentRiver == null) {
            MetroLogger.logInfo("[GameClickController::handleRiverLineFinish] No active river");
            return;
        }

        GameWorld world = (GameWorld) screen.getWorld();

        // Optionally add final point if cell is free
        if (x >= 0 && x < world.getWidth() && y >= 0 && y < world.getHeight()) {
            if (world.getGameTile(x, y).getContent() == null) {
                RiverPoint point = new RiverPoint(world, x, y, screen.currentRiver);
                world.addRiverPoint(point);
                screen.currentRiver.addPoint(point);
            }
        }

        // Validate river (need at least 2 points)
        if (screen.currentRiver.getPoints().size() < 2) {
            // Remove invalid river
            for (RiverPoint p : new ArrayList<>(screen.currentRiver.getPoints())) {
                world.removeRiverPoint(p);
            }
            world.removeRiver(screen.currentRiver);
            MetroLogger.logInfo("[GameClickController::handleRiverLineFinish] River cancelled (< 2 points)");
        } else {
            MetroLogger.logInfo("[GameClickController::handleRiverLineFinish] River completed with " +
                    screen.currentRiver.getPoints().size() + " points");
        }

        // Reset for next river
        screen.currentRiver = null;
        screen.invalidateCache();
        screen.repaint();
    }

    /**
     * Delete RiverPoint or entire river (Shift+LMB)
     */
    public void handleRiverLineDelete(int x, int y) {
        // [GameClickController::handleRiverLineDelete] Deleting at position
        if (x < 0 || x >= screen.getWorld().getWidth() || y < 0 || y >= screen.getWorld().getHeight()) {
            return;
        }

        GameWorld world = (GameWorld) screen.getWorld();

        // Check for RiverPoint at position
        RiverPoint point = world.getRiverPointAt(x, y);
        if (point != null) {
            River parentRiver = point.getParentRiver();
            world.removeRiverPoint(point);

            // If river now has < 2 points, remove entire river
            if (parentRiver != null && parentRiver.getPoints().size() < 2) {
                for (RiverPoint p : new ArrayList<>(parentRiver.getPoints())) {
                    world.removeRiverPoint(p);
                }
                world.removeRiver(parentRiver);

                if (screen.currentRiver == parentRiver) {
                    screen.currentRiver = null;
                }
                MetroLogger.logInfo("[GameClickController::handleRiverLineDelete] River removed (< 2 points)");
            } else {
                MetroLogger.logInfo("[GameClickController::handleRiverLineDelete] Point removed at: " + x + ", " + y);
            }

            screen.invalidateCache();
            screen.repaint();
            return;
        }

        // Check for river path at position (click on river line)
        River river = world.getRiverAt(x, y);
        if (river != null) {
            // Remove entire river and all its points
            for (RiverPoint p : new ArrayList<>(river.getPoints())) {
                world.removeRiverPoint(p);
            }
            world.removeRiver(river);

            if (screen.currentRiver == river) {
                screen.currentRiver = null;
            }

            MetroLogger.logInfo("[GameClickController::handleRiverLineDelete] Entire river removed");
            screen.invalidateCache();
            screen.repaint();
        }
    }

}
