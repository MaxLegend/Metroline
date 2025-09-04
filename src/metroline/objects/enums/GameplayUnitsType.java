package metroline.objects.enums;

import metroline.util.localizate.LngUtil;

public enum GameplayUnitsType {
    BIG_HOUSE(1.03f, "units.big_house"),
    HOUSE(1.02f,"units.house"),
    SMALL_HOUSE(1.01f,"units.small_house"),
    PERSONAL_HOUSE(1.024f,"units.personal_house"),
    BIG_PERSONAL_HOUSE(1.028f,"units.big_personal_house"),
    MUSEUM(1.05f, "units.museum"),
    AIRPORT(1.5f,"units.airport"),
    CHURCH(1.03f,"units.church"),
    CITYHALL(1.008f,"units.cityhall"),
    HOUSE_CULTURE(1.022f,"units.house_culture"),
    SHOP(1.11f,"units.shop"),
    FACTORY(1.13f,"units.factory"),
    FACTORY2(1.16f,"units.factory2"),
    FACTORY3(1.12f,"units.factory3"),
    FACTORY4(1.33f,"units.factory4"),
    FACTORY5(1.45f,"units.factory5"),
    PORT(1.3f,"units.port");

    private final float incomeMultiplier;
    private final String localizationKey;
    GameplayUnitsType(float incomeMultiplier, String localizationKey) {
        this.incomeMultiplier = incomeMultiplier;

        this.localizationKey = localizationKey;
    }
    public boolean isPassengerBuilding() {
        return this == HOUSE
                || this == PORT
                || this == AIRPORT
                || this == SMALL_HOUSE
                || this == PERSONAL_HOUSE
                || this == BIG_PERSONAL_HOUSE
                || this == BIG_HOUSE;
    }
    public float getPassengerGeneration() {
        switch (this) {
            case PORT: return 1600;
            case AIRPORT: return 2500;
            case BIG_HOUSE: return 1000;
            case HOUSE: return 700;
            case SMALL_HOUSE: return 400;
            case PERSONAL_HOUSE: return 300;
            case BIG_PERSONAL_HOUSE: return 600;
            default: return 0;
        }
    }
    public float getPaymentGeneration() {
        switch (this) {
            case FACTORY: return 0.8f;
            case FACTORY2: return 0.84f;
            case FACTORY3: return 0.9f;
            case FACTORY4: return 0.76f;
            case FACTORY5: return 0.91f;
            case AIRPORT: return 2.1f;
            case PORT: return 1.4f;
            case HOUSE_CULTURE: return 0.7f;
            case MUSEUM: return 0.89f;
            case CHURCH: return 2.0f;
            case CITYHALL: return 2.6f;
            case SHOP: return 1.3f;
            default: return 0;
        }
    }
    public int getInfluenceRadius() {
        switch (this) {
            case FACTORY:
                return 4;
            case FACTORY2:
                return 7;
            case FACTORY3:
                return 6;
            case FACTORY4:
                return 3;
            case FACTORY5:
                return 4;
            case AIRPORT:
                return 5;
            case PORT:
                return 4;
            case HOUSE_CULTURE:
                return 4;
            case MUSEUM:
                return 5;
            case CHURCH:
                return 2;
            case CITYHALL:
                return 2;
            case SHOP:
                return 6;
            case BIG_HOUSE:
                return 4;
            case HOUSE:
                return 3;
            case SMALL_HOUSE:
                return 2;
            case PERSONAL_HOUSE:
                return 2;
            case BIG_PERSONAL_HOUSE:
                return 3;
            default:
                return 0;
        }
    }
    public String getLocalizedName() {
        return LngUtil.translatable(localizationKey);
    }
    public float getIncomeMultiplier() {
        return incomeMultiplier;
    }

}
