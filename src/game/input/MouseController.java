package game.input;

import screens.WorldScreen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Mouse controller for world interaction
 */
public class MouseController extends MouseAdapter {
    private WorldScreen screen;

    public MouseController(WorldScreen screen) {
        this.screen = screen;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            screen.startDrag(e.getX(), e.getY());
        } else if (SwingUtilities.isLeftMouseButton(e)) {
            Point worldPos = screen.screenToWorld(e.getX(), e.getY());
            screen.handleClick(worldPos.x, worldPos.y);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            screen.updateDrag(e.getX(), e.getY());
        }
        else if (SwingUtilities.isLeftMouseButton(e) &&
                screen.getCurrentMode() == WorldScreen.GameMode.EDIT) {
            Point worldPos = screen.screenToWorld(e.getX(), e.getY());
            if (worldPos != null) {
                screen.handleEditDrag(worldPos.x, worldPos.y);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            // Stop dragging
            screen.stopDrag();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // Zoom in/out based on wheel movement
        float zoomFactor = 1.0f - e.getWheelRotation() * 0.1f;
        float newZoom = screen.getZoom() * zoomFactor;

        // Convert mouse position to world before zoom
        Point beforeZoom = screen.screenToWorld(e.getX(), e.getY());

        // Apply new zoom
        screen.setZoom(newZoom);

        // Convert mouse position to world after zoom
        Point afterZoom = screen.screenToWorld(e.getX(), e.getY());

        // Adjust offset to keep mouse position stable
        int offsetX = screen.getOffsetX() + (beforeZoom.x - afterZoom.x) * 32;
        int offsetY = screen.getOffsetY() + (beforeZoom.y - afterZoom.y) * 32;
        screen.setOffset(offsetX, offsetY);
    }
}
