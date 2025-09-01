package metroline.screens;

import metroline.MainFrame;
import metroline.input.KeyboardController;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.sandbox.SandboxWorldScreen;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.MetrolineButton;

import javax.swing.*;
import java.awt.*;

public class GlobalSettingsScreen extends GameScreen {

    public GlobalSettingsScreen(MainFrame parent) {
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
        JLabel title = new JLabel(LngUtil.translatable("global_settings.title"), SwingUtilities.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 42));
        gbc.insets = new Insets(0, 5, 50, 0);
        add(title, gbc);

        // Reset insets for buttons
        gbc.insets = new Insets(10, 50, 10, 0);

        // Minimalist buttons aligned to left
        MetrolineButton changeLngButton = MetrolineButton.createMetrolineButton("global_settings.change_lng", e -> parent.changeLanguage());
        MetrolineButton exitButton = MetrolineButton.createMetrolineButton("button.backToMenu", e -> parent.mainFrameUI.backToMenu());


        add(changeLngButton, gbc);
        add(exitButton, gbc);

        parent.translatables.add(changeLngButton);
        parent.translatables.add(exitButton);

        parent.updateLanguage();
    }




    @Override
    public void onActivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(this);
        requestFocusInWindow();
    }

    @Override
    public void onDeactivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(null);
    }
}