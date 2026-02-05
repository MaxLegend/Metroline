package metroline.screens.worldscreens.normal;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.input.KeyboardController;
import metroline.objects.gameobjects.GameConstants;
import metroline.screens.GameScreen;
import metroline.util.ColorUtil;
import metroline.util.ImageUtil;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

//TODO Настройки для случайных событий
public class GameWorldSettingsScreen extends GameScreen implements CachedBackgroundScreen {
    private Image backgroundImage;
    private boolean backgroundLoaded = false;
    private int bgWidth = 0;
    private int bgHeight = 0;
    public static final Color[] WORLD_COLORS = {
            new Color(203, 202, 202), // Светло-серый
            new Color(110, 110, 110), // Серый (по умолчанию)
            new Color(70, 70, 70),    // Темно-серый
            new Color(150, 100, 80),  // Коричневый
            new Color(80, 100, 150),   // Голубоватый
            new Color(100, 120, 100)  // Зеленовато-серый
    };

    private static int worldColorIndex = 1;

    private MetrolineSlider widthSlider;
    private MetrolineSlider heightSlider;

    private MetrolineCheckbox roundStationsCheck;

    private MainFrame parent;
    private MetrolineButton colorButton; // Кнопка для выбора цвета
    private int worldColor = 0x6E6E6E; // Цвет по умолчанию


    // Метки для новых слайдеров
    private MetrolineLabel centerTitle = new MetrolineLabel("", SwingConstants.CENTER);
    private MetrolineLabel widthLabel;
    private MetrolineLabel heightLabel;

    public GameWorldSettingsScreen(MainFrame parent) {
        super(parent);
        this.parent = parent;
        String[] backgroundCandidates = { "bc5.png"};
        for (String imageName : backgroundCandidates) {
            ImageCacheUtil.loadCachedImage("/backgrounds/" + imageName);
        }
        String selectedBackground = backgroundCandidates[new Random().nextInt(backgroundCandidates.length)];
        ImageUtil.loadBackgroundImage(this, selectedBackground);
        setLayout(new BorderLayout(0, 0)); // Changed to BorderLayout with horizontal gap
        initUI();

        parent.updateLanguage();
    }


    private void initUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Заголовок
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.weighty = 0.4;
        gbc.insets = new Insets(0, 100, 20, 0); // Увеличили отступ слева до 100

        centerTitle.setFont(new Font("Arial", Font.BOLD, 42));
        centerTitle.setHorizontalAlignment(SwingConstants.LEFT);
        add(centerTitle, gbc);

        // Панель настроек
        JPanel settingsPanel = createSettingsPanel();

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.5; // Увеличили вес колонки, чтобы она была шире
        gbc.weighty = 0.6;
        gbc.anchor = GridBagConstraints.NORTHWEST; // Строго на север-запад
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 100, 0, 0);

        add(settingsPanel, gbc);

        // Заполнитель справа, чтобы левая часть не расползалась на весь экран
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        add(new JPanel() {{ setOpaque(false); }}, gbc);
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Константы размеров для "Жесткого" столбца
        int totalColumnWidth = 500; // Ширина всего столбца
        Dimension fullSize = new Dimension(totalColumnWidth, 45);
        Dimension halfBtnSize = new Dimension((totalColumnWidth - 14) / 2, 45); // Кнопки внизу с учетом зазора

        // 1. Слайдеры (теперь на всю ширину столбца)
        widthSlider = new MetrolineSlider("width_world_slider_desc", 20, 400, 40, 10);
        heightSlider = new MetrolineSlider("height_world_slider_desc", 20, 400, 40, 10);
        widthLabel = new MetrolineLabel(widthSlider.getValue() + " M");
        widthLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        heightLabel = new MetrolineLabel(heightSlider.getValue() + " M");
        heightLabel.setForeground(StyleUtil.FOREGROUND_COLOR);

        panel.add(createRow("world.width", widthSlider, widthLabel, fullSize));
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(createRow("world.height", heightSlider, heightLabel, fullSize));

        panel.add(Box.createRigidArea(new Dimension(0, 25)));

        // 2. Чекбокс
        roundStationsCheck = StyleUtil.createMetrolineCheckBox("world.round_stations", "world.round_stations_desc");
        roundStationsCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        roundStationsCheck.setOpaque(false);
        // Принудительно задаем ширину чекбоксу, чтобы кликабельная зона была предсказуемой
        roundStationsCheck.setMaximumSize(fullSize);
        panel.add(roundStationsCheck);

        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 3. Кнопка выбора цвета (теперь на всю ширину столбца)
        colorButton = MetrolineButton.createMetrolineColorableButton("world.color", e -> showWindowColorSelection(), new Color(worldColor));
        colorButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        colorButton.setMaximumSize(fullSize);
        colorButton.setPreferredSize(fullSize);
        panel.add(colorButton);

        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 4. Панель кнопок (Назад + Создать)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(fullSize); // Панель тоже на всю ширину

        MetrolineButton backButton = new MetrolineButton("world.back", e -> parent.switchScreen("menu"));
        MetrolineButton createGameButton = new MetrolineButton("world.create_standart", e -> createWorld());

        // Задаем им одинаковые размеры (половина столбца)
        backButton.setPreferredSize(halfBtnSize);
        createGameButton.setPreferredSize(halfBtnSize);

        buttonPanel.add(backButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(14, 0))); // Промежуток между кнопками
        buttonPanel.add(createGameButton);

        panel.add(buttonPanel);

        registerTranslatables(backButton, createGameButton);
        return panel;
    }


    private void registerTranslatables(MetrolineButton back, MetrolineButton create) {
        parent.translatables.add(centerTitle);
        parent.translatables.add(widthSlider);
        parent.translatables.add(heightSlider);
        parent.translatables.add(heightLabel);
        parent.translatables.add(widthLabel);
        parent.translatables.add(colorButton);
        parent.translatables.add(roundStationsCheck);
        parent.translatables.add(back);
        parent.translatables.add(create);
    }


    private JPanel createRow(String labelKey, MetrolineSlider slider, MetrolineLabel valueLabel, Dimension size) {
        JPanel row = new JPanel(new BorderLayout(20, 0));
        row.setOpaque(false);
        row.setMaximumSize(size);
        row.setPreferredSize(size);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        MetrolineLabel titleLabel = StyleUtil.createMetrolineLabel(labelKey, SwingConstants.LEFT);
        // 140 пикселей хватит даже для длинных названий, чтобы они не толкали слайдер
        titleLabel.setPreferredSize(new Dimension(140, 30));

        valueLabel.setPreferredSize(new Dimension(70, 30));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        slider.setValueLabel(valueLabel, " M ");

        row.add(titleLabel, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(valueLabel, BorderLayout.EAST);

        return row;
    }
    private void showWindowColorSelection() {
        // Создаем диалоговое окно
        JDialog colorDialog = new JDialog(SwingUtilities.getWindowAncestor(this));
        colorDialog.setUndecorated(true);
        colorDialog.setBackground(new Color(0, 0, 0, 0));

        // Панель с цветами
        JPanel colorPanel = new JPanel(new GridLayout(1, WORLD_COLORS.length, 5, 0));
        colorPanel.setBorder(BorderFactory.createLineBorder(StyleUtil.FOREGROUND_COLOR, 2));
        colorPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        colorPanel.setOpaque(true);

        // Создаем кнопки для каждого цвета
        for (Color color : WORLD_COLORS) {
            JButton colorBtn = new JButton();
            colorBtn.setBackground(color);
            colorBtn.setPreferredSize(new Dimension(40, 40));
            colorBtn.setBorder(BorderFactory.createEmptyBorder());
            colorBtn.setContentAreaFilled(false);
            colorBtn.setOpaque(true);
            colorBtn.setFocusPainted(false);

            colorBtn.addActionListener(e -> {

                worldColor = ColorUtil.colorToRGB(color);
               updateColorButtonAppearance();
                colorDialog.dispose();
            });

            // Эффект при наведении
            colorBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    colorBtn.setBackground(StyleUtil.changeColorShade(color, -40));
                }
                public void mouseExited(MouseEvent e) {
                    colorBtn.setBackground(color);
                }
            });

            colorPanel.add(colorBtn);
        }

        colorDialog.add(colorPanel);
        colorDialog.pack();

        // Позиционируем окно рядом с кнопкой
        Point buttonLoc = colorButton.getLocationOnScreen();
        colorDialog.setLocation(
                buttonLoc.x + colorButton.getWidth()/2 - colorDialog.getWidth()/2,
                buttonLoc.y + colorButton.getHeight() + 5
        );

        // Закрытие при клике вне окна
        colorDialog.addWindowFocusListener(new WindowAdapter() {
            public void windowLostFocus(WindowEvent e) {
                colorDialog.dispose();
            }
        });

        colorDialog.setVisible(true);
    }
    private void updateColorButtonAppearance() {
        colorButton.setBackground(new Color(worldColor));


        // Добавляем новые обработчики с текущим цветом
        colorButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                colorButton.setBackground(StyleUtil.changeColorShade(new Color(worldColor), 20));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                colorButton.setBackground(new Color(worldColor));
            }
        });
    }

    private void createWorld() {
        float width = widthSlider.getValue();
        float height = heightSlider.getValue();

        boolean roundStations = roundStationsCheck.isSelected();

            parent.switchScreen(MainFrame.GAME_SCREEN_NAME);
            GameWorldScreen gameScreen = (GameWorldScreen) parent.getCurrentScreen();
            gameScreen.createNewWorld((short) width, (short) height, worldColor);
            gameScreen.getWorld().setRoundStationsEnabled(roundStations);


    }

    @Override
    public void onActivate() {

        widthSlider.setValue(100);
        heightSlider.setValue(100);

        roundStationsCheck.setSelected(false);

        KeyboardController.getInstance().setCurrentWorldScreen(this);
        requestFocusInWindow();
    }
    public static int getWorldColorIndex() {
        return worldColorIndex;
    }

    public static void setWorldColorIndex(int index) {
        if (index >= 0 && index < WORLD_COLORS.length) {
            worldColorIndex = index;
        }
    }
    @Override
    public void onDeactivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(null);
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
}