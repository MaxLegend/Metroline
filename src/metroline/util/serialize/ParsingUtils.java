package metroline.util.serialize;

import metroline.core.world.GameWorld;
import metroline.objects.enums.*;
import metroline.objects.gameobjects.*;
import metroline.objects.gameobjects.StationLabel;

import java.awt.*;

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
            return Color.BLACK; // Значение по умолчанию
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге цвета: " + colorStr);
            return Color.BLACK;
        }
    }

    /**
     * Экранирует строку для сохранения (добавляет кавычки и экранирует внутренние кавычки).
     *
     * @param input Исходная строка.
     * @return Экранированная строка.
     */
    public static String escapeString(String input) {
        if (input == null) return "\"\"";
        return "\"" + input.replace("\"", "\\\"") + "\"";
    }

    /**
     * Убирает экранирование из строки (убирает внешние кавычки и внутренние экранированные кавычки).
     *
     * @param input Экранированная строка.
     * @return Исходная строка.
     */
    public static String unescapeString(String input) {
        if (input == null) return "";
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1).replace("\\\"", "\"");
        }
        return input; // Если кавычек нет, возвращаем как есть
    }

    /**
     * Извлекает значение из пары "ключ:значение".
     *
     * @param part        Строка с парой "ключ:значение".
     * @param expectedKey Ожидаемый ключ (для проверки).
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
     * Разбирает строку, представляющую игровой объект, и создает соответствующий экземпляр.
     *
     * @param contentStr     Строка с данными объекта ("type:id_or_data").
     * @param world          Ссылка на текущий игровой мир.
     * @param stationIdMap   Карта для поиска станций по ID.
     * @return Созданный объект GameObject или null, если не удалось распознать.
     */
    public static GameObject parseGameObject(String contentStr, GameWorld world, Map<Long, Station> stationIdMap) {
        if (contentStr == null || contentStr.equals("null")) {
            return null;
        }
        String[] parts = contentStr.split(":", 2); // Разделяем только по первому ':'
        if (parts.length < 2) {
            System.err.println("Некорректный формат строки объекта: " + contentStr);
            return null; // Некорректный формат
        }
        String type = parts[0];
        String value = parts[1];

        return switch (type) {
            case "station" -> {
                try {
                    long stationId = Long.parseLong(value);
                    yield stationIdMap.get(stationId); // Получаем станцию по ID
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
            case "gameplay_units" -> {
                try {
                    long unitsId = Long.parseLong(value);
                    for (GameplayUnits gUnits : world.getGameplayUnits()) {
                        if (gUnits.getUniqueId() == unitsId) {
                            yield gUnits;
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Ошибка при парсинге ID GameplayUnits: " + value);
                }
                yield null;
            }
            case "train" -> {
                try {
                    long trainId = Long.parseLong(value);
                    for (Train train : world.getTrains()) {
                        if (train.getUniqueId() == trainId) {
                            yield train;
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Ошибка при парсинге ID поезда: " + value);
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
     * Преобразует информацию об игровом объекте в строку для сохранения.
     *
     * @param obj Объект для сериализации.
     * @return Строка в формате "type:id_or_data".
     */
    public static String serializeGameObject(GameObject obj) {
        if (obj instanceof Station) {
            Station station = (Station) obj;
            return String.format("station:%d", station.getUniqueId()); // Используем ID вместо имени
        } else if (obj instanceof StationLabel) {
            StationLabel stationLabel = (StationLabel) obj;
            // Используем уникальный ID для надежности
            return String.format("label:%d", stationLabel.getUniqueId());
        } else if (obj instanceof PathPoint) {
            PathPoint pathPoint = (PathPoint) obj;
            return String.format("pathpoint:%d,%d", pathPoint.getX(), pathPoint.getY());
        } else if (obj instanceof GameplayUnits) {
            GameplayUnits gUnits = (GameplayUnits) obj;
            return String.format("gameplay_units:%d", gUnits.getUniqueId());
        }  else if (obj instanceof Train) {
        Train train = (Train) obj;
        return String.format("train:%d", train.getUniqueId());
    }
        return "null";
    }

    /**
     * Разбирает строку, представляющую станцию.
     *
     * @param line Строка с данными станции.
     * @param world Ссылка на текущий игровой мир.
     * @return Созданный объект Station или null при ошибке.
     */
//    public static Station parseStation(String line, GameWorld world) {
//        // Пример строки: {id:123,name:"Station 1",x:10,y:20,color:#FF0000,type:REGULAR}
//        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
//            System.err.println("Некорректный формат строки станции: " + line);
//            return null;
//        }
//        String content = line.substring(1, line.length() - 1); // Убираем { и }
//        String[] parts = content.split(",");
//
//        long id = -1;
//        String name = "";
//        int x = 0, y = 0;
//        Color color = Color.BLACK; // Значение по умолчанию
//        StationType type = StationType.REGULAR; // Значение по умолчанию
//        long constructionDate = world.getGameTime().getCurrentTimeMillis(); // Текущее время по умолчанию
//        // Парсим все поля по ключам
//        for (String part : parts) {
//            String[] keyValue = part.split(":", 2);
//            if (keyValue.length == 2) {
//                String key = keyValue[0].trim();
//                String value = keyValue[1].trim();
//                switch (key) {
//                    case "id":
//                        try { id = Long.parseLong(value); } catch (NumberFormatException ignored) {}
//                        break;
//                    case "name":
//                        name = unescapeString(value);
//                        break;
//                    case "x":
//                        try { x = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
//                        break;
//                    case "y":
//                        try { y = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
//                        break;
//                    case "color":
//                        color = parseColor(value);
//                        break;
//                    case "type":
//                        try { type = StationType.valueOf(value); } catch (IllegalArgumentException ignored) {}
//                        break;
//                    case "constructionDate":
//                        try { constructionDate = Long.parseLong(value); } catch (NumberFormatException ignored) {
//                            System.err.println("Некорректный формат constructionDate: " + value);
//                        }
//                        break;
//                }
//            }
//        }
//
//        Station station = new Station(world, x, y, StationColors.fromColor(color), type);
//        station.setName(name);
//        station.setConstructionDate(constructionDate); // Устанавливаем дату постройки
//        if (id != -1) {
//            station.setUniqueId(id); // Устанавливаем сохраненный ID
//        }
//        return station;
//    }
    public static Station parseStation(String line, GameWorld world) {
        // Пример строки: {id:123,name:"Station 1",x:10,y:20,color:#FF0000,type:REGULAR}
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки станции: " + line);
            return null;
        }
        String content = line.substring(1, line.length() - 1); // Убираем { и }
        String[] parts = content.split(",");

        long id = -1;
        String name = "";
        int x = 0, y = 0;
        Color color = Color.BLACK; // Значение по умолчанию
        StationType type = StationType.REGULAR; // Значение по умолчанию
        long constructionDate = world.getGameTime().getCurrentTimeMillis(); // Текущее время по умолчанию
        boolean isDepo = false; // Флаг для станций депо

        // Парсим все поля по ключам
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
                    case "constructionDate":
                        try { constructionDate = Long.parseLong(value); } catch (NumberFormatException ignored) {
                            System.err.println("Некорректный формат constructionDate: " + value);
                        }
                        break;
                    case "isDepo":
                        // Если есть флаг isDepo, устанавливаем тип станции как DEPO
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
        station.setConstructionDate(constructionDate); // Устанавливаем дату постройки
        station.setSkipTypeUpdate(true);
        if (id != -1) {
            station.setUniqueId(id); // Устанавливаем сохраненный ID
        }
        return station;
    }
    /**
     * Разбирает строку, представляющую туннель.
     *
     * @param line Строка с данными туннеля.
     * @param world Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     * @return Созданный объект Tunnel или null при ошибке.
     */
    public static Tunnel parseTunnel(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки туннеля: " + line);
            return null;
        }
        String content = line.substring(1, line.length() - 1); // Убираем { и }
        String[] parts = content.split(",");

        long startId = -1;
        long endId = -1;
        TunnelType type = TunnelType.PLANNED; // Значение по умолчанию

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
            Tunnel tunnel = new Tunnel(world, start, end, type);
            // Длина будет рассчитана в конструкторе Tunnel
            return tunnel;
        }
        return null;
    }

    /**
     * Разбирает строку, представляющую игровой юнит (GameplayUnits).
     *
     * @param line Строка с данными юнита.
     * @param world Ссылка на текущий игровой мир.
     * @return Созданный объект GameplayUnits или null при ошибке.
     */
    public static GameplayUnits parseGameplayUnit(String line, GameWorld world) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки GameplayUnits: " + line);
            return null;
        }
        String content = line.substring(1, line.length() - 1); // Убираем { и }
        String[] parts = content.split(",");

        long id = -1;
        String name = "";
        int x = 0, y = 0;
        GameplayUnitsType type = GameplayUnitsType.FACTORY; // Значение по умолчанию

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
                    case "type":
                        try { type = GameplayUnitsType.valueOf(value); } catch (IllegalArgumentException ignored) {}
                        break;
                }
            }
        }

        // Создаем юнит
        GameplayUnits gUnits = new GameplayUnits(world, x, y, type);
        if (id != -1) {
            gUnits.setUniqueId(id);
        }
        return gUnits;
    }

    /**
     * Разбирает строку, представляющую метку.
     *
     * @param line Строка с данными метки.
     * @param world Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     * @return Созданный объект Label или null при ошибке.
     */
    public static StationLabel parseLabel(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        // Пример строки: {text:"Label text",x:15,y:25,parentStationId:123456}
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки метки: " + line);
            return null;
        }
        try {
            String content = line.substring(1, line.length() - 1); // Убираем { и }
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
     * Разбирает строку, представляющую соединение между станциями.
     *
     * @param line Строка с данными соединения.
     * @param world Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     */
    public static void parseConnection(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        // Пример строки: {stationId:123,direction:NORTH,connectedToId:456}
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки соединения: " + line);
            return;
        }
        String content = line.substring(1, line.length() - 1); // Убираем { и }
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
     * @param line Строка с данными точек пути.
     * @param world Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     */
    public static void parsePathPoints(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        // Пример строки: {tunnelStartId:123,tunnelEndId:456,points:[{x:10,y:20},{x:11,y:21}]}
        if (line == null || !line.contains("points:[")) {
            System.err.println("Некорректный формат строки точек пути: " + line);
            return;
        }

        String content = line.substring(1, line.length() - 1); // Убираем { и }

        String tunnelPart = "";
        String pointsPart = "";

        int pointsStartIndex = content.indexOf("points:[");
        if (pointsStartIndex != -1) {
            tunnelPart = content.substring(0, pointsStartIndex);
            // Извлекаем часть с точками, убирая "points:[" и "]"
            pointsPart = content.substring(pointsStartIndex + "points:[".length(), content.length() - 1);
        } else {
            tunnelPart = content; // Если points не найдены, обрабатываем только туннельную часть
        }

        // Парсим ID станций туннеля
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

        // Находим соответствующий туннель
        Tunnel tunnel = world.getTunnels().stream()
                             .filter(t -> t.getStart() == start && t.getEnd() == end)
                             .findFirst()
                             .orElse(null);

        if (tunnel != null && !pointsPart.isEmpty()) {
            tunnel.getPath().clear();
            // Парсим точки: {x:10,y:20},{x:11,y:21}
            // Разделяем точки по "},{" и обрабатываем каждую
            String[] pointStrings = pointsPart.split("\\},\\{");
            for (String pointStr : pointStrings) {
                pointStr = pointStr.replace("{", "").replace("}", ""); // Убираем оставшиеся скобки
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
     * Разбирает строки с данными о строительстве/разрушении станций или туннелей.
     *
     * @param line Строка с данными о строительстве/разрушении.
     * @param world Ссылка на текущий игровой мир.
     * @param stationIdMap Карта для поиска станций по ID.
     * @param isStation True, если обрабатываются станции, false для туннелей.
     * @param isDestruction True, если обрабатывается разрушение, false для строительства.
     */
    public static void parseConstructionData(String line, GameWorld world,
            Map<Long, Station> stationIdMap,
            boolean isStation, boolean isDestruction) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки данных строительства/разрушения: " + line);
            return;
        }
        String content = line.substring(1, line.length() - 1); // Убираем { и }
        String[] parts = content.split(",");

        if (isStation) {
            // Для станций: {stationId:123,start:12345,duration:10000}
            long stationId = -1;
            long start = 0;
            long duration = 0;

            for (String part : parts) {
                String[] keyValue = part.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    switch (key) {
                        case "stationId":
                            try { stationId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                            break;
                        case "start":
                            try { start = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                            break;
                        case "duration":
                            try { duration = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                            break;
                    }
                }
            }

            Station station = stationIdMap.get(stationId);
            if (station != null) {
                if (isDestruction) {
                    world.getConstructionProcessor().stationDestructionStartTimes.put(station, start);
                    world.getConstructionProcessor().stationDestructionDurations.put(station, duration);
                } else {
                    world.getConstructionProcessor().stationBuildStartTimes.put(station, start);
                    world.getConstructionProcessor().stationBuildDurations.put(station, duration);
                }
            } else {
                System.err.println("Станция не найдена для данных строительства/разрушения: ID=" + stationId);
            }
        } else {
            // Для туннелей: {startId:123,endId:456,startTime:12345,duration:10000}
            long startId = -1;
            long endId = -1;
            long startTime = 0;
            long duration = 0;

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
                        case "startTime": // Используем "startTime" как в сохранении
                            try { startTime = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                            break;
                        case "duration":
                            try { duration = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                            break;
                    }
                }
            }

            Station start = stationIdMap.get(startId);
            Station end = stationIdMap.get(endId);

            // Находим соответствующий туннель
            Tunnel tunnel = world.getTunnels().stream()
                                 .filter(t -> t.getStart() == start && t.getEnd() == end)
                                 .findFirst()
                                 .orElse(null);

            if (tunnel != null) {
                if (isDestruction) {
                    world.getConstructionProcessor().tunnelDestructionStartTimes.put(tunnel, startTime);
                    world.getConstructionProcessor().tunnelDestructionDurations.put(tunnel, duration);
                } else {
                    world.getConstructionProcessor().tunnelBuildStartTimes.put(tunnel, startTime);
                    world.getConstructionProcessor().tunnelBuildDurations.put(tunnel, duration);
                }
            } else {
                System.err.println("Туннель не найден для данных строительства/разрушения: startId=" + startId + ", endId=" + endId);
            }
        }
    }
    static Train parseTrain(String line, GameWorld world, Map<Long, Station> stationIdMap) {
        if (line == null || !line.startsWith("{") || !line.endsWith("}")) {
            System.err.println("Некорректный формат строки поезда: " + line);
            return null;
        }

        String content = line.substring(1, line.length() - 1);
        String[] parts = content.split(",");

        long id = -1;
        TrainType type = TrainType.CLASSIC;
        Color color = Color.GRAY; // Цвет по умолчанию
        long currentStationId = -1;
        long prevTunnelStartId = -1, prevTunnelEndId = -1;
        long currTunnelStartId = -1, currTunnelEndId = -1;
        float progress = 0f;
        boolean movingForward = true;
        float waitTimer = 0f;
        boolean hasPaid = false;
        float currentSpeed = 0f;
        float targetSpeed = 0f;
        boolean isAccelerating = false;
        boolean isBraking = false;
        Direction direction = Direction.EAST;

        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                switch (key) {
                    case "id":
                        try { id = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "type":
                        try { type = TrainType.valueOf(value); } catch (IllegalArgumentException ignored) {}
                        break;
                    case "color": // Добавляем парсинг цвета
                        try {  color = parseColor(value); } catch (IllegalArgumentException ignored) {}
                        break;
                    case "currentStationId":
                        try { currentStationId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "previousTunnelStartId":
                        try { prevTunnelStartId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "previousTunnelEndId":
                        try { prevTunnelEndId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "currentTunnelStartId":
                        try { currTunnelStartId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "currentTunnelEndId":
                        try { currTunnelEndId = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "progress":
                        try { progress = Float.parseFloat(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "movingForward":
                        try { movingForward = Boolean.parseBoolean(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "waitTimer":
                        try { waitTimer = Float.parseFloat(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "hasPaid":
                        try { hasPaid = Boolean.parseBoolean(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "currentSpeed":
                        try { currentSpeed = Float.parseFloat(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "targetSpeed":
                        try { targetSpeed = Float.parseFloat(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "isAccelerating":
                        try { isAccelerating = Boolean.parseBoolean(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "isBraking":
                        try { isBraking = Boolean.parseBoolean(value); } catch (NumberFormatException ignored) {}
                        break;
                    case "direction":
                        try { direction = Direction.valueOf(value); } catch (IllegalArgumentException ignored) {}
                        break;
                }
            }
        }

        // Находим станцию и туннели
        Station currentStation = stationIdMap.get(currentStationId);
        Tunnel previousTunnel = findTunnel(world, prevTunnelStartId, prevTunnelEndId);
        Tunnel currentTunnel = findTunnel(world, currTunnelStartId, currTunnelEndId);

        // Создаем поезд
        Train train;
        if (currentStation != null) {
            train = new Train(world, currentStation, type);

        } else {
            // Если станция не найдена, создаем поезд в депо или на первой доступной станции
            Station firstStation = world.getStations().stream()
                                        .filter(s -> s.getType() == StationType.DEPO)
                                        .findFirst()
                                        .orElse(world.getStations().isEmpty() ? null : world.getStations().get(0));
            if (firstStation == null) return null;
            train = new Train(world, firstStation, type);
        }
        train.setColor(color);
        // Устанавливаем сохраненные параметры
        if (id != -1) {
            train.setUniqueId(id);
        }
        train.setCurrentStation(currentStation);
        train.setPreviousTunnel(previousTunnel);
        train.setCurrentTunnel(currentTunnel);
        train.setProgress(progress);
        train.setMovingForward(movingForward);
        train.setWaitTimer(waitTimer);
        train.setHasPaidAtCurrentStation(hasPaid);
        train.setCurrentSpeed(currentSpeed);
        train.setTargetSpeed(targetSpeed);
        train.setAccelerating(isAccelerating);
        train.setBraking(isBraking);
        train.setDirection(direction);

        // Обновляем позицию
        if (currentTunnel != null && currentTunnel.getPath() != null && !currentTunnel.getPath().isEmpty()) {
            train.updatePositionFromPathProgress();
        }

        return train;
    }

    private static Tunnel findTunnel(GameWorld world, long startId, long endId) {
        if (startId == -1 || endId == -1) return null;

        Station start = world.getStations().stream()
                             .filter(s -> s.getUniqueId() == startId)
                             .findFirst()
                             .orElse(null);
        Station end = world.getStations().stream()
                           .filter(s -> s.getUniqueId() == endId)
                           .findFirst()
                           .orElse(null);

        if (start == null || end == null) return null;

        return world.getTunnels().stream()
                    .filter(t -> (t.getStart() == start && t.getEnd() == end) ||
                            (t.getStart() == end && t.getEnd() == start))
                    .findFirst()
                    .orElse(null);
    }
}
