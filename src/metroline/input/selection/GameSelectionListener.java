package metroline.input.selection;

import metroline.MainFrame;

import metroline.screens.GameScreen;

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
