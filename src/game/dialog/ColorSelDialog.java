package game.dialog;

import game.World;
import game.objects.Station;
import game.tiles.GameTile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ColorSelDialog extends JDialog {
    private Color selectedColor;
    private static final Color[] PREDEFINED_COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.PINK,
            new Color(128, 0, 0), // Темно-красный
            new Color(0, 128, 0), // Темно-зеленый
            new Color(0, 0, 128), // Темно-синий
            new Color(128, 128, 0), // Оливковый
            new Color(128, 0, 128), // Фиолетовый
            new Color(0, 128, 128), // Бирюзовый
            new Color(192, 192, 192), // Серебряный
            new Color(128, 128, 128), // Серый
            new Color(255, 165, 0), // Оранжевый
            new Color(165, 42, 42), // Коричневый
            new Color(0, 255, 255), // Аква
            new Color(255, 0, 255)  // Фуксия
    };

    public ColorSelDialog(Frame owner, int x, int y, World world) {
        super(owner, "Select Station Color", true);
        setLayout(new GridLayout(4, 5, 5, 5));
        setSize(300, 250);
        setLocationRelativeTo(owner);

        selectedColor = Color.RED; // По умолчанию

        // Создаем кнопки для каждого цвета
        for (Color color : PREDEFINED_COLORS) {
            JButton colorButton = new JButton();
            colorButton.setBackground(color);
            colorButton.setPreferredSize(new Dimension(40, 40));
            colorButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedColor = color;
                    createStation(x, y, world);
                    dispose();
                }
            });
            add(colorButton);
        }
    }

    private void createStation(int x, int y, World world) {
        GameTile tile = world.getGameLayer()[x][y];
        if (!tile.hasGameObject()) {
            Station station = new Station(x, y, selectedColor);
            tile.setGameObject(station);
        }
    }

    public Color getSelectedColor() {
        return selectedColor;
    }
}
