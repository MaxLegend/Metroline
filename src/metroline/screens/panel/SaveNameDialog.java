package metroline.screens.panel;


import metroline.util.ui.StyleUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Minimalist dialog for entering save file name
 */
public class SaveNameDialog extends JDialog {

    private JTextField nameField;
    private String result = null;

    public SaveNameDialog(Window parent, String defaultName) {
        super(parent, "Save Game", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(350, 150);
        setLocationRelativeTo(parent);

        initUI(defaultName);
    }

    private void initUI(String defaultName) {
        // Main panel with dark theme
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(35, 35, 35));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2d.dispose();
            }
        };
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("Enter save name:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(StyleUtil.getMetrolineFont(14));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Text field
        nameField = createStyledTextField(defaultName);
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmSave();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelSave();
                }
            }
        });
        mainPanel.add(nameField, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setOpaque(false);

        JButton saveBtn = createButton("Save", new Color(70, 130, 70));
        saveBtn.addActionListener(e -> confirmSave());

        JButton cancelBtn = createButton("Cancel", new Color(100, 100, 100));
        cancelBtn.addActionListener(e -> cancelSave());

        buttonsPanel.add(cancelBtn);
        buttonsPanel.add(saveBtn);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setBackground(new Color(0, 0, 0, 0));
    }

    private JTextField createStyledTextField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(StyleUtil.getMetrolineFont(14));
        field.setForeground(Color.WHITE);
        field.setBackground(new Color(50, 50, 50));
        field.setCaretColor(Color.YELLOW);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.selectAll();
        return field;
    }

    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getModel().isPressed() ? bgColor.darker() :
                        getModel().isRollover() ? bgColor.brighter() : bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(StyleUtil.getMetrolineFont(12));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(80, 32));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void confirmSave() {
        String name = nameField.getText().trim();
        if (!name.isEmpty()) {
            // Ensure .metro extension
            if (!name.endsWith(".metro")) {
                name += ".metro";
            }
            result = name;
            dispose();
        }
    }

    private void cancelSave() {
        result = null;
        dispose();
    }

    /**
     * Show dialog and return entered name (or null if cancelled)
     */
    public static String showDialog(Window parent, String defaultName) {
        SaveNameDialog dialog = new SaveNameDialog(parent, defaultName);
        dialog.setVisible(true);
        return dialog.result;
    }
}
