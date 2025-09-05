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
            case PORT: return 5f;
            case AIRPORT: return 7f;
            case BIG_HOUSE: return 3.3f;
            case HOUSE: return 2.8f;
            case SMALL_HOUSE: return 2.1f;
            case PERSONAL_HOUSE: return 2.3f;
            case BIG_PERSONAL_HOUSE: return 3.1f;
            default: return 0;
        }
    }
    public float getPaymentGeneration() {
        switch (this) {
            case FACTORY: return 1.8f;
            case FACTORY2: return 1.84f;
            case FACTORY3: return 1.9f;
            case FACTORY4: return 1.76f;
            case FACTORY5: return 1.91f;
            case AIRPORT: return 3.1f;
            case PORT: return 2.4f;
            case HOUSE_CULTURE: return 1.7f;
            case MUSEUM: return 1.89f;
            case CHURCH: return 3.0f;
            case CITYHALL: return 3.6f;
            case SHOP: return 2.3f;
            default: return 0;
        }
    }
    public int getInfluenceRadius() {
        switch (this) {
            case FACTORY:
                return 3;
            case FACTORY2:
                return 7;
            case FACTORY3:
                return 5;
            case FACTORY4:
                return 5;
            case FACTORY5:
                return 5;
            case AIRPORT:
                return 9;
            case PORT:
                return 7;
            case HOUSE_CULTURE:
                return 5;
            case MUSEUM:
                return 5;
            case CHURCH:
                return 3;
            case CITYHALL:
                return 3;
            case SHOP:
                return 7;
            case BIG_HOUSE:
                return 7;
            case HOUSE:
                return 5;
            case SMALL_HOUSE:
                return 3;
            case PERSONAL_HOUSE:
                return 3;
            case BIG_PERSONAL_HOUSE:
                return 5;
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
