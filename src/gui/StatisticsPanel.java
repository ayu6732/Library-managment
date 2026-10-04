package com.library.gui;

import com.library.models.items.LibraryItem;
import com.library.services.*;
import com.library.utils.BorrowGraph;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class StatisticsPanel extends JPanel {

    public StatisticsPanel(LibraryService ls, MemberService ms,
                           LoanService los, ReservationService rs,
                           BorrowGraph borrowGraph) {
        setLayout(new BorderLayout(0, 20));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("\uD83D\uDCCA  Statistics"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("System-wide analytics and borrow graph"), BorderLayout.SOUTH);

        // ── Cards row ─────────────────────────────────────────────
        JPanel catalogGrid = buildGrid(new String[][]{
            {"Total Items",    String.valueOf(ls.getAllItems().size())},
            {"Books",          String.valueOf(ls.getItemCountByType("Book"))},
            {"Digital Books",  String.valueOf(ls.getItemCountByType("DigitalBook"))},
            {"DVDs",           String.valueOf(ls.getItemCountByType("DVD"))},
            {"Magazines",      String.valueOf(ls.getItemCountByType("Magazine"))},
            {"Available",      String.valueOf(ls.getAvailableItems().size())},
            {"Borrowed",       String.valueOf(ls.getBorrowedItems().size())},
            {"Unique Authors",  String.valueOf(ls.getUniqueGenres().size())}
        });
        JPanel catalogCard = GuiUtils.sectionCard("Catalog", catalogGrid);
        catalogCard.setPreferredSize(new Dimension(280, 0));

        Map<String, Integer> typeCounts = ms.getMemberTypeCount();
        int r = 0;
        String[][] memData = new String[typeCounts.size() + 1][2];
        memData[r][0] = "Total Members"; memData[r][1] = String.valueOf(ms.getAllMembers().size()); r++;
        for (Map.Entry<String, Integer> e : typeCounts.entrySet()) {
            memData[r][0] = e.getKey();
            memData[r][1] = String.valueOf(e.getValue()); r++;
        }
        JPanel memCard = GuiUtils.sectionCard("Members", buildGrid(memData));
        memCard.setPreferredSize(new Dimension(260, 0));

        Map<String, Integer> lc = los.getLoanCountPerMember();
        String topMember = "\u2014";
        if (!lc.isEmpty()) {
            String topId = Collections.max(lc.entrySet(), Map.Entry.comparingByValue()).getKey();
            topMember = topId + " (" + lc.get(topId) + ")";
        }
        JPanel loanCard = GuiUtils.sectionCard("Loans", buildGrid(new String[][]{
            {"Active Loans",  String.valueOf(los.getAllActiveLoans().size())},
            {"Overdue Loans", String.valueOf(los.getOverdueLoans().size())},
            {"Total Fees",    String.format("%.2f DA", los.calculateTotalLateFees())},
            {"Most Active",   topMember}
        }));
        loanCard.setPreferredSize(new Dimension(280, 0));

        JPanel resCard = GuiUtils.sectionCard("Reservations", buildGrid(new String[][]{
            {"Active",  String.valueOf(rs.getActiveReservations().size())},
            {"Expired", String.valueOf(rs.getExpiredReservations().size())}
        }));
        resCard.setPreferredSize(new Dimension(220, 0));

        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        cardsRow.setBackground(GuiUtils.BG_DARK);
        cardsRow.add(catalogCard);
        cardsRow.add(memCard);
        cardsRow.add(loanCard);
        cardsRow.add(resCard);

        // ── Top borrowed items ───────────────────────────────────
        JPanel topList = new JPanel();
        topList.setLayout(new BoxLayout(topList, BoxLayout.Y_AXIS));
        topList.setBackground(GuiUtils.BG_CARD);

        List<LibraryItem> topItems = ls.getMostBorrowedItems(5);
        if (topItems.isEmpty()) {
            topList.add(GuiUtils.secondaryLabel("No borrow data yet."));
        } else {
            int rank = 1;
            for (LibraryItem item : topItems) {
                JLabel l = GuiUtils.bodyLabel(rank++ + ".  " + item.getTitle()
                    + "   [" + item.getClass().getSimpleName() + "]");
                l.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                topList.add(l);
            }
        }
        JPanel topCard = GuiUtils.sectionCard("Most Borrowed Items", topList);

        // ── Overview tab content ─────────────────────────────────
        JPanel overviewContent = new JPanel(new BorderLayout(0, 16));
        overviewContent.setBackground(GuiUtils.BG_DARK);
        overviewContent.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        overviewContent.add(cardsRow, BorderLayout.NORTH);
        overviewContent.add(topCard,  BorderLayout.CENTER);

        // FIX: plain text tab labels — no emoji prefix that renders as broken squares
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(GuiUtils.FONT_BOLD);
        tabs.setBackground(GuiUtils.BG_DARK);
        tabs.setForeground(GuiUtils.TEXT_PRI);

        tabs.addTab("Overview",      overviewContent);
        tabs.addTab("Borrow Graph",  new GraphPanel(borrowGraph, los, ms, ls));

        JPanel topArea = new JPanel(new BorderLayout(0, 12));
        topArea.setBackground(GuiUtils.BG_DARK);
        topArea.add(header, BorderLayout.NORTH);

        add(topArea, BorderLayout.NORTH);
        add(tabs,    BorderLayout.CENTER);
    }

    private JPanel buildGrid(String[][] rows) {
        JPanel grid = new JPanel(new GridLayout(rows.length, 2, 20, 8));
        grid.setBackground(GuiUtils.BG_CARD);
        for (String[] row : rows) {
            grid.add(GuiUtils.secondaryLabel(row[0]));
            JLabel v = new JLabel(row[1]);
            v.setFont(GuiUtils.FONT_BOLD);
            v.setForeground(GuiUtils.TEXT_PRI);
            grid.add(v);
        }
        return grid;
    }
}