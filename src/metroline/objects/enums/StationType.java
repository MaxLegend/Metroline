package metroline.objects.enums;

import metroline.util.LngUtil;

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
    ON_WATER("stype.onwater");

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
