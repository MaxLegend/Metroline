package metroline.util;

import metroline.objects.enums.*;
import metroline.objects.gameobjects.*;
import metroline.core.time.GameTime;
import metroline.core.world.GameWorld;
import metroline.core.world.tiles.GameTile;
import metroline.core.world.tiles.WorldTile;
import metroline.objects.gameobjects.Label;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


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
                            "{x:%d,y:%d,perm:%.2f,isWater:%b,abilityPay:%.2f,passengerCount:%d,color:%s}",
                            x, y,
                            tile.getPerm(),
                            tile.isWater(),
                            tile.getAbilityPay(),
                            tile.getPassengerCount(),
                            colorToHex(tile.getCurrentColor())
                    ) + "\n");
                }
            }
            writer.write("]\n");

            // Станции
            writer.write("stations:[\n");
            for (Station station : world.getStations()) {
                writer.write(String.format(
                        "{id:%d,name:%s,x:%d,y:%d,color:%s,type:%s}",
                        station.getUniqueId(),
                        escapeString(station.getName()),
                        station.getX(),
                        station.getY(),
                        colorToHex(station.getColor()),
                        station.getType().name()
                ) + "\n");
            }
            writer.write("]\n");

            writer.write("connections:[\n");
            for (Station station : world.getStations()) {
                for (Map.Entry<Direction, Station> entry : station.getConnections().entrySet()) {
                    writer.write(String.format(
                            "{stationId:%d,direction:%s,connectedToId:%d}",
                            station.getUniqueId(),
                            entry.getKey().name(),
                            entry.getValue().getUniqueId()
                    ) + "\n");
                }
            }
            writer.write("]\n");

            // Туннели
            writer.write("tunnels:[\n");
            for (Tunnel tunnel : world.getTunnels()) {
                writer.write(String.format(
                        "{startId:%d,endId:%d,type:%s,length:%d}",
                        tunnel.getStart().getUniqueId(),
                        tunnel.getEnd().getUniqueId(),
                        tunnel.getType().name(),
                        tunnel.getLength()
                ) + "\n");
            }
            writer.write("]\n");

            //gameplay units
            writer.write("gameplay_units:[\n");
            for (GameplayUnits gUnits : world.getGameplayUnits()) {
                writer.write(String.format(
                        "{id:%d,name:%s,x:%d,y:%d,type:%s}",
                        gUnits.getUniqueId(),
                        escapeString(gUnits.getName()),
                        gUnits.getX(),
                        gUnits.getY(),
                        gUnits.getType().name()
                ) + "\n");
            }
            writer.write("]\n");

            //path points
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
            for (Label label : world.getLabels()) {
                writer.write(String.format(
                        "{text:%s,x:%d,y:%d,parentStationId:%d}",
                        escapeString(label.getText()),
                        label.getX(),
                        label.getY(),
                        label.getParentGameObject().getUniqueId()
                ) + "\n");
            }
            writer.write("]\n");


            // Строительство станций
            writer.write("stationBuild:[\n");
            for (Map.Entry<Station, Long> entry : world.stationBuildStartTimes.entrySet()) {
                Station station = entry.getKey();
                writer.write(String.format(
                        "{stationId:%d,start:%d,duration:%d}",
                        station.getUniqueId(),
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
                        "{stationId:%d,start:%d,duration:%d}",
                        station.getUniqueId(),
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
                        "{startId:%d,endId:%d,startTime:%d,duration:%d}",
                        tunnel.getStart().getUniqueId(),
                        tunnel.getEnd().getUniqueId(),
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
                        "{startId:%d,endId:%d,startTime:%d,duration:%d}",
                        tunnel.getStart().getUniqueId(),
                        tunnel.getEnd().getUniqueId(),
                        entry.getValue(),
                        world.tunnelDestructionDurations.get(tunnel)
                ) + "\n");
            }
            writer.write("]\n");

            writer.write("]\n");
            writer.write("gameGrid:[\n");
            for (int y = 0; y < world.getHeight(); y++) {
                for (int x = 0; x < world.getWidth(); x++) {
                    GameTile tile = world.getGameGrid()[x][y];
                    GameObject content = tile.getContent();

                    writer.write(String.format(
                            "{x:%d,y:%d,content:%s}",
                            x, y,
                            content != null ? serializeGameObject(content) : "null"
                    ) + "\n");
                }
            }
            writer.write("]\n");
        }
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
            Map<Long, Station> stationIdMap = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("version:")) {
                    version = line.substring("version:".length());
                    continue;
                }

                if (line.startsWith("width:")) {
                    world.setWidth(Integer.parseInt(line.substring("width:".length())));
                }
                else if (line.startsWith("height:")) {
                    world.setHeight(Integer.parseInt(line.substring("height:".length())));
                }

                else if (line.startsWith("money:")) {
                    world.setMoney((int) Float.parseFloat(line.substring("money:".length())));
                }
                else if (line.startsWith("roundStations:")) {
                    world.setRoundStationsEnabled(Boolean.parseBoolean(line.substring("roundStations:".length())));
                }
                else if (line.startsWith("gameTime:")) {
                    long time = Long.parseLong(line.substring("gameTime:".length()));
                    GameTime gameTime = new GameTime();
              //      gameTime.setCurrentTime(time);
                    world.gameTime = gameTime;
                }  else if (line.equals("worldGrid:[")) {
                    // Чтение worldGrid с улучшенной обработкой ошибок
                    world.initWorldGrid();

                    while (!(line = reader.readLine()).equals("]")) {

                            line = line.trim();
                            if (!line.startsWith("{") || !line.endsWith("}")) continue;

                            String content = line.substring(1, line.length()-1);
                            // Используем регулярное выражение для корректного разбиения
                            String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");


                            int x = Integer.parseInt(extractValue(parts[0], "x"));
                            int y = Integer.parseInt(extractValue(parts[1], "y"));
                            float perm = Float.parseFloat(extractValue(parts[2], "perm"));
                            boolean isWater = Boolean.parseBoolean(extractValue(parts[3], "isWater"));
                            float abilityPay = Float.parseFloat(extractValue(parts[4], "abilityPay"));
                            float passengerCount = Float.parseFloat(extractValue(parts[5], "passengerCount"));
                            Color color = parseColor(extractValue(parts[6], "color"));

                            WorldTile tile = world.getWorldTile(x, y);
                            if (tile != null) {
                                tile.setPerm(perm);
                                tile.setBaseTileColor(color);
                                tile.setAbilityPay(abilityPay);
                                tile.setWater(isWater);
                                tile.setPassengerCount((int) passengerCount);

                            }

                    }
                }

                else
                if (line.equals("stations:[")) {

                    // Чтение станций
                    while (!(line = reader.readLine()).equals("]")) {
                        Station station = parseStation(line, world);
                        stationMap.put(station.getName(), station);
                        stationIdMap.put(station.getUniqueId(), station);
                        if (station != null) {
                            world.getStations().add(station);

                        }
                    }

                }
                else if (line.equals("connections:[")) {
                    // Чтение соединений
                    while (!(line = reader.readLine()).equals("]")) {
                        parseConnection(line, world, stationIdMap);
                    }
                }
                else if (line.equals("tunnels:[")) {
                    // Чтение туннелей
                    while (!(line = reader.readLine()).equals("]")) {
                        Tunnel tunnel = parseTunnel(line, world, stationIdMap);
                        if (tunnel != null) {
                            world.getTunnels().add(tunnel);
                        }
                    }
                }
                else if (line.equals("gameplay_units:[")) {

                    // Чтение меток
                    while (!(line = reader.readLine()).equals("]")) {
                        GameplayUnits gUnits = parseGameplayUnit(line, world);
                        if (gUnits != null) {
                            world.getGameplayUnits().add(gUnits);
                        }
                    }

                }

                else if (line.equals("pathPoints:[")) {
                    // Чтение путевых точек для туннелей
                    while (!(line = reader.readLine()).equals("]")) {
                        parsePathPoints(line, world, stationIdMap);
                    }
                }
                else if (line.equals("labels:[")) {

                    // Чтение меток
                    while (!(line = reader.readLine()).equals("]")) {
                        Label label = parseLabel(line, world, stationMap, stationIdMap);
                        if (label != null) {
                            world.getLabels().add(label);
                        }
                    }

                }
                else if (line.equals("stationBuild:[")) {
                    // Чтение данных о строительстве станций
                    while (!(line = reader.readLine()).equals("]")) {
                        parseConstructionData(line, world, stationIdMap, true, false);
                    }
                }
                else if (line.equals("stationDestroy:[")) {
                    // Чтение данных о разрушении станций
                    while (!(line = reader.readLine()).equals("]")) {
                        parseConstructionData(line, world, stationIdMap, true, true);
                    }
                }
                else if (line.equals("tunnelBuild:[")) {
                    // Чтение данных о строительстве туннелей
                    while (!(line = reader.readLine()).equals("]")) {
                        parseConstructionData(line, world, stationIdMap, false, false);
                    }
                }
                else if (line.equals("tunnelDestroy:[")) {
                    // Чтение данных о разрушении туннелей
                    while (!(line = reader.readLine()).equals("]")) {
                        parseConstructionData(line, world, stationIdMap, false, true);
                    }
                }
                else if (line.equals("gameGrid:[")) {
                    world.initGameGrid();
                    while (!(line = reader.readLine()).equals("]")) {

                            line = line.trim();
                            if (!line.startsWith("{") || !line.endsWith("}")) continue;

                            String content = line.substring(1, line.length()-1);
                            String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                            int x = Integer.parseInt(extractValue(parts[0], "x"));
                            int y = Integer.parseInt(extractValue(parts[1], "y"));
                            String contentStr = extractValue(parts[2], "content");

                            if (!contentStr.equals("null")) {
                                GameObject obj = parseGameObject(contentStr, world, stationIdMap);
                                world.getGameGrid()[x][y].setContent(obj);
                            }
                    }
                }
            }
        }

        // Восстанавливаем связи между объектами
        world.initTransientFields();
        return world;
    }


    /*****************************************
     * PARSING AND AUXILIARY METHODS SECTIONS
     *****************************************/
    private GameObject parseGameObject(String contentStr, GameWorld world, Map<Long, Station> stationIdMap) {
        String[] parts = contentStr.split(":");
        String type = parts[0];
        String value = parts[1];
//        String[] parts = contentStr.split(":");
//        String type = parts[0];
//        String value = unescapeString(parts[1]);

        return switch (type) {
            case "station" -> {
                long stationId = Long.parseLong(value);
                Station station = stationIdMap.get(stationId);
                yield station;
            }
            case "label" -> {
                for (Label label : world.getLabels()) {
                    if (label.getText().equals(value)) {
                        yield label;
                    }
                }
                yield null;
            }
            case "pathpoint" -> {
                String[] coords = value.split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                yield new PathPoint(x, y);
            }
            case "gameplay_units" -> {
                for (GameplayUnits gUnits : world.getGameplayUnits()) {
                    if (gUnits.getName().equals(value)) {
                        yield gUnits;
                    }
                }
                yield null;
            }
            default -> null;
        };
    }
    private String serializeGameObject(GameObject obj) {
        if (obj instanceof Station) {
            Station station = (Station) obj;
            return String.format("station:%d", station.getUniqueId()); // Используем ID вместо имени
        } else if (obj instanceof Label) {
            Label label = (Label) obj;
            return String.format("label:%d", label.getUniqueId()); // Используем ID
        } else if (obj instanceof PathPoint) {
            PathPoint pathPoint = (PathPoint) obj;
            return String.format("pathpoint:%d,%d", pathPoint.getX(), pathPoint.getY());
        } else    if (obj instanceof GameplayUnits) {
            GameplayUnits gUnits = (GameplayUnits) obj;
            return String.format("gameplay_units:%d", gUnits.getUniqueId());
        }
        return "null";
    }
    private void parseConnection(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        // Пример строки: {stationId:123,direction:NORTH,connectedToId:456}
        String[] parts = line.substring(1, line.length()-1).split(",");

        long stationId = Long.parseLong(parts[0].split(":")[1]);
        Direction direction = Direction.valueOf(parts[1].split(":")[1]);
        long connectedToId = Long.parseLong(parts[2].split(":")[1]);

        Station station = stationIdMap.get(stationId);
        Station connectedTo = stationIdMap.get(connectedToId);

        if (station != null && connectedTo != null) {
            station.getConnections().put(direction, connectedTo);
        }
    }
    private GameplayUnits parseGameplayUnit(String line, GameWorld world) {
        String[] parts = line.substring(1, line.length()-1).split(",");

        long id = -1;
        String name = "";
        int x = 0, y = 0;
        GameplayUnitsType type = GameplayUnitsType.FACTORY;

        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            String key = keyValue[0].trim();
            String value = keyValue.length > 1 ? keyValue[1].trim() : "";

            switch (key) {
                case "id":
                    id = Long.parseLong(value);
                    break;
                case "name":
                    name = unescapeString(value);
                    break;
                case "x":
                    x = Integer.parseInt(value);
                    break;
                case "y":
                    y = Integer.parseInt(value);
                    break;
                case "type":
                    type = GameplayUnitsType.valueOf(value);
                    break;
            }
        }

        // Здесь нужно создать соответствующий тип GameplayUnits
        // В зависимости от вашей реализации, это может быть:
        GameplayUnits gUnits = new GameplayUnits(world, x, y, type);
        if (id != -1) {
            gUnits.setUniqueId(id);
        }
        return gUnits;
    }
    private Station parseStation(String line, GameWorld world) {
        // Пример строки: {id:123,name:"Station 1",x:10,y:20,color:rgb(255,0,0),type:REGULAR}
        String[] parts = line.substring(1, line.length()-1).split(",");

        long id = -1;
        String name = "";
        int x = 0, y = 0;
        Color color = Color.BLACK;
        StationType type = StationType.REGULAR;

        // Парсим все поля по ключам
        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            String key = keyValue[0].trim();
            String value = keyValue.length > 1 ? keyValue[1].trim() : "";

            switch (key) {
                case "id":
                    id = Long.parseLong(value);
                    break;
                case "name":
                    name = unescapeString(value);
                    break;
                case "x":
                    x = Integer.parseInt(value);
                    break;
                case "y":
                    y = Integer.parseInt(value);
                    break;
                case "color":
                    color = parseColor(value);
                    break;
                case "type":
                    type = StationType.valueOf(value);
                    break;
            }
        }

        Station station = new Station(world, x, y, StationColors.fromColor(color), type);
        station.setName(name);
        if (id != -1) {
            station.setUniqueId(id); // Устанавливаем сохраненный ID
        }

        return station;
    }


    private Tunnel parseTunnel(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        String[] parts = line.substring(1, line.length()-1).split(",");

        long startId = Long.parseLong(parts[0].split(":")[1]);
        long endId = Long.parseLong(parts[1].split(":")[1]);

        Station start = stationIdMap.get(startId);
        Station end = stationIdMap.get(endId);

        TunnelType type = TunnelType.valueOf(parts[2].split(":")[1]);

        if (start != null && end != null) {
            Tunnel tunnel = new Tunnel(world, start, end, type);
            return tunnel;
        }
        return null;
    }

    private Label parseLabel(String line, GameWorld world, Map<String, Station> stationMap, Map<Long, Station> stationIdMap) {
        // Пример строки: {text:"Label text",x:15,y:25,parentStationId:123456}
        try {
            String[] parts = line.substring(1, line.length()-1).split(",");
            String text = unescapeString(parts[0].split(":")[1]);
            int x = Integer.parseInt(parts[1].split(":")[1]);
            int y = Integer.parseInt(parts[2].split(":")[1]);

            // Ищем parentStationId более надежным способом
            Station parent = null;
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("parentStationId:")) {
                    String idStr = part.substring("parentStationId:".length()).trim();
                    long parentId = Long.parseLong(idStr);
                    parent = stationIdMap.get(parentId);
                    break;
                }
            }

            if (parent != null) {
                Label label = new Label(world, x, y, text, parent);

                return label;
            }
        } catch (Exception e) {
            System.err.println("Error parsing label line: " + line);
            e.printStackTrace();
        }
        return null;
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
            return Color.BLACK;
        }
    }
    private void parsePathPoints(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        // Пример строки: {tunnelStartId:123,tunnelEndId:456,points:[{x:10,y:20},{x:11,y:21}]}
        if (!line.contains("points:[")) return;

        String[] mainParts = line.substring(1, line.length()-1).split(",points:\\[");
        String tunnelPart = mainParts[0];
        String pointsPart = mainParts[1].substring(0, mainParts[1].length()-1); // убираем последнюю ]

        // Парсим ID станций туннеля
        String[] tunnelParts = tunnelPart.split(",");
        long startId = -1, endId = -1;

        for (String part : tunnelParts) {
            if (part.startsWith("tunnelStartId:")) {
                startId = Long.parseLong(part.split(":")[1]);
            } else if (part.startsWith("tunnelEndId:")) {
                endId = Long.parseLong(part.split(":")[1]);
            }
        }

        Station start = stationIdMap.get(startId);
        Station end = stationIdMap.get(endId);

        // Находим соответствующий туннель
        Tunnel tunnel = world.getTunnels().stream()
                             .filter(t -> t.getStart() == start && t.getEnd() == end)
                             .findFirst()
                             .orElse(null);

        if (tunnel != null && !pointsPart.isEmpty()) {
            tunnel.getPath().clear();

            // Парсим точки: {x:10,y:20},{x:11,y:21}
            String[] pointStrings = pointsPart.split("\\},\\{");
            for (String pointStr : pointStrings) {
                pointStr = pointStr.replace("{", "").replace("}", "");
                String[] coords = pointStr.split(",");
                int x = Integer.parseInt(coords[0].split(":")[1]);
                int y = Integer.parseInt(coords[1].split(":")[1]);
                tunnel.getPath().add(new PathPoint(x, y));
            }
        }
    }

    private void parseConstructionData(String line, GameWorld world,
            Map<Long, Station> stationIdMap,
            boolean isStation, boolean isDestruction) {
        if (isStation) {
            // Для станций: {stationId:123,start:12345,duration:10000}
            String[] parts = line.substring(1, line.length()-1).split(",");
            long stationId = Long.parseLong(parts[0].split(":")[1]);
            Station station = stationIdMap.get(stationId);
            long start = Long.parseLong(parts[1].split(":")[1]);
            long duration = Long.parseLong(parts[2].split(":")[1]);

            if (station != null) {
                if (isDestruction) {
                    world.stationDestructionStartTimes.put(station, start);
                    world.stationDestructionDurations.put(station, duration);
                } else {
                    world.stationBuildStartTimes.put(station, start);
                    world.stationBuildDurations.put(station, duration);
                }
            }
        } else {
            // Для туннелей: {startId:123,endId:456,startTime:12345,duration:10000}
            String[] parts = line.substring(1, line.length()-1).split(",");
            long startId = Long.parseLong(parts[0].split(":")[1]);
            long endId = Long.parseLong(parts[1].split(":")[1]);
            Station start = stationIdMap.get(startId);
            Station end = stationIdMap.get(endId);
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
