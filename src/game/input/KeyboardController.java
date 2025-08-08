package game.input;

import screens.GameScreen;
import screens.WorldGameScreen;
import screens.WorldSandboxScreen;
import screens.WorldScreen;
import util.SaveToImageUtil;


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
            sbScreen.isCtrlPressed = pressedKeys.contains(KeyEvent.VK_CONTROL);
            sbScreen.isShiftPressed = pressedKeys.contains(KeyEvent.VK_SHIFT);
            sbScreen.isCPressed = pressedKeys.contains(KeyEvent.VK_C);
        }
    }
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
                SaveToImageUtil.saveWorldToPNG(screen instanceof WorldSandboxScreen);
                e.consume();
            }
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_D) {
                worldScreen.toggleDebugMode();
                e.consume();
            }
            if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                if (screen instanceof WorldSandboxScreen gamescreen) {
                    gamescreen.sandboxClickHandler.deleteSelectedObject();
                }
                if (screen instanceof WorldGameScreen gamescreen) {
                    gamescreen.gameClickHandler.deleteSelectedObject();
                }
            }
        }
        if(screen instanceof WorldScreen sbScreen) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                sbScreen.isCtrlPressed = true;
                break;
            case KeyEvent.VK_SHIFT:
                sbScreen.isShiftPressed = true;
                break;
            case KeyEvent.VK_C:
                sbScreen.isCPressed = true;
                break;
            case KeyEvent.VK_ESCAPE:
                sbScreen.isEscPressed = true;
                break;
            case KeyEvent.VK_ALT:
                sbScreen.isAltPressed = true;
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
}
