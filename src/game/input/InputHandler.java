package game.input;

import screens.WorldScreen;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputHandler implements KeyListener {
    private WorldScreen worldScreen;

    public InputHandler(WorldScreen worldScreen) {
        this.worldScreen = worldScreen;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_S:
                worldScreen.setBuildStationMode(!worldScreen.isBuildStationMode());
                if (worldScreen.isBuildStationMode()) {
                    worldScreen.setBuildTunnelMode(false);
                }
                break;

            case KeyEvent.VK_T:
                worldScreen.setBuildTunnelMode(!worldScreen.isBuildTunnelMode());
                if (worldScreen.isBuildTunnelMode()) {
                    worldScreen.setBuildStationMode(false);
                }
                break;

            case KeyEvent.VK_ESCAPE:
                // Сброс режимов
                worldScreen.setBuildStationMode(false);
                worldScreen.setBuildTunnelMode(false);
                worldScreen.setFirstSelectedStation(null);
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}
