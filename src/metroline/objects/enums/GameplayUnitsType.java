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
    public String getLocalizedName() {
        return LngUtil.translatable(localizationKey);
    }
    public float getIncomeMultiplier() {
        return incomeMultiplier;
    }

}
