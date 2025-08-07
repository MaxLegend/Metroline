package screens;

import game.input.GameClickHandler;
import game.input.SandboxClickHandler;
import game.objects.PathPoint;

import java.awt.*;

/**
 * The main class for summarizing all game mechanics for the basics of the world.
 */
public class WorldScreen extends GameScreen{
    public static WorldScreen INSTANCE;

    //Debug
    public boolean debugMode = false;
    public Font debugFont = new Font("Monospaced", Font.PLAIN, 12);

    public float zoom = 1.0f;
    public  int offsetX = 0;
    public  int offsetY = 0;
    //Central click handler


    // Service keys
    public boolean isEscPressed = false;
    public boolean isAltPressed = false;
    public boolean isCtrlPressed = false;
    public boolean isShiftPressed = false;
    public boolean isCPressed = false;

    public WorldScreen(MainFrame parent) {
        super(parent);

    }



    public static WorldScreen getInstance() {
        return INSTANCE;
    }
    @Override
    public void onActivate() {
        requestFocusInWindow();
    }
    /**
     * Gets the current zoom level
     * @return Zoom level
     */
    public float getZoom() { return zoom; }

    /**
     * Sets the zoom level
     * @param zoom New zoom level
     */
    public void setZoom(float zoom) {
        this.zoom = Math.max(0.1f, Math.min(3.0f, zoom));
        repaint();
    }
    public void toggleDebugMode() {
        debugMode = !debugMode;
        repaint();
    }
    /**
     * Gets the horizontal offset
     * @return X offset
     */
    public int getOffsetX() { return offsetX; }

    /**
     * Gets the vertical offset
     * @return Y offset
     */
    public int getOffsetY() { return offsetY; }

    /**
     * Sets the view offset
     * @param offsetX New X offset
     * @param offsetY New Y offset
     */
    public void setOffset(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        repaint();
    }
    /**
     * Converts screen coordinates to world coordinates
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @return PathPoint in world coordinates
     */
    public PathPoint screenToWorld(int screenX, int screenY) {
        int worldX = (int)((screenX / zoom - offsetX) / 32);
        int worldY = (int)((screenY / zoom - offsetY) / 32);
        return new PathPoint(worldX, worldY);
    }
    /**
     * Converts world coordinates to screen coordinates
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @return Point in screen coordinates
     */
    public Point worldToScreen(int worldX, int worldY) {
        int screenX = (int)((worldX * 32 + offsetX) * zoom);
        int screenY = (int)((worldY * 32 + offsetY) * zoom);
        return new Point(screenX, screenY);
    }
}
