package com.library.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

public class GuiUtils {

    // ── Colour palette  (white / beige / brown library theme) ────
    public static final Color BG_DARK    = new Color(250, 247, 242);
    public static final Color BG_PANEL   = new Color(244, 239, 229);
    public static final Color BG_CARD    = new Color(255, 253, 248);
    public static final Color BG_SIDEBAR = new Color(62,  42,  28);
    public static final Color BORDER_COL = new Color(210, 195, 170);
    public static final Color ACCENT     = new Color(139,  90,  43);
    public static final Color ACCENT_GRN = new Color( 56, 142,  60);
    public static final Color ACCENT_RED = new Color(198,  40,  40);
    public static final Color ACCENT_YEL = new Color(183, 122,   0);
    public static final Color TEXT_PRI   = new Color( 40,  24,   8);
    public static final Color TEXT_SEC   = new Color(120,  95,  70);
    public static final Color BTN_PRIMARY= new Color(139,  90,  43);
    public static final Color SIDEBAR_FG = new Color(230, 210, 185);
    public static final Color SIDEBAR_SEL= new Color( 93,  64,  35);

    // ── Fonts ────────────────────────────────────────────────────
    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_SUB    = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_MONO   = new Font("Courier New", Font.PLAIN, 12);
    public static final Font FONT_CARD_V = new Font("Segoe UI", Font.BOLD,  28);
    public static final Font FONT_CARD_L = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_NAV    = new Font("Segoe UI", Font.PLAIN, 13);

    /**
     * Page title: renders a bold title with a small painted brown accent square
     * to the left — no emoji, so it always renders correctly on all systems.
     */
    public static JPanel pageTitlePanel(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 4, 5, 22, 3, 3);
            }
        };
        p.setBackground(BG_DARK);
        p.setOpaque(false);
        JLabel lbl = new JLabel("  " + text);
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(TEXT_PRI);
        p.add(lbl);
        return p;
    }

    public static JLabel pageTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI Emoji", Font.BOLD, 22));
        lbl.setForeground(TEXT_PRI);
        lbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, ACCENT),
            BorderFactory.createEmptyBorder(0, 10, 0, 0)
        ));
        return lbl;
    }

    public static JLabel pageSubtitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SUB);
        lbl.setForeground(TEXT_SEC);
        return lbl;
    }

    public static JLabel bodyLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_PRI);
        return lbl;
    }

    public static JLabel secondaryLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_SEC);
        return lbl;
    }

    public static JPanel statCard(String emoji, String value, String label, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),
            BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));
        card.setPreferredSize(new Dimension(180, 110));

        JLabel emojiLbl = new JLabel(emoji);
        emojiLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        emojiLbl.setForeground(accentColor);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(FONT_CARD_V);
        valueLbl.setForeground(accentColor);

        JLabel nameLbl = new JLabel(label);
        nameLbl.setFont(FONT_CARD_L);
        nameLbl.setForeground(TEXT_SEC);

        card.add(emojiLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(nameLbl);
        return card;
    }

    public static JPanel sectionCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));
        if (title != null && !title.isEmpty()) {
            JLabel t = new JLabel(title);
            t.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
            t.setForeground(ACCENT);
            t.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            JSeparator sep = new JSeparator();
            sep.setForeground(BORDER_COL);
            sep.setBackground(BORDER_COL);
            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(BG_CARD);
            top.add(t,   BorderLayout.NORTH);
            top.add(sep, BorderLayout.SOUTH);
            card.add(top, BorderLayout.NORTH);
        }
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates an icon-style button: emoji icon displayed above a short text label.
     * Use this for main action buttons in toolbars (add, delete, refresh, etc.)
     */
    /** Warm beige — use as bg for secondary/ghost icon buttons */
    public static final Color BG_BTN_GHOST = new Color(222, 208, 186);

    /**
     * Primary action icon button — large emoji icon on top, text label below.
     * isPrimary=true: bigger icon for main actions like "Add" / "Register"
     */
    public static JButton iconButton(String icon, String text, Color bg, Color fg) {
        return iconButton(icon, text, bg, fg, false);
    }

    public static JButton iconButton(String icon, String text, Color bg, Color fg, boolean large) {
        Color actualBg = bg.equals(BG_CARD) ? BG_BTN_GHOST : bg;
        int iconSize = large ? 22 : 16;
        final Color hoverBg = actualBg.brighter();

        JButton btn = new JButton() {
            @Override public Dimension getPreferredSize() { return new Dimension(82, 58); }
        };
        btn.setLayout(new BorderLayout(0, 0));
        btn.setBackground(actualBg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(
                Math.max(0, actualBg.getRed()-25),
                Math.max(0, actualBg.getGreen()-25),
                Math.max(0, actualBg.getBlue()-25)), 1, true),
            BorderFactory.createEmptyBorder(6, 4, 4, 4)
        ));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, iconSize));
        iconLbl.setForeground(fg);
        iconLbl.setOpaque(false);

        JLabel textLbl = new JLabel(text, SwingConstants.CENTER);
        textLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        textLbl.setForeground(fg);
        textLbl.setOpaque(false);

        btn.add(iconLbl, BorderLayout.CENTER);
        btn.add(textLbl, BorderLayout.SOUTH);

        // Hover animation: scale-like effect by brightening background
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hoverBg);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(actualBg);
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(actualBg.darker());
            }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                btn.setBackground(actualBg);
            }
        });

        return btn;
    }

    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(BTN_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return btn;
    }

    public static JButton dangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(ACCENT_RED);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return btn;
    }

    public static JButton successButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(ACCENT_GRN);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return btn;
    }

    public static JButton ghostButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        btn.setForeground(TEXT_SEC);
        btn.setBackground(new Color(232, 220, 200));   // warm beige — visible on parchment bg
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    public static JTextField styledField(String placeholder) {
        JTextField tf = new JTextField(20);
        tf.setFont(FONT_BODY);
        tf.setForeground(TEXT_PRI);
        tf.setBackground(Color.WHITE);
        tf.setCaretColor(TEXT_PRI);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        tf.putClientProperty("placeholder", placeholder);
        return tf;
    }

    public static JComboBox<String> styledCombo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(FONT_BODY);
        cb.setForeground(TEXT_PRI);
        cb.setBackground(Color.WHITE);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        return cb;
    }

    public static JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table.setFont(FONT_BODY);
        table.setForeground(TEXT_PRI);
        table.setBackground(BG_CARD);
        table.setGridColor(BORDER_COL);
        table.setRowHeight(32);
        table.setSelectionBackground(new Color(139, 90, 43, 50));
        table.setSelectionForeground(TEXT_PRI);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setForeground(TEXT_SEC);
        header.setBackground(BG_PANEL);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
        return table;
    }

    public static JScrollPane scrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(BG_CARD);
        sp.getViewport().setBackground(BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        return sp;
    }

    public static JTextArea monoArea() {
        JTextArea ta = new JTextArea();
        ta.setFont(FONT_MONO);
        ta.setForeground(new Color(60, 35, 10));
        ta.setBackground(new Color(255, 252, 242));
        ta.setCaretColor(TEXT_PRI);
        ta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        ta.setEditable(false);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        return ta;
    }

    public static JScrollPane scrollMono(JTextArea ta) {
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        sp.getViewport().setBackground(new Color(255, 252, 242));
        return sp;
    }

    public static JPanel formRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(BG_PANEL);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_SEC);
        lbl.setPreferredSize(new Dimension(120, 28));
        row.add(lbl,   BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    public static JPanel padded(JComponent content, int top, int left, int bottom, int right) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(content.getBackground());
        p.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    public static JPanel toolbar(JComponent... components) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setBackground(BG_DARK);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        for (JComponent c : components) bar.add(c);
        return bar;
    }

    public static JLabel badge(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(color);
        lbl.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
        lbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80), 1, true),
            BorderFactory.createEmptyBorder(2, 10, 2, 10)
        ));
        lbl.setOpaque(true);
        return lbl;
    }

    public static JSeparator darkSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(80, 55, 35));
        sep.setBackground(new Color(80, 55, 35));
        return sep;
    }

    public static void applyGlobalTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
        UIManager.put("Panel.background",            BG_DARK);
        UIManager.put("OptionPane.background",        BG_PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT_PRI);
        UIManager.put("Button.background",            BG_CARD);
        UIManager.put("Button.foreground",            TEXT_PRI);
        UIManager.put("TextField.background",         Color.WHITE);
        UIManager.put("TextField.foreground",         TEXT_PRI);
        UIManager.put("TextField.caretForeground",    TEXT_PRI);
        UIManager.put("ComboBox.background",          Color.WHITE);
        UIManager.put("ComboBox.foreground",          TEXT_PRI);
        UIManager.put("Label.foreground",             TEXT_PRI);
        UIManager.put("ScrollPane.background",        BG_CARD);
        UIManager.put("Viewport.background",          BG_CARD);
        UIManager.put("Dialog.background",            BG_PANEL);
        UIManager.put("ToolTip.background",           BG_CARD);
        UIManager.put("ToolTip.foreground",           TEXT_PRI);
    }
}
