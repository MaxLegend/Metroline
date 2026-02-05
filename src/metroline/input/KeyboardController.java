package metroline.input;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.screens.GameScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.ImageUtil;
import metroline.util.serialize.GlobalSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import static metroline.MainFrame.SOUND_ENGINE;

/**
 * Keyboard controller for game controls
 * @author Tesmio
 */
public class KeyboardController {
    private static KeyboardController instance;
    private Set<Integer> pressedKeys = new HashSet<>();

    private GameScreen currentScreen;
    private MainFrame mainFrame;

    private KeyboardController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setupGlobalKeyListener();
        setupFocusListener();
    }

    public static void initialize(MainFrame mainFrame) {
        if (instance == null) {
            instance = new KeyboardController(mainFrame);
        }
    }



    public static KeyboardController getInstance() {
        return instance;
    }

    private void setupGlobalKeyListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .addKeyEventDispatcher(this::dispatchGlobalKeyEvent);
    }

    private boolean dispatchGlobalKeyEvent(KeyEvent e) {
        switch (e.getID()) {
            case KeyEvent.KEY_PRESSED:
                if (!pressedKeys.contains(e.getKeyCode())) {
                    pressedKeys.add(e.getKeyCode());
                    handleKeyPressed(e);
                }
                break;

            case KeyEvent.KEY_RELEASED:
                pressedKeys.remove(e.getKeyCode());
                handleKeyReleased(e);
                break;
        }
        return false;
    }
    public void updatePanArroyKeys(GameWorldScreen gScreen) {
        if (isTextFieldFocused()) {
            return;
        }
        // Текущее время в наносекундах
        long currentTime = System.nanoTime();
        // Время, прошедшее с прошлого обновления, в секундах
        float deltaTime = (currentTime - gScreen.lastUpdateTime2) / 1_000_000_000.0f; // наносек → сек
        gScreen.lastUpdateTime2 = currentTime;

        // Желаемая скорость движения: пикселей в секунду (на экране)
        float pixelsPerSecond = 500.0f; // ← НАСТРОЙ ЭТО ЗНАЧЕНИЕ под себя!

        // Ускорение при зажатом Shift
        if (isKeyPressed(KeyEvent.VK_SHIFT)) {
            pixelsPerSecond *= 3;
        }

        // Конвертируем скорость в мировые координаты
        float worldSpeed = pixelsPerSecond / gScreen.getZoom();

        int dx = 0, dy = 0;

        if (isKeyPressed(KeyEvent.VK_DOWN)) {
            dy -= 1;
        }
        if (isKeyPressed(KeyEvent.VK_UP)) {
            dy += 1;
        }
        if (isKeyPressed(KeyEvent.VK_RIGHT)) {
            dx -= 1;
        }
        if (isKeyPressed(KeyEvent.VK_LEFT)) {
            dx += 1;
        }

        // Если есть движение — применяем смещение, пропорциональное времени
        if (dx != 0 || dy != 0) {
            float moveX = dx * worldSpeed * deltaTime;
            float moveY = dy * worldSpeed * deltaTime;

            gScreen.setOffset(
                    gScreen.getOffsetX() + (int) moveX,
                    gScreen.getOffsetY() + (int) moveY
            );
        }
    }
    private void handleKeyPressed(KeyEvent e) {
        if (isTextFieldFocused()) {
            return;
        }
        // Глобальные комбинации клавиш (работают всегда)
        if (e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_A) {
            mainFrame.mainFrameUI.toggleToolbar();
            e.consume();
            return;
        }

        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
            if(mainFrame.getCurrentScreen() instanceof WorldScreen worldScreen) {
                boolean isSandbox = worldScreen != null &&
                        worldScreen.getClass().getSimpleName().contains("Sandbox");
                ImageUtil.saveEntireWorldToPNG(isSandbox, GlobalSettings.getPngScale());
                e.consume();
                return;
            }
        }

        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_D) {

            if(mainFrame.getCurrentScreen() instanceof GameWorldScreen worldScreen) {

                worldScreen.toggleDebugMode();
            }
            e.consume();
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_F11) {
            mainFrame.toggleFullscreen();
            e.consume();
            return;
        }
        if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_8) {
            int toolIndex = e.getKeyCode() - KeyEvent.VK_1;
            mainFrame.mainFrameUI.toggleToolByIndex(toolIndex);
            e.consume();
            return;
        }
        // Передаем обработку текущему WorldScreen
        if (mainFrame.getCurrentScreen() != null) {
            handleWorldScreenKeyPressed(e);
        }
    }
    private boolean isTextFieldFocused() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focusOwner instanceof JTextField || focusOwner instanceof JTextArea;
    }
    private void setupFocusListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .addPropertyChangeListener("focusOwner", evt -> {
                                Component newFocus = (Component) evt.getNewValue();
                                Component oldFocus = (Component) evt.getOldValue();

                                // Если фокус ушел из нашего приложения
                                if (oldFocus != null && newFocus != oldFocus) {
                                    clearAllKeys(); // Сбрасываем все клавиши
                                }
                            });
    }
    private void handleWorldScreenKeyPressed(KeyEvent e) {
        if (isTextFieldFocused()) {
            return;
        }
        // Update key states for WorldScreen
        updateWorldScreenKeyStates();

        if(mainFrame.getCurrentScreen() instanceof GameWorldScreen worldScreen) {
            GameWorld world = (GameWorld) worldScreen.getWorld();

            switch (e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    worldScreen.isEscPressed = true;
                    // Full reset - deselect everything and reset tools
                    mainFrame.mainFrameUI.resetAllTools();
                    break;

                // Hotkeys 1-7 for toolbar buttons
                case KeyEvent.VK_1:
                    mainFrame.mainFrameUI.activateToolByIndex(0); // Station
                    break;
                case KeyEvent.VK_2:
                    mainFrame.mainFrameUI.activateToolByIndex(1); // Tunnel
                    break;
                case KeyEvent.VK_3:
                    mainFrame.mainFrameUI.activateToolByIndex(2); // Destroy
                    break;
                case KeyEvent.VK_4:
                    mainFrame.mainFrameUI.activateToolByIndex(3); // Color
                    break;
                case KeyEvent.VK_5:
                    mainFrame.mainFrameUI.activateToolByIndex(4); // Cancel
                    break;
                case KeyEvent.VK_6:
                    mainFrame.mainFrameUI.activateToolByIndex(5); // River
                    break;
                case KeyEvent.VK_7:
                    mainFrame.mainFrameUI.activateToolByIndex(6); // River Brush
                    break;
                case KeyEvent.VK_8:
                    mainFrame.mainFrameUI.activateToolByIndex(7); // River Brush
                    break;
            }
        }
    }


    private void handleKeyReleased(KeyEvent e) {
        // Обновляем состояния для WorldScreen при отпускании клавиш
        if (mainFrame.getCurrentScreen() != null) {
            updateWorldScreenKeyStates();
        }
    }

    public void setCurrentWorldScreen(GameScreen screen) {
        mainFrame.setCurrentScreen(screen);
        // При смене экрана обновляем состояния клавиш
        if (screen != null) {
            updateWorldScreenKeyStates();
        }
    }

    private void updateWorldScreenKeyStates() {

        if (mainFrame.getCurrentScreen() != null) {
            if(mainFrame.getCurrentScreen() instanceof WorldScreen worldScreen) {
                worldScreen.isAltPressed = isKeyPressed(KeyEvent.VK_ALT);
                worldScreen.isCtrlPressed = isKeyPressed(KeyEvent.VK_CONTROL);
                worldScreen.isSpacePressed = isKeyPressed(KeyEvent.VK_SPACE);
                worldScreen.isShiftPressed = isKeyPressed(KeyEvent.VK_SHIFT);
                worldScreen.isCPressed = isKeyPressed(KeyEvent.VK_C);
                worldScreen.isTildePressed = isKeyPressed(KeyEvent.VK_BACK_QUOTE);
                worldScreen.isAPressed = isKeyPressed(KeyEvent.VK_A);

                worldScreen.isEscPressed = isKeyPressed(KeyEvent.VK_ESCAPE);

            }
        }
    }

    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    public void clearAllKeys() {
        pressedKeys.clear();
        if (mainFrame.getCurrentScreen() != null) {
            resetWorldScreenKeyStates();
        }
    }

    private void resetWorldScreenKeyStates() {
        if(mainFrame.getCurrentScreen() instanceof WorldScreen worldScreen) {
            worldScreen.isAltPressed = false;
            worldScreen.isCtrlPressed = false;
            worldScreen.isSpacePressed = false;
            worldScreen.isShiftPressed = false;
            worldScreen.isCPressed = false;
            worldScreen.isAPressed = false;
            worldScreen.isEscPressed = false;
            worldScreen.isTildePressed = false;
        }
    }
}
