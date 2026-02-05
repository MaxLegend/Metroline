package metroline.objects.enums;

import metroline.util.localizate.LngUtil;

public enum TunnelType {
    DESTROYED("ttype.destroyed"),
    PLANNED("ttype.planned"),
    BUILDING("ttype.building"),
    ACTIVE("ttype.active");

    private final String name;

    TunnelType(String name) {
        this.name = name;
    }

    public String getLocalizedName() {
        return LngUtil.translatable(name);
    }
}
