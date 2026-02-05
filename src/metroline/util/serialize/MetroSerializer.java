package metroline.util.serialize;

import metroline.objects.enums.*;
import metroline.objects.gameobjects.*;
import metroline.core.world.GameWorld;
import metroline.core.world.tiles.GameTile;
import metroline.core.world.tiles.WorldTile;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Сериализатор для сохранения и загрузки состояния игрового мира.
 * Сохраняет мир в текстовом формате с тегами.
 */
public class MetroSerializer {

    private static final String VERSION = "1.2";
    private static final String SAVE_FOLDER = "saves";

    /**
     * Преобразует объект Color в строку формата HEX.
     *
     * @param color Цвет для преобразования.
     * @return Строка в формате "#RRGGBB".
     */
    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                color.getRed(),
                color.getGreen(),
                color.getBlue());
    }

    /**
     * Сохраняет состояние игрового мира в файл.
     *
     * @param world    Объект GameWorld для сохранения.
     * @param filename Имя файла для сохранения (без пути).
     * @throws IOException Если возникает ошибка при записи файла.
     */
    public void saveWorld(GameWorld world, String filename) throws IOException {
        File saveDir = new File(SAVE_FOLDER);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        File saveFile = new File(SAVE_FOLDER + File.separator + filename);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(saveFile), StandardCharsets.UTF_8))) {

            // Версия для обратной совместимости
            writer.write("version:" + VERSION + "\n");

            // Основные параметры мира
            writer.write("width:" + world.getWidth() + "\n");
            writer.write("height:" + world.getHeight() + "\n");
            writer.write("roundStations:" + world.isRoundStationsEnabled() + "\n");
            writer.write("worldColor:" + Integer.toHexString(metroline.core.world.tiles.WorldTile.getStaticBaseTileColor().getRGB() & 0xFFFFFF) + "\n");

            // Мир (worldGrid)
            writer.write("worldGrid:[\n");
            for (int y = 0; y < world.getHeight(); y++) {
                for (int x = 0; x < world.getWidth(); x++) {
                    WorldTile tile = world.getWorldTile(x, y);
                    writer.write(String.format(
                            "{x:%d,y:%d,isWater:%b}\n",
                            x, y,
                            tile.isWater()
                    ));
                }
            }
            writer.write("]\n");

            // Станции
            writer.write("stations:[\n");
            for (Station station : world.getStations()) {
                StringBuilder stationStr = new StringBuilder();
                stationStr.append(String.format(
                        "{id:%d,name:%s,x:%d,y:%d,color:%s,type:%s",
                        station.getUniqueId(),
                        ParsingUtils.escapeString(station.getName()),
                        station.getX(),
                        station.getY(),
                        colorToHex(station.getColor()),
                        station.getType().name()
                ));

                // Add isDepo flag if needed
                if (station.getType() == StationType.DEPO) {
                    stationStr.append(",isDepo:true");
                }

                // Add custom properties if they exist
                if (!station.getCustomProperties().isEmpty()) {
                    stationStr.append(",properties:");
                    stationStr.append(ParsingUtils.serializeProperties(station.getCustomProperties()));
                }

                stationStr.append("}\n");
                writer.write(stationStr.toString());
            }
            writer.write("]\n");

            // Соединения
            writer.write("connections:[\n");
            for (Station station : world.getStations()) {
                for (Map.Entry<Direction, Station> entry : station.getConnections().entrySet()) {
                    writer.write(String.format(
                            "{stationId:%d,direction:%s,connectedToId:%d}\n",
                            station.getUniqueId(),
                            entry.getKey().name(),
                            entry.getValue().getUniqueId()
                    ));
                }
            }
            writer.write("]\n");

            // Туннели
            writer.write("tunnels:[\n");
            for (Tunnel tunnel : world.getTunnels()) {
                writer.write(String.format(
                        "{startId:%d,endId:%d,type:%s,length:%d}\n",
                        tunnel.getStart().getUniqueId(),
                        tunnel.getEnd().getUniqueId(),
                        tunnel.getType().name(),
                        tunnel.getLength()
                ));
            }
            writer.write("]\n");

            // Точки пути (path points)
            writer.write("pathPoints:[\n");
            for (Tunnel tunnel : world.getTunnels()) {
                if (!tunnel.getPath().isEmpty()) {
                    writer.write(String.format(
                            "{tunnelStartId:%d,tunnelEndId:%d,points:[",
                            tunnel.getStart().getUniqueId(),
                            tunnel.getEnd().getUniqueId()
                    ));
                    for (int i = 0; i < tunnel.getPath().size(); i++) {
                        PathPoint point = tunnel.getPath().get(i);
                        writer.write(String.format("{x:%d,y:%d}", point.getX(), point.getY()));
                        if (i < tunnel.getPath().size() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("]}\n");
                }
            }
            writer.write("]\n");

            // Метки
            writer.write("labels:[\n");
            for (StationLabel stationLabel : world.getLabels()) {
                writer.write(String.format(
                        "{text:%s,x:%d,y:%d,parentStationId:%d}\n",
                        ParsingUtils.escapeString(stationLabel.getText()),
                        stationLabel.getX(),
                        stationLabel.getY(),
                        stationLabel.getParentGameObject().getUniqueId()
                ));
            }
            writer.write("]\n");

            // Игровая сетка (gameGrid)
            writer.write("gameGrid:[\n");
            GameTile[] gameGrid = world.getGameGrid();
            for (int i = 0; i < gameGrid.length; i++) {
                GameTile tile = gameGrid[i];
                if (tile != null) {
                    int x = i % world.getWidth();
                    int y = i / world.getWidth();
                    GameObject content = tile.getContent();
                    writer.write(String.format(
                            "{index:%d,x:%d,y:%d,content:%s}\n",
                            i, x, y,
                            content != null ? ParsingUtils.serializeGameObject(content) : "null"
                    ));
                }
            }
            writer.write("]\n");

            // Custom line names
            writer.write("lineNames:[\n");
            writer.write(ParsingUtils.serializeLineNames(world.getCustomLineNames()));
            writer.write("]\n");

            // Rivers
            writer.write("rivers:[\n");
            for (River river : world.getRivers()) {
                writer.write(String.format(
                        "{id:%d,name:%s,width:%.2f,color:%s,pointCount:%d}\n",
                        river.getUniqueId(),
                        ParsingUtils.escapeString(river.getName()),
                        river.getWidth(),
                        colorToHex(river.getRiverColor()),
                        river.getPoints().size()
                ));
            }
            writer.write("]\n");

// River Points with coordinates
            writer.write("riverPoints:[\n");
            for (River river : world.getRivers()) {
                for (RiverPoint point : river.getPoints()) {
                    writer.write(String.format(
                            "{riverId:%d,x:%d,y:%d,orderIndex:%d}\n",
                            river.getUniqueId(),
                            point.getX(),
                            point.getY(),
                            point.getOrderIndex()
                    ));
                }
            }
            writer.write("]\n");
        }
    }

    /**
     * Загружает состояние игрового мира из файла.
     *
     * @param filename Имя файла для загрузки (без пути).
     * @return Загруженный объект GameWorld.
     * @throws IOException Если возникает ошибка при чтении файла или файл не найден.
     */
    public GameWorld loadWorld(String filename) throws IOException {
        File saveFile = new File(SAVE_FOLDER + File.separator + filename);
        if (!saveFile.exists()) {
            throw new FileNotFoundException("Save file not found: " + saveFile.getPath());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(saveFile), StandardCharsets.UTF_8))) {
            return new GameWorld(reader);
        }
    }

    /**
     * Воссоздает игровой мир из BufferedReader.
     *
     * @param reader    BufferedReader с данными сохранения.
     * @param gameWorld Объект GameWorld для заполнения.
     * @throws IOException Если возникает ошибка при чтении.
     */
    public void recreateWorld(BufferedReader reader, GameWorld gameWorld) throws IOException {
        String line;
        Map<Long, Station> stationIdMap = new HashMap<>();

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty()) continue;

            if (line.startsWith("version:")) {
                continue;
            }
            if (line.startsWith("width:")) {
                gameWorld.setWidth(Short.parseShort(line.substring("width:".length())));
                continue;
            }
            if (line.startsWith("height:")) {
                gameWorld.setHeight(Short.parseShort(line.substring("height:".length())));
                if (gameWorld.getWidth() > 0 && gameWorld.getHeight() > 0) {
                    gameWorld.initWorldGrid();
                    gameWorld.initGameGrid();
                }
                continue;
            }
            if (line.startsWith("roundStations:")) {
                gameWorld.roundStationsEnabled = Boolean.parseBoolean(line.substring("roundStations:".length()));
                continue;
            }
            if (line.startsWith("worldColor:")) {
                String colorStr = line.substring("worldColor:".length());
                try {
                    int rgb = Integer.parseInt(colorStr, 16);
                    metroline.core.world.tiles.WorldTile.setStaticBaseTileColor(rgb);
                } catch (NumberFormatException e) {
                    System.err.println("[MetroSerializer::recreateWorld] Error parsing worldColor: " + colorStr);
                }
                continue;
            }
            // --- Обработка секций данных ---

            if (line.equals("worldGrid:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    line = line.trim();
                    if (!line.startsWith("{") || !line.endsWith("}")) continue;
                    String content = line.substring(1, line.length() - 1);
                    String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    try {
                        int x = Integer.parseInt(ParsingUtils.extractValue(parts[0], "x"));
                        int y = Integer.parseInt(ParsingUtils.extractValue(parts[1], "y"));
                        boolean isWater = Boolean.parseBoolean(ParsingUtils.extractValue(parts[2], "isWater"));

                        WorldTile tile = gameWorld.getWorldTile(x, y);
                        if (tile != null) {
                            tile.setWater(isWater);
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка при парсинге worldGrid: " + line);
                        e.printStackTrace();
                    }
                }
                continue;
            }

            if (line.equals("stations:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    Station station = ParsingUtils.parseStation(line, gameWorld);
                    if (station != null) {
                        stationIdMap.put(station.getUniqueId(), station);
                        gameWorld.stations.add(station);
                    }
                }
                continue;
            }

            if (line.equals("connections:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConnection(line, gameWorld, stationIdMap);
                }

                continue;
            }

            if (line.equals("tunnels:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    Tunnel tunnel = ParsingUtils.parseTunnel(line, gameWorld, stationIdMap);
                    if (tunnel != null) {
                        gameWorld.tunnels.add(tunnel);
                    }
                }
                continue;
            }

            if (line.equals("pathPoints:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parsePathPoints(line, gameWorld, stationIdMap);
                }
                continue;
            }

            if (line.equals("labels:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    StationLabel stationLabel = ParsingUtils.parseLabel(line, gameWorld, stationIdMap);
                    if (stationLabel != null) {
                        gameWorld.stationLabels.add(stationLabel);
                        int index = stationLabel.getX() + stationLabel.getY() * gameWorld.getWidth();
                        if (index < gameWorld.gameGrid.length) {
                            gameWorld.gameGrid[index].setContent(stationLabel);
                        }
                    }
                }
                continue;
            }
            if (line.equals("rivers:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    River river = ParsingUtils.parseRiver(line, gameWorld);
                    if (river != null) {
                        gameWorld.rivers.add(river);
                    }
                }
                continue;
            }

            if (line.equals("riverPoints:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseRiverPoint(line, gameWorld);
                }
                continue;
            }
            if (line.equals("lineNames:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    ParsingUtils.parseLineName(line, gameWorld);
                }
                continue;
            }
            if (line.equals("gameGrid:[")) {
                while (!(line = reader.readLine()).equals("]")) {
                    line = line.trim();
                    if (!line.startsWith("{") || !line.endsWith("}")) continue;
                    String content = line.substring(1, line.length() - 1);
                    String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length < 4) {
                        System.err.println("Недостаточно частей при парсинге gameGrid: " + line);
                        continue;
                    }

                    try {
                        int index = Integer.parseInt(ParsingUtils.extractValue(parts[0], "index"));
                        String contentStr = ParsingUtils.extractValue(parts[3], "content");

                        if (!contentStr.equals("null")) {
                            GameObject obj = ParsingUtils.parseGameObject(contentStr, gameWorld, stationIdMap);
                            if (obj != null && gameWorld.gameGrid != null && index < gameWorld.gameGrid.length) {
                                gameWorld.gameGrid[index].setContent(obj);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка при парсинге gameGrid: " + line);
                        e.printStackTrace();
                    }
                }
                continue;
            }
        }
    }
}