package metroline.core.world.event.base;

import metroline.core.world.GameWorld;
import metroline.util.MetroLogger;

/**
 * Класс для глобальных событий, влияющих на всю игровую экономику или метрополитен.
 */
public class GlobalWorldEvent extends WorldEvent {

    // Эффекты, применяемые к экономике
    private final float passengerFlowMultiplier;
    private final float paymentAbilityMultiplier;
    private final float upkeepCostMultiplier;

    public GlobalWorldEvent(GameWorld world, String name, String description, long duration, float severity,
            float passengerFlowMultiplier, float paymentAbilityMultiplier, float upkeepCostMultiplier) {
        super(world, name, description, duration, severity);
        this.passengerFlowMultiplier = passengerFlowMultiplier;
        this.paymentAbilityMultiplier = paymentAbilityMultiplier;
        this.upkeepCostMultiplier = upkeepCostMultiplier;
    }

    @Override
    public void applyEffects() {
        // Эффекты применяются через EventEffectApplier или напрямую через модификаторы в EconomyManager
        MetroLogger.logInfo("НАЧАЛОСЬ ГЛОБАЛЬНОЕ СОБЫТИЕ: " + name + ". " + description);
    }

    @Override
    public void removeEffects() {
        // Эффекты автоматически прекратятся, так как модификаторы больше не применяются в calculateRevenue/Upkeep.
        MetroLogger.logInfo("Завершено глобальное событие: " + name);
    }

    // Getters для модификаторов
    public float getPassengerFlowMultiplier() { return passengerFlowMultiplier; }
    public float getPaymentAbilityMultiplier() { return paymentAbilityMultiplier; }
    public float getUpkeepCostMultiplier() { return upkeepCostMultiplier; }
}
