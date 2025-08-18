package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.objects.gameobjects.GameConstants;
import metroline.screens.GameScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.ui.MetrolineButton;
import metroline.util.ui.MetrolineSlider;
import metroline.util.LngUtil;
import metroline.util.ui.MetrolineCheckbox;
import metroline.util.ui.StyleUtil;

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
    private MetrolineSlider moneySlider;
    private JLabel moneyValueLabel;

    private MetrolineSlider widthSlider;
    private MetrolineSlider heightSlider;
    private MetrolineCheckbox landscapeCheck, abilityPayCheck, passengerCountCheck;
    private MetrolineCheckbox riversCheck;
    private MetrolineCheckbox roundStationsCheck;

    private MainFrame parent;
    private JButton colorButton; // Кнопка для выбора цвета
    private Color worldColor = new Color(110, 110, 110); // Цвет по умолчанию

    // Новые слайдеры для экономических констант
    private MetrolineSlider stationBaseCostSlider ;
    private MetrolineSlider tunnelCostPerSegmentSlider;
    private MetrolineSlider stationBaseRevenueSlider;
    private MetrolineSlider baseStationUpkeepSlider;
    private MetrolineSlider baseTunnelUpkeepSlider;
    private MetrolineSlider gameplayUnitsCountSlider;

    // Метки для новых слайдеров
    private JLabel stationBaseCostValueLabel = new JLabel("M");
    private JLabel tunnelCostPerSegmentValueLabel= new JLabel("M");
    private JLabel stationBaseRevenueValueLabel= new JLabel("M");
    private JLabel baseStationUpkeepValueLabel= new JLabel("M");
    private JLabel baseTunnelUpkeepValueLabel= new JLabel("M");
    private JLabel gameplayUnitsCountValueLabel= new JLabel("M");

    public WorldSettingsScreen(MainFrame parent) {
        super(parent);
        this.parent = parent;
        setBackground(StyleUtil.BACKGROUND_COLOR);
        setLayout(new BorderLayout(0, 0)); // Changed to BorderLayout with horizontal gap
        initUI();
    }


    private void initUI() {
        // Create main container panel with GridLayout (1 row, 3 columns)
        JPanel mainContainer = new JPanel(new GridLayout(1, 3));
        mainContainer.setBackground(StyleUtil.BACKGROUND_COLOR);
        mainContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Left panel
        JPanel leftPanel = createLeftPanel();

        // Center panel
        JPanel centerPanel = createCenterPanel();

        // Right panel (economy)
        JPanel rightPanel = createEconomyPanel();

        // Add all panels to main container
        mainContainer.add(leftPanel);
        mainContainer.add(centerPanel);
        mainContainer.add(rightPanel);

        // Add main container to screen
        add(mainContainer, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add placeholder content
        JLabel placeholder = new JLabel(LngUtil.translatable("world.left_panel_placeholder"), SwingConstants.CENTER);
        placeholder.setForeground(StyleUtil.FOREGROUND_COLOR);
        placeholder.setFont(new Font("Arial", Font.ITALIC, 16));
        leftPanel.add(placeholder, BorderLayout.CENTER);

        return leftPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Title
        JLabel title = new JLabel(LngUtil.translatable("create_world_title"), SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(StyleUtil.FOREGROUND_COLOR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(title);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));


        widthSlider = new MetrolineSlider(LngUtil.translatable("width_world_slider_desc"), 20, 200, 40,10);
        heightSlider = new MetrolineSlider(LngUtil.translatable("height_world_slider_desc"),20, 200, 40, 10);
        widthSlider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        heightSlider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // World Size
        JPanel sizePanel = new JPanel(new GridLayout(2, 3, 10, 15));
        sizePanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        sizePanel.setMaximumSize(new Dimension(600, 80));
//
//        JLabel widthValueLabel = new JLabel("wwl");
//        widthValueLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
//        widthValueLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
//
//        JLabel heightValueLabel = new JLabel("hwl");
//        heightValueLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
//        heightValueLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));


        JLabel widthLabel = StyleUtil.createMetrolineLabel(LngUtil.translatable("world.width"), SwingConstants.CENTER);
        widthLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        widthLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));

        JLabel heightLabel = StyleUtil.createMetrolineLabel(LngUtil.translatable("world.height"), SwingConstants.CENTER);
        widthLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        heightLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));

        addRow(centerPanel, LngUtil.translatable("world.width"), widthSlider, widthLabel);
        addRow(centerPanel, LngUtil.translatable("world.height"), heightSlider, heightLabel);
//        sizePanel.add(widthLabel);
//        sizePanel.add(widthSlider);
//        sizePanel.add(widthValueLabel);
//        sizePanel.add(heightLabel);
//        sizePanel.add(heightSlider);
//        sizePanel.add(heightValueLabel);

        centerPanel.add(sizePanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Money settings
        JPanel moneyPanel = new JPanel();
        moneyPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        moneyPanel.setLayout(new BoxLayout(moneyPanel, BoxLayout.X_AXIS));
        moneyPanel.setMaximumSize(new Dimension(600, 50));

        moneyValueLabel = new JLabel("10000");
        moneyValueLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        moneyValueLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));

        moneySlider = new MetrolineSlider(LngUtil.translatable("world.start_money_desc"),50, 10000, 2000, 100);
        JLabel moneyTextLabel = StyleUtil.createMetrolineLabel(LngUtil.translatable("world.start_money"), SwingConstants.RIGHT);
        moneyTextLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        addRow(centerPanel, LngUtil.translatable("world.start_money"), moneySlider, moneyTextLabel);
        centerPanel.add(moneyPanel);
//        moneyPanel.add(moneyTextLabel);
//        moneyPanel.add(Box.createRigidArea(new Dimension(10, 0)));
//        moneyPanel.add(moneySlider);
//        moneyPanel.add(Box.createRigidArea(new Dimension(10, 0)));
//        moneyPanel.add(moneyValueLabel);

//        moneySlider.addChangeListener(e -> {
//            float value = moneySlider.getValue();
//            String formatted = String.format("%,.0f M", value * 100);
//            moneyValueLabel.setText(formatted);
//        });

    //    centerPanel.add(moneyPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Features checkboxes
        JPanel featuresPanel = new JPanel();
        featuresPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
        featuresPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        featuresPanel.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));

        colorButton = StyleUtil.createMetrolineColorableButton(LngUtil.translatable("world.color"), e -> showWindowColorSelection(), worldColor);
        abilityPayCheck = StyleUtil.createMetrolineCheckBox(LngUtil.translatable("world.gen_abilityPay_zone"), LngUtil.translatable("world.gen_abilityPay_zone_desc"));
        passengerCountCheck = StyleUtil.createMetrolineCheckBox(LngUtil.translatable("world.gen_passengerCount_zone"),LngUtil.translatable("world.gen_passengerCount_zone_desc"));
        landscapeCheck = StyleUtil.createMetrolineCheckBox(LngUtil.translatable("world.gen_landscape"),LngUtil.translatable("world.gen_landscape_desc"));
        riversCheck = StyleUtil.createMetrolineCheckBox(LngUtil.translatable("world.gen_river"),LngUtil.translatable("world.gen_river_desc"));
        roundStationsCheck = StyleUtil.createMetrolineCheckBox(LngUtil.translatable("world.round_stations"),LngUtil.translatable("world.round_stations_desc"));

        featuresPanel.add(passengerCountCheck);
        featuresPanel.add(abilityPayCheck);
        featuresPanel.add(landscapeCheck);
        featuresPanel.add(riversCheck);
        featuresPanel.add(roundStationsCheck);
        featuresPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        featuresPanel.add(colorButton);

        centerPanel.add(featuresPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        MetrolineButton backButton = new MetrolineButton(LngUtil.translatable("world.back"), e -> parent.switchScreen("menu"));

        MetrolineButton createGameButton = new MetrolineButton(LngUtil.translatable("world.create_standart"), e -> createWorld());

        buttonPanel.add(backButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(createGameButton);

        centerPanel.add(buttonPanel);

        return centerPanel;
    }

    private JPanel createEconomyPanel() {
        JPanel economyPanel = new JPanel();
        economyPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        economyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        economyPanel.setLayout(new BoxLayout(economyPanel, BoxLayout.Y_AXIS));

        // Title
        JLabel title = new JLabel(LngUtil.translatable("world.economy_settings"), SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(StyleUtil.FOREGROUND_COLOR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        economyPanel.add(title);
        economyPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Create economic sliders
        stationBaseCostSlider = new MetrolineSlider(LngUtil.translatable("world.station_cost_desc"), 0.1f, 20, 2, 0.1f);
        tunnelCostPerSegmentSlider = new MetrolineSlider(LngUtil.translatable("world.tunnel_segment_cost_desc"),0.1f, 10, 2, 0.1f);
        stationBaseRevenueSlider = new MetrolineSlider(LngUtil.translatable("world.station_revenue_desc"),0.1f, 5, 2, 0.1f);
        baseStationUpkeepSlider = new MetrolineSlider(LngUtil.translatable("world.station_upkeep_desc"),0.1f, 8, 0.5f, 0.1f);
        baseTunnelUpkeepSlider = new MetrolineSlider(LngUtil.translatable("world.tunnel_upkeep_desc"),0.1f, 2, 0.2f, 0.1f);
        gameplayUnitsCountSlider = new MetrolineSlider(LngUtil.translatable("world.gameplay_units_desc"),5f, 100, 20, 1f);

        // Add economic sliders to panel
        addRow(economyPanel, LngUtil.translatable("world.station_cost"), stationBaseCostSlider, stationBaseCostValueLabel);
        addRow(economyPanel, LngUtil.translatable("world.tunnel_segment_cost"), tunnelCostPerSegmentSlider, tunnelCostPerSegmentValueLabel);
        addRow(economyPanel, LngUtil.translatable("world.station_revenue"), stationBaseRevenueSlider, stationBaseRevenueValueLabel);
        addRow(economyPanel, LngUtil.translatable("world.station_upkeep"), baseStationUpkeepSlider, baseStationUpkeepValueLabel);
        addRow(economyPanel, LngUtil.translatable("world.tunnel_upkeep"), baseTunnelUpkeepSlider, baseTunnelUpkeepValueLabel);
        addRow(economyPanel, LngUtil.translatable("world.gameplay_units"), gameplayUnitsCountSlider, gameplayUnitsCountValueLabel);

        return economyPanel;
    }

    private void addRow(JPanel panel, String labelText, MetrolineSlider slider, JLabel valueLabel) {
        JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
        rowPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        rowPanel.setMaximumSize(new Dimension(400, 50));

        JLabel label = StyleUtil.createMetrolineLabel(labelText, SwingConstants.LEFT);
        label.setFont(new Font("Sans Serif", Font.BOLD, 13));

        valueLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        valueLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // Устанавливаем связь между слайдером и меткой
        slider.setValueLabel(valueLabel, " M ");

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        sliderPanel.add(label, BorderLayout.WEST);
        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(valueLabel, BorderLayout.EAST);
        rowPanel.add(sliderPanel, BorderLayout.CENTER);

        panel.add(rowPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
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

    private void createWorld() {
        float width = widthSlider.getValue();
        float height = heightSlider.getValue();
        boolean hasPassengerCount = passengerCountCheck.isSelected();
        boolean hasAbilityPay = abilityPayCheck.isSelected();
        boolean hasLandscape = landscapeCheck.isSelected();
        boolean hasRivers = riversCheck.isSelected();
        boolean roundStations = roundStationsCheck.isSelected();
        float startMoney = moneySlider.getValue();
        float stationBaseCost = stationBaseCostSlider.getValue();
        float tunnelCostPerSegment = tunnelCostPerSegmentSlider.getValue();
        float stationBaseRevenue = stationBaseRevenueSlider.getValue();
        float baseStationUpkeep = baseStationUpkeepSlider.getValue();
        float baseTunnelUpkeep = baseTunnelUpkeepSlider.getValue();
        float gameplayUnitsCount = gameplayUnitsCountSlider.getValue();

        // Установка значений в GameConstants
        GameConstants.STATION_BASE_COST = stationBaseCost;
        GameConstants.TUNNEL_COST_PER_SEGMENT = tunnelCostPerSegment;
        GameConstants.STATION_BASE_REVENUE = stationBaseRevenue;
        GameConstants.BASE_STATION_UPKEEP = baseStationUpkeep;
        GameConstants.BASE_TUNNEL_UPKEEP_PER_SEGMENT = baseTunnelUpkeep;
        GameConstants.GAMEPLAY_UNITS_COUNT = gameplayUnitsCount;
            parent.switchScreen(MainFrame.GAME_SCREEN_NAME);
            GameWorldScreen gameScreen = (GameWorldScreen) parent.getCurrentScreen();
            gameScreen.createNewWorld((int) width, (int) height,hasPassengerCount,hasAbilityPay,  hasLandscape, hasRivers, worldColor, (int) startMoney);
            gameScreen.getWorld().setRoundStationsEnabled(roundStations);
            ((GameWorld)gameScreen.getWorld()).setMoney((int) startMoney); // Устанавливаем начальные деньги
            gameScreen.updateMoneyDisplay(); // Обновляем отображение

    }

    @Override
    public void onActivate() {
        stationBaseCostSlider.setValue(GameConstants.STATION_BASE_COST);
        tunnelCostPerSegmentSlider.setValue(GameConstants.TUNNEL_COST_PER_SEGMENT);
        stationBaseRevenueSlider.setValue(GameConstants.STATION_BASE_REVENUE);
        baseStationUpkeepSlider.setValue(GameConstants.BASE_STATION_UPKEEP);
        baseTunnelUpkeepSlider.setValue(GameConstants.BASE_TUNNEL_UPKEEP_PER_SEGMENT);
        gameplayUnitsCountSlider.setValue(GameConstants.GAMEPLAY_UNITS_COUNT);
        widthSlider.setValue(100);
        heightSlider.setValue(100);
        passengerCountCheck.setSelected(false);
        abilityPayCheck.setSelected(false);
        landscapeCheck.setSelected(false);
        riversCheck.setSelected(false);
        roundStationsCheck.setSelected(false);
        moneySlider.setValue(10000);
        String formatted = String.format("%,d ₽", moneySlider.getValue());
        moneyValueLabel.setText(formatted);
    }

    @Override
    public void onDeactivate() {

    }


}