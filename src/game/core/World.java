package game.core;

import game.objects.PathPoint;
import game.objects.Station;
import game.objects.Tunnel;
import game.tiles.GameTile;

import game.tiles.WorldTile;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * World class containing game logic and state
 */
public class World {
    private WorldTile[][] worldGrid;
    private GameTile[][] gameGrid;

    private List<Station> stations = new ArrayList<>();
    private List<Tunnel> tunnels = new ArrayList<>();
    private int width, height;

    public World(int width, int height) {
        this.width = width;
        this.height = height;
        generateWorld();
    }

    /**
     * Generates the world with terrain permissions
     */
    private void generateWorld() {
        // Create world grid
        worldGrid = new WorldTile[width][height];
        gameGrid = new GameTile[width][height];


        // Initialize with all perm=0
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                worldGrid[x][y] = new WorldTile(x, y, 0);
                gameGrid[x][y] = new GameTile(x, y);
            }
        }

        // Add some perm=0.5 areas
        addRandomAreas(0.5f, 5, 10, 20);

        // Add some perm=1 areas (smaller)
        addRandomAreas(1.0f, 8, 5, 10);

        // Apply gradient smoothing
        applyGradient();
    }

    /**
     * Adds random areas with specified permission value
     * @param perm Permission value for areas
     * @param count Number of areas to add
     * @param minSize Minimum size of area
     * @param maxSize Maximum size of area
     */
    private void addRandomAreas(float perm, int count, int minSize, int maxSize) {
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            int centerX = rand.nextInt(width);
            int centerY = rand.nextInt(height);
            int size = minSize + rand.nextInt(maxSize - minSize + 1);

            for (int y = Math.max(0, centerY - size); y < Math.min(height, centerY + size); y++) {
                for (int x = Math.max(0, centerX - size); x < Math.min(width, centerX + size); x++) {
                    // Simple circular area
                    double dist = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                    if (dist < size / 2.0) {
                        worldGrid[x][y].setPerm(perm);
                    }
                }
            }
        }
    }

    /**
     * Applies gradient smoothing around permission boundaries
     */
    private void applyGradient() {
        // Make a copy for reference
        float[][] originalPerm = new float[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                originalPerm[x][y] = worldGrid[x][y].getPerm();
            }
        }

        // Apply smoothing
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (originalPerm[x][y] == 0 || originalPerm[x][y] == 1) {
                    // Only smooth at boundaries
                    boolean hasNeighbor = false;
                    for (int ny = Math.max(0, y-1); ny < Math.min(height, y+2); ny++) {
                        for (int nx = Math.max(0, x-1); nx < Math.min(width, x+2); nx++) {
                            if (originalPerm[nx][ny] != originalPerm[x][y]) {
                                hasNeighbor = true;
                                break;
                            }
                        }
                        if (hasNeighbor) break;
                    }

                    if (hasNeighbor) {
                        // Calculate average of neighbors
                        float sum = 0;
                        int count = 0;
                        for (int ny = Math.max(0, y-1); ny < Math.min(height, y+2); ny++) {
                            for (int nx = Math.max(0, x-1); nx < Math.min(width, x+2); nx++) {
                                if (nx != x || ny != y) {
                                    sum += originalPerm[nx][ny];
                                    count++;
                                }
                            }
                        }
                        worldGrid[x][y].setPerm(sum / count);
                    }
                }
            }
        }
    }

    /**
     * Gets the world grid
     * @return 2D array of world tiles
     */
    public WorldTile[][] getWorldGrid() { return worldGrid; }

    /**
     * Gets the game grid
     * @return 2D array of game tiles
     */
    public GameTile[][] getGameGrid() { return gameGrid; }




    /**
     * Gets all stations
     * @return List of stations
     */
    public List<Station> getStations() { return stations; }

    /**
     * Gets all tunnels
     * @return List of tunnels
     */
    public List<Tunnel> getTunnels() { return tunnels; }

    /**
     * Adds a station to the world
     * @param station Station to add
     */
    public void addStation(Station station) {
        stations.add(station);

        gameGrid[station.getX()][station.getY()].setContent(station);
    }

    /**
     * Removes a station from the world
     * @param station Station to remove
     */
    public void removeStation(Station station) {
        stations.remove(station);
        gameGrid[station.getX()][station.getY()].setContent(null);

        // Remove any tunnels connected to this station
        tunnels.removeIf(t -> t.getStart() == station || t.getEnd() == station);
    }

    /**
     * Adds a tunnel between two stations
     * @param tunnel Tunnel to add
     * @return True if tunnel was added successfully
     */
    public boolean addTunnel(Tunnel tunnel) {
        if (tunnel.getStart().connect(tunnel.getEnd())) {
            tunnels.add(tunnel);
            return true;
        }
        return false;
    }

    /**
     * Removes a tunnel from the world
     * @param tunnel Tunnel to remove
     */
    public void removeTunnel(Tunnel tunnel) {
        tunnels.remove(tunnel);
        tunnel.getStart().disconnect(tunnel.getEnd());
        tunnel.getEnd().disconnect(tunnel.getStart());
    }

    /**
     * Gets the station at specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return Station or null if none exists
     */
    public Station getStationAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        GameObject obj = gameGrid[x][y].getContent();
        return obj instanceof Station ? (Station)obj : null;
    }

    /**
     * Gets the tunnel at specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return Tunnel or null if none exists
     */
    public Tunnel getTunnelAt(int x, int y) {
        for (Tunnel tunnel : tunnels) {
            for (PathPoint p : tunnel.getPath()) {
                if (p.getX() == x && p.getY() == y) {
                    return tunnel;
                }
            }
        }
        return null;
    }

    /**
     * Gets the width of the world
     * @return Width in tiles
     */
    public int getWidth() { return width; }

    /**
     * Gets the height of the world
     * @return Height in tiles
     */
    public int getHeight() { return height; }
}

