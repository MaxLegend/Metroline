package metroline.objects.gameobjects;

import metroline.core.world.World;

import java.awt.*;

/**
 * TODO Вынести рендер в отдельный класс
 */
public class Label extends GameObject {
    private String text;
    private Station parentStation;



    public Label() {
        super(0, 0);
    }

    public Label(World world, int x, int y, String text, Station parentStation) {
        super(x, y);
        this.setWorld(world);
        this.text = text;
        this.parentStation = parentStation;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Station getParentStation() {
        return parentStation;
    }

    public boolean tryMoveTo(int newX, int newY) {
        Station parent = getParentStation();
        World world = getWorld(); // Используем сохраненную ссылку

        if (world.isLabelPositionValid(newX, newY, parent)) {
            world.getGameGrid()[getX()][getY()].setContent(null);
            this.x = newX;
            this.y = newY;
            world.getGameGrid()[newX][newY].setContent(this);
            return true;
        }
        return false;
    }
    @Override
    public void draw(Graphics g, int offsetX, int offsetY, float zoom) {
        // Получаем относительное положение текста к станции
        int relX = getX() - parentStation.getX();
        int relY = getY() - parentStation.getY();

        // Базовые смещения (для положения справа от станции)
        int baseOffsetX = 32 + 8; // 32 - размер клетки, 8 - дополнительный отступ
        int baseOffsetY = 20;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        // Корректируем смещение в зависимости от положения
        if (relX < 0) { // Если текст слева от станции
            baseOffsetX = -textWidth - 8; // Смещаем влево на ширину текста
        }
        else if (relX == 0) { // Если текст сверху/снизу станции
            // Центрируем по горизонтали
            baseOffsetX = (32 - textWidth) / 2;

            if (relY < 0) { // Если текст выше станции
                baseOffsetY = -textHeight + 4;
            }
            else if (relY > 0) { // Если текст ниже станции
                baseOffsetY = textHeight+ 32;
            }
        }

        // Рассчитываем позицию текста
        int drawX = (int)((parentStation.getX() * 32 + offsetX + baseOffsetX) * zoom);
        int drawY = (int)((parentStation.getY() * 32 + offsetY + baseOffsetY) * zoom);

        // Настройки внешнего вида
        Color textColor = selected ? Color.RED : new Color(30, 30, 30);
        Color bgColor = new Color(255, 255, 255, 180);

        // Настройка шрифта
        int fontSize = (int)(12 * zoom);
        fontSize = Math.max(fontSize, 8);
        Font font = new Font("Arial", Font.PLAIN, fontSize);
        g.setFont(font);



        // Обновляем позицию с учетом ширины текста (если нужно)
        if (relX < 0) {
            drawX = (int)((parentStation.getX() * 32 + offsetX - textWidth - 8) * zoom);
        }

        // Включаем сглаживание
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем подложку
        g2d.setColor(bgColor);
        int round = (int)(4 * zoom);
        g2d.fillRoundRect(drawX - 3, drawY - fm.getAscent() - 2,
                textWidth + 6, textHeight + 4, round, round);

        // Рисуем текст
        g2d.setColor(textColor);
        g2d.drawString(text, drawX, drawY);

        if (selected) {
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2 * zoom));
            g2d.drawRoundRect(drawX - 5, drawY - fm.getAscent() - 4,
                    textWidth + 10, textHeight + 8, round + 2, round + 2);
        }
    }

}
