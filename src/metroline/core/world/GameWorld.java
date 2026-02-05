package metroline.core.world;

import metroline.MainFrame;

import metroline.core.world.tiles.GameTile;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.enums.*;
import metroline.objects.gameobjects.*;
import metroline.screens.panel.LinesLegendWindow;
import metroline.screens.worldscreens.CachedWorldScreen;
import metroline.screens.worldscreens.normal.GameWorldScreen;
import metroline.util.localizate.LngUtil;
import metroline.util.MetroLogger;
import metroline.util.serialize.MetroSerializer;
import metroline.util.ui.UserInterfaceUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;


/**
 *
 */

public class GameWorld extends World {
    private transient MainFrame mainFrame;


    private MetroSerializer worldSerializer;


    public transient LinesLegendWindow legendWindow;
    public GameWorld() {
        super();

    }

    public GameWorld(short width, short height, int worldColor) {
        super(null, width, height,worldColor);
        this.mainFrame = MainFrame.getInstance();

        generateWorld( worldColor);

    }
    /**
     * Создает новый GameWorld, загружая его состояние из BufferedReader.
     * Этот конструктор предназначен для использования сериализатором.
     *
     * @param reader BufferedReader, откуда будут читаться данные.
     * @throws IOException Если возникает ошибка при чтении.
     */
    public GameWorld(BufferedReader reader) throws IOException {
        super(); // Вызываем базовый конструктор World()


        this.mainFrame = MainFrame.getInstance();

        worldSerializer = new MetroSerializer();
        worldSerializer.recreateWorld(reader, this);


    }

    public void generateWorld(int worldColor) {
        initWorldGrid();
        MetroLogger.logInfo("Generating game world...");
        worldGrid = new WorldTile[width*height];
        gameGrid = new GameTile[width*height];
        gameplayGrid = new GameTile[width*height];  // ← ДОБАВИТЬ ЭТУ СТРОКУ

        WorldTile worldTile;
        GameTile gameTile;


        for (short y = 0; y < height; y++) {
            for (short x = 0; x < width; x++) {
                worldTile = new WorldTile(x, y, 0f, false, 0,0, 0x6E6E6E);
                setWorldTile(x, y, worldTile);
                worldTile.setBaseTileColor(worldColor);

                gameTile = new GameTile(x, y);
                setGameTile(x, y, gameTile);

            }
        }
    }

    public void update() {

    }


    public StationLabel getLabelForStation(Station station) {
        for (StationLabel stationLabel : stationLabels) {
            if (stationLabel.getParentGameObject() == station) {
                return stationLabel;
            }
        }
        return null;
    }



    public void setLegendWindow(LinesLegendWindow legendWindow) {
        this.legendWindow = legendWindow;
    }
    public void updateLegendWindow() {
        if (screen instanceof GameWorldScreen) {
            GameWorldScreen gameScreen = (GameWorldScreen) screen;
            if (gameScreen.parent != null && gameScreen.parent.legendWindow != null) {
                gameScreen.parent.legendWindow.updateLegend(this);
            }
        }
    }
    public PathPoint findFreePositionNear(int x, int y, String name) {
        // Сортируем направления по приоритету (включая диагонали)
        List<Direction> priorityDirections = Arrays.asList(
                Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH,
                Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHEAST, Direction.NORTHWEST
        );

        for (Direction dir : priorityDirections) {
            int nx = x + dir.getDx();
            int ny = y + dir.getDy();

            if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
                    getStationAt(nx, ny) == null && getLabelAt(nx, ny) == null) {

                // Проверяем, что текст не будет перекрывать станцию
                if (isTextPositionGood(dir, name)) {
                    return new PathPoint(nx, ny);
                }
            }
        }

        return null;
    }

    @Override
    public void addStation(Station station) {
        super.addStation(station);
    }
    @Override
    public void addTunnel(Tunnel tunnel) {
        super.addTunnel(tunnel);
    }


    /**
     * Gets the any game object at specified coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return GameObject or null if none exists
     */
    @Override
    public GameObject getGameObjectAt(int x, int y) {
        // Сначала проверяем станции
        Station station = this.getStationAt(x, y);
        if (station != null) {
            return station;
        }

        // Затем проверяем туннели
        Tunnel tunnel = this.getTunnelAt(x, y);
        if (tunnel != null) {
            return tunnel;
        }

        // Затем проверяем метки (если нужно)
        StationLabel stationLabel = this.getLabelAt(x, y);
        if (stationLabel != null) {
            return stationLabel;
        }

        return null;
    }



    /*********************
     * ECONOMIC SECTION
     *********************/

    @Override
    public void removeStation(Station station) {
        super.removeStation(station);

    }




    // Проверяет, есть ли вода в соседних клетках
    private boolean hasWaterNeighbor(int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue; // Пропускаем центральную клетку

                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if (getWorldTile(nx, ny).isWater()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }



    @Override
    public World getWorld() {
        return this;
    }

    public void updateConnectedTunnels(Station station) {
        List<Tunnel> tunnelsToUpdate = new ArrayList<>(this.getTunnels());

        for (Tunnel tunnel : tunnelsToUpdate) {
            if (tunnel.getStart() == station || tunnel.getEnd() == station) {
                Station otherEnd = (tunnel.getStart() == station) ? tunnel.getEnd() : tunnel.getStart();

                // Туннель может начать строиться только если:
                // 1. Обе станции в BUILDING ИЛИ
                // 2. Одна в BUILDING, а другая уже построена (не PLANNED и не BUILDING)
                boolean canStartBuilding = false;

                // Обе станции в BUILDING
                boolean bothBuilding = station.getType() == StationType.BUILDING &&
                        otherEnd.getType() == StationType.BUILDING;

                // Одна в BUILDING, другая уже построена
                boolean oneBuildingOneBuilt =
                        (station.getType() == StationType.BUILDING &&
                                otherEnd.getType() != StationType.PLANNED &&
                                otherEnd.getType() != StationType.BUILDING) ||
                                (otherEnd.getType() == StationType.BUILDING &&
                                        station.getType() != StationType.PLANNED &&
                                        station.getType() != StationType.BUILDING);

                if ((bothBuilding || oneBuildingOneBuilt) &&
                        tunnel.getType() == TunnelType.PLANNED) {

                        tunnel.setType(TunnelType.BUILDING);
                        tunnel.getWorld().addTunnel(tunnel);
                        canStartBuilding = true;

                }

                if (!canStartBuilding &&
                        station.getType() != StationType.PLANNED &&
                        station.getType() != StationType.BUILDING &&
                        otherEnd.getType() != StationType.PLANNED &&
                        otherEnd.getType() != StationType.BUILDING) {

                    tunnel.setType(TunnelType.ACTIVE);
                }
            }
        }
    }
    private void restoreStationConnections() {
        // Создаем карту станций по именам для быстрого доступа
        Map<String, Station> stationMap = new HashMap<>();
        for (Station station : this.stations) {
            stationMap.put(station.getName(), station);
        }

        // Восстанавливаем соединения
        for (Station station : this.stations) {
            for (Map.Entry<Direction, Station> entry : station.getConnections().entrySet()) {
                Direction dir = entry.getKey();
                Station connectedTo = entry.getValue();

                // Обновляем ссылку на соединенную станцию
                Station actualConnectedTo = stationMap.get(connectedTo.getName());
                if (actualConnectedTo != null) {
                    station.getConnections().put(dir, actualConnectedTo);
                }
            }
        }

        // Дополнительно проверяем соседей для восстановления TRANSFER станций
        for (Station station : this.stations) {
            station.updateType();
        }
    }




    public void saveWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();
            serializer.saveWorld(this, SAVE_FILE);

            MetroLogger.logInfo("World successfully saved");
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.saved"), false, 2000);
        } catch (IOException ex) {
            MetroLogger.logError("Failed to save world", ex);
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.not_saved") + ex.getMessage(), true, 2000);
        }
    }
    /**
     * Загружает мир из стандартного файла сохранения.
     * Делегирует загрузку статическому методу MetroSerializer.
     * @return true если загрузка успешна, false если файл не найден.
     */
    public boolean loadWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();
            // MetroSerializer.loadWorld теперь возвращает полностью сконструированный GameWorld
            GameWorld loadedWorld = serializer.loadWorld(SAVE_FILE);



            // Пример копирования полей (предполагая, что сетки уже инициализированы):
            this.width = loadedWorld.width;
            this.height = loadedWorld.height;

            this.worldGrid = loadedWorld.worldGrid; // Ссылка
            this.gameGrid = loadedWorld.gameGrid;
            this.gameplayGrid = loadedWorld.gameplayGrid;  // Ссылка

            this.stations = loadedWorld.stations;   // Ссылка

            this.tunnels = loadedWorld.tunnels;     // Ссылка
            this.stationLabels = loadedWorld.stationLabels;       // Ссылка
            this.rivers = loadedWorld.rivers;
            this.roundStationsEnabled = loadedWorld.roundStationsEnabled;
            this.customLineNames = new HashMap<>(loadedWorld.customLineNames);
            restoreStationConnections();


            if (this.screen != null) {
                this.screen.reinitializeControllers();

            }

            MetroLogger.logInfo("World successfully loaded");

            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.loaded"), false, 2000);
            return true;
        } catch (java.io.FileNotFoundException ex) {
            // Файл не найден - это нормально при первом запуске
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            MetroLogger.logError("Failed to load world", ex);
            UserInterfaceUtil.showTimedMessage(LngUtil.translatable("world.not_loaded") + ex.getMessage(), true, 2000);
        }
        return false;
    }



}
