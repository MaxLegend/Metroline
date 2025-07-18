package screens;

import javax.swing.*;

public abstract class GameScreen extends JPanel {
    protected MainFrame frame;

    public GameScreen(MainFrame frame) {
        this.frame = frame;
    }

    public abstract void update();
}
