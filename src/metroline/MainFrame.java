package metroline;


import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

import metroline.core.time.GameTime;
import metroline.core.world.GameWorld;
import metroline.core.world.SandboxWorld;
import metroline.core.world.World;
import metroline.objects.gameobjects.GameplayUnits;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.screens.GameScreen;
import metroline.screens.MenuScreen;
import metroline.screens.panel.InfoWindow;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.WorldSettingsScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.screens.worldscreens.sandbox.SandboxSettingsScreen;
import metroline.screens.worldscreens.sandbox.SandboxWorldScreen;

import metroline.util.IntegerDocumentFilter;
import metroline.util.LngUtil;
import metroline.util.MetroLogger;
import metroline.util.ui.MetrolineButton;
import metroline.util.ui.MetrolineToggleButton;
import metroline.util.ui.StyleUtil;

/**
 * Main application frame that contains all game screens and toolbar
 * TODO: Реструктуризация, вынос всех отображений в отдельный класс - тут только вызовы
 * @author Tesmio
 */
public class MainFrame extends JFrame {
    public static MainFrame INSTANCE;
    public static final String SANDBOX_SCREEN_NAME = "sandbox_gamescreen";
    public static final String MAIN_MENU_SCREEN_NAME = "menu";
    public static final String GAME_SCREEN_NAME = "gamescreen";
    public static final String WORLD_SETTINGS_SCREEN_NAME = "world_settings";
    public static final String SANDBOX_SETTINGS_SCREEN_NAME = "sandbox_world_settings";

    private GameScreen currentScreen;


    private Map<String, GameScreen> screens = new HashMap<>();
    private boolean isFullscreen = false;


    private JLabel timeLabel = new JLabel("00:00 01.01.0000");
    private JToolBar toolBar;

    public LinesLegendWindow legendWindow;



    private JButton legendButton;
    private MetrolineToggleButton economicLayerButton;

    public JLabel moneyLabel = new JLabel("100");

    private JPanel timePanel = new JPanel();
    private Timer timeUpdateTimer;
    private String lastDisplayedTime = "00:00 01.01.0000";
    // Настройки стиля
    static {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            Color darkBg = new Color(45, 45, 45);
            UIManager.put("OptionPane.background", darkBg);
            UIManager.put("Panel.background", darkBg);
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * MainFrame constructor
     */
    public MainFrame() {
        super("Metroline");
        INSTANCE = this;

        if (screens.isEmpty()) {
            screens.put(MAIN_MENU_SCREEN_NAME, new MenuScreen(this));
            screens.put(SANDBOX_SCREEN_NAME, new SandboxWorldScreen(this));
            screens.put(GAME_SCREEN_NAME, new GameWorldScreen(this));
            screens.put(WORLD_SETTINGS_SCREEN_NAME, new WorldSettingsScreen(this));
            screens.put(SANDBOX_SETTINGS_SCREEN_NAME, new SandboxSettingsScreen(this));
        }
        initializeWindow(false);
        setupKeyBindings();
        initTimeUpdater();
    }
    public static MainFrame getInstance() {
        return INSTANCE;
    }
    private void initializeWindow(boolean preserveState) {
        try {

            GameScreen previousScreen = currentScreen;
            String activeScreenName = getActiveScreenName();
            MetroLogger.logInfo("Screen mode: " + (isFullscreen ? "Fullscreen" : "Windowed"));
            dispose();

            if (isFullscreen) {

                setupFullscreenWindow();
            } else {

                setupWindowedMode();
            }

            initUI();

            if (preserveState && previousScreen != null) {
                screens.put(activeScreenName, previousScreen);
                switchScreen(activeScreenName);

            } else {
                switchScreen(MAIN_MENU_SCREEN_NAME);
            }

            setVisible(true);
        } catch (Exception e) {
            MetroLogger.logError( "Failed changed screen mode!", e);
        }
    }


    String getActiveScreenName() {
        for (Map.Entry<String, GameScreen> entry : screens.entrySet()) {
            if (entry.getValue() == currentScreen) {
                return entry.getKey();
            }
        }
        return MAIN_MENU_SCREEN_NAME;
    }
    private void setupKeyBindings() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullscreen");
        actionMap.put("toggleFullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFullscreen();
            }
        });
    }
    public void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        initializeWindow(true);

    }
    private void setupFullscreenWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true); // Убираем стандартную рамку - на время отладки.
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(30, 30, 30));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        isFullscreen = true;
    }
    /**
     * Set to windowed mode
     */
    private void setupWindowedMode() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(false); // Стандартная рамка окна
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(30, 30, 30));
        setSize(1100, 600);
        setLocationRelativeTo(null); // Центрируем окно
        isFullscreen = false;
    }
//    private void setupFullscreenWindow() {
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setUndecorated(true);
//        setLayout(new BorderLayout());
//        getContentPane().setBackground(new Color(30, 30, 30));
//
//        // Настоящий полноэкранный режим
//        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
//        GraphicsDevice device = env.getDefaultScreenDevice();
//        device.setFullScreenWindow(this);
//
//        isFullscreen = true;
//    }
//
//    private void setupWindowedMode() {
//        // Выход из полноэкранного режима
//        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
//        GraphicsDevice device = env.getDefaultScreenDevice();
//        device.setFullScreenWindow(null);
//
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setUndecorated(false);
//        setLayout(new BorderLayout());
//        getContentPane().setBackground(new Color(30, 30, 30));
//        setSize(1100, 600);
//        setLocationRelativeTo(null);
//        isFullscreen = false;
//    }

    /**
     * UI Initialization
     */
    public void updateMoneyDisplay(float amount) {
        SwingUtilities.invokeLater(() -> {
            moneyLabel.setText(String.format("%.2f M", amount));
            timePanel.repaint();
        });
    }
    private JWindow currentPopup;
    private AWTEventListener outsideClickListener;


    private void initUI() {
        initTimePanel();
        initLegendWindow();
        initToolBar();
    }

    private void initTimePanel() {
        timePanel.removeAll();
        timePanel.setLayout(new BorderLayout());
        timePanel.setBackground(new Color(60, 60, 60));

        timePanel.add(createTimeControlPanel(), BorderLayout.WEST);
       // timePanel.add(createTimeScalePanel(), BorderLayout.CENTER);
        timePanel.add(createRightPanel(), BorderLayout.EAST);

        add(timePanel, BorderLayout.SOUTH);
    }

    private JPanel createTimeControlPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(60, 60, 60));

        timeLabel = new JLabel(lastDisplayedTime);
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Sans Serif", Font.BOLD, 13));
        panel.add(timeLabel);

        panel.add(MetrolineButton.createMetrolineInGameButton(
                LngUtil.translatable("timebar.pause"),
                e -> timeControl()
        ));
        panel.add(createTimeScalePanel(), BorderLayout.EAST);
        return panel;
    }

    private JPanel createTimeScalePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(60, 60, 60));

        JLabel label = new JLabel(LngUtil.translatable("timebar.timespeed"));
        label.setFont(new Font("Sans Serif", Font.BOLD, 13));
        label.setForeground(Color.WHITE);
        panel.add(label);

        JTextField timeScaleField = createTimeScaleField();
        panel.add(timeScaleField);

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
                LngUtil.translatable("legend.button"),
                e -> toggleLegendWindow()
        );
        panel.add(legendButton, BorderLayout.EAST);

            economicLayerButton = new MetrolineToggleButton(
                    LngUtil.translatable("timebar.economic_layers"),
                    e -> showEconomicLayerPopupMenu((JButton) e.getSource())
            );
            panel.add(economicLayerButton, BorderLayout.WEST);

        moneyLabel.setForeground(Color.WHITE);
        moneyLabel.setFont(new Font("Sans Serif", Font.BOLD, 14));
        panel.add(moneyLabel);

        return panel;
    }

    private void initLegendWindow() {
        legendWindow = new LinesLegendWindow(this);
        Rectangle bounds = getBounds();
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



        JButton optionsButton = createToolBarButton(
                "toolbar.game",
                e -> showOptionsPopupMenu((JButton)e.getSource())
        );
        toolBar.add(optionsButton);
//        toolBar.add(createToolBarButton("toolbar.save_game", e -> saveGame()));
//        toolBar.add(createToolBarButton("toolbar.load_game", e -> loadGame()));
//        toolBar.add(createToolBarButton("toolbar.back_menu", e -> backToMenu()));
//        toolBar.add(createToolBarButton("toolbar.exit", e -> exitGame()));

        add(toolBar, BorderLayout.NORTH);
    }

    private JButton createToolBarButton(String translationKey, ActionListener listener) {
        return MetrolineButton.createMetrolineInGameButton(
                LngUtil.translatable(translationKey),
                listener
        );
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

                world.getGameTime().setTimeScale(scale*100);
            }
        } else if (currentScreen instanceof GameWorldScreen) {
            GameWorld world = (GameWorld) ((GameWorldScreen) currentScreen).getWorld();
            if (world != null && world.getGameTime() != null) {
                world.getGameTime().setTimeScale(scale*100);
            }
        }
    }
    private void toggleLegendWindow() {
        if (legendWindow == null) {
            legendWindow = new LinesLegendWindow(this);
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
    public void togglePaymentZones() {
        GameWorld.showPaymentZones = !GameWorld.showPaymentZones;
        if (currentScreen instanceof GameWorldScreen) {
            ((GameWorldScreen)currentScreen).invalidateCache();
            currentScreen.repaint();
        }
    }

    public void togglePassengerZones() {
        GameWorld.showPassengerZones = !GameWorld.showPassengerZones;
        if (currentScreen instanceof GameWorldScreen) {
            ((GameWorldScreen)currentScreen).invalidateCache();
            currentScreen.repaint();
        }
    }
    private void showEconomicLayerPopupMenu(JButton sourceButton) {

        closePopupMenu();
        JPanel menuPanel = new JPanel(new GridLayout(0, 1));
        menuPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        JButton passengerButton = MetrolineButton.createMetrolineInGameButton(LngUtil.translatable("elayer.passenger" ), e -> togglePassengerZones());
        JButton abilityPayButton = MetrolineButton.createMetrolineInGameButton(LngUtil.translatable("elayer.abilityPay" ), e -> togglePaymentZones());

        passengerButton.setSize(sourceButton.getSize());
        abilityPayButton.setSize(sourceButton.getSize());
        menuPanel.add(passengerButton);
        menuPanel.add(abilityPayButton);

        // Создаем и настраиваем popup-окно
        currentPopup = new JWindow(this);
        currentPopup.getContentPane().add(menuPanel);
        currentPopup.pack();

        // Позиционируем под кнопкой

        Point location = sourceButton.getLocationOnScreen();
        currentPopup.setLocation(location.x, location.y -18 - sourceButton.getHeight());

        currentPopup.setVisible(true);

        // Добавляем глобальный слушатель кликов
        outsideClickListener = event -> {
            if (event.getID() == MouseEvent.MOUSE_PRESSED && currentPopup != null) {
                MouseEvent mouseEvent = (MouseEvent)event;

                if (!currentPopup.getBounds().contains(mouseEvent.getLocationOnScreen()) &&
                        !sourceButton.getBounds().contains(mouseEvent.getPoint())) {
                    closePopupMenu();
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
    }
    private void showOptionsPopupMenu(JButton sourceButton) {

        closePopupMenu();
        JPanel menuPanel = new JPanel(new GridLayout(0, 1));
        menuPanel.setBackground(StyleUtil.BACKGROUND_COLOR);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JButton save_game = createToolBarButton("toolbar.save_game", e -> saveGame());
        JButton load_game = createToolBarButton("toolbar.load_game", e -> loadGame());
        JButton back_menu = createToolBarButton("toolbar.back_menu", e -> backToMenu());
        JButton exit_game = createToolBarButton("toolbar.exit", e -> exitGame());

      //  JButton button = StyleUtil.createMetrolineInGameButton(LngUtil.translatable("options.1" ), e -> {});
        menuPanel.add(save_game);
        menuPanel.add(load_game);
        menuPanel.add(back_menu);
        menuPanel.add(exit_game);

        // Создаем и настраиваем popup-окно
        currentPopup = new JWindow(this);
        currentPopup.getContentPane().add(menuPanel);
        currentPopup.pack();

        // Позиционируем под кнопкой

        Point location = sourceButton.getLocationOnScreen();
        currentPopup.setLocation(location.x, location.y + sourceButton.getHeight());

        currentPopup.setVisible(true);

        // Добавляем глобальный слушатель кликов
        outsideClickListener = event -> {
            if ( event.getID() == MouseEvent.MOUSE_PRESSED && currentPopup != null) {
                MouseEvent mouseEvent = (MouseEvent)event;

                if ((!currentPopup.getBounds().contains(mouseEvent.getLocationOnScreen()) &&
                        !sourceButton.getBounds().contains(mouseEvent.getPoint()) ) || getCurrentScreen() instanceof MenuScreen) {
                    closePopupMenu();
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void closePopupMenu() {
        if (currentPopup != null) {
            currentPopup.dispose();
            currentPopup = null;
        }
        if (outsideClickListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
            outsideClickListener = null;
        }
    }
    /**
     * Time controller
     */
    /**
     * Управление временем (паузой)
     */
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
     * Back to Main menu
     */
    public void backToMenu() {

        switchScreen(MAIN_MENU_SCREEN_NAME);
        InfoWindow.updateWindowsVisibility(this);
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

                if (gameTime.checkHourPassed()) {
                    world.updateStationsRevenue();
                    world.updateStationsWear();
                    world.calculateStationsUpkeep();
                    world.calculateTunnelsUpkeep();
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

    /**
     * Screen switcher
     */
    public void switchScreen(String screenName) {
        if (currentScreen != null) {
            remove(currentScreen);
            toolBar.setVisible(false);
        }
//        if (!(screenName.equals(GAME_SCREEN_NAME) || screenName.equals(SANDBOX_SCREEN_NAME))) {
//            clearLegendWindow();
//        }
        InfoWindow.updateWindowsVisibility(this);
        //     LinesLegendWindow.updateWindowsVisibility(this);
        boolean isSandboxGameScreen = SANDBOX_SCREEN_NAME.equals(screenName);
        boolean isGameScreen = GAME_SCREEN_NAME.equals(screenName);
        currentScreen = screens.get(screenName);

        add(currentScreen, BorderLayout.CENTER);
        toolBar.setVisible(isGameScreen || isSandboxGameScreen);
        timePanel.setVisible(isGameScreen|| isSandboxGameScreen);
        legendButton.setVisible(isGameScreen || isSandboxGameScreen);

        revalidate();
        repaint();
        currentScreen.requestFocusInWindow();

    }
    public void showInfoPanel(Object selectedObject, int worldX, int worldY) {
        if (selectedObject == null ) {
            return;
        }
        if(currentScreen instanceof GameWorldScreen screen) {
            // Получаем экранные координаты
            Point screenPoint = screen.worldToScreen(worldX, worldY);
            Point windowPoint = new Point(screenPoint);
            SwingUtilities.convertPointToScreen(windowPoint, this);

            // Создаем новое окно для каждого объекта

            InfoWindow newWindow = new InfoWindow(this);

            if (selectedObject instanceof Station) {
                newWindow.displayStationInfo((Station) selectedObject, windowPoint);
            } else if (selectedObject instanceof Tunnel) {
                newWindow.displayTunnelInfo((Tunnel) selectedObject, windowPoint);
            }else if (selectedObject instanceof Label) {
                newWindow.displayLabelInfo((Label) selectedObject, windowPoint);
            }else if (selectedObject instanceof GameplayUnits) {
                newWindow.displayGameplayUnitsInfo((GameplayUnits) selectedObject, windowPoint);
            }

            screen.infoWindows.add(newWindow);
        }
    }

    /**
     * Get current GameScreen
     * @return Current screen (SUDDENLY!)
     */
    public GameScreen getCurrentScreen() { return currentScreen; }
}
