package metroline.input;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.screens.GameScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.ImageUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

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

    private void handleKeyPressed(KeyEvent e) {
        // Глобальные комбинации клавиш (работают всегда)
        if (e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_A) {
            mainFrame.mainFrameUI.toggleToolbar();
            e.consume();
            return;
        }

        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
            if(mainFrame.getCurrentScreen() instanceof GameWorldScreen worldScreen) {
                boolean isSandbox = worldScreen != null &&
                        worldScreen.getClass().getSimpleName().contains("Sandbox");
                float zoom = worldScreen.getZoom();
                float offsetX = worldScreen.getOffsetX();
                float offsetY = worldScreen.getOffsetY();
                Rectangle visibleArea = ImageUtil.getVisibleArea(worldScreen, zoom, offsetX, offsetY);
                ImageUtil.saveVisibleAreaToPNG(isSandbox, 4, visibleArea, zoom, offsetX, offsetY);
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

        // Передаем обработку текущему WorldScreen
        if (mainFrame.getCurrentScreen() != null) {
            handleWorldScreenKeyPressed(e);
        }
    }

    private void handleWorldScreenKeyPressed(KeyEvent e) {
        // Обновляем состояния клавиш для WorldScreen
        updateWorldScreenKeyStates();

        if(mainFrame.getCurrentScreen() instanceof GameWorldScreen worldScreen) {
            GameWorld world = (GameWorld) worldScreen.getWorld();

            switch (e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    worldScreen.isEscPressed = true;
                    break;
                case KeyEvent.VK_F2:
                    mainFrame.mainFrameUI.toggleGrassZones();
                    break;
                case KeyEvent.VK_F1:
                     mainFrame.mainFrameUI.toggleGameplayUnits();
                    break;
                case KeyEvent.VK_F3:
                    mainFrame.mainFrameUI.togglePaymentZones();
                    break;
                case KeyEvent.VK_F4:
                    mainFrame.mainFrameUI.togglePassengerZones();
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
