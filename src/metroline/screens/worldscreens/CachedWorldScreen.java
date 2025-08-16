package metroline.screens.worldscreens;

import metroline.MainFrame;
import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.screens.panel.LinesLegendWindow;
import metroline.util.debug.DebugInfoRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public abstract class CachedWorldScreen extends WorldScreen {
    protected BufferedImage worldCache;
    protected boolean cacheValid = false;
    protected Font debugFont = new Font("Monospaced", Font.PLAIN, 12);
    public LinesLegendWindow legendWindow;
    public CachedWorldScreen(MainFrame parent, World world) {
        super(parent, world);
    }
    public void setLegendWindow(LinesLegendWindow legendWindow) {
        this.legendWindow = legendWindow;
        if (getWorld() instanceof GameWorld) {
            ((GameWorld)getWorld()).setLegendWindow(legendWindow);
        }
    }
    protected void initWorldCache(int width, int height) {
        worldCache = new BufferedImage(width * 32, height * 32, BufferedImage.TYPE_INT_ARGB);
    }

    public void invalidateCache() {
        cacheValid = false;
    }

    protected void updateWorldCache(int width, int height, Consumer<Graphics2D> drawer) {
        if (worldCache == null || worldCache.getWidth() != width * 32 || worldCache.getHeight() != height * 32) {
            initWorldCache(width, height);
        }

        Graphics2D cacheGraphics = worldCache.createGraphics();
        try {
            cacheGraphics.setComposite(AlphaComposite.Clear);
            cacheGraphics.fillRect(0, 0, worldCache.getWidth(), worldCache.getHeight());
            cacheGraphics.setComposite(AlphaComposite.SrcOver);
            cacheGraphics.scale(2, 2);
            drawer.accept(cacheGraphics);
        } finally {
            cacheGraphics.dispose();
        }
        cacheValid = true;
    }

    protected void drawStaticWorld(Graphics2D g) {
        AffineTransform originalTransform = g.getTransform();
        for (int y = 0; y < getWorld().getHeight(); y++) {
            for (int x = 0; x < getWorld().getWidth(); x++) {
                getWorld().getWorldGrid()[x][y].draw(g, 0, 0, 1);
            }
        }
        g.setTransform(originalTransform);
    }

    protected void drawDebugInfo(Graphics2D g, Object selectedObject) {
        // Общая реализация отладочной информации
        g.setFont(debugFont);
        g.setColor(Color.YELLOW);

        int yPos = 60;
        g.drawString("=== GLOBAL DEBUG INFO ===", 10, yPos);
        yPos += 15;
        g.drawString("Stations: " + getWorld().getStations().size(), 10, yPos);
        yPos += 15;
        g.drawString("Tunnels: " + getWorld().getTunnels().size(), 10, yPos);
        yPos += 15;
        g.drawString("Labels: " + getWorld().getLabels().size(), 10, yPos);
        yPos += 15;
        g.drawString("Zoom: " + String.format("%.2f", zoom), 10, yPos);
        yPos += 15;
        g.drawString("Offset: (" + offsetX + "," + offsetY + ")", 10, yPos);
        yPos += 15;

        // Дополнительная информация о выбранном объекте
        if (selectedObject != null) {
            DebugInfoRenderer.renderDebugInfo(g, selectedObject, getWorld(), yPos);
        }
    }
}
