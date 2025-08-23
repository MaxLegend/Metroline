package metroline.objects.enums;

import metroline.util.localizate.LngUtil;

public enum GameplayUnitsType {
    BIG_HOUSE(1.1f, "units.big_house"),
    HOUSE(3.2f,"units.house"),
    SMALL_HOUSE(1.2f,"units.small_house"),
    PERSONAL_HOUSE(3.2f,"units.personal_house"),
    BIG_PERSONAL_HOUSE(1.2f,"units.big_personal_house"),
    MUSEUM(1.1f, "units.museum"),
    AIRPORT(3.2f,"units.airport"),
    CHURCH(1.2f,"units.church"),
    CITYHALL(2.0f,"units.cityhall"),
    HOUSE_CULTURE(1.4f,"units.house_culture"),
    SHOP(3.0f,"units.shop"),
    FACTORY(2.7f,"units.factory"),
    FACTORY2(2.7f,"units.factory2"),
    FACTORY3(2.7f,"units.factory3"),
    FACTORY4(2.7f,"units.factory4"),
    FACTORY5(2.7f,"units.factory5"),
    PORT(1.8f,"units.port");

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
