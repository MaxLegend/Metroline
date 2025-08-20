package metroline.core.world;

import metroline.core.world.tiles.GameTile;
import metroline.core.world.tiles.WorldTile;
import metroline.util.MetroLogger;
import metroline.util.PerlinNoise;
import metroline.util.VoronoiNoise;

import java.awt.*;

public class SandboxWorld extends World {

    private static final String SAVE_FILE = "sandbox_save.bin";

    public SandboxWorld() {
        super();
    }

    public SandboxWorld(short width, short height,  int worldColor) {
        super(null, width, height, false, false, false ,false,worldColor, SAVE_FILE);
        generateWorld(worldColor);
    }
    public void generateWorld( int worldColor) {
        //Create world grid
        MetroLogger.logInfo("Generating sandbox world...");
        worldGrid = new WorldTile[width*height];
        gameGrid = new GameTile[width*height];
        WorldTile worldTile;
        GameTile gameTile;
            for (short y = 0; y < height; y++) {
                for (short x = 0; x < width; x++) {
                    worldTile = new WorldTile(x, y, 0f, false, 0, 0, 0x6E6E6E);
                    setWorldTile(x,y,worldTile);
                    worldTile.setBaseTileColor(worldColor);
                    gameTile = new GameTile(x, y);
                    setGameTile(x, y, gameTile);
                }

        }
        MetroLogger.logInfo("World successfully created!");
    }
    @Override
    public World getWorld() {
        return this;
    }

}
