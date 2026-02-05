package metroline.util.ui;

import metroline.util.localizate.ITranslatable;
import metroline.util.localizate.LngUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import static metroline.MainFrame.SOUND_ENGINE;

public class MetrolineSlider extends JComponent implements SwingConstants, ITranslatable {


    private String tooltipText;
    private Timer hoverTimer;
    private JWindow tooltipWindow;
    private static final int HOVER_DELAY = 10;
    private static final int TOOLTIP_OFFSET_Y = 20;
    private static final int MAX_TOOLTIP_WIDTH = 200;

    private float minValue;
    private float maxValue;
    private float value;
    private float stepSize;
    private int orientation = HORIZONTAL;
    private boolean isAdjusting = false;
    private DecimalFormat valueFormat = new DecimalFormat("#.##");
    private JLabel valueLabel;
    private String valueSuffix = "";
    private ChangeEvent changeEvent = new ChangeEvent(this);
    private EventListenerList listenerList = new EventListenerList();
    public MetrolineSlider(float min, float max, float value, float step) {
        this.minValue = min;
        this.maxValue = max;
        this.stepSize = step;
        setValue(value);


        setBackground(StyleUtil.BACKGROUND_COLOR);
        setFocusable(true);
        setOpaque(true);
        updateUI();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                SOUND_ENGINE.playUISound("entered");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                SOUND_ENGINE.playUISound("mouse");
            }


        });

        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (!isEnabled()) return;

                // Определяем направление прокрутки
                int rotation = e.getWheelRotation();
                float step = getStepSize();

                // Уменьшаем/увеличиваем значение с учетом шага
                float newValue = getValue() - (rotation * step);
                setValue(newValue);

                // Предотвращаем прокрутку родительского компонента
                e.consume();
            }
        });
        // Обработка клавиатуры
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_DOWN:
                        setValue(getValue() - stepSize);
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_UP:
                        setValue(getValue() + stepSize);
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        setValue(getValue() + stepSize * 10);
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        setValue(getValue() - stepSize * 10);
                        break;
                    case KeyEvent.VK_HOME:
                        setValue(minValue);
                        break;
                    case KeyEvent.VK_END:
                        setValue(maxValue);
                        break;
                }
            }
        });
    }
    public MetrolineSlider(String tooltipText,float min, float max, float value, float step) {
        this.minValue = min;
        this.maxValue = max;
        this.stepSize = step;
        this.tooltipText = tooltipText;
        setValue(value);


        setBackground(StyleUtil.BACKGROUND_COLOR);
        setFocusable(true);
        setOpaque(true);
        updateUI();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                SOUND_ENGINE.playUISound("entered");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                startHoverTimer(e);
                SOUND_ENGINE.playUISound("mouse");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelHoverTimer();
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
        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (!isEnabled()) return;

                // Определяем направление прокрутки
                int rotation = e.getWheelRotation();
                float step = getStepSize();

                // Уменьшаем/увеличиваем значение с учетом шага
                float newValue = getValue() - (rotation * step);
                setValue(newValue);

                // Предотвращаем прокрутку родительского компонента
                e.consume();
            }
        });
        // Обработка клавиатуры
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_DOWN:
                        setValue(getValue() - stepSize);
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_UP:
                        setValue(getValue() + stepSize);
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        setValue(getValue() + stepSize * 10);
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        setValue(getValue() - stepSize * 10);
                        break;
                    case KeyEvent.VK_HOME:
                        setValue(minValue);
                        break;
                    case KeyEvent.VK_END:
                        setValue(maxValue);
                        break;
                }
            }
        });
    }
    public void setValueLabel(MetrolineLabel label, String suffix) {
        this.valueLabel = label;
        this.valueSuffix = suffix != null ? suffix : "";
        updateValueLabel();
    }

    private void updateValueLabel() {
        if (valueLabel != null) {
            valueLabel.setText(String.format("%.1f%s", value, valueSuffix));
        }
    }
    public void setValueSuffix(String suffix) {
         valueSuffix = suffix;
    }
    public String getValueSuffix() {
        return valueSuffix;
    }
    public void setMetrosliderTooltip(String tooltipText) {
        this.tooltipText = tooltipText;
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
    @Override
    public void updateUI() {
        setUI(MetrolineSliderUI.createUI(this));
    }

    public void setUI(MetrolineSliderUI ui) {
        super.setUI(ui);
    }

    public MetrolineSliderUI getUI() {
        return (MetrolineSliderUI) ui;
    }

    public float getMinValue() {
        return minValue;
    }

    public void setMinValue(float min) {
        this.minValue = min;
        setValue(value); // Проверяем границы
        repaint();
    }

    public float getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(float max) {
        this.maxValue = max;
        setValue(value); // Проверяем границы
        repaint();
    }

    public float getStepSize() {
        return stepSize;
    }

    public void setStepSize(float step) {
        this.stepSize = step;
        repaint();
    }

    public float getValue() {
        return value;
    }

    public void setValue(float v) {
        float oldValue = value;
        value = Math.max(minValue, Math.min(maxValue, v));

        // Округляем до ближайшего шага
        if (stepSize > 0) {
            value = Math.round(value / stepSize) * stepSize;
        }

        if (value != oldValue) {
            updateValueLabel();
            fireStateChanged();
        }
        repaint();
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("Orientation must be HORIZONTAL or VERTICAL");
        }
        this.orientation = orientation;
        repaint();
    }

    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }

    public void setValueIsAdjusting(boolean b) {
        if (isAdjusting != b) {
            isAdjusting = b;
            fireStateChanged();
        }
    }

    public void setValueFormat(DecimalFormat format) {
        this.valueFormat = format;
        repaint();
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    protected void fireStateChanged() {
        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        if (listeners != null && listeners.length > 0) {
            for (ChangeListener listener : listeners) {
                listener.stateChanged(changeEvent);
            }
        }
    }


    @Override
    public void updateTranslation() {
        setMetrosliderTooltip(LngUtil.translatable(tooltipText));
    }
}
