package metroline.util.sound;

import metroline.core.soundengine.SoundEngine;
import metroline.util.MetroLogger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Предзагружает все звуки игры в память при старте.
 */
public class SoundPreloader {

    private static final Map<String, String> SOUND_MAP = new LinkedHashMap<>();

    static {

        SOUND_MAP.put("Metrotechno", "sounds/music/Metrotechno.mp3");
        SOUND_MAP.put("mouse", "sounds/gui/mouse.wav");
        SOUND_MAP.put("click", "sounds/gui/click.wav");
        SOUND_MAP.put("build", "sounds/gui/build.wav");
        SOUND_MAP.put("set", "sounds/gui/set.wav");
        SOUND_MAP.put("key_f1", "sounds/gui/k_f1.wav");
        SOUND_MAP.put("entered", "sounds/gui/entered.wav");
    }

    public static void preloadAllSounds(SoundEngine engine) {
        MetroLogger.logInfo("Starting sound preloading...");

        int loaded = 0;
        for (Map.Entry<String, String> entry : SOUND_MAP.entrySet()) {
            String key = entry.getKey();
            String path = entry.getValue();
            try {
                engine.loadSound(path, key);
                loaded++;
            } catch (Exception e) {
                MetroLogger.logError("Failed to load sound: " + key + " (" + path + ")", e);
            }
        }
        String[] uiSounds = {"buttonEntered", "click", "mouse", "error"};
        for (String key : uiSounds) {
            try {
                engine.preloadUIClip(key);
            } catch (Exception e) {
                MetroLogger.logError("Failed to preload UI sound: " + key, e);
            }
        }
        MetroLogger.logInfo("Sound preloading completed: " + loaded + "/" + SOUND_MAP.size() + " sounds loaded.");
    }
}
