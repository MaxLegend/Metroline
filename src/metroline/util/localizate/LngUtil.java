package metroline.util.localizate;

import metroline.util.MetroLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Утилита для локализации текстов в игре
 */
public class LngUtil {
    private static final Map<String, Properties> translations = new HashMap<>();
    private static String currentLanguage = "en"; // По умолчанию английский

    // Статический блок инициализации
    static {
        loadTranslations("en"); // Загружаем английский
        loadTranslations("ru"); // Загружаем русский
        currentLanguage = metroline.util.serialize.GlobalSettings.getLanguage();
    }
    public static String getCurrentLanguage() {
        return currentLanguage;
    }
    /**
     * Загружает переводы для указанного языка
     */
    private static void loadTranslations(String language) {
        Properties props = new Properties();
        String resourcePath = "/lang/" + language + ".lng";

        try (InputStream input = LngUtil.class.getResourceAsStream(resourcePath);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {

            if (input == null) {
                MetroLogger.logInfo("Файл локализации не найден: " + resourcePath);
                return;
            }

            props.load(reader);
            translations.put(language, props);

        } catch (IOException ex) {
            MetroLogger.logInfo("Ошибка загрузки локализации для " + language + ": " + ex.getMessage());
        }
    }
    /**
     * Устанавливает текущий язык
     */
    public static void setLanguage(String language) {
        if (translations.containsKey(language)) {
            currentLanguage = language;
        } else {
            System.err.println("Unsupported language: " + language);
        }
    }

    /**
     * Получает перевод для указанного ключа
     */
    public static String translatable(String key) {
        Properties langProps = translations.get(currentLanguage);
        if (langProps != null && langProps.containsKey(key)) {
            return langProps.getProperty(key);
        }
        // Если перевод не найден, возвращаем ключ как есть
        return key;
    }

    /**
     * Получает перевод с подстановкой параметров
     */
    public static String translatable(String key, Object... args) {
        String pattern = translatable(key);
        return String.format(pattern, args);
    }
}
