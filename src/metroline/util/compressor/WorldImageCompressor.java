package metroline.util.compressor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.HashMap;
import java.util.Map;

public class WorldImageCompressor {

    private static final int CHUNK_SIZE = 64; // 64x64 тайлов в чанке
    private static Map<String, BufferedImage> chunkCache = new HashMap<>();

    /**
     * Сжимает изображение мира с автоматическим разделением на чанки
     */
    public static BufferedImage compressWorldImageOptimized(VolatileImage sourceImage,
            int worldWidth, int worldHeight,
            int tileSize) {
        if (sourceImage == null) {
            System.out.println("[COMPRESSION] Source image is null - skipping compression");
            return null;
        }

        System.out.println("[COMPRESSION] Starting chunk-based compression:");
        System.out.println("[COMPRESSION] World size: " + worldWidth + "x" + worldHeight + " tiles");

        long startTime = System.nanoTime();

        BufferedImage sourceBuffer = convertVolatileImageToBufferedImage(sourceImage);
        if (sourceBuffer == null) {
            System.out.println("[COMPRESSION] Failed to convert volatile image to buffered image");
            return null;
        }

        // Очищаем старый кэш чанков
        chunkCache.clear();

        // Создаем главное сжатое изображение (маленькое, для навигации)
        BufferedImage mainCompressed = new BufferedImage(
                worldWidth, worldHeight, BufferedImage.TYPE_INT_ARGB
        );

        // Разбиваем на чанки и сжимаем каждый отдельно
        int chunksX = (int) Math.ceil((double) worldWidth / CHUNK_SIZE);
        int chunksY = (int) Math.ceil((double) worldHeight / CHUNK_SIZE);

        System.out.println("[COMPRESSION] Dividing into " + chunksX + "x" + chunksY + " chunks");

        for (int chunkY = 0; chunkY < chunksY; chunkY++) {
            for (int chunkX = 0; chunkX < chunksX; chunkX++) {
                compressChunk(sourceBuffer, mainCompressed,
                        chunkX, chunkY, worldWidth, worldHeight, tileSize);
            }
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;

        System.out.println("[COMPRESSION] Chunk compression completed in " + durationMs + "ms");
        System.out.println("[COMPRESSION] Total chunks: " + chunkCache.size());

        return mainCompressed;
    }

    /**
     * Сжимает отдельный чанк
     */
    private static void compressChunk(BufferedImage source, BufferedImage mainCompressed,
            int chunkX, int chunkY,
            int worldWidth, int worldHeight, int tileSize) {

        int startX = chunkX * CHUNK_SIZE;
        int startY = chunkY * CHUNK_SIZE;
        int endX = Math.min(startX + CHUNK_SIZE, worldWidth);
        int endY = Math.min(startY + CHUNK_SIZE, worldHeight);

        // Создаем чанк
        BufferedImage chunk = new BufferedImage(
                CHUNK_SIZE, CHUNK_SIZE, BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D chunkG = chunk.createGraphics();
        try {
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    int pixelX = x * tileSize;
                    int pixelY = y * tileSize;

                    // Берем цвет из исходного изображения
                    int rgb = source.getRGB(pixelX, pixelY);

                    // Записываем в главное сжатое изображение
                    mainCompressed.setRGB(x, y, rgb);

                    // Записываем в чанк (относительные координаты)
                    chunk.setRGB(x - startX, y - startY, rgb);
                }
            }
        } finally {
            chunkG.dispose();
        }

        // Сохраняем чанк в кэш
        String chunkKey = chunkX + "_" + chunkY;
        chunkCache.put(chunkKey, chunk);

        System.out.println("[COMPRESSION] Chunk " + chunkKey + " compressed: " +
                (endX - startX) + "x" + (endY - startY));
    }

    /**
     * Распаковывает сжатое изображение обратно в полноразмерное с использованием чанков
     */
    public static BufferedImage decompressWorldImage(BufferedImage compressedImage, int tileSize) {
        if (compressedImage == null) {
            System.out.println("[DECOMPRESSION] Compressed image is null");
            return null;
        }

        int worldWidth = compressedImage.getWidth();
        int worldHeight = compressedImage.getHeight();

        System.out.println("[DECOMPRESSION] Starting chunk-based decompression:");
        System.out.println("[DECOMPRESSION] World size: " + worldWidth + "x" + worldHeight);

        long startTime = System.nanoTime();

        BufferedImage decompressed = new BufferedImage(
                worldWidth * tileSize, worldHeight * tileSize, BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = decompressed.createGraphics();
        try {
            // Если чанки есть в кэше - используем их
            if (!chunkCache.isEmpty()) {
                decompressFromChunks(g, worldWidth, worldHeight, tileSize);
            } else {
                // Fallback: распаковываем из главного сжатого изображения
                decompressFromMainImage(g, compressedImage, tileSize);
            }
        } finally {
            g.dispose();
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;

        System.out.println("[DECOMPRESSION] Decompression completed in " + durationMs + "ms");
        return decompressed;
    }

    /**
     * Распаковывает из чанков
     */
    private static void decompressFromChunks(Graphics2D g, int worldWidth, int worldHeight, int tileSize) {
        int chunksX = (int) Math.ceil((double) worldWidth / CHUNK_SIZE);
        int chunksY = (int) Math.ceil((double) worldHeight / CHUNK_SIZE);

        System.out.println("[DECOMPRESSION] Using " + chunkCache.size() + " chunks");

        for (int chunkY = 0; chunkY < chunksY; chunkY++) {
            for (int chunkX = 0; chunkX < chunksX; chunkX++) {
                String chunkKey = chunkX + "_" + chunkY;
                BufferedImage chunk = chunkCache.get(chunkKey);

                if (chunk != null) {
                    int startX = chunkX * CHUNK_SIZE;
                    int startY = chunkY * CHUNK_SIZE;

                    // Рисуем чанк
                    for (int y = 0; y < CHUNK_SIZE; y++) {
                        for (int x = 0; x < CHUNK_SIZE; x++) {
                            int worldX = startX + x;
                            int worldY = startY + y;

                            if (worldX < worldWidth && worldY < worldHeight) {
                                int rgb = chunk.getRGB(x, y);
                                g.setColor(new Color(rgb, true));
                                g.fillRect(worldX * tileSize, worldY * tileSize, tileSize, tileSize);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Распаковывает из главного сжатого изображения (fallback)
     */
    private static void decompressFromMainImage(Graphics2D g, BufferedImage compressedImage, int tileSize) {
        int worldWidth = compressedImage.getWidth();
        int worldHeight = compressedImage.getHeight();

        System.out.println("[DECOMPRESSION] Using main image fallback");

        for (int y = 0; y < worldHeight; y++) {
            for (int x = 0; x < worldWidth; x++) {
                int color = compressedImage.getRGB(x, y);
                g.setColor(new Color(color, true));
                g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
            }
        }
    }

    /**
     * Освобождает память, занятую чанками
     */
    public static void flushChunkCache() {
        for (BufferedImage chunk : chunkCache.values()) {
            chunk.flush();
        }
        chunkCache.clear();
        System.out.println("[CACHE] Chunk cache flushed");
    }

    /**
     * Восстанавливает конкретный чанк в видеопамять
     */
    public static VolatileImage restoreChunkToVRAM(String chunkKey, GraphicsConfiguration gc) {
        BufferedImage chunk = chunkCache.get(chunkKey);
        if (chunk == null || gc == null) return null;

        return convertBufferedImageToVolatileImage(chunk, gc);
    }

    // ... остальные методы без изменений ...

    private static BufferedImage convertVolatileImageToBufferedImage(VolatileImage volatileImage) {
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
}
