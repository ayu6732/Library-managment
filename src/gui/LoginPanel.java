package com.library.gui;

import com.library.models.persons.*;
import com.library.services.MemberService;
import com.library.db.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class LoginPanel extends JPanel {

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "100906";
    private static String adminSessionPassword = null; // loaded from DB lazily

    /** Call this once at startup to force a fresh load from DB. */
    public static void resetPasswordCache() {
        adminSessionPassword = null;
    }

    public static String getAdminPassword() {
        if (adminSessionPassword == null) {
            // Always load fresh from DB; fall back to hardcoded default only if DB has nothing
            adminSessionPassword = DatabaseManager.loadSetting("admin_password", ADMIN_PASSWORD);
        }
        return adminSessionPassword;
    }

    public static boolean updateAdminPassword(String p) {
        // Save to DB first — only update cache if DB save succeeded
        try {
            DatabaseManager.saveSetting("admin_password", p);
            adminSessionPassword = p;
            return true;
        } catch (Exception e) {
            System.err.println("✗ Failed to persist admin password: " + e.getMessage());
            return false;
        }
    }

    private String loggedInRole   = null;
    private String loggedInUserId = null;
    private String loggedInName   = null;

    private final MemberService memberService;
    private final Runnable      onLoginSuccess;

    private JTextField     userField;
    private JPasswordField passField;
    private JLabel         errorLabel;
    private JButton        loginBtn;

    // ── Brand colours ─────────────────────────────────────────
    private static final Color BG_PARCHMENT  = new Color(0xFD, 0xF6, 0xEC); // card bg
    private static final Color BG_INPUT      = new Color(0xF0, 0xEE, 0xEB); // light gray input
    private static final Color BORDER_INPUT  = new Color(0xD8, 0xD4, 0xCE); // input border
    private static final Color BORDER_CARD   = new Color(0xD4, 0xB8, 0x96); // card border
    private static final Color BG_ICON       = new Color(0xF5, 0xE9, 0xD6); // icon bg
    private static final Color BG_ROLE       = new Color(0xFF, 0xF8, 0xF0); // inactive role btn
    private static final Color BROWN_DARK    = new Color(0x8B, 0x5C, 0x2A); // button / active role
    private static final Color BROWN_MID     = new Color(0x6B, 0x42, 0x26); // title text
    private static final Color BROWN_LIGHT   = new Color(0x9C, 0x7A, 0x5B); // labels / sub text
    private static final Color BROWN_MUTED   = new Color(0xC4, 0xA8, 0x82); // footer / placeholder
    private static final Color BORDER_FOOTER = new Color(0xE8, 0xD5, 0xBF); // footer divider
    private static final Color TEXT_INPUT    = new Color(0x6B, 0x42, 0x26); // typed text
    private static final Color TEXT_PH       = new Color(0xAA, 0xA6, 0x9F); // placeholder

    public LoginPanel(MemberService memberService, Runnable onLoginSuccess) {
        this.memberService  = memberService;
        this.onLoginSuccess = onLoginSuccess;

        setLayout(new GridBagLayout());
        setBackground(new Color(0xF0, 0xE8, 0xD8));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx      = 0;
        gbc.gridy      = 0;
        gbc.anchor     = GridBagConstraints.CENTER;  // true center both axes
        gbc.fill       = GridBagConstraints.NONE;    // do NOT stretch the card
        gbc.weightx    = 1.0;
        gbc.weighty    = 1.0;
        add(buildCard(), gbc);
    }

    private JPanel buildCard() {
        // Outer wrapper that draws the top accent bar + card border
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Top gradient bar (4px)
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, BROWN_DARK,
                    getWidth(), 0, new Color(0xC4, 0x9A, 0x6C));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
                g2.dispose();
            }
        };
        wrapper.setBackground(BG_PARCHMENT);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_CARD, 1, true),
            new EmptyBorder(4, 0, 0, 0) // space for the top bar
        ));
        wrapper.setPreferredSize(new Dimension(400, 700));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(BG_PARCHMENT);
        card.setBorder(new EmptyBorder(24, 28, 24, 28));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        int row = 0;

        // helper to add a vertical gap
        // ── Icon ─────────────────────────────────────────────
        JLabel icon = new JLabel("\uD83C\uDFDB", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        JPanel iconWrap = new JPanel(new GridBagLayout());
        iconWrap.setPreferredSize(new Dimension(56, 56));
        iconWrap.setBackground(BG_ICON);
        iconWrap.setBorder(BorderFactory.createLineBorder(BORDER_CARD, 1, true));
        iconWrap.add(icon);
        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        iconRow.setBackground(BG_PARCHMENT);
        iconRow.add(iconWrap);
        c.gridy = row++; c.insets = new Insets(0, 0, 12, 0);
        card.add(iconRow, c);

        // ── Title ────────────────────────────────────────────
        JLabel title = new JLabel("Library System", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(BROWN_MID);
        c.gridy = row++; c.insets = new Insets(0, 0, 2, 0);
        card.add(title, c);

        // ── Subtitle ─────────────────────────────────────────
        JLabel sub = new JLabel("Sign in to your account", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(BROWN_LIGHT);
        c.gridy = row++; c.insets = new Insets(0, 0, 16, 0);
        card.add(sub, c);

        // ── Role toggle ──────────────────────────────────────
        JToggleButton adminBtn = buildRoleBtn("\uD83D\uDEE1  Admin");
        JToggleButton userBtn  = buildRoleBtn("\uD83D\uDC64  User");
        ButtonGroup bg = new ButtonGroup();
        bg.add(adminBtn); bg.add(userBtn);
        adminBtn.setSelected(true);
        applyRoleStyle(adminBtn, true);
        applyRoleStyle(userBtn,  false);
        adminBtn.addItemListener(e -> applyRoleStyle(adminBtn, e.getStateChange() == ItemEvent.SELECTED));
        userBtn.addItemListener( e -> applyRoleStyle(userBtn,  e.getStateChange() == ItemEvent.SELECTED));

        JPanel roleRow = new JPanel(new GridLayout(1, 2, 8, 0));
        roleRow.setBackground(BG_PARCHMENT);
        roleRow.add(adminBtn);
        roleRow.add(userBtn);
        c.gridy = row++; c.insets = new Insets(0, 0, 4, 0);
        card.add(roleRow, c);

        // ── Hint label ───────────────────────────────────────
        JLabel hintLabel = new JLabel("Username: admin  |  Password: ••••••");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setForeground(BROWN_MUTED);
        c.gridy = row++; c.insets = new Insets(0, 0, 14, 0);
        card.add(hintLabel, c);

        adminBtn.addActionListener(e -> {
            hintLabel.setText("Username: admin  |  Password: ••••••");
            userField.setText(""); passField.setText(""); errorLabel.setText(" ");
        });
        userBtn.addActionListener(e -> {
            hintLabel.setText("Use your Member ID and password");
            userField.setText(""); passField.setText(""); errorLabel.setText(" ");
        });

        // ── Username label + field ────────────────────────────
        c.gridy = row++; c.insets = new Insets(0, 0, 4, 0);
        card.add(fieldLabel("USERNAME OR MEMBER ID"), c);

        userField = buildInputField("admin");
        c.gridy = row++; c.insets = new Insets(0, 0, 10, 0);
        card.add(userField, c);

        // ── Password label + field ────────────────────────────
        c.gridy = row++; c.insets = new Insets(0, 0, 4, 0);
        card.add(fieldLabel("PASSWORD"), c);

        passField = new JPasswordField();
        stylePassField(passField);
        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);
        card.add(passField, c);

        // ── Show password checkbox ────────────────────────────
        JCheckBox showPass = new JCheckBox("Show password");
        showPass.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        showPass.setForeground(BROWN_LIGHT);
        showPass.setBackground(BG_PARCHMENT);
        showPass.setFocusPainted(false);
        showPass.addActionListener(e ->
            passField.setEchoChar(showPass.isSelected() ? (char) 0 : '\u2022'));
        c.gridy = row++; c.insets = new Insets(0, 0, 4, 0);
        card.add(showPass, c);

        // ── Error label ──────────────────────────────────────
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        errorLabel.setForeground(new Color(0xB0, 0x3A, 0x2E));
        c.gridy = row++; c.insets = new Insets(0, 0, 10, 0);
        card.add(errorLabel, c);

        // ── Sign in button ───────────────────────────────────
        loginBtn = new JButton("Sign in  \u2192");
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginBtn.setBackground(BROWN_DARK);
        loginBtn.setForeground(BG_PARCHMENT);
        loginBtn.setFocusPainted(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setPreferredSize(new Dimension(0, 42));
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { loginBtn.setBackground(new Color(0xA0, 0x6C, 0x38)); }
            public void mouseExited(MouseEvent e)  { loginBtn.setBackground(BROWN_DARK); }
        });
        c.gridy = row++; c.insets = new Insets(0, 0, 12, 0);
        card.add(loginBtn, c);

        // ── Separator ─────────────────────────────────────────
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_FOOTER);
        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0);
        card.add(sep, c);

        // ── Footer ───────────────────────────────────────────
        JLabel footer = new JLabel("Library Management System", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        footer.setForeground(BROWN_MUTED);
        c.gridy = row++; c.insets = new Insets(0, 0, 0, 0);
        card.add(footer, c);

        // ── Login action ─────────────────────────────────────
        ActionListener doLogin = e -> attemptLogin(adminBtn.isSelected());
        loginBtn.addActionListener(doLogin);
        passField.addActionListener(doLogin);
        userField.addActionListener(doLogin);

        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private void attemptLogin(boolean isAdmin) {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Please enter both username and password.");
            shakeField(user.isEmpty() ? userField : passField);
            return;
        }

        if (isAdmin) {
            if (user.equals(ADMIN_USERNAME) && pass.equals(getAdminPassword())) {
                loggedInRole   = "admin";
                loggedInUserId = "admin";
                loggedInName   = "Administrator";
                animateSuccess();
            } else {
                showError("Invalid admin credentials.");
                shakeField(passField);
            }
        } else {
            String memberId = user.toUpperCase();
            Person member = memberService.findMemberById(memberId);
            if (member == null) {
                member = memberService.getAllMembers().stream()
                    .filter(m -> m.getId().equalsIgnoreCase(user))
                    .findFirst().orElse(null);
            }
            if (member == null) {
                showError("Member ID not found.");
                shakeField(userField);
                return;
            }
            String expected = memberService.getPassword(member.getId());
            if (!pass.equals(expected)) {
                showError("Incorrect password.");
                shakeField(passField);
                return;
            }
            loggedInRole   = "user";
            loggedInUserId = member.getId();
            loggedInName   = member.getName().trim();
            animateSuccess();
        }
    }

    private void animateSuccess() {
        loginBtn.setText("\u2713  Welcome!");
        loginBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13)); 
        loginBtn.setBackground(new Color(0x5A, 0x7A, 0x3A));
        errorLabel.setForeground(new Color(0x5A, 0x7A, 0x3A));
        errorLabel.setText("Login successful — loading...");
        errorLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));  // Set emoji 
        Timer t = new Timer(700, e -> onLoginSuccess.run());
        t.setRepeats(false);
        t.start();
    }

    private void showError(String msg) {
        errorLabel.setForeground(new Color(0xB0, 0x3A, 0x2E));
        errorLabel.setText(msg);
    }

    private void shakeField(JComponent field) {
        final int DIST = 8, STEPS = 10, DELAY = 30;
        final Point orig = field.getLocation();
        Timer timer = new Timer(DELAY, null);
        final int[] step = {0};
        timer.addActionListener(e -> {
            if (step[0] >= STEPS) { field.setLocation(orig); timer.stop(); }
            else field.setLocation(orig.x + (step[0] % 2 == 0 ? DIST : -DIST), orig.y);
            step[0]++;
        });
        timer.start();
    }

    public static String generatePassword(String memberId) {
        String clean = memberId.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        String part  = clean.length() >= 3 ? clean.substring(0, 3) : clean;
        return part + "2024";
    }

    public String getRole()     { return loggedInRole; }
    public String getUserId()   { return loggedInUserId; }
    public String getUserName() { return loggedInName; }

    // ── Helpers ───────────────────────────────────────────────
    private JLabel centeredLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return l;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(BROWN_LIGHT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField buildInputField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_INPUT);
        f.setCaretColor(TEXT_INPUT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_INPUT, 1, true),
            new EmptyBorder(8, 10, 8, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Placeholder effect
        f.setText(placeholder);
        f.setForeground(TEXT_PH);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(TEXT_INPUT); }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(TEXT_PH); }
            }
        });
        return f;
    }

    private void stylePassField(JPasswordField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_INPUT);
        f.setCaretColor(TEXT_INPUT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_INPUT, 1, true),
            new EmptyBorder(8, 10, 8, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JToggleButton buildRoleBtn(String text) {
        JToggleButton btn = new JToggleButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));  // Bigger font
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createLineBorder(BORDER_CARD, 2, true));  // Thicker border
        
        // Make buttons physically larger
        btn.setPreferredSize(new Dimension(100, 35));
        btn.setMinimumSize(new Dimension(90, 32));
        
        return btn;
    }
    private void applyRoleStyle(JToggleButton btn, boolean selected) {
        btn.setBackground(selected ? BROWN_DARK : BG_ROLE);
        btn.setForeground(selected ? BG_PARCHMENT : BROWN_LIGHT);
    }
}