package metroline.core.time;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Class for controllin game time
 * @author Tesmio
 */
/**
 * Управление внутриигровым временем.
 *
 * - Хранит игровое время (epochMillis).
 * - Поддерживает ускорение времени (timeScale).
 * - Поддерживает паузу (paused).
 * - Использует один Swing Timer (transient) для регулярного обновления.
 * - При десериализации таймер восстанавливается в readObject(...).
 *
 * Формат отображения: "HH:mm dd.MM.yyyy"
 */
public class GameTime implements Serializable {
    private static final long serialVersionUID = 1L;

    // Сериализуемые поля (сохраняются в файл)
    // Сериализуемые поля
    private long epochMillis;        // текущее игровое время (мс с эпохи)
    private double timeScale = 1002000.0; // ускорение (1.0 = реальное время)
    private boolean paused = true;   // флаг паузы

    // transient поля
    private transient Timer timer;         // Swing таймер
    private transient long lastRealMillis; // момент последнего обновления
    private transient int lastMinute = -1;
    private transient int lastDay = -1;
    private transient int lastHour = -1;

    private static final int TICK_MS = 50; // частота тиков (мс)
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Конструктор: ставим игровую дату по умолчанию — 1 января 1930 00:00 (локальная зона).
     * Таймер не стартует автоматически — стартует SandboxWorld при создании мира (gameTime.start()).
     */
    public GameTime() {
        ZonedDateTime zdt = LocalDateTime.of(1863, 1, 1, 0, 0)
                                         .atZone(ZoneId.of("UTC"));
        this.epochMillis = zdt.toInstant().toEpochMilli();
        this.paused = true;
        // timer не создаём здесь, чтобы не запускать таймер вне контекста UI/мода.
    }

    /**
     * Запускает игровой таймер и снимает паузу.
     * Если таймер не создан — создаётся.
     */
    public synchronized void start() {
        lastRealMillis = System.currentTimeMillis();
        paused = false;
        ensureTimer();
    }

    /**
     * Останавливает таймер и ставит на паузу.
     */
    public synchronized void stop() {
        paused = true;
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    /**
     * Переключает паузу.
     */
    public synchronized void togglePause() {
        paused = !paused;
        if (!paused) {
            // Сбрасываем базовый момент, чтобы избежать скачка времени
            lastRealMillis = System.currentTimeMillis();
        }
        ensureTimer();
    }

    /**
     * Устанавливает множитель ускорения времени (>0).
     */
    public synchronized void setTimeScale(double scale) {
        if (scale <= 0) throw new IllegalArgumentException("timeScale must be > 0");
        this.timeScale = scale;
    }

    public synchronized double getTimeScale() { return timeScale; }
    public synchronized boolean isPaused() { return paused; }
//    public String formatDate(long timeMillis) {
//        // Конвертируем игровое время в читаемую дату
//        long days = TimeUnit.MILLISECONDS.toDays(timeMillis);
//        int year = (int)(days / 365) + 1;
//        int month = (int)((days % 365) / 30) + 1;
//        int day = (int)(days % 30) + 1;
//
//        return String.format("%02d.%02d.%02d", day, month, year);
//    }
    /**
     * Возвращает текущее игровое время в миллисекундах (epoch).
     */
    public synchronized long getCurrentTimeMillis() { return epochMillis; }

    public synchronized void setCurrentTimeMillis(long time) {
         epochMillis = time;
    }

    /**
     * Возвращает Instant текущего игрового времени.
     */
    public synchronized Instant getCurrentInstant() { return Instant.ofEpochMilli(epochMillis); }

    /**
     * Форматированная строка времени для HUD.
     */
//    public synchronized String getDateTimeString() {
//        Instant inst = Instant.ofEpochMilli(epochMillis);
//        return LocalDateTime.ofInstant(inst, ZoneId.systemDefault()).format(DISPLAY_FMT);
//    }
    /**
     * Проверяет, наступила ли новая игровая минута
     * @return true если минута изменилась с последней проверки
     */
    public synchronized boolean checkMinutePassed() {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        int currentMinute = instant.atZone(ZoneId.systemDefault()).getMinute();

        if (currentMinute != lastMinute) {
            lastMinute = currentMinute;
            return true;
        }
        return false;
    }
    public synchronized boolean checkHourPassed() {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        int currentHour = instant.atZone(ZoneId.systemDefault()).getHour();

        if (currentHour != lastHour) {
            lastHour = currentHour;
            return true;
        }
        return false;
    }
    public synchronized boolean checkDayPassed() {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        int currentDay = instant.atZone(ZoneId.systemDefault()).getDayOfMonth();

        if (currentDay != lastDay) {
            lastDay = currentDay;
            return true;
        }
        return false;
    }
    /**
     * Создаёт (если нужно) и запускает Swing Timer.
     * Timer сам ничего не прибавляет при paused == true.
     */
    private synchronized void ensureTimer() {
        if (timer != null) return;

        lastRealMillis = System.currentTimeMillis();
        timer = new Timer(TICK_MS, e -> tick());
        timer.setRepeats(true);
        timer.start();
    }

    /**
     * Шаг обновления, вызывается на EDT (Timer).
     * Добавляем в epochMillis (realDelta * timeScale).
     */
    private synchronized void tick() {
        long now = System.currentTimeMillis();
        long realDelta = now - lastRealMillis;
        lastRealMillis = now;

        if (paused) return;

        long gameDelta = (long) Math.round(realDelta * timeScale);

        // Защита от переполнения (особенно при больших timeScale)
        if (epochMillis > 0 && gameDelta > Long.MAX_VALUE - epochMillis) {
            epochMillis = Long.MAX_VALUE;
        } else if (epochMillis < 0 && gameDelta < Long.MIN_VALUE - epochMillis) {
            epochMillis = Long.MIN_VALUE;
        } else {
            epochMillis += gameDelta;
        }
    }
    /**
     * Возвращает текущую игровую дату и время в формате "HH:mm dd.MM.yyyy"
     */
    public synchronized String getDateTimeString() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                            .format(DATE_TIME_FORMATTER);
    }

    /**
     * Возвращает только время в формате "HH:mm"
     */
    public synchronized String getTimeString() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                            .format(TIME_FORMATTER);
    }

    /**
     * Возвращает только дату в формате "dd.MM.yyyy"
     */
    public synchronized String getDateString() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                            .format(DATE_FORMATTER);
    }

    /**
     * Возвращает год (4 цифры)
     */
    public synchronized int getYear() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                            .getYear();
    }

    /**
     * Возвращает месяц (1-12)
     */
    public synchronized int getMonth() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                            .getMonthValue();
    }

    /**
     * Возвращает день месяца (1-31)
     */
    public synchronized int getDayOfMonth() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                            .getDayOfMonth();
    }

    /**
     * Возвращает час дня (0-23)
     */
    public synchronized int getHour() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                            .getHour();
    }

    /**
     * Возвращает минуты (0-59)
     */
    public synchronized int getMinute() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                            .getMinute();
    }

    /**
     * Возвращает количество дней с начала игровой эпохи
     */
    public synchronized long getDaysSinceEpoch() {
        return TimeUnit.MILLISECONDS.toDays(epochMillis);
    }

    /**
     * Форматирует произвольное время в формате "dd.MM.yyyy"
     */
    public static String formatDate(long timeMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault())
                            .format(DATE_FORMATTER);
    }

    /**
     * Форматирует произвольное время в формате "HH:mm dd.MM.yyyy"
     */
    public static String formatDateTime(long timeMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault())
                            .format(DATE_TIME_FORMATTER);
    }
    /**
     * При десериализации — восстанавливаем transient-поля (таймер).
     * Если мир был в паузе, paused останется true, но таймер создадим —
     * это безопасно: tick() не будет прибавлять время пока paused == true.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Восстанавливаем таймер, чтобы время продолжило тикать или корректно реагировать на togglePause().
        ensureTimer();
    }
}