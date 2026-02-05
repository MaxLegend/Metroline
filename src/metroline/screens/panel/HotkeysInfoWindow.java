package metroline.screens.panel;

import metroline.MainFrame;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.MetrolineButton;
import metroline.util.ui.MetrolineLabel;
import metroline.util.ui.StyleUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class HotkeysInfoWindow extends JWindow {

    private JPanel contentPanel;
    private JButton closeButton;

    // Hotkey data: [combination_key, description_key]
    private static final String[][] HOTKEYS = {
            {"hotkey.shift", "hotkey.shift_desc"},
            {"hotkey.ctrl", "hotkey.ctrl_desc"},
            {"hotkey.alt", "hotkey.alt_desc"},
            {"hotkey.arrows", "hotkey.arrows_desc"},
            {"hotkey.c", "hotkey.c_desc"},
            {"hotkey.lmb", "hotkey.lmb_desc"},
            {"hotkey.esc", "hotkey.esc_desc"},
            {"hotkey.ctrl_s", "hotkey.ctrl_s_desc"},
            {"hotkey.ctrl_d", "hotkey.ctrl_d_desc"},
            {"hotkey.rmb", "hotkey.rmb_desc"},
            {"hotkey.mmb", "hotkey.mmb_desc"},
            {"hotkey.f11", "hotkey.f11_desc"}
    };

    public HotkeysInfoWindow(MainFrame parent) {
        super(parent);
        initUI();
        centerOnScreen(parent);
        setVisible(true);
    }

    private void initUI() {
        setBackground(new Color(0, 0, 0, 0));

        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(30, 30, 30, 245));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2d.setColor(new Color(70, 70, 70, 200));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2d.dispose();
            }
        };
        contentPanel.setLayout(new BorderLayout(0, 10));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        contentPanel.setOpaque(false);
        contentPanel.setPreferredSize(new Dimension(800, 500));
        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        MetrolineLabel titleLabel = new MetrolineLabel("hotkey.window_title");
        titleLabel.setFont(StyleUtil.getMetrolineFont(16));
        titleLabel.setForeground(Color.WHITE);

        closeButton = new JButton("✕");
        closeButton.setFont(StyleUtil.getMetrolineFont(16));
        closeButton.setForeground(Color.LIGHT_GRAY);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(null);
        closeButton.addActionListener(e -> hideWindow());
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(closeButton, BorderLayout.EAST);

        // --- HOTKEYS LIST ---
        JPanel hotkeysPanel = new JPanel();
        hotkeysPanel.setLayout(new BoxLayout(hotkeysPanel, BoxLayout.Y_AXIS));
        hotkeysPanel.setOpaque(false);
        hotkeysPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        for (String[] hotkey : HOTKEYS) {
            JPanel row = createHotkeyRow(hotkey[0], hotkey[1]);
            hotkeysPanel.add(row);
            hotkeysPanel.add(Box.createVerticalStrut(8));
        }

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(hotkeysPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(450, 350));

        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(contentPanel);
        pack();

        // Enable dragging
        MouseAdapter drag = createDragAdapter();
        contentPanel.addMouseListener(drag);
        contentPanel.addMouseMotionListener(drag);

        // Add translatable components
        MainFrame.getInstance().translatables.add(titleLabel);
        for (String[] hotkey : HOTKEYS) {
            // These are added in createHotkeyRow
        }
    }
    public void showWindow() {
        setVisible(true);
        toFront();
    }

    public void hideWindow() {
        setVisible(false);
    }
    private JPanel createHotkeyRow(String combKey, String descKey) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Combination label (left)
        MetrolineLabel combLabel = new MetrolineLabel(combKey);
        combLabel.setFont(StyleUtil.getMetrolineFont(13));
        combLabel.setForeground(new Color(150, 200, 255));
        combLabel.setPreferredSize(new Dimension(120, 25));

        // Description label (right)
        MetrolineLabel descLabel = new MetrolineLabel(descKey);
        descLabel.setFont(StyleUtil.getMetrolineFont(12));
        descLabel.setForeground(new Color(200, 200, 200));

        row.add(combLabel, BorderLayout.WEST);
        row.add(descLabel, BorderLayout.CENTER);

        // Register for translations
        MainFrame.getInstance().translatables.add(combLabel);
        MainFrame.getInstance().translatables.add(descLabel);

        return row;
    }

    private void centerOnScreen(MainFrame parent) {
        Rectangle parentBounds = parent.getBounds();
        Dimension windowSize = getSize();
        setLocation(
                parentBounds.x + (parentBounds.width - windowSize.width) / 2,
                parentBounds.y + (parentBounds.height - windowSize.height) / 2
        );
    }

    private MouseAdapter createDragAdapter() {
        return new MouseAdapter() {
            private Point start;
            @Override
            public void mousePressed(MouseEvent e) {
                start = e.getLocationOnScreen();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (start != null) {
                    Point curr = e.getLocationOnScreen();
                    setLocation(getLocation().x + (curr.x - start.x), getLocation().y + (curr.y - start.y));
                    start = curr;
                }
            }
        };
    }
}
