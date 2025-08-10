package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.screens.GameScreen;
import metroline.util.LngUtil;
import metroline.util.StyleUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class WorldSettingsScreen extends GameScreen {
    public static final Color[] WORLD_COLORS = {
            new Color(203, 202, 202), // Светло-серый
            new Color(110, 110, 110), // Серый (по умолчанию)
            new Color(70, 70, 70),    // Темно-серый
            new Color(150, 100, 80),  // Коричневый
            new Color(80, 100, 150)   // Голубоватый
    };
    private JSlider moneySlider;
    private JLabel moneyValueLabel;

    private JSlider widthSlider;
    private JSlider heightSlider;
    private JCheckBox organicPatchesCheck;
    private JCheckBox riversCheck;
    private JCheckBox roundStationsCheck;

    private MainFrame parent;
    private JButton colorButton; // Кнопка для выбора цвета
    private Color worldColor = new Color(110, 110, 110); // Цвет по умолчанию

    //just gebug only!
    public static final boolean innerDebugUI = false;

    public WorldSettingsScreen(MainFrame parent) {
        super(parent);
        this.parent = parent;
        setBackground(StyleUtil.BACKGROUND_COLOR);
        setLayout(new GridBagLayout());
        initUI();
    }

    private void initUI() {
        // Основной контейнер для центрирования
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        if(innerDebugUI) {
            centerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.RED, 2),
                    BorderFactory.createEmptyBorder(50, 50, 50, 50)
            ));
        } else {
            centerPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        }


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 30, 10, 30);
        gbc.weightx = 0;

        // Title
        JLabel title = new JLabel(LngUtil.translatable("create_world_title"), SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(StyleUtil.FOREGROUND_COLOR);
        gbc.insets = new Insets(0, 30, 30, 30);
        centerPanel.add(title, gbc);

        // Reset insets for other components
        gbc.insets = new Insets(10, 30, 10, 30);

        // World Size
        JPanel sizePanel = new JPanel(new GridLayout(2, 3, 10, 15));

        sizePanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        if(innerDebugUI) {
            sizePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLUE, 2),
                    BorderFactory.createEmptyBorder(0, 0, 20, 0)
            ));
        } else {
            sizePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        }

        // Метки для значений
        JLabel widthValueLabel = new JLabel("wwl");
        if(innerDebugUI) widthValueLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 1));
        widthValueLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        widthValueLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));

        JLabel heightValueLabel = new JLabel("hwl");
        if(innerDebugUI) heightValueLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 1));
        heightValueLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        heightValueLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));

        widthSlider = StyleUtil.createMetrolineSlider(20, 200, 40, " " + LngUtil.translatable("world.cells"), widthValueLabel);
        if(innerDebugUI)  widthSlider.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 1)); // Желтая граница

        heightSlider = StyleUtil.createMetrolineSlider(20, 200, 40, " " + LngUtil.translatable("world.cells"), heightValueLabel);
        if(innerDebugUI) heightSlider.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 1)); // Желтая граница

        JLabel widthLabel = StyleUtil.createMetrolineLabel(LngUtil.translatable("world.width"), SwingConstants.RIGHT);
        widthLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        if(innerDebugUI) widthLabel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));

        JLabel heightLabel = StyleUtil.createMetrolineLabel(LngUtil.translatable("world.height"), SwingConstants.RIGHT);
        heightLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        if(innerDebugUI) heightLabel.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));

        sizePanel.add(widthLabel);
        sizePanel.add(widthSlider);
        sizePanel.add(widthValueLabel);
        sizePanel.add(heightLabel);
        sizePanel.add(heightSlider);
        sizePanel.add(heightValueLabel);

        centerPanel.add(sizePanel, gbc);

        // World Features
        JPanel featuresPanel = new JPanel(new GridLayout(4, 1, 5, 0));
        featuresPanel.setSize(100, 100);
        featuresPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
      if(innerDebugUI) {
          featuresPanel.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createLineBorder(new Color(128, 0, 128), 2), // Фиолетовый
                  BorderFactory.createEmptyBorder(0, 100, 0, 100)
          ));
      }
        else {
          featuresPanel.setBorder(BorderFactory.createEmptyBorder(0, 100, 0, 100));
      }
        // Кнопка выбора цвета
        colorButton = StyleUtil.createMetrolineColorableButton(LngUtil.translatable("world.color"), e -> showWindowColorSelection(), worldColor);

        organicPatchesCheck = StyleUtil.createMetrolineCheckBox(LngUtil.translatable("world.gen_hard_rocks"), true);

        riversCheck = StyleUtil.createMetrolineCheckBox(LngUtil.translatable("world.gen_river"), true);
        roundStationsCheck = StyleUtil.createMetrolineCheckBox(LngUtil.translatable("world.round_stations"), false);

        JPanel moneyPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        moneyPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        if(innerDebugUI) {
            moneyPanel.setBorder(BorderFactory.createLineBorder(Color.PINK, 2));
        }
        moneyValueLabel = new JLabel("10000");
        moneyValueLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        moneyValueLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        moneySlider = StyleUtil.createMetrolineSlider(
                100,      // мин значение (100 тыс)
                10000,   // макс значение (100 млн)
                1000,    // начальное значение (10 млн)
                "",
                moneyValueLabel
        );
        JLabel moneyTextLabel = StyleUtil.createMetrolineLabel(
                LngUtil.translatable("world.start_money"),
                SwingConstants.RIGHT
        );
        moneyTextLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        moneyPanel.add(moneyTextLabel);
        moneyPanel.add(moneySlider);
        moneyPanel.add(moneyValueLabel);

        moneySlider.addChangeListener(e -> {
            int value = moneySlider.getValue();
            // Форматируем число с разделителями тысяч
            String formatted = String.format("%,d ₽", value);
            moneyValueLabel.setText(formatted);
        });
        featuresPanel.add(organicPatchesCheck);
        featuresPanel.add(organicPatchesCheck);
        featuresPanel.add(riversCheck);
        featuresPanel.add(roundStationsCheck);
        featuresPanel.add(colorButton);

        centerPanel.add(moneyPanel, gbc);
        centerPanel.add(featuresPanel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        if(innerDebugUI) {
            buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.ORANGE, 2),
                    BorderFactory.createEmptyBorder(20, 0, 0, 0)
            ));
        } else {
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        }


        JButton createSBButton = StyleUtil.createMetrolineButton(LngUtil.translatable("world.create_sandbox"),e -> createWorld(true));
        JButton createGameButton = StyleUtil.createMetrolineButton(LngUtil.translatable("world.create_standart"),e -> createWorld(false)); //createWorld(false)

        JButton backButton = StyleUtil.createMetrolineButton(LngUtil.translatable("world.back"),e -> parent.switchScreen("menu"));

        buttonPanel.add(backButton);
        buttonPanel.add(createSBButton);
        buttonPanel.add(createGameButton);

        centerPanel.add(buttonPanel, gbc);

        // Добавляем центральную панель в основной экран
        add(centerPanel);
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
                worldColor = color;
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
        colorButton.setBackground(worldColor);


        // Добавляем новые обработчики с текущим цветом
        colorButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                colorButton.setBackground(StyleUtil.changeColorShade(worldColor, 20));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                colorButton.setBackground(worldColor);
            }
        });
    }

    private void createWorld(boolean isSandbox) {
        int width = widthSlider.getValue();
        int height = heightSlider.getValue();
        boolean hasOrganicPatches = organicPatchesCheck.isSelected();
        boolean hasRivers = riversCheck.isSelected();
        boolean roundStations = roundStationsCheck.isSelected();
        int startMoney = moneySlider.getValue();

        if(isSandbox) {
            parent.switchScreen(MainFrame.SANDBOX_SCREEN_NAME);
            WorldSandboxScreen gameScreen = (WorldSandboxScreen) parent.getCurrentScreen();
            gameScreen.createNewWorld(width, height, hasOrganicPatches, hasRivers, worldColor);
            gameScreen.getWorld().setRoundStationsEnabled(roundStations);
        } else {
            parent.switchScreen(MainFrame.GAME_SCREEN_NAME);
            WorldGameScreen gameScreen = (WorldGameScreen) parent.getCurrentScreen();
            gameScreen.createNewWorld(width, height, hasOrganicPatches, hasRivers, worldColor, startMoney);
            gameScreen.getWorld().setRoundStationsEnabled(roundStations);
            ((GameWorld)gameScreen.getWorld()).setMoney(startMoney); // Устанавливаем начальные деньги
            gameScreen.updateMoneyDisplay(); // Обновляем отображение
        }
    }

    @Override
    public void onActivate() {
        widthSlider.setValue(100);
        heightSlider.setValue(100);
        organicPatchesCheck.setSelected(false);
        riversCheck.setSelected(false);
        roundStationsCheck.setSelected(false);
        moneySlider.setValue(10000);
        String formatted = String.format("%,d ₽", moneySlider.getValue());
        moneyValueLabel.setText(formatted);
    }


}