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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveToImageUtil {

    public static void saveWorldToPNG(boolean isSandbox) {
        // Создаем папку screenshots, если ее нет
        File screenshotsDir = new File("screenshots");
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdir();
        }

        // Генерируем имя файла с timestamp для уникальности
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "metro_map_" + timestamp + ".png";
        File file = new File(screenshotsDir, filename);

        try {
            // Вызываем ваш метод сохранения
            if(isSandbox) {
                saveToPNG(file, WorldSandboxScreen.getInstance());
            } else {
                saveToPNG(file, WorldGameScreen.getInstance());
            }

            // Показываем сообщение с путем к файлу
            String message = "Save: " + file.getAbsolutePath();
            MessageUtil.showTimedMessage(message, false, 4000);

        } catch (IOException ex) {
            // Показываем сообщение об ошибке
            MessageUtil.showTimedMessage("Save Error: " + ex.getMessage(), true, 4000);
            ex.printStackTrace();
        }
    }
    /**
     * Сохраняет текущее состояние мира в PNG файл
     * @param file Файл для сохранения
     * @throws IOException Если произошла ошибка сохранения
     */
    public static void saveToPNG(File file, WorldScreen screen) throws IOException {
        // Создаем изображение размером с мир
        int width =  screen.widthWorld * 32;  // 32 пикселя на клетку
        int height =  screen.heightWorld * 32;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        try {
            // Очищаем фон (прозрачный)
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, width, height);
            g2d.setComposite(AlphaComposite.SrcOver);

            // Рисуем все элементы мира
            WorldSandboxScreen.getInstance().drawStaticWorld(g2d);  // Сетка и статические объекты

            // Рисуем туннели
            for (Tunnel tunnel :  screen.getWorld().getTunnels()) {
                tunnel.draw(g2d, 0, 0, 1);
            }

            // Рисуем станции
            for (Station station : screen.getWorld().getStations()) {
                station.draw(g2d, 0, 0, 1);
            }

            // Рисуем метки
            for (Label label :  screen.getWorld().getLabels()) {
                label.draw(g2d, 0, 0, 1);
            }
        } finally {
            g2d.dispose();
        }

        // Сохраняем в файл
        ImageIO.write(image, "PNG", file);
    }
}
