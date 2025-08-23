package metroline.input;

import metroline.MainFrame;
import metroline.screens.GameScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.util.SaveToImageUtil;

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
            if(currentScreen instanceof WorldScreen worldScreen) {
                boolean isSandbox = worldScreen != null &&
                        worldScreen.getClass().getSimpleName().contains("Sandbox");
                SaveToImageUtil.saveWorldToPNG(isSandbox);
                e.consume();
                return;
            }
        }

        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_D) {
            if(currentScreen instanceof WorldScreen worldScreen && currentScreen != null) {
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
        if (currentScreen != null) {
            handleWorldScreenKeyPressed(e);
        }
    }

    private void handleWorldScreenKeyPressed(KeyEvent e) {
        // Обновляем состояния клавиш для WorldScreen
        updateWorldScreenKeyStates();

        // Дополнительная обработка для WorldScreen
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                if(currentScreen instanceof WorldScreen worldScreen) worldScreen.isEscPressed = true;
                break;
            // Добавьте другие специальные обработки по необходимости
        }
    }

    private void handleKeyReleased(KeyEvent e) {
        // Обновляем состояния для WorldScreen при отпускании клавиш
        if (currentScreen != null) {
            updateWorldScreenKeyStates();
        }
    }

    public void setCurrentWorldScreen(GameScreen screen) {
        this.currentScreen = screen;
        // При смене экрана обновляем состояния клавиш
        if (screen != null) {
            updateWorldScreenKeyStates();
        }
    }

    private void updateWorldScreenKeyStates() {
        if (currentScreen != null) {
            if(currentScreen instanceof WorldScreen worldScreen) {
                worldScreen.isAltPressed = isKeyPressed(KeyEvent.VK_ALT);
                worldScreen.isCtrlPressed = isKeyPressed(KeyEvent.VK_CONTROL);
                worldScreen.isSpacePressed = isKeyPressed(KeyEvent.VK_SPACE);
                worldScreen.isShiftPressed = isKeyPressed(KeyEvent.VK_SHIFT);
                worldScreen.isCPressed = isKeyPressed(KeyEvent.VK_C);
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
        if (currentScreen != null) {
            resetWorldScreenKeyStates();
        }
    }

    private void resetWorldScreenKeyStates() {
        if(currentScreen instanceof WorldScreen worldScreen) {
            worldScreen.isAltPressed = false;
            worldScreen.isCtrlPressed = false;
            worldScreen.isSpacePressed = false;
            worldScreen.isShiftPressed = false;
            worldScreen.isCPressed = false;
            worldScreen.isAPressed = false;
            worldScreen.isEscPressed = false;
        }
    }
}
//public class KeyboardController extends KeyAdapter {
//    private GameScreen screen;
//    private Set<Integer> pressedKeys = new HashSet<>();
//    public KeyboardController(GameScreen screen) {
//        this.screen = screen;
//        KeyboardFocusManager.getCurrentKeyboardFocusManager()
//                            .addKeyEventDispatcher(this::dispatchKeyEvent);
//    }
//
//
//    /*********************************
//     * KEY HANDLER METHODS SECTION
//     *********************************/
//    public void setScreen(GameScreen newScreen) {
//        this.screen = newScreen;
//        if (newScreen != null) {
//            newScreen.setFocusable(true);
//            newScreen.requestFocusInWindow();
//        }
//    }
//    @Override
//    public void keyPressed(KeyEvent e) {
//        if (screen == null || !screen.hasFocus()) {
//            if (screen != null) {
//                screen.requestFocusInWindow();
//            }
//            return;
//        }
//        if (!screen.hasFocus()) {
//            screen.requestFocusInWindow();
//            return; // Пропускаем обработку в этом кадре
//        }
//        if(screen instanceof GameScreen) {
//            if(screen instanceof WorldScreen worldScreen) {
//                // Обработка специальных комбинаций
//                if (e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_A) {
//                    MainFrame.getInstance().mainFrameUI.toggleToolbar();
//                    e.consume();
//                }
//                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
//                    SaveToImageUtil.saveWorldToPNG(screen instanceof SandboxWorldScreen);
//                    e.consume();
//                }
//                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_D) {
//                    worldScreen.toggleDebugMode();
//                    e.consume();
//                }
//            }
//            if(screen instanceof WorldScreen worldScreen) {
//                switch (e.getKeyCode()) {
//                    case KeyEvent.VK_CONTROL:
//                        worldScreen.isCtrlPressed = true;
//                        break;
//                    case KeyEvent.VK_SHIFT:
//                        worldScreen.isShiftPressed = true;
//                        break;
//                    case KeyEvent.VK_SPACE:
//                        worldScreen.isSpacePressed = true;
//                        break;
//                    case KeyEvent.VK_A:
//                        worldScreen.isAPressed = true;
//                        break;
//                    case KeyEvent.VK_C:
//                        worldScreen.isCPressed = true;
//                        break;
//                    case KeyEvent.VK_ESCAPE:
//                        worldScreen.isEscPressed = true;
//                        break;
//                    case KeyEvent.VK_ALT:
//                        worldScreen.isAltPressed = true;
//                        break;
//                }
//
//            }
//            if(e.getKeyCode() == KeyEvent.VK_F11) {
//                MainFrame.getInstance().toggleFullscreen();
//                e.consume();
//            }
//
//        }
//
//    }
//    @Override
//    public void keyReleased(KeyEvent e) {
//        if(screen instanceof WorldScreen worldScreen) {
//
//
//            switch (e.getKeyCode()) {
//                case KeyEvent.VK_CONTROL:
//                    worldScreen.isCtrlPressed = false;
//                    break;
//                case KeyEvent.VK_SHIFT:
//                    worldScreen.isShiftPressed = false;
//                    break;
//                case KeyEvent.VK_SPACE:
//                    worldScreen.isSpacePressed = false;
//                    break;
//                case KeyEvent.VK_A:
//                    worldScreen.isAPressed = false;
//                    break;
//                case KeyEvent.VK_C:
//                    worldScreen.isCPressed = false;
//                    break;
//                case KeyEvent.VK_ESCAPE:
//                    worldScreen.isEscPressed = false;
//                    break;
//                case KeyEvent.VK_ALT:
//                    worldScreen.isAltPressed = false;
//                    break;
//            }
//        }
//    }
//
//    /*********************************
//     * KEY HANDLER AUXILIARY METHODS
//     *********************************/
//
//    private boolean dispatchKeyEvent(KeyEvent e) {
//        switch (e.getID()) {
//            case KeyEvent.KEY_PRESSED:
//                pressedKeys.add(e.getKeyCode());
//                updateKeyStates();
//                break;
//            case KeyEvent.KEY_RELEASED:
//                pressedKeys.remove(e.getKeyCode());
//                updateKeyStates();
//                break;
//        }
//        return false;
//    }
//    private void updateKeyStates() {
//        if(screen instanceof WorldScreen sbScreen) {
//            sbScreen.isAltPressed = pressedKeys.contains(KeyEvent.VK_ALT);
//            sbScreen.isCtrlPressed = pressedKeys.contains(KeyEvent.VK_CONTROL);
//            sbScreen.isSpacePressed = pressedKeys.contains(KeyEvent.VK_SPACE);
//            sbScreen.isShiftPressed = pressedKeys.contains(KeyEvent.VK_SHIFT);
//            sbScreen.isCPressed = pressedKeys.contains(KeyEvent.VK_C);
//            sbScreen.isAPressed = pressedKeys.contains(KeyEvent.VK_A);
//        }
//    }
//}