package metroline.core.world.tiles;


import metroline.core.world.GameWorld;

import java.awt.*;

/**
 * World tile that represents terrain with building permissions
 * @author Tesmio
 */
public class WorldTile extends Tile {
    private float perm; // 0 = can build, 1 = cannot build
    private static int baseTileColorRGB = 0x6E6E6E; // (110,110,110)
    private transient Color cachedBaseTileColor; // создаётся лениво, 1 раз
    private float grassValue;
    private boolean isWater; // Новая переменная для воды
    private float abilityPay; // Платежеспособность
    private float passengerCount; // Количество пассажиров
    private static final Color RIVER_COLOR = new Color(70, 130, 180, 255);
    private float waterDepth; // 0 (край реки) - 1 (центр)

    private static final int WATER_STEPS = 64;
    private static final Color[] WATER_PALETTE = new Color[WATER_STEPS];
    static {
        Color c1 = new Color(77, 158, 204);
        Color c2 = new Color(58, 124, 199);
        for (int i = 0; i < WATER_STEPS; i++) {
            float ratio = (float) i / (WATER_STEPS - 1);
            int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
            int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
            int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
            WATER_PALETTE[i] = new Color(r, g, b);
        }
    }
    public WorldTile() {
        super((short) 0, (short) 0, (byte) 16);
    }
    public WorldTile(short x, short y) {
        super(x, y, (byte) 16);
    }

    public WorldTile(short x, short y, float perm, boolean isWater, float abilityPay, int passengerCount, int rgbColor) {
        super(x, y, (byte) 16);
        this.perm = perm;
        this.isWater = isWater;
        this.abilityPay = abilityPay;
        this.passengerCount = passengerCount;
        this.baseTileColorRGB = rgbColor;
        //  this.baseTileColor = color;
    }
    public void setWaterDepth(float depth) {
        this.waterDepth = Math.min(1, Math.max(0, depth));
    }
//    public Color getAnimatedWaterColor() {
//        if (!isWater()) {
//            return getBaseTileColor(); // или что у тебя по умолчанию
//        }
//
//        long time = System.nanoTime() / 1_000_000; // миллисекунды
//        double noise = (
//                Math.sin((x * 13.17 + time * 0.003)) +
//                        Math.sin((y * 7.31  + time * 0.004)) +
//                        Math.sin((x * 5.13  + y * 9.24 + time * 0.002))
//        ) / 3.0;
//
//        float brightness = (float) (0.85 + 0.1 * noise); // лёгкая рябь
//
//        Color base = new Color(30, 144, 255); // dodgerblue
//        int r = (int) Math.min(255, base.getRed()   * brightness);
//        int g = (int) Math.min(255, base.getGreen() * brightness);
//        int b = (int) Math.min(255, base.getBlue()  * brightness);
//
//        return new Color(r, g, b);
//    }
    public static void setStaticBaseTileColor(int rgb) {
        baseTileColorRGB = rgb;
    }

    public WorldTile getWorldTile() {
        return new WorldTile(getX(), getY());
    }
    public Color getCurrentColor() {
        if(isWater) {
            return getAnimatedWaterColor();
        }
        Color baseColor = getBaseTileColor();
        int range = 50; // Должно совпадать с тем, что используется в draw()
        int red = Math.max(0, Math.min(255, baseColor.getRed() - (int)(perm * range)));
        int green = Math.max(0, Math.min(255, baseColor.getGreen() - (int)(perm * range)));
        int blue = Math.max(0, Math.min(255, baseColor.getBlue() - (int)(perm * range)));
        return new Color(red, green, blue);
    }
    public Color getAnimatedWaterColor() {
        if (!isWater()) {
            return getBaseTileColor();
        }
        return RIVER_COLOR;
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

    public void setBaseTileColor(int rgb) {
        this.baseTileColorRGB = rgb;
        this.cachedBaseTileColor = null; // сброс кеша
    }
    public static Color getStaticBaseTileColor() {
 return new Color(baseTileColorRGB);
    }
    public Color getBaseTileColor() {
        if (cachedBaseTileColor == null) {
            cachedBaseTileColor = new Color(baseTileColorRGB);
        }
        return cachedBaseTileColor;
    }
    public boolean isWater() { return isWater; }
    public void setWater(boolean water) { this.isWater = water; }

    public float getAbilityPay() { return abilityPay; }
    public void setAbilityPay(float abilityPay) { this.abilityPay = abilityPay; }

    public float getPassengerCount() { return passengerCount; }
    public void setPassengerCount(float passengerCount) { this.passengerCount = passengerCount; }
    public float getGrassValue() {
        return grassValue;
    }

    public void setGrassValue(float grassValue) {
        this.grassValue = Math.max(0, Math.min(1, grassValue));
    }
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
        //  g.setColor(getAnimatedWaterColor());
        g.setColor(RIVER_COLOR);
        g.fillRect(x, y, size, size);
    }

    private void drawLand(Graphics2D g, int x, int y, int size) {
        Color baseColor = getBaseTileColor();
        int range = 50;
        int red = Math.max(0, Math.min(255, baseColor.getRed() - (int)(perm * range)));
        int green = Math.max(0, Math.min(255, baseColor.getGreen() - (int)(perm * range)));
        int blue = Math.max(0, Math.min(255, baseColor.getBlue() - (int)(perm * range)));

        g.setColor(new Color(red, green, blue));
        g.fillRect(x, y, size, size);

    }

}
