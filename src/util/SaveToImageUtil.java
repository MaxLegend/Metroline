package util;

import game.core.world.World;
import game.objects.Label;
import game.objects.Station;
import game.objects.Tunnel;
import screens.WorldGameScreen;
import screens.WorldSandboxScreen;
import screens.WorldScreen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// TODO: В глобальные настройки - SCALE_FACTOR качество сохранения мира в PNG
public class SaveToImageUtil {
    private static final int SCALE_FACTOR = 2; // Увеличение в 2 раза
    private static final int TILE_SIZE = 32; // Размер клетки в пикселях

    public static void saveWorldToPNG(boolean isSandbox) {
        File screenshotsDir = new File("screenshots");
        if (!screenshotsDir.exists() && !screenshotsDir.mkdir()) {
            MessageUtil.showTimedMessage("Failed to create screenshots directory", true, 4000);
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "metro_map_" + timestamp + ".png";
        File file = new File(screenshotsDir, filename);

        try {
            WorldScreen screen = isSandbox ?
                    WorldSandboxScreen.getInstance() :
                    WorldGameScreen.getInstance();

            saveToPNG(file, screen);
            MessageUtil.showTimedMessage("Saved: " + file.getAbsolutePath(), false, 4000);
        } catch (IOException ex) {
            MessageUtil.showTimedMessage("Save Error: " + ex.getMessage(), true, 4000);
            MetroLogger.logError("Failed to save world to PNG", ex);
        }
    }

    /**
     * Сохраняет мир в PNG файл с увеличенным размером и улучшенным качеством
     */
    public static void saveToPNG(File file, WorldScreen screen) throws IOException {
        World world = screen.getWorld();
        int originalWidth = world.getWidth() * TILE_SIZE;
        int originalHeight = world.getHeight() * TILE_SIZE;

        // Создаем изображение в 2 раза больше с улучшенным качеством
        BufferedImage image = new BufferedImage(
                originalWidth * SCALE_FACTOR,
                originalHeight * SCALE_FACTOR,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = image.createGraphics();
        try {
            // Настройка качества рендеринга
            setupHighQualityRendering(g2d);

            // Очищаем фон прозрачным
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d.setComposite(AlphaComposite.SrcOver);

            // Масштабируем графику
            AffineTransform transform = AffineTransform.getScaleInstance(SCALE_FACTOR, SCALE_FACTOR);
            g2d.setTransform(transform);

            // Рисуем мир
            drawWorldContent(g2d, screen, world);
        } finally {
            g2d.dispose();
        }

        // Сохраняем с максимальным качеством
        ImageIO.write(image, "PNG", file);
    }

    private static void setupHighQualityRendering(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void drawWorldContent(Graphics2D g2d, WorldScreen screen, World world) {
        // Рисуем статичные элементы (сетку, фон)
        if (screen instanceof WorldSandboxScreen) {
            ((WorldSandboxScreen)screen).drawStaticWorld(g2d);
        } else if (screen instanceof WorldGameScreen) {
            ((WorldGameScreen)screen).drawStaticWorld(g2d);
        }

        // Рисуем туннели
        for (Tunnel tunnel : world.getTunnels()) {
            tunnel.draw(g2d, 0, 0, 1);
        }

        // Рисуем станции
        for (Station station : world.getStations()) {
            station.draw(g2d, 0, 0, 1);
        }

        // Рисуем метки с улучшенным качеством текста
        for (Label label : world.getLabels()) {
            Graphics2D labelG = (Graphics2D) g2d.create();
            try {
                labelG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                label.draw(labelG, 0, 0, 1);
            } finally {
                labelG.dispose();
            }
        }
    }
}