package metroline;


import metroline.core.world.GameWorld;

import metroline.screens.GameScreen;
import metroline.screens.GlobalSettingsScreen;
import metroline.screens.MenuScreen;

import metroline.screens.panel.HotkeysInfoWindow;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.LoadGameScreen;
import metroline.screens.worldscreens.WorldMenuScreen;
import metroline.screens.worldscreens.WorldScreen;
import metroline.screens.worldscreens.normal.GameWorldSettingsScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;

import metroline.screens.panel.SaveNameDialog;
import metroline.util.ImageUtil;
import metroline.util.localizate.LngUtil;
import metroline.util.serialize.GlobalSettings;
import metroline.util.ui.*;
import metroline.util.ui.tooltip.CursorTooltip;

import javax.swing.*;
import java.awt.*;

import static metroline.MainFrame.*;

public class MainFrameUI {
    public static MainFrame PARENT;

    private GameScreen currentScreen;
    private String currentScreenName;

    private JToolBar toolBar;
    public LinesLegendWindow legendWindow;
    private MetrolineButton legendButton;

    private MetrolineToggleButton[] toolButtons;

    private JPanel bottomPanel = new JPanel();

    private JPanel toolPanel;
    private boolean toolPanelVisible = false;

    // Кнопки инструментов для центральной панели
    private MetrolineToggleButton stationBtn;
    private MetrolineToggleButton tunnelBtn;
    private MetrolineToggleButton destroyBtn;
    private MetrolineToggleButton colorBtn;
    private MetrolineToggleButton cancelBtn;
    private MetrolineToggleButton riverBtn;
    private MetrolineToggleButton riverBrushBtn;
    private MetrolineToggleButton riverLineBtn;
    public MainFrameUI(MainFrame parent) {
        PARENT = parent;
        initUI();

        PARENT.updateLanguage();
    }
    public void initUI() {
        initBottomPanel();
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
    private void initBottomPanel() {
        bottomPanel.removeAll();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBackground(new Color(60, 60, 60));

        bottomPanel.add(createToolsPanel(), BorderLayout.CENTER);
        bottomPanel.add(createRightPanel(), BorderLayout.EAST);

        PARENT.add(bottomPanel, BorderLayout.SOUTH);
    }
    private JPanel createToolsPanel() {
        JPanel toolsPanel = new JPanel();
        toolsPanel.setBackground(new Color(60, 60, 60));
        toolsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        // Создаем кнопки инструментов
        stationBtn = new MetrolineToggleButton("toolbar.build_station", "toolbar.build_station_desc");
        stationBtn.setFont(StyleUtil.getMetrolineFont(20));
        tunnelBtn = new MetrolineToggleButton("toolbar.build_tunnel", "toolbar.build_tunnel_desc");
        tunnelBtn.setFont(StyleUtil.getMetrolineFont(20));
        destroyBtn = new MetrolineToggleButton("toolbar.destroy", "toolbar.destroy_desc");
        destroyBtn.setFont(StyleUtil.getMetrolineFont(20));
        colorBtn = new MetrolineToggleButton("toolbar.choose_color", "toolbar.choose_color_desc");
        colorBtn.setFont(StyleUtil.getMetrolineFont(20));
        cancelBtn = new MetrolineToggleButton("toolbar.cancel_selection", "toolbar.cancel_selection_desc");
        cancelBtn.setFont(StyleUtil.getMetrolineFont(20));
        riverBtn = new MetrolineToggleButton("toolbar.river_tool", "toolbar.river_tool_desc");
        riverBtn.setFont(StyleUtil.getMetrolineFont(20));
        riverBrushBtn = new MetrolineToggleButton("toolbar.river_brush3x3", "toolbar.river_brush3x3_desc");
        riverBrushBtn.setFont(StyleUtil.getMetrolineFont(20));
        riverLineBtn = new MetrolineToggleButton("toolbar.river_lineTool", "toolbar.river_lineTool_desc");
        riverLineBtn.setFont(StyleUtil.getMetrolineFont(20));

        toolButtons = new MetrolineToggleButton[] {
                stationBtn, tunnelBtn, destroyBtn, colorBtn, cancelBtn, riverBtn, riverBrushBtn,riverLineBtn
        };

        // Добавляем слушатели
        setupToolButtonsListeners();

        // Добавляем кнопки на панель
        toolsPanel.add(stationBtn);
        toolsPanel.add(tunnelBtn);
        toolsPanel.add(destroyBtn);
        toolsPanel.add(colorBtn);
        toolsPanel.add(cancelBtn);
        toolsPanel.add(riverBtn);
        toolsPanel.add(riverBrushBtn);
        toolsPanel.add(riverLineBtn);

        PARENT.translatables.add(stationBtn);
        PARENT.translatables.add(tunnelBtn);
        PARENT.translatables.add(destroyBtn);
        PARENT.translatables.add(colorBtn);
        PARENT.translatables.add(cancelBtn);
        PARENT.translatables.add(riverBtn);
        PARENT.translatables.add(riverBrushBtn);
        PARENT.translatables.add(riverLineBtn);

        return toolsPanel;
    }


    private void setupToolButtonsListeners() {
        stationBtn.addActionListener(e -> {
            if (stationBtn.isSelected()) {
                deselectOtherTools(stationBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.stationButtonModeActive = true;
                    worldScreen.tunnelButtonModeActive = false;
                    worldScreen.destroyButtonModeActive = false;
                    worldScreen.colorButtonModeActive = false;
                    worldScreen.isRiverToolActive = false;
                    worldScreen.isRiverBrushToolActive = false;
                    worldScreen.isRiverLineToolActive = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.stationButtonModeActive = false;
                }
            }
        });

        tunnelBtn.addActionListener(e -> {
            if (tunnelBtn.isSelected()) {
                deselectOtherTools(tunnelBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.tunnelButtonModeActive = true;
                    worldScreen.stationButtonModeActive = false;
                    worldScreen.destroyButtonModeActive = false;
                    worldScreen.colorButtonModeActive = false;
                    worldScreen.isRiverToolActive = false;
                    worldScreen.isRiverBrushToolActive = false;
                    worldScreen.isRiverLineToolActive = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.tunnelButtonModeActive = false;
                }
            }
        });

        destroyBtn.addActionListener(e -> {
            if (destroyBtn.isSelected()) {
                deselectOtherTools(destroyBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.destroyButtonModeActive = true;
                    worldScreen.stationButtonModeActive = false;
                    worldScreen.tunnelButtonModeActive = false;
                    worldScreen.colorButtonModeActive = false;
                    worldScreen.isRiverToolActive = false;
                    worldScreen.isRiverBrushToolActive = false;
                    worldScreen.isRiverLineToolActive = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.destroyButtonModeActive = false;
                }
            }
        });

        colorBtn.addActionListener(e -> {
            if (colorBtn.isSelected()) {
                deselectOtherTools(colorBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.colorButtonModeActive = true;
                    worldScreen.stationButtonModeActive = false;
                    worldScreen.tunnelButtonModeActive = false;
                    worldScreen.destroyButtonModeActive = false;
                    worldScreen.isRiverToolActive = false;
                    worldScreen.isRiverBrushToolActive = false;
                    worldScreen.isRiverLineToolActive = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.colorButtonModeActive = false;
                }
            }
        });

        cancelBtn.addActionListener(e -> {
            if (cancelBtn.isSelected()) {
                deselectOtherTools(cancelBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.stationButtonModeActive = false;
                    worldScreen.tunnelButtonModeActive = false;
                    worldScreen.destroyButtonModeActive = false;
                    worldScreen.colorButtonModeActive = false;
                    worldScreen.isRiverToolActive = false;
                    worldScreen.isRiverBrushToolActive = false;
                    worldScreen.isRiverLineToolActive = false;
                }
            }
        });

        riverBtn.addActionListener(e -> {
            if (riverBtn.isSelected()) {
                deselectOtherTools(riverBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isRiverToolActive = true;
                    worldScreen.isRiverBrushToolActive = false;
                    worldScreen.stationButtonModeActive = false;
                    worldScreen.tunnelButtonModeActive = false;
                    worldScreen.destroyButtonModeActive = false;
                    worldScreen.colorButtonModeActive = false;
                    worldScreen.isRiverLineToolActive = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isRiverToolActive = false;
                }
            }
        });

        riverBrushBtn.addActionListener(e -> {
            if (riverBrushBtn.isSelected()) {
                deselectOtherTools(riverBrushBtn);
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isRiverBrushToolActive = true;
                    worldScreen.isRiverToolActive = false;
                    worldScreen.stationButtonModeActive = false;
                    worldScreen.tunnelButtonModeActive = false;
                    worldScreen.destroyButtonModeActive = false;
                    worldScreen.colorButtonModeActive = false;
                    worldScreen.isRiverLineToolActive = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isRiverBrushToolActive = false;
                }
            }
        });
        riverLineBtn.addActionListener(e -> {  // ✓ Правильная кнопка
            if (riverLineBtn.isSelected()) {   // ✓ Правильная проверка
                deselectOtherTools(riverLineBtn); // ✓ Правильная деселекция
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isRiverLineToolActive = true;  // ✓ Линейный инструмент
                    worldScreen.isRiverBrushToolActive = false;
                    worldScreen.isRiverToolActive = false;
                    worldScreen.stationButtonModeActive = false;
                    worldScreen.tunnelButtonModeActive = false;
                    worldScreen.destroyButtonModeActive = false;
                    worldScreen.colorButtonModeActive = false;
                }
            } else {
                if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
                    worldScreen.isRiverLineToolActive = false;
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
        riverBtn.setSelected(false);
        riverBrushBtn.setSelected(false);
        riverLineBtn.setSelected(false);
    }

    private void deselectOtherTools(MetrolineToggleButton selectedBtn) {
        if (stationBtn != selectedBtn) stationBtn.setSelected(false);
        if (tunnelBtn != selectedBtn) tunnelBtn.setSelected(false);
        if (destroyBtn != selectedBtn) destroyBtn.setSelected(false);
        if (colorBtn != selectedBtn) colorBtn.setSelected(false);
        if (cancelBtn != selectedBtn) cancelBtn.setSelected(false);
        if (riverBtn != selectedBtn) riverBtn.setSelected(false);
        if (riverBrushBtn != selectedBtn) riverBrushBtn.setSelected(false);
        if (riverLineBtn != selectedBtn) riverLineBtn.setSelected(false);
    }

    public void toggleToolbar() {
        toolPanelVisible = !toolPanelVisible;
        toolPanel.setVisible(toolPanelVisible);
        PARENT.revalidate();
        PARENT.repaint();
    }


    public void activateToolByIndex(int index) {
        if (index >= 0 && index < toolButtons.length) {
            MetrolineToggleButton button = toolButtons[index];
            if (!button.isSelected()) {
                button.setSelected(true);
                button.getActionListeners()[0].actionPerformed(null);
            }
        }
    }

    public void resetAllTools() {
        for (MetrolineToggleButton btn : toolButtons) {
            btn.setSelected(false);
        }

        if (PARENT.getCurrentScreen() instanceof WorldScreen worldScreen) {
            worldScreen.stationButtonModeActive = false;
            worldScreen.tunnelButtonModeActive = false;
            worldScreen.destroyButtonModeActive = false;
            worldScreen.colorButtonModeActive = false;
            worldScreen.isRiverLineToolActive = false;
            worldScreen.isRiverToolActive = false;
            worldScreen.isRiverBrushToolActive = false;

        }

        metroline.input.selection.SelectionManager.getInstance().deselect();
    }


    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(60, 60, 60));

        legendButton = MetrolineButton.createMetrolineInGameButton(
                "legend.button",
                e -> toggleLegendWindow()
        );
        panel.add(legendButton, BorderLayout.EAST);

        PARENT.translatables.add(legendButton);

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

        MetrolineButton hotkeysButton = MetrolineButton.createMetrolineInGameButton(
                "toolbar.hotkeys",
                e -> showHotkeysWindow()
        );
        toolBar.add(hotkeysButton);
        PARENT.translatables.add(hotkeysButton);
        PARENT.add(toolBar, BorderLayout.NORTH);
    }

    private void showHotkeysWindow() {
        PARENT.showHotkeysInfoWindow();
        currentScreen.requestFocusInWindow();
    }


    private void toggleLegendWindow() {
        if (legendWindow == null) {
            legendWindow = new LinesLegendWindow(PARENT);
            if (currentScreen instanceof GameWorldScreen gameScreen) {
                gameScreen.setLegendWindow(legendWindow);
            }
        }

        if (legendWindow.isVisible()) {
            legendWindow.hideWindow();
        } else {
            // Always update with current world data before showing
            if (currentScreen instanceof GameWorldScreen gameScreen) {
                if (gameScreen.getWorld() != null) {
                    legendWindow.updateLines(gameScreen.getWorld());
                }
            }
            legendWindow.showWindow();
        }
    }
    public void toggleToolByIndex(int index) {
        if (index >= 0 && index < toolButtons.length) {
            MetrolineToggleButton button = toolButtons[index];
            // Toggle selection state
            button.setSelected(!button.isSelected());
            // Fire action listener to apply the change
            if (button.getActionListeners().length > 0) {
                button.getActionListeners()[0].actionPerformed(null);
            }
        }
    }
    /**
     * Back to Main menu
     */
    public void backToMenu() {

        PARENT.switchScreen(MAIN_MENU_SCREEN_NAME);
        //   InfoWindow.updateWindowsVisibility(PARENT);
    }
    /**
     * Exit from game
     */
    public void exitGame() {
        System.exit(0);
    }





    /**
     * Save game
     */
    private void saveGame() {
        if (currentScreen instanceof GameWorldScreen gameScreen) {
            String currentName = gameScreen.getWorld().SAVE_FILE;
            if (currentName == null || currentName.isEmpty()) {
                currentName = "save";
            }
            // Remove .metro extension for display
            if (currentName.endsWith(".metro")) {
                currentName = currentName.substring(0, currentName.length() - 6);
            }

            String saveName = SaveNameDialog.showDialog(PARENT, currentName);
            if (saveName != null) {
                gameScreen.getWorld().SAVE_FILE = saveName;
                gameScreen.getWorld().saveWorld();
            }
        }
        currentScreen.requestFocusInWindow();
    }

    /**
     * Load game
     */
    private void loadGame() {
        PARENT.switchScreen(LOAD_GAME_SCREEN_NAME);
    }
    private void saveWorldAsPNG() {
        if (currentScreen instanceof WorldScreen worldScreen) {
            boolean isSandbox = worldScreen.getClass().getSimpleName().contains("Sandbox");
            ImageUtil.saveEntireWorldToPNG(isSandbox, GlobalSettings.getPngScale());
        }
        currentScreen.requestFocusInWindow();
    }
    private void showOptionsPopupMenu(JButton sourceButton) {
        MetrolinePopupMenu popupMenu = new MetrolinePopupMenu();

        // Добавляем пункты меню
        popupMenu.addMetrolineItem(LngUtil.translatable("toolbar.save_game"), this::saveGame);
        popupMenu.addMetrolineItem(LngUtil.translatable("toolbar.load_game"), this::loadGame);
        popupMenu.addMetrolineItem(LngUtil.translatable("toolbar.save_png"), this::saveWorldAsPNG);
        popupMenu.addMetrolineItem(LngUtil.translatable("button.backToMenu"), this::backToMenu);
        popupMenu.addMetrolineItem(LngUtil.translatable("toolbar.exit"), this::exitGame);

        popupMenu.showUnderComponent(sourceButton);
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


        boolean isGameScreen = GAME_SCREEN_NAME.equals(screenName);
        toolBar.setVisible(isGameScreen);
        bottomPanel.setVisible(isGameScreen );

        if (legendButton != null) {
            legendButton.setVisible(isGameScreen );
        }

        PARENT.revalidate();
        PARENT.repaint();
        currentScreen.requestFocusInWindow();
    }

    private GameScreen createScreen(String screenName) {
        switch (screenName) {
            case MAIN_MENU_SCREEN_NAME:
                return new MenuScreen(PARENT);

            case GAME_SCREEN_NAME:
                return new GameWorldScreen(PARENT);
            case GAME_WORLD_SETTINGS_SCREEN_NAME:
                return new GameWorldSettingsScreen(PARENT);

            case GLOBAL_SETTINGS_SCREEN_NAME:
                return new GlobalSettingsScreen(PARENT);
            case WORLD_MENU_SCREEN_NAME:
                return new WorldMenuScreen(PARENT);
            case LOAD_GAME_SCREEN_NAME:
                return new LoadGameScreen(PARENT);
            default:
                throw new IllegalArgumentException("Unknown screen: " + screenName);
        }
    }
    public GameScreen getCurrentScreen() {
        return currentScreen;
    }
}
