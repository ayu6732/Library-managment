package com.library.gui;

import com.library.models.items.LibraryItem;
import com.library.models.persons.Person;
import com.library.models.transactions.Reservation;
import com.library.services.*;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ReservationsPanel extends JPanel {

    private final ReservationService reservationService;
    private final MemberService      memberService;
    private final LibraryService     libraryService;
    private final DefaultTableModel  model;
    private final JTable             table;
    private final JPanel             pills;
    private final String             lockedMemberId; // null = admin, set = user mode

    public ReservationsPanel(ReservationService rs, MemberService ms, LibraryService ls) {
        this(rs, ms, ls, null);
    }

    public ReservationsPanel(ReservationService rs, MemberService ms,
                             LibraryService ls, String lockedMemberId) {
        this.reservationService = rs;
        this.memberService      = ms;
        this.libraryService     = ls;
        this.lockedMemberId     = lockedMemberId;

        setLayout(new BorderLayout(0, 16));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("\uD83D\uDCC5  Reservations"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("Manage item reservations and waiting queues"), BorderLayout.SOUTH);

        pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pills.setBackground(GuiUtils.BG_DARK);
        rebuildPills();

        JButton addBtn      = GuiUtils.iconButton("\uD83D\uDCC5", "Reserve",  GuiUtils.BTN_PRIMARY, java.awt.Color.WHITE, true);
        JButton completeBtn = GuiUtils.iconButton("\u2714",       "Complete", GuiUtils.ACCENT_GRN,  java.awt.Color.WHITE, false);
        JButton cancelBtn   = GuiUtils.iconButton("\u274C",       "Cancel",   GuiUtils.ACCENT_RED,  java.awt.Color.WHITE, false);
        JButton refreshBtn  = GuiUtils.iconButton("\uD83D\uDD04", "Refresh",  GuiUtils.BG_CARD,     GuiUtils.TEXT_SEC,    false);

        addBtn.addActionListener(e      -> showReserveDialog());
        completeBtn.addActionListener(e -> processSelected("COMPLETE"));
        cancelBtn.addActionListener(e   -> processSelected("CANCEL"));
        refreshBtn.addActionListener(e  -> refreshTable());

        JPanel toolbar = GuiUtils.toolbar(addBtn, completeBtn, cancelBtn, refreshBtn);

        String[] cols = {"ID", "Member", "Item", "Expires", "Status"};
        model = new DefaultTableModel(cols, 0);
        table = GuiUtils.styledTable(model);

        table.getColumnModel().getColumn(4).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JLabel lbl = new JLabel(value != null ? value.toString() : "");
                lbl.setFont(GuiUtils.FONT_BOLD);
                lbl.setOpaque(true);
                lbl.setBackground(isSelected ? new Color(31, 111, 235, 80) : GuiUtils.BG_CARD);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                String s = value != null ? value.toString() : "";
                lbl.setForeground("ACTIVE".equals(s)    ? GuiUtils.ACCENT_GRN :
                                  "COMPLETED".equals(s) ? GuiUtils.TEXT_SEC   :
                                                          GuiUtils.ACCENT_RED);
                return lbl;
            });

        JScrollPane scroll = GuiUtils.scrollPane(table);
        JPanel tableCard   = GuiUtils.sectionCard("", scroll);

        JPanel topArea = new JPanel(new BorderLayout(0, 8));
        topArea.setBackground(GuiUtils.BG_DARK);
        topArea.add(header,  BorderLayout.NORTH);
        topArea.add(pills,   BorderLayout.CENTER);
        topArea.add(toolbar, BorderLayout.SOUTH);

        add(topArea,   BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
        refreshTable();

        // Auto-refresh when this panel becomes visible (navigating to it via sidebar)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshTable();
            }
        });
    }

    private void rebuildPills() {
        pills.removeAll();
        pills.add(GuiUtils.badge("Active: "  + reservationService.getActiveReservations().size(),  GuiUtils.ACCENT_GRN));
        pills.add(GuiUtils.badge("Expired: " + reservationService.getExpiredReservations().size(), GuiUtils.ACCENT_RED));
        pills.revalidate();
        pills.repaint();
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (Reservation r : reservationService.getActiveReservations()) {
            if (lockedMemberId != null &&
                !r.getPerson().getId().equalsIgnoreCase(lockedMemberId)) continue;
            model.addRow(new Object[]{r.getTransactionId(), r.getPerson().getName(),
                r.getItem().getTitle(), r.getExpirationDate().toString(), r.getStatus()});
        }
        for (Reservation r : reservationService.getExpiredReservations()) {
            if (lockedMemberId != null &&
                !r.getPerson().getId().equalsIgnoreCase(lockedMemberId)) continue;
            model.addRow(new Object[]{r.getTransactionId(), r.getPerson().getName(),
                r.getItem().getTitle(), r.getExpirationDate().toString(), "EXPIRED"});
        }
        rebuildPills();
    }

    private void processSelected(String action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            MainApp.showError(this, "No Selection", "Please select a reservation first."); return;
        }
        String id = (String) model.getValueAt(row, 0);
        if ("COMPLETE".equals(action)) reservationService.completeReservation(id);
        if ("CANCEL".equals(action))   reservationService.cancelReservation(id);
        refreshTable();
    }

    private void showReserveDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), "Reserve Item", true);
        dialog.setSize(400, lockedMemberId != null ? 170 : 210);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(GuiUtils.BG_PANEL);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(GuiUtils.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));

        JTextField fM = GuiUtils.styledField("Member ID  (s001 or S001 both work)");
        JTextField fI = GuiUtils.styledField("Item ID  (b001 or B001 both work)");

        if (lockedMemberId != null) {
            fM.setText(lockedMemberId);
            fM.setEnabled(false);
            fM.setForeground(GuiUtils.TEXT_SEC);
            form.add(GuiUtils.formRow("Item ID", fI));
        } else {
            form.add(GuiUtils.formRow("Member ID", fM));
            form.add(Box.createVerticalStrut(10));
            form.add(GuiUtils.formRow("Item ID",   fI));
        }

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(GuiUtils.BG_PANEL);
        btns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL));
        JButton cancel = GuiUtils.ghostButton("Cancel");
        JButton ok     = GuiUtils.primaryButton("Reserve");
        cancel.addActionListener(e -> dialog.dispose());

        ok.addActionListener(e -> {
            String rawM = lockedMemberId != null ? lockedMemberId : fM.getText().trim();
            String rawI = fI.getText().trim();

            if (rawM.isEmpty()) {
                MainApp.showError(dialog, "Invalid Member ID", "Member ID cannot be empty."); return;
            }
            if (rawI.isEmpty()) {
                MainApp.showError(dialog, "Invalid Item ID", "Item ID cannot be empty."); return;
            }

            // FIX: case-insensitive member lookup
            Person person = memberService.getAllMembers().stream()
                .filter(m -> m.getId().equalsIgnoreCase(rawM))
                .findFirst().orElse(null);
            if (person == null) {
                MainApp.showError(dialog, "Member Not Found",
                    "No member found with ID: \"" + rawM + "\".\n"
                    + "Tip: IDs are case-insensitive — s001 and S001 both work."); return;
            }

            // FIX: case-insensitive item lookup
            LibraryItem item = libraryService.getAllItems().stream()
                .filter(i -> i.getItemId().equalsIgnoreCase(rawI))
                .findFirst().orElse(null);
            if (item == null) {
                MainApp.showError(dialog, "Item Not Found",
                    "No item found with ID: \"" + rawI + "\".\n"
                    + "Tip: IDs are case-insensitive — b001 and B001 both work."); return;
            }

            Reservation r = reservationService.reserveItem(person, item);
            if (r != null) {
                MainApp.showInfo(this, "Reserved",
                    "Reservation " + r.getTransactionId() + " created.\n"
                    + "Member: " + person.getName() + "\n"
                    + "Item: " + item.getTitle());
                refreshTable();
                dialog.dispose();
            } else {
                MainApp.showError(dialog, "Reservation Failed",
                    "Could not create reservation.\n"
                    + "The item may be available (reserve only borrowed items)\n"
                    + "or you already have a reservation for it.");
            }
        });

        btns.add(cancel); btns.add(ok);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}