package metroline.input;

import metroline.screens.GameScreen;
import metroline.screens.worldscreens.WorldScreen;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Keyboard controller for game controls
 * @author Tesmio
 */
public class KeyboardController extends KeyAdapter {
    private GameScreen screen;
    private Set<Integer> pressedKeys = new HashSet<>();
    public KeyboardController(GameScreen screen) {
        this.screen = screen;
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .addKeyEventDispatcher(this::dispatchKeyEvent);
    }


    /*********************************
     * KEY HANDLER METHODS SECTION
     *********************************/

    @Override
    public void keyPressed(KeyEvent e) {
        if (screen == null || !screen.hasFocus()) {
            if (screen != null) {
                screen.requestFocusInWindow();
            }
            return;
        }
        if(screen instanceof WorldScreen worldScreen) {

            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
           //     SaveToImageUtil.saveWorldToPNG(screen instanceof SandboxWorldScreen);
                e.consume();
            }
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_D) {
                worldScreen.toggleDebugMode();
                e.consume();
            }
        }
        if(screen instanceof WorldScreen worldScreen) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                worldScreen.isCtrlPressed = true;
                break;
            case KeyEvent.VK_SHIFT:
                worldScreen.isShiftPressed = true;
                break;
            case KeyEvent.VK_SPACE:
                worldScreen.isSpacePressed = true;
                break;
            case KeyEvent.VK_A:
                worldScreen.isAPressed = true;
                break;
            case KeyEvent.VK_C:
                worldScreen.isCPressed = true;
                break;
            case KeyEvent.VK_ESCAPE:
                worldScreen.isEscPressed = true;
                break;
            case KeyEvent.VK_ALT:
                worldScreen.isAltPressed = true;
                break;
        }

        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
        if(screen instanceof WorldScreen worldScreen) {


            switch (e.getKeyCode()) {
                case KeyEvent.VK_CONTROL:
                    worldScreen.isCtrlPressed = false;
                    break;
                case KeyEvent.VK_SHIFT:
                    worldScreen.isShiftPressed = false;
                    break;
                case KeyEvent.VK_SPACE:
                    worldScreen.isSpacePressed = false;
                    break;
                case KeyEvent.VK_A:
                    worldScreen.isAPressed = false;
                    break;
                case KeyEvent.VK_C:
                    worldScreen.isCPressed = false;
                    break;
                case KeyEvent.VK_ESCAPE:
                    worldScreen.isEscPressed = false;
                    break;
                case KeyEvent.VK_ALT:
                    worldScreen.isAltPressed = false;
                    break;
            }
        }
    }

    /*********************************
     * KEY HANDLER AUXILIARY METHODS
     *********************************/

    private boolean dispatchKeyEvent(KeyEvent e) {
        switch (e.getID()) {
            case KeyEvent.KEY_PRESSED:
                pressedKeys.add(e.getKeyCode());
                updateKeyStates();
                break;
            case KeyEvent.KEY_RELEASED:
                pressedKeys.remove(e.getKeyCode());
                updateKeyStates();
                break;
        }
        return false;
    }
    private void updateKeyStates() {
        if(screen instanceof WorldScreen sbScreen) {
            sbScreen.isAltPressed = pressedKeys.contains(KeyEvent.VK_ALT);
            sbScreen.isCtrlPressed = pressedKeys.contains(KeyEvent.VK_CONTROL);
            sbScreen.isSpacePressed = pressedKeys.contains(KeyEvent.VK_SPACE);
            sbScreen.isShiftPressed = pressedKeys.contains(KeyEvent.VK_SHIFT);
            sbScreen.isCPressed = pressedKeys.contains(KeyEvent.VK_C);
            sbScreen.isAPressed = pressedKeys.contains(KeyEvent.VK_A);
        }
    }
}
