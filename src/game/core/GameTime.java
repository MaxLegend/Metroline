package game.core;

import javax.swing.*;
import java.io.Serializable;
import java.util.Calendar;

/**
 * Class for controllin game time
 * @author Tesmio
 */
public class GameTime implements Serializable {
    public static final GameTime INSTANCE = new GameTime();
    private static final long serialVersionUID = 1L;

    // Текущая дата и время в игре
    private Calendar currentTime;
    private transient Timer timer;
    // Коэффициент ускорения времени (1.0 - реальное время)
    private float timeScale;

    // Время последнего обновления (в мс реального времени)
    private transient long lastUpdateTime;

    // Флаг паузы
    private boolean paused;

    public GameTime() {
        // Начинаем с текущей даты или можно задать конкретную начальную дату
        currentTime = Calendar.getInstance();
        currentTime.set(1930, Calendar.JANUARY, 1, 0, 0, 0);
        timeScale = 24.0f; // По умолчанию время идет в 24 раза быстрее
        lastUpdateTime = System.currentTimeMillis();
        paused = false;
    }
    public void start() {
        if (timer != null) timer.stop();

        timer = new Timer(1000, e -> update()); // Обновление каждую секунду
        timer.start();
    }

    public void stop() {
        if (timer != null) timer.stop();
    }
    public void reset() {
        currentTime.set(1930, Calendar.JANUARY, 1, 0, 0, 0);
      timer.restart();

    }
    public void update() {
        if (!paused) {
            // Добавляем 1 игровую минуту при каждом обновлении
            currentTime.add(Calendar.MINUTE, 1);
        }
    }



    /**
     * Устанавливает коэффициент ускорения времени
     * @param scale новый коэффициент (1.0 - реальное время)
     */
    public void setTimeScale(float scale) {
        this.timeScale = scale;
    }

    public float getTimeScale() {
        return timeScale;
    }

    /**
     * Пауза/продолжение игрового времени
     */
    public void togglePause() {
        paused = !paused;
        if (!paused) {
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    public boolean isPaused() {
        return paused;
    }


    /**
     * Возвращает полную строку даты и времени
     */
    public String getDateTimeString() {
        return String.format("%02d:%02d %02d.%02d.%d",
                currentTime.get(Calendar.HOUR_OF_DAY),
                currentTime.get(Calendar.MINUTE),
                currentTime.get(Calendar.DAY_OF_MONTH),
                currentTime.get(Calendar.MONTH) + 1,
                currentTime.get(Calendar.YEAR));
    }

    /**
     * Возвращает timestamp текущего игрового времени
     */
    public long getCurrentTimeMillis() {
        return currentTime.getTimeInMillis();
    }
    public Calendar getCurrentTime() {
        return currentTime;
    }
    /**
     * Проверяет, прошло ли указанное время с момента startTime
     * @param startTime начальный момент времени (в мс игрового времени)
     * @param duration продолжительность для проверки (в мс игрового времени)
     * @return true если duration прошло с startTime
     */
    public boolean hasTimePassed(long startTime, long duration) {
        return (getCurrentTimeMillis() - startTime) >= duration;
    }
}