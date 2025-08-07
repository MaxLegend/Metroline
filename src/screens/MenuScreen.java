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
        title.setFont(new Font("Arial", Font.BOLD, 42));
        gbc.insets = new Insets(0, 30, 50, 0);
        add(title, gbc);

        // Reset insets for buttons
        gbc.insets = new Insets(10, 50, 10, 0);

        // Minimalist buttons aligned to left
        JButton startButton = StyleUtil.createMetrolineButton("New Game",e -> parent.switchScreen(MainFrame.WORLD_SETTINGS_SCREEN_NAME));
        JButton loadSBButton = StyleUtil.createMetrolineButton("Load Sandbox Game",e -> loadGame(parent, true));
        JButton loadButton = StyleUtil.createMetrolineButton("Load Game",e -> loadGame(parent, false));

        JButton exitButton = StyleUtil.createMetrolineButton("Exit", e -> System.exit(0));

        add(startButton, gbc);
        add(loadSBButton, gbc);
        add(loadButton, gbc);
        add(exitButton, gbc);

    }


    private void loadGame(MainFrame parent, boolean isSandbox) {
        if(isSandbox)
        parent.switchScreen(MainFrame.SANDBOX_SCREEN_NAME);
        else parent.switchScreen(MainFrame.GAME_SCREEN_NAME);

        if(isSandbox) {
            WorldSandboxScreen gameScreen = (WorldSandboxScreen) parent.getCurrentScreen();
            gameScreen.sandboxWorld.loadWorld();
        } else {
            WorldGameScreen gameScreen = (WorldGameScreen) parent.getCurrentScreen();
            gameScreen.gameWorld.loadWorld();
        }
    }

    @Override
    public void onActivate() {
        // Nothing special needed for menu screen
    }
}
