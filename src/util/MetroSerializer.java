package util;

import game.core.GameTime;
import game.core.world.GameWorld;
import game.core.world.World;
import game.core.world.tiles.GameTile;
import game.core.world.tiles.WorldTile;
import game.objects.Label;
import game.objects.Station;
import game.objects.Tunnel;
import game.objects.enums.StationType;
import game.objects.enums.TunnelType;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MetroSerializer {
    private static final String VERSION = "1.0";
    private static final String SAVE_FOLDER = "saves";
    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                color.getRed(),
                color.getGreen(),
                color.getBlue());
    }
    public void saveWorld(GameWorld world, String filename) throws IOException {
        File saveDir = new File(SAVE_FOLDER);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }

        File saveFile = new File(SAVE_FOLDER + File.separator + filename);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                                new FileOutputStream(saveFile), StandardCharsets.UTF_8))) {

            // Записываем версию для обратной совместимости
            writer.write("version:" + VERSION + "\n");

            // Основные параметры мира
            writer.write("width:" + world.getWidth() + "\n");
            writer.write("height:" + world.getHeight() + "\n");
            writer.write("money:" + world.getMoney() + "\n");
            writer.write("roundStations:" + world.isRoundStationsEnabled() + "\n");

            // Время игры
            writer.write("gameTime:" + world.getGameTime().getCurrentTimeMillis() + "\n");


            //Мир
            writer.write("worldGrid:[\n");
            for (int y = 0; y < world.getHeight(); y++) {
                for (int x = 0; x < world.getWidth(); x++) {
                    WorldTile tile = world.getWorldTile(x, y);
                    writer.write(String.format(Locale.US,
                            "{x:%d,y:%d,perm:%.2f,color:%s}",
                            x, y,
                            tile.getPerm(),
                            colorToHex(tile.getCurrentColor())
                    ) + "\n");
                }
            }
            writer.write("]\n");

            // Добавляем сохранение gameGrid (если нужно)
            writer.write("gameGrid:[\n");
            for (int y = 0; y < world.getHeight(); y++) {
                for (int x = 0; x < world.getWidth(); x++) {
                    GameTile tile = world.getGameGrid()[x][y];
                    // Здесь можно сохранить необходимые данные из GameTile
                    writer.write(String.format("{x:%d,y:%d}", x, y) + "\n");
                }
            }
            writer.write("]\n");
            // Станции
            writer.write("stations:[\n");
            for (Station station : world.getStations()) {
                writer.write(String.format(
                        "{name:%s,x:%d,y:%d,color:%s,type:%s}",
                        escapeString(station.getName()),
                        station.getX(),
                        station.getY(),
                        colorToHex(station.getColor()),
                        station.getType().name()
                ) + "\n");
            }
            writer.write("]\n");

            // Туннели
            writer.write("tunnels:[\n");
            for (Tunnel tunnel : world.getTunnels()) {
                writer.write(String.format(
                        "{start:%s,end:%s,type:%s,length:%d}",
                        escapeString(tunnel.getStart().getName()),
                        escapeString(tunnel.getEnd().getName()),
                        tunnel.getType().name(),
                        tunnel.getLength()
                ) + "\n");
            }
            writer.write("]\n");

            // Метки
            writer.write("labels:[\n");
            for (Label label : world.getLabels()) {
                writer.write(String.format(
                        "{text:%s,x:%d,y:%d,parent:%s}",
                        escapeString(label.getText()),
                        label.getX(),
                        label.getY(),
                        escapeString(label.getParentStation().getName())
                ) + "\n");
            }
            writer.write("]\n");

            // Строительство станций
            writer.write("stationBuild:[\n");
            for (Map.Entry<Station, Long> entry : world.stationBuildStartTimes.entrySet()) {
                Station station = entry.getKey();
                writer.write(String.format(
                        "{station:%s,start:%d,duration:%d}",
                        escapeString(station.getName()),
                        entry.getValue(),
                        world.stationBuildDurations.get(station)
                ) + "\n");
            }
            writer.write("]\n");

            // Разрушение станций
            writer.write("stationDestroy:[\n");
            for (Map.Entry<Station, Long> entry : world.stationDestructionStartTimes.entrySet()) {
                Station station = entry.getKey();
                writer.write(String.format(
                        "{station:%s,start:%d,duration:%d}",
                        escapeString(station.getName()),
                        entry.getValue(),
                        world.stationDestructionDurations.get(station)
                ) + "\n");
            }
            writer.write("]\n");

            // Строительство туннелей
            writer.write("tunnelBuild:[\n");
            for (Map.Entry<Tunnel, Long> entry : world.tunnelBuildStartTimes.entrySet()) {
                Tunnel tunnel = entry.getKey();
                writer.write(String.format(
                        "{start:%s,end:%s,startTime:%d,duration:%d}",
                        escapeString(tunnel.getStart().getName()),
                        escapeString(tunnel.getEnd().getName()),
                        entry.getValue(),
                        world.tunnelBuildDurations.get(tunnel)
                ) + "\n");
            }
            writer.write("]\n");

            // Разрушение туннелей
            writer.write("tunnelDestroy:[\n");
            for (Map.Entry<Tunnel, Long> entry : world.tunnelDestructionStartTimes.entrySet()) {
                Tunnel tunnel = entry.getKey();
                writer.write(String.format(
                        "{start:%s,end:%s,startTime:%d,duration:%d}",
                        escapeString(tunnel.getStart().getName()),
                        escapeString(tunnel.getEnd().getName()),
                        entry.getValue(),
                        world.tunnelDestructionDurations.get(tunnel)
                ) + "\n");
            }
            writer.write("]\n");
        }
    }
    private String extractValue(String part, String expectedKey) {
        String[] keyValue = part.split(":", 2);
        if (keyValue.length < 2) {
            throw new IllegalArgumentException("Invalid key-value pair: " + part);
        }
        if (!keyValue[0].trim().equals(expectedKey)) {
            throw new IllegalArgumentException("Expected key '" + expectedKey + "' but found '" + keyValue[0] + "'");
        }
        return keyValue[1].trim();
    }
    public GameWorld loadWorld(String filename) throws IOException {
        File saveFile = new File(SAVE_FOLDER + File.separator + filename);
        if (!saveFile.exists()) {
            throw new FileNotFoundException("Save file not found: " + saveFile.getPath());
        }

        GameWorld world = new GameWorld();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                                new FileInputStream(saveFile), StandardCharsets.UTF_8))) {

            String version = null;
            String line;
            Map<String, Station> stationMap = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("version:")) {
                    version = line.substring("version:".length());
                    continue;
                }

                if (line.equals("worldGrid:[")) {
                    // Чтение worldGrid с улучшенной обработкой ошибок
                    world.initWorldGrid();
                    while (!(line = reader.readLine()).equals("]")) {
                        try {
                            line = line.trim();
                            if (!line.startsWith("{") || !line.endsWith("}")) continue;

                            String content = line.substring(1, line.length()-1);
                            // Используем регулярное выражение для корректного разбиения
                            String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                            if (parts.length < 4) {
                                MetroLogger.logInfo("Invalid worldGrid line format: " + line);
                                continue;
                            }

                            int x = Integer.parseInt(extractValue(parts[0], "x"));
                            int y = Integer.parseInt(extractValue(parts[1], "y"));
                            float perm = Float.parseFloat(extractValue(parts[2], "perm"));
                            String colorStr = extractValue(parts[3], "color");
                            Color color = parseColor(colorStr);

                            WorldTile tile = world.getWorldTile(x, y);
                            if (tile != null) {
                                tile.setPerm(perm);
                                tile.setBaseTileColor(color);
                            }
                        } catch (Exception e) {
                            MetroLogger.logError("Failed to parse worldGrid line: " + line, e);
                        }
                    }
                }
                else if (line.equals("gameGrid:[")) {
                    // Чтение gameGrid (если нужно)
                    world.initGameGrid();

                }

//                if (line.equals("worldGrid:[")) {
//                    // Чтение worldGrid
//                    while (!(line = reader.readLine()).equals("]")) {
//                        String[] parts = line.substring(1, line.length()-1).split(",");
//
//                        int x = Integer.parseInt(parts[0].split(":")[1]);
//                        int y = Integer.parseInt(parts[1].split(":")[1]);
//                        float perm = Float.parseFloat(parts[2].split(":")[1]);
//                        String colorStr = parts[3].split(":")[1];
//                        Color color = parseColor(colorStr);
//
//                        WorldTile tile = world.getWorldTile(x, y);
//                        tile.setPerm(perm);
//                        tile.setBaseTileColor(color);
//                    }
//                }
                else
                if (line.equals("stations:[")) {
                    // Чтение станций
                    while (!(line = reader.readLine()).equals("]")) {
                        Station station = parseStation(line, world);
                        stationMap.put(station.getName(), station);
                        world.getStations().add(station);
                    }
                }
                else if (line.equals("tunnels:[")) {
                    // Чтение туннелей
                    while (!(line = reader.readLine()).equals("]")) {
                        Tunnel tunnel = parseTunnel(line, world, stationMap);
                        world.getTunnels().add(tunnel);
                    }
                }
                else if (line.equals("labels:[")) {
                    // Чтение меток
                    while (!(line = reader.readLine()).equals("]")) {
                        Label label = parseLabel(line, world, stationMap);
                        world.getLabels().add(label);
                    }
                }
                else if (line.equals("stationBuild:[")) {
                    // Чтение данных о строительстве станций
                    while (!(line = reader.readLine()).equals("]")) {
                        parseConstructionData(line, world, stationMap, true, false);
                    }
                }
                else if (line.equals("stationDestroy:[")) {
                    // Чтение данных о разрушении станций
                    while (!(line = reader.readLine()).equals("]")) {
                        parseConstructionData(line, world, stationMap, true, true);
                    }
                }
                else if (line.equals("tunnelBuild:[")) {
                    // Чтение данных о строительстве туннелей
                    while (!(line = reader.readLine()).equals("]")) {
                        parseConstructionData(line, world, stationMap, false, false);
                    }
                }
                else if (line.equals("tunnelDestroy:[")) {
                    // Чтение данных о разрушении туннелей
                    while (!(line = reader.readLine()).equals("]")) {
                        parseConstructionData(line, world, stationMap, false, true);
                    }
                }
                else if (line.startsWith("width:")) {
                    world.setWidth(Integer.parseInt(line.substring("width:".length())));
                }
                else if (line.startsWith("height:")) {
                    world.setHeight(Integer.parseInt(line.substring("height:".length())));
                }
                else if (line.startsWith("money:")) {
                    world.setMoney(Integer.parseInt(line.substring("money:".length())));
                }
                else if (line.startsWith("roundStations:")) {
                    world.setRoundStationsEnabled(Boolean.parseBoolean(line.substring("roundStations:".length())));
                }
                else if (line.startsWith("gameTime:")) {
                    long time = Long.parseLong(line.substring("gameTime:".length()));
                    GameTime gameTime = new GameTime();
              //      gameTime.setCurrentTime(time);
                    world.gameTime = gameTime;
                }
            }
        }

        // Восстанавливаем связи между объектами
        world.initTransientFields();
        return world;
    }

    private Station parseStation(String line, GameWorld world) {
        // Пример строки: {name:"Station 1",x:10,y:20,color:rgb(255,0,0),type:REGULAR}
        String[] parts = line.substring(1, line.length()-1).split(",");
        String name = unescapeString(parts[0].split(":")[1]);
        int x = Integer.parseInt(parts[1].split(":")[1]);
        int y = Integer.parseInt(parts[2].split(":")[1]);

        // Парсим цвет
        String colorStr = parts[3].split(":")[1];
        Color color = parseColor(colorStr);

        StationType type = StationType.valueOf(parts[4].split(":")[1]);

        Station station = new Station(world, x, y, color, type);
        station.setName(name);
        return station;
    }

    private Tunnel parseTunnel(String line, GameWorld world, Map<String, Station> stationMap) {
        // Пример строки: {start:"Station 1",end:"Station 2",type:ACTIVE,length:5}
        String[] parts = line.substring(1, line.length()-1).split(",");
        Station start = stationMap.get(unescapeString(parts[0].split(":")[1]));
        Station end = stationMap.get(unescapeString(parts[1].split(":")[1]));
        TunnelType type = TunnelType.valueOf(parts[2].split(":")[1]);

        Tunnel tunnel = new Tunnel(world, start, end, type);
        return tunnel;
    }

    private Label parseLabel(String line, GameWorld world, Map<String, Station> stationMap) {
        // Пример строки: {text:"Label text",x:15,y:25,parent:"Station 1"}
        String[] parts = line.substring(1, line.length()-1).split(",");
        String text = unescapeString(parts[0].split(":")[1]);
        int x = Integer.parseInt(parts[1].split(":")[1]);
        int y = Integer.parseInt(parts[2].split(":")[1]);
        Station parent = stationMap.get(unescapeString(parts[3].split(":")[1]));

        return new Label(world, x, y, text, parent);
    }
    private Color parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#") && colorStr.length() == 7) {
                int r = Integer.parseInt(colorStr.substring(1, 3), 16);
                int g = Integer.parseInt(colorStr.substring(3, 5), 16);
                int b = Integer.parseInt(colorStr.substring(5, 7), 16);
                return new Color(r, g, b);
            }
            return Color.BLACK; // Значение по умолчанию
        } catch (Exception e) {
            MetroLogger.logError("Invalid color format: " + colorStr, e);
            return Color.BLACK;
        }
    }
//    private Color parseColor(String colorStr) {
//        // Парсим строку вида "rgb(255,0,0)"
//        if (colorStr.startsWith("rgb(") && colorStr.endsWith(")")) {
//            String rgbValues = colorStr.substring(4, colorStr.length() - 1);
//            String[] rgb = rgbValues.split(",");
//            int r = Integer.parseInt(rgb[0]);
//            int g = Integer.parseInt(rgb[1]);
//            int b = Integer.parseInt(rgb[2]);
//            return new Color(r, g, b);
//        }
//        return Color.BLACK; // Значение по умолчанию
//    }

    private void parseConstructionData(String line, GameWorld world,
            Map<String, Station> stationMap,
            boolean isStation, boolean isDestruction) {
        if (isStation) {
            // Для станций: {station:"Station 1",start:12345,duration:10000}
            String[] parts = line.substring(1, line.length()-1).split(",");
            Station station = stationMap.get(unescapeString(parts[0].split(":")[1]));
            long start = Long.parseLong(parts[1].split(":")[1]);
            long duration = Long.parseLong(parts[2].split(":")[1]);

            if (isDestruction) {
                world.stationBuildStartTimes.put(station, start);
                world.stationDestructionDurations.put(station, duration);
            } else {
                world.stationBuildStartTimes.put(station, start);
                world.stationBuildDurations.put(station, duration);
            }
        } else {
            // Для туннелей: {start:"Station 1",end:"Station 2",startTime:12345,duration:10000}
            String[] parts = line.substring(1, line.length()-1).split(",");
            Station start = stationMap.get(unescapeString(parts[0].split(":")[1]));
            Station end = stationMap.get(unescapeString(parts[1].split(":")[1]));
            long startTime = Long.parseLong(parts[2].split(":")[1]);
            long duration = Long.parseLong(parts[3].split(":")[1]);

            // Находим соответствующий туннель
            Tunnel tunnel = world.getTunnels().stream()
                                 .filter(t -> t.getStart() == start && t.getEnd() == end)
                                 .findFirst()
                                 .orElse(null);

            if (tunnel != null) {
                if (isDestruction) {
                    world.tunnelDestructionStartTimes.put(tunnel, startTime);
                    world.tunnelDestructionDurations.put(tunnel, duration);
                } else {
                    world.tunnelBuildStartTimes.put(tunnel, startTime);
                    world.tunnelBuildDurations.put(tunnel, duration);
                }
            }
        }
    }

    private String escapeString(String input) {
        return "\"" + input.replace("\"", "\\\"") + "\"";
    }

    private String unescapeString(String input) {
        return input.substring(1, input.length()-1).replace("\\\"", "\"");
    }
}
