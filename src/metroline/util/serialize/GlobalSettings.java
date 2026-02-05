package metroline.util.serialize;

import metroline.util.MetroLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class GlobalSettings {
    private static final String SETTINGS_FILE = "config/metroline.properties";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_MUSIC_VOLUME = "music_volume";
    private static final String KEY_SFX_VOLUME = "sfx_volume";

    private static final float DEFAULT_VOLUME = 0.7f;
    private static final float DEFAULT_MUSIC_VOLUME = 1.0f;
    private static final float DEFAULT_SFX_VOLUME = 1.0f;

    private static final String KEY_LANGUAGE = "language";
    private static final String DEFAULT_LANGUAGE = "en";

    private static final String KEY_PNG_SCALE = "png_scale";
    private static final int DEFAULT_PNG_SCALE = 2;

    private static Properties properties;

    static {
        properties = new Properties();
        load();
    }
// ================ LANGUAGE ================

    public static String getLanguage() {
        return properties.getProperty(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    public static void setLanguage(String language) {
        properties.setProperty(KEY_LANGUAGE, language);
        save();
    }
    // ================ ГРОМКОСТЬ ================

    public static float getVolume() {
        return getFloatProperty(KEY_VOLUME, DEFAULT_VOLUME);
    }

    public static void setVolume(float volume) {
        setFloatProperty(KEY_VOLUME, volume);
        save();
    }

    // ================ МУЗЫКА ================

    public static float getMusicVolume() {
        return getFloatProperty(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME);
    }

    public static void setMusicVolume(float volume) {
        setFloatProperty(KEY_MUSIC_VOLUME, volume);
        save();
    }
// ================ PNG SCALE ================

    public static int getPngScale() {
        String value = properties.getProperty(KEY_PNG_SCALE);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                MetroLogger.logWarning("[GlobalSettings] Invalid PNG scale: " + value);
            }
        }
        return DEFAULT_PNG_SCALE;
    }

    public static void setPngScale(int scale) {
        properties.setProperty(KEY_PNG_SCALE, String.valueOf(scale));
        save();
    }
    // ================ SFX/UI ================

    public static float getSfxVolume() {
        return getFloatProperty(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME);
    }

    public static void setSfxVolume(float volume) {
        setFloatProperty(KEY_SFX_VOLUME, volume);
        save();
    }

    // ================ ВСПОМОГАТЕЛЬНЫЕ ================

    private static float getFloatProperty(String key, float defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                MetroLogger.logWarning("Invalid value for " + key + ": " + value + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    private static void setFloatProperty(String key, float value) {
        properties.setProperty(key, String.valueOf(value));
    }

    private static void load() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            new File("config").mkdirs();
            // Инициализируем значениями по умолчанию
            setFloatProperty(KEY_VOLUME, DEFAULT_VOLUME);
            setFloatProperty(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME);
            setFloatProperty(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME);
            properties.setProperty(KEY_LANGUAGE, DEFAULT_LANGUAGE);
            save(); // Создаём файл при первом запуске
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
            // Убедимся, что все ключи существуют
            if (!properties.containsKey(KEY_VOLUME)) setFloatProperty(KEY_VOLUME, DEFAULT_VOLUME);
            if (!properties.containsKey(KEY_MUSIC_VOLUME)) setFloatProperty(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME);
            if (!properties.containsKey(KEY_SFX_VOLUME)) setFloatProperty(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME);
            if (!properties.containsKey(KEY_PNG_SCALE)) properties.setProperty(KEY_PNG_SCALE, String.valueOf(DEFAULT_PNG_SCALE));
            if (!properties.containsKey(KEY_LANGUAGE)) properties.setProperty(KEY_LANGUAGE, DEFAULT_LANGUAGE);
        } catch (IOException e) {
            MetroLogger.logError("Cannot load global settings: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            File dir = new File("config");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(SETTINGS_FILE);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                properties.store(fos, "Metroline Global Settings");
            }
        } catch (IOException e) {
            MetroLogger.logError("Cannot save global settings: " + e.getMessage());
        }
    }
}
