package screens;

import util.StyleUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Menu screen implementation
 */
public class MenuScreen extends GameScreen {
    public MenuScreen(MainFrame parent) {
        super(parent);
        setBackground(new Color(30, 30, 30));
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 50, 10, 0);
        gbc.weightx = 0;

        // Title
        JLabel title = new JLabel("METROLINE");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 52));
        gbc.insets = new Insets(0, 30, 50, 0);
        add(title, gbc);

        // Reset insets for buttons
        gbc.insets = new Insets(10, 50, 10, 0);

        // Minimalist buttons aligned to left
        JButton startButton = StyleUtil.createMetrolineButton("New Game",e -> parent.switchScreen("world_settings"));
        JButton loadButton = StyleUtil.createMetrolineButton("Load Game",e -> loadGame(parent));
        JButton exitButton = StyleUtil.createMetrolineButton("Exit", e -> System.exit(0));

        add(startButton, gbc);
        add(loadButton, gbc);
        add(exitButton, gbc);

    }


    private void loadGame(MainFrame parent) {
        // Сначала переключаемся на игровой экран
        parent.switchScreen("game");

        // Затем загружаем мир
        WorldSandboxScreen gameScreen = (WorldSandboxScreen) parent.getCurrentScreen();
        gameScreen.sandboxWorld.loadWorld();

        // Если загрузка не удалась, предлагаем создать новый мир
        if (gameScreen.getWorld() == null) {
            int choice = JOptionPane.showConfirmDialog(parent,
                    "Failed to load game. Create new world?",
                    "Load Error", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
            //    gameScreen.resetWorld();
            }
        }
    }

    @Override
    public void onActivate() {
        // Nothing special needed for menu screen
    }
}
