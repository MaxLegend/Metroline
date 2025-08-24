package metroline.input;

import metroline.objects.gameobjects.GameObject;
import metroline.objects.gameobjects.PathPoint;
import metroline.objects.gameobjects.Train;
import metroline.screens.worldscreens.WorldScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.normal.WorldClickController;
import metroline.screens.worldscreens.sandbox.SandboxClickHandler;
import metroline.screens.worldscreens.sandbox.SandboxWorldScreen;

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
        if (SwingUtilities.isRightMouseButton(e)) {
            PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());

            if (screen instanceof GameWorldScreen) {
                GameWorldScreen gameScreen = (GameWorldScreen) screen;

                gameScreen.worldClickController.handleRightClick(worldPos.x, worldPos.y);
            }
            handleRightMousePressed(e);
        } else if (SwingUtilities.isLeftMouseButton(e)) {
            handleLeftMousePressed(e);
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

        if (screen instanceof SandboxWorldScreen sbScreen) {
            if (!sbScreen.isCtrlPressed && !sbScreen.isShiftPressed) {
                sbScreen.sandboxClickHandler.handleEditStationName(worldPos.x, worldPos.y);
                sbScreen.sandboxClickHandler.handleRemoveTunnel(worldPos.x, worldPos.y);
            }
        } else if (screen instanceof GameWorldScreen gScreen) {
            // Проверяем выделенный объект, а не объект под курсором
            GameObject selected = gScreen.worldClickController.getSelectedObject();

            if (selected instanceof Train train) {
                System.out.println("Double click on train: " + train.getName());
                gScreen.showTrainInfo(train, worldPos.x, worldPos.y);
            } else {
                // Для других объектов используем старую логику
                GameObject objectUnderCursor = gScreen.getWorld().getGameObjectAt(worldPos.x, worldPos.y);
                gScreen.showInfoPanel(objectUnderCursor, worldPos.x, worldPos.y);
            }
        }
    }

    /**
     * Обработка нажатия правой кнопки мыши
     */
    private void handleRightMousePressed(MouseEvent e) {
        lastDragPoint = e.getPoint();
        isRightMouseDragging = true;

        if (screen instanceof SandboxWorldScreen) {
            SandboxClickHandler.startDrag(e.getX(), e.getY());
        }
    }

    /**
     * Обработка нажатия левой кнопки мыши
     */
    private void handleLeftMousePressed(MouseEvent e) {
        PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
        isLeftMouseDragging = true;

        if (screen instanceof SandboxWorldScreen sbScreen) {
            sbScreen.handleClick(worldPos.x, worldPos.y);
        } else if (screen instanceof GameWorldScreen gsScreen) {
            gsScreen.handleWorldClick(worldPos.x, worldPos.y);
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

    /**
     * Обработка перетаскивания левой кнопкой мыши
     */
    private void handleLeftMouseDrag(MouseEvent e) {
        PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
        if (worldPos != null) {
            if (screen instanceof SandboxWorldScreen sbscreen) {
                sbscreen.sandboxClickHandler.handleEditDrag(worldPos.x, worldPos.y);
            } else if (screen instanceof GameWorldScreen gamescreen) {
                gamescreen.worldClickController.handleEditDrag(worldPos.x, worldPos.y);
            }
        }
    }

    /**
     * Обработка отпускания правой кнопки мыши
     */
    private void handleRightMouseReleased(MouseEvent e) {
        isRightMouseDragging = false;

        if (screen instanceof SandboxWorldScreen) {
            SandboxClickHandler.stopDrag();
        }
    }

    /**
     * Обработка отпускания левой кнопки мыши
     */
    private void handleLeftMouseReleased(MouseEvent e) {
        isLeftMouseDragging = false;

        if (screen instanceof SandboxWorldScreen sbscreen) {
            sbscreen.sandboxClickHandler.selectedObject = null;
            SandboxClickHandler.dragOffset = null;
        } else if (screen instanceof GameWorldScreen gScreen) {
            gScreen.worldClickController.selectedObject = null;
            WorldClickController.dragOffset = null;
        }
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

