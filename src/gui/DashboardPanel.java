package com.library.gui;

import com.library.models.items.LibraryItem;
import com.library.models.transactions.Loan;
import com.library.services.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final LibraryService     libraryService;
    private final MemberService      memberService;
    private final LoanService        loanService;
    private final ReservationService reservationService;

    // Live UI components that need refreshing
    private final JPanel            statsRow;
    private final DefaultTableModel loansModel;
    private final JPanel            summaryInner;
    private final BarChartPanel     barChart;
    private final DonutChartPanel   donutChart;

    public DashboardPanel(LibraryService libraryService, MemberService memberService,
                          LoanService loanService, ReservationService reservationService) {
        this.libraryService     = libraryService;
        this.memberService      = memberService;
        this.loanService        = loanService;
        this.reservationService = reservationService;

        setLayout(new BorderLayout(0, 20));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("\uD83C\uDFE0  Dashboard"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("Welcome \u2014 "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy"))),
                BorderLayout.SOUTH);

        // Stat cards row (populated by refresh())
        statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        statsRow.setBackground(GuiUtils.BG_DARK);

        // Active Loans table
        String[] cols = {"Loan ID", "Member", "Item", "Due Date", "Status"};
        loansModel = new DefaultTableModel(cols, 0);
        JTable table = GuiUtils.styledTable(loansModel);
        table.getColumnModel().getColumn(4).setCellRenderer((t, value, isSelected, hasFocus, row, col) -> {
            JLabel lbl = new JLabel(value != null ? value.toString() : "");
            lbl.setFont(GuiUtils.FONT_BOLD);
            lbl.setOpaque(true);
            lbl.setBackground(isSelected ? new Color(139, 90, 43, 50) : GuiUtils.BG_CARD);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
            if (value != null && value.toString().contains("Overdue"))
                lbl.setForeground(GuiUtils.ACCENT_RED);
            else
                lbl.setForeground(GuiUtils.ACCENT_GRN);
            return lbl;
        });

        JScrollPane tableScroll = GuiUtils.scrollPane(table);
        tableScroll.setPreferredSize(new Dimension(0, 200));
        JPanel loansCard = GuiUtils.sectionCard("Active Loans", tableScroll);

        // Catalog Summary grid
        summaryInner = new JPanel(new GridLayout(5, 2, 20, 8));
        summaryInner.setBackground(GuiUtils.BG_CARD);
        JPanel summaryCard = GuiUtils.sectionCard("Catalog Summary", summaryInner);
        summaryCard.setPreferredSize(new Dimension(220, 0));

        // Bar Chart
        Color[] barColors  = { GuiUtils.ACCENT, GuiUtils.ACCENT_GRN, GuiUtils.ACCENT_YEL, new Color(160, 100, 200) };
        String[] barLabels = { "Books", "Digital", "DVDs", "Magazines" };
        barChart = new BarChartPanel(new int[]{0, 0, 0, 0}, barLabels, barColors, "Items by Type");
        barChart.setPreferredSize(new Dimension(0, 200));
        JPanel barCard = GuiUtils.sectionCard("Items by Type", barChart);

        // Donut Chart
        String[] donutLabels = { "Available", "Borrowed", "Overdue" };
        Color[]  donutColors = { GuiUtils.ACCENT_GRN, GuiUtils.ACCENT_YEL, GuiUtils.ACCENT_RED };
        donutChart = new DonutChartPanel(new int[]{0, 0, 0}, donutLabels, donutColors);
        donutChart.setPreferredSize(new Dimension(220, 200));
        JPanel donutCard = GuiUtils.sectionCard("Availability Status", donutChart);
        donutCard.setPreferredSize(new Dimension(240, 0));

        // Layout
        JPanel chartsRow = new JPanel(new BorderLayout(16, 0));
        chartsRow.setBackground(GuiUtils.BG_DARK);
        chartsRow.add(barCard,   BorderLayout.CENTER);
        chartsRow.add(donutCard, BorderLayout.EAST);

        JPanel bottomRow = new JPanel(new BorderLayout(16, 0));
        bottomRow.setBackground(GuiUtils.BG_DARK);
        bottomRow.add(loansCard,   BorderLayout.CENTER);
        bottomRow.add(summaryCard, BorderLayout.EAST);

        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setBackground(GuiUtils.BG_DARK);
        centerContent.add(chartsRow);
        centerContent.add(Box.createVerticalStrut(16));
        centerContent.add(bottomRow);

        JPanel topSection = new JPanel(new BorderLayout(0, 16));
        topSection.setBackground(GuiUtils.BG_DARK);
        topSection.add(header,   BorderLayout.NORTH);
        topSection.add(statsRow, BorderLayout.SOUTH);

        add(topSection,    BorderLayout.NORTH);
        add(centerContent, BorderLayout.CENTER);

        // Auto-refresh every time this panel becomes visible (e.g. switching tabs)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refresh();
            }
        });

        refresh(); // initial load
    }

    /** Reloads all data from services and repaints the dashboard. */
    public void refresh() {
        List<LibraryItem> allItems = libraryService.getAllItems();
        List<LibraryItem> available = libraryService.getAvailableItems();
        List<LibraryItem> borrowed  = libraryService.getBorrowedItems();
        List<Loan> activeLoans      = loanService.getAllActiveLoans();
        List<Loan> overdue          = loanService.getOverdueLoans();

        int bookCount    = libraryService.getItemCountByType("Book");
        int digitalCount = libraryService.getItemCountByType("DigitalBook");
        int dvdCount     = libraryService.getItemCountByType("DVD");
        int magCount     = libraryService.getItemCountByType("Magazine");
        int reservCount  = reservationService.getActiveReservations().size();

        // Stat cards
        statsRow.removeAll();
        statsRow.add(GuiUtils.statCard("\uD83D\uDCD6", String.valueOf(allItems.size()),                       "Total Items", GuiUtils.ACCENT));
        statsRow.add(GuiUtils.statCard("\u2705",        String.valueOf(available.size()),                      "Available",   GuiUtils.ACCENT_GRN));
        statsRow.add(GuiUtils.statCard("\uD83D\uDCE4",  String.valueOf(borrowed.size()),                       "Borrowed",    GuiUtils.ACCENT_YEL));
        statsRow.add(GuiUtils.statCard("\uD83D\uDC65",  String.valueOf(memberService.getAllMembers().size()),   "Members",     GuiUtils.ACCENT));
        statsRow.add(GuiUtils.statCard("\u26A0",         String.valueOf(overdue.size()),                        "Overdue",     GuiUtils.ACCENT_RED));
        statsRow.revalidate();
        statsRow.repaint();

        // Loans table
        loansModel.setRowCount(0);
        for (Loan loan : activeLoans) {
            loansModel.addRow(new Object[]{
                loan.getTransactionId(),
                loan.getPerson().getName(),
                loan.getItem().getTitle(),
                loan.getDueDate().toString(),
                loan.isOverdue() ? "Overdue" : "Active"
            });
        }

        // Catalog summary
        summaryInner.removeAll();
        addSummaryRow(summaryInner, "Books",         String.valueOf(bookCount));
        addSummaryRow(summaryInner, "Digital Books", String.valueOf(digitalCount));
        addSummaryRow(summaryInner, "DVDs",          String.valueOf(dvdCount));
        addSummaryRow(summaryInner, "Magazines",     String.valueOf(magCount));
        addSummaryRow(summaryInner, "Reservations",  String.valueOf(reservCount));
        summaryInner.revalidate();
        summaryInner.repaint();

        // Charts
        barChart.setValues(new int[]{ bookCount, digitalCount, dvdCount, magCount });
        donutChart.setValues(new int[]{ available.size(), borrowed.size(), overdue.size() });

        revalidate();
        repaint();
    }

    private void addSummaryRow(JPanel panel, String label, String value) {
        JLabel l = GuiUtils.secondaryLabel(label);
        JLabel v = new JLabel(value);
        v.setFont(GuiUtils.FONT_BOLD);
        v.setForeground(GuiUtils.TEXT_PRI);
        panel.add(l);
        panel.add(v);
    }

    // ══════════════════════════════════════════════════════════
    // Inner class: Bar Chart (pure Swing / Graphics2D)
    // ══════════════════════════════════════════════════════════
    static class BarChartPanel extends JPanel {
        private int[]    values;
        private final String[] labels;
        private final Color[]  colors;
        private final String   title;

        BarChartPanel(int[] values, String[] labels, Color[] colors, String title) {
            this.values = values;
            this.labels = labels;
            this.colors = colors;
            this.title  = title;
            setBackground(GuiUtils.BG_CARD);
            setOpaque(true);
        }

        void setValues(int[] newValues) {
            this.values = newValues;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int padL = 20, padR = 20, padT = 14, padB = 36;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            int maxVal = 1;
            for (int v : values) if (v > maxVal) maxVal = v;

            int n       = values.length;
            int barW    = (chartW - (n - 1) * 10) / n;
            int spacing = 10;

            for (int i = 0; i < n; i++) {
                int barH = (int) ((double) values[i] / maxVal * chartH);
                int x    = padL + i * (barW + spacing);
                int y    = padT + chartH - barH;

                g2.setColor(colors[i % colors.length]);
                g2.fillRoundRect(x, y, barW, barH, 6, 6);
                if (barH > 6)
                    g2.fillRect(x, y + 6, barW, barH - 6);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(GuiUtils.TEXT_PRI);
                String valStr = String.valueOf(values[i]);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (barW - fm.stringWidth(valStr)) / 2;
                g2.drawString(valStr, tx, y - 4);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(GuiUtils.TEXT_SEC);
                FontMetrics fm2 = g2.getFontMetrics();
                int lx = x + (barW - fm2.stringWidth(labels[i])) / 2;
                g2.drawString(labels[i], lx, padT + chartH + 16);
            }

            g2.setColor(GuiUtils.BORDER_COL);
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);
            g2.dispose();
        }
    }

    // ══════════════════════════════════════════════════════════
    // Inner class: Donut Chart (pure Swing / Graphics2D)
    // ══════════════════════════════════════════════════════════
    static class DonutChartPanel extends JPanel {
        private int[]    values;
        private final String[] labels;
        private final Color[]  colors;

        DonutChartPanel(int[] values, String[] labels, Color[] colors) {
            this.values = values;
            this.labels = labels;
            this.colors = colors;
            setBackground(GuiUtils.BG_CARD);
            setOpaque(true);
        }

        void setValues(int[] newValues) {
            this.values = newValues;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int total = 0;
            for (int v : values) total += v;

            int legendW   = 90;
            int donutSize = Math.min(w - legendW - 20, h - 20);
            if (donutSize < 20) { g2.dispose(); return; }

            int cx    = 10 + donutSize / 2;
            int cy    = h / 2;
            int outer = donutSize / 2;
            int inner = (int) (outer * 0.55);

            if (total == 0) {
                g2.setColor(GuiUtils.BORDER_COL);
                g2.setStroke(new BasicStroke(outer - inner));
                g2.drawOval(cx - (outer + inner) / 2, cy - (outer + inner) / 2,
                        outer + inner, outer + inner);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(GuiUtils.TEXT_SEC);
                g2.drawString("No data", cx - 20, cy + 4);
                g2.dispose();
                return;
            }

            double startAngle = -90;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == 0) continue;
                double sweep = 360.0 * values[i] / total;
                g2.setColor(colors[i % colors.length]);
                g2.setStroke(new BasicStroke(outer - inner, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                int arcR = (outer + inner) / 2;
                Arc2D arc = new Arc2D.Double(
                        cx - arcR, cy - arcR,
                        arcR * 2, arcR * 2,
                        startAngle, -sweep, Arc2D.OPEN);
                g2.draw(arc);
                startAngle -= sweep;
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2.setColor(GuiUtils.TEXT_PRI);
            String totalStr = String.valueOf(total);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(totalStr, cx - fm.stringWidth(totalStr) / 2, cy + 6);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(GuiUtils.TEXT_SEC);
            String totalLbl = "total";
            g2.drawString(totalLbl, cx - g2.getFontMetrics().stringWidth(totalLbl) / 2, cy + 18);

            int lx = 10 + donutSize + 10;
            int ly = cy - (values.length * 22) / 2;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            for (int i = 0; i < values.length; i++) {
                g2.setColor(colors[i % colors.length]);
                g2.fillRoundRect(lx, ly + i * 22, 10, 10, 3, 3);
                g2.setColor(GuiUtils.TEXT_SEC);
                g2.drawString(labels[i], lx + 14, ly + i * 22 + 10);
                g2.setColor(GuiUtils.TEXT_PRI);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(String.valueOf(values[i]), lx + 14, ly + i * 22 + 20);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            }

            g2.dispose();
        }
    }
}
