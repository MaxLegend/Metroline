package metroline.objects.enums;

import metroline.util.LngUtil;

import java.awt.*;

public enum GameplayUnitsType {
    MUSEUM(1.1f, "units.museum"),
    AIRPORT(3.2f,"units.airport"),
    CHURCH(1.2f,"units.church"),
    CITYHALL(2.0f,"units.cityhall"),
    HOUSE_CULTURE(1.4f,"units.house_culture"),
    SHOP(3.0f,"units.shop"),
    FACTORY(2.7f,"units.factory"),
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
