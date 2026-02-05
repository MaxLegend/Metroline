package metroline.util.ui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Утилита для кэширования изображений
 */
public class ImageCacheUtil {
    private static final Map<String, Image> imageCache = new HashMap<>();
    private static final Map<String, Dimension> imageDimensions = new HashMap<>();

    /**
     * Загружает изображение из кэша или из ресурсов
     */
    public static Image loadCachedImage(String imagePath) {
        return imageCache.computeIfAbsent(imagePath, path -> {
            try {
                URL imageUrl = ImageCacheUtil.class.getResource(path);
                if (imageUrl != null) {
                    BufferedImage img = ImageIO.read(imageUrl);
                    // Сохраняем размеры изображения
                    imageDimensions.put(path, new Dimension(img.getWidth(), img.getHeight()));
                    return img;
                }
            } catch (IOException e) {
                System.err.println("Error loading image: " + path + " - " + e.getMessage());
            }
            return null;
        });
    }

    /**
     * Получает размеры закэшированного изображения
     */
    public static Dimension getCachedImageSize(String imagePath) {
        return imageDimensions.getOrDefault(imagePath, new Dimension(0, 0));
    }

    /**
     * Очищает кэш изображений
     */
    public static void clearCache() {
        imageCache.clear();
        imageDimensions.clear();
    }

    /**
     * Удаляет конкретное изображение из кэша
     */
    public static void removeFromCache(String imagePath) {
        imageCache.remove(imagePath);
        imageDimensions.remove(imagePath);
    }
}
