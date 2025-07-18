package game.input;

import screens.WorldScreen;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Keyboard controller for game controls
 */
public class KeyboardController extends KeyAdapter {
    private WorldScreen screen;

    public KeyboardController(WorldScreen screen) {
        this.screen = screen;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_S:
                screen.setMode(WorldScreen.GameMode.STATION);
                break;
            case KeyEvent.VK_T:
                screen.setMode(WorldScreen.GameMode.TUNNEL);
                break;
            case KeyEvent.VK_E:
                screen.setMode(WorldScreen.GameMode.EDIT);
                break;
            case KeyEvent.VK_D:
                screen.toggleDebugMode(); // Переключаем режим отладки
            case KeyEvent.VK_ESCAPE:
                screen.setMode(WorldScreen.GameMode.NONE);
                break;
        }
    }
}
