package metroline.util.ui;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class MetrolineFont {
    private static Font mainFont;
    private static Font boldFont;
    private static Font titleFont;

    static {
        try {
            // Загрузка шрифта из ресурсов
            InputStream fontStream = MetrolineFont.class.getResourceAsStream("/fonts/montserrat_light.ttf");
            if (fontStream != null) {
                mainFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                // Создаем производные шрифты
                boldFont = mainFont.deriveFont(Font.BOLD, 14f);
                titleFont = mainFont.deriveFont(Font.BOLD, 36f);
            } else {
                // Fallback на системные шрифты если встроенный не найден
                mainFont = new Font("Arial", Font.PLAIN, 14);
                boldFont = new Font("Arial", Font.BOLD, 14);
                titleFont = new Font("Arial", Font.BOLD, 36);
                System.err.println("Встроенный шрифт не найден, используются системные");
            }
        } catch (FontFormatException | IOException e) {
            System.err.println("Ошибка загрузки шрифта: " + e.getMessage());
            mainFont = new Font("Arial", Font.PLAIN, 14);
            boldFont = new Font("Arial", Font.BOLD, 14);
            titleFont = new Font("Arial", Font.BOLD, 36);
        }
    }

    public static Font getMainFont() {
        return mainFont;
    }

    public static Font getMainFont(float size) {
        return mainFont.deriveFont(size);
    }

    public static Font getMainFont(int style, float size) {
        return mainFont.deriveFont(style, size);
    }

    public static Font getBoldFont() {
        return boldFont;
    }

    public static Font getBoldFont(float size) {
        return boldFont.deriveFont(size);
    }

    public static Font getTitleFont() {
        return titleFont;
    }

    public static Font getTitleFont(float size) {
        return titleFont.deriveFont(size);
    }
}
