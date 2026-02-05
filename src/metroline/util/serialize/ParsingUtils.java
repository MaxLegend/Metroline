package metroline.util.serialize;

import metroline.core.world.GameWorld;
import metroline.objects.enums.*;
import metroline.objects.gameobjects.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ParsingUtils {

    /**
     * Преобразует строку HEX в объект Color.
     *
     * @param colorStr Строка в формате "#RRGGBB".
     * @return Объект Color или Color.BLACK при ошибке.
     */
    public static Color parseColor(String colorStr) {
        try {
            if (colorStr != null && colorStr.startsWith("#") && colorStr.length() == 7) {
                int r = Integer.parseInt(colorStr.substring(1, 3), 16);
                int g = Integer.parseInt(colorStr.substring(3, 5), 16);
                int b = Integer.parseInt(colorStr.substring(5, 7), 16);
                return new Color(r, g, b);
            }
            return Color.BLACK;
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге цвета: " + colorStr);
            return Color.BLACK;
        }
    }

    /**
     * Экранирует строку для сохранения.
     *
     * @param input Исходная строка.
     * @return Экранированная строка.
     */
    public static String escapeString(String input) {
        if (input == null) return "\"\"";
        return "\"" + input.replace("\"", "\\\"") + "\"";
    }

    /**
     * Убирает экранирование из строки.
     *
     * @param input Экранированная строка.
     * @return Исходная строка.
     */
    public static String unescapeString(String input) {
        if (input == null) return "";
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1).replace("\\\"", "\"");
        }
        return input;
    }

    /**
     * Извлекает значение из пары "ключ:значение".
     *
     * @param part        Строка с парой "ключ:значение".
     * @param expectedKey Ожидаемый ключ.
     * @return Значение.
     * @throws IllegalArgumentException Если ключ не совпадает или формат некорректен.
     */
    public static String extractValue(String part, String expectedKey) {
        if (part == null || part.isEmpty()) {
            throw new IllegalArgumentException("Input part is null or empty");
        }
        String[] keyValue = part.split(":", 2);
        if (keyValue.length < 2) {
            throw new IllegalArgumentException("Invalid key-value pair: " + part);
        }
        if (!keyValue[0].trim().equals(expectedKey)) {
            throw new IllegalArgumentException("Expected key '" + expectedKey + "' but found '" + keyValue[0] + "'");
        }
        return keyValue[1].trim();
    }

    /**
     * Разбирает строку игрового объекта и создает соответствующий экземпляр.
     *
     * @param contentStr   Строка с данными объекта ("type:id_or_data").
     * @param world        Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     * @return Созданный объект GameObject или null.
     */
    public static GameObject parseGameObject(String contentStr, GameWorld world, Map<Long, Station> stationIdMap) {
        if (contentStr == null || contentStr.equals("null")) {
            return null;
        }
        String[] parts = contentStr.split(":", 2);
        if (parts.length < 2) {
            System.err.println("Некорректный формат строки объекта: " + contentStr);
            return null;
        }
        String type = parts[0];
        String value = parts[1];

        return switch (type) {
            case "station" -> {
                try {
                    long stationId = Long.parseLong(value);
                    yield stationIdMap.get(stationId);
                } catch (NumberFormatException e) {
                    System.err.println("Ошибка при парсинге ID станции: " + value);
                    yield null;
                }
            }
            case "label" -> {
                try {
                    long labelId = Long.parseLong(value);
                    for (StationLabel stationLabel : world.getLabels()) {
                        if (stationLabel.getUniqueId() == labelId) {
                            yield stationLabel;
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Ошибка при парсинге ID метки: " + value);
                }
                yield null;
            }
            case "pathpoint" -> {
                String[] coords = value.split(",");
                if (coords.length == 2) {
                    try {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        yield new PathPoint(x, y);
                    } catch (NumberFormatException e) {
                        System.err.println("Ошибка при парсинге координат PathPoint: " + value);
                    }
                }
                yield null;
            }
            case "riverpoint" -> {
                try {
                    long riverPointId = Long.parseLong(value);
                    for (RiverPoint riverPoint : world.getRiverPoints()) {
                        if (riverPoint.getUniqueId() == riverPointId) {
                            yield riverPoint;
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing RiverPoint ID: " + value);
                }
                yield null;
            }
            default -> {
                System.err.println("Неизвестный тип объекта: " + type);
                yield null;
            }
        };
    }
    /**
     * Parse River from save file line
     * Format: {id:123,name:"river_1",width:20.00,color:#4682B4,pointCount:5}
     * @param line Line with river data
     * @param world GameWorld reference
     * @return River object or null on error
     */
    public static River parseRiver(String line, GameWorld world) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("[ParsingUtils::parseRiver] Invalid format: " + line);
            return null;
        }

        String content = line.substring(1, line.length() - 1);
        String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Split by comma, but not inside quotes

        long id = -1;
        String name = "";
        float width = 20f;
        Color color = new Color(70, 130, 180, 200); // Default blue river color

        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                switch (key) {
                    case "id":
                        try {
                            id = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            System.err.println("[ParsingUtils::parseRiver] Error parsing id: " + value);
                        }
                        break;
                    case "name":
                        name = unescapeString(value);
                        break;
                    case "width":
                        try {
                            width = Float.parseFloat(value);
                        } catch (NumberFormatException e) {
                            System.err.println("[ParsingUtils::parseRiver] Error parsing width: " + value);
                        }
                        break;
                    case "color":
                        color = parseColor(value);
                        // Preserve alpha channel for rivers
                        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 200);
                        break;
                    case "pointCount":
                        // Just informational, actual points loaded separately
                        break;
                }
            }
        }

        River river = new River(world);
        river.setName(name);
        river.setWidth(width);
        river.setRiverColor(color);

        if (id != -1) {
            river.setUniqueId(id);
        }

        return river;
    }

    /**
     * Parse RiverPoint from save file line and add to corresponding river
     * Format: {riverId:123,x:10,y:20,orderIndex:0}
     * @param line Line with river point data
     * @param world GameWorld reference
     */
    public static void parseRiverPoint(String line, GameWorld world) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("[ParsingUtils::parseRiverPoint] Invalid format: " + line);
            return;
        }

        String content = line.substring(1, line.length() - 1);
        String[] parts = content.split(",");

        long riverId = -1;
        int x = 0, y = 0, orderIndex = 0;

        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                switch (key) {
                    case "riverId":
                        try {
                            riverId = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            System.err.println("[ParsingUtils::parseRiverPoint] Error parsing riverId: " + value);
                        }
                        break;
                    case "x":
                        try {
                            x = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            System.err.println("[ParsingUtils::parseRiverPoint] Error parsing x: " + value);
                        }
                        break;
                    case "y":
                        try {
                            y = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            System.err.println("[ParsingUtils::parseRiverPoint] Error parsing y: " + value);
                        }
                        break;
                    case "orderIndex":
                        try {
                            orderIndex = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            System.err.println("[ParsingUtils::parseRiverPoint] Error parsing orderIndex: " + value);
                        }
                        break;
                }
            }
        }

        // Find river by ID
        River targetRiver = null;
        for (River river : world.getRivers()) {
            if (river.getUniqueId() == riverId) {
                targetRiver = river;
                break;
            }
        }

        if (targetRiver == null) {
            System.err.println("[ParsingUtils::parseRiverPoint] River not found for id: " + riverId);
            return;
        }

        // Check bounds
        if (x < 0 || x >= world.getWidth() || y < 0 || y >= world.getHeight()) {
            System.err.println("[ParsingUtils::parseRiverPoint] Out of bounds: " + x + ", " + y);
            return;
        }

        // Create and add RiverPoint
        RiverPoint point = new RiverPoint(world, x, y, targetRiver);
        point.setOrderIndex(orderIndex);

        // Add to world's gameGrid
        world.addRiverPoint(point);

        // Add to river's points list
        targetRiver.addPoint(point);
    }
    /**
     * Преобразует игровой объект в строку для сохранения.
     *
     * @param obj Объект для сериализации.
     * @return Строка в формате "type:id_or_data".
     */
    public static String serializeGameObject(GameObject obj) {
        if (obj instanceof Station station) {
            return String.format("station:%d", station.getUniqueId());
        } else if (obj instanceof StationLabel stationLabel) {
            return String.format("label:%d", stationLabel.getUniqueId());
        } else if (obj instanceof PathPoint pathPoint) {
            return String.format("pathpoint:%d,%d", pathPoint.getX(), pathPoint.getY());
        }else if (obj instanceof RiverPoint riverPoint) {
            return String.format("riverpoint:%d", riverPoint.getUniqueId());
        }
        return "null";
    }

    /**
     * Разбирает строку станции.
     *
     * @param line  Строка с данными станции.
     * @param world Ссылка на текущий игровой мир.
     * @return Созданный объект Station или null при ошибке.
     */
    public static Station parseStation(String line, GameWorld world) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки станции: " + line);
            return null;
        }
        String content = line.substring(1, line.length() - 1);
        String[] parts = content.split(",");

        long id = -1;
        String name = "";
        int x = 0, y = 0;
        Color color = Color.BLACK;
        StationType type = StationType.REGULAR;
        boolean isDepo = false;

        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                switch (key) {
                    case "id":
                        try { id = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "name":
                        name = unescapeString(value);
                        break;
                    case "x":
                        try { x = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "y":
                        try { y = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "color":
                        color = parseColor(value);
                        break;
                    case "type":
                        try { type = StationType.valueOf(value); } catch (IllegalArgumentException ignored) {}
                        break;
                    case "isDepo":
                        isDepo = Boolean.parseBoolean(value);
                        if (isDepo && type != StationType.DEPO) {
                            type = StationType.DEPO;
                        }
                        break;
                }
            }
        }

        Station station = new Station(world, x, y, StationColors.fromColor(color), type);
        station.setName(name);

        if (id != -1) {
            station.setUniqueId(id);
        }

        if (content.contains(",properties:")) {
            int propStart = content.indexOf(",properties:") + ",properties:".length();
            int propEnd = content.length();
            // Найти конец свойств (учитывая вложенность скобок)
            int bracketCount = 0;
            boolean inBrackets = false;
            for (int i = propStart; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '[') {
                    bracketCount++;
                    inBrackets = true;
                } else if (c == ']') {
                    bracketCount--;
                    if (bracketCount == 0 && inBrackets) {
                        propEnd = i + 1;
                        break;
                    }
                }
            }
            String propStr = content.substring(propStart, propEnd);
            station.setCustomProperties(ParsingUtils.parseProperties(propStr));
        }
        return station;
    }
    /**
     * Serializes line names map to string format
     */
    public static String serializeLineNames(Map<metroline.objects.enums.StationColors, String> lineNames) {
        if (lineNames == null || lineNames.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<metroline.objects.enums.StationColors, String> entry : lineNames.entrySet()) {
            sb.append(String.format("{color:%s,name:%s}\n",
                    entry.getKey().name(),
                    escapeString(entry.getValue())));
        }
        return sb.toString();
    }

    /**
     * Parses a single line name entry
     */
    public static void parseLineName(String line, metroline.core.world.GameWorld world) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("[ParsingUtils::parseLineName] Invalid line name format: " + line);
            return;
        }

        String content = line.substring(1, line.length() - 1);
        String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);

        try {
            String colorStr = extractValue(parts[0], "color");
            String nameStr = extractValue(parts[1], "name");

            metroline.objects.enums.StationColors color = metroline.objects.enums.StationColors.valueOf(colorStr);
            String name = unescapeString(nameStr);

            world.setCustomLineName(color, name);
            metroline.util.MetroLogger.logInfo("[ParsingUtils::parseLineName] Loaded: " +
                    color.name() + " = " + name);
        } catch (Exception e) {
            System.err.println("[ParsingUtils::parseLineName] Error parsing: " + line);
            e.printStackTrace();
        }
    }
    /**
     * Разбирает строку туннеля.
     *
     * @param line         Строка с данными туннеля.
     * @param world        Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     * @return Созданный объект Tunnel или null при ошибке.
     */
    public static Tunnel parseTunnel(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки туннеля: " + line);
            return null;
        }
        String content = line.substring(1, line.length() - 1);
        String[] parts = content.split(",");

        long startId = -1;
        long endId = -1;
        TunnelType type = TunnelType.PLANNED;

        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                switch (key) {
                    case "startId":
                        try { startId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "endId":
                        try { endId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "type":
                        try { type = TunnelType.valueOf(value); } catch (IllegalArgumentException ignored) {}
                        break;
                }
            }
        }

        Station start = stationIdMap.get(startId);
        Station end = stationIdMap.get(endId);

        if (start != null && end != null) {
            return new Tunnel(world, start, end, type);
        }
        return null;
    }

    /**
     * Разбирает строку метки.
     *
     * @param line         Строка с данными метки.
     * @param world        Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     * @return Созданный объект StationLabel или null при ошибке.
     */
    public static StationLabel parseLabel(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки метки: " + line);
            return null;
        }
        try {
            String content = line.substring(1, line.length() - 1);
            String[] parts = content.split(",");

            String text = "";
            int x = 0, y = 0;
            long parentStationId = -1;

            for (String part : parts) {
                String[] keyValue = part.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    switch (key) {
                        case "text":
                            text = unescapeString(value);
                            break;
                        case "x":
                            try { x = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                            break;
                        case "y":
                            try { y = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                            break;
                        case "parentStationId":
                            try { parentStationId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                            break;
                    }
                }
            }

            Station parent = stationIdMap.get(parentStationId);
            if (parent != null) {
                StationLabel stationLabel = new StationLabel(world, x, y, text, parent);
                parent.setLabel(stationLabel);
                stationLabel.setParentGameObject(parent);

                int index = x + y * world.getWidth();
                if (index < world.gameGrid.length) {
                    world.gameGrid[index].setContent(stationLabel);
                }

                return stationLabel;
            } else {
                System.err.println("Родительская станция не найдена для метки: " + text + " с ID: " + parentStationId);
            }
        } catch (Exception e) {
            System.err.println("Error parsing label line: " + line);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Разбирает строку соединения между станциями.
     *
     * @param line         Строка с данными соединения.
     * @param world        Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     */
    public static void parseConnection(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки соединения: " + line);
            return;
        }
        String content = line.substring(1, line.length() - 1);
        String[] parts = content.split(",");

        long stationId = -1;
        Direction direction = null;
        long connectedToId = -1;

        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                switch (key) {
                    case "stationId":
                        try { stationId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "direction":
                        try { direction = Direction.valueOf(value); } catch (IllegalArgumentException ignored) {}
                        break;
                    case "connectedToId":
                        try { connectedToId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                }
            }
        }

        Station station = stationIdMap.get(stationId);
        Station connectedTo = stationIdMap.get(connectedToId);
        if (station != null && direction != null && connectedTo != null) {
            station.getConnections().put(direction, connectedTo);
        } else {
            System.err.println("Ошибка при парсинге соединения: " + line +
                    " | stationId: " + stationId + ", direction: " + direction + ", connectedToId: " + connectedToId);
        }
    }

    /**
     * Разбирает строки с точками пути туннеля.
     *
     * @param line         Строка с данными точек пути.
     * @param world        Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     */
    public static void parsePathPoints(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        if (line == null || !line.contains("points:[")) {
            System.err.println("Некорректный формат строки точек пути: " + line);
            return;
        }

        String content = line.substring(1, line.length() - 1);

        String tunnelPart = "";
        String pointsPart = "";

        int pointsStartIndex = content.indexOf("points:[");
        if (pointsStartIndex != -1) {
            tunnelPart = content.substring(0, pointsStartIndex);
            pointsPart = content.substring(pointsStartIndex + "points:[".length(), content.length() - 1);
        } else {
            tunnelPart = content;
        }

        String[] tunnelParts = tunnelPart.split(",");
        long startId = -1, endId = -1;
        for (String part : tunnelParts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                switch (key) {
                    case "tunnelStartId":
                        try { startId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "tunnelEndId":
                        try { endId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                }
            }
        }

        Station start = stationIdMap.get(startId);
        Station end = stationIdMap.get(endId);

        Tunnel tunnel = world.getTunnels().stream()
                             .filter(t -> t.getStart() == start && t.getEnd() == end)
                             .findFirst()
                             .orElse(null);

        if (tunnel != null && !pointsPart.isEmpty()) {
            tunnel.getPath().clear();
            String[] pointStrings = pointsPart.split("\\},\\{");
            for (String pointStr : pointStrings) {
                pointStr = pointStr.replace("{", "").replace("}", "");
                String[] coords = pointStr.split(",");
                int x = 0, y = 0;
                boolean xParsed = false, yParsed = false;
                for (String coord : coords) {
                    String[] keyValue = coord.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        switch (key) {
                            case "x":
                                try { x = Integer.parseInt(value); xParsed = true; } catch (NumberFormatException ignored) {}
                                break;
                            case "y":
                                try { y = Integer.parseInt(value); yParsed = true; } catch (NumberFormatException ignored) {}
                                break;
                        }
                    }
                }
                if (xParsed && yParsed) {
                    tunnel.getPath().add(new PathPoint(x, y));
                } else {
                    System.err.println("Ошибка при парсинге координат точки пути: " + pointStr);
                }
            }
        } else if (tunnel == null) {
            System.err.println("Туннель не найден для точек пути: startId=" + startId + ", endId=" + endId);
        }
    }

    /**
     * Сериализует пользовательские свойства станции в строку
     */
    public static String serializeProperties(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!first) sb.append(",");
            sb.append(String.format("{key:%s,value:%s}",
                    escapeString(entry.getKey()),
                    escapeString(entry.getValue())));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Десериализует свойства из строки в карту
     */
    public static Map<String, String> parseProperties(String propStr) {
        Map<String, String> properties = new HashMap<>();
        if (propStr == null || propStr.isEmpty() || !propStr.startsWith("[")) {
            return properties;
        }

        // Убираем внешние скобки []
        String content = propStr.substring(1, propStr.length() - 1).trim();
        if (content.isEmpty()) return properties;

        // Разбиваем по },{ но учитываем вложенные структуры
        String[] items = content.split("\\},\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "").trim();
            if (item.isEmpty()) continue;

            String key = null, value = null;
            String[] parts = item.split(",");
            for (String part : parts) {
                String[] kv = part.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim();
                    String v = kv[1].trim();
                    if ("key".equals(k)) {
                        key = unescapeString(v);
                    } else if ("value".equals(k)) {
                        value = unescapeString(v);
                    }
                }
            }
            if (key != null && value != null) {
                properties.put(key, value);
            }
        }
        return properties;
    }
}