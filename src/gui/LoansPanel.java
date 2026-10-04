package com.library.gui;

import com.library.models.items.LibraryItem;
import com.library.models.persons.*;
import com.library.models.transactions.Loan;
import com.library.services.*;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LoansPanel extends JPanel {

    private final LoanService    loanService;
    private final MemberService  memberService;
    private final LibraryService libraryService;
    private final DefaultTableModel model;
    private final JTable table;
    private final JPanel pills;
    private boolean showAll = true;

    // For user-mode: pre-fill member ID and lock it
    private final String lockedMemberId; // null = admin mode

    public LoansPanel(LoanService loanService, MemberService memberService,
                      LibraryService libraryService) {
        this(loanService, memberService, libraryService, null);
    }

    public LoansPanel(LoanService loanService, MemberService memberService,
                      LibraryService libraryService, String lockedMemberId) {
        this.loanService      = loanService;
        this.memberService    = memberService;
        this.libraryService   = libraryService;
        this.lockedMemberId   = lockedMemberId;

        setLayout(new BorderLayout(0, 16));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("\uD83D\uDCE4  Loans"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("Borrow and return items, track overdue loans"), BorderLayout.SOUTH);

        pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pills.setBackground(GuiUtils.BG_DARK);
        rebuildPills();

        JButton borrowBtn  = GuiUtils.iconButton("\uD83D\uDCE4", "Borrow",     GuiUtils.BTN_PRIMARY, Color.WHITE, true);
        JButton returnBtn  = GuiUtils.iconButton("\uD83D\uDCE5", "Return",     GuiUtils.ACCENT_GRN,  Color.WHITE, false);
        JButton renewBtn   = GuiUtils.iconButton("\uD83D\uDD04", "Renew",      GuiUtils.BG_CARD,     GuiUtils.TEXT_SEC, false);
        JButton refreshBtn = GuiUtils.iconButton("\uD83D\uDD04", "Refresh",    GuiUtils.BG_CARD,     GuiUtils.TEXT_SEC, false);

        borrowBtn.addActionListener(e  -> showBorrowDialog());
        returnBtn.addActionListener(e  -> showReturnDialog());
        renewBtn.addActionListener(e   -> showRenewDialog());
        refreshBtn.addActionListener(e -> refreshTable());

        JPanel toolbar = GuiUtils.toolbar(borrowBtn, returnBtn, renewBtn, refreshBtn);

        String[] cols = {"Loan ID", "Member", "Item", "Due Date", "Return Date", "Status"};
        model = new DefaultTableModel(cols, 0);
        table = GuiUtils.styledTable(model);

        table.getColumnModel().getColumn(5).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JLabel lbl = new JLabel(value != null ? value.toString() : "");
                lbl.setFont(GuiUtils.FONT_BOLD);
                lbl.setOpaque(true);
                lbl.setBackground(isSelected ? new Color(31, 111, 235, 80) : GuiUtils.BG_CARD);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                String s = value != null ? value.toString() : "";
                lbl.setForeground(s.contains("Overdue")   ? GuiUtils.ACCENT_RED  :
                                  s.contains("Completed") ? GuiUtils.TEXT_SEC    :
                                                            GuiUtils.ACCENT_GRN);
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
        pills.add(GuiUtils.badge("Active: "  + loanService.getAllActiveLoans().size(), GuiUtils.ACCENT_GRN));
        pills.add(GuiUtils.badge("Overdue: " + loanService.getOverdueLoans().size(),  GuiUtils.ACCENT_RED));
        pills.revalidate();
        pills.repaint();
    }

    private void refreshTable() {
        model.setRowCount(0);
        List<Loan> loans = loanService.getAllActiveLoans();
        for (Loan loan : loans) {
            // In user mode, only show this user's loans
            if (lockedMemberId != null &&
                !loan.getPerson().getId().equalsIgnoreCase(lockedMemberId)) continue;
            model.addRow(row(loan));
        }
        for (Loan loan : loanService.getOverdueLoans()) {
            if (lockedMemberId != null &&
                !loan.getPerson().getId().equalsIgnoreCase(lockedMemberId)) continue;
            boolean already = false;
            for (int i = 0; i < model.getRowCount(); i++)
                if (model.getValueAt(i, 0).equals(loan.getTransactionId())) { already = true; break; }
            if (!already) model.addRow(row(loan));
        }
        rebuildPills();
    }

    private Object[] row(Loan loan) {
        return new Object[]{
            loan.getTransactionId(),
            loan.getPerson().getName(),
            loan.getItem().getTitle(),
            loan.getDueDate().toString(),
            loan.getReturnDate() != null ? loan.getReturnDate().toString() : "—",
            loan.isOverdue() ? "! Overdue"
                : "COMPLETED".equals(loan.getStatus()) ? "+ Completed" : "+ Active"
        };
    }

    // ── BORROW DIALOG ─────────────────────────────────────────
    private void showBorrowDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), "Borrow Item", true);
        dialog.setSize(420, lockedMemberId != null ? 180 : 220);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(GuiUtils.BG_PANEL);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(GuiUtils.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));

        JTextField fMember = GuiUtils.styledField("Member ID");
        JTextField fItem   = GuiUtils.styledField("Item ID  e.g. B001");

        if (lockedMemberId != null) {
            // User mode: member ID is pre-filled and locked
            fMember.setText(lockedMemberId);
            fMember.setEnabled(false);
            fMember.setForeground(GuiUtils.TEXT_SEC);
        }

        if (lockedMemberId == null) {
            form.add(GuiUtils.formRow("Member ID", fMember));
            form.add(Box.createVerticalStrut(10));
        }
        form.add(GuiUtils.formRow("Item ID", fItem));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(GuiUtils.BG_PANEL);
        btns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL));
        JButton cancel = GuiUtils.ghostButton("Cancel");
        JButton ok     = GuiUtils.primaryButton("Borrow");
        cancel.addActionListener(e -> dialog.dispose());

        ok.addActionListener(e -> {
            String memberId = lockedMemberId != null
                ? lockedMemberId : fMember.getText().trim().toUpperCase();
            String itemId   = fItem.getText().trim().toUpperCase();

            if (memberId.isEmpty()) {
                MainApp.showError(dialog, "Invalid Member ID", "Member ID cannot be empty."); return;
            }
            if (itemId.isEmpty()) {
                MainApp.showError(dialog, "Invalid Item ID", "Item ID cannot be empty."); return;
            }

            Person person = memberService.findMemberById(memberId);
            if (person == null) {
                MainApp.showError(dialog, "Member Not Found",
                    "No member found with ID: \"" + memberId + "\"."); return;
            }

            LibraryItem item = libraryService.findItemById(itemId);
            if (item == null) {
                MainApp.showError(dialog, "Item Not Found",
                    "No item found with ID: \"" + itemId + "\"."); return;
            }

            if (!item.isAvailable()) {
                MainApp.showError(dialog, "Item Not Available",
                    "\"" + item.getTitle() + "\" is currently borrowed.\n"
                    + "You can make a reservation instead."); return;
            }

            int currentLoans = loanService.getActiveLoansForPerson(person.getId()).size();
            int maxLimit     = getPersonLoanLimit(person);
            if (currentLoans >= maxLimit) {
                MainApp.showError(dialog, "Loan Limit Exceeded",
                    person.getName() + " has reached the loan limit.\n"
                    + "Current: " + currentLoans + " / " + maxLimit); return;
            }

            Loan loan = loanService.borrowItem(person, item);
            if (loan != null) {
                MainApp.showInfo(this, "Borrowed Successfully",
                    "Loan ID: " + loan.getTransactionId() + "\n"
                    + "Member: " + person.getName() + "\n"
                    + "Item: " + item.getTitle() + "\n"
                    + "Due Date: " + loan.getDueDate());
                refreshTable();
                dialog.dispose();
            } else {
                MainApp.showError(dialog, "Failed", "Could not create loan. Please try again.");
            }
        });

        btns.add(cancel); btns.add(ok);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── RETURN DIALOG ─────────────────────────────────────────
    private void showReturnDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), "Return Item", true);
        dialog.setSize(380, 170);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(GuiUtils.BG_PANEL);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(GuiUtils.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));

        JTextField fLoan = GuiUtils.styledField("Loan ID  e.g. L001");
        form.add(GuiUtils.formRow("Loan ID", fLoan));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(GuiUtils.BG_PANEL);
        btns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL));
        JButton cancel = GuiUtils.ghostButton("Cancel");
        JButton ok     = GuiUtils.primaryButton("Return");
        cancel.addActionListener(e -> dialog.dispose());

        ok.addActionListener(e -> {
            String loanId = fLoan.getText().trim();
            if (loanId.isEmpty()) {
                MainApp.showError(dialog, "Invalid Loan ID", "Loan ID cannot be empty."); return;
            }

            // Check if loan exists before trying to return
            List<Loan> all = loanService.getAllActiveLoans();
            Loan found = all.stream()
                .filter(l -> l.getTransactionId().equalsIgnoreCase(loanId))
                .findFirst().orElse(null);

            if (found == null) {
                MainApp.showError(dialog, "Loan Not Found",
                    "No active loan found with ID: \"" + loanId + "\".\n"
                    + "It may have already been returned, or the ID is incorrect."); return;
            }

            // In user mode, only allow returning your own loans
            if (lockedMemberId != null &&
                !found.getPerson().getId().equalsIgnoreCase(lockedMemberId)) {
                MainApp.showError(dialog, "Access Denied",
                    "You can only return your own loans."); return;
            }

            boolean ok2 = loanService.returnItem(loanId.trim());
            if (ok2) {
                MainApp.showInfo(this, "Returned Successfully",
                    "\"" + found.getItem().getTitle() + "\" has been returned.\n"
                    + "Thank you, " + found.getPerson().getName() + "!");
                refreshTable();
                dialog.dispose();
            } else {
                MainApp.showError(dialog, "Failed", "Could not process return. Please try again.");
            }
        });

        btns.add(cancel); btns.add(ok);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── RENEW DIALOG ──────────────────────────────────────────
    private void showRenewDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), "Renew Loan", true);
        dialog.setSize(380, 210);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(GuiUtils.BG_PANEL);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(GuiUtils.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));

        JTextField fLoan = GuiUtils.styledField("Loan ID  e.g. L001");
        JTextField fDays = GuiUtils.styledField("Additional days  e.g. 7");
        form.add(GuiUtils.formRow("Loan ID", fLoan));
        form.add(Box.createVerticalStrut(10));
        form.add(GuiUtils.formRow("Extra Days", fDays));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(GuiUtils.BG_PANEL);
        btns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL));
        JButton cancel = GuiUtils.ghostButton("Cancel");
        JButton ok     = GuiUtils.primaryButton("Renew");
        cancel.addActionListener(e -> dialog.dispose());

        ok.addActionListener(e -> {
            String loanId = fLoan.getText().trim();
            String daysStr = fDays.getText().trim();

            if (loanId.isEmpty()) {
                MainApp.showError(dialog, "Invalid Loan ID", "Loan ID cannot be empty."); return;
            }
            if (!daysStr.matches("\\d+") || Integer.parseInt(daysStr) < 1) {
                MainApp.showError(dialog, "Invalid Days",
                    "Enter a positive number of days (e.g. 7)."); return;
            }
            try {
                boolean res = loanService.renewLoan(loanId, Integer.parseInt(daysStr));
                if (res) {
                    MainApp.showInfo(this, "Renewed", "Loan renewed for " + daysStr + " more days.");
                    refreshTable();
                    dialog.dispose();
                } else {
                    MainApp.showError(dialog, "Failed",
                        "Loan ID \"" + loanId + "\" not found or cannot be renewed.");
                }
            } catch (NumberFormatException ex) {
                MainApp.showError(dialog, "Invalid Input", "Days must be a number.");
            }
        });

        btns.add(cancel); btns.add(ok);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private int getPersonLoanLimit(Person person) {
        if (person instanceof Professor) return ((Professor) person).getMaxLoanLimit();
        if (person instanceof Staff)     return ((Staff)     person).getMaxLoanLimit();
        if (person instanceof Student)   return ((Student)   person).getMaxLoanLimit();
        if (person instanceof Member)    return ((Member)    person).getMaxLoanLimit();
        return 5;
    }
}