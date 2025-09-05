package metroline.objects.gameobjects;

import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.input.selection.SelectionManager;
import metroline.objects.enums.*;
import metroline.util.MetroLogger;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
// FIX меню выбора поездов накладывается поверх тултайпа когда всплывающая панель выходит за рамки окна
// FIX иногда NPE по выбору туннеля в currentTunnel В (java.lang.NullPointerException: Cannot invoke "metroline.objects.gameobjects.Tunnel.setCurrentTrain(metroline.objects.gameobjects.Train)" because "this.currentTunnel" is null)
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

    private float currentSpeed = 0.0f;
    private float targetSpeed = 0.0f;
    private boolean isAccelerating = false;
    private boolean isBraking = false;

    // Коэффициенты ускорения/торможения (относительно базовой скорости)
    private static final float ACCELERATION_FACTOR = 0.8f; // ускорение (доли от макс. скорости в секунду)
    private static final float DECELERATION_FACTOR = 1.2f; // торможение (доли от макс. скорости в секунду)
    private static final float CRUISING_SPEED_FACTOR = 0.9f; // Крейсерская скорость относительно макс.
    private static final float CURVE_SPEED_PENALTY_MAX = 0.4f; // Максимальное замедление на поворотах
    private static final float STATION_APPROACH_SPEED = 0.3f; // Скорость при подходе к станции


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


        // Обновляем скорость перед движением
        updateSpeed(deltaSeconds * gameTimeMultiplier);
        if (currentTunnel != null) {
            moveAlongTunnel(deltaSeconds * gameTimeMultiplier);
        } else if (currentStation != null) {
            handleStationWait(deltaSeconds * gameTimeMultiplier);
        }

        this.x = Math.round(currentX);
        this.y = Math.round(currentY);


    }

    public Tunnel getCurrentTunnel() {
        return currentTunnel;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    private void updateSpeed(float deltaSeconds) {
        float baseSpeed = trainType.getSpeed();

        if (currentTunnel != null) {
            // В туннеле - ускоряемся до целевой скорости с учетом типа поезда
            float maxPossibleSpeed = calculateMaxPossibleSpeed();
            targetSpeed = maxPossibleSpeed * CRUISING_SPEED_FACTOR;

            float accelerationRate = baseSpeed * ACCELERATION_FACTOR;
            float decelerationRate = baseSpeed * DECELERATION_FACTOR;

            if (currentSpeed < targetSpeed) {
                // Разгон - более быстрые поезда разгоняются быстрее
                currentSpeed = Math.min(currentSpeed + accelerationRate * deltaSeconds, targetSpeed);
                isAccelerating = true;
                isBraking = false;
            } else if (currentSpeed > targetSpeed) {
                // Торможение до крейсерской скорости
                currentSpeed = Math.max(currentSpeed - decelerationRate * deltaSeconds, targetSpeed);
                isAccelerating = false;
                isBraking = true;
            } else {
                // Движение с постоянной скоростью
                isAccelerating = false;
                isBraking = false;
            }
        } else if (currentStation != null) {
            // На станции - тормозим до полной остановки
            targetSpeed = 0.0f;
            float decelerationRate = baseSpeed * DECELERATION_FACTOR * 1.5f; // Усиленное торможение на станции

            if (currentSpeed > 0) {
                currentSpeed = Math.max(currentSpeed - decelerationRate * deltaSeconds, 0.0f);
                isAccelerating = false;
                isBraking = true;
            } else {
                currentSpeed = 0.0f;
                isAccelerating = false;
                isBraking = false;
            }
        }
    }
    private float calculateMaxPossibleSpeed() {
        float baseSpeed = trainType.getSpeed();

        // Учет изгибов туннеля (замедление на поворотах)
        float curvaturePenalty = calculateCurvaturePenalty();
        float curveSpeedModifier = 1.0f - curvaturePenalty;

        return baseSpeed * curveSpeedModifier;
    }

    private float calculateCurvaturePenalty() {
        if (currentPath == null || currentPath.size() < 3) return 0.0f;

        // Более быстрые поезда сильнее страдают от поворотов
        float baseSpeed = trainType.getSpeed();
        float sensitivity = Math.min(baseSpeed / 0.05f, 2.0f); // Чувствительность к поворотам

        float totalAngleChange = 0.0f;
        int segments = Math.min(5, currentPath.size() - 2);

        for (int i = 0; i < segments; i++) {
            PathPoint p1 = currentPath.get(i);
            PathPoint p2 = currentPath.get(i + 1);
            PathPoint p3 = currentPath.get(i + 2);

            float angle = calculateAngleBetweenVectors(p1, p2, p3);
            totalAngleChange += Math.abs(angle);
        }

        // Нормализуем и применяем чувствительность
        float normalizedPenalty = totalAngleChange / 180.0f;
        float effectivePenalty = normalizedPenalty * sensitivity;

        return Math.min(effectivePenalty * CURVE_SPEED_PENALTY_MAX, CURVE_SPEED_PENALTY_MAX);
    }

    private float calculateAngleBetweenVectors(PathPoint p1, PathPoint p2, PathPoint p3) {
        float dx1 = p2.getX() - p1.getX();
        float dy1 = p2.getY() - p1.getY();

        float dx2 = p3.getX() - p2.getX();
        float dy2 = p3.getY() - p2.getY();

        float dot = dx1 * dx2 + dy1 * dy2;
        float mag1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
        float mag2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);

        if (mag1 == 0 || mag2 == 0) return 0.0f;

        float cosAngle = dot / (mag1 * mag2);
        cosAngle = Math.max(-1.0f, Math.min(1.0f, cosAngle));

        return (float) Math.toDegrees(Math.acos(cosAngle));
    }


    private void moveAlongTunnel(float deltaSeconds) {
        if (currentPath == null || currentPath.size() < 2) return;
        if (currentTunnel != null && currentTunnel.getCurrentTrain() != this) {
            currentTunnel.setCurrentTrain(this);
        }

        float totalPathLength = calculateTotalPathLength();
        if (totalPathLength == 0) return;

        // Проверяем, нужно ли начинать торможение перед станцией
        if (isApproachingStation(totalPathLength)) {
            beginStationApproach();
        }

        float distanceToMove = currentSpeed * deltaSeconds;

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

    public float getPathProgress() {
        return pathProgress;
    }

    private boolean isApproachingStation(float totalPathLength) {
        if (currentTunnel == null) return false;

        // Определяем расстояние до конечной станции
        float distanceToEnd;
        if (movingForward) {
            distanceToEnd = (1.0f - pathProgress) * totalPathLength;
        } else {
            distanceToEnd = pathProgress * totalPathLength;
        }

        // Расстояние для начала торможения зависит от скорости поезда
        float brakingDistance = calculateBrakingDistance();

        return distanceToEnd <= brakingDistance;
    }

    private float calculateBrakingDistance() {
        float baseSpeed = trainType.getSpeed();
        // Более быстрые поезда требуют большего расстояния для торможения
        return baseSpeed * baseSpeed * 2.0f; // Квадратичная зависимость
    }

    private void beginStationApproach() {
        float baseSpeed = trainType.getSpeed();
        float approachSpeed = baseSpeed * STATION_APPROACH_SPEED;

        if (currentSpeed > approachSpeed) {
            targetSpeed = approachSpeed;
            isAccelerating = false;
            isBraking = true;
        }
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public float getNormalizedSpeed() {
        return currentSpeed / trainType.getSpeed();
    }

    public float getTargetSpeed() {
        return targetSpeed;
    }

    public boolean isAccelerating() {
        return isAccelerating;
    }

    public boolean isBraking() {
        return isBraking;
    }
    public float getAccelerationProgress() {
        if (targetSpeed == 0) return 0.0f;
        return Math.min(currentSpeed / targetSpeed, 1.0f);
    }

    public void emergencyBrake() {
        targetSpeed = 0.0f;
        isBraking = true;
        isAccelerating = false;
    }

    public void startSmoothAcceleration() {
        if (currentTunnel != null) {
            targetSpeed = calculateMaxPossibleSpeed() * CRUISING_SPEED_FACTOR;
            isAccelerating = true;
            isBraking = false;
        }
    }

    private float calculateTotalPathLength() {
        if (currentPath == null || currentPath.size() < 2) {
            MetroLogger.logWarning("Attempt to calculate path length for null or empty path");
            return 0f;
        }
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

    public void updatePositionFromPathProgress() {
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

        // Если время стоянки закончилось, пытаемся найти следующий туннель
        if (waitTimer > 0) {
            waitTimer -= deltaSeconds;
        }

        // Когда время ожидания истекло ИЛИ мы на невалидной станции (waitTimer = 0)
        if (waitTimer <= 0) {
            findNextTunnel(); // Будет вызываться каждый кадр, пока не найдет свободный путь
        }
    }
    private void addTrainOnStationRevenue() {
        if (currentStation != null && isStationValid(currentStation)) {
            // ПРОВЕРЬТЕ, что getWorld() возвращает тот же GameWorld
            if (getWorld() instanceof GameWorld) {
                GameWorld gameWorld = (GameWorld) getWorld();

                // ПРОВЕРЬТЕ, что economyManager не null
                if (gameWorld.getEconomyManager() != null) {
                    float revenue = gameWorld.getEconomyManager().calculateStationRevenue(currentStation);
                    gameWorld.addMoney(revenue);
                } else {
                    MetroLogger.logError("EconomyManager is null in GameWorld!");
                }
            }
        }
    }
    public void removeFromStation() {
        if (currentStation != null) {
            currentStation.clearTrain();
            currentStation = null;
        }
    }
    private boolean isNextStationOccupied(Station nextStation) {
        if (nextStation == null) return false;

        // Проверяем, есть ли уже поезд на станции
        if (nextStation.hasTrain() && nextStation.getCurrentTrain() != this) {
            return true;
        }

        return false;
    }
    public float getCurrentX() {
        return currentX;
    }

    public float getCurrentY() {
        return currentY;
    }

    public boolean isStationValid(Station station) {
        if(station.getType() == StationType.TRANSFER || station.getType() == StationType.REGULAR  || station.getType() == StationType.TRANSIT
        || station.getType() == StationType.TERMINAL) return true; //!isNextStationOccupied(station);
        else return false;
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
        if (currentTunnel == null) return;
        Station reachedStation = movingForward ? currentTunnel.getEnd() : currentTunnel.getStart();
        // Освобождаем туннель
        if (currentTunnel != null) {
            currentTunnel.clearTrain();
        }
        // Уведомляем предыдущую станцию об отправлении поезда
        if (currentStation != null) {
            currentStation.clearTrain();
        }

        // Уведомляем новую станцию о прибытии поезда
        reachedStation.setCurrentTrain(this);
        previousTunnel = currentTunnel;
        currentX = reachedStation.getX();
        currentY = reachedStation.getY();
        currentStation = reachedStation;
        currentTunnel = null;
        currentPath = null;

        if (reachedStation.getType() == StationType.TRANSIT || reachedStation.getType() == StationType.TRANSFER ||
        reachedStation.getType() == StationType.TERMINAL  ) {
            waitTimer = STATION_WAIT_TIME; // Нормальное время ожидания для валидных станций
            hasPaidAtCurrentStation = false;

        } else {
            waitTimer = 0; // Нулевое время ожидания для невалидных станций
            hasPaidAtCurrentStation = true;

        }

        pathProgress = movingForward ? 1.0f : 0.0f;
        currentSpeed = 0.0f;
        targetSpeed = 0.0f;
        isAccelerating = false;
        isBraking = false;
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

        // Фильтруем доступные туннели по типу станции
        List<Tunnel> availableTunnels = filterTunnelsByStationType(connectedTunnels);

        if (availableTunnels.isEmpty()) {
            availableTunnels = connectedTunnels; // Fallback
        }

        // Перемешиваем для случайного выбора
        Collections.shuffle(availableTunnels);

        // Ищем первый свободный туннель и станцию
        for (Tunnel candidateTunnel : availableTunnels) {
            boolean movingForward = candidateTunnel.getStart() == currentStation;
            Station nextStation = movingForward ? candidateTunnel.getEnd() : candidateTunnel.getStart();

            // Проверяем доступность туннеля и станции
            if (isPathClear(candidateTunnel, nextStation)) {
                // Нашли свободный путь - начинаем движение
                startMovement(candidateTunnel, movingForward, nextStation);
                return;
            }
        }

        // Все пути заняты - ждем
        waitTimer = 0.5f;
    }

    /**
     * Фильтрует туннели по типу текущей станции
     */
    private List<Tunnel> filterTunnelsByStationType(List<Tunnel> connectedTunnels) {
        if (currentStation == null) {
            return Collections.emptyList(); // или tunnels, если логика позволяет
        }
        switch (currentStation.getType()) {
            case TERMINAL:
                // Для терминалов - только предыдущий туннель (возврат)
                return connectedTunnels.stream()
                                       .filter(t -> t == previousTunnel)
                                       .collect(Collectors.toList());

            case TRANSFER:
            case TRANSIT:
                // Для транзитных - исключаем предыдущий туннель (движение вперед)
                return connectedTunnels.stream()
                                       .filter(t -> t != previousTunnel)
                                       .collect(Collectors.toList());

            case REGULAR:
            default:
                // Для обычных - предпочитаем движение вперед, но разрешаем возврат если нет других вариантов
                List<Tunnel> forwardTunnels = connectedTunnels.stream()
                                                              .filter(t -> t != previousTunnel)
                                                              .collect(Collectors.toList());
                return !forwardTunnels.isEmpty() ? forwardTunnels : connectedTunnels;
        }
    }

    /**
     * Проверяет, свободен ли путь (туннель и станция)
     */
    private boolean isPathClear(Tunnel tunnel, Station nextStation) {
        // Проверяем туннель
        if (tunnel.hasTrain() && tunnel.getCurrentTrain() != this) {
            Train trainInTunnel = tunnel.getCurrentTrain();

            // Если поезд в туннеле движется В НАПРАВЛЕНИИ следующей станции - нельзя ехать
            if (isTrainMovingTowardsStation(trainInTunnel, nextStation)) {
                return false;
            }

        }

        // Проверяем станцию (для терминалов разрешаем занятость)
        if (nextStation.hasTrain() && nextStation.getCurrentTrain() != this && !nextStation.isTerminal()
        && nextStation.getType() != StationType.TRANSIT && nextStation.getType() != StationType.TRANSFER) {
            // Для терминалов проверяем, что занят именно этот поезд (ждет отправления)
            Train stationTrain = nextStation.getCurrentTrain();
            if (stationTrain != null && stationTrain != this &&
                    stationTrain.getCurrentStation() == nextStation) {
                return false;
            }
        }

        return true;
    }
    /**
     * Проверяет, движется ли поезд в направлении указанной станции
     */
    private boolean isTrainMovingTowardsStation(Train train, Station targetStation) {
        if (train == null || targetStation == null || !train.isMoving()) {
            return false;
        }

        // Получаем текущий туннель поезда
        Tunnel trainTunnel = train.getCurrentTunnel();
        if (trainTunnel == null) {
            return false;
        }

        // Определяем, в какую сторону движется поезд
        boolean trainMovingForward = train.isMovingForward();
        Station trainTargetStation = trainMovingForward ? trainTunnel.getEnd() : trainTunnel.getStart();

        // Проверяем, совпадает ли целевая станция поезда с нашей целевой станцией
        return trainTargetStation == targetStation;
    }
    /**
     * Начинает движение по выбранному туннелю
     */
    private void startMovement(Tunnel tunnel, boolean movingForward, Station nextStation) {
        currentTunnel = tunnel;
        this.movingForward = movingForward;
        currentPath = tunnel.getPath();
        pathProgress = movingForward ? 0.0f : 1.0f;

        // Уведомляем туннель о въезде
        tunnel.setCurrentTrain(this);

        // Уведомляем текущую станцию об отправлении
        if (currentStation != null) {
            currentStation.clearTrain();
        }

        currentStation = null;
        updatePositionFromPathProgress();
    }



    // Метод для получения информации о скорости для отладки
    public String getSpeedInfo() {
        return String.format("%.1f/%.1f u/s (%.0f%%)",
                currentSpeed,
                trainType.getSpeed(),
                getNormalizedSpeed() * 100
        );
    }
    public boolean isSelected() {
        return SelectionManager.getInstance().isSelected(this);
    }
    @Override
    public void draw(Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        if (getWorld() == null) return;

        float screenX = (currentX * 32 + offsetX + 16) * zoom;
        float screenY = (currentY * 32 + offsetY + 16) * zoom;

        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(screenX, screenY);
        g2d.rotate(getRotationAngle());
        if (isSelected()) {
            drawSelection(g2d, zoom);
        }

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
    /**
     * Отрисовка выделения поезда
     */
    private void drawSelection(Graphics2D g2d, float zoom) {
        int selectionSize = (int)(30 * zoom);
        int trainWidth = (int)(24 * zoom);
        int trainHeight = (int)(18 * zoom);

        g2d.setColor(new Color(255, 255, 0, 100)); // Полупрозрачный желтый
        g2d.setStroke(new BasicStroke(2 * zoom));
        g2d.drawRect(-trainWidth/2 - 3, -trainHeight/2 - 3,
                trainWidth + 6, trainHeight + 6);

        // Анимированное выделение
        long time = System.currentTimeMillis();
        float pulse = (float)(Math.sin(time * 0.005) * 0.3 + 0.7);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse * 0.3f));
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(-trainWidth/2 - 5, -trainHeight/2 - 5,
                trainWidth + 10, trainHeight + 10);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
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

        // 3. Рисуем отличительные знаки в зависимости от типа поезда
        drawTrainTypeFeatures(g2d, trainWidth, trainHeight, zoom);

        g2d.dispose();

        // Сохраняем параметры кэша
        cachedColor = color;
        cachedTrainType = trainType;
        cachedZoom = zoom;
    }
    private void drawTrainTypeFeatures(Graphics2D g2d, int trainWidth, int trainHeight, float zoom) {
        //     Color featureColor = color.darker();
        Color featureColor = new Color(210, 182, 20, 200);
        g2d.setColor(featureColor);

        int padding = (int)(4 * zoom); // отступ от краев
        int stripeWidth = (int)(3 * zoom); // одинаковая толщина для всех полосок
        int stripeLength = trainWidth - 2 * padding;

        switch (trainType) {
            case FIRST:
                // Оставляем как есть - без дополнительных элементов
                break;

            case OLD:
                // Одна продольная полоска по центру
                g2d.fillRect(-stripeLength / 2, -stripeWidth / 2, stripeLength, stripeWidth);
                break;

            case CLASSIC:
                // Две продольные полоски - делим высоту на 3 равные части
                int classicSectionHeight = trainHeight / 3;
                int classicY1 = -trainHeight / 2 + classicSectionHeight - stripeWidth / 2;
                int classicY2 = -trainHeight / 2 + 2 * classicSectionHeight - stripeWidth / 2;

                g2d.fillRect(-stripeLength / 2, classicY1, stripeLength, stripeWidth);
                g2d.fillRect(-stripeLength / 2, classicY2, stripeLength, stripeWidth);
                break;

            case MODERN:
                // Три продольные полоски - делим высоту на 4 равные части
                int modernSectionHeight = trainHeight / 4;
                int modernY1 = -trainHeight / 2 + modernSectionHeight - stripeWidth / 2;
                int modernY2 = -trainHeight / 2 + 2 * modernSectionHeight - stripeWidth / 2;
                int modernY3 = -trainHeight / 2 + 3 * modernSectionHeight - stripeWidth / 2;

                g2d.fillRect(-stripeLength / 2, modernY1, stripeLength, stripeWidth);
                g2d.fillRect(-stripeLength / 2, modernY2, stripeLength, stripeWidth);
                g2d.fillRect(-stripeLength / 2, modernY3, stripeLength, stripeWidth);
                break;

            case NEW:
                // Один круг в центре (больше)
                int newCircleSize = (int) (8 * zoom);
                g2d.fillOval(-newCircleSize / 2, -newCircleSize / 2, newCircleSize, newCircleSize);
                break;

            case NEWEST:
                // Два круга симметрично (больше)
                int newestCircleSize = (int) (6 * zoom);
                int newestCircleSpacing = (int) (8 * zoom);

                g2d.fillOval(-newestCircleSpacing - newestCircleSize / 2, -newestCircleSize / 2,
                        newestCircleSize, newestCircleSize);
                g2d.fillOval(newestCircleSpacing - newestCircleSize / 2, -newestCircleSize / 2,
                        newestCircleSize, newestCircleSize);
                break;

            case FUTURISTIC:
                // Три круга
                int futuristicCircleSize = (int) (6 * zoom);
                int futuristicCircleSpacing = (int) (8 * zoom);

                // Центральный круг
                g2d.fillOval(-futuristicCircleSize / 2, -futuristicCircleSize / 2,
                        futuristicCircleSize, futuristicCircleSize);
                // Левый круг
                g2d.fillOval(-futuristicCircleSpacing - futuristicCircleSize / 2, -futuristicCircleSize / 2,
                        futuristicCircleSize, futuristicCircleSize);
                // Правый круг
                g2d.fillOval(futuristicCircleSpacing - futuristicCircleSize / 2, -futuristicCircleSize / 2,
                        futuristicCircleSize, futuristicCircleSize);
                break;

            case FAR_FUTURISTIC:
                // Треугольник, направленный вперед
                int triangleWidth = (int)(12 * zoom);   // ширина основания треугольника
                int triangleHeight = (int)(10 * zoom);  // высота треугольника
                int triangleOffset = (int)(4 * zoom);   // отступ от переднего края

                // Вершины треугольника (острие направлено вперед - вправо по оси X)
                int[] xPoints = {
                        trainWidth/2 - triangleOffset,                    // острие (правая точка)
                        trainWidth/2 - triangleOffset - triangleHeight,   // левая верхняя
                        trainWidth/2 - triangleOffset - triangleHeight    // левая нижняя
                };
                int[] yPoints = {
                        0,                                              // центр по Y
                        -triangleWidth/2,                               // верх
                        triangleWidth/2                                 // низ
                };

                // Смещаем координаты в систему отсчета центра поезда
                for (int i = 0; i < xPoints.length; i++) {
                    xPoints[i] -= trainWidth/2 - 2;
                }

                g2d.fillPolygon(xPoints, yPoints, 3);
                break;
        }
    }

    private void drawDynamic(Graphics2D g2d, int offsetX, int offsetY, float zoom) {
        if (getWorld() == null) return;

        float screenX = (currentX * 32 + offsetX + 16) * zoom;
        float screenY = (currentY * 32 + offsetY + 16) * zoom;

        int trainWidth = (int)(24 * zoom);
        int trainHeight = (int)(18 * zoom);
        int shadowSizeIncrease = (int)(2 * zoom);

        AffineTransform oldTransform = g2d.getTransform();

        g2d.translate(screenX, screenY);
        g2d.rotate(getRotationAngle());
        g2d.translate(-trainWidth/2, -trainHeight/2);

        // Включаем сглаживание
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Размытая тень
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g2d.setColor(new Color(0, 0, 0, 60));

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
        g2d.setStroke(new BasicStroke(zoom));
        g2d.drawRect(0, 0, trainWidth, trainHeight);

        // 4. Рисуем отличительные знаки
        g2d.translate(trainWidth/2, trainHeight/2); // Переходим в центр поезда
        drawTrainTypeFeatures(g2d, trainWidth, trainHeight, zoom);
        g2d.translate(-trainWidth/2, -trainHeight/2); // Возвращаем обратно

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
    public TrainState getState() {
        if (currentTunnel != null) return TrainState.MOVING;
        if (currentStation != null) return TrainState.STOPPED;
        return TrainState.IDLE;
    }

    public void setState(TrainState state) {
        // Это в основном для загрузки, чтобы восстановить состояние
    }

    public void setProgress(float progress) {
        this.pathProgress = progress;
    }

    public float getProgress() {
        return pathProgress;
    }

    public void setMovingForward(boolean movingForward) {
        this.movingForward = movingForward;
    }

    public boolean isMovingForward() {
        return movingForward;
    }

    public void setWaitTimer(float waitTimer) {
        this.waitTimer = waitTimer;
    }

    public float getWaitTimer() {
        return waitTimer;
    }

    public void setHasPaidAtCurrentStation(boolean hasPaid) {
        this.hasPaidAtCurrentStation = hasPaid;
    }

    public boolean hasPaidAtCurrentStation() {
        return hasPaidAtCurrentStation;
    }

    public void setCurrentSpeed(float speed) {
        this.currentSpeed = speed;
    }

    public void setTargetSpeed(float speed) {
        this.targetSpeed = speed;
    }

    public void setAccelerating(boolean accelerating) {
        this.isAccelerating = accelerating;
    }

    public void setBraking(boolean braking) {
        this.isBraking = braking;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setPreviousTunnel(Tunnel tunnel) {
        this.previousTunnel = tunnel;
    }

    public Tunnel getPreviousTunnel() {
        return previousTunnel;
    }

    public void setCurrentTunnel(Tunnel tunnel) {
        this.currentTunnel = tunnel;
        if (tunnel != null) {
            this.currentPath = tunnel.getPath();
        }
    }

    public void setCurrentStation(Station station) {
        this.currentStation = station;
        if (station != null) {
            this.currentX = station.getX();
            this.currentY = station.getY();
        }
    }

}