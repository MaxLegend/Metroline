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

    public SandboxWorld(int width, int height,  Color worldColor) {
        super(null, width, height, false, false, false ,false,worldColor, SAVE_FILE);
        generateWorld(worldColor);
    }
    public void generateWorld( Color worldColor) {
        //Create world grid
        MetroLogger.logInfo("Generating sandbox world...");
        worldGrid = new WorldTile[width][height];
        gameGrid = new GameTile[width][height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    worldGrid[x][y] = new WorldTile(x, y, 0f, false, 0, 0, Color.DARK_GRAY);
                    worldGrid[x][y].setBaseTileColor(worldColor);
                    gameGrid[x][y] = new GameTile(x, y);
                }

        }
        MetroLogger.logInfo("World successfully created!");
    }
    @Override
    public World getWorld() {
        return this;
    }

}
