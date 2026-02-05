package metroline.screens;

import metroline.MainFrame;

import javax.swing.*;

public abstract class GameScreen extends JPanel {
    public MainFrame parent;


    public GameScreen(MainFrame parent) {
        this.parent = parent;
        setFocusable(true);

    }

    /**
     * Called when screen becomes active
     */
    public abstract void onActivate();
    public abstract void onDeactivate();
}
