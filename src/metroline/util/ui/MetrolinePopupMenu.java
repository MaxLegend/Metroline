package metroline.util.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;

/**
 * Стилизованное popup menu в стиле Metroline
 */
public class MetrolinePopupMenu extends JPopupMenu {

    public MetrolinePopupMenu() {
        super();
        initStyle();
    }

    private void initStyle() {
        setBackground(new Color(60, 60, 60));
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80), 1));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
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
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(closeListener);
            }
        });
    }
}