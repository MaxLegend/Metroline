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
        title.setFont(new Font("Arial", Font.BOLD, 32));
        gbc.insets = new Insets(0, 50, 50, 0);
        add(title, gbc);

        // Reset insets for buttons
        gbc.insets = new Insets(10, 50, 10, 0);

        // Minimalist buttons aligned to left
        JButton startButton = createMenuButton("New Game");
        startButton.addActionListener(e -> parent.switchScreen("game"));

        JButton loadButton = createMenuButton("Load Game");
        loadButton.addActionListener(e -> loadGame(parent));

        add(startButton, gbc);
        add(loadButton, gbc);
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(200, 40));
        button.setBackground(new Color(50, 50, 50));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        return button;
    }

    private void loadGame(MainFrame parent) {
        // Сначала переключаемся на игровой экран
        parent.switchScreen("game");

        // Затем загружаем мир
        WorldGameScreen gameScreen = (WorldGameScreen) parent.getCurrentScreen();
        gameScreen.loadWorld();

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
