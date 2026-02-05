package metroline.util;

import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.objects.gameobjects.*;

import metroline.screens.GameScreen;
import metroline.screens.render.RiverRender;
import metroline.screens.render.StationRender;

import metroline.screens.worldscreens.WorldScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;

import metroline.util.ui.CachedBackgroundScreen;
import metroline.util.ui.ImageCacheUtil;
import metroline.util.ui.UserInterfaceUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 *
 * FIX Сохранение области экрана работает некорректно
 * Утилита для сохранения игрового мира в изображения
 * Поддерживает сохранение всего мира и видимой области с масштабированием
 */
public class ImageUtil {
    private static final int DEFAULT_SCALE_FACTOR = 2;
    private static final int TILE_SIZE = 32; // Размер клетки в пикселях
    public static Image backgroundImage;
    public static int bgWidth = -1;
    public static int bgHeight = -1;
    public static boolean imageLoaded = false;


    public static void loadBackgroundImage(GameScreen screen, String imageName) {
        String imagePath = "/backgrounds/" + imageName;
        Image backgroundImage = ImageCacheUtil.loadCachedImage(imagePath);

        // Сохраняем ссылку на изображение и его размеры в screen
        if (screen instanceof CachedBackgroundScreen) {
            ((CachedBackgroundScreen) screen).setBackgroundImage(backgroundImage);
            ((CachedBackgroundScreen) screen).setBackgroundLoaded(backgroundImage != null);

            if (backgroundImage != null) {
                Dimension size = ImageCacheUtil.getCachedImageSize(imagePath);
                ((CachedBackgroundScreen) screen).setBackgroundSize(size.width, size.height);
            }
        }
    }
    /**
     * Вариант 3: Умное масштабирование с сохранением пропорций
     * Изображение масштабируется чтобы полностью заполнить экран
     * с сохранением пропорций и обрезкой лишнего
     */
    public static void drawScaledProportional(Graphics g, Image backgroundImage,
            int bgWidth, int bgHeight,
            int screenWidth, int screenHeight) {
        if (backgroundImage == null || bgWidth <= 0 || bgHeight <= 0) return;

        // Рассчитываем масштаб для заполнения экрана с сохранением пропорций
        float scaleX = (float) screenWidth / bgWidth;
        float scaleY = (float) screenHeight / bgHeight;
        float scale = Math.max(scaleX, scaleY); // Масштаб для заполнения всего экрана

        // Рассчитываем новые размеры
        int scaledWidth = (int) (bgWidth * scale);
        int scaledHeight = (int) (bgHeight * scale);

        // Центрируем изображение
        int x = (screenWidth - scaledWidth) / 2;
        int y = (screenHeight - scaledHeight) / 2;

        // Рисуем масштабированное изображение
        g.drawImage(backgroundImage, x, y, scaledWidth, scaledHeight, null);
        drawVignetteEffect(g, screenWidth, screenHeight);
    }

    /**
     * Дополнительный эффект: затемнение краев (виньетка)
     */
    public static void drawVignetteEffect(Graphics g, int screenWidth, int screenHeight) {
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Создаем радиальный градиент для виньетки
            RadialGradientPaint vignette = new RadialGradientPaint(
                    screenWidth / 2f, screenHeight / 2f, Math.max(screenWidth, screenHeight) / 2f,
                    new float[] {0.0f, 0.7f, 1.0f},
                    new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(0, 0, 0, 100)}
            );

            g2d.setPaint(vignette);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
        } finally {
            g2d.dispose();
        }
    }
    /**
     * Сохраняет весь мир в PNG файл
     */
    public static void saveEntireWorldToPNG(boolean isSandbox, int scaleFactor) {
        File screenshotsDir = new File("screenshots");
        if (!screenshotsDir.exists() && !screenshotsDir.mkdirs()) {
            UserInterfaceUtil.showTimedMessage("Не удалось создать папку screenshots", true, 4000);
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "metro_world_" + timestamp + ".png";
        File file = new File(screenshotsDir, filename);

        try {
            WorldScreen screen =
                    GameWorldScreen.getInstance();

            saveEntireWorldToPNG(file, screen, scaleFactor);
            UserInterfaceUtil.showTimedMessage("Сохранено: " + file.getName(), false, 4000);
        } catch (IOException ex) {
            UserInterfaceUtil.showTimedMessage("Ошибка сохранения: " + ex.getMessage(), true, 4000);
            MetroLogger.logError("Failed to save world to PNG", ex);
        }
    }

    /**
     * Сохраняет видимую область экрана в PNG файл
     */
    public static void saveVisibleAreaToPNG(boolean isSandbox, int scaleFactor,
            Rectangle visibleArea, float zoom,
            float offsetX, float offsetY) {
        File screenshotsDir = new File("screenshots");
        if (!screenshotsDir.exists() && !screenshotsDir.mkdirs()) {
            UserInterfaceUtil.showTimedMessage("Не удалось создать папку screenshots", true, 4000);
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "metro_screenshot_" + timestamp + ".png";
        File file = new File(screenshotsDir, filename);

        try {
            WorldScreen screen =
                    GameWorldScreen.getInstance();

            saveVisibleAreaToPNG(file, screen, scaleFactor, visibleArea, zoom, offsetX, offsetY);
            UserInterfaceUtil.showTimedMessage("Скриншот сохранен: " + file.getName(), false, 4000);
        } catch (IOException ex) {
            UserInterfaceUtil.showTimedMessage("Ошибка скриншота: " + ex.getMessage(), true, 4000);
            MetroLogger.logError("Failed to save screenshot", ex);
        }
    }

    /**
     * Сохраняет весь мир в PNG с высоким качеством
     */
    private static void saveEntireWorldToPNG(File file, WorldScreen screen, int scaleFactor) throws IOException {
        World world = screen.getWorld();
        int width = world.getWidth() * TILE_SIZE * scaleFactor;
        int height = world.getHeight() * TILE_SIZE * scaleFactor;

        BufferedImage image = createHighQualityImage(width, height);
        Graphics2D g2d = image.createGraphics();

        try {
            setupHighQualityRendering(g2d);

            // Масштабируем и рисуем весь мир
            AffineTransform transform = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
            g2d.setTransform(transform);

            drawWorldContent(g2d, screen, world, 0, 0,
                    world.getWidth() * TILE_SIZE,
                    world.getHeight() * TILE_SIZE);
        } finally {
            g2d.dispose();
        }

        saveImageWithCompression(image, file);
    }

    /**
     * Сохраняет видимую область мира
     */
    private static void saveVisibleAreaToPNG(File file, WorldScreen screen, int scaleFactor,
            Rectangle visibleArea, float zoom,
            float offsetX, float offsetY) throws IOException {
        // Рассчитываем область мира, которая видна на экране
        int worldX = (int) ((-offsetX) / zoom);
        int worldY = (int) ((-offsetY) / zoom);
        int worldWidth = (int) (visibleArea.width / zoom);
        int worldHeight = (int) (visibleArea.height / zoom);

        // Создаем изображение с масштабированием
        int imageWidth = visibleArea.width * scaleFactor;
        int imageHeight = visibleArea.height * scaleFactor;

        BufferedImage image = createHighQualityImage(imageWidth, imageHeight);
        Graphics2D g2d = image.createGraphics();

        try {
            setupHighQualityRendering(g2d);

            // Комбинированное преобразование: масштабирование + смещение
            AffineTransform transform = new AffineTransform();
            transform.scale(scaleFactor * zoom, scaleFactor * zoom);
            transform.translate(offsetX / zoom, offsetY / zoom);
            g2d.setTransform(transform);

            // Рисуем только видимую область
            drawWorldContent(g2d, screen, screen.getWorld(),
                    worldX, worldY, worldWidth, worldHeight);
        } finally {
            g2d.dispose();
        }

        saveImageWithCompression(image, file);
    }

    /**
     * Создает изображение высокого качества
     */
    private static BufferedImage createHighQualityImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Настраивает высококачественный рендеринг
     */
    private static void setupHighQualityRendering(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /**
     * Рисует содержимое мира
     */
    private static void drawWorldContent(Graphics2D g2d, WorldScreen screen, World world,
            int x, int y, int width, int height) {
        // Очищаем область прозрачным цветом
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(x, y, width, height);
        g2d.setComposite(AlphaComposite.SrcOver);


        // Рисуем статичный мир (фон, сетку и т.д.)
        drawStaticWorld(g2d, screen, x, y, width, height);
        // Рисуем реки
        drawRivers(g2d, world, x, y, width, height);
        // Рисуем туннели
        drawTunnels(g2d, world, x, y, width, height);

        // Рисуем станции
        drawStations(g2d, screen, world, x, y, width, height);

        // Рисуем метки
        drawLabels(g2d, world, x, y, width, height);
    }
    /**
     * Рисует реки в указанной области
     */
    private static void drawRivers(Graphics2D g2d, World world,
            int x, int y, int width, int height) {
        for (River river : world.getRivers()) {
            // Используем 0,0 и zoom 1, так как трансформация уже применена к g2d
            RiverRender.drawRiver(river, g2d, 0, 0, 1f);
        }
    }
    /**
     * Рисует статичную часть мира
     */
    private static void drawStaticWorld(Graphics2D g2d, WorldScreen screen,
            int x, int y, int width, int height) {
        // Сохраняем текущий клип
        Shape oldClip = g2d.getClip();
        g2d.setClip(x, y, width, height);

        try {
           if (screen instanceof GameWorldScreen) {
                g2d.scale(2,2);
                ((GameWorldScreen) screen).drawStaticWorld(g2d);
                g2d.scale(0.5f,0.5f);
            }
        } finally {
            g2d.setClip(oldClip);
        }
    }

    /**
     * Рисует туннели в указанной области
     */
    private static void drawTunnels(Graphics2D g2d, World world,
            int x, int y, int width, int height) {
        Rectangle2D area = new Rectangle2D.Float(x, y, width, height);

        for (Tunnel tunnel : world.getTunnels()) {
            // Проверяем, попадает ли туннель в область отрисовки

                tunnel.draw(g2d, 0, 0, 1);

        }
    }

    /**
     * Рисует станции в указанной области
     */
    private static void drawStations(Graphics2D g2d, WorldScreen screen, World world,
            int x, int y, int width, int height) {
        Rectangle2D area = new Rectangle2D.Float(x, y, width, height);
        boolean roundStations = world.isRoundStationsEnabled();

        // Рисуем базовые элементы станций
        for (Station station : world.getStations()) {
                if (roundStations) {
                    StationRender.drawWorldColorRing(station, g2d, 0, 0, 1);
                } else {
                    StationRender.drawWorldColorSquare(station, g2d, 0, 0, 1);
                }
                StationRender.drawRoundTransfer(station, g2d, 0, 0, 1);

        }

        // Рисуем детали станций
        for (Station station : world.getStations()) {
                if (roundStations) {
                    StationRender.drawRoundStation(station, g2d, 0, 0, 1);
                } else {
                    StationRender.drawSquareStation(station, g2d, 0, 0, 1);
                }

        }
    }



    /**
     * Рисует метки в указанной области
     */
    private static void drawLabels(Graphics2D g2d, World world,
            int x, int y, int width, int height) {
        Rectangle2D area = new Rectangle2D.Float(x, y, width, height);

        for (StationLabel label : world.getLabels()) {

                // Для меток используем отдельный графический контекст
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

    /**
     * Сохраняет изображение с сжатием высокого качества
     */
    private static void saveImageWithCompression(BufferedImage image, File file) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (writers.hasNext()) {
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();

            // Включаем сжатие если поддерживается
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.9f); // Максимальное качество
            }

            try (FileImageOutputStream output = new FileImageOutputStream(file)) {
                writer.setOutput(output);
                writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
        } else {
            // Fallback на стандартный метод
            ImageIO.write(image, "PNG", file);
        }

        image.flush();
    }

    /**
     * Утилитный метод для получения видимой области из WorldScreen
     */
    public static Rectangle getVisibleArea(WorldScreen screen, float zoom, float offsetX, float offsetY) {
        // Просто возвращаем размеры экрана
        // Все трансформации (zoom, offset) уже применяются при отрисовке
        return new Rectangle(0, 0, screen.getWidth(), screen.getHeight());
    }
}