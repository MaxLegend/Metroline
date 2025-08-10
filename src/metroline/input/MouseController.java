package metroline.input;

import metroline.objects.gameobjects.PathPoint;
import metroline.screens.worldscreens.WorldGameScreen;
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
    private Point dragVelocity;



    public MouseController(WorldScreen screen) {
        this.screen = screen;
        this.dragVelocity = new Point(0, 0);

    }
    //Double click
    @Override
    public void mouseClicked(MouseEvent e) {
        PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
        if (screen instanceof WorldSandboxScreen sbScreen) {
            if (!sbScreen.isCtrlPressed && !sbScreen.isShiftPressed && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                sbScreen.sandboxClickHandler.handleEditStationName(worldPos.x, worldPos.y);
                sbScreen.sandboxClickHandler.handleRemoveTunnel(worldPos.x, worldPos.y);
            }
        }
        if (screen instanceof WorldGameScreen gScreen) {
            if(SwingUtilities.isLeftMouseButton(e)&& e.getClickCount() == 2) {
                gScreen.gameClickHandler.handleEditStationName(worldPos.x, worldPos.y);
            }
        }
    }


    @Override
    public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                lastDragPoint = e.getPoint();
                dragVelocity.setLocation(0, 0);
                SandboxClickHandler.startDrag(e.getX(), e.getY());
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
                if(screen instanceof WorldSandboxScreen sbScreen) sbScreen.handleClick(worldPos.x, worldPos.y);
                if(screen instanceof WorldGameScreen gsScreen) {
                    gsScreen.handleWorldClick(worldPos.x, worldPos.y);
                }
            }

    }
    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            Point currentPoint = e.getPoint();
            float zoomFactor = 1.0f / screen.getZoom();
            int dx = (int)((currentPoint.x - lastDragPoint.x) * zoomFactor);
            int dy = (int)((currentPoint.y - lastDragPoint.y) * zoomFactor);

            screen.setOffset(
                    screen.getOffsetX() + dx,
                    screen.getOffsetY() + dy
            );
            lastDragPoint = currentPoint;

        } else if (SwingUtilities.isLeftMouseButton(e)) {
            PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
            if (worldPos != null) {
                if(screen instanceof WorldSandboxScreen sbscreen) {
                    sbscreen.sandboxClickHandler.handleEditDrag(worldPos.x, worldPos.y);
                }
                if(screen instanceof WorldGameScreen gamescreen) {
                    gamescreen.gameClickHandler.handleEditDrag(worldPos.x, worldPos.y);
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(screen instanceof WorldSandboxScreen sbscreen) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                sbscreen.sandboxClickHandler.selectedObject = null;
                SandboxClickHandler.dragOffset = null;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                SandboxClickHandler.stopDrag();
            }
        }
        if(screen instanceof WorldGameScreen gScreen) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                gScreen.gameClickHandler.selectedObject = null;
                GameClickHandler.dragOffset = null;

            }
            if (SwingUtilities.isRightMouseButton(e)) {
                gScreen.gameClickHandler.dragging = false;
            }
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
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
