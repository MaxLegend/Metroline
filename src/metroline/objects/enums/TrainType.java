package metroline.objects.enums;

public enum TrainType {
    FIRST("fisrt", 0.005f),
    OLD("old",0.008f),
    CLASSIC("classic",0.01f),
    MODERN("modern",0.015f),
    NEW("new",0.03f),
    NEWEST("newest",0.044f),
    FUTURISTIC("futuristic",0.07f),
    FAR_FUTURISTIC("far_futuristic",0.1f);

    public float speed;
    public String name;
    TrainType(String name, float speed) {
        this.name = name;
        this.speed = speed;
    }

    public String getName() {
        return name;
    }

    public float getSpeed() {
        return speed;
    }
}
