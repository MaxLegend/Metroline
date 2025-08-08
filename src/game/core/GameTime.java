package game.core;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

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
    private long epochMillis;        // текущее игровое время (мс с эпохи)
    private double timeScale = 12.0; // ускорение (1.0 = реальное время)
    private boolean paused = true;   // флаг паузы

    // transient — не сериализуются
    private transient Timer timer;         // Swing таймер
    private transient long lastRealMillis; // момент последнего обновления (мс), для корректных дельт

    private static final int TICK_MS = 50; // частота тиков (мс) — ~20 тиков/сек
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

    /**
     * Конструктор: ставим игровую дату по умолчанию — 1 января 1930 00:00 (локальная зона).
     * Таймер не стартует автоматически — стартует SandboxWorld при создании мира (gameTime.start()).
     */
    public GameTime() {
        ZonedDateTime zdt = LocalDateTime.of(1930, 1, 1, 0, 0)
                                         .atZone(ZoneId.systemDefault());
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

    /**
     * Возвращает текущее игровое время в миллисекундах (epoch).
     */
    public synchronized long getCurrentTimeMillis() { return epochMillis; }

    /**
     * Возвращает Instant текущего игрового времени.
     */
    public synchronized Instant getCurrentInstant() { return Instant.ofEpochMilli(epochMillis); }

    /**
     * Форматированная строка времени для HUD.
     */
    public synchronized String getDateTimeString() {
        Instant inst = Instant.ofEpochMilli(epochMillis);
        return LocalDateTime.ofInstant(inst, ZoneId.systemDefault()).format(DISPLAY_FMT);
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

        if (paused) {
            // при паузе ничего не добавляем, но базовую отметку уже обновили
            return;
        }

        long gameDelta = (long) Math.round(realDelta * timeScale);
        epochMillis += gameDelta;
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