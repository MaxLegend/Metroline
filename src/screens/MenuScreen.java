package screens;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuScreen extends GameScreen {
    public MenuScreen(MainFrame frame) {
        super(frame);
        setLayout(new GridBagLayout());

        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.switchToGameScreen();
            }
        });

        add(startButton);
    }

    @Override
    public void update() {
        // Логика обновления меню
    }
}
