package metroline;


import metroline.core.soundengine.SoundEngine;
import metroline.core.soundengine.decoder.exceptions.JavaLayerException;
import metroline.input.KeyboardController;
import metroline.screens.GameScreen;


import metroline.screens.panel.HotkeysInfoWindow;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.MetroLogger;

import metroline.util.localizate.ITranslatable;
import metroline.util.localizate.LngUtil;
import metroline.util.serialize.GlobalSettings;
import metroline.util.sound.SoundPreloader;
import metroline.util.ui.ImageCacheUtil;

import metroline.util.ui.tooltip.CursorTooltip;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Main application frame that contains all game screens and toolbar
 *

 * @author Tesmio
 */
public class MainFrame extends JFrame {
    public static MainFrame INSTANCE;

    public static final String MAIN_MENU_SCREEN_NAME = "menu";
    public static final String GAME_SCREEN_NAME = "gamescreen";
    public static final String WORLD_MENU_SCREEN_NAME = "world_menu";
    public static final String GAME_WORLD_SETTINGS_SCREEN_NAME = "game_world_settings";

    public static final String GLOBAL_SETTINGS_SCREEN_NAME = "global_settings";
    public static final String LOAD_GAME_SCREEN_NAME = "load_game";

    public static SoundEngine SOUND_ENGINE;

    static {
        try {
            SOUND_ENGINE = SoundEngine.getInstance();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean repaintBlocked = false;
    private GameScreen currentScreen;
    private String currentScreenName;


    private boolean isFullscreen = false;

    public LinesLegendWindow legendWindow;
    public HotkeysInfoWindow hotkeysInfoWindow;

    public final ArrayList<ITranslatable> translatables = new ArrayList<>();
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
    public void preloadBackgrounds() {
        // Предзагружаем все фоновые изображения при запуске
        String[] backgrounds = {
                "/backgrounds/background.png",
                "/backgrounds/background2.png",
                // добавьте другие фоны которые используются
        };

        for (String bg : backgrounds) {
            ImageCacheUtil.loadCachedImage(bg);
        }
    }

    /**
     * MainFrame constructor
     */
    public MainFrame() throws LineUnavailableException, IOException, JavaLayerException, InterruptedException {
        super("Metroline");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        INSTANCE = this;

        try {
            SOUND_ENGINE = SoundEngine.getInstance();
        } catch (LineUnavailableException e) {
            // Логируем ошибку, но не падаем — игра должна работать и без звука!
            MetroLogger.logError("Cannot initialize sound engine");
            e.printStackTrace();
            SOUND_ENGINE = null; // или создай заглушку
        }



        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        KeyboardController.initialize(this);
        mainFrameUI = new MainFrameUI(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CursorTooltip.hideTooltip();
            }
        });
        preloadBackgrounds();

        initializeWindow(false);


        updateTranslations();


        if (SOUND_ENGINE != null) {
            // Устанавливаем громкость

            SOUND_ENGINE.setGlobalVolume(GlobalSettings.getVolume());
            SOUND_ENGINE.setMusicVolume(GlobalSettings.getMusicVolume());
            SOUND_ENGINE.setSfxVolume(GlobalSettings.getSfxVolume());
            SoundPreloader.preloadAllSounds(SOUND_ENGINE);

                // Добавляем треки в очередь
                SOUND_ENGINE.enqueueMusic("Metrotechno");

                SOUND_ENGINE.playMusicQueue(true);

        }

    }



    public static MainFrame getInstance() {
        return INSTANCE;
    }

    public ArrayList getTranslatables() {
        return translatables;
    }

    private void initializeWindow(boolean preserveState) {
        try {
        String previousScreenName = currentScreenName;


        setSize(1920, 1080);
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

        if (!isFullscreen) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            dispose();
            setUndecorated(true);
            setVisible(true);
            isFullscreen = true;
        } else {
            // Возврат в оконный режим


            dispose();

            setExtendedState(JFrame.NORMAL);
            setSize(1100, 600);
            setLocationRelativeTo(null);
            setUndecorated(false);
            setVisible(true);
            isFullscreen = false;
        }




}

    public void showHotkeysInfoWindow() {
        if (hotkeysInfoWindow == null || !hotkeysInfoWindow.isVisible()) {
            // Создаём только если ещё не создано или было закрыто
            hotkeysInfoWindow = new HotkeysInfoWindow(this);
        }
        hotkeysInfoWindow.showWindow(); // ← нужно добавить этот метод (см. ниже)
    }

    public void updateTranslations() {
        for (ITranslatable tc : translatables) {
            tc.updateTranslation();
        }
    }
    public void updateLanguage() {
        LngUtil.setLanguage(LngUtil.getCurrentLanguage());
        updateTranslations();
    }
    public void changeLanguage() {
        String newLang = LngUtil.getCurrentLanguage().equals("ru") ? "en" : "ru";
        LngUtil.setLanguage(newLang);
        GlobalSettings.setLanguage(newLang); // Save to config
        updateTranslations();
    }
    public void switchScreen(String screenName) {

        mainFrameUI.switchScreen(screenName);
        if (MAIN_MENU_SCREEN_NAME.equals(screenName)) {

        }
    }


    public void setCurrentScreen(GameScreen screen) {
        this.currentScreen = screen;
    }

    /**
     * Get current GameScreen
     * @return Current screen (SUDDENLY!)
     */
    public GameScreen getCurrentScreen() {

        return mainFrameUI.getCurrentScreen();
    }
}
