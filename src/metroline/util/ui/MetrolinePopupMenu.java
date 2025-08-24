package metroline.util.ui;

import metroline.util.ui.tooltip.CursorTooltip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Стилизованное popup menu в стиле Metroline
 */
public class MetrolinePopupMenu extends JPopupMenu {

    public MetrolinePopupMenu() {
        super();
        initStyle();
        initTooltipSupport();
    }

    private void initStyle() {
        setBackground(new Color(60, 60, 60));
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80), 1));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    private void initTooltipSupport() {
        // Добавляем слушатели для показа тултипов при наведении на пункты меню
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateTooltipForHoveredItem(e.getPoint());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                // Скрываем тултип при выходе из меню
                CursorTooltip.hideTooltip();
            }
        });
    }

    /**
     * Обновляет тултип для наведенного пункта меню
     */
    private void updateTooltipForHoveredItem(Point mousePoint) {
        Component hoveredComponent = getComponentAt(mousePoint);

        if (hoveredComponent instanceof MetrolineMenuItem) {
            MetrolineMenuItem menuItem = (MetrolineMenuItem) hoveredComponent;
            String tooltipText = menuItem.getTooltipText();

            if (tooltipText != null && !tooltipText.isEmpty()) {
                // Получаем позицию курсора относительно основного окна
                Point menuLocation = getLocationOnScreen();
                Point parentFrameLocation = getParentFrameLocation();

                int mouseX = menuLocation.x + mousePoint.x - parentFrameLocation.x;
                int mouseY = menuLocation.y + mousePoint.y - parentFrameLocation.y;

                CursorTooltip.showTooltip(tooltipText, mouseX, mouseY);
            } else {
                CursorTooltip.hideTooltip();
            }
        } else {
            CursorTooltip.hideTooltip();
        }
    }

    /**
     * Получает позицию основного родительского фрейма
     */
    private Point getParentFrameLocation() {
        Component parent = this;
        while (parent != null && !(parent instanceof JFrame)) {
            parent = parent.getParent();
        }

        if (parent instanceof JFrame) {
            return ((JFrame) parent).getLocationOnScreen();
        }

        // Fallback: возвращаем позицию самого меню
        return getLocationOnScreen();
    }

    /**
     * Добавляет пункт меню с автоматическим стилем Metroline
     */
    public JMenuItem addMetrolineItem(String text, Runnable action) {
        MetrolineMenuItem menuItem = new MetrolineMenuItem(text, action);
        add(menuItem);
        return menuItem;
    }

    /**
     * Добавляет пункт меню с тултипом
     */
    public JMenuItem addMetrolineItem(String text, String tooltip, Runnable action) {
        MetrolineMenuItem menuItem = new MetrolineMenuItem(text, action);
        menuItem.setTooltipText(tooltip);
        add(menuItem);
        return menuItem;
    }

    /**
     * Показывает меню под указанным компонентом с автоматическим закрытием при клике вне меню
     */
    public void showUnderComponent(Component component) {
        show(component, 0, component.getHeight());
        addAutoCloseListener(component);
    }

    public void showAboveComponent(Component component) {
        // Показываем над компонентом (сверху)
        show(component, 0, -getPreferredSize().height);
        addAutoCloseListener(component);
    }

    /**
     * Добавляет слушатель для автоматического закрытия при клике вне меню
     */
    private void addAutoCloseListener(Component sourceComponent) {
        AWTEventListener closeListener = new AWTEventListener() {
            public void eventDispatched(AWTEvent event) {
                if (event.getID() == MouseEvent.MOUSE_PRESSED && isVisible()) {
                    MouseEvent mouseEvent = (MouseEvent) event;

                    boolean clickOutsidePopup = !isPointInComponent(mouseEvent, MetrolinePopupMenu.this);
                    boolean clickOutsideSource = !isPointInComponent(mouseEvent, sourceComponent);

                    if (clickOutsidePopup && clickOutsideSource) {
                        setVisible(false);
                        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
                        CursorTooltip.hideTooltip(); // Скрываем тултип при закрытии меню
                    }
                }
            }

            private boolean isPointInComponent(MouseEvent event, Component comp) {
                if (comp == null || !comp.isVisible()) return false;

                try {
                    Point compLocation = comp.getLocationOnScreen();
                    Rectangle compBounds = new Rectangle(compLocation, comp.getSize());
                    return compBounds.contains(event.getLocationOnScreen());
                } catch (Exception e) {
                    return false;
                }
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(closeListener, AWTEvent.MOUSE_EVENT_MASK);

        addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(closeListener);
                CursorTooltip.hideTooltip(); // Скрываем тултип при закрытии меню
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(closeListener);
                CursorTooltip.hideTooltip(); // Скрываем тултип при отмене меню
            }
        });
    }
}