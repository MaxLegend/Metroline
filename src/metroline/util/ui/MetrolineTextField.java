package metroline.util.ui;

import metroline.MainFrame;
import metroline.util.IntegerDocumentFilter;
import metroline.util.localizate.ITranslatable;
import metroline.util.localizate.LngUtil;
import metroline.util.ui.tooltip.CursorTooltip;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;

public class MetrolineTextField extends JTextField implements ITranslatable {
    public static final Color DEFAULT_BACKGROUND = new Color(60, 60, 60);
    public static final Color EDIT_BACKGROUND = new Color(45, 45, 45);
    public static final Color DEFAULT_FOREGROUND = Color.WHITE;
    public static final Font DEFAULT_FONT = new Font("Sans Serif", Font.BOLD, 13);

    private String translationKey;
    private String tooltipText;
    private boolean isEditableByDoubleClick;

    public MetrolineTextField() {
        this("", false);
    }

    public MetrolineTextField(String text) {
        this(text, false);
    }

    public MetrolineTextField(String text, boolean editableByDoubleClick) {
        this(text, editableByDoubleClick, "");
    }

    public MetrolineTextField(String text, boolean editableByDoubleClick, String tooltip) {
        super(text);
        this.translationKey = text;
        this.tooltipText = tooltip;
        this.isEditableByDoubleClick = editableByDoubleClick;
        initDefaultStyle();
    }
    public MetrolineTextField(String text,int columns, boolean editableByDoubleClick, String tooltip) {
        super(text,columns);
        this.translationKey = text;
        this.tooltipText = tooltip;
        this.isEditableByDoubleClick = editableByDoubleClick;
        initDefaultStyle();
    }
    public MetrolineTextField(int columns) {
        this("", columns, false);
    }

    public MetrolineTextField(String text, int columns) {
        this(text, columns, false);
    }

    public MetrolineTextField(String text, int columns, boolean editableByDoubleClick) {
        super(text, 3);
        this.translationKey = text;
        this.tooltipText = "";
        this.isEditableByDoubleClick = editableByDoubleClick;
        initDefaultStyle();
    }

    private void initDefaultStyle() {
        setHorizontalAlignment(JTextField.CENTER);
        setForeground(DEFAULT_FOREGROUND);
        setBackground(DEFAULT_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder());
        setFont(DEFAULT_FONT);
        setOpaque(false);
        setCaretColor(Color.WHITE);

        // Установка фильтра для чисел
        ((PlainDocument) getDocument()).setDocumentFilter(new IntegerDocumentFilter());

        if (isEditableByDoubleClick) {
            setEditable(false);
            addDoubleClickEditingSupport();
        }

        addTooltipSupport();
    }

    private void addDoubleClickEditingSupport() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setEditable(true);
                    setOpaque(true);
                    setBackground(EDIT_BACKGROUND);
                    requestFocus();
                    selectAll();
                }
            }
        });

        addActionListener(e -> finishEditing());

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                finishEditing();
            }
        });
    }

    private void finishEditing() {
        if (isEditableByDoubleClick && isEditable()) {
            setEditable(false);
            setOpaque(false);
            setBackground(DEFAULT_BACKGROUND);

            // Оповещаем слушателей об изменении значения
            fireActionPerformed();
        }
    }

    private void addTooltipSupport() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                showTooltip(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                CursorTooltip.hideTooltip();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (CursorTooltip.isVisible() &&
                        CursorTooltip.getCurrentText().equals(tooltipText)) {
                    showTooltip(e);
                }
            }
        });
    }

    private void showTooltip(MouseEvent e) {
        if (tooltipText != null && !tooltipText.isEmpty()) {
            Point mousePos = e.getPoint();
            SwingUtilities.convertPointToScreen(mousePos, MetrolineTextField.this);
            Point framePos = MainFrame.INSTANCE.getLocationOnScreen();
            CursorTooltip.showTooltip(tooltipText,
                    mousePos.x - framePos.x,
                    mousePos.y - framePos.y);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем фон с закругленными углами
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Убираем стандартную отрисовку границы
    }

    public void setEditableByDoubleClick(boolean editableByDoubleClick) {
        this.isEditableByDoubleClick = editableByDoubleClick;
        if (editableByDoubleClick) {
            setEditable(false);
            setOpaque(false);
            setBackground(DEFAULT_BACKGROUND);
        }
    }

    public void setTooltipText(String tooltipText) {
        this.tooltipText = tooltipText;
    }

    @Override
    public void updateTranslation() {
        if (translationKey == null) return;
        setText(LngUtil.translatable(translationKey));
        setTooltipText(LngUtil.translatable(tooltipText));
    }

    // Статические фабричные методы для удобства создания
    public static MetrolineTextField createTimeScaleField() {
        MetrolineTextField field = new MetrolineTextField("1", 3, true);
        return field;
    }

    public static MetrolineTextField createTextField(String text, String tooltip) {
        return new MetrolineTextField(text, false, tooltip);
    }

    public static MetrolineTextField createEditableTextField(String text, String tooltip) {
        MetrolineTextField field = new MetrolineTextField(text, false, tooltip);
        field.setEditable(true);
        field.setOpaque(true);
        field.setBackground(EDIT_BACKGROUND);
        return field;
    }
}
