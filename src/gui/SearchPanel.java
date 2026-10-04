package com.library.gui;

import com.library.models.items.*;
import com.library.services.LibraryService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class SearchPanel extends JPanel {

    private final LibraryService libraryService;
    private final DefaultTableModel model;
    private final JLabel resultLabel;
    private JTable table;

    public SearchPanel(LibraryService libraryService) {
        this.libraryService = libraryService;
        setLayout(new BorderLayout(0, 16));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("\uD83D\uDD0D  Search"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("Search the catalog by title, author, or ISBN"), BorderLayout.SOUTH);

        JTextField searchField = GuiUtils.styledField("Type to search...");
        searchField.setPreferredSize(new Dimension(380, 34));
        JComboBox<String> modeCb = GuiUtils.styledCombo("By Title", "By Author", "By ISBN");
        JButton searchBtn = GuiUtils.iconButton("\uD83D\uDD0D", "Search", GuiUtils.BTN_PRIMARY, Color.WHITE, false);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setBackground(GuiUtils.BG_DARK);
        searchBar.add(searchField);
        searchBar.add(modeCb);
        searchBar.add(searchBtn);

        resultLabel = GuiUtils.pageSubtitle("Enter a query and press Search.");
        resultLabel.setFont(new java.awt.Font("Segoe UI Emoji", java.awt.Font.PLAIN, 12));

        String[] cols = {"ID", "Type", "Title", "Year", "Status"};
        model = new DefaultTableModel(cols, 0);
        table = GuiUtils.styledTable(model);

        // ── Tooltip / hover: show full info ───────────────────
        table.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    String id = (String) model.getValueAt(row, 0);
                    LibraryItem item = libraryService.findItemById(id);
                    if (item != null)
                        table.setToolTipText(buildFullInfo(item));
                } else {
                    table.setToolTipText(null);
                }
            }
        });

        // ── Click row: show full info dialog ──────────────────
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row < 0) return;
                    String id = (String) model.getValueAt(row, 0);
                    LibraryItem item = libraryService.findItemById(id);
                    if (item != null) showFullInfoDialog(item);
                }
            }
        });

        JScrollPane scroll = GuiUtils.scrollPane(table);

        // ── Hint label under table ────────────────────────────
        JLabel hint = GuiUtils.secondaryLabel("  Hover over a row to preview  |  Double-click for full details");
        hint.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JPanel tableArea = new JPanel(new BorderLayout(0, 4));
        tableArea.setBackground(GuiUtils.BG_DARK);
        tableArea.add(scroll, BorderLayout.CENTER);
        tableArea.add(hint,   BorderLayout.SOUTH);

        JPanel tableCard = GuiUtils.sectionCard("Results", tableArea);

        // ── Search logic ──────────────────────────────────────
        Runnable doSearch = () -> {
            String q = searchField.getText().trim();

            // FIX: empty search shows clear error message
            if (q.isEmpty()) {
                resultLabel.setText("  Please enter a search term first.");
                resultLabel.setForeground(GuiUtils.ACCENT_RED);
                model.setRowCount(0);
                return;
            }
            resultLabel.setForeground(GuiUtils.TEXT_SEC);
            model.setRowCount(0);

            List<? extends LibraryItem> results;
            switch ((String) modeCb.getSelectedItem()) {
                case "By Author": results = libraryService.searchByAuthor(q); break;
                case "By ISBN":   results = libraryService.searchByISBN(q);   break;
                default:          results = libraryService.searchByTitle(q);
            }

            if (results.isEmpty()) {
                resultLabel.setText("\u2715  No results found for \"" + q + "\"");
                resultLabel.setForeground(GuiUtils.ACCENT_RED);
            } else {
                for (LibraryItem item : results) {
                    model.addRow(new Object[]{
                        item.getItemId(),
                        item.getClass().getSimpleName(),
                        item.getTitle(),
                        item.getYear(),
                        item.isAvailable() ? "+ Available" : "^ Borrowed"
                    });
                }
                resultLabel.setText("\u2714  Found " + results.size()
                    + " result(s) for \"" + q + "\"");
                resultLabel.setForeground(GuiUtils.ACCENT_GRN);
            }
        };

        searchBtn.addActionListener(e -> doSearch.run());
        searchField.addActionListener(e -> doSearch.run());

        // ── Button hover animation ────────────────────────────
        searchBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { searchBtn.setBackground(GuiUtils.ACCENT.brighter()); }
            public void mouseExited(MouseEvent e)  { searchBtn.setBackground(GuiUtils.BTN_PRIMARY); }
        });

        JPanel topArea = new JPanel(new BorderLayout(0, 12));
        topArea.setBackground(GuiUtils.BG_DARK);
        topArea.add(header,      BorderLayout.NORTH);
        topArea.add(searchBar,   BorderLayout.CENTER);
        topArea.add(resultLabel, BorderLayout.SOUTH);

        add(topArea,   BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    // ── Full info dialog ──────────────────────────────────────
    private void showFullInfoDialog(LibraryItem item) {
        JPanel info = new JPanel(new GridLayout(0, 2, 16, 8));
        info.setBackground(GuiUtils.BG_PANEL);
        info.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        addInfoRow(info, "Item ID",    item.getItemId());
        addInfoRow(info, "Title",      item.getTitle());
        addInfoRow(info, "Type",       item.getClass().getSimpleName());
        addInfoRow(info, "Year",       String.valueOf(item.getYear()));
        addInfoRow(info, "Status",     item.isAvailable() ? "Available" : "Borrowed");

        if (item instanceof DigitalBook) {
            DigitalBook db = (DigitalBook) item;
            addInfoRow(info, "Author",        db.getauthor());
            addInfoRow(info, "Publisher",     db.getpublisher());
            addInfoRow(info, "ISBN",          db.getISBN());
            addInfoRow(info, "Pages",         String.valueOf(db.getNp()));
            addInfoRow(info, "File Size",     db.getFileSize() + " MB");
            addInfoRow(info, "Format",        db.getFormat());
            addInfoRow(info, "Download Link", db.getDownloadlink());
        } else if (item instanceof Book) {
            Book b = (Book) item;
            addInfoRow(info, "Author",    b.getauthor());
            addInfoRow(info, "Publisher", b.getpublisher());
            addInfoRow(info, "ISBN",      b.getISBN());
            addInfoRow(info, "Pages",     String.valueOf(b.getNp()));
        } else if (item instanceof DVD) {
            DVD d = (DVD) item;
            addInfoRow(info, "Director", d.getDirector());
            addInfoRow(info, "Duration", d.getDuration() + " min");
            addInfoRow(info, "Rating",   d.getRating());
        } else if (item instanceof Magazine) {
            Magazine m = (Magazine) item;
            addInfoRow(info, "Publisher",  m.getPublisher());
            addInfoRow(info, "Issue Date", m.getIsDate());
            addInfoRow(info, "Volume No.", String.valueOf(m.getVn()));
        }

        JOptionPane.showMessageDialog(this, info,
            "Full Info — " + item.getTitle(), JOptionPane.PLAIN_MESSAGE);
    }

    private void addInfoRow(JPanel panel, String label, String value) {
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(GuiUtils.FONT_BOLD);
        lbl.setForeground(GuiUtils.TEXT_SEC);

        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(GuiUtils.FONT_BODY);
        val.setForeground(GuiUtils.TEXT_PRI);

        panel.add(lbl);
        panel.add(val);
    }

    private String buildFullInfo(LibraryItem item) {
        StringBuilder sb = new StringBuilder("<html><b>" + item.getTitle() + "</b><br>");
        sb.append("ID: ").append(item.getItemId()).append("  |  ");
        sb.append("Year: ").append(item.getYear()).append("  |  ");
        sb.append("Status: ").append(item.isAvailable() ? "Available" : "Borrowed");
        if (item instanceof Book) {
            Book b = (Book) item;
            sb.append("<br>Author: ").append(b.getauthor());
            sb.append("  |  ISBN: ").append(b.getISBN());
        } else if (item instanceof DVD) {
            DVD d = (DVD) item;
            sb.append("<br>Director: ").append(d.getDirector());
            sb.append("  |  ").append(d.getDuration()).append(" min  |  ").append(d.getRating());
        } else if (item instanceof Magazine) {
            Magazine m = (Magazine) item;
            sb.append("<br>Publisher: ").append(m.getPublisher());
            sb.append("  |  Issue: ").append(m.getIsDate());
        }
        sb.append("</html>");
        return sb.toString();
    }
}