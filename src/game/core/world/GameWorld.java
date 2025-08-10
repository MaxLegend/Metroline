package game.core.world;

import game.core.GameObject;
import game.core.GameTime;
import game.core.world.tiles.GameTile;
import game.core.world.tiles.GameTileBig;
import game.core.world.tiles.WorldTile;
import game.objects.Label;
import game.objects.PathPoint;
import game.objects.Station;
import game.objects.Tunnel;
import game.objects.enums.Direction;
import game.objects.enums.StationType;
import game.objects.enums.TunnelType;
import screens.MainFrame;
import screens.WorldGameScreen;
import screens.WorldSandboxScreen;
import util.LngUtil;
import util.MessageUtil;
import util.MetroLogger;
import util.MetroSerializer;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

import static game.input.GameClickHandler.TUNNEL_COST_PER_SEGMENT;

public class GameWorld extends World {
    private transient MainFrame mainFrame;
    private static String SAVE_FILE = "game_save.metro";
    public int money;

    private long stationDestroyTime = 100000; // Время разрушения станции
    private long tunnelDestroyTime = 100000;
    private long stationBuildTime = 100000;
    private long tunnelBuildTime = 100000;

    public transient Map<Station, Long> stationDestructionStartTimes = new HashMap<>();
    public transient Map<Station, Long> stationDestructionDurations = new HashMap<>();
    public transient Map<Tunnel, Long> tunnelDestructionStartTimes = new HashMap<>();
    public transient Map<Tunnel, Long> tunnelDestructionDurations = new HashMap<>();

    public transient Map<Station, Long> stationBuildStartTimes = new HashMap<>();
    public transient Map<Station, Long> stationBuildDurations = new HashMap<>();
    public transient Map<Tunnel, Long> tunnelBuildStartTimes = new HashMap<>();
    public transient Map<Tunnel, Long> tunnelBuildDurations = new HashMap<>();

    public GameWorld() {
        super();
        initTransientFields();
    }

    public GameWorld(int width, int height, boolean hasOrganicPatches, boolean hasRivers, Color worldColor, int money) {
        super(null, width, height, hasOrganicPatches,hasRivers,worldColor, SAVE_FILE);
        this.mainFrame = MainFrame.getInstance();
        this.money = money;
        initTransientFields();
    }
    public void initTransientFields() {
        if (stationDestructionStartTimes == null) {
            stationDestructionStartTimes = new HashMap<>();
        }
        if (stationDestructionDurations == null) {
            stationDestructionDurations = new HashMap<>();
        }
        if (tunnelDestructionStartTimes == null) {
            tunnelDestructionStartTimes = new HashMap<>();
        }
        if (tunnelDestructionDurations == null) {
            tunnelDestructionDurations = new HashMap<>();
        }
        if (stationBuildStartTimes == null) {
            stationBuildStartTimes = new HashMap<>();
        }
        if (stationBuildDurations == null) {
            stationBuildDurations = new HashMap<>();
        }
        if (tunnelBuildStartTimes == null) {
            tunnelBuildStartTimes = new HashMap<>();
        }
        if (tunnelBuildDurations == null) {
            tunnelBuildDurations = new HashMap<>();
        }
    }
    public void startDestroyingTunnel(Tunnel tunnel) {
        if (tunnel.getType() != TunnelType.ACTIVE) {
            MetroLogger.logWarning("Cannot destroy tunnel of type " + tunnel.getType());
            return;
        }

        tunnel.setType(TunnelType.DESTROYED);
        long startTime = gameTime.getCurrentTimeMillis();
        tunnelDestructionStartTimes.put(tunnel, startTime);
        tunnelDestructionDurations.put(tunnel, tunnelDestroyTime);

        MetroLogger.logInfo("Tunnel destruction started");
    }
    public void startDestroyingStation(Station station) {
        if (station.getType() != StationType.REGULAR &&
                station.getType() != StationType.TRANSFER &&
                station.getType() != StationType.TERMINAL &&
                station.getType() != StationType.CLOSED &&
                station.getType() != StationType.TRANSIT) {
            MetroLogger.logWarning("Cannot destroy station of type " + station.getType());
            return;
        }

        station.setType(StationType.DESTROYED);
        long startTime = gameTime.getCurrentTimeMillis();
        stationDestructionStartTimes.put(station, startTime);
        stationDestructionDurations.put(station, stationDestroyTime);

        MetroLogger.logInfo("Station destruction started: " + station.getName());
    }

    @Override
    public void addStation(Station station) {
        super.addStation(station);

        if (station.getType() == StationType.BUILDING) {
            // Проверяем, не добавлена ли уже станция
            if (!stationBuildStartTimes.containsKey(station)) {
                long startTime = gameTime.getCurrentTimeMillis();
                stationBuildStartTimes.put(station, startTime);
                stationBuildDurations.put(station, stationBuildTime);

                if(startTime % 2000 == 0) MetroLogger.logInfo("Station construction REGISTERED: " + station.getName() +
                        " | Start: " + startTime +
                        " | Duration: " + stationBuildTime + "ms" +
                        " | Expected finish: " + (startTime + stationBuildTime));
            } else {
                MetroLogger.logWarning("Station already in construction: " + station.getName());
            }
        }
    }

    @Override
    public void addTunnel(Tunnel tunnel) {
        super.addTunnel(tunnel);
        if (tunnelBuildStartTimes == null) {
            tunnelBuildStartTimes = new HashMap<>();
        }
        if (tunnelBuildDurations == null) {
            tunnelBuildDurations = new HashMap<>();
        }
        if (tunnel.getType() == TunnelType.BUILDING) {
//            long startTime = gameTime.getCurrentTimeMillis();
//            tunnelBuildStartTimes.put(tunnel, startTime);
//            tunnelBuildDurations.put(tunnel, tunnelBuildTime);
            long startTime = gameTime.getCurrentTimeMillis();
            tunnelBuildStartTimes.put(tunnel, startTime);
            tunnelBuildDurations.put(tunnel, tunnelBuildTime);

            // Устанавливаем стоимость строительства в зависимости от длины
            int lengthBasedCost = tunnel.getLength() * 10; // Например, 10 за сегмент
            tunnelBuildTime = Math.max(50000, lengthBasedCost * 1000); // Минимум 50 секунд
        }
    }
    private float calculateProgress(long startTime, long duration) {
        long currentTime = gameTime.getCurrentTimeMillis();
        if (startTime > currentTime) {
            return 0f;
        }
        return Math.min(1.0f, (float)(currentTime - startTime) / duration);
    }
    public float getStationConstructionProgress(Station station) {
        if (!stationBuildStartTimes.containsKey(station)) {
//            MetroLogger.logInfo("Station " + station.getName() +
//                    " is not registered for construction!" +
//                    " Current type: " + station.getType() +
//                    " | Expected: BUILDING");
            return 0f; // Возвращаем 0 вместо 1, чтобы было заметно
        }


        if (station.getType() == StationType.BUILDING && stationBuildStartTimes.containsKey(station)) {
            //    MetroLogger.logInfo("Construction PROGRESS: " + station.getName() + " - " + (progress * 100) + "%");
            return calculateProgress(stationBuildStartTimes.get(station), stationBuildDurations.get(station));
        } else if (station.getType() == StationType.DESTROYED && stationDestructionStartTimes.containsKey(station)) {
         //   MetroLogger.logInfo("Destroyed PROGRESS: " + station.getName() + " - " + (progress) + "%");
            return 1.0f - calculateProgress(stationDestructionStartTimes.get(station), stationDestructionDurations.get(station));
        }
        return 0f;
    }

    public void debugConstructionState() {
        MetroLogger.logInfo("=== CONSTRUCTION STATE DEBUG ===");
        MetroLogger.logInfo("Current game time: " + gameTime.getCurrentTimeMillis());

        MetroLogger.logInfo("Stations in construction (" + stationBuildStartTimes.size() + "):");
        stationBuildStartTimes.forEach((station, time) -> {
            MetroLogger.logInfo(" - " + station.getName() +
                    " | Start: " + time +
                    " | Type: " + station.getType());
        });

        MetroLogger.logInfo("Tunnels in construction (" + tunnelBuildStartTimes.size() + "):");
        tunnelBuildStartTimes.forEach((tunnel, time) -> {
            MetroLogger.logInfo(" - " + tunnel.getStart().getName() + " -> " +
                    tunnel.getEnd().getName() +
                    " | Start: " + time +
                    " | Type: " + tunnel.getType());
        });
    }
    public float getTunnelConstructionProgress(Tunnel tunnel) {
        if (!tunnelBuildStartTimes.containsKey(tunnel)) {
//            MetroLogger.logInfo("Tunnel between " +
//                    tunnel.getStart().getName() + " and " +
//                    tunnel.getEnd().getName() + " is not under construction");
            return 1.0f;
        }

        if (tunnel.getType() == TunnelType.BUILDING && tunnelBuildStartTimes.containsKey(tunnel)) {
            return calculateProgress(tunnelBuildStartTimes.get(tunnel),
                    tunnelBuildDurations.get(tunnel));
        } else if (tunnel.getType() == TunnelType.DESTROYED && tunnelDestructionStartTimes.containsKey(tunnel)) {
            return 1.0f - calculateProgress(tunnelDestructionStartTimes.get(tunnel),
                    tunnelDestructionDurations.get(tunnel));
        }
        return 1.0f;
    }


    public GameTime getGameTime() {
        return gameTime;
    }
    @Override
    public World getWorld() {
        return this;
    }
    public int getMoney() {
        return money;
    }

    public boolean canAfford(int amount) {
        return money >= amount;
    }

    public boolean addMoney(int amount) {
        if (amount < 0 && !canAfford(-amount)) {
            return false;
        }
        money += amount;

        if (MainFrame.getInstance() != null) {
            MainFrame.getInstance().updateMoneyDisplay(money);
        }

        return true;
    }

    public void setMoney(int amount) {
        this.money = amount;
        if (MainFrame.getInstance() != null) {
            MainFrame.getInstance().updateMoneyDisplay(money);
        }
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

                    int tunnelCost = tunnel.getLength() * TUNNEL_COST_PER_SEGMENT;
                    if (canAfford(tunnelCost)) {
                        addMoney(-tunnelCost);
                        tunnel.setType(TunnelType.BUILDING);
                        addTunnel(tunnel); // Обновит время строительства
                        canStartBuilding = true;
                    }
                }

                // Туннель становится ACTIVE только когда обе станции построены
                // (не PLANNED и не BUILDING)
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

public void initWorldGrid() {
    this.worldGrid = new WorldTile[width][height];

    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            worldGrid[x][y] = new WorldTile(x,y, 0);
        }
    }
}
    public void initGameGrid() {
        this.gameGrid = new GameTile[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                gameGrid[x][y] = new GameTile(x,y);
            }
        }
    }
    public void saveWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();
            serializer.saveWorld(this, SAVE_FILE);

            MetroLogger.logInfo("World successfully saved");
            MessageUtil.showTimedMessage(LngUtil.translatable("world.saved"), false, 2000);
        } catch (IOException ex) {
            MetroLogger.logError("Failed to save world", ex);
            MessageUtil.showTimedMessage(LngUtil.translatable("world.not_saved") + ex.getMessage(), true, 2000);
        }
    }

    public boolean loadWorld() {
        try {
            MetroSerializer serializer = new MetroSerializer();
            GameWorld loaded = serializer.loadWorld(SAVE_FILE);



            // Копируем данные из загруженного мира
            this.width = loaded.width;
            this.height = loaded.height;
            this.money = loaded.money;
            this.worldGrid = loaded.worldGrid;
            this.gameGrid = loaded.gameGrid;
            this.stations = loaded.stations;
            this.tunnels = loaded.tunnels;
            this.labels = loaded.labels;
            this.gameTime = loaded.gameTime;
            this.roundStationsEnabled = loaded.roundStationsEnabled;

            // Восстанавливаем временные данные
            this.stationBuildStartTimes = loaded.stationBuildStartTimes;
            this.stationBuildDurations = loaded.stationBuildDurations;
            this.tunnelBuildStartTimes = loaded.tunnelBuildStartTimes;
            this.tunnelBuildDurations = loaded.tunnelBuildDurations;
            this.stationDestructionStartTimes = loaded.stationDestructionStartTimes;
            this.stationDestructionDurations = loaded.stationDestructionDurations;
            this.tunnelDestructionStartTimes = loaded.tunnelDestructionStartTimes;
            this.tunnelDestructionDurations = loaded.tunnelDestructionDurations;

            // Запускаем игровое время
            if (this.gameTime != null) {
                this.gameTime.start();
            } else {
                this.gameTime = new GameTime();
                this.gameTime.start();
            }

            if (this.screen != null) {
                this.screen.reinitializeControllers();
            }

            MetroLogger.logInfo("World successfully loaded");
            MessageUtil.showTimedMessage(LngUtil.translatable("world.loaded"), false, 2000);
            return true;
        } catch (FileNotFoundException ex) {
            // Файл не найден - это нормально при первом запуске
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            MetroLogger.logError("Failed to load world", ex);
            MessageUtil.showTimedMessage(LngUtil.translatable("world.not_loaded") + ex.getMessage(), true, 2000);
        }
        return false;
    }

}
