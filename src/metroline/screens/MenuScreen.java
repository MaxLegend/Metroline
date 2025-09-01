package metroline.screens;

import metroline.MainFrame;
import metroline.input.KeyboardController;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.sandbox.SandboxWorldScreen;
import metroline.util.ImageUtil;
import metroline.util.ui.CachedBackgroundScreen;
import metroline.util.ui.MetrolineButton;
import metroline.util.ui.MetrolineFont;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import static metroline.util.ImageUtil.*;

/**
 * Menu screen implementation
 */
public class MenuScreen extends GameScreen implements CachedBackgroundScreen  {
    private Image backgroundImage;
    private boolean backgroundLoaded = false;
    private int bgWidth = 0;
    private int bgHeight = 0;

    public MenuScreen(MainFrame parent) {
        super(parent);
        loadBackgroundImage(this, "background.png");
        initUI(parent);
    }



    private void initUI(MainFrame parent) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 1, 10);

        // Заголовок - занимает весь верхний ряд (3 колонки)
        JLabel title = new JLabel("", SwingUtilities.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        gbc.gridwidth = 3;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.weighty = 0.45;
        add(title, gbc);

        // Создаем кнопки
        MetrolineButton worldMenuButton = MetrolineButton.createMetrolineButton("main_menu.start",
                e -> parent.switchScreen(MainFrame.WORLD_MENU_SCREEN_NAME));
        MetrolineButton globalSettingsButton = MetrolineButton.createMetrolineButton("main_menu.global_settings",
                e -> parent.switchScreen(MainFrame.GLOBAL_SETTINGS_SCREEN_NAME));
        MetrolineButton exitButton = MetrolineButton.createMetrolineButton("main_menu.exit",
                e -> System.exit(0));

        // Панель для кнопок
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridBagLayout());

        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonGbc.fill = GridBagConstraints.HORIZONTAL;
        buttonGbc.insets = new Insets(5, 0, 5, 0);

        Dimension buttonSize = new Dimension(300, 60);
        Font buttonFont = MetrolineFont.getMainFont(20);

        worldMenuButton.setPreferredSize(buttonSize);
        globalSettingsButton.setPreferredSize(buttonSize);
        exitButton.setPreferredSize(buttonSize);
        worldMenuButton.setFont(buttonFont);
        globalSettingsButton.setFont(buttonFont);
        exitButton.setFont(buttonFont);

        buttonPanel.add(worldMenuButton, buttonGbc);
        buttonPanel.add(globalSettingsButton, buttonGbc);
        buttonPanel.add(exitButton, buttonGbc);

        // Размещение панели с кнопками
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0.3;
        gbc.weighty = 0.4;
        add(buttonPanel, gbc);

        // Пустые панели для остальных ячеек сетки
        addEmptyPanel(gbc, 1, 1, 0.4); // Центр
        addEmptyPanel(gbc, 2, 1, 0.3); // Право
        addEmptyPanel(gbc, 0, 2, 0.3); // Низ

        // Локализация
        parent.translatables.add(worldMenuButton);
        parent.translatables.add(globalSettingsButton);
        parent.translatables.add(exitButton);
        parent.updateLanguage();
    }
    @Override
    public void setBackgroundImage(Image image) {
        this.backgroundImage = image;
    }

    @Override
    public void setBackgroundLoaded(boolean loaded) {
        this.backgroundLoaded = loaded;
    }

    @Override
    public void setBackgroundSize(int width, int height) {
        this.bgWidth = width;
        this.bgHeight = height;
    }

    @Override
    public Image getBackgroundImage() {
        return backgroundImage;
    }

    @Override
    public boolean isBackgroundLoaded() {
        return backgroundLoaded;
    }

    @Override
    public int getBackgroundWidth() {
        return bgWidth;
    }

    @Override
    public int getBackgroundHeight() {
        return bgHeight;
    }
    private void addEmptyPanel(GridBagConstraints gbc, int gridx, int gridy, double weightx) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = (gridy == 2) ? 3 : 1;
        gbc.weightx = weightx;

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        add(panel, gbc);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundLoaded && backgroundImage != null) {
            // Используем утилитный метод для отрисовки
            ImageUtil.drawScaledProportional(g, backgroundImage,
                    bgWidth, bgHeight, getWidth(), getHeight());
        } else {
            // Fallback: сплошной цветной фон
            g.setColor(new Color(20, 20, 20));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
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