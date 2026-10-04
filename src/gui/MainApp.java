package com.library.gui;

import com.library.services.*;

import com.library.utils.BorrowGraph;
import com.library.db.DatabaseManager;
import com.library.db.ItemDAO;
import com.library.db.MemberDAO;
import com.library.db.LoanDAO;
import com.library.db.ReservationDAO;
import com.library.models.items.LibraryItem;
import com.library.models.persons.Person;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.function.BiConsumer;

public class MainApp extends JFrame {

    private LibraryService     libraryService;
    private MemberService      memberService;
    private LoanService        loanService;
    private ReservationService reservationService;
    private BorrowGraph        borrowGraph;

    private JPanel     contentArea;
    private CardLayout cardLayout;

    private LoginPanel loginPanel;
    private String     role;
    private String     userId;
    private String     userName;

    private JLabel greetingLabel;

    public MainApp() {
        System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
            @Override public void write(int b) {}
        }));
        System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
            @Override public void write(int b) {}
        }));

        GuiUtils.applyGlobalTheme();

        libraryService     = new LibraryService();
        memberService      = new MemberService();
        loanService        = new LoanService(memberService, libraryService);
        reservationService = new ReservationService(memberService, libraryService);
        borrowGraph        = new BorrowGraph();

        //loadAllData();
        DatabaseManager.initializeSchema();

        setTitle("Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        setBackground(GuiUtils.BG_DARK);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                saveAllData();
                DatabaseManager.closeConnection();
            }
        });

        setLayout(new BorderLayout());
        showLoginScreen();
        setVisible(true);
    }

    private void showLoginScreen() {
        getContentPane().removeAll();
        loginPanel = new LoginPanel(memberService, this::onLoginSuccess);
        getContentPane().add(loginPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void onLoginSuccess() {
        role     = loginPanel.getRole();
        userId   = loginPanel.getUserId();
        userName = loginPanel.getUserName();

        getContentPane().removeAll();
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildTopBar(),  BorderLayout.NORTH);

        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(GuiUtils.BG_DARK);
        buildPanels();
        add(contentArea, BorderLayout.CENTER);

        cardLayout.show(contentArea, "Dashboard");
        revalidate();
        repaint();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(GuiUtils.BG_SIDEBAR);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, GuiUtils.BORDER_COL),
            new EmptyBorder(6, 16, 6, 16)
        ));

        String roleText = "admin".equals(role) ? "  \uD83D\uDEE1 Admin" : "  \uD83D\uDC64 User";
        JLabel roleLbl  = new JLabel(roleText);
        roleLbl.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        roleLbl.setForeground("admin".equals(role) ? GuiUtils.ACCENT : GuiUtils.ACCENT_GRN);

        greetingLabel = new JLabel("Hello, " + userName + "  \uD83D\uDC4B");
        greetingLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        greetingLabel.setForeground(new Color(230, 210, 185));

        JButton profileBtn = new JButton("\uD83D\uDC64  " + userName + "  \u25BE");
        profileBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        profileBtn.setForeground(new Color(230, 210, 185));
        profileBtn.setBackground(new Color(80, 55, 30));
        profileBtn.setBorderPainted(false);
        profileBtn.setFocusPainted(false);
        profileBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GuiUtils.BORDER_COL, 1, true),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        profileBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { profileBtn.setBackground(new Color(100, 70, 35)); }
            public void mouseExited(MouseEvent e)  { profileBtn.setBackground(new Color(80, 55, 30)); }
        });

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(GuiUtils.BG_PANEL);
        menu.setBorder(BorderFactory.createLineBorder(GuiUtils.BORDER_COL));

        JMenuItem showInfoItem = styledMenuItem("\uD83D\uDCCB  Show My Info");
        showInfoItem.addActionListener(e -> showUserInfoDialog());
        menu.add(showInfoItem);
        menu.addSeparator();

        JMenuItem changePwdItem = styledMenuItem("\uD83D\uDD11  Change Password");
        changePwdItem.addActionListener(e -> showChangePasswordDialog());
        menu.add(changePwdItem);

        if (!"admin".equals(role)) {
            JMenuItem changeNameItem = styledMenuItem("\u270F  Change Display Name");
            changeNameItem.addActionListener(e -> showChangeNameDialog());
            menu.add(changeNameItem);
        }
        menu.addSeparator();

        JMenuItem logoutItem = styledMenuItem("\uD83D\uDEAA  Logout");
        logoutItem.setForeground(GuiUtils.ACCENT_RED);
        logoutItem.addActionListener(e -> {
            if (MainApp.confirm(this, "Log out of the system?"))
                showLoginScreen();
        });
        menu.add(logoutItem);

        profileBtn.addActionListener(e -> menu.show(profileBtn, 0, profileBtn.getHeight()));

        JPanel left  = new JPanel(new FlowLayout(FlowLayout.LEFT,  8, 0));
        left.setBackground(GuiUtils.BG_SIDEBAR);
        left.add(roleLbl);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(GuiUtils.BG_SIDEBAR);
        right.add(greetingLabel);
        right.add(profileBtn);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JMenuItem styledMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        item.setBackground(GuiUtils.BG_PANEL);
        item.setForeground(GuiUtils.TEXT_PRI);
        item.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        item.setOpaque(true);
        return item;
    }

    private void showUserInfoDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 14, 8));
        panel.setBackground(GuiUtils.BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        BiConsumer<String, String> addRow = (lbl, val) -> {
            JLabel l = new JLabel(lbl + ":");
            l.setFont(GuiUtils.FONT_BOLD);
            l.setForeground(GuiUtils.TEXT_SEC);
            JLabel v = new JLabel(val != null ? val : "\u2014");
            v.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            v.setForeground(GuiUtils.TEXT_PRI);
            panel.add(l); panel.add(v);
        };

        if ("admin".equals(role)) {
            addRow.accept("Username", "admin");
            addRow.accept("Role",     "Administrator");
            addRow.accept("Password", LoginPanel.getAdminPassword());
        } else {
            com.library.models.persons.Person p = memberService.findMemberById(userId);
            addRow.accept("Member ID", userId);
            addRow.accept("Name",      userName);
            addRow.accept("Role",      "User");
            if (p != null) {
                addRow.accept("Email",  p.getEmail());
                addRow.accept("Phone",  p.getPhoneNumber());
                addRow.accept("Type",   p.getClass().getSimpleName());
            }
            addRow.accept("Password", memberService.getPassword(userId));
        }

        JOptionPane.showMessageDialog(this, panel,
            "\uD83D\uDCCB  My Account Info", JOptionPane.PLAIN_MESSAGE);
    }

    private void buildPanels() {
        contentArea.removeAll();
        boolean isAdmin = "admin".equals(role);

        contentArea.add(new DashboardPanel(libraryService, memberService,
                loanService, reservationService),                                   "Dashboard");
        contentArea.add(new ItemsPanel(libraryService, isAdmin),                    "Items");
        contentArea.add(new MembersPanel(memberService, isAdmin),                   "Members");
        contentArea.add(new LoansPanel(loanService, memberService, libraryService,
                isAdmin ? null : userId),                                           "Loans");
        contentArea.add(new ReservationsPanel(reservationService, memberService,
                libraryService, isAdmin ? null : userId),                           "Reservations");
        contentArea.add(new SearchPanel(libraryService),                            "Search");
        contentArea.add(new StatisticsPanel(libraryService, memberService,
                loanService, reservationService, borrowGraph),                      "Statistics");
        contentArea.add(new ReflectionPanel(libraryService, memberService,
                loanService),                                                       "Inspect");
        contentArea.add(new DatabasePanel(libraryService, memberService,
                loanService, reservationService),                                   "Database");
    }

    private JPanel buildSidebar() {
        boolean isAdmin = "admin".equals(role);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(GuiUtils.BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, GuiUtils.BORDER_COL));
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel logo = new JLabel("  LibSys");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(new Color(230, 210, 185));
        logo.setBorder(BorderFactory.createEmptyBorder(20, 10, 18, 10));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(GuiUtils.darkSeparator());
        sidebar.add(Box.createVerticalStrut(8));

        String[][] navItems = isAdmin ? new String[][]{
            {"\uD83C\uDFE0", "Dashboard",    "Dashboard"},
            {"\uD83D\uDCD6", "Items",        "Items"},
            {"\uD83D\uDC64", "Members",      "Members"},
            {"\uD83D\uDCE4", "Loans",        "Loans"},
            {"\uD83D\uDCC5", "Reservations", "Reservations"},
            {"\uD83D\uDD0D", "Search",       "Search"},
            {"\uD83D\uDCCA", "Statistics",   "Statistics"},
        } : new String[][]{
            {"\uD83C\uDFE0", "Dashboard",       "Dashboard"},
            {"\uD83D\uDCD6", "Items",           "Items"},
            {"\uD83D\uDD0D", "Search",          "Search"},
            {"\uD83D\uDCE4", "My Loans",        "Loans"},
            {"\uD83D\uDCC5", "My Reservations", "Reservations"},
        };

        ButtonGroup group = new ButtonGroup();
        for (String[] nav : navItems) {
            JToggleButton btn = buildNavButton(nav[0], nav[1], nav[2]);
            group.add(btn);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(2));
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(GuiUtils.darkSeparator());

        if (isAdmin) {
            JButton saveBtn = buildSidebarAction("\uD83D\uDCBE  Save All Data");
            saveBtn.addActionListener(e -> {
                saveAllData();
                JOptionPane.showMessageDialog(this,
                    "All data saved successfully.", "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
            });

        
        sidebar.add(Box.createVerticalStrut(10));
        
        
        }
     return sidebar;
    }

    private JToggleButton buildNavButton(String icon, String label, String card) {
        JToggleButton btn = new JToggleButton();
        btn.setLayout(new BorderLayout(4, 0));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        iconLbl.setForeground(new Color(200, 175, 145));
        iconLbl.setPreferredSize(new Dimension(30, 0));

        JLabel textLbl = new JLabel(label);
        textLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textLbl.setForeground(new Color(200, 175, 145));

        btn.add(iconLbl, BorderLayout.WEST);
        btn.add(textLbl, BorderLayout.CENTER);

        btn.setBackground(GuiUtils.BG_SIDEBAR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setMaximumSize(new Dimension(220, 40));
        btn.setPreferredSize(new Dimension(220, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 8));
        btn.setIcon(null); btn.setSelectedIcon(null);
        btn.setRolloverIcon(null); btn.setPressedIcon(null); btn.setDisabledIcon(null);

        btn.addActionListener(e -> cardLayout.show(contentArea, card));
        btn.addItemListener(e -> {
            boolean sel = e.getStateChange() == ItemEvent.SELECTED;
            Color fg = sel ? Color.WHITE          : new Color(200, 175, 145);
            Color bg = sel ? new Color(93, 64, 35) : GuiUtils.BG_SIDEBAR;
            Font  f  = sel ? new Font("Segoe UI", Font.BOLD,  13)
                           : new Font("Segoe UI", Font.PLAIN, 13);
            btn.setBackground(bg);
            iconLbl.setForeground(fg);
            textLbl.setForeground(fg);
            textLbl.setFont(f);
            btn.repaint();
        });
        return btn;
    }

    private JButton buildSidebarAction(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        btn.setForeground(new Color(180, 155, 120));
        btn.setBackground(GuiUtils.BG_SIDEBAR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 38));
        btn.setPreferredSize(new Dimension(220, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 8));
        return btn;
    }

   /* private void loadAllData() {
        if (new File("data/items.txt").exists())        libraryService.loadFromFile("items.txt");
        if (new File("data/members.txt").exists())      memberService.loadFromFile("members.txt");
        if (new File("data/loans.txt").exists())        loanService.loadFromFile("loans.txt");
        if (new File("data/reservations.txt").exists()) reservationService.loadFromFile("reservations.txt");
    }*/

    private void saveAllData() {
        libraryService.saveToFile("items.txt");
        memberService.saveToFile("members.txt");
        loanService.saveToFile("loans.txt");
        reservationService.saveToFile("reservations.txt");
    }

    // ── Change Password dialog ─────────────────────────────────
    private void showChangePasswordDialog() {
        JDialog dialog = new JDialog(this, "Change Password", true);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(GuiUtils.BG_PANEL);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(GuiUtils.BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 10, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx   = 0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel titleLbl = new JLabel("\uD83D\uDD11  Change Your Password");
        titleLbl.setFont(new Font("Segoe UI Emoji", Font.BOLD, 15));
        titleLbl.setForeground(GuiUtils.ACCENT);
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 16, 0);
        panel.add(titleLbl, gbc);

        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(makeLabel("Current Password:"), gbc);
        JPasswordField currentField = styledPassField();
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(currentField, gbc);

        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(makeLabel("New Password:"), gbc);
        JPasswordField newField = styledPassField();
        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(newField, gbc);

        gbc.gridy = 5; gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(makeLabel("Confirm New Password:"), gbc);
        JPasswordField confirmField = styledPassField();
        gbc.gridy = 6; gbc.insets = new Insets(0, 0, 8, 0);
        panel.add(confirmField, gbc);

        JCheckBox showCb = new JCheckBox("Show passwords");
        showCb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        showCb.setForeground(GuiUtils.TEXT_SEC);
        showCb.setBackground(GuiUtils.BG_PANEL);
        showCb.setFocusPainted(false);
        showCb.addActionListener(e -> {
            char echo = showCb.isSelected() ? (char) 0 : '\u2022';
            currentField.setEchoChar(echo);
            newField.setEchoChar(echo);
            confirmField.setEchoChar(echo);
        });
        gbc.gridy = 7; gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(showCb, gbc);

        // Fixed-size error label — always holds space, text is always visible
        JLabel errorLbl = new JLabel(" ");
        errorLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLbl.setForeground(GuiUtils.ACCENT_RED);
        errorLbl.setPreferredSize(new Dimension(370, 18));
        gbc.gridy = 8; gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(errorLbl, gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnRow.setBackground(GuiUtils.BG_PANEL);
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL));
        JButton cancelBtn = GuiUtils.ghostButton("Cancel");
        JButton saveBtn   = GuiUtils.primaryButton("Save Password");
        cancelBtn.addActionListener(e -> dialog.dispose());
        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);

        saveBtn.addActionListener(e -> {
            String current = new String(currentField.getPassword()).trim();
            String np      = new String(newField.getPassword()).trim();
            String cp      = new String(confirmField.getPassword()).trim();

            boolean currentOk = "admin".equals(role)
                ? current.equals(LoginPanel.getAdminPassword())
                : current.equals(memberService.getPassword(userId));

            // FIX: errorLbl.repaint() — not dialog.repaint() — so the label
            // text change is flushed immediately on screen.
            if (!currentOk) {
                errorLbl.setText("Current password is incorrect.");
                errorLbl.repaint();
                return;
            }
            if (np.isEmpty()) {
                errorLbl.setText("New password cannot be empty.");
                errorLbl.repaint();
                return;
            }
            if (np.length() < 4) {
                errorLbl.setText("Password must be at least 4 characters.");
                errorLbl.repaint();
                return;
            }
            if (!np.equals(cp)) {
                errorLbl.setText("New passwords do not match.");
                errorLbl.repaint();
                return;
            }

            if ("admin".equals(role)) {
                LoginPanel.updateAdminPassword(np);
            } else {
                memberService.setPassword(userId, np);
                saveAllData();
            }

            // Show success BEFORE dispose so the parent window is still valid
            showInfo(this, "Password Changed", "Your password has been updated successfully.");
            dialog.dispose();
        });

        dialog.add(panel,  BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── Change Display Name (user only) ───────────────────────
    private void showChangeNameDialog() {
        String current = userName;
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(GuiUtils.BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 24, 10, 24));

        JLabel titleLbl = new JLabel("\u270F  Change Your Name");
        titleLbl.setFont(new Font("Segoe UI Emoji", Font.BOLD, 15));
        titleLbl.setForeground(GuiUtils.ACCENT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLbl);
        panel.add(Box.createVerticalStrut(14));

        panel.add(makeLabel("Current Name: " + current));
        panel.add(Box.createVerticalStrut(10));
        panel.add(makeLabel("New Name:"));
        panel.add(Box.createVerticalStrut(4));

        JTextField nameField = GuiUtils.styledField(current);
        nameField.setText(current);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(nameField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Change Name", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newName = nameField.getText().trim();
        if (newName.isEmpty()) { showError(this, "Invalid Name", "Name cannot be empty."); return; }
        if (!newName.matches("[a-zA-Z\\s\\-']+")) {
            showError(this, "Invalid Name", "Name must contain letters only."); return;
        }

        com.library.models.persons.Person p = memberService.findMemberById(userId);
        if (p != null) {
            p.setName(newName);
            memberService.updateMember(userId, p);
            saveAllData();
        }
        userName = newName;
        greetingLabel.setText("Hello, " + userName + "  \uD83D\uDC4B");
        showInfo(this, "Name Changed", "Your name has been updated to: " + newName);
    }

    private JPasswordField styledPassField() {
        JPasswordField f = new JPasswordField(20);
        f.setFont(GuiUtils.FONT_BODY);
        f.setBackground(GuiUtils.BG_CARD);
        f.setForeground(GuiUtils.TEXT_PRI);
        f.setCaretColor(GuiUtils.TEXT_PRI);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GuiUtils.BORDER_COL),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setPreferredSize(new Dimension(370, 36));
        return f;
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(GuiUtils.TEXT_SEC);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public static void showInfo(Component parent, String title, String msg) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String title, String msg) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainApp::new);
    }
}