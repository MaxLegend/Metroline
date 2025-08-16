package metroline.input;

import metroline.objects.gameobjects.GameObject;
import metroline.objects.gameobjects.PathPoint;
import metroline.screens.worldscreens.gameworld.GameWorldScreen;
import metroline.screens.worldscreens.WorldSandboxScreen;
import metroline.screens.worldscreens.WorldScreen;

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

        if (screen instanceof WorldSandboxScreen sbScreen) {
            if (!sbScreen.isCtrlPressed && !sbScreen.isShiftPressed) {
                sbScreen.sandboxClickHandler.handleEditStationName(worldPos.x, worldPos.y);
                sbScreen.sandboxClickHandler.handleRemoveTunnel(worldPos.x, worldPos.y);
            }
        } else if (screen instanceof GameWorldScreen gScreen) {
            GameObject selectedObject = gScreen.getWorld().getGameObjectAt(worldPos.x, worldPos.y);
            gScreen.parent.showInfoPanel(selectedObject, worldPos.x, worldPos.y);
        }
    }

    /**
     * Обработка нажатия правой кнопки мыши
     */
    private void handleRightMousePressed(MouseEvent e) {
        lastDragPoint = e.getPoint();
        isRightMouseDragging = true;

        if (screen instanceof WorldSandboxScreen) {
            SandboxClickHandler.startDrag(e.getX(), e.getY());
        }
    }

    /**
     * Обработка нажатия левой кнопки мыши
     */
    private void handleLeftMousePressed(MouseEvent e) {
        PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
        isLeftMouseDragging = true;

        if (screen instanceof WorldSandboxScreen sbScreen) {
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
            if (screen instanceof WorldSandboxScreen sbscreen) {
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

        if (screen instanceof WorldSandboxScreen) {
            SandboxClickHandler.stopDrag();
        }
    }

    /**
     * Обработка отпускания левой кнопки мыши
     */
    private void handleLeftMouseReleased(MouseEvent e) {
        isLeftMouseDragging = false;

        if (screen instanceof WorldSandboxScreen sbscreen) {
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

        PathPoint worldPosBefore = screen.screenToWorld(e.getX(), e.getY());
        float zoomDelta = -e.getWheelRotation() * 0.1f;
        float newZoom = currentZoom * (1 + zoomDelta);
        newZoom = Math.max(0.1f, Math.min(3.0f, newZoom));

        if (currentZoom == newZoom) return;

        screen.setZoom(newZoom);

        Point screenPosAfter = screen.worldToScreen(worldPosBefore.x, worldPosBefore.y);
        int dx = e.getX() - screenPosAfter.x;
        int dy = e.getY() - screenPosAfter.y;

        screen.setOffset(
                currentOffsetX + dx,
                currentOffsetY + dy
        );
    }
}

