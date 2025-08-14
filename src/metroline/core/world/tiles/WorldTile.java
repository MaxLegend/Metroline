package metroline.core.world.tiles;

import metroline.MainFrame;
import metroline.screens.worldscreens.WorldGameScreen;
import metroline.screens.worldscreens.WorldSandboxScreen;

import java.awt.*;
import java.util.Random;

/**
 * World tile that represents terrain with building permissions
 * @author Tesmio
 */
public class WorldTile extends Tile {
    private float perm; // 0 = can build, 1 = cannot build
    private Color baseTileColor = new Color(110, 110, 110);
    private boolean isWater; // Новая переменная для воды
    private float abilityPay; // Платежеспособность
    private int passengerCount; // Количество пассажиров

    private float waterDepth; // 0 (край реки) - 1 (центр)
    private static final Color[] WATER_PALETTE = {
            new Color(77, 158, 204), // Очень светлый голубой (мелководье)
            new Color(70, 170, 230),  // Светлый голубой
            new Color(50, 140, 210),  // Средний голубой
            new Color(48, 143, 222),  // Темный голубой
            new Color(58, 124, 199)    // Очень темный синий (глубоководье)
    };

    public WorldTile() {
        super(0, 0, 16);
    }
    public WorldTile(int x, int y) {
        super(x, y, 16);
    }

    public WorldTile(int x, int y, float perm, boolean isWater, float abilityPay, int passengerCount, Color color) {
        super(x, y, 16);
        this.perm = perm;
        this.isWater = isWater;
        this.abilityPay = abilityPay;
        this.passengerCount = passengerCount;
        this.baseTileColor = color;
    }
    public void setWaterDepth(float depth) {
        this.waterDepth = Math.min(1, Math.max(0, depth));
    }
    public Color getAnimatedWaterColor() {
        // Базовый индекс цвета в палитре на основе глубины
        float baseIndex = waterDepth * (WATER_PALETTE.length - 1);

        // Анимация - плавное колебание между соседними цветами

        float animatedIndex = baseIndex;

        // Нормализуем индекс
        animatedIndex = animatedIndex % (WATER_PALETTE.length - 1);
        if (animatedIndex < 0) animatedIndex += WATER_PALETTE.length - 1;

        // Интерполяция между двумя ближайшими цветами
        int index1 = (int)Math.floor(animatedIndex);
        int index2 = (int)Math.ceil(animatedIndex);
        float ratio = animatedIndex - index1;

        Color c1 = WATER_PALETTE[index1];
        Color c2 = WATER_PALETTE[index2];

        return interpolateColors(c1, c2, ratio);
    }

    // Вспомогательный метод для интерполяции цветов
    private Color interpolateColors(Color c1, Color c2, float ratio) {
        int r = (int)(c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int)(c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int)(c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(r, g, b);
    }
    public WorldTile getWorldTile() {
        return new WorldTile(getX(), getY());
    }
    public Color getCurrentColor() {
        if(isWater) {
            return getAnimatedWaterColor();
        }
        int range = 50; // Должно совпадать с тем, что используется в draw()
        int red = Math.max(0, Math.min(255, baseTileColor.getRed() - (int)(perm * range)));
        int green = Math.max(0, Math.min(255, baseTileColor.getGreen() - (int)(perm * range)));
        int blue = Math.max(0, Math.min(255, baseTileColor.getBlue() - (int)(perm * range)));
        return new Color(red, green, blue);
    }
    /**
     * Gets the building permission value
     * @return Permission value (0-1)
     */
    public float getPerm() { return perm; }

    /**
     * Sets the building permission value
     * @param perm New permission value (0-1)
     */
    public void setPerm(float perm) { this.perm = perm; }

    public void setBaseTileColor(Color color) {
        this.baseTileColor = color;
    }
    public Color getBaseTileColor() {
        return baseTileColor;
    }
    public boolean isWater() { return isWater; }
    public void setWater(boolean water) { this.isWater = water; }

    public float getAbilityPay() { return abilityPay; }
    public void setAbilityPay(float abilityPay) { this.abilityPay = abilityPay; }

    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }

//
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        int drawSize = (int)(size * zoom);
        int drawX = (int)((x * size + offsetX) * zoom);
        int drawY = (int)((y * size + offsetY) * zoom);
        Graphics2D g2d = (Graphics2D)g;

        if (isWater) {
                drawRealisticWater(g2d, drawX, drawY, drawSize);
        } else {
            // Обычная отрисовка земли
            drawLand(g2d, drawX, drawY, drawSize);
        }
    }


    private void drawRealisticWater(Graphics2D g, int x, int y, int size) {
        // Реалистичная отрисовка воды (ваш существующий код)
        g.setColor(getAnimatedWaterColor());
        g.fillRect(x, y, size, size);
    }

    private void drawLand(Graphics2D g, int x, int y, int size) {
        // Обычная отрисовка земли
        int range = 50;
        int red = Math.max(0, Math.min(255, baseTileColor.getRed() - (int)(perm * range)));
        int green = Math.max(0, Math.min(255, baseTileColor.getGreen() - (int)(perm * range)));
        int blue = Math.max(0, Math.min(255, baseTileColor.getBlue() - (int)(perm * range)));

        g.setColor(new Color(red, green, blue));
        g.fillRect(x, y, size, size);
                //        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                int layerColor = 180 + (int)(55 * abilityPay);
                // Отрисовка платежеспособности (красный)
                if (MainFrame.showPaymentZones && abilityPay > 0) {

                    g.setColor(new Color(layerColor, 0, 0, 180));
                    g.fillRect(x, y, size, size);
                }

                // Отрисовка пассажиропотока (зеленый)
                if (MainFrame.showPassengerZones && passengerCount > 0) {
                    g.setColor(new Color(0, layerColor, 0, 180));
                    g.fillRect(x, y, size, size);
                }
    }
//    @Override
//    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
//  //      super.draw(g, offsetX, offsetY, zoom);
//
//        int drawSize = (int)(size * zoom);
//        int drawX = (int)((x * size + offsetX) * zoom);
//        int drawY = (int)((y * size + offsetY) * zoom);
//        Graphics2D g2d = (Graphics2D)g;
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//      //  int range = 50;
//
////        int red = Math.max(0, Math.min(255, baseTileColor.getRed() - (int)(perm * range)));
////        int green = Math.max(0, Math.min(255, baseTileColor.getGreen() - (int)(perm * range)));
////        int blue = Math.max(0, Math.min(255, baseTileColor.getBlue() - (int)(perm * range)));
//        Color baseColor;
//        if (isWater) {
//            // Используем анимированный цвет воды, передаем текущее время
//            long time = System.currentTimeMillis();
//            baseColor = getAnimatedWaterColor(time);
//        } else {
//            int range = 50;
//            int red = Math.max(0, Math.min(255, baseTileColor.getRed() - (int)(perm * range)));
//            int green = Math.max(0, Math.min(255, baseTileColor.getGreen() - (int)(perm * range)));
//            int blue = Math.max(0, Math.min(255, baseTileColor.getBlue() - (int)(perm * range)));
//            baseColor = new Color(red, green, blue);
//        }
////        // Создаем основной цвет плитки
////        Color baseColor = isWater ?
////                new Color(64, 164, 223) : // Водный цвет
////                new Color(red, green, blue);
//
//        g.setColor(baseColor);
//        g2d.fillRect(drawX, drawY, drawSize, drawSize);
//
//                // Настройка прозрачности
//        //        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
//                int layerColor = 180 + (int)(55 * abilityPay);
//                // Отрисовка платежеспособности (красный)
//                if (MainFrame.showPaymentZones && abilityPay > 0) {
//
//                    g2d.setColor(new Color(layerColor, 0, 0, 180));
//                    g2d.fillRect(drawX, drawY, drawSize, drawSize);
//                }
//
//                // Отрисовка пассажиропотока (зеленый)
//                if (MainFrame.showPassengerZones && passengerCount > 0) {
//                    g2d.setColor(new Color(0, layerColor, 0, 180));
//                    g2d.fillRect(drawX, drawY, drawSize, drawSize);
//                }
//
//
//    }

}
