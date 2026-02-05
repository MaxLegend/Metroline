package metroline.input;

import metroline.input.selection.Selectable;
import metroline.input.selection.SelectionManager;
import metroline.objects.gameobjects.*;
import metroline.objects.gameobjects.StationLabel;

import metroline.screens.panel.GameInfoWindow;
import metroline.screens.worldscreens.WorldScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.normal.GameClickController;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;


/**
 * Mouse controller for world interaction
 * @author Tesmio
 */
public class MouseController extends MouseAdapter {
    private WorldScreen screen;
    private Point lastDragPoint;
    private boolean isRightMouseDragging = false;
    private boolean isLeftMouseDragging = false;

    public MouseController(WorldScreen screen) {
        this.screen = screen;
        this.lastDragPoint = new Point(0, 0);
    }

    /*********************************
     * ОБРАБОТЧИКИ СОБЫТИЙ МЫШИ
     *********************************/

    /**
     * Обработка кликов мыши
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());

        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            handleDoubleClick(e, worldPos);
        }
    }

    /**
     * Обработка нажатия кнопок мыши
     */
    @Override
    public void mousePressed(MouseEvent e) {
        SelectionManager selectionManager = SelectionManager.getInstance();

        if (SwingUtilities.isRightMouseButton(e)) {
            PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
            if (screen instanceof GameWorldScreen) {
                GameWorldScreen gameScreen = (GameWorldScreen) screen;
                gameScreen.worldClickController.handleRightClick(worldPos.x, worldPos.y);

            }
            handleRightMousePressed(e);
        } else if (SwingUtilities.isLeftMouseButton(e)) {
            PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
            // Проверяем, не кликнули ли мы уже на выделенный объект
            Selectable selected = selectionManager.getSelected();
            if (selected instanceof GameObject) {
                GameObject selectedObj = (GameObject) selected;
                // Если клик на уже выделенный объект - начинаем перетаскивание

                if (isClickOnSelectedObject(selectedObj, worldPos.x, worldPos.y)) {
                    isLeftMouseDragging = true;
                    // Сохраняем смещение для перетаскивания
                    GameClickController.dragOffset = new PathPoint(
                            worldPos.x - selectedObj.getX(),
                            worldPos.y - selectedObj.getY()
                    );
                    return;
                }
            }

            // Обычный клик - обрабатываем через контроллер
            handleLeftMousePressed(e, worldPos);
        }
    }


    /**
     * Обработка перетаскивания мыши
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (isRightMouseDragging) {
            handleRightMouseDrag(e);
        } else if (isLeftMouseDragging) {
            handleLeftMouseDrag(e);
        }
    }

    /**
     * Обработка отпускания кнопок мыши
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            handleRightMouseReleased(e);
        } else if (SwingUtilities.isLeftMouseButton(e)) {
            handleLeftMouseReleased(e);
        }
    }

    /**
     * Обработка колесика мыши
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        handleMouseWheel(e);
    }

    /*********************************
     * ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
     *********************************/

    /**
     * Обработка двойного клика
     */

    private void handleDoubleClick(MouseEvent e, PathPoint worldPos) {
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();

    if (screen instanceof GameWorldScreen gScreen) {
                GameObject objectUnderCursor = gScreen.getWorld().getGameObjectAt(worldPos.x, worldPos.y);
                if (objectUnderCursor instanceof Station) {
                    new GameInfoWindow(objectUnderCursor, worldPos.x, worldPos.y, gScreen.worldClickController).setVisible(true);
                }

        }
    }
    /**
     * Обработка нажатия правой кнопки мыши
     */
    private void handleRightMousePressed(MouseEvent e) {
        lastDragPoint = e.getPoint();
        isRightMouseDragging = true;

    }

    /**
     * Обработка нажатия левой кнопки мыши
     */

    private void handleLeftMousePressed(MouseEvent e, PathPoint worldPos) {
        isLeftMouseDragging = true;
        if (screen instanceof GameWorldScreen gsScreen) {
            if (screen.isRiverToolActive) {
                gsScreen.worldClickController.handleRiverTool(worldPos.x, worldPos.y);
            } else if (screen.isRiverBrushToolActive) {
                gsScreen.worldClickController.handleRiverBrush3x3(worldPos.x, worldPos.y);
            } else {
                gsScreen.handleWorldClick(worldPos.x, worldPos.y);
            }
        }
    }
    /**
     * Обработка перетаскивания правой кнопкой мыши
     */
    private void handleRightMouseDrag(MouseEvent e) {
        Point currentPoint = e.getPoint();
        float zoomFactor = 1.0f / screen.getZoom();
        int dx = (int) ((currentPoint.x - lastDragPoint.x) * zoomFactor);
        int dy = (int) ((currentPoint.y - lastDragPoint.y) * zoomFactor);

        screen.setOffset(
                screen.getOffsetX() + dx,
                screen.getOffsetY() + dy
        );
        lastDragPoint = currentPoint;
    }

    private void handleLeftMouseDrag(MouseEvent e) {
        SelectionManager selectionManager = SelectionManager.getInstance();
        Selectable selected = selectionManager.getSelected();

        PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
        if (worldPos == null) return;

        if (screen instanceof GameWorldScreen gamescreen) {
            // River tool has priority during drag
            if (screen.isRiverToolActive) {
                gamescreen.worldClickController.handleRiverTool(worldPos.x, worldPos.y);
                return;
            }
            if (screen.isRiverBrushToolActive) {
                gamescreen.worldClickController.handleRiverBrush3x3(worldPos.x, worldPos.y);
                return;
            }

            // Normal drag handling for selected objects
            if (selected != null) {
                gamescreen.worldClickController.handleEditDrag(worldPos.x, worldPos.y);
            }
        }
    }
    /**
     * Обработка отпускания правой кнопки мыши
     */
    private void handleRightMouseReleased(MouseEvent e) {
        isRightMouseDragging = false;

    }

    /**
     * Обработка отпускания левой кнопки мыши
     */
    private void handleLeftMouseReleased(MouseEvent e) {
        isLeftMouseDragging = false;
        GameClickController.dragOffset = null;
        //    SandboxClickHandler.dragOffset = null;
        if(SelectionManager.getInstance().getSelected() instanceof Tunnel) {
            SelectionManager.getInstance().deselect();
        }

    }
    /**
     * Проверка, кликнули ли на уже выделенный объект
     */
    private boolean isClickOnSelectedObject(GameObject selectedObj, int worldX, int worldY) {
        if (selectedObj instanceof Station) {
            Station station = (Station) selectedObj;
            return worldX == station.getX() && worldY == station.getY();
        } else if (selectedObj instanceof StationLabel) {
            StationLabel stationLabel = (StationLabel) selectedObj;
            return worldX == stationLabel.getX() && worldY == stationLabel.getY();
        } else if (selectedObj instanceof Tunnel) {
            Tunnel tunnel = (Tunnel) selectedObj;
            for (PathPoint p : tunnel.getPath()) {
                if (p.getX() == worldX && p.getY() == worldY) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Обработка колесика мыши (масштабирование)
     */

    private void handleMouseWheel(MouseWheelEvent e) {
        float currentZoom = screen.getZoom();
        int currentOffsetX = screen.getOffsetX();
        int currentOffsetY = screen.getOffsetY();

        // Получаем позицию курсора в мировых координатах ДО изменения зума
        PathPoint worldPosBefore = screen.screenToWorld(e.getX(), e.getY());
        if (worldPosBefore == null) return;

        // Вычисляем новый зум
        float zoomFactor = 1.1f;
        if (e.getWheelRotation() > 0) {
            zoomFactor = 1.0f / zoomFactor; // Уменьшение масштаба
        }

        float newZoom = currentZoom * zoomFactor;


        newZoom = Math.max(0.06f, Math.min(3.0f, newZoom));

        if (Math.abs(currentZoom - newZoom) < 0.01f) return;
     //   DebugInfoRenderer.logMemoryUsage("Before zoom");
        // Устанавливаем новый зум
        screen.setZoom(newZoom);
   //     DebugInfoRenderer.logMemoryUsage("After zoom");
        // Вычисляем новую позицию курсора в экранных координатах после изменения зума
        Point screenPosAfter = screen.worldToScreen(worldPosBefore.x, worldPosBefore.y);
        if (screenPosAfter == null) return;

        // Корректируем смещение так, чтобы точка под курсором осталась на месте
        int dx = e.getX() - screenPosAfter.x;
        int dy = e.getY() - screenPosAfter.y;

        screen.setOffset(currentOffsetX + dx, currentOffsetY + dy);
    }

}

