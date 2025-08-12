package metroline.core.world;

import java.awt.*;

public class SandboxWorld extends World {

    private static final String SAVE_FILE = "sandbox_save.bin";

    public SandboxWorld() {
        super();
    }

    public SandboxWorld(int width, int height, boolean hasOrganicPatches, boolean hasRivers, Color worldColor) {
        super(null, width, height, false, false, hasOrganicPatches,hasRivers,worldColor, SAVE_FILE);
    }

    @Override
    public World getWorld() {
        return this;
    }

}
