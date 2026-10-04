package com.library.gui;

import com.library.models.persons.*;
import com.library.services.MemberService;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

public class MembersPanel extends JPanel {

    private final MemberService memberService;
    private final DefaultTableModel model;
    private final JTable table;
    private final JPanel badgeRow;
    private final boolean isAdmin;

    public MembersPanel(MemberService memberService) {
        this(memberService, true);
    }

    /**
     * @wbp.parser.constructor
     */
    public MembersPanel(MemberService memberService, boolean isAdmin) {
        this.memberService = memberService;
        this.isAdmin       = isAdmin;

        setLayout(new BorderLayout(0, 16));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("\uD83D\uDC64  Members"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("Register and manage library members"), BorderLayout.SOUTH);

        badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        badgeRow.setBackground(GuiUtils.BG_DARK);
        rebuildBadges();

        JButton addBtn     = GuiUtils.iconButton("\uD83D\uDC64", "Register", GuiUtils.BTN_PRIMARY, Color.WHITE, true);
        JButton editBtn    = GuiUtils.iconButton("\u270F",       "Edit",     GuiUtils.BG_CARD,     GuiUtils.TEXT_SEC, false);
        JButton infoBtn    = GuiUtils.iconButton("\u2139",       "Full Info", GuiUtils.BG_CARD,    GuiUtils.TEXT_SEC, false);
        JButton deleteBtn  = GuiUtils.iconButton("\uD83D\uDDD1", "Delete",   GuiUtils.ACCENT_RED,  Color.WHITE, false);
        JButton refreshBtn = GuiUtils.iconButton("\uD83D\uDD04", "Refresh",  GuiUtils.BG_CARD,     GuiUtils.TEXT_SEC, false);

        addBtn.addActionListener(e    -> {
            if (!isAdmin) { accessDenied("register members"); return; }
            showRegisterDialog(addBtn);
        });
        editBtn.addActionListener(e   -> {
            if (!isAdmin) { accessDenied("edit members"); return; }
            showEditDialog();
        });
        infoBtn.addActionListener(e   -> showFullInfoDialog());
        deleteBtn.addActionListener(e -> {
            if (!isAdmin) { accessDenied("delete members"); return; }
            deleteSelected();
        });
        refreshBtn.addActionListener(e -> refreshTable());

        JPanel toolbar = isAdmin
            ? GuiUtils.toolbar(addBtn, editBtn, infoBtn, deleteBtn, refreshBtn)
            : GuiUtils.toolbar(infoBtn, refreshBtn);

        String[] cols = {"ID", "Type", "Name", "Email", "Phone", "Address"};
        model = new DefaultTableModel(cols, 0);
        table = GuiUtils.styledTable(model);

        table.getColumnModel().getColumn(1).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JLabel lbl = new JLabel(value != null ? value.toString() : "");
                lbl.setFont(GuiUtils.FONT_BOLD);
                lbl.setOpaque(true);
                lbl.setBackground(isSelected ? new Color(139, 90, 43, 50) : GuiUtils.BG_CARD);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                String s = value != null ? value.toString() : "";
                lbl.setForeground(
                    "Professor".equals(s) ? GuiUtils.ACCENT     :
                    "Student".equals(s)   ? GuiUtils.ACCENT_GRN :
                    "Staff".equals(s)     ? GuiUtils.ACCENT_YEL :
                                            GuiUtils.TEXT_SEC);
                return lbl;
            });

        // ── Hover tooltip ─────────────────────────────────────
        table.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    String id = (String) model.getValueAt(row, 0);
                    Person p  = memberService.findMemberById(id);
                    if (p != null) table.setToolTipText(buildTooltip(p));
                } else {
                    table.setToolTipText(null);
                }
            }
        });

        // ── Double-click = full info ───────────────────────────
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showFullInfoDialog();
            }
        });

        JScrollPane scroll = GuiUtils.scrollPane(table);
        JPanel tableCard   = GuiUtils.sectionCard("", scroll);

        JPanel topArea = new JPanel(new BorderLayout(0, 8));
        topArea.setBackground(GuiUtils.BG_DARK);
        topArea.add(header,   BorderLayout.NORTH);
        topArea.add(badgeRow, BorderLayout.CENTER);
        topArea.add(toolbar,  BorderLayout.SOUTH);

        add(topArea,   BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
        refreshTable();

        // Auto-refresh when this panel becomes visible
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshTable();
            }
        });
    }

    private void accessDenied(String action) {
        MainApp.showError(this, "Access Denied",
            "You do not have permission to " + action + ".\n"
            + "This action requires admin privileges.");
    }

    private void rebuildBadges() {
        badgeRow.removeAll();
        Map<String, Integer> counts = memberService.getMemberTypeCount();
        if (counts.isEmpty()) {
            badgeRow.add(GuiUtils.badge("No members yet", GuiUtils.TEXT_SEC));
        } else {
            counts.forEach((type, count) ->
                badgeRow.add(GuiUtils.badge(type + ": " + count, GuiUtils.ACCENT)));
        }
        badgeRow.revalidate();
        badgeRow.repaint();
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (Person p : memberService.getAllMembers()) {
            model.addRow(new Object[]{
                p.getId(), p.getClass().getSimpleName(),
                p.getName(), p.getEmail(), p.getPhoneNumber(), p.getAddress()
            });
        }
        rebuildBadges();
    }

    // ── FULL INFO DIALOG ──────────────────────────────────────
    private void showFullInfoDialog() {
        int row = table.getSelectedRow();
        if (row < 0) {
            MainApp.showError(this, "No Selection", "Please select a member first."); return;
        }
        String id = (String) model.getValueAt(row, 0);
        Person p  = memberService.findMemberById(id);
        if (p == null) return;

        JPanel info = new JPanel(new GridLayout(0, 2, 16, 8));
        info.setBackground(GuiUtils.BG_PANEL);
        info.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        addRow(info, "ID",    p.getId());
        addRow(info, "Type",  p.getClass().getSimpleName());
        addRow(info, "Name",  p.getName());
        addRow(info, "Email", p.getEmail());
        addRow(info, "Phone", p.getPhoneNumber());
        addRow(info, "Address", p.getAddress());

        if (p instanceof Professor) {
            Professor pr = (Professor) p;
            addRow(info, "Department",  pr.getDepartment());
            addRow(info, "Speciality",  pr.getSpeciality());
            addRow(info, "Loan Limit",  String.valueOf(pr.getMaxLoanLimit()));
        } else if (p instanceof Student) {
            Student st = (Student) p;
            addRow(info, "Department", st.getDepartment());
            addRow(info, "Loan Limit", String.valueOf(st.getMaxLoanLimit()));
        } else if (p instanceof Staff) {
            Staff st = (Staff) p;
            addRow(info, "Position",   st.getPosition());
            addRow(info, "Loan Limit", String.valueOf(st.getMaxLoanLimit()));
        } else if (p instanceof Member) {
            Member m = (Member) p;
            addRow(info, "Position",   m.getPosition());
            addRow(info, "Loan Limit", String.valueOf(m.getMaxLoanLimit()));
        }

        // Admin sees password
        if (isAdmin) {
            info.add(new JLabel()); info.add(new JLabel()); // spacer row

            JLabel passLbl = new JLabel("Password (admin only):");
            passLbl.setFont(GuiUtils.FONT_BOLD);
            passLbl.setForeground(GuiUtils.ACCENT_RED);

            String pw = memberService.getPassword(p.getId());
            JLabel passVal = new JLabel(pw);
            passVal.setFont(new Font("Monospaced", Font.BOLD, 13));
            passVal.setForeground(GuiUtils.ACCENT_RED);

            info.add(passLbl);
            info.add(passVal);
        }

        // Bottom: Change Password button for admin
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setBackground(GuiUtils.BG_PANEL);
        wrapper.add(info, BorderLayout.CENTER);

        if (isAdmin) {
            JButton changePwdBtn = GuiUtils.dangerButton("\uD83D\uDD11  Change Member Password");
            changePwdBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            changePwdBtn.addActionListener(ev -> {
                showChangeMemberPasswordDialog(p);
            });
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnRow.setBackground(GuiUtils.BG_PANEL);
            btnRow.add(changePwdBtn);
            wrapper.add(btnRow, BorderLayout.SOUTH);
        }

        JOptionPane.showMessageDialog(this, wrapper,
            "Member Info \u2014 " + p.getName(), JOptionPane.PLAIN_MESSAGE);
    }

    // ── ADMIN: CHANGE A MEMBER'S PASSWORD ─────────────────────
    private void showChangeMemberPasswordDialog(Person p) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(GuiUtils.BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel info = new JLabel("Setting new password for: " + p.getName() + "  [" + p.getId() + "]");
        info.setFont(GuiUtils.FONT_BOLD);
        info.setForeground(GuiUtils.ACCENT);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(info);
        panel.add(Box.createVerticalStrut(14));

        JPasswordField newField     = styledPass();
        JPasswordField confirmField = styledPass();
        JCheckBox showCb = new JCheckBox("Show passwords");
        showCb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        showCb.setForeground(GuiUtils.TEXT_SEC);
        showCb.setBackground(GuiUtils.BG_PANEL);
        showCb.setFocusPainted(false);
        showCb.addActionListener(e -> {
            char echo = showCb.isSelected() ? (char) 0 : '\u2022';
            newField.setEchoChar(echo);
            confirmField.setEchoChar(echo);
        });

        panel.add(makeLabel("New Password:"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(newField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(makeLabel("Confirm New Password:"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(confirmField);
        panel.add(Box.createVerticalStrut(6));
        panel.add(showCb);

        JLabel errorLbl = new JLabel(" ");
        errorLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLbl.setForeground(GuiUtils.ACCENT_RED);
        errorLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(Box.createVerticalStrut(4));
        panel.add(errorLbl);

        // Custom dialog with OK/Cancel
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), "Change Member Password", true);
        dialog.setSize(400, 310);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(GuiUtils.BG_PANEL);
        dialog.getContentPane().setLayout(new BorderLayout());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnRow.setBackground(GuiUtils.BG_PANEL);
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL));
        JButton cancelBtn = GuiUtils.ghostButton("Cancel");
        JButton saveBtn   = GuiUtils.primaryButton("Save Password");

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            String np = new String(newField.getPassword()).trim();
            String cp = new String(confirmField.getPassword()).trim();
            if (np.isEmpty()) { errorLbl.setText("New password cannot be empty."); return; }
            if (np.length() < 4) { errorLbl.setText("Password must be at least 4 characters."); return; }
            if (!np.equals(cp)) { errorLbl.setText("Passwords do not match."); return; }
            memberService.setPassword(p.getId(), np);
            dialog.dispose();
            MainApp.showInfo(this, "Password Changed",
                "Password for " + p.getName() + " updated successfully.\nNew password: " + np);
        });

        btnRow.add(cancelBtn); btnRow.add(saveBtn);
        dialog.getContentPane().add(panel,  BorderLayout.CENTER);
        dialog.getContentPane().add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPasswordField styledPass() {
        JPasswordField f = new JPasswordField(20);
        f.setFont(GuiUtils.FONT_BODY);
        f.setBackground(Color.WHITE);
        f.setForeground(GuiUtils.TEXT_PRI);
        f.setCaretColor(GuiUtils.TEXT_PRI);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GuiUtils.BORDER_COL),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(GuiUtils.TEXT_SEC);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ── EDIT DIALOG ───────────────────────────────────────────
    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row < 0) {
            MainApp.showError(this, "No Selection", "Please select a member to edit."); return;
        }
        String id = (String) model.getValueAt(row, 0);
        Person p  = memberService.findMemberById(id);
        if (p == null) return;

        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), "Edit Member", true);
        dialog.setSize(460, 360);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(GuiUtils.BG_PANEL);
        dialog.getContentPane().setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(GuiUtils.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));

        JTextField fName  = GuiUtils.styledField(p.getName());
        JTextField fEmail = GuiUtils.styledField(p.getEmail());
        JTextField fPhone = GuiUtils.styledField(p.getPhoneNumber());
        JTextField fAddr  = GuiUtils.styledField(p.getAddress());

        fName.setText(p.getName());
        fEmail.setText(p.getEmail());
        fPhone.setText(p.getPhoneNumber());
        fAddr.setText(p.getAddress());

        form.add(GuiUtils.formRow("Name",    fName));  form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("Email",   fEmail)); form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("Phone",   fPhone)); form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("Address", fAddr));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(GuiUtils.BG_PANEL);
        btns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL));
        JButton cancel = GuiUtils.ghostButton("Cancel");
        JButton save   = GuiUtils.primaryButton("Save Changes");
        cancel.addActionListener(e -> dialog.dispose());

        save.addActionListener(e -> {
            String name  = fName.getText().trim();
            String email = fEmail.getText().trim();
            String phone = fPhone.getText().trim();
            String addr  = fAddr.getText().trim();

            if (name.isEmpty()) {
                MainApp.showError(dialog, "Invalid Name", "Name cannot be empty."); return;
            }
            if (!name.matches("[a-zA-Z\\s\\-']+")) {
                MainApp.showError(dialog, "Invalid Name",
                    "Name must contain letters only."); return;
            }
            if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                MainApp.showError(dialog, "Invalid Email",
                    "Email must be in valid format.\nExample: name@gmail.com"); return;
            }
            if (!phone.matches("(05|06|07)\\d{8}")) {
                MainApp.showError(dialog, "Invalid Phone",
                    "Phone must start with 05, 06, or 07 and have 10 digits."); return;
            }
            if (addr.isEmpty()) {
                MainApp.showError(dialog, "Invalid Address", "Address cannot be empty."); return;
            }

            // Apply changes by creating updated person of same type
            p.setName(name);
            p.setEmail(email);
            p.setPhoneNumber(phone);
            p.setAddress(addr);

            memberService.updateMember(id, p);
            refreshTable();
            MainApp.showInfo(this, "Updated", name + "'s information has been updated.");
            dialog.dispose();
        });

        btns.add(cancel); btns.add(save);
        dialog.getContentPane().add(form, BorderLayout.CENTER);
        dialog.getContentPane().add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── DELETE ────────────────────────────────────────────────
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            MainApp.showError(this, "No Selection", "Please select a member first."); return;
        }
        String id   = (String) model.getValueAt(row, 0);
        String name = (String) model.getValueAt(row, 2);
        if (MainApp.confirm(this, "Delete member \"" + name + "\"?")) {
            memberService.deleteMember(id);
            refreshTable();
        }
    }

    // ── REGISTER DIALOG with shake animation ──────────────────
    private void showRegisterDialog(JButton triggerBtn) {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), "Register New Member", true);
        dialog.setSize(460, 490);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(GuiUtils.BG_PANEL);
        dialog.getContentPane().setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(GuiUtils.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));

        JComboBox<String> typeCb = GuiUtils.styledCombo("Professor", "Student", "Staff", "Member");
        JTextField fId    = GuiUtils.styledField("e.g. S005");
        JTextField fName  = GuiUtils.styledField("Full name (letters only)");
        JTextField fEmail = GuiUtils.styledField("example@gmail.com");
        JTextField fPhone = GuiUtils.styledField("Starts with 05/06/07, e.g. 0555123456");
        JTextField fAddr  = GuiUtils.styledField("City");
        JTextField fE2    = GuiUtils.styledField("Speciality");

        String[] professorDepts = {"Computer Science","Mathematics","Physics","Chemistry",
            "Biology","Engineering","Economics","Law","Medicine","Literature","History","Other"};
        String[] studentDepts   = professorDepts.clone();
        String[] staffPositions = {"Librarian","Administrator","Security","Technician",
            "Receptionist","Accountant","IT Support","Cleaning Staff","Other"};
        String[] memberPositions= {"Student","Employee","Researcher","Visitor","Other"};

        JComboBox<String> fE1Cb = GuiUtils.styledCombo(professorDepts);
        JPanel e2Row = GuiUtils.formRow("Speciality", fE2);
        e2Row.setVisible(true);

        typeCb.addActionListener(e -> {
            String t = (String) typeCb.getSelectedItem();
            fE1Cb.removeAllItems();
            String[] opts = "Professor".equals(t) ? professorDepts
                          : "Student".equals(t)   ? studentDepts
                          : "Staff".equals(t)      ? staffPositions
                          :                          memberPositions;
            for (String o : opts) fE1Cb.addItem(o);
            e2Row.setVisible("Professor".equals(t));
            e2Row.revalidate();
        });

        form.add(GuiUtils.formRow("Type",     typeCb)); form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("ID",       fId));    form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("Name",     fName));  form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("Email",    fEmail)); form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("Phone",    fPhone)); form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("Address",  fAddr));  form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("Dept/Pos", fE1Cb));  form.add(Box.createVerticalStrut(8));
        form.add(e2Row);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(GuiUtils.BG_PANEL);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL));
        JButton cancelBtn = GuiUtils.ghostButton("Cancel");
        JButton okBtn     = GuiUtils.primaryButton("Register");
        cancelBtn.addActionListener(e -> dialog.dispose());

        okBtn.addActionListener(e -> {
            String type  = (String) typeCb.getSelectedItem();
            String id    = fId.getText().trim().toUpperCase();
            String name  = fName.getText().trim();
            String email = fEmail.getText().trim();
            String phone = fPhone.getText().trim();
            String addr  = fAddr.getText().trim();
            String e1    = (String) fE1Cb.getSelectedItem();
            String e2    = fE2.getText().trim();

            // Validate with shake on error
            if (id.isEmpty())                               { shake(fId);    MainApp.showError(dialog, "Invalid ID",       "ID cannot be empty.");               return; }
            if (memberService.findMemberById(id) != null)   { shake(fId);    MainApp.showError(dialog, "Duplicate ID",     "ID \"" + id + "\" already exists."); return; }
            if (name.isEmpty())                             { shake(fName);  MainApp.showError(dialog, "Invalid Name",     "Name cannot be empty.");             return; }
            if (!name.matches("[a-zA-Z\\s\\-']+"))          { shake(fName);  MainApp.showError(dialog, "Invalid Name",     "Letters only.");                     return; }
            if (email.isEmpty() || !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                shake(fEmail); MainApp.showError(dialog, "Invalid Email", "Enter a valid email.\nExample: name@gmail.com"); return;
            }
            if (!phone.matches("(05|06|07)\\d{8}"))         { shake(fPhone); MainApp.showError(dialog, "Invalid Phone",   "Must start with 05/06/07, 10 digits."); return; }
            if (addr.isEmpty())                             { shake(fAddr);  MainApp.showError(dialog, "Invalid Address", "Address cannot be empty.");           return; }
            if ("Professor".equals(type) && e2.isEmpty())   { shake(fE2);    MainApp.showError(dialog, "Invalid Speciality", "Speciality required for Professors."); return; }

            Person person;
            switch (type) {
                case "Professor": person = new Professor(id, name, email, phone, addr, e1, e2); break;
                case "Student":   person = new Student(id, name, email, phone, addr, 5, e1);    break;
                case "Staff":     person = new Staff(id, name, email, phone, addr, e1);         break;
                default:          person = new Member(id, name, email, phone, addr, e1);
            }

            memberService.registerMember(person);
            refreshTable();
            MainApp.showInfo(this, "Registered", name + " registered successfully.\n"
                + "Their login password is: " + LoginPanel.generatePassword(id));
            dialog.dispose();
        });

        btnPanel.add(cancelBtn); btnPanel.add(okBtn);
        dialog.getContentPane().add(form,     BorderLayout.CENTER);
        dialog.getContentPane().add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── Shake animation for invalid field ─────────────────────
    private void shake(JComponent field) {
        final Point orig  = field.getLocation();
        final int[] step  = {0};
        Timer timer = new Timer(28, null);
        timer.addActionListener(e -> {
            int offset = (step[0] % 2 == 0) ? 7 : -7;
            if (step[0] >= 10) { field.setLocation(orig); timer.stop(); }
            else                field.setLocation(orig.x + offset, orig.y);
            step[0]++;
        });
        timer.start();
    }

    // ── Helper rows ───────────────────────────────────────────
    private void addRow(JPanel panel, String label, String value) {
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(GuiUtils.FONT_BOLD);
        lbl.setForeground(GuiUtils.TEXT_SEC);
        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(GuiUtils.FONT_BODY);
        val.setForeground(GuiUtils.TEXT_PRI);
        panel.add(lbl); panel.add(val);
    }

    private String buildTooltip(Person p) {
        return "<html><b>" + p.getName() + "</b>  [" + p.getClass().getSimpleName() + "]<br>"
            + "Email: " + p.getEmail() + "<br>"
            + "Phone: " + p.getPhoneNumber() + "<br>"
            + "Address: " + p.getAddress() + "</html>";
    }
}