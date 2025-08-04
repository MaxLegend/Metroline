package screens;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Menu screen implementation
 */
public class MenuScreen extends GameScreen {
    public MenuScreen(MainFrame parent) {
        super(parent);
        setLayout(new GridBagLayout());

        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(e -> parent.switchScreen("game"));

        add(startButton);

    }

    @Override
    public void onActivate() {
        // Nothing special needed for menu screen
    }
}
