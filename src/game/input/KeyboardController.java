package game.input;

import screens.GameScreen;
import screens.WorldSandboxScreen;

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
        WorldSandboxScreen.getInstance().isCtrlPressed = pressedKeys.contains(KeyEvent.VK_CONTROL);
        WorldSandboxScreen.getInstance().isShiftPressed = pressedKeys.contains(KeyEvent.VK_SHIFT);
        WorldSandboxScreen.getInstance().isCPressed = pressedKeys.contains(KeyEvent.VK_C);
    }
    @Override
    public void keyPressed(KeyEvent e) {

        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
            if(screen instanceof WorldSandboxScreen gamescreen) {
                WorldSandboxScreen.getInstance().sandboxWorld.saveWorldToPNG();
                e.consume();
            }
        }
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_D) {

                WorldSandboxScreen.getInstance().toggleDebugMode();
                e.consume();
        }
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            if(screen instanceof WorldSandboxScreen gamescreen) {
                gamescreen.clickHandler.deleteSelectedObject();
            }

        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                WorldSandboxScreen.getInstance().isCtrlPressed = true;
                break;
            case KeyEvent.VK_SHIFT:
                WorldSandboxScreen.getInstance().isShiftPressed = true;
                break;
            case KeyEvent.VK_C:
                WorldSandboxScreen.getInstance().isCPressed = true;
                break;
            case KeyEvent.VK_ESCAPE:
                WorldSandboxScreen.getInstance().isEscPressed = true;
                break;
            case KeyEvent.VK_ALT:
                WorldSandboxScreen.getInstance().isAltPressed = true;
                break;


        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                WorldSandboxScreen.getInstance().isCtrlPressed = false;
                break;
            case KeyEvent.VK_SHIFT:
                WorldSandboxScreen.getInstance().isShiftPressed = false;
                break;
            case KeyEvent.VK_C:

                WorldSandboxScreen.getInstance().isCPressed = false;

                break;
            case KeyEvent.VK_ESCAPE:
                WorldSandboxScreen.getInstance().isEscPressed = false;
                break;
            case KeyEvent.VK_ALT:
                WorldSandboxScreen.getInstance().isAltPressed = false;
                break;
        }
    }
}
