package metroline.objects.enums;

import metroline.objects.gameobjects.GameConstants;

public enum TrainType {
    FIRST("fisrt", 0.005f, GameConstants.TRAIN_REVENUE_MULTIPLIER_FIRST),
    OLD("old",0.008f,GameConstants.TRAIN_REVENUE_MULTIPLIER_OLD),
    CLASSIC("classic",0.01f,GameConstants.TRAIN_REVENUE_MULTIPLIER_CLASSIC),
    MODERN("modern",0.015f,GameConstants.TRAIN_REVENUE_MULTIPLIER_MODERN),
    NEW("new",0.03f,GameConstants.TRAIN_REVENUE_MULTIPLIER_NEW),
    NEWEST("newest",0.044f,GameConstants.TRAIN_REVENUE_MULTIPLIER_NEWEST),
    FUTURISTIC("futuristic",0.07f,GameConstants.TRAIN_REVENUE_MULTIPLIER_FUTURISTIC),
    FAR_FUTURISTIC("far_futuristic",0.1f,GameConstants.TRAIN_REVENUE_MULTIPLIER_FAR_FUTURISTIC);


    public float speed;
    public String name;
    private final float revenueMultiplier;
    TrainType(String name, float speed, float revenueMultiplier) {
        this.name = name;
        this.speed = speed;
        this.revenueMultiplier = revenueMultiplier;
    }

    public String getName() {
        return name;
    }

    public float getRevenueMultiplier() {
        return revenueMultiplier;
    }
    public float getSpeed() {
        return speed;
    }
}
