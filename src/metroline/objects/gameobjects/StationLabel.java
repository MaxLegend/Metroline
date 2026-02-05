package metroline.objects.gameobjects;

import metroline.core.world.World;
import metroline.input.selection.SelectionManager;
import metroline.screens.worldscreens.normal.GameWorldScreen;

import java.awt.*;

/**
 * TODO Вынести рендер в отдельный класс
 */
public class StationLabel extends GameObject {
    private String text;
    private GameObject parentStation;
    private boolean visible = true;


    public StationLabel() {
        super(0, 0);
    }

    public StationLabel(World world, int x, int y, String text, GameObject parentStation) {
        super(x, y);
        this.setWorld(world);
        this.text = text;
        this.parentStation = parentStation;
    }
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public GameObject getParentGameObject() {
        return parentStation;
    }
    public void setParentGameObject(GameObject obj) {
        this.parentStation = obj;
    }
    public long getParentStationId() {
        return parentStation != null ? parentStation.getUniqueId() : -1;
    }
//    public boolean tryMoveTo(int newX, int newY) {
//        GameObject parent = getParentGameObject();
//        World world = getWorld();
//
//        if (parent != null) {
//            int dx = newX - parent.getX();
//            int dy = newY - parent.getY();
//
//            // Разрешаем перемещение только в 8 клетках вокруг станции (3x3 grid без центра)
//            // dx и dy должны быть в диапазоне [-1, 1], но не оба равны 0
//            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && (dx != 0 || dy != 0)) {
//                // Проверяем, что клетка свободна (нет других станций или меток)
//                if (world.getStationAt(newX, newY) == null &&
//                        (world.getLabelAt(newX, newY) == null || world.getLabelAt(newX, newY) == this
//
//
//                        )) {
//
//                    // Убираем метку из старой позиции
//                    world.getGameTile(getX(), getY()).setContent(null);
//
//                    // Обновляем логические координаты
//                    this.x = newX;
//                    this.y = newY;
//
//                    // Помещаем метку в новую позицию
//                    world.getGameTile(newX, newY).setContent(this);
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    @Override
    public void draw(Graphics2D g, int offsetX, int offsetY, float zoom) {
        if (!visible) {
            return;
        }

        if (parentStation != null) {
            int relX = getX() - parentStation.getX();
            int relY = getY() - parentStation.getY();

            int baseOffsetX = 32 + 8;
            int baseOffsetY = 20;

            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();

            boolean isDiagonal = Math.abs(relX) == 1 && Math.abs(relY) == 1;

            // Обработка диагональных позиций
            if (isDiagonal) {
                if (relX == -1 && relY == -1) { // Слева-сверху
                    baseOffsetX = -textWidth - 8;
                    baseOffsetY = -textHeight + 4;
                } else if (relX == 1 && relY == -1) { // Справа-сверху
                    baseOffsetX = 32 + 8;
                    baseOffsetY = -textHeight + 4;
                } else if (relX == -1 && relY == 1) { // Слева-снизу
                    baseOffsetX = -textWidth - 8;
                    baseOffsetY = textHeight + 32;
                } else if (relX == 1 && relY == 1) { // Справа-снизу
                    baseOffsetX = 32 + 8;
                    baseOffsetY = textHeight + 32;
                }
            }
            // Обработка ортогональных позиций
            else if (relX == -1) {
                baseOffsetX = -textWidth - 8;
                baseOffsetY = 20;
            } else if (relX == 1) {
                baseOffsetX = 32 + 8;
                baseOffsetY = 20;
            } else if (relX == 0) {
                baseOffsetX = (32 - textWidth) / 2;
                if (relY == -1) {
                    baseOffsetY = -textHeight + 4;
                } else if (relY == 1) {
                    baseOffsetY = textHeight + 32;
                }
            }

            // Рассчитываем позицию текста
            int drawX = (int) ((parentStation.getX() * 32 + offsetX + baseOffsetX) * zoom);
            int drawY = (int) ((parentStation.getY() * 32 + offsetY + baseOffsetY) * zoom);

            // Настройки внешнего вида
            Color textColor = isSelected() ? Color.RED : new Color(30, 30, 30);
            Color bgColor = new Color(255, 255, 255, 180);

            int fontSize = (int) (12 * zoom);
            fontSize = Math.max(fontSize, 8);
            Font font = new Font("Arial", Font.PLAIN, fontSize);
            g.setFont(font);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Рисуем подложку
            g2d.setColor(bgColor);
            int round = (int) (4 * zoom);
            g2d.fillRoundRect(drawX - 3, drawY - fm.getAscent() - 2,
                    textWidth + 6, textHeight + 4, round, round);

            // Рисуем текст
            g2d.setColor(textColor);
            g2d.drawString(text, drawX, drawY);

            if (isSelected()) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2 * zoom));
                g2d.drawRoundRect(drawX - 5, drawY - fm.getAscent() - 4,
                        textWidth + 10, textHeight + 8, round + 2, round + 2);
            }

        }
    }
    public boolean isSelected() {
        return SelectionManager.getInstance().isSelected(this);
    }
}
