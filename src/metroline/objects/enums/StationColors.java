package metroline.objects.enums;

import metroline.util.LngUtil;

import java.awt.*;

public enum StationColors {
    RED(157, 6, 6, "color.red"),
    DARK_GREEN(0, 100, 0, "color.dark_green"),
    BLUE(0, 120, 190, "color.blue"),
    DARK_BLUE(27, 57, 208, "color.dark_blue"),
    CYAN(25, 149, 176, "color.cyan"),
    BROWN(110, 63, 21, "color.brown"),
    ORANGE(200, 100, 0, "color.orange"),
    PURPLE(133, 7, 133, "color.purple"),
    YELLOW(211, 179, 8, "color.yellow"),
    GRAY(153, 153, 153, "color.gray"),
    LIME(153, 204, 0, "color.lime"),
    TEAL(79, 155, 155, "color.teal"),
    PINK(201, 48, 128, "color.pink"),
    MINT(3, 121, 95, "color.mint"),
    MAROON(148, 21, 73, "color.maroon"),
    LIGHT_GREEN(109, 148, 104, "color.light_green");

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
