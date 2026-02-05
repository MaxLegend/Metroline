package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.input.KeyboardController;
import metroline.screens.GameScreen;
import metroline.screens.MenuScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;

import metroline.util.ImageUtil;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.*;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

import static metroline.util.ImageUtil.*;

//FIXME разобраться с центровкой
public class WorldMenuScreen extends GameScreen implements CachedBackgroundScreen {
    private Image backgroundImage;
    private boolean backgroundLoaded = false;
    private int bgWidth = 0;
    private int bgHeight = 0;

    public WorldMenuScreen(MainFrame parent) {
        super(parent);

        // 1. Подготовка фона (как в MenuScreen)
        String[] backgroundCandidates = {"bc5.png"};
        for (String imageName : backgroundCandidates) {
            ImageCacheUtil.loadCachedImage("/backgrounds/" + imageName);
        }
        String selectedBackground = backgroundCandidates[new Random().nextInt(backgroundCandidates.length)];
        ImageUtil.loadBackgroundImage(this, selectedBackground);

        // 2. Вызываем инициализацию интерфейса
            initUI(parent);
    }
    private void initUI(MainFrame parent) {
        // Основная компоновка экрана — BorderLayout
        setLayout(new BorderLayout());

        // === 1. Панель основного контента с текущей сеткой (без нижнего ряда) ===
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 1, 10);

        // Заголовок
        JLabel title = new JLabel("", SwingUtilities.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.weighty = 0.45;
        gbc.insets = new Insets(0, 70, 20, 0);
        contentPanel.add(title, gbc);

        // Кнопки
        MetrolineButton startButton = MetrolineButton.createMetrolineButton("world_menu.start",
                e -> parent.switchScreen(MainFrame.GAME_WORLD_SETTINGS_SCREEN_NAME));
        MetrolineButton loadButton = MetrolineButton.createMetrolineButton("world_menu.load_standart",
                e -> parent.switchScreen(MainFrame.LOAD_GAME_SCREEN_NAME));
        MetrolineButton exitButton = MetrolineButton.createMetrolineButton("button.backToMenu",
                e -> parent.mainFrameUI.backToMenu());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridBagLayout());

        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonGbc.fill = GridBagConstraints.HORIZONTAL;
        buttonGbc.insets = new Insets(5, 0, 5, 0);

        Dimension buttonSize = new Dimension(300, 60);
        Font buttonFont = MetrolineFont.getMainFont(20);

        startButton.setPreferredSize(buttonSize);
        loadButton.setPreferredSize(buttonSize);
        exitButton.setPreferredSize(buttonSize);
        startButton.setFont(buttonFont);
        loadButton.setFont(buttonFont);
        exitButton.setFont(buttonFont);

        buttonPanel.add(startButton, buttonGbc);
        buttonPanel.add(loadButton, buttonGbc);
        buttonPanel.add(exitButton, buttonGbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0.3;
        gbc.weighty = 1.5; // Заполняет оставшееся пространство
        contentPanel.add(buttonPanel, gbc);

        // Пустые панели для баланса сетки (только ряды 0 и 1)
        addEmptyPanel(contentPanel, gbc, 1, 1, 0.4); // Центр
        addEmptyPanel(contentPanel, gbc, 2, 1, 0.3); // Право

        // Добавляем основной контент в центр экрана
        add(contentPanel, BorderLayout.CENTER);

        // === 2. Информационная панель — абсолютно независимая от сетки ===
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 0)); // Отступы: верх, лево, низ, право

        MetrolineLabel poweredByLabel = new MetrolineLabel("powered_by");
        MetrolineLabel versionLabel = new MetrolineLabel("version");
        MetrolineLabel authorLabel = new MetrolineLabel("author");

        Font infoFont = MetrolineFont.getMainFont(13);
        Color infoColor = new Color(220, 220, 220, 200);

        poweredByLabel.setFont(infoFont);
        poweredByLabel.setForeground(infoColor);
        versionLabel.setFont(infoFont);
        versionLabel.setForeground(infoColor);
        authorLabel.setFont(infoFont);
        authorLabel.setForeground(infoColor);

        infoPanel.add(versionLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(authorLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(poweredByLabel);

        // Регистрация для локализации
        parent.translatables.add(poweredByLabel);
        parent.translatables.add(versionLabel);
        parent.translatables.add(authorLabel);

        // Добавляем информационную панель вниз экрана — НЕ в сетку!
        add(infoPanel, BorderLayout.SOUTH);

        // Локализация кнопок
        parent.translatables.add(startButton);
        parent.translatables.add(loadButton);
        parent.translatables.add(exitButton);
        parent.updateLanguage();
    }
    private void addEmptyPanel(JPanel targetPanel, GridBagConstraints gbc, int gridx, int gridy, double weightx) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = 1;
        gbc.weightx = weightx;
        gbc.weighty = (gridy == 0) ? 0.45 : 0.55; // Сохраняем веса рядов

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        targetPanel.add(panel, gbc);
    }
//    private void initUI(MainFrame parent) {
//        setLayout(new GridBagLayout());
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.insets = new Insets(10, 10, 1, 10);
//
//        // Заголовок - занимает весь верхний ряд (3 колонки)
//        JLabel title = new JLabel("", SwingUtilities.CENTER);
//        title.setForeground(Color.WHITE);
//        title.setFont(new Font("Arial", Font.BOLD, 48));
//        gbc.gridx = 0;
//        gbc.gridy = 0;
//        gbc.gridwidth = 1;
//        gbc.anchor = GridBagConstraints.SOUTHWEST;
//        gbc.weighty = 0.45; // "Толкает" заголовок на нужную высоту
//        gbc.insets = new Insets(0, 70, 20, 0); // Отступ 70 слева для стиля
//        add(title, gbc);
//
//        MetrolineButton startButton = MetrolineButton.createMetrolineButton("world_menu.start",
//                e -> parent.switchScreen(MainFrame.GAME_WORLD_SETTINGS_SCREEN_NAME));
//        MetrolineButton loadButton = MetrolineButton.createMetrolineButton("world_menu.load_standart",
//                e -> parent.switchScreen(MainFrame.LOAD_GAME_SCREEN_NAME));
//        MetrolineButton exitButton = MetrolineButton.createMetrolineButton("button.backToMenu",
//                e -> parent.mainFrameUI.backToMenu());
//
//        // Панель для кнопок
//        JPanel buttonPanel = new JPanel();
//        buttonPanel.setOpaque(false);
//        buttonPanel.setLayout(new GridBagLayout());
//
//        GridBagConstraints buttonGbc = new GridBagConstraints();
//        buttonGbc.gridwidth = GridBagConstraints.REMAINDER;
//        buttonGbc.fill = GridBagConstraints.HORIZONTAL;
//        buttonGbc.insets = new Insets(5, 0, 5, 0);
//
//        Dimension buttonSize = new Dimension(300, 60);
//        Font buttonFont = MetrolineFont.getMainFont(20);
//
//        startButton.setPreferredSize(buttonSize);
//        loadButton.setPreferredSize(buttonSize);
//        exitButton.setPreferredSize(buttonSize);
//        startButton.setFont(buttonFont);
//        loadButton.setFont(buttonFont);
//        exitButton.setFont(buttonFont);
//
//        buttonPanel.add(startButton, buttonGbc);
//        buttonPanel.add(loadButton, buttonGbc);
//        buttonPanel.add(exitButton, buttonGbc);
//
//        // Размещение панели с кнопками
//        gbc.gridwidth = 1;
//        gbc.gridx = 0;
//        gbc.gridy = 1;
//        gbc.anchor = GridBagConstraints.CENTER;
//        gbc.weightx = 0.3;
//        gbc.weighty = 0.4;
//        add(buttonPanel, gbc);
//
//        // Пустые панели для остальных ячеек сетки
//        addEmptyPanel(gbc, 1, 1, 0.4); // Центр
//        addEmptyPanel(gbc, 2, 1, 0.3); // Право
//        addEmptyPanel(gbc, 0, 2, 0.3); // Низ
//
//        // Локализация
//        parent.translatables.add(startButton);
//        parent.translatables.add(loadButton);
//        parent.translatables.add(exitButton);
//        parent.updateLanguage();
//    }

    private void setupButton(MetrolineButton btn, Dimension size, Font font, MainFrame parent) {
        btn.setPreferredSize(size);
        btn.setFont(font);
        parent.translatables.add(btn);
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
   {
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
