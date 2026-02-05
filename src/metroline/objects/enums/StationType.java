package metroline.objects.enums;

import metroline.util.localizate.LngUtil;

/**
 * Enum for station types
 */
public enum StationType {
    DESTROYED("stype.destroyed"),
    CLOSED("stype.closed"),
    PLANNED("stype.planned"),
    BUILDING("stype.building"),
    REGULAR("stype.regular"),
    TRANSFER("stype.transfer"),
    TERMINAL("stype.terminal"),
    TRANSIT("stype.transit"),
    ABANDONED("stype.abandoned"),
    DROWNED("stype.drowned"),
    RUINED("stype.ruined"),
    REPAIR("stype.repair"),
    DEPO("stype.depo"),
    BURNED("stype.burned");

    private final String name;
    StationType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public String getLocalizedName() {
        return LngUtil.translatable(name);
    }
}
