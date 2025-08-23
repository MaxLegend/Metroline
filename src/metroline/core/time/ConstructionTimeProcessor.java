package metroline.core.time;

import metroline.core.world.GameWorld;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TunnelType;
import metroline.objects.gameobjects.Station;
import metroline.objects.gameobjects.Tunnel;

import java.util.HashMap;
import java.util.Map;

import static metroline.objects.gameobjects.GameConstants.STATION_DESTROY_TIME;
import static metroline.objects.gameobjects.GameConstants.TUNNEL_BUILD_TIME;
import static metroline.objects.gameobjects.GameConstants.STATION_BUILD_TIME;
import static metroline.objects.gameobjects.GameConstants.TUNNEL_DESTROY_TIME;

public class ConstructionTimeProcessor {



    public GameWorld world;

    private final GameTime gameTime;

    public Map<Station, Long> stationDestructionStartTimes = new HashMap<>();
    public Map<Station, Long> stationDestructionDurations = new HashMap<>();
    public Map<Tunnel, Long> tunnelDestructionStartTimes = new HashMap<>();
    public Map<Tunnel, Long> tunnelDestructionDurations = new HashMap<>();

    public Map<Station, Long> stationBuildStartTimes = new HashMap<>();
    public Map<Station, Long> stationBuildDurations = new HashMap<>();
    public Map<Tunnel, Long> tunnelBuildStartTimes = new HashMap<>();
    public Map<Tunnel, Long> tunnelBuildDurations = new HashMap<>();

        public ConstructionTimeProcessor(GameTime gameTime, GameWorld world) {
            this.gameTime = gameTime;
            this.world = world;
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

        public void registerStationConstruction(Station station) {
            if (!stationBuildStartTimes.containsKey(station)) {
                long startTime = gameTime.getCurrentTimeMillis();
                stationBuildStartTimes.put(station, startTime);
                stationBuildDurations.put(station, STATION_BUILD_TIME);
            }
        }

        public void registerTunnelConstruction(Tunnel tunnel) {
            if (tunnelBuildStartTimes == null) {
                tunnelBuildStartTimes = new HashMap<>();
            }
            if (tunnelBuildDurations == null) {
                tunnelBuildDurations = new HashMap<>();
            }

            long startTime = gameTime.getCurrentTimeMillis();
            tunnelBuildStartTimes.put(tunnel, startTime);
            tunnelBuildDurations.put(tunnel, TUNNEL_BUILD_TIME);
        }

        public void registerStationDestruction(Station station) {
            stationDestructionStartTimes.put(station, gameTime.getCurrentTimeMillis());
            stationDestructionDurations.put(station, STATION_DESTROY_TIME);
        }

        public void registerTunnelDestruction(Tunnel tunnel) {
            tunnelDestructionStartTimes.put(tunnel, gameTime.getCurrentTimeMillis());
            tunnelDestructionDurations.put(tunnel, TUNNEL_DESTROY_TIME);
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
                return 0f;
            }

            if (station.getType() == StationType.BUILDING && stationBuildStartTimes.containsKey(station)) {
                return calculateProgress(stationBuildStartTimes.get(station), stationBuildDurations.get(station));
            } else if (station.getType() == StationType.DESTROYED && stationDestructionStartTimes.containsKey(station)) {
                return 1.0f - calculateProgress(stationDestructionStartTimes.get(station), stationDestructionDurations.get(station));
            }
            return 0f;
        }

        public float getTunnelConstructionProgress(Tunnel tunnel) {
            if (!tunnelBuildStartTimes.containsKey(tunnel)) {
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

        // Геттеры и сеттеры для времени строительства/разрушения

    public void setTunnelDestructionStartTimes(Map<Tunnel, Long> tunnelDestructionStartTimes) {
        this.tunnelDestructionStartTimes = tunnelDestructionStartTimes;
    }

    public void setTunnelDestructionDurations(Map<Tunnel, Long> tunnelDestructionDurations) {
        this.tunnelDestructionDurations = tunnelDestructionDurations;
    }

    public void setTunnelBuildDurations(Map<Tunnel, Long> tunnelBuildDurations) {
        this.tunnelBuildDurations = tunnelBuildDurations;
    }

    public void setTunnelBuildStartTimes(Map<Tunnel, Long> tunnelBuildStartTimes) {
        this.tunnelBuildStartTimes = tunnelBuildStartTimes;
    }

    public void setStationDestructionDurations(Map<Station, Long> stationDestructionDurations) {
        this.stationDestructionDurations = stationDestructionDurations;
    }

    public void setStationDestructionStartTimes(Map<Station, Long> stationDestructionStartTimes) {
        this.stationDestructionStartTimes = stationDestructionStartTimes;
    }

    public GameTime getGameTime() {
        return gameTime;
    }

    public void setStationBuildDurations(Map<Station, Long> stationBuildDurations) {
        this.stationBuildDurations = stationBuildDurations;
    }

    public void setStationBuildStartTimes(Map<Station, Long> stationBuildStartTimes) {
        this.stationBuildStartTimes = stationBuildStartTimes;
    }

    public Map<Tunnel, Long> getTunnelDestructionStartTimes() {
        return tunnelDestructionStartTimes;
    }

    public Map<Tunnel, Long> getTunnelDestructionDurations() {
        return tunnelDestructionDurations;
    }

    public Map<Tunnel, Long> getTunnelBuildStartTimes() {
        return tunnelBuildStartTimes;
    }

    public Map<Tunnel, Long> getTunnelBuildDurations() {
        return tunnelBuildDurations;
    }

    public Map<Station, Long> getStationDestructionStartTimes() {
        return stationDestructionStartTimes;
    }

    public Map<Station, Long> getStationDestructionDurations() {
        return stationDestructionDurations;
    }

    public Map<Station, Long> getStationBuildStartTimes() {
        return stationBuildStartTimes;
    }

    public Map<Station, Long> getStationBuildDurations() {
        return stationBuildDurations;
    }

    public long getstationDestroyTime() {

            return STATION_DESTROY_TIME;
        }

        public void setStationDestroyTime(long stationDestroyTime) {
            STATION_DESTROY_TIME = stationDestroyTime;
        }

        public long getTunnelDestroyTime() {
            return TUNNEL_DESTROY_TIME;
        }

        public void setTunnelDestroyTime(long tunnelDestroyTime) {
            TUNNEL_DESTROY_TIME = tunnelDestroyTime;
        }

        public long getStationBuildTime() {
            return STATION_BUILD_TIME;
        }

        public void setStationBuildTime(long stationBuildTime) {
            STATION_BUILD_TIME = stationBuildTime;
        }

        public long getTunnelBuildTime() {
            return TUNNEL_BUILD_TIME;
        }

        public void setTunnelBuildTime(long tunnelBuildTime) {
            TUNNEL_BUILD_TIME = tunnelBuildTime;
        }
    }
