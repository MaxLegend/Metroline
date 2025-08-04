package game.input;

import screens.GameScreen;
import screens.WorldGameScreen;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Keyboard controller for game controls
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
        WorldGameScreen.getInstance().isCtrlPressed = pressedKeys.contains(KeyEvent.VK_CONTROL);
        WorldGameScreen.getInstance().isShiftPressed = pressedKeys.contains(KeyEvent.VK_SHIFT);
        WorldGameScreen.getInstance().isCPressed = pressedKeys.contains(KeyEvent.VK_C);
    }
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                WorldGameScreen.getInstance().isCtrlPressed = true;
                break;
            case KeyEvent.VK_SHIFT:
                WorldGameScreen.getInstance().isShiftPressed = true;

                break;
            case KeyEvent.VK_C:
                WorldGameScreen.getInstance().isCPressed = true;
                break;
            case KeyEvent.VK_DELETE:
                WorldGameScreen.getInstance().deleteSelectedStation();
                break;
            case KeyEvent.VK_D:
                WorldGameScreen.getInstance().toggleDebugMode();
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                WorldGameScreen.getInstance().isCtrlPressed = false;
                break;
            case KeyEvent.VK_SHIFT:
                WorldGameScreen.getInstance().isShiftPressed = false;
                break;
            case KeyEvent.VK_C:

                WorldGameScreen.getInstance().isCPressed = false;

                break;
        }
    }
}
