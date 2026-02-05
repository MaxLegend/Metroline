package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.core.world.World;
import metroline.input.MouseController;
import metroline.objects.gameobjects.PathPoint;
import metroline.objects.gameobjects.River;
import metroline.screens.GameScreen;

import java.awt.*;

/**
 * The main class for summarizing all game mechanics for the basics of the world.
 */
public class WorldScreen extends GameScreen {
    public static short widthWorld = 100, heightWorld = 100;
    public  World world;
    //Debug
    public boolean debugMode = false;
    public Font debugFont = new Font("Monospaced", Font.PLAIN, 12);
    public float zoom = 1.0f;
    public  int offsetX = 0;
    public  int offsetY = 0;

    //Central click handler
    public MouseController mouseController;

    private final Runtime runtime = Runtime.getRuntime();
    private long lastGCTime = 0;
    private int frameCount = 0;
    private volatile boolean gcRequested = false;

    // Настройки GC TODO Сделать конфигурируемым
    private static final long GC_MEMORY_THRESHOLD = 512L * 1024 * 1024; // 150MB порог
    private static final long GC_COOLDOWN_MS = 2000; // 2 секунды между вызовами
    private static final int GC_FRAME_INTERVAL = 60; // Каждые 60 кадров
    private static final long MAX_GC_PAUSE_MS = 10; // Максимальная пауза для GC

    // Service keys
    public boolean isEscPressed = false;
    public boolean isAltPressed = false;
    public boolean isCtrlPressed = false;
    public boolean isShiftPressed = false;
    public boolean isCPressed = false;
    public boolean isAPressed = false;

    public boolean isTildePressed = false;
    public boolean isSpacePressed = false;

    public boolean colorButtonModeActive = false;
    public boolean stationButtonModeActive = false;
    public boolean tunnelButtonModeActive = false;
    public boolean destroyButtonModeActive = false;
    public boolean isRiverLineToolActive = false;
    public PathPoint riverLineStartPoint = null;
    public River currentRiver = null;
    public boolean isRiverToolActive = false;
    public boolean isRiverBrushToolActive = false;

    public WorldScreen(MainFrame parent) {
        super(parent);

    }
    public WorldScreen(MainFrame parent, World worldIn) {
        super(parent);
        this.world = worldIn;
        mouseController = new MouseController(this);
        addMouseListener(mouseController);
        addMouseMotionListener(mouseController);
        addMouseWheelListener(mouseController);
    }

    public void reinitializeControllers() {
        // Удаляем старые слушатели
        this.removeMouseListener(mouseController);
        this.removeMouseMotionListener(mouseController);
        this.mouseController = new MouseController(this);
        this.addMouseListener(mouseController);
        this.addMouseMotionListener(mouseController);
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

    }

    @Override
    public void onDeactivate() {

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

    protected void incrementalGarbageCollection() {
        frameCount++;
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long currentTime = System.currentTimeMillis();

        // Условия для запуска GC
        boolean shouldRunGC = (frameCount % GC_FRAME_INTERVAL == 0) &&
                (usedMemory > GC_MEMORY_THRESHOLD) &&
                (currentTime - lastGCTime > GC_COOLDOWN_MS) &&
                !gcRequested;

        if (shouldRunGC) {
            startBackgroundGC();
        }

//        // Логирование памяти каждые 120 кадров
//        if (frameCount % 880 == 0) {
//            logMemoryUsage();
//        }
    }

    private void startBackgroundGC() {
        gcRequested = true;
        lastGCTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                long startTime = System.nanoTime();

                // Soft GC - менее агрессивный
                System.gc();
                System.runFinalization();

                long gcTime = (System.nanoTime() - startTime) / 1000000;

//                System.out.println(String.format(
//                        "Background GC: %dms, Memory: %dMB -> %dMB",
//                        gcTime,
//                        (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
//                        (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
//                             ));

            } catch (Exception e) {
                System.err.println("GC thread error: " + e.getMessage());
            } finally {
                gcRequested = false;
            }
        }, "GC-Thread").start();
    }

    public void controlFrameRate(long frameStartTime) {
        long frameTimeNs = System.nanoTime() - frameStartTime;
        long frameTimeMs = frameTimeNs / 1000000;
        long targetFrameTimeMs = 1000 / 880; // ~7ms для 144 FPS

        if (frameTimeMs < targetFrameTimeMs) {
            try {
                // Небольшая пауза для точного контроля FPS
                Thread.sleep(targetFrameTimeMs - frameTimeMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void logMemoryUsage() {
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();

        System.out.println(String.format(
                "Memory: %dMB / %dMB (%.1f%%) - GC: %s",
                used / 1024 / 1024,
                max / 1024 / 1024,
                (used * 100.0 / max),
                gcRequested ? "active" : "idle"
        ));
    }

    // Метод для ручного вызова GC извне (например, при переключении экранов)
    public void requestImmediateGC() {
        if (!gcRequested) {
            startBackgroundGC();
        }
    }
}
