package metroline.core.world.tiles;

import metroline.MainFrame;
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

    public WorldTile getWorldTile() {
        return new WorldTile(getX(), getY());
    }
    public Color getCurrentColor() {
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
    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
  //      super.draw(g, offsetX, offsetY, zoom);

        int drawSize = (int)(size * zoom);
        int drawX = (int)((x * size + offsetX) * zoom);
        int drawY = (int)((y * size + offsetY) * zoom);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int range = 50;

        int red = Math.max(0, Math.min(255, baseTileColor.getRed() - (int)(perm * range)));
        int green = Math.max(0, Math.min(255, baseTileColor.getGreen() - (int)(perm * range)));
        int blue = Math.max(0, Math.min(255, baseTileColor.getBlue() - (int)(perm * range)));

        // Создаем основной цвет плитки
        Color baseColor = isWater ?
                new Color(64, 164, 223) : // Водный цвет
                new Color(red, green, blue);

        g.setColor(baseColor);
        g2d.fillRect(drawX, drawY, drawSize, drawSize);

                // Настройка прозрачности
        //        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                int layerColor = 180 + (int)(55 * abilityPay);
                // Отрисовка платежеспособности (красный)
                if (MainFrame.showPaymentZones && abilityPay > 0) {

                    g2d.setColor(new Color(layerColor, 0, 0, 180));
                    g2d.fillRect(drawX, drawY, drawSize, drawSize);
                }

                // Отрисовка пассажиропотока (зеленый)
                if (MainFrame.showPassengerZones && passengerCount > 0) {
                    g2d.setColor(new Color(0, layerColor, 0, 180));
                    g2d.fillRect(drawX, drawY, drawSize, drawSize);
                }


    }

}
