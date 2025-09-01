package metroline.objects.gameobjects;

import metroline.core.world.GameWorld;
import metroline.core.world.World;
import metroline.input.selection.SelectionManager;
import metroline.objects.enums.*;

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
    public float getCurrentX() {
        return currentX;
    }

    public float getCurrentY() {
        return currentY;
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
        hasPaidAtCurrentStation = false;

        // Полная остановка на станции
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