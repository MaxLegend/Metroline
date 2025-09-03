package metroline;

import metroline.core.time.GameTime;
import metroline.core.world.GameWorld;
import metroline.core.world.SandboxWorld;
import metroline.screens.GameScreen;
import metroline.screens.GlobalSettingsScreen;
import metroline.screens.MenuScreen;
import metroline.screens.panel.InfoWindow;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.WorldMenuScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.screens.worldscreens.normal.GameWorldSettingsScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.sandbox.SandboxSettingsScreen;
import metroline.screens.worldscreens.sandbox.SandboxWorldScreen;
import metroline.util.IntegerDocumentFilter;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.MetrolineButton;
import metroline.util.ui.MetrolineLabel;
import metroline.util.ui.MetrolinePopupMenu;
import metroline.util.ui.MetrolineToggleButton;
import metroline.util.ui.tooltip.CursorTooltip;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;

import static metroline.MainFrame.*;

public class MainFrameUI {
    public static MainFrame PARENT;

    private GameScreen currentScreen;
    private String currentScreenName;
    private MetrolineLabel timeLabel = new MetrolineLabel("00:00 01.01.0000");
    private JToolBar toolBar;
    public LinesLegendWindow legendWindow;
    private MetrolineButton legendButton;
    private MetrolineButton economicLayerButton;
    public JLabel moneyLabel = new JLabel("100");
    private JPanel timePanel = new JPanel();
    private Timer timeUpdateTimer;
    private String lastDisplayedTime = "00:00 01.01.0000";
    private JPanel toolPanel;
    private boolean toolPanelVisible = false;

    // Кнопки инструментов для центральной панели
    private MetrolineToggleButton stationBtn;
    private MetrolineToggleButton tunnelBtn;
    private MetrolineToggleButton destroyBtn;
    private MetrolineToggleButton colorBtn;
    private MetrolineToggleButton cancelBtn;
    public void updateMoneyDisplay(float amount) {
        SwingUtilities.invokeLater(() -> {
            moneyLabel.setText(String.format("%.2f M", amount));
            timePanel.repaint();
        });
    }

    public MainFrameUI(MainFrame parent) {
        PARENT = parent;
        initUI();
        initTimeUpdater();
        PARENT.updateLanguage();
    }
    public void initUI() {
        initTimePanel();
        initLegendWindow();
        initToolBar();
        initToolPanel();
        CursorTooltip.init(PARENT);
    }


    private void initToolPanel() {
        toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
        toolPanel.setBackground(new Color(60, 60, 60));
        toolPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        toolPanel.setPreferredSize(new Dimension(60, 0));
        toolPanel.setVisible(false);
        // Заглушка для боковой панели
        MetrolineLabel placeholder = new MetrolineLabel("Tools");
        placeholder.setForeground(Color.WHITE);
        placeholder.setAlignmentX(Component.CENTER_ALIGNMENT);
        toolPanel.add(placeholder);
    }
    private void initTimePanel() {
        timePanel.removeAll();
        timePanel.setLayout(new BorderLayout());
        timePanel.setBackground(new Color(60, 60, 60));

        timePanel.add(createTimeControlPanel(), BorderLayout.WEST);
        timePanel.add(createToolsPanel(), BorderLayout.CENTER);
        timePanel.add(createRightPanel(), BorderLayout.EAST);

        PARENT.add(timePanel, BorderLayout.SOUTH);
    }
    private JPanel createToolsPanel() {
        JPanel toolsPanel = new JPanel();
        toolsPanel.setBackground(new Color(60, 60, 60));
        toolsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        // Создаем кнопки инструментов
        stationBtn = new MetrolineToggleButton("toolbar.build_station", "toolbar.build_station_desc");
        tunnelBtn = new MetrolineToggleButton("toolbar.build_tunnel", "toolbar.build_tunnel_desc");
        destroyBtn = new MetrolineToggleButton("toolbar.destroy", "toolbar.destroy_desc");
        colorBtn = new MetrolineToggleButton("toolbar.choose_color", "toolbar.choose_color_desc");
        cancelBtn = new MetrolineToggleButton("toolbar.cancel_selection", "toolbar.cancel_selection_desc");

        // Добавляем слушатели
        setupToolButtonsListeners();

        // Добавляем кнопки на панель
        toolsPanel.add(stationBtn);
        toolsPanel.add(tunnelBtn);
        toolsPanel.add(destroyBtn);
        toolsPanel.add(colorBtn);
        toolsPanel.add(cancelBtn);

        PARENT.translatables.add(stationBtn);
        PARENT.translatables.add(tunnelBtn);
        PARENT.translatables.add(destroyBtn);
        PARENT.translatables.add(colorBtn);
        PARENT.translatables.add(cancelBtn);

        return toolsPanel;
    }


    private void setupToolButtonsListeners() {
        stationBtn.addActionListener(e -> {
            if (stationBtn.isSelected()) {
                deselectOtherTools(stationBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isShiftPressed = true;
                    worldScreen.isCtrlPressed = false;
                    worldScreen.isAltPressed = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isShiftPressed = false;
                }
            }
        });

        tunnelBtn.addActionListener(e -> {
            if (tunnelBtn.isSelected()) {
                deselectOtherTools(tunnelBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isCtrlPressed = true;
                    worldScreen.isShiftPressed = false;
                    worldScreen.isAltPressed = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isCtrlPressed = false;
                }
            }
        });

        destroyBtn.addActionListener(e -> {
            if (destroyBtn.isSelected()) {
                deselectOtherTools(destroyBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isAltPressed = true;
                    worldScreen.isShiftPressed = true;
                    worldScreen.isCtrlPressed = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isAltPressed = false;
                    worldScreen.isShiftPressed = false;
                }
            }
        });

        colorBtn.addActionListener(e -> {
            if (colorBtn.isSelected()) {
                deselectOtherTools(colorBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isShiftPressed = true;
                    worldScreen.isCPressed = true;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isShiftPressed = false;
                    worldScreen.isCPressed = false;
                }
            }
        });

        cancelBtn.addActionListener(e -> {
            deselectAllTools();
            if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                worldScreen.isShiftPressed = false;
                worldScreen.isCtrlPressed = false;
                worldScreen.isAltPressed = false;
                worldScreen.isCPressed = false;

                if (PARENT.getCurrentScreen() instanceof GameWorldScreen gameScreen) {
                    gameScreen.worldClickController.deselectAll();
                }
            }
        });
    }


    private void deselectAllTools() {
        stationBtn.setSelected(false);
        tunnelBtn.setSelected(false);
        destroyBtn.setSelected(false);
        colorBtn.setSelected(false);
        cancelBtn.setSelected(false);
    }

    private void deselectOtherTools(MetrolineToggleButton selectedBtn) {
        if (stationBtn != selectedBtn) stationBtn.setSelected(false);
        if (tunnelBtn != selectedBtn) tunnelBtn.setSelected(false);
        if (destroyBtn != selectedBtn) destroyBtn.setSelected(false);
        if (colorBtn != selectedBtn) colorBtn.setSelected(false);
        if (cancelBtn != selectedBtn) cancelBtn.setSelected(false);
    }

    public void toggleToolbar() {
        toolPanelVisible = !toolPanelVisible;
        toolPanel.setVisible(toolPanelVisible);
        PARENT.revalidate();
        PARENT.repaint();
    }
    private JPanel createTimeControlPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(60, 60, 60));

        timeLabel = new MetrolineLabel(lastDisplayedTime);
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        panel.add(timeLabel);

        MetrolineButton pauseButton = MetrolineButton.createMetrolineInGameButton(
                "timebar.pause",
                e -> timeControl()
        );
        panel.add(pauseButton);

        panel.add(createTimeScalePanel(), BorderLayout.EAST);
        PARENT.translatables.add(pauseButton);
        return panel;
    }

    private JPanel createTimeScalePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(60, 60, 60));

        MetrolineLabel label = new MetrolineLabel("timebar.timespeed");
        label.setFont(new Font("Sans Serif", Font.BOLD, 13));
        label.setForeground(Color.WHITE);
        panel.add(label);

        JTextField timeScaleField = createTimeScaleField();
        panel.add(timeScaleField);

        PARENT.translatables.add(label);
        return panel;
    }

    private JTextField createTimeScaleField() {

        JTextField field = new JTextField("1", 3) {
            @Override
            protected void paintBorder(Graphics g) {}
        };

        field.setHorizontalAlignment(JTextField.CENTER);
        field.setForeground(Color.WHITE);
        field.setBackground(new Color(60, 60, 60));
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setFont(new Font("Sans Serif", Font.BOLD, 13));
        field.setEditable(false);
        field.setOpaque(false);

        ((PlainDocument)field.getDocument()).setDocumentFilter(new IntegerDocumentFilter());

        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    field.setEditable(true);
                    field.setOpaque(true);
                    field.setBackground(new Color(45, 45, 45));
                    field.requestFocus();
                    field.selectAll();
                }
            }
        });

        field.addActionListener(e -> applyTimeScale(field));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyTimeScale(field);
            }
        });

        return field;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(60, 60, 60));

        legendButton = MetrolineButton.createMetrolineInGameButton(
                "legend.button",
                e -> toggleLegendWindow()
        );
        panel.add(legendButton, BorderLayout.EAST);

        economicLayerButton =  new MetrolineButton(
                "timebar.economic_layers",
                e -> showEconomicLayerPopupMenu((MetrolineButton) e.getSource())
        );
        panel.add(economicLayerButton, BorderLayout.WEST);

        moneyLabel.setForeground(Color.WHITE);
        moneyLabel.setFont(new Font("Sans Serif", Font.BOLD, 14));
        panel.add(moneyLabel);

        PARENT.translatables.add(legendButton);
        PARENT.translatables.add(economicLayerButton);
        return panel;
    }

    private void initLegendWindow() {
        legendWindow = new LinesLegendWindow(PARENT);
        Rectangle bounds = PARENT.getBounds();
        legendWindow.setLocation(
                bounds.x + bounds.width - 350,
                bounds.y + bounds.height - 260
        );
    }

    private void initToolBar() {
        toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        toolBar.setVisible(false);
        //toolBar.setLayout(new BorderLayout());
        toolBar.setBackground(new Color(60, 60, 60));


        MetrolineButton optionsButton = MetrolineButton.createMetrolineInGameButton(
                "toolbar.game",
                e -> showOptionsPopupMenu((JButton) e.getSource())
        );
        toolBar.add(optionsButton);
        PARENT.translatables.add(optionsButton);
        PARENT.add(toolBar, BorderLayout.NORTH);
    }


    private void applyTimeScale(JTextField timeScaleField) {
        try {
            int scale = Integer.parseInt(timeScaleField.getText());
            if (scale < 1) scale = 1;
            timeScaleField.setText(String.valueOf(scale));
            setTimeScale(scale);
        } catch (NumberFormatException ex) {
            timeScaleField.setText("1");
            setTimeScale(1);
        }
        timeScaleField.setBackground(new Color(60, 60, 60));
        timeScaleField.setEditable(false);
    }
    private void setTimeScale(int scale) {
        if (currentScreen instanceof SandboxWorldScreen) {
            SandboxWorld world = (SandboxWorld) ((SandboxWorldScreen) currentScreen).getWorld();
            if (world != null && world.getGameTime() != null) {

                world.getGameTime().setTimeScale(scale*1000);
            }
        } else if (currentScreen instanceof GameWorldScreen) {
            GameWorld world = (GameWorld) ((GameWorldScreen) currentScreen).getWorld();
            if (world != null && world.getGameTime() != null) {
                world.getGameTime().setTimeScale(scale*1000);
            }
        }
    }
    private void toggleLegendWindow() {
        if (legendWindow == null) {
            legendWindow = new LinesLegendWindow(PARENT);
            if (currentScreen instanceof GameWorldScreen) {
                ((GameWorldScreen)currentScreen).setLegendWindow(legendWindow);
            }
            if (currentScreen instanceof SandboxWorldScreen) {
                ((SandboxWorldScreen)currentScreen).setLegendWindow(legendWindow);
            }
        }
        if (legendWindow.isVisible()) {
            legendWindow.hideWindow();
        } else {
            if (currentScreen instanceof GameWorldScreen) {
                GameWorldScreen screen = (GameWorldScreen) currentScreen;
                if (screen.getWorld() instanceof GameWorld) {
                    legendWindow.updateLines(screen.getWorld());
                }

            }
            if (currentScreen instanceof SandboxWorldScreen) {
                SandboxWorldScreen screen = (SandboxWorldScreen) currentScreen;
                if (screen.getWorld() instanceof SandboxWorld) {
                    legendWindow.updateLines(screen.getWorld());
                }

            }
            legendWindow.showWindow();
        }
    }
    /**
     * Back to Main menu
     */
    public void backToMenu() {

        PARENT.switchScreen(MAIN_MENU_SCREEN_NAME);
        InfoWindow.updateWindowsVisibility(PARENT);
    }
    /**
     * Exit from game
     */
    public void exitGame() {
        System.exit(0);
    }

    private void initTimeUpdater() {
        timeUpdateTimer = new Timer(33, e -> updateTimeDisplay());
        timeUpdateTimer.start();
    }

    private void updateTimeDisplay() {
        SwingUtilities.invokeLater(() -> {
            String newTime = null;
            if (currentScreen instanceof SandboxWorldScreen) {
                SandboxWorld world = (SandboxWorld) ((SandboxWorldScreen) currentScreen).getWorld();
                if (world != null && world.getGameTime() != null) newTime = world.getGameTime().getDateTimeString();
            } else if (currentScreen instanceof GameWorldScreen) {
                GameWorld world =  ((GameWorld)((GameWorldScreen)currentScreen).getWorld());
                if (world != null && world.getGameTime() != null) newTime = world.getGameTime().getDateTimeString();
                GameTime gameTime = world.getGameTime();

                if (gameTime != null && gameTime.checkHourPassed()) {
                    world.update();
                }
                //

            }

            if (newTime != null && !newTime.equals(lastDisplayedTime)) {
                timeLabel.setText(newTime);
                lastDisplayedTime = newTime;
                timePanel.repaint();
            }
        });
    }
    public void timeControl() {
        if (currentScreen instanceof SandboxWorldScreen) {
            SandboxWorld sandboxWorld = (SandboxWorld) ((SandboxWorldScreen) currentScreen).getWorld();
            if (sandboxWorld != null && sandboxWorld.getGameTime() != null) {
                sandboxWorld.getGameTime().togglePause();
            }
        }
        if (currentScreen instanceof GameWorldScreen) {
            GameWorld gameWorld = (GameWorld) ((GameWorldScreen) currentScreen).getWorld();
            if (gameWorld != null && gameWorld.getGameTime() != null) {
                gameWorld.getGameTime().togglePause();
            }
        }
    }


    /**
     * Save game
     */
    private void saveGame() {

        if (currentScreen instanceof SandboxWorldScreen) {

            ((SandboxWorldScreen)currentScreen).getWorld().saveWorld();

        }

        if (currentScreen instanceof GameWorldScreen) {

            ((GameWorldScreen)currentScreen).getWorld().saveWorld();

        }
        currentScreen.requestFocusInWindow();
    }

    /**
     * Load game
     */
    private void loadGame() {
        if (currentScreen instanceof SandboxWorldScreen) {
            ((SandboxWorldScreen)currentScreen).getWorld().loadWorld();
        }
        if (currentScreen instanceof GameWorldScreen) {
            ((GameWorldScreen)currentScreen).getWorld().loadWorld();
        }
    }
    public void toggleGameplayUnits() {
        GameWorld.showGameplayUnits = !GameWorld.showGameplayUnits;
        if (currentScreen instanceof GameWorldScreen) {
            ((GameWorldScreen)currentScreen).invalidateCache();
            currentScreen.repaint();
        }
    }
    public void togglePaymentZones() {
        GameWorld.showPaymentZones = !GameWorld.showPaymentZones;
        if (currentScreen instanceof GameWorldScreen) {
            ((GameWorldScreen)currentScreen).invalidateCache();
            ((GameWorldScreen)currentScreen).invalidateZonesCache();
            currentScreen.repaint();
        }
    }

    public void togglePassengerZones() {
        GameWorld.showPassengerZones = !GameWorld.showPassengerZones;
        if (currentScreen instanceof GameWorldScreen) {
            ((GameWorldScreen)currentScreen).invalidateCache();
            ((GameWorldScreen)currentScreen).invalidateZonesCache();
            currentScreen.repaint();
        }
    }
    private void showOptionsPopupMenu(JButton sourceButton) {
        MetrolinePopupMenu popupMenu = new MetrolinePopupMenu();

        // Добавляем пункты меню
        popupMenu.addMetrolineItem(LngUtil.translatable("toolbar.save_game"), this::saveGame);
        popupMenu.addMetrolineItem(LngUtil.translatable("toolbar.load_game"), this::loadGame);
        popupMenu.addMetrolineItem(LngUtil.translatable("button.backToMenu"), this::backToMenu);
        popupMenu.addMetrolineItem(LngUtil.translatable("toolbar.exit"), this::exitGame);

        popupMenu.showUnderComponent(sourceButton);
    }
    private void showEconomicLayerPopupMenu(MetrolineButton sourceButton) {
        MetrolinePopupMenu popupMenu = new MetrolinePopupMenu();
        popupMenu.addMetrolineItem(LngUtil.translatable("elayer.passenger"), this::togglePassengerZones);
        popupMenu.addMetrolineItem(LngUtil.translatable("elayer.abilityPay"), this::togglePaymentZones);
        popupMenu.addMetrolineItem(LngUtil.translatable("elayer.gameplayUnits"), this::toggleGameplayUnits);
        popupMenu.showAboveComponent(sourceButton);
    }
    public void switchScreen(String screenName) {
        // Удаляем текущий экран
        if (currentScreen != null) {
            PARENT.remove(currentScreen);
            toolBar.setVisible(false);
            currentScreen = null;
        }

        toolBar.setVisible(false);

        // Создаем новый экран
        currentScreen = createScreen(screenName);
        currentScreenName = screenName;

        PARENT.add(currentScreen, BorderLayout.CENTER);

        boolean isSandboxGameScreen = SANDBOX_SCREEN_NAME.equals(screenName);
        boolean isGameScreen = GAME_SCREEN_NAME.equals(screenName);
        toolBar.setVisible(isGameScreen || isSandboxGameScreen);
        timePanel.setVisible(isGameScreen || isSandboxGameScreen);

        if (legendButton != null) {
            legendButton.setVisible(isGameScreen || isSandboxGameScreen);
        }

        PARENT.revalidate();
        PARENT.repaint();
        currentScreen.requestFocusInWindow();
    }

    private GameScreen createScreen(String screenName) {
        switch (screenName) {
            case MAIN_MENU_SCREEN_NAME:
                return new MenuScreen(PARENT);
            case SANDBOX_SCREEN_NAME:
                return new SandboxWorldScreen(PARENT);
            case GAME_SCREEN_NAME:
                return new GameWorldScreen(PARENT);
            case GAME_WORLD_SETTINGS_SCREEN_NAME:
                return new GameWorldSettingsScreen(PARENT);
            case SANDBOX_SETTINGS_SCREEN_NAME:
                return new SandboxSettingsScreen(PARENT);
            case GLOBAL_SETTINGS_SCREEN_NAME:
                return new GlobalSettingsScreen(PARENT);
            case WORLD_MENU_SCREEN_NAME:
                return new WorldMenuScreen(PARENT);
            default:
                throw new IllegalArgumentException("Unknown screen: " + screenName);
        }
    }
    public GameScreen getCurrentScreen() {
        return currentScreen;
    }
}
