package metroline.util;

import java.awt.*;

public class ColorUtil {
    public static int colorToRGB(Color color) {
        if (color == null) {
            return 0; // или какое-то значение по умолчанию
        }
        return (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    // Альтернативный вариант с alpha каналом (0xAARRGGBB)
    public static int colorToARGB(Color color) {
        if (color == null) {
            return 0;
        }
        return (color.getAlpha() << 24) | (color.getRed() << 16) |
                (color.getGreen() << 8) | color.getBlue();
    }
    public static Color rgbToColor(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return new Color(red, green, blue);
    }

    // Для ARGB (0xAARRGGBB)
    public static Color argbToColor(int argb) {
        int alpha = (argb >> 24) & 0xFF;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        return new Color(red, green, blue, alpha);
    }

    /**
     * Получает RGB строку в формате #RRGGBB
     */
    public static String colorToHex(Color color) {
        if (color == null) {
            return "#000000";
        }
        return String.format("#%02x%02x%02x",
                color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Конвертирует HEX строку (#RRGGBB) в Color
     */
    public static Color hexToColor(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) {
            return Color.BLACK;
        }
        try {
            int rgb = Integer.parseInt(hex.substring(1), 16);
            return rgbToColor(rgb);
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }
}
