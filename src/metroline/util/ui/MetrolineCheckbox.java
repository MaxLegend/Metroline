package metroline.util.ui;

import metroline.util.localizate.ITranslatable;
import metroline.util.localizate.LngUtil;
import metroline.util.serialize.GlobalSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.TimerTask;
import java.util.Timer;

import static metroline.MainFrame.SOUND_ENGINE;

public class MetrolineCheckbox extends JCheckBox implements ITranslatable {
    private String tooltipText;
    private String titleText;
    private Timer hoverTimer;
    private JWindow tooltipWindow;
    private static final int HOVER_DELAY = 10; // Задержка перед показом (мс)
    private static final int TOOLTIP_OFFSET_Y = 20; // Смещение подсказки по Y
    private static final int MAX_TOOLTIP_WIDTH = 300;
    private boolean isHover = false;

    public MetrolineCheckbox(String text, String tooltipText) {
        super(text);
        this.titleText = text;
        this.tooltipText = tooltipText;
        setIcon(new MetrolineCheckboxUI(false));

        setSelectedIcon(new MetrolineCheckboxUI(true));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                SOUND_ENGINE.playUISound("entered", GlobalSettings.getSfxVolume());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                startHoverTimer(e);
                isHover = true;
                SOUND_ENGINE.playUISound("mouse", GlobalSettings.getSfxVolume());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelHoverTimer();
                isHover = false;
                hideTooltip();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (tooltipWindow != null && tooltipWindow.isVisible()) {
                    // Если подсказка уже видна - обновляем её позицию
                    updateTooltipPosition(e);
                } else {
                    // Иначе перезапускаем таймер
                    restartHoverTimer(e);
                }
            }
        });
    }
    @Override
    public void setToolTipText(String text) {
        tooltipText = text;
    }

    @Override
    public void updateTranslation() {
        setToolTipText(LngUtil.translatable(tooltipText));
        setText(LngUtil.translatable(titleText));
    }
    private void startHoverTimer(MouseEvent e) {
        cancelHoverTimer();
        hoverTimer = new Timer();
        hoverTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                showTooltip(e);
            }
        }, HOVER_DELAY);
    }

    private void restartHoverTimer(MouseEvent e) {
        cancelHoverTimer();
        hoverTimer = new Timer();
        hoverTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                showTooltip(e);
            }
        }, HOVER_DELAY);
    }

    private void cancelHoverTimer() {
        if (hoverTimer != null) {
            hoverTimer.cancel();
            hoverTimer = null;
        }
    }

    private void showTooltip(MouseEvent e) {
        SwingUtilities.invokeLater(() -> {
            if (tooltipWindow == null) {
                tooltipWindow = new JWindow((Window) SwingUtilities.getRoot(this));
                tooltipWindow.setFocusableWindowState(false);

                JTextArea textArea = new JTextArea(tooltipText);
                textArea.setWrapStyleWord(true); // Перенос по словам
                textArea.setLineWrap(true); // Включить перенос строк
                textArea.setEditable(false);
                textArea.setBackground(new Color(60, 60, 60));
                textArea.setFont(new Font("Sans Serif", Font.BOLD, 13));
                textArea.setForeground(Color.WHITE);
                textArea.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 0));

                // Устанавливаем предпочтительный размер с учетом максимальной ширины
                textArea.setSize(new Dimension(MAX_TOOLTIP_WIDTH, Integer.MAX_VALUE));
                Dimension prefSize = textArea.getPreferredSize();
                textArea.setPreferredSize(new Dimension(
                        Math.min(prefSize.width, MAX_TOOLTIP_WIDTH),
                        prefSize.height
                ));

                tooltipWindow.add(textArea);
            }

            updateTooltipPosition(e);
            tooltipWindow.pack(); // Переупаковываем с учетом возможного изменения размера
            tooltipWindow.setVisible(true);
        });
    }

    private void updateTooltipPosition(MouseEvent e) {
        if (tooltipWindow != null) {
            Point location = e.getLocationOnScreen();
            location.y += TOOLTIP_OFFSET_Y;

            // Проверяем, чтобы подсказка не выходила за пределы экрана справа
            GraphicsConfiguration gc = tooltipWindow.getGraphicsConfiguration();
            Rectangle screenBounds = gc.getBounds();
            int maxX = screenBounds.x + screenBounds.width - tooltipWindow.getWidth();
            location.x = Math.min(location.x, maxX);

            tooltipWindow.setLocation(location);
        }
    }

    private void hideTooltip() {
        if (tooltipWindow != null) {
            tooltipWindow.dispose();
            tooltipWindow = null;
        }
    }

    private class MetrolineCheckboxUI implements Icon, Serializable {
        private static final long serialVersionUID = 1L;
        private static final int SIZE = 16;
        private static final int INNER_PADDING = 2;
        private final boolean selected;

        public MetrolineCheckboxUI() {
            this(false);
        }

        public MetrolineCheckboxUI(boolean selected) {
            this.selected = selected;
        }


        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Внешний квадрат (фон)

            if (MetrolineCheckbox.this.isHover) {
                g2.setColor(new Color(100, 100, 100)); // Светло-серый
                g2.fillRect(x, y, SIZE, SIZE);
            } else {
                g2.setColor(new Color(70, 70, 70)); // Темный серый
                g2.fillRect(x, y, SIZE, SIZE);
            }
            // Граница
            g2.setColor(new Color(100, 100, 100)); // Серый
            g2.drawRect(x, y, SIZE, SIZE);

            if (selected) {
                // Внутренний квадрат (выбранное состояние)
                g2.setColor(new Color(150, 150, 150)); // Светло-серый
                g2.fillRect(x + INNER_PADDING, y + INNER_PADDING,
                        SIZE - 2*INNER_PADDING, SIZE - 2*INNER_PADDING);
            }
            g2.dispose();
        }



        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
