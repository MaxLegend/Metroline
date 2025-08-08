package game.core.world;

import java.awt.*;

public class SandboxWorld extends World {

    private static final String SAVE_FILE = "sandbox_save.metro";

    public SandboxWorld() {
        super();
    }

    public SandboxWorld(int width, int height, boolean hasOrganicPatches, boolean hasRivers, Color worldColor) {
        super(null, width, height, hasOrganicPatches,hasRivers,worldColor, SAVE_FILE);
    }

    @Override
    public World getWorld() {
        return this;
    }

}
