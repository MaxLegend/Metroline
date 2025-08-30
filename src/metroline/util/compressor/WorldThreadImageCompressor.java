package metroline.util.compressor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.HashMap;
import java.util.Map;

public class WorldThreadImageCompressor {

    private static final int CHUNK_SIZE = 64; // 64x64 тайлов в чанке
    private static final int MAX_CHUNKS_IN_MEMORY = 16; // Максимум чанков в памяти
    private static Map<String, BufferedImage> chunkCache = new HashMap<>();

    /**
     * Сжимает изображение мира с потоковой обработкой чанков
     */
    public static BufferedImage compressWorldImageOptimized(VolatileImage sourceImage,
            int worldWidth, int worldHeight,
            int tileSize) {
        if (sourceImage == null) {
            System.out.println("[COMPRESSION] Source image is null - skipping compression");
            return null;
        }

        System.out.println("[COMPRESSION] Starting stream compression:");
        System.out.println("[COMPRESSION] World size: " + worldWidth + "x" + worldHeight + " tiles");

        long startTime = System.nanoTime();

        // Очищаем старый кэш чанков
        flushChunkCache();

        // Создаем главное сжатое изображение (маленькое, для навигации)
        BufferedImage mainCompressed = new BufferedImage(
                worldWidth, worldHeight, BufferedImage.TYPE_INT_RGB // Используем RGB для экономии памяти
        );

        // Разбиваем на чанки и обрабатываем потоково
        int chunksX = (int) Math.ceil((double) worldWidth / CHUNK_SIZE);
        int chunksY = (int) Math.ceil((double) worldHeight / CHUNK_SIZE);

        System.out.println("[COMPRESSION] Dividing into " + chunksX + "x" + chunksY + " chunks");

        Graphics2D mainG = mainCompressed.createGraphics();
        try {
            for (int chunkY = 0; chunkY < chunksY; chunkY++) {
                for (int chunkX = 0; chunkX < chunksX; chunkX++) {
                    // Обрабатываем чанк потоково, без загрузки всего изображения в память
                    processChunkStreaming(sourceImage, mainG,
                            chunkX, chunkY,
                            worldWidth, worldHeight, tileSize);

                    // Очищаем память после обработки каждого чанка
                    if (chunkCache.size() > MAX_CHUNKS_IN_MEMORY) {
                        flushOldestChunks();
                    }
                }
            }
        } finally {
            mainG.dispose();
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;

        System.out.println("[COMPRESSION] Stream compression completed in " + durationMs + "ms");
        System.out.println("[COMPRESSION] Memory used: " +
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");

        return mainCompressed;
    }

    /**
     * Обрабатывает чанк без загрузки всего изображения в память
     */
    /**
     * Обрабатывает чанк из буфера
     */
    private static void processChunkFromBuffer(BufferedImage chunkBuffer, Graphics2D mainG,
            int startX, int startY, int endX, int endY,
            int mainWidth, int mainHeight) {

        // Размер чанка в тайлах
        int chunkWidthTiles = endX - startX;
        int chunkHeightTiles = endY - startY;

        for (int tileY = 0; tileY < chunkHeightTiles; tileY++) {
            for (int tileX = 0; tileX < chunkWidthTiles; tileX++) {
                // Координаты центра тайла в пикселях внутри чанка
                int centerX = tileX * 32 + 16;
                int centerY = tileY * 32 + 16;

                // Проверяем, что координаты внутри bounds chunkBuffer
                if (centerX < chunkBuffer.getWidth() && centerY < chunkBuffer.getHeight()) {
                    try {
                        int rgb = chunkBuffer.getRGB(centerX, centerY);

                        // Записываем в главное сжатое изображение
                        int worldX = startX + tileX;
                        int worldY = startY + tileY;

                        // Проверяем границы главного изображения
                        if (worldX < mainWidth && worldY < mainHeight) {
                            mainG.setColor(new Color(rgb));
                            mainG.fillRect(worldX, worldY, 1, 1);
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // Игнорируем пиксели за границами
                        System.out.println("[WARNING] Pixel out of bounds: " + centerX + "," + centerY +
                                " in chunk " + chunkBuffer.getWidth() + "x" + chunkBuffer.getHeight());
                    }
                }
            }
        }
    }
    /**
     * Обрабатывает чанк из буфера
     */
    /**
     * Обрабатывает чанк из буфера
     */
    private static void processChunkStreaming(VolatileImage source, Graphics2D mainG,
            int chunkX, int chunkY,
            int worldWidth, int worldHeight, int tileSize) {

        int startX = chunkX * CHUNK_SIZE;
        int startY = chunkY * CHUNK_SIZE;
        int endX = Math.min(startX + CHUNK_SIZE, worldWidth);
        int endY = Math.min(startY + CHUNK_SIZE, worldHeight);

        // Размер чанка в тайлах
        int chunkWidthTiles = endX - startX;
        int chunkHeightTiles = endY - startY;

        // Размер чанка в пикселях
        int chunkWidthPixels = chunkWidthTiles * tileSize;
        int chunkHeightPixels = chunkHeightTiles * tileSize;

        // Создаем маленький буфер для текущего чанка (в пикселях!)
        BufferedImage chunkBuffer = new BufferedImage(
                chunkWidthPixels, chunkHeightPixels, BufferedImage.TYPE_INT_RGB
        );

        // Копируем только нужную область из VolatileImage
        Graphics2D chunkG = chunkBuffer.createGraphics();
        try {
            // Рисуем только нужную область источника
            chunkG.drawImage(source,
                    0, 0, chunkWidthPixels, chunkHeightPixels, // destination
                    startX * tileSize, startY * tileSize, // source start
                    startX * tileSize + chunkWidthPixels, startY * tileSize + chunkHeightPixels, // source end
                    null);
        } finally {
            chunkG.dispose();
        }

        // Обрабатываем чанк с передачей правильных размеров
        processChunkFromBuffer(chunkBuffer, mainG, startX, startY, endX, endY,
                worldWidth, worldHeight);

        // Освобождаем память сразу после обработки
        chunkBuffer.flush();
    }
    /**
     * Удаляет самые старые чанки из кэша
     */
    private static void flushOldestChunks() {
        if (chunkCache.size() > MAX_CHUNKS_IN_MEMORY) {
            // Удаляем первые несколько чанков (простейшая стратегия)
            int chunksToRemove = chunkCache.size() - MAX_CHUNKS_IN_MEMORY;
            var iterator = chunkCache.entrySet().iterator();

            for (int i = 0; i < chunksToRemove && iterator.hasNext(); i++) {
                var entry = iterator.next();
                entry.getValue().flush();
                iterator.remove();
            }

            System.out.println("[MEMORY] Flushed " + chunksToRemove + " oldest chunks");
        }
    }

    /**
     * Распаковывает сжатое изображение с потоковой обработкой
     */
    public static BufferedImage decompressWorldImage(BufferedImage compressedImage, int tileSize) {
        if (compressedImage == null) {
            System.out.println("[DECOMPRESSION] Compressed image is null");
            return null;
        }

        int worldWidth = compressedImage.getWidth();
        int worldHeight = compressedImage.getHeight();

        System.out.println("[DECOMPRESSION] Starting stream decompression:");

        long startTime = System.nanoTime();

        // Создаем пустое изображение для результата
        BufferedImage decompressed = new BufferedImage(
                worldWidth * tileSize, worldHeight * tileSize, BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = decompressed.createGraphics();
        try {
            // Потоковая распаковка: обрабатываем построчно
            for (int y = 0; y < worldHeight; y++) {
                for (int x = 0; x < worldWidth; x++) {
                    int color = compressedImage.getRGB(x, y);
                    g.setColor(new Color(color));
                    g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
                }

                // Периодически вызываем GC для больших изображений
                if (y % 100 == 0 && worldHeight > 200) {
                    System.gc();
                }
            }
        } finally {
            g.dispose();
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;

        System.out.println("[DECOMPRESSION] Stream decompression completed in " + durationMs + "ms");
        return decompressed;
    }

    /**
     * Освобождает всю память, занятую чанками
     */
    public static void flushChunkCache() {
        for (BufferedImage chunk : chunkCache.values()) {
            chunk.flush();
        }
        chunkCache.clear();
        System.gc(); // Принудительная сборка мусора
        System.out.println("[CACHE] Chunk cache completely flushed");
    }

    /**
     * Проверяет, можно ли обработать мир такого размера
     */
    public static boolean canHandleWorldSize(int width, int height, int tileSize) {
        long estimatedMemory = (long) width * height * 4; // Основное сжатое изображение
        long availableMemory = Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory();

        System.out.println("[MEMORY] Estimated: " + estimatedMemory / 1024 / 1024 +
                "MB, Available: " + availableMemory / 1024 / 1024 + "MB");

        return estimatedMemory < availableMemory * 0.7; // 70% от доступной памяти
    }

    /**
     * Конвертирует VolatileImage в BufferedImage
     */
    public static BufferedImage convertVolatileImageToBufferedImage(VolatileImage volatileImage) {
        if (volatileImage == null) return null;

        BufferedImage bufferedImage = new BufferedImage(
                volatileImage.getWidth(),
                volatileImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = bufferedImage.createGraphics();
        try {
            g.drawImage(volatileImage, 0, 0, null);
        } finally {
            g.dispose();
        }

        return bufferedImage;
    }

    /**
     * Создает VolatileImage из BufferedImage
     */
    public static VolatileImage convertBufferedImageToVolatileImage(BufferedImage bufferedImage,
            GraphicsConfiguration gc) {
        if (bufferedImage == null || gc == null) return null;

        VolatileImage volatileImage = gc.createCompatibleVolatileImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                Transparency.TRANSLUCENT
        );

        Graphics2D g = volatileImage.createGraphics();
        try {
            g.drawImage(bufferedImage, 0, 0, null);
        } finally {
            g.dispose();
        }

        return volatileImage;
    }

    /**
     * Альтернативная версия с использованием getRGB() для большей эффективности
     * (сохранена для обратной совместимости)
     */
    public static BufferedImage compressWorldImageLegacy(VolatileImage sourceImage,
            int worldWidth, int worldHeight,
            int tileSize) {
        if (sourceImage == null) {
            System.out.println("[COMPRESSION] Source image is null - skipping compression");
            return null;
        }

        System.out.println("[COMPRESSION] Starting legacy compression:");

        long startTime = System.nanoTime();

        BufferedImage sourceBuffer = convertVolatileImageToBufferedImage(sourceImage);
        if (sourceBuffer == null) {
            System.out.println("[COMPRESSION] Failed to convert volatile image to buffered image");
            return null;
        }

        BufferedImage compressed = new BufferedImage(worldWidth, worldHeight,
                BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < worldHeight; y++) {
            for (int x = 0; x < worldWidth; x++) {
                int pixelX = x * tileSize;
                int pixelY = y * tileSize;

                int rgb = sourceBuffer.getRGB(pixelX, pixelY);
                compressed.setRGB(x, y, rgb);
            }
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;

        System.out.println("[COMPRESSION] Legacy compression completed in " + durationMs + "ms");
        return compressed;
    }
}
