package metroline.screens.worldscreens;

import metroline.core.world.World;
import metroline.input.KeyboardController;
import metroline.input.MouseController;
import metroline.objects.gameobjects.PathPoint;
import metroline.screens.GameScreen;
import metroline.MainFrame;

import java.awt.*;

/**
 * The main class for summarizing all game mechanics for the basics of the world.
 */
public class WorldScreen extends GameScreen {
    public static int widthWorld = 100, heightWorld = 100;
    public  World world;
    //Debug
    public boolean debugMode = false;
    public Font debugFont = new Font("Monospaced", Font.PLAIN, 12);
    public float zoom = 1.0f;
    public  int offsetX = 0;
    public  int offsetY = 0;

    //Central click handler
    public MouseController mouseController;
    public KeyboardController keyboardController;


    // Service keys
    public boolean isEscPressed = false;
    public boolean isAltPressed = false;
    public boolean isCtrlPressed = false;
    public boolean isShiftPressed = false;
    public boolean isCPressed = false;
    public boolean isAPressed = false;

    public boolean isSpacePressed = false;

    public WorldScreen(MainFrame parent) {
        super(parent);

    }
    public WorldScreen(MainFrame parent, World worldIn) {
        super(parent);
        this.world = worldIn;
        // Initialize controllers
        mouseController = new MouseController(this);
        keyboardController = new KeyboardController(this);
        addMouseListener(mouseController);
        addMouseMotionListener(mouseController);
        addMouseWheelListener(mouseController);
        addKeyListener(keyboardController);
    }

    public void reinitializeControllers() {
        // Удаляем старые слушатели
        this.removeMouseListener(mouseController);
        this.removeMouseMotionListener(mouseController);
        this.removeKeyListener(keyboardController);

        // Создаем новые контроллеры
        this.mouseController = new MouseController(this);
        this.keyboardController = new KeyboardController(this);

        // Добавляем слушатели
        this.addMouseListener(mouseController);
        this.addMouseMotionListener(mouseController);
        this.addKeyListener(keyboardController);
        requestFocusInWindow();
    }
    public World getWorld() {
        return world;
    }
    public void setWorld(World worldIn) {
        this.world = worldIn;
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
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            requestFocusInWindow();
        }
    }
}
