package screens;

import game.input.InputHandler;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private GameScreen currentScreen;
    private JToolBar toolBar;

    public MainFrame() {
        setTitle("Metro Constructor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLayout(new BorderLayout());

        initToolBar();
        switchToMenuScreen();
    }

    private void initToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);
        toolBar.setVisible(false);
    }

    public void switchToMenuScreen() {
        if (currentScreen != null) {
            remove(currentScreen);
        }
        currentScreen = new MenuScreen(this);
        add(currentScreen, BorderLayout.CENTER);
        toolBar.setVisible(false);
        revalidate();
        repaint();
    }

    public void switchToGameScreen() {
        if (currentScreen != null) {
            remove(currentScreen);
        }
        currentScreen = new WorldScreen(this);
        add(currentScreen, BorderLayout.CENTER);

        // Добавляем обработчик ввода
        InputHandler inputHandler = new InputHandler((WorldScreen) currentScreen);
        currentScreen.addKeyListener(inputHandler);
        currentScreen.setFocusable(true);
        currentScreen.requestFocusInWindow();

        toolBar.setVisible(true);
        revalidate();
        repaint();
    }
}
