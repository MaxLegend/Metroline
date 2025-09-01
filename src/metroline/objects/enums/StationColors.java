package metroline.objects.enums;

import metroline.util.localizate.LngUtil;

import java.awt.*;

public enum StationColors {
    RED(150, 20, 20, "color.red"),          // Яркость: 120.0
    DARK_GREEN(0, 120, 0, "color.dark_green"), // Яркость: 120.0
    BLUE(40, 120, 180, "color.blue"),       // Яркость: 120.0
    DARK_BLUE(50, 80, 200, "color.dark_blue"), // Яркость: 120.0
    CYAN(40, 150, 160, "color.cyan"),       // Яркость: 120.0
    BROWN(120, 70, 30, "color.brown"),      // Яркость: 120.0
    ORANGE(200, 100, 0, "color.orange"),    // Яркость: 120.0
    PURPLE(130, 30, 130, "color.purple"),   // Яркость: 120.0
    YELLOW(200, 180, 0, "color.yellow"),    // Яркость: 120.0
    GRAY(120, 120, 120, "color.gray"),      // Яркость: 120.0
    LIME(140, 190, 0, "color.lime"),        // Яркость: 120.0
    TEAL(70, 150, 150, "color.teal"),       // Яркость: 120.0
    PINK(190, 60, 130, "color.pink"),       // Яркость: 120.0
    MINT(30, 130, 100, "color.mint"),       // Яркость: 120.0
    MAROON(140, 30, 80, "color.maroon"),    // Яркость: 120.0
    LIGHT_GREEN(110, 150, 100, "color.light_green"); // Яркость: 120.0

    private final Color color;
    private final String localizationKey;

    StationColors(int r, int g, int b, String localizationKey) {
        this.color = new Color(r, g, b);
        this.localizationKey = localizationKey;
    }

    // Возвращает цвет
    public Color getColor() {
        return color;
    }

    // Возвращает красную составляющую
    public int getRed() {
        return color.getRed();
    }

    // Возвращает зеленую составляющую
    public int getGreen() {
        return color.getGreen();
    }

    // Возвращает синюю составляющую
    public int getBlue() {
        return color.getBlue();
    }

    // Возвращает RGB в виде массива
    public int[] getRGB() {
        return new int[]{color.getRed(), color.getGreen(), color.getBlue()};
    }

    // Возвращает локализованное название цвета
    public String getLocalizedName() {
        return LngUtil.translatable(localizationKey);
    }

    // Возвращает hex-представление цвета (например, #9D0606)
    public String getHex() {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    // Возвращает все цвета в виде массива (для обратной совместимости)
    public static Color[] getAllColors() {
        StationColors[] values = values();
        Color[] colors = new Color[values.length];
        for (int i = 0; i < values.length; i++) {
            colors[i] = values[i].getColor();
        }
        return colors;
    }

    // Находит цвет по RGB компонентам
    public static StationColors fromRGB(int r, int g, int b) {
        for (StationColors stationColor : values()) {
            if (stationColor.getRed() == r &&
                    stationColor.getGreen() == g &&
                    stationColor.getBlue() == b) {
                return stationColor;
            }
        }
        return null;
    }

    // Находит цвет по Color объекту
    public static StationColors fromColor(Color color) {
        return fromRGB(color.getRed(), color.getGreen(), color.getBlue());
    }

}
