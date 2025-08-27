package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.input.KeyboardController;
import metroline.objects.gameobjects.GameConstants;
import metroline.screens.GameScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.ColorUtil;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class WorldSettingsScreen extends GameScreen {
    public static final Color[] WORLD_COLORS = {
            new Color(203, 202, 202), // Светло-серый
            new Color(110, 110, 110), // Серый (по умолчанию)
            new Color(70, 70, 70),    // Темно-серый
            new Color(150, 100, 80),  // Коричневый
            new Color(80, 100, 150)   // Голубоватый
    };
    private MetrolineSlider moneySlider;
    private MetrolineLabel moneyValueLabel = new MetrolineLabel("M");;
    private MetrolineLabel moneyTextLabel;

    private MetrolineSlider widthSlider;
    private MetrolineSlider heightSlider;
    private MetrolineCheckbox landscapeCheck, abilityPayCheck, passengerCountCheck;
    private MetrolineCheckbox riversCheck;
    private MetrolineCheckbox roundStationsCheck;

    private MainFrame parent;
    private MetrolineButton colorButton; // Кнопка для выбора цвета
    private int worldColor = 0x6E6E6E; // Цвет по умолчанию

    // Новые слайдеры для экономических констант
    private MetrolineSlider stationBaseCostSlider ;
    private MetrolineSlider tunnelCostPerSegmentSlider;
    private MetrolineSlider stationBaseRevenueSlider;
    private MetrolineSlider baseStationUpkeepSlider;
    private MetrolineSlider baseTunnelUpkeepSlider;
    private MetrolineSlider gameplayUnitsCountSlider;

    // Метки для новых слайдеров
    private MetrolineLabel centerTitle = new MetrolineLabel("create_world_title", SwingConstants.CENTER);
    private MetrolineLabel widthLabel;
    private MetrolineLabel heightLabel;
    private MetrolineLabel stationBaseCostValueLabel = new MetrolineLabel("M");
    private MetrolineLabel tunnelCostPerSegmentValueLabel= new MetrolineLabel("M");
    private MetrolineLabel stationBaseRevenueValueLabel= new MetrolineLabel("M");
    private MetrolineLabel baseStationUpkeepValueLabel= new MetrolineLabel("M");
    private MetrolineLabel baseTunnelUpkeepValueLabel= new MetrolineLabel("M");
    private MetrolineLabel gameplayUnitsCountValueLabel= new MetrolineLabel("M");

    public WorldSettingsScreen(MainFrame parent) {
        super(parent);
        this.parent = parent;
        setBackground(StyleUtil.BACKGROUND_COLOR);
        setLayout(new BorderLayout(0, 0)); // Changed to BorderLayout with horizontal gap
        initUI();

        parent.updateLanguage();
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


      //  parent.translatables.add(title);

        return leftPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Title

        centerTitle.setFont(new Font("Arial", Font.BOLD, 24));
        centerTitle.setForeground(StyleUtil.FOREGROUND_COLOR);
        centerTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(centerTitle);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));


        widthSlider = new MetrolineSlider("width_world_slider_desc", 20, 200, 40,10);
        heightSlider = new MetrolineSlider("height_world_slider_desc",20, 200, 40, 10);
        widthSlider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        heightSlider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // World Size
        JPanel sizePanel = new JPanel(new GridLayout(2, 3, 10, 15));
        sizePanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        sizePanel.setMaximumSize(new Dimension(600, 80));

        widthLabel = new MetrolineLabel(widthSlider.getValue() + " M");
        widthLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        widthLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));


        heightLabel = new MetrolineLabel(heightSlider.getValue() + " M");
        heightLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        heightLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));

        addRow(centerPanel, "world.width", widthSlider, widthLabel);
        addRow(centerPanel, "world.height", heightSlider, heightLabel);


        centerPanel.add(sizePanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Money settings
        JPanel moneyPanel = new JPanel();
        moneyPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        moneyPanel.setLayout(new BoxLayout(moneyPanel, BoxLayout.X_AXIS));
        moneyPanel.setMaximumSize(new Dimension(600, 50));


        moneyValueLabel.setForeground(StyleUtil.FOREGROUND_COLOR);
        moneyValueLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));

        moneySlider = new MetrolineSlider("world.start_money_desc",50, 10000, 2000, 100);

        moneyTextLabel = new MetrolineLabel(moneySlider.getValue() + " M");
        moneyTextLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        addRow(centerPanel, "world.start_money", moneySlider, moneyTextLabel);
        centerPanel.add(moneyPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Features checkboxes
        JPanel featuresPanel = new JPanel();
        featuresPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
        featuresPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        featuresPanel.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));

        colorButton = MetrolineButton.createMetrolineColorableButton("world.color", e -> showWindowColorSelection(), new Color(worldColor));
        abilityPayCheck = StyleUtil.createMetrolineCheckBox("world.gen_abilityPay_zone", "world.gen_abilityPay_zone_desc");
        passengerCountCheck = StyleUtil.createMetrolineCheckBox("world.gen_passengerCount_zone","world.gen_passengerCount_zone_desc");
        landscapeCheck = StyleUtil.createMetrolineCheckBox("world.gen_landscape","world.gen_landscape_desc");
        riversCheck = StyleUtil.createMetrolineCheckBox("world.gen_river","world.gen_river_desc");
        roundStationsCheck = StyleUtil.createMetrolineCheckBox("world.round_stations","world.round_stations_desc");

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

        MetrolineButton backButton = new MetrolineButton("world.back", e -> parent.switchScreen("menu"));

        MetrolineButton createGameButton = new MetrolineButton("world.create_standart", e -> createWorld());

        buttonPanel.add(backButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(createGameButton);

        centerPanel.add(buttonPanel);

        parent.translatables.add(centerTitle);
        parent.translatables.add(widthSlider);
        parent.translatables.add(heightSlider);
        parent.translatables.add(heightLabel);
        parent.translatables.add(widthLabel);
        parent.translatables.add(moneySlider);
        parent.translatables.add(moneyTextLabel);
        parent.translatables.add(abilityPayCheck);
        parent.translatables.add(colorButton);
        parent.translatables.add(landscapeCheck);
        parent.translatables.add(riversCheck);
        parent.translatables.add(roundStationsCheck);
        parent.translatables.add(passengerCountCheck);
        parent.translatables.add(backButton);
        parent.translatables.add(createGameButton);
        return centerPanel;
    }

    private JPanel createEconomyPanel() {
        JPanel economyPanel = new JPanel();
        economyPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        economyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        economyPanel.setLayout(new BoxLayout(economyPanel, BoxLayout.Y_AXIS));

        // Title
        MetrolineLabel title = new MetrolineLabel("world.economy_settings", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(StyleUtil.FOREGROUND_COLOR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        economyPanel.add(title);
        economyPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Create economic sliders
        stationBaseCostSlider = new MetrolineSlider("world.station_cost_desc", 5f, 40, GameConstants.STATION_BASE_COST, 0.5f);
        tunnelCostPerSegmentSlider = new MetrolineSlider("world.tunnel_segment_cost_desc",0.1f, 10, GameConstants.TUNNEL_COST_PER_SEGMENT, 0.1f);
        stationBaseRevenueSlider = new MetrolineSlider("world.station_revenue_desc",0.1f, 5, GameConstants.STATION_BASE_REVENUE, 0.1f);
        baseStationUpkeepSlider = new MetrolineSlider("world.station_upkeep_desc",0.1f, 8, GameConstants.BASE_STATION_UPKEEP, 0.1f);
        baseTunnelUpkeepSlider = new MetrolineSlider("world.tunnel_upkeep_desc",0.1f, 2, GameConstants.BASE_TUNNEL_UPKEEP_PER_SEGMENT, 0.5f);
        gameplayUnitsCountSlider = new MetrolineSlider("world.gameplay_units_desc",5f, 100, GameConstants.GAMEPLAY_UNITS_COUNT, 1f);

        // Add economic sliders to panel
        addRow(economyPanel, "world.station_cost", stationBaseCostSlider, stationBaseCostValueLabel);
        addRow(economyPanel, "world.tunnel_segment_cost", tunnelCostPerSegmentSlider, tunnelCostPerSegmentValueLabel);
        addRow(economyPanel, "world.station_revenue", stationBaseRevenueSlider, stationBaseRevenueValueLabel);
        addRow(economyPanel, "world.station_upkeep", baseStationUpkeepSlider, baseStationUpkeepValueLabel);
        addRow(economyPanel, "world.tunnel_upkeep", baseTunnelUpkeepSlider, baseTunnelUpkeepValueLabel);
        addRow(economyPanel, "world.gameplay_units", gameplayUnitsCountSlider, gameplayUnitsCountValueLabel);


        parent.translatables.add(title);
        parent.translatables.add(stationBaseCostSlider);
        parent.translatables.add(tunnelCostPerSegmentSlider);
        parent.translatables.add(stationBaseRevenueSlider);
        parent.translatables.add(baseStationUpkeepSlider);
        parent.translatables.add(baseTunnelUpkeepSlider);
        parent.translatables.add(gameplayUnitsCountSlider);

        return economyPanel;
    }

    private void addRow(JPanel panel, String labelText, MetrolineSlider slider, MetrolineLabel valueLabel) {
        JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
        rowPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        rowPanel.setMaximumSize(new Dimension(400, 50));

        MetrolineLabel label = StyleUtil.createMetrolineLabel(labelText, SwingConstants.LEFT);
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
            gameScreen.createNewWorld((short) width, (short) height,hasPassengerCount,hasAbilityPay,  hasLandscape, hasRivers, worldColor, (int) startMoney);
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

        KeyboardController.getInstance().setCurrentWorldScreen(this);
        requestFocusInWindow();
    }

    @Override
    public void onDeactivate() {
        KeyboardController.getInstance().setCurrentWorldScreen(null);
    }


}