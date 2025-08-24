package metroline.objects.gameobjects;

import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.objects.enums.Direction;
import metroline.objects.enums.StationType;
import metroline.objects.enums.TrainType;
import metroline.objects.enums.TunnelType;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

public class Train extends GameObject {
    private Station currentStation;
    private Tunnel currentTunnel;
    private Tunnel previousTunnel;
    private List<PathPoint> currentPath;
    public TrainType trainType;
    private float pathProgress; // Прогресс вдоль всего пути (0.0 to 1.0)
    private float currentX;
    private float currentY;
    private boolean movingForward = true;
    private Color color;

    private Direction direction;
    private float waitTimer;
    private boolean hasPaidAtCurrentStation = false; // Флаг для отслеживания оплаты
    private static final float STATION_WAIT_TIME = 600.0f; // 5 секунд стоянки


    private transient BufferedImage cachedTrainImage;
    private transient Color cachedColor;
    private transient TrainType cachedTrainType;
    private transient float cachedZoom = -1f;

    public Train(World world, Station spawnStation, TrainType trainType) {
        super(world, spawnStation.getX(), spawnStation.getY());
        this.currentStation = spawnStation;
        this.color = spawnStation.getColor();
        this.name = "train_" + getUniqueId();
        this.currentX = spawnStation.getX();
        this.currentY = spawnStation.getY();
        this.trainType = trainType;
        this.waitTimer = STATION_WAIT_TIME; // Используем константу
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void update(long deltaTime) {
        float deltaSeconds = deltaTime / 1_000_000_000.0f;
        float gameTimeMultiplier = (float) getWorld().getGameTime().getTimeScale();

        if(getWorld().getGameTime().isPaused()) {
            return;
        }
        if (currentTunnel != null) {
            moveAlongTunnel(deltaSeconds * gameTimeMultiplier, getTrainType());
        } else if (currentStation != null) {
            handleStationWait(deltaSeconds * gameTimeMultiplier);
        }

        this.x = Math.round(currentX);
        this.y = Math.round(currentY);
    }

    private void moveAlongTunnel(float deltaSeconds, TrainType trainType) {
        if (currentPath == null || currentPath.size() < 2) return;

        float totalPathLength = calculateTotalPathLength();
        if (totalPathLength == 0) return;
        float distanceToMove;

        distanceToMove = trainType.getSpeed() * deltaSeconds ;


        if (movingForward) {
            pathProgress += distanceToMove / totalPathLength;
            if (pathProgress >= 1.0f) {
                reachTunnelEnd();
                return;
            }
        } else {
            pathProgress -= distanceToMove / totalPathLength;
            if (pathProgress <= 0.0f) {
                reachTunnelEnd();
                return;
            }
        }

        updatePositionFromPathProgress();
    }

    private float calculateTotalPathLength() {
        float totalLength = 0;
        for (int i = 0; i < currentPath.size() - 1; i++) {
            PathPoint p1 = currentPath.get(i);
            PathPoint p2 = currentPath.get(i + 1);
            totalLength += (float) Math.sqrt(
                    Math.pow(p2.getX() - p1.getX(), 2) +
                            Math.pow(p2.getY() - p1.getY(), 2)
            );
        }
        return totalLength;
    }

    private void updatePositionFromPathProgress() {
        if (currentPath == null || currentPath.size() < 2) return;

        float targetDistance = pathProgress * calculateTotalPathLength();
        float accumulatedDistance = 0;

        for (int i = 0; i < currentPath.size() - 1; i++) {
            PathPoint p1 = currentPath.get(i);
            PathPoint p2 = currentPath.get(i + 1);

            float segmentLength = (float) Math.sqrt(
                    Math.pow(p2.getX() - p1.getX(), 2) +
                            Math.pow(p2.getY() - p1.getY(), 2)
            );

            if (accumulatedDistance + segmentLength >= targetDistance) {
                float segmentProgress = (targetDistance - accumulatedDistance) / segmentLength;

                currentX = p1.getX() + (p2.getX() - p1.getX()) * segmentProgress;
                currentY = p1.getY() + (p2.getY() - p1.getY()) * segmentProgress;

                updateDirection(p1, p2);
                return;
            }

            accumulatedDistance += segmentLength;
        }

        PathPoint lastPoint = currentPath.get(currentPath.size() - 1);
        currentX = lastPoint.getX();
        currentY = lastPoint.getY();
    }

    private void handleStationWait(float deltaSeconds) {
        // Добавляем доход при прибытии на станцию (только один раз)
        if (!hasPaidAtCurrentStation) {
            addTrainOnStationRevenue();
            hasPaidAtCurrentStation = true;
        }

        waitTimer -= deltaSeconds;

        // Если время стоянки закончилось, пытаемся найти следующий туннель
        if (waitTimer <= 0) {
            Station nextStation = getNextStation();

            // Проверяем, свободна ли следующая станция
            if (nextStation != null && isNextStationOccupied(nextStation)) {
                // Станция занята - ждем еще
                waitTimer = 1.0f; // Ждем дополнительное короткое время
            } else {
                // Станция свободна или следующая станция не определена - едем
                findNextTunnel();
            }
        }
    }

    private boolean isNextStationOccupied(Station nextStation) {
        if (nextStation == null) return false;

        GameWorld world = (GameWorld) getWorld();
        if (world == null) return false;

        // Проверяем все поезда в мире
        for (GameObject obj : world.getGameplayUnits()) {
            if (obj instanceof Train) {
                Train otherTrain = (Train) obj;
                // Если поезд стоит на целевой станции
                if (otherTrain.getCurrentStation() == nextStation && otherTrain != this) {
                    return true;
                }
            }
        }
        return false;
    }

    private Station getNextStation() {
        if (currentStation == null) return null;

        List<Tunnel> connectedTunnels = getWorld().getTunnels().stream()
                                                  .filter(t -> t.getType() == TunnelType.ACTIVE)
                                                  .filter(t -> t.getStart() == currentStation || t.getEnd() == currentStation)
                                                  .collect(Collectors.toList());

        if (connectedTunnels.isEmpty()) return null;

        // Выбираем следующий туннель (аналогично логике findNextTunnel)
        List<Tunnel> availableTunnels;

        switch (currentStation.getType()) {
            case TERMINAL:
                availableTunnels = connectedTunnels.stream()
                                                   .filter(t -> t == previousTunnel)
                                                   .collect(Collectors.toList());
                break;

            case TRANSFER:
            case TRANSIT:
                availableTunnels = connectedTunnels.stream()
                                                   .filter(t -> t != previousTunnel)
                                                   .collect(Collectors.toList());
                break;

            default:
                availableTunnels = connectedTunnels;
                break;
        }

        if (availableTunnels.isEmpty()) {
            availableTunnels = connectedTunnels;
        }

        if (availableTunnels.isEmpty()) return null;

        Tunnel nextTunnel = availableTunnels.get((int)(Math.random() * availableTunnels.size()));
        boolean movingForward = nextTunnel.getStart() == currentStation;

        return movingForward ? nextTunnel.getEnd() : nextTunnel.getStart();
    }

    private void addTrainOnStationRevenue() {
        if (currentStation != null) {
            // Получаем игровую экономику и добавляем доход
            int revenue = calculateRevenue();
            if(currentStation.getWorld() instanceof GameWorld w) {
                w.addMoney(revenue);
            }
        }
    }

    private int calculateRevenue() {
        // Базовая стоимость + бонусы в зависимости от типа станции
        int baseRevenue = 10;

        if (currentStation.getType() == StationType.TRANSFER) {
            baseRevenue += 5; // Бонус за пересадочную станцию
        } else if (currentStation.getType() == StationType.TERMINAL) {
            baseRevenue += 3; // Бонус за конечную станцию
        }

        // Можно добавить другие модификаторы (уровень станции, подключенные линии и т.д.)
        return baseRevenue;
    }

    private void updateDirection(PathPoint from, PathPoint to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();

        if (dx > 0 && dy == 0) direction = Direction.EAST;
        else if (dx < 0 && dy == 0) direction = Direction.WEST;
        else if (dx == 0 && dy > 0) direction = Direction.SOUTH;
        else if (dx == 0 && dy < 0) direction = Direction.NORTH;
        else if (dx > 0 && dy > 0) direction = Direction.SOUTHEAST;
        else if (dx > 0 && dy < 0) direction = Direction.NORTHEAST;
        else if (dx < 0 && dy > 0) direction = Direction.SOUTHWEST;
        else if (dx < 0 && dy < 0) direction = Direction.NORTHWEST;
    }

    private void reachTunnelEnd() {
        Station reachedStation = movingForward ? currentTunnel.getEnd() : currentTunnel.getStart();

        previousTunnel = currentTunnel;
        currentX = reachedStation.getX();
        currentY = reachedStation.getY();
        currentStation = reachedStation;
        currentTunnel = null;
        currentPath = null;
        waitTimer = STATION_WAIT_TIME;
        pathProgress = movingForward ? 1.0f : 0.0f;
        hasPaidAtCurrentStation = false; // Сбрасываем флаг оплаты при прибытии на новую станцию
    }

    private void findNextTunnel() {
        if (currentStation == null) return;

        List<Tunnel> connectedTunnels = getWorld().getTunnels().stream()
                                                  .filter(t -> t.getType() == TunnelType.ACTIVE)
                                                  .filter(t -> t.getStart() == currentStation || t.getEnd() == currentStation)
                                                  .collect(Collectors.toList());

        if (connectedTunnels.isEmpty()) {
            waitTimer = 1.0f;
            return;
        }

        List<Tunnel> availableTunnels;

        switch (currentStation.getType()) {
            case TERMINAL:
                availableTunnels = connectedTunnels.stream()
                                                   .filter(t -> t == previousTunnel)
                                                   .collect(Collectors.toList());
                break;

            case TRANSFER:
            case TRANSIT:
                availableTunnels = connectedTunnels.stream()
                                                   .filter(t -> t != previousTunnel)
                                                   .collect(Collectors.toList());
                break;

            default:
                availableTunnels = connectedTunnels;
                break;
        }

        if (availableTunnels.isEmpty()) {
            availableTunnels = connectedTunnels;
        }

        // Выбираем туннель и проверяем, свободна ли следующая станция
        Tunnel selectedTunnel = availableTunnels.get((int)(Math.random() * availableTunnels.size()));
        boolean movingForward = selectedTunnel.getStart() == currentStation;
        Station nextStation = movingForward ? selectedTunnel.getEnd() : selectedTunnel.getStart();

        // Дополнительная проверка: если следующая станция занята, ждем
        if (isNextStationOccupied(nextStation)) {
            waitTimer = 1.0f; // Ждем дополнительное короткое время
            return;
        }

        // Все проверки пройдены - начинаем движение
        currentTunnel = selectedTunnel;
        this.movingForward = movingForward;
        currentPath = currentTunnel.getPath();
        pathProgress = movingForward ? 0.0f : 1.0f;

        currentStation = null;
        updatePositionFromPathProgress();
    }
    @Override
    public void draw(Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        if (getWorld() == null) return;

        float screenX = (currentX * 32 + offsetX + 16) * zoom;
        float screenY = (currentY * 32 + offsetY + 16) * zoom;

        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(screenX, screenY);
        g2d.rotate(getRotationAngle());

        // Получаем или создаем кэшированное изображение
        BufferedImage trainImage = getCachedTrainImage(zoom);
        if (trainImage != null) {
            // Центрируем изображение
            g2d.drawImage(trainImage, -trainImage.getWidth()/2, -trainImage.getHeight()/2, null);
        } else {
            // Fallback: рисуем динамически если кэш недоступен
            drawDynamic(g2d,offsetX,offsetY, zoom);
        }

        g2d.setTransform(oldTransform);
    }   private BufferedImage getCachedTrainImage(float zoom) {
        // Проверяем, нужно ли обновить кэш
        if (cachedTrainImage == null ||
                !color.equals(cachedColor) ||
                trainType != cachedTrainType ||
                Math.abs(zoom - cachedZoom) > 0.01f) {

            createTrainImageCache(zoom);
        }
        return cachedTrainImage;
    }

    private void createTrainImageCache(float zoom) {
        int trainWidth = (int)(24 * zoom);
        int trainHeight = (int)(18 * zoom);
        int shadowSizeIncrease = (int)(2 * zoom);

        // Создаем изображение с достаточным местом для тени
        int imageWidth = trainWidth + shadowSizeIncrease * 4;
        int imageHeight = trainHeight + shadowSizeIncrease * 4;

        cachedTrainImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = cachedTrainImage.createGraphics();

        // Настраиваем качество
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Смещаем начало координат в центр изображения
        g2d.translate(imageWidth/2, imageHeight/2);

        // 1. Рисуем тень
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g2d.setColor(new Color(0, 0, 0, 60));

        for (int i = 0; i < 3; i++) {
            int currentIncrease = shadowSizeIncrease + i * 2;
            float alpha = 0.15f - i * 0.04f;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.fillRect(
                    -trainWidth/2 - currentIncrease/2,
                    -trainHeight/2 - currentIncrease/2,
                    trainWidth + currentIncrease,
                    trainHeight + currentIncrease
            );
        }

        // 2. Основной корпус
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setColor(color);
        g2d.fillRect(-trainWidth/2, -trainHeight/2, trainWidth, trainHeight);



        g2d.dispose();

        // Сохраняем параметры кэша
        cachedColor = color;
        cachedTrainType = trainType;
        cachedZoom = zoom;
    }
    private void drawDynamic(Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        if (getWorld() == null) return;

        float screenX = (currentX * 32 + offsetX + 16) * zoom;
        float screenY = (currentY * 32 + offsetY + 16) * zoom;

        int trainWidth = (int)(24 * zoom);
        int trainHeight = (int)(18 * zoom);
        int shadowSizeIncrease = (int)(2 * zoom); // Увеличение размера для тени

        AffineTransform oldTransform = g2d.getTransform();

        g2d.translate(screenX, screenY);
        g2d.rotate(getRotationAngle());
        g2d.translate(-trainWidth/2, -trainHeight/2);

        // Включаем сглаживание
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Размытая тень - рисуем несколько полупрозрачных прямоугольников
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g2d.setColor(new Color(0, 0, 0, 60));

        // Рисуем несколько слоев для эффекта размытия
        for (int i = 0; i < 3; i++) {
            int currentIncrease = shadowSizeIncrease + i * 2;
            float alpha = 0.15f - i * 0.04f;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.fillRect(
                    -currentIncrease/2,
                    -currentIncrease/2,
                    trainWidth + currentIncrease,
                    trainHeight + currentIncrease
            );
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // 2. Основной корпус
        g2d.setColor(color);
        g2d.fillRect(0, 0, trainWidth, trainHeight);

        // 3. Основная обводка
        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke( zoom));
        g2d.drawRect(0, 0, trainWidth, trainHeight);

        // Восстанавливаем оригинальные настройки
        g2d.setTransform(oldTransform);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    public TrainType getTrainType() {
        return trainType;
    }
    private double getRotationAngle() {
        if (direction == null) return 0;

        switch (direction) {
            case EAST: return 0;
            case WEST: return Math.PI;
            case NORTH: return -Math.PI/2;
            case SOUTH: return Math.PI/2;
            case NORTHEAST: return -Math.PI/4;
            case SOUTHEAST: return Math.PI/4;
            case NORTHWEST: return -3*Math.PI/4;
            case SOUTHWEST: return 3*Math.PI/4;
            default: return 0;
        }
    }

    public boolean isMoving() {
        return currentTunnel != null;
    }

    public Station getCurrentStation() {
        return currentStation;
    }

    public boolean isAtStation() {
        return currentStation != null;
    }


}