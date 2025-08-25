package metroline.input.selection;

import metroline.MainFrame;
import metroline.objects.gameobjects.Station;
import metroline.screens.GameScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.sandbox.SandboxWorldScreen;

public class GameSelectionListener implements SelectionListener {
    private MainFrame parentFrame;
    public GameSelectionListener(MainFrame parentFrame) {
        this.parentFrame = parentFrame;
    }
    @Override
    public void onSelectionChanged(Selectable previous, Selectable current) {
        // Получаем текущий активный экран
        GameScreen currentScreen = parentFrame.getCurrentScreen();

        if (currentScreen != null) {
            currentScreen.repaint();
        }
    }
}
