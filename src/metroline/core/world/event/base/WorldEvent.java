package metroline.core.world.event.base;

import metroline.core.world.GameWorld;
import metroline.util.MetroLogger;

import java.util.UUID;

/**
 * Абстрактный базовый класс для всех игровых событий.
 */
public abstract class WorldEvent {
    protected final String id;
    protected final String name;
    protected final String description;
    protected final long startTime;
    protected final long duration; // в миллисекундах игрового времени
    protected final GameWorld world;
    protected boolean isActive;
    protected float severity; // 0.0 (легкое) - 1.0 (катастрофическое)

    public WorldEvent(GameWorld world, String name, String description, long duration, float severity) {
        this.id = UUID.randomUUID().toString();
        this.world = world;
        this.name = name;
        this.description = description;
        this.startTime = world.getGameTime().getCurrentTimeMillis();
        this.duration = duration;
        this.severity = Math.max(0.0f, Math.min(1.0f, severity));
        this.isActive = true;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return isActive; }
    public float getSeverity() { return severity; }
    public long getTimeRemaining() {
        long elapsed = world.getGameTime().getCurrentTimeMillis() - startTime;
        return Math.max(0, duration - elapsed);
    }

    /**
     * Абстрактный метод для применения эффектов события.
     * Должен быть реализован в конкретных классах событий.
     */
    public abstract void applyEffects();

    /**
     * Абстрактный метод для отмены эффектов события после его завершения.
     * Должен быть реализован в конкретных классах событий.
     */
    public abstract void removeEffects();

    /**
     * Проверяет, истекло ли время события, и деактивирует его.
     * @return true, если событие завершено и деактивировано.
     */
    public boolean checkAndDeactivate() {
        if (isActive && getTimeRemaining() <= 0) {
            this.isActive = false;
            removeEffects();
            MetroLogger.logInfo("Событие '" + name + "' завершилось.");
            return true;
        }
        return false;
    }
}