package metroline.screens;

import metroline.MainFrame;
import metroline.screens.worldscreens.WorldGameScreen;
import metroline.screens.worldscreens.WorldSandboxScreen;
import metroline.util.LngUtil;
import metroline.util.StyleUtil;

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
        JLabel title = new JLabel("METROLINE", SwingUtilities.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 42));
        gbc.insets = new Insets(0, 30, 50, 0);
        add(title, gbc);

        // Reset insets for buttons
        gbc.insets = new Insets(10, 50, 10, 0);

        // Minimalist buttons aligned to left
        JButton startButton = StyleUtil.createMetrolineButton(LngUtil.translatable("main_menu.start"),e -> parent.switchScreen(MainFrame.WORLD_SETTINGS_SCREEN_NAME));
        JButton loadSBButton = StyleUtil.createMetrolineButton(LngUtil.translatable("main_menu.load_sandbox"),e -> loadGame(parent, true));
        JButton loadButton = StyleUtil.createMetrolineButton(LngUtil.translatable("main_menu.load_standart"),e -> loadGame(parent, false)); //

        JButton exitButton = StyleUtil.createMetrolineButton(LngUtil.translatable("main_menu.exit"), e -> System.exit(0));

        add(startButton, gbc);
        add(loadSBButton, gbc);
        add(loadButton, gbc);
        add(exitButton, gbc);

    }



    private void loadGame(MainFrame parent, boolean isSandbox) {
        if(isSandbox) {
            parent.switchScreen(MainFrame.SANDBOX_SCREEN_NAME);
            WorldSandboxScreen gameScreen = (WorldSandboxScreen) parent.getCurrentScreen();
            gameScreen.getWorld().loadWorld();
        } else {
            parent.switchScreen(MainFrame.GAME_SCREEN_NAME);
            WorldGameScreen gameScreen = (WorldGameScreen) parent.getCurrentScreen();
            gameScreen.getWorld().loadWorld();
        }
    }

    @Override
    public void onActivate() {

    }
}
