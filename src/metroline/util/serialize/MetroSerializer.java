package metroline.util.serialize;

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

/**
 * Сериализатор для сохранения и загрузки состояния игрового мира.
 * Сохраняет мир в текстовом формате с тегами.
 */
public class MetroSerializer {

    private static final String VERSION = "1.1";
    private static final String SAVE_FOLDER = "saves";

    /**
     * Преобразует объект Color в строку формата HEX.
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
     * @param world Объект GameWorld для сохранения.
     * @param filename Имя файла для сохранения (без пути).
     * @throws IOException Если возникает ошибка при записи файла.
     */
    public void saveWorld(GameWorld world, String filename) throws IOException {
        File saveDir = new File(SAVE_FOLDER);
        if (!saveDir.exists()) {
            saveDir.mkdirs(); // Используем mkdirs для создания всех необходимых родительских директорий
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

            // Мир (worldGrid)
            writer.write("worldGrid:[\n");
            for (int y = 0; y < world.getHeight(); y++) {
                for (int x = 0; x < world.getWidth(); x++) {
                    WorldTile tile = world.getWorldTile(x, y);
                    writer.write(String.format(Locale.US,
                            "{x:%d,y:%d,perm:%.2f,isWater:%b,abilityPay:%.2f,passengerCount:%d}\n",
                            x, y,
                            tile.getPerm(),
                            tile.isWater(),
                            tile.getAbilityPay(),
                            tile.getPassengerCount()
                            // colorToHex(tile.getCurrentColor()) // Удалено, как в оригинале
                    ));
                }
            }
            writer.write("]\n");

            // Станции
            writer.write("stations:[\n");
            for (Station station : world.getStations()) {
                writer.write(String.format(
                        "{id:%d,name:%s,x:%d,y:%d,color:%s,type:%s,constructionDate:%d}\n",
                        station.getUniqueId(),
                        ParsingUtils.escapeString(station.getName()),
                        station.getX(),
                        station.getY(),
                        colorToHex(station.getColor()),
                        station.getType().name(),
                        station.getConstructionDate()
                ));
            }
            writer.write("]\n");

            writer.write("connections:[\n");
            for (Station station : world.getStations()) {
                for (java.util.Map.Entry<Direction, Station> entry : station.getConnections().entrySet()) {
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

            // Игровые юниты (gameplay units)
            writer.write("gameplay_units:[\n");
            for (GameplayUnits gUnits : world.getGameplayUnits()) {
                writer.write(String.format(
                        "{id:%d,name:%s,x:%d,y:%d,type:%s}\n",
                        gUnits.getUniqueId(),
                        ParsingUtils.escapeString(gUnits.getName()),
                        gUnits.getX(),
                        gUnits.getY(),
                        gUnits.getType().name()
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
            for (Label label : world.getLabels()) {
                writer.write(String.format(
                        "{text:%s,x:%d,y:%d,parentStationId:%d}\n",
                        ParsingUtils.escapeString(label.getText()),
                        label.getX(),
                        label.getY(),
                        label.getParentGameObject().getUniqueId()
                ));
            }
            writer.write("]\n");

            // Строительство станций
            writer.write("stationBuild:[\n");
            for (java.util.Map.Entry<Station, Long> entry : world.getConstructionProcessor().stationBuildStartTimes.entrySet()) {
                Station station = entry.getKey();
                writer.write(String.format(
                        "{stationId:%d,start:%d,duration:%d}\n",
                        station.getUniqueId(),
                        entry.getValue(),
                        world.getConstructionProcessor().stationBuildDurations.get(station)
                ));
            }
            writer.write("]\n");

            // Разрушение станций
            writer.write("stationDestroy:[\n");
            for (java.util.Map.Entry<Station, Long> entry : world.getConstructionProcessor().stationDestructionStartTimes.entrySet()) {
                Station station = entry.getKey();
                writer.write(String.format(
                        "{stationId:%d,start:%d,duration:%d}\n",
                        station.getUniqueId(),
                        entry.getValue(),
                        world.getConstructionProcessor().stationDestructionDurations.get(station)
                ));
            }
            writer.write("]\n");

            // Строительство туннелей
            writer.write("tunnelBuild:[\n");
            for (java.util.Map.Entry<Tunnel, Long> entry : world.getConstructionProcessor().tunnelBuildStartTimes.entrySet()) {
                Tunnel tunnel = entry.getKey();
                writer.write(String.format(
                        "{startId:%d,endId:%d,startTime:%d,duration:%d}\n",
                        tunnel.getStart().getUniqueId(),
                        tunnel.getEnd().getUniqueId(),
                        entry.getValue(),
                        world.getConstructionProcessor().tunnelBuildDurations.get(tunnel)
                ));
            }
            writer.write("]\n");

            // Разрушение туннелей
            writer.write("tunnelDestroy:[\n");
            for (java.util.Map.Entry<Tunnel, Long> entry : world.getConstructionProcessor().tunnelDestructionStartTimes.entrySet()) {
                Tunnel tunnel = entry.getKey();
                writer.write(String.format(
                        "{startId:%d,endId:%d,startTime:%d,duration:%d}\n",
                        tunnel.getStart().getUniqueId(),
                        tunnel.getEnd().getUniqueId(),
                        entry.getValue(),
                        world.getConstructionProcessor().tunnelDestructionDurations.get(tunnel)
                ));
            }
            writer.write("]\n");

            // Игровая сетка (gameGrid)
            writer.write("gameGrid:[\n");
            for (int y = 0; y < world.getHeight(); y++) {
                for (int x = 0; x < world.getWidth(); x++) {
                    GameTile tile = world.getGameTile(x,y);
                    GameObject content = tile.getContent();
                    writer.write(String.format(
                            "{x:%d,y:%d,content:%s}\n",
                            x, y,
                            content != null ? ParsingUtils.serializeGameObject(content) : "null"
                    ));
                }
            }
            writer.write("]\n");
        }
    }

    /**
     * Загружает состояние игрового мира из файла.
     * Этот метод делегирует загрузку новому конструктору GameWorld.
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
            // Новый конструктор GameWorld будет обрабатывать весь процесс загрузки
            return new GameWorld(reader);
        }
    }
    public void recreateWorld(BufferedReader reader, GameWorld gameWorld) throws IOException {
        String line;
        Map<Long, Station> stationIdMap = new HashMap<>();

        while ((line = reader.readLine()) != null) {
            line = line.trim(); // Убираем пробелы в начале и конце

            if (line.isEmpty()) continue; // Пропускаем пустые строки

            if (line.startsWith("version:")) {
                continue;
            }
            if (line.startsWith("width:")) {
                gameWorld.setWidth(Short.parseShort(line.substring("width:".length())));
                continue; // Продолжаем, чтобы не попасть в else if
            }
            if (line.startsWith("height:")) {
                gameWorld.setHeight(Short.parseShort(line.substring("height:".length())));
                // Как только получили width и height, инициализируем сетки
                if (gameWorld.getWidth() > 0 && gameWorld.getHeight() > 0) {
                    gameWorld.initWorldGrid(); // Инициализируем worldGrid
                    gameWorld.initGameGrid();  // Инициализируем gameGrid
                }
                continue;
            }
            if (line.startsWith("money:")) {
                gameWorld.money = (int) Float.parseFloat(line.substring("money:".length()));
                continue;
            }
            if (line.startsWith("roundStations:")) {
                gameWorld.roundStationsEnabled = Boolean.parseBoolean(line.substring("roundStations:".length()));
                continue;
            }
            if (line.startsWith("gameTime:")) {
                long time = Long.parseLong(line.substring("gameTime:".length()));
                gameWorld.gameTime = new GameTime();
                // ПредZполагается, что GameTime может быть инициализирован напрямую или имеет сеттер
                gameWorld.gameTime.setCurrentTimeMillis(time); // Если такой метод существует
                continue;
            }

            // --- Обработка секций данных ---

            if (line.equals("worldGrid:[")) {
                // Чтение worldGrid
                while (!(line = reader.readLine()).equals("]")) {
                    line = line.trim();
                    if (!line.startsWith("{") || !line.endsWith("}")) continue;
                    String content = line.substring(1, line.length() - 1);

                    // Используем регулярное выражение для корректного разбиения
                    String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length < 6) {
                        System.err.println("Недостаточно частей при парсинге worldGrid: " + line);
                        continue;
                    }

                    try {
                        int x = Integer.parseInt(ParsingUtils.extractValue(parts[0], "x"));
                        int y = Integer.parseInt(ParsingUtils.extractValue(parts[1], "y"));
                        float perm = Float.parseFloat(ParsingUtils.extractValue(parts[2], "perm"));
                        boolean isWater = Boolean.parseBoolean(ParsingUtils.extractValue(parts[3], "isWater"));
                        float abilityPay = Float.parseFloat(ParsingUtils.extractValue(parts[4], "abilityPay"));
                        int passengerCount = Integer.parseInt(ParsingUtils.extractValue(parts[5], "passengerCount").split("\\.")[0]); // Убираем .00

                        WorldTile tile = gameWorld.getWorldTile(x, y);
                        if (tile != null) {
                            tile.setPerm(perm);
                            tile.setAbilityPay(abilityPay);
                            tile.setWater(isWater);
                            tile.setPassengerCount(passengerCount);
                            // tile.setBaseTileColor(color); // Если цвет сохраняется
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка при парсинге worldGrid: " + line);
                        e.printStackTrace();
                    }
                }
                continue;
            }

            if (line.equals("stations:[")) {
                // Чтение станций
                while (!(line = reader.readLine()).equals("]")) {
                    Station station = ParsingUtils.parseStation(line, gameWorld); // Используем вспомогательный метод
                    if (station != null) {
                        stationIdMap.put(station.getUniqueId(), station);
                        gameWorld.stations.add(station); // Добавляем в список станций мира
                    }
                }
                continue;
            }

            if (line.equals("connections:[")) {
                // Чтение соединений
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConnection(line, gameWorld, stationIdMap); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("tunnels:[")) {
                // Чтение туннелей
                while (!(line = reader.readLine()).equals("]")) {
                    Tunnel tunnel = ParsingUtils.parseTunnel(line, gameWorld, stationIdMap); // Используем вспомогательный метод
                    if (tunnel != null) {
                        gameWorld.tunnels.add(tunnel); // Добавляем в список туннелей мира
                    }
                }
                continue;
            }

            if (line.equals("gameplay_units:[")) {
                // Чтение игровых юнитов
                while (!(line = reader.readLine()).equals("]")) {
                    GameplayUnits gUnits = ParsingUtils.parseGameplayUnit(line, gameWorld); // Используем вспомогательный метод
                    if (gUnits != null) {
                        gameWorld.getGameplayUnits().add(gUnits); // Добавляем в список юнитов мира
                        // Также нужно установить контент в gameGrid, если это необходимо
                        // gameGrid[gUnits.getX()][gUnits.getY()].setContent(gUnits);
                    }
                }
                continue;
            }

            if (line.equals("pathPoints:[")) {
                // Чтение путевых точек для туннелей
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parsePathPoints(line, gameWorld, stationIdMap); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("labels:[")) {
                // Чтение меток
                while (!(line = reader.readLine()).equals("]")) {
                    Label label = ParsingUtils.parseLabel(line, gameWorld, stationIdMap); // Используем вспомогательный метод
                    if (label != null) {
                        gameWorld.labels.add(label); // Добавляем в список меток мира
                        // Также нужно установить контент в gameGrid, если это необходимо
                        // gameGrid[label.getX()][label.getY()].setContent(label);
                    }
                }
                continue;
            }

            if (line.equals("stationBuild:[")) {
                // Чтение данных о строительстве станций
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConstructionData(line, gameWorld, stationIdMap, true, false); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("stationDestroy:[")) {
                // Чтение данных о разрушении станций
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConstructionData(line, gameWorld, stationIdMap, true, true); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("tunnelBuild:[")) {
                // Чтение данных о строительстве туннелей
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConstructionData(line, gameWorld, stationIdMap, false, false); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("tunnelDestroy:[")) {
                // Чтение данных о разрушении туннелей
                while (!(line = reader.readLine()).equals("]")) {
                    ParsingUtils.parseConstructionData(line, gameWorld, stationIdMap, false, true); // Используем вспомогательный метод
                }
                continue;
            }

            if (line.equals("gameGrid:[")) {
                // Чтение gameGrid (содержимое клеток)
                while (!(line = reader.readLine()).equals("]")) {
                    line = line.trim();
                    if (!line.startsWith("{") || !line.endsWith("}")) continue;
                    String content = line.substring(1, line.length() - 1);
                    String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length < 3) {
                        System.err.println("Недостаточно частей при парсинге gameGrid: " + line);
                        continue;
                    }

                    try {
                        int x = Integer.parseInt(ParsingUtils.extractValue(parts[0], "x"));
                        int y = Integer.parseInt(ParsingUtils.extractValue(parts[1], "y"));
                        String contentStr = ParsingUtils.extractValue(parts[2], "content");

                        if (!contentStr.equals("null")) {
                            GameObject obj = ParsingUtils.parseGameObject(contentStr, gameWorld, stationIdMap); // Используем вспомогательный метод
                            if (obj != null && gameWorld.gameGrid != null && x < gameWorld.getWidth() && y < gameWorld.getHeight()) {
                                gameWorld.getGameplayTile(x,y).setContent(obj);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка при парсинге gameGrid: " + line);
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}


