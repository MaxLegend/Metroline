package screens;

import game.core.GameObject;

import javax.swing.*;

public abstract class GameScreen extends JPanel {
    protected MainFrame parent;


    public GameScreen(MainFrame parent) {
        this.parent = parent;
        setFocusable(true);
    }

    /**
     * Called when screen becomes active
     */
    public abstract void onActivate();
}
