package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.input.KeyboardController;
import metroline.screens.GameScreen;
import metroline.screens.MenuScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.sandbox.SandboxWorldScreen;
import metroline.util.ImageUtil;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.CachedBackgroundScreen;
import metroline.util.ui.MetrolineButton;

import javax.swing.*;
import java.awt.*;

import static metroline.util.ImageUtil.*;

//TODO разобраться с центровкой
public class WorldMenuScreen extends GameScreen implements CachedBackgroundScreen {
    private Image backgroundImage;
    private boolean backgroundLoaded = false;
    private int bgWidth = 0;
    private int bgHeight = 0;

    public WorldMenuScreen(MainFrame parent) {
        super(parent);
        loadBackgroundImage(this, "background2.png");
      //  setBackground(new Color(30, 30, 30));
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 50, 10, 0);
        gbc.weightx = 0;

        // Title
        JLabel title = new JLabel(LngUtil.translatable("world_menu.title"), SwingUtilities.LEFT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Sans Serif", Font.BOLD, 42));
        gbc.insets = new Insets(0, 0, 50, 0);
        add(title, gbc);

        // Reset insets for buttons
        gbc.insets = new Insets(10, 170, 10, 0);

        // Minimalist buttons aligned to left
        MetrolineButton startButton = MetrolineButton.createMetrolineButton("world_menu.start",e -> parent.switchScreen(MainFrame.GAME_WORLD_SETTINGS_SCREEN_NAME));
        MetrolineButton startButtonSandbox = MetrolineButton.createMetrolineButton("world_menu.start_sandbox",e -> parent.switchScreen(MainFrame.SANDBOX_SETTINGS_SCREEN_NAME));
        MetrolineButton loadSBButton = MetrolineButton.createMetrolineButton("world_menu.load_sandbox",e -> loadGame(parent, true));
        MetrolineButton loadButton = MetrolineButton.createMetrolineButton("world_menu.load_standart",e -> loadGame(parent, false)); //
        MetrolineButton exitButton = MetrolineButton.createMetrolineButton("button.backToMenu", e -> parent.mainFrameUI.backToMenu());

        add(startButton, gbc);
        add(loadButton, gbc);
        add(startButtonSandbox, gbc);
        add(loadSBButton, gbc);

        add(exitButton, gbc);


        parent.translatables.add(startButton);
        parent.translatables.add(startButtonSandbox);
        parent.translatables.add(loadSBButton);
        parent.translatables.add(loadButton);

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

    private void loadGame(MainFrame parent, boolean isSandbox) {
        if(isSandbox) {
            parent.switchScreen(MainFrame.SANDBOX_SCREEN_NAME);
            SandboxWorldScreen gameScreen = (SandboxWorldScreen) parent.getCurrentScreen();
            gameScreen.getWorld().loadWorld();
        } else {
            parent.switchScreen(MainFrame.GAME_SCREEN_NAME);
            GameWorldScreen gameScreen = (GameWorldScreen) parent.getCurrentScreen();
            gameScreen.getWorld().loadWorld();
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
