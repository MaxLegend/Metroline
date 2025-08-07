package game.input;

import game.objects.PathPoint;
import game.objects.Station;
import screens.WorldSandboxScreen;
import screens.WorldScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import static game.input.ClickHandler.editStationName;


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
    @Override
    public void mouseClicked(MouseEvent e) {
            if (!screen.isShiftPressed && e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                // Обработка двойного клика
                PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
                handleDoubleClick(worldPos.x, worldPos.y);
            }

    }

    private void handleDoubleClick(int x, int y) {
        Station station = WorldSandboxScreen.getInstance().sandboxWorld.getStationAt(x, y);
        if (station != null) {
            editStationName(station);
        }
    }
    @Override
    public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                lastDragPoint = e.getPoint();
                dragVelocity.setLocation(0, 0);
                ClickHandler.startDrag(e.getX(), e.getY());
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                PathPoint worldPos = screen.screenToWorld(e.getX(), e.getY());
                screen.handleClick(worldPos.x, worldPos.y);
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
                if(screen instanceof WorldSandboxScreen gamescreen) {
                    gamescreen.clickHandler.handleEditDrag(worldPos.x, worldPos.y);
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                screen.clickHandler.selectedObject = null;
                ClickHandler.dragOffset = null;
            }
            if (SwingUtilities.isRightMouseButton(e) ) {
                ClickHandler.stopDrag();
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
