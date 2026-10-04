package com.library.gui;

import com.library.db.*;
import com.library.models.items.LibraryItem;
import com.library.models.persons.Person;
import com.library.services.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DatabasePanel extends JPanel {

    private final LibraryService     libraryService;
    private final MemberService      memberService;
    private final LoanService        loanService;
    private final ReservationService reservationService;
    private final JTextArea          output;

    public DatabasePanel(LibraryService ls, MemberService ms,
                         LoanService los, ReservationService rs) {
        this.libraryService     = ls;
        this.memberService      = ms;
        this.loanService        = los;
        this.reservationService = rs;

        setLayout(new BorderLayout(0, 16));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("\uD83D\uDDC4  Database"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("JDBC + SQLite database browser"), BorderLayout.SOUTH);

        // Row 1 buttons
        JButton testBtn    = GuiUtils.primaryButton("\uD83D\uDD0C Test Connection");
        JButton syncBtn    = GuiUtils.successButton("\u2B06 Sync All to DB");
        JButton txnBtn     = GuiUtils.ghostButton("\u26A1 Transaction Demo");
        JPanel row1 = GuiUtils.toolbar(testBtn, syncBtn, txnBtn);

        // Row 2 buttons
        JButton loansBtn   = GuiUtils.ghostButton("\uD83D\uDCCB Loans from DB");
        JButton membersBtn = GuiUtils.ghostButton("\uD83D\uDC65 Members from DB");
        JButton itemsBtn   = GuiUtils.ghostButton("\uD83D\uDCD6 Items from DB");
        JPanel row2 = GuiUtils.toolbar(loansBtn, membersBtn, itemsBtn);

        output = GuiUtils.monoArea();
        output.setForeground(GuiUtils.ACCENT_YEL);
        output.setText("Database: library.db (SQLite — auto-created on startup)\nPress Test Connection to verify.\n");

        JScrollPane outputScroll = GuiUtils.scrollMono(output);
        JPanel outputCard = GuiUtils.sectionCard("Database Output", outputScroll);

        testBtn.addActionListener(e ->
            log(DatabaseManager.testConnection() ? "\u2713 Connected to library.db\n" : "\u2717 Connection failed\n"));

        syncBtn.addActionListener(e -> {
            ItemDAO.syncAll(libraryService.getAllItems());
            MemberDAO.syncAll(memberService.getAllMembers());
            LoanDAO.syncAll(loanService.getAllActiveLoans());
            ReservationDAO.syncAll(reservationService.getActiveReservations());
            log("\u2713 All data synced to database.\n");
        });

        txnBtn.addActionListener(e -> log(
            "=== ATOMIC TRANSACTION DEMO ===\n\n" +
            "  DatabaseManager.beginTransaction();\n" +
            "  INSERT INTO loans (loan_id, ...) VALUES (...);\n" +
            "  UPDATE library_items SET available = 0 WHERE item_id = ?;\n" +
            "  DatabaseManager.commit();\n" +
            "  // on failure -> DatabaseManager.rollback();\n\n" +
            "Both INSERT and UPDATE succeed or both fail.\n"
        ));

        loansBtn.addActionListener(e -> {
            List<String[]> rows = LoanDAO.getAllLoansRaw();
            StringBuilder sb = new StringBuilder("=== LOANS IN DATABASE ===\n");
            sb.append(String.format("%-8s %-8s %-8s %-12s %-12s %-10s%n", "LoanID","MemberID","ItemID","DueDate","ReturnDate","Status"));
            sb.append("-".repeat(62)).append("\n");
            for (String[] r : rows)
                sb.append(String.format("%-8s %-8s %-8s %-12s %-12s %-10s%n", r[0],r[1],r[2],r[3],r[4]!=null?r[4]:"—",r[5]));
            if (rows.isEmpty()) sb.append("(No data — press Sync first)\n");
            log(sb.toString());
        });

        membersBtn.addActionListener(e -> {
            List<Person> list = MemberDAO.getAllMembers();
            StringBuilder sb = new StringBuilder("=== MEMBERS IN DATABASE ===\n");
            for (Person p : list)
                sb.append("  [").append(p.getClass().getSimpleName()).append("] ")
                  .append(p.getId()).append(" — ").append(p.getName()).append("\n");
            if (list.isEmpty()) sb.append("(No data — press Sync first)\n");
            log(sb.toString());
        });

        itemsBtn.addActionListener(e -> {
            List<LibraryItem> list = ItemDAO.getAllItems();
            StringBuilder sb = new StringBuilder("=== ITEMS IN DATABASE ===\n");
            for (LibraryItem i : list)
                sb.append("  [").append(i.getClass().getSimpleName()).append("] ")
                  .append(i.getItemId()).append(" — ").append(i.getTitle())
                  .append(i.isAvailable() ? " [Available]" : " [Borrowed]").append("\n");
            if (list.isEmpty()) sb.append("(No data — press Sync first)\n");
            log(sb.toString());
        });

        JPanel toolbarArea = new JPanel(new BorderLayout());
        toolbarArea.setBackground(GuiUtils.BG_DARK);
        toolbarArea.add(row1, BorderLayout.NORTH);
        toolbarArea.add(row2, BorderLayout.SOUTH);

        JPanel topArea = new JPanel(new BorderLayout(0, 12));
        topArea.setBackground(GuiUtils.BG_DARK);
        topArea.add(header,      BorderLayout.NORTH);
        topArea.add(toolbarArea, BorderLayout.SOUTH);

        add(topArea,    BorderLayout.NORTH);
        add(outputCard, BorderLayout.CENTER);
    }

    private void log(String text) { output.append(text + "\n"); }
}