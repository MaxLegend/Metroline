package metroline;


import metroline.objects.gameobjects.GameplayUnits;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;
import metroline.screens.GameScreen;
import metroline.screens.panel.InfoWindow;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.CachedWorldScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.MetroLogger;
import metroline.util.ui.tooltip.CursorTooltip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
    private String currentScreenName;

    //  private Map<String, GameScreen> screens = new HashMap<>();

    private boolean isFullscreen = false;

    public LinesLegendWindow legendWindow;


    public MainFrameUI mainFrameUI;
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
        mainFrameUI = new MainFrameUI(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CursorTooltip.hideTooltip();
            }
        });
        initializeWindow(false);
    //    setupKeyBindings();




    }
    public static MainFrame getInstance() {
        return INSTANCE;
    }

    private void initializeWindow(boolean preserveState) {
        try {
        String previousScreenName = currentScreenName;
        dispose(); //он тут не причем, все равно изображение теряется

        setSize(1100, 600);
        setLocationRelativeTo(null);

        mainFrameUI.initUI();

        if (preserveState && previousScreenName != null) {
            switchScreen(previousScreenName);
        } else {
            switchScreen(MAIN_MENU_SCREEN_NAME);
        }

        setVisible(true);
    } catch (Exception e) {
        MetroLogger.logError("Failed changed screen mode!", e);
    }
}
    String getActiveScreenName() {
        return currentScreenName;
    }

    public void toggleFullscreen() {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getDefaultScreenDevice();

    if (!isFullscreen) {
        // Переход в полноэкранный режим
        dispose(); // Важно: dispose() перед изменением режима
        setUndecorated(true);

        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        if (currentScreen instanceof CachedWorldScreen) {
           ((CachedWorldScreen) currentScreen).invalidateCache();
       }
        isFullscreen = true;
    } else {

        dispose();
        gd.setFullScreenWindow(null);
        setUndecorated(false);
        setVisible(true);
        if (currentScreen instanceof CachedWorldScreen) {
            ((CachedWorldScreen) currentScreen).invalidateCache();
        }
        isFullscreen = false;
    }


    revalidate();
    repaint();

    if (currentScreen != null) {
        currentScreen.requestFocusInWindow();
    }
}



    public void switchScreen(String screenName) {
        mainFrameUI.switchScreen(screenName);
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
    public GameScreen getCurrentScreen() {
        return mainFrameUI.getCurrentScreen();
    }
}
