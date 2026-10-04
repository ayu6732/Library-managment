package com.library.gui;

import com.library.exceptions.InvalidISBNException;
import com.library.exceptions.ItemNotFoundException;
import com.library.models.items.*;
import com.library.services.LibraryService;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.function.BiConsumer;

public class ItemsPanel extends JPanel {

    private final LibraryService libraryService;
    private final DefaultTableModel model;
    private final JTable table;
    private JLabel detailLabel;
    private final boolean isAdmin;

    public ItemsPanel(LibraryService libraryService) {
        this(libraryService, true);
    }

    public ItemsPanel(LibraryService libraryService, boolean isAdmin) {
        this.libraryService = libraryService;
        this.isAdmin = isAdmin;
        setLayout(new BorderLayout(0, 16));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("\uD83D\uDCD6  Library Items"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("Browse books, DVDs, magazines and digital books"), BorderLayout.SOUTH);

        JButton addBtn     = GuiUtils.iconButton("\uD83D\uDCD6", "Add Item",  GuiUtils.BTN_PRIMARY, java.awt.Color.WHITE, true);
        JButton infoBtn    = GuiUtils.iconButton("\u2139",       "Full Info", GuiUtils.BG_CARD,     GuiUtils.TEXT_SEC,    false);
        JButton deleteBtn  = GuiUtils.iconButton("\uD83D\uDDD1", "Delete",    GuiUtils.ACCENT_RED,  java.awt.Color.WHITE, false);
        JButton refreshBtn = GuiUtils.iconButton("\uD83D\uDD04", "Refresh",   GuiUtils.BG_CARD,     GuiUtils.TEXT_SEC,    false);

        addBtn.addActionListener(e -> {
            if (!isAdmin) {
                showAccessDenied("add items");
                return;
            }
            showAddDialog();
        });
        infoBtn.addActionListener(e -> showFullInfoDialog());
        deleteBtn.addActionListener(e -> {
            if (!isAdmin) { showAccessDenied("delete items"); return; }
            deleteSelected();
        });
        refreshBtn.addActionListener(e -> refreshTable());

        // Tooltip hints for non-admin
        if (!isAdmin) {
            deleteBtn.setToolTipText("Admin only");
            addBtn.setToolTipText("Admin only");
        }

        JPanel toolbar = GuiUtils.toolbar(addBtn, infoBtn, deleteBtn, refreshBtn);

        String[] cols = {"ID", "Type", "Title", "Year", "Status"};
        model = new DefaultTableModel(cols, 0);
        table = GuiUtils.styledTable(model);

        table.getColumnModel().getColumn(4).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JLabel lbl = new JLabel(value != null ? value.toString() : "");
                lbl.setFont(GuiUtils.FONT_BOLD);
                lbl.setOpaque(true);
                lbl.setBackground(isSelected ? new Color(31, 111, 235, 80) : GuiUtils.BG_CARD);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                lbl.setForeground(value != null && value.toString().contains("Borrowed")
                        ? GuiUtils.ACCENT_YEL : GuiUtils.ACCENT_GRN);
                return lbl;
            });

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String id = (String) model.getValueAt(row, 0);
                LibraryItem item = libraryService.findItemById(id);
                if (item != null) detailLabel.setText(buildDetail(item));
            }
        });

        JScrollPane scroll = GuiUtils.scrollPane(table);
        detailLabel = GuiUtils.secondaryLabel(" ");
        detailLabel.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 0));

        JPanel tableArea = new JPanel(new BorderLayout(0, 4));
        tableArea.setBackground(GuiUtils.BG_DARK);
        tableArea.add(scroll,      BorderLayout.CENTER);
        tableArea.add(detailLabel, BorderLayout.SOUTH);

        JPanel tableCard = GuiUtils.sectionCard("", tableArea);

        JPanel topArea = new JPanel(new BorderLayout(0, 12));
        topArea.setBackground(GuiUtils.BG_DARK);
        topArea.add(header,  BorderLayout.NORTH);
        topArea.add(toolbar, BorderLayout.SOUTH);

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

    // ── Normalize ISBN: user types with or without hyphens,
    //    always stored as  978-XXXXXXXXXX  to match file format ──
    private String normalizeISBN(String raw) {
        String clean = raw.replaceAll("[-\\s]", "");
        if (clean.length() == 13)
            return clean.substring(0, 3) + "-" + clean.substring(3);
        return clean; // ISBN-10: keep as-is
    }

    // ── Auto-suggest next ID: scans existing items for the given prefix ──
    private String nextId(String prefix) {
        int max = 0;
        for (LibraryItem item : libraryService.getAllItems()) {
            String id = item.getItemId().toUpperCase();
            if (id.startsWith(prefix.toUpperCase())) {
                try {
                    int num = Integer.parseInt(id.substring(prefix.length()));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return prefix.toUpperCase() + String.format("%03d", max + 1);
    }

    private void showAccessDenied(String action) {
        JOptionPane.showMessageDialog(this,
            "\uD83D\uDEAB  Access Denied\n\nYou do not have permission to " + action + ".\nPlease contact an Administrator.",
            "Access Denied", JOptionPane.WARNING_MESSAGE);
    }

    private void showFullInfoDialog() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select an item from the list first.",
                "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = (String) model.getValueAt(row, 0);
        LibraryItem item = libraryService.findItemById(id);
        if (item == null) return;

        JPanel panel = new JPanel(new GridLayout(0, 2, 14, 8));
        panel.setBackground(GuiUtils.BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        // Helper to add a row
        java.util.function.BiConsumer<String, String> addRow = (lbl, val) -> {
            JLabel l = new JLabel(lbl + ":");
            l.setFont(GuiUtils.FONT_BOLD);
            l.setForeground(GuiUtils.TEXT_SEC);
            JLabel v = new JLabel(val != null ? val : "\u2014");
            v.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            v.setForeground(GuiUtils.TEXT_PRI);
            panel.add(l); panel.add(v);
        };

        addRow.accept("Item ID",   item.getItemId());
        addRow.accept("Type",      item.getClass().getSimpleName());
        addRow.accept("Title",     item.getTitle());
        addRow.accept("Year",      String.valueOf(item.getYear()));

        boolean avail = item.isAvailable();
        JLabel statusLbl = new JLabel(avail ? "\u2705  Available" : "\u26A0  Borrowed");
        statusLbl.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        statusLbl.setForeground(avail ? GuiUtils.ACCENT_GRN : GuiUtils.ACCENT_YEL);
        JLabel statusKey = new JLabel("Status:");
        statusKey.setFont(GuiUtils.FONT_BOLD);
        statusKey.setForeground(GuiUtils.TEXT_SEC);
        panel.add(statusKey); panel.add(statusLbl);

        // Type-specific fields
        if (item instanceof DigitalBook) {
            DigitalBook db = (DigitalBook) item;
            addRow.accept("Author",       db.getauthor());
            addRow.accept("Publisher",    db.getpublisher());
            addRow.accept("ISBN",         db.getISBN());
            addRow.accept("Pages",        String.valueOf(db.getNp()));
            addRow.accept("File Size",    db.getFileSize() + " MB");
            addRow.accept("Format",       db.getFormat());
            addRow.accept("Download Link",db.getDownloadlink());
        } else if (item instanceof Book) {
            Book b = (Book) item;
            addRow.accept("Author",    b.getauthor());
            addRow.accept("Publisher", b.getpublisher());
            addRow.accept("ISBN",      b.getISBN());
            addRow.accept("Pages",     String.valueOf(b.getNp()));
        } else if (item instanceof DVD) {
            DVD d = (DVD) item;
            addRow.accept("Director", d.getDirector());
            addRow.accept("Duration", d.getDuration() + " min");
            addRow.accept("Rating",   d.getRating());
        } else if (item instanceof Magazine) {
            Magazine m = (Magazine) item;
            addRow.accept("Publisher",     m.getPublisher());
            addRow.accept("Issue Date",    m.getIsDate());
            addRow.accept("Volume Number", String.valueOf(m.getVn()));
        }

        JOptionPane.showMessageDialog(this, panel,
            "\uD83D\uDCD6  " + item.getTitle(), JOptionPane.PLAIN_MESSAGE);
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (LibraryItem item : libraryService.getAllItems()) {
            model.addRow(new Object[]{
                item.getItemId(),
                item.getClass().getSimpleName(),
                item.getTitle(),
                item.getYear(),
                item.isAvailable() ? "+ Available" : "^ Borrowed"
            });
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { MainApp.showError(this, "No Selection", "Please select an item first."); return; }
        String id    = (String) model.getValueAt(row, 0);
        String title = (String) model.getValueAt(row, 2);
        LibraryItem item = libraryService.findItemById(id);
        if (item != null && !item.isAvailable()) {
            MainApp.showError(this, "Cannot Delete", "Item is currently borrowed."); return;
        }
        if (MainApp.confirm(this, "Delete \"" + title + "\"?")) {
            try { libraryService.deleteItem(id); refreshTable(); }
            catch (ItemNotFoundException ex) { MainApp.showError(this, "Error", ex.getMessage()); }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ADD ITEM DIALOG
    //
    //  Book       (itemId, title, author, publisher, year, ISBN, np)
    //  DVD        (itemId, title, year, director, duration, rating)
    //  Magazine   (itemId, title, year, publisher, isDate, vn)
    //  DigitalBook(itemId, title, author, publisher, year, ISBN, np,
    //              fileSize, format, downloadlink)
    // ══════════════════════════════════════════════════════════════════════
    private void showAddDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), "Add New Item", true);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(GuiUtils.BG_PANEL);
        dialog.setLayout(new BorderLayout());

        // ── Type selector + shared fields ─────────────────────
        JComboBox<String> typeCb = GuiUtils.styledCombo("Book", "DVD", "Magazine", "DigitalBook");

        // itemId  — pre-filled, changes per type
        JTextField fId    = GuiUtils.styledField(nextId("B"));
        // title   — same for all
        JTextField fTitle = GuiUtils.styledField("e.g. Introduction to Java");
        // year    — same for all
        JTextField fYear  = GuiUtils.styledField("e.g. 2023");

        // ── BOOK fields ───────────────────────────────────────
        // Book(itemId, title, author, publisher, year, ISBN, np)
        JTextField fBookAuthor    = GuiUtils.styledField("author          e.g. James Gosling");
        JTextField fBookPublisher = GuiUtils.styledField("publisher       e.g. Tech Books");
        JTextField fBookISBN      = GuiUtils.styledField("ISBN            e.g. 9780134685991");
        JTextField fBookNp        = GuiUtils.styledField("np              e.g. 500");

        // ── DVD fields ────────────────────────────────────────
        // DVD(itemId, title, year, director, duration, rating)
        JTextField fDvdDirector  = GuiUtils.styledField("director        e.g. Christopher Nolan");
        JTextField fDvdDuration  = GuiUtils.styledField("duration        e.g. 148  (minutes)");
        JTextField fDvdRating    = GuiUtils.styledField("rating          e.g. PG-13");

        // ── MAGAZINE fields ───────────────────────────────────
        // Magazine(itemId, title, year, publisher, isDate, vn)
        JTextField fMagPublisher = GuiUtils.styledField("publisher       e.g. National Geographic Society");
        JTextField fMagIsDate    = GuiUtils.styledField("isDate          e.g. 2024-03-01");
        JTextField fMagVn        = GuiUtils.styledField("vn              e.g. 245");

        // ── DIGITALBOOK fields ────────────────────────────────
        // DigitalBook(itemId, title, author, publisher, year, ISBN, np,
        //             fileSize, format, downloadlink)
        JTextField fDbAuthor       = GuiUtils.styledField("author          e.g. Eric Matthes");
        JTextField fDbPublisher    = GuiUtils.styledField("publisher       e.g. No Starch Press");
        JTextField fDbISBN         = GuiUtils.styledField("ISBN            e.g. 9781593279288");
        JTextField fDbNp           = GuiUtils.styledField("np              e.g. 560");
        JTextField fDbFileSize     = GuiUtils.styledField("fileSize        e.g. 3.2  (MB)");
        JTextField fDbFormat       = GuiUtils.styledField("format          e.g. PDF");
        JTextField fDbDownloadlink = GuiUtils.styledField("downloadlink    e.g. http://download.com/book.pdf");

        // ── Per-type panels ───────────────────────────────────
        JPanel bookPanel = makeTypePanel(
            new String[]{"author", "publisher", "ISBN", "np  (pages)"},
            fBookAuthor, fBookPublisher, fBookISBN, fBookNp);

        JPanel dvdPanel = makeTypePanel(
            new String[]{"director", "duration  (min)", "rating"},
            fDvdDirector, fDvdDuration, fDvdRating);

        JPanel magPanel = makeTypePanel(
            new String[]{"publisher", "isDate  (YYYY-MM-DD)", "vn  (volume no.)"},
            fMagPublisher, fMagIsDate, fMagVn);

        JPanel dbPanel = makeTypePanel(
            new String[]{"author", "publisher", "ISBN", "np  (pages)",
                         "fileSize  (MB)", "format", "downloadlink"},
            fDbAuthor, fDbPublisher, fDbISBN, fDbNp,
            fDbFileSize, fDbFormat, fDbDownloadlink);

        // Book visible by default
        dvdPanel.setVisible(false);
        magPanel.setVisible(false);
        dbPanel.setVisible(false);

        // ── Type switch: swap panels + refresh suggested ID ───
        typeCb.addActionListener(e -> {
            String t = (String) typeCb.getSelectedItem();
            bookPanel.setVisible("Book".equals(t));
            dvdPanel.setVisible("DVD".equals(t));
            magPanel.setVisible("Magazine".equals(t));
            dbPanel.setVisible("DigitalBook".equals(t));
            String prefix = "Book".equals(t)        ? "B"
                          : "DVD".equals(t)         ? "D"
                          : "Magazine".equals(t)    ? "M"
                          :                           "DB";
            JPanel form = new JPanel();
            fId.setText(nextId(prefix));
            form.revalidate();
            dialog.pack();
            dialog.setLocationRelativeTo(this);
        });

        // ── Form layout ───────────────────────────────────────
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(GuiUtils.BG_PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));

        form.add(GuiUtils.formRow("Type",    typeCb)); form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("itemId",  fId));    form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("title",   fTitle)); form.add(Box.createVerticalStrut(8));
        form.add(GuiUtils.formRow("year",    fYear));  form.add(Box.createVerticalStrut(10));

        JSeparator sep = new JSeparator();
        sep.setForeground(GuiUtils.BORDER_COL);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        form.add(sep);
        form.add(Box.createVerticalStrut(10));

        form.add(bookPanel);
        form.add(dvdPanel);
        form.add(magPanel);
        form.add(dbPanel);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getViewport().setBackground(GuiUtils.BG_PANEL);
        formScroll.getVerticalScrollBar().setUnitIncrement(12);

        // ── Buttons ───────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(GuiUtils.BG_PANEL);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL));
        JButton cancelBtn = GuiUtils.ghostButton("Cancel");
        JButton okBtn     = GuiUtils.primaryButton("Add Item");
        cancelBtn.addActionListener(e -> dialog.dispose());

        okBtn.addActionListener(e -> {
            try {
                String type    = (String) typeCb.getSelectedItem();
                String id      = fId.getText().trim().toUpperCase();
                String title   = fTitle.getText().trim();
                String yearStr = fYear.getText().trim();

                // ── Common validation ──────────────────────────
                if (id.isEmpty()) {
                    MainApp.showError(dialog, "Invalid itemId", "itemId cannot be empty."); return;
                }
                if (libraryService.findItemById(id) != null) {
                    MainApp.showError(dialog, "Duplicate itemId",
                        "An item with itemId \"" + id + "\" already exists."); return;
                }
                if (title.isEmpty()) {
                    MainApp.showError(dialog, "Invalid title", "title cannot be empty."); return;
                }
                if (!yearStr.matches("\\d{4}")) {
                    MainApp.showError(dialog, "Invalid year",
                        "year must be exactly 4 digits.\nExample: 2023"); return;
                }
                int yr = Integer.parseInt(yearStr);
                if (yr < 1000 || yr > 2100) {
                    MainApp.showError(dialog, "Invalid year",
                        "year must be between 1000 and 2100."); return;
                }

                LibraryItem item;

                switch (type) {

                    // ──────────────────────────────────────────────────────
                    //  Book(itemId, title, author, publisher, year, ISBN, np)
                    // ──────────────────────────────────────────────────────
                    case "Book": {
                        String author    = fBookAuthor.getText().trim();
                        String publisher = fBookPublisher.getText().trim();
                        String isbnRaw   = fBookISBN.getText().trim();
                        String npStr     = fBookNp.getText().trim();

                        if (author.isEmpty()) {
                            MainApp.showError(dialog, "Invalid author", "author cannot be empty."); return;
                        }
                        if (publisher.isEmpty()) {
                            MainApp.showError(dialog, "Invalid publisher", "publisher cannot be empty."); return;
                        }
                        if (!InvalidISBNException.isValidISBN(isbnRaw)) {
                            MainApp.showError(dialog, "Invalid ISBN",
                                "ISBN must be 10 or 13 digits.\n" +
                                "With or without hyphens are both accepted.\n" +
                                "Example: 9780134685991"); return;
                        }
                        if (!npStr.matches("\\d+") || Integer.parseInt(npStr) < 1) {
                            MainApp.showError(dialog, "Invalid np",
                                "np (number of pages) must be a positive whole number.\n" +
                                "Example: 500"); return;
                        }
                        item = new Book(id, title, author, publisher, yr,
                                normalizeISBN(isbnRaw), Integer.parseInt(npStr));
                        break;
                    }

                    // ──────────────────────────────────────────────────────
                    //  DVD(itemId, title, year, director, duration, rating)
                    // ──────────────────────────────────────────────────────
                    case "DVD": {
                        String director    = fDvdDirector.getText().trim();
                        String durationStr = fDvdDuration.getText().trim();
                        String rating      = fDvdRating.getText().trim();

                        if (director.isEmpty()) {
                            MainApp.showError(dialog, "Invalid director", "director cannot be empty."); return;
                        }
                        if (!durationStr.matches("\\d+") || Integer.parseInt(durationStr) < 1) {
                            MainApp.showError(dialog, "Invalid duration",
                                "duration must be a positive number of minutes.\n" +
                                "Example: 148"); return;
                        }
                        if (rating.isEmpty()) {
                            MainApp.showError(dialog, "Invalid rating",
                                "rating cannot be empty.\nExample: PG-13"); return;
                        }
                        item = new DVD(id, title, yr, director,
                                Integer.parseInt(durationStr), rating);
                        break;
                    }

                    // ──────────────────────────────────────────────────────
                    //  Magazine(itemId, title, year, publisher, isDate, vn)
                    // ──────────────────────────────────────────────────────
                    case "Magazine": {
                        String publisher = fMagPublisher.getText().trim();
                        String isDate    = fMagIsDate.getText().trim();
                        String vnStr     = fMagVn.getText().trim();

                        if (publisher.isEmpty()) {
                            MainApp.showError(dialog, "Invalid publisher", "publisher cannot be empty."); return;
                        }
                        if (!isDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            MainApp.showError(dialog, "Invalid isDate",
                                "isDate must be in format YYYY-MM-DD.\n" +
                                "Example: 2024-03-01"); return;
                        }
                        if (!vnStr.matches("\\d+") || Integer.parseInt(vnStr) < 1) {
                            MainApp.showError(dialog, "Invalid vn",
                                "vn (volume number) must be a positive whole number.\n" +
                                "Example: 245"); return;
                        }
                        item = new Magazine(id, title, yr, publisher, isDate,
                                Integer.parseInt(vnStr));
                        break;
                    }

                    // ──────────────────────────────────────────────────────
                    //  DigitalBook(itemId, title, author, publisher, year,
                    //              ISBN, np, fileSize, format, downloadlink)
                    // ──────────────────────────────────────────────────────
                    case "DigitalBook": {
                        String author        = fDbAuthor.getText().trim();
                        String publisher     = fDbPublisher.getText().trim();
                        String isbnRaw       = fDbISBN.getText().trim();
                        String npStr         = fDbNp.getText().trim();
                        String fileSizeStr   = fDbFileSize.getText().trim();
                        String format        = fDbFormat.getText().trim().toUpperCase();
                        String downloadlink  = fDbDownloadlink.getText().trim();

                        if (author.isEmpty()) {
                            MainApp.showError(dialog, "Invalid author", "author cannot be empty."); return;
                        }
                        if (publisher.isEmpty()) {
                            MainApp.showError(dialog, "Invalid publisher", "publisher cannot be empty."); return;
                        }
                        if (!InvalidISBNException.isValidISBN(isbnRaw)) {
                            MainApp.showError(dialog, "Invalid ISBN",
                                "ISBN must be 10 or 13 digits.\n" +
                                "With or without hyphens are both accepted.\n" +
                                "Example: 9781593279288"); return;
                        }
                        if (!npStr.matches("\\d+") || Integer.parseInt(npStr) < 1) {
                            MainApp.showError(dialog, "Invalid np",
                                "np (number of pages) must be a positive whole number.\n" +
                                "Example: 560"); return;
                        }
                        if (!fileSizeStr.matches("\\d+(\\.\\d+)?") || Double.parseDouble(fileSizeStr) <= 0) {
                            MainApp.showError(dialog, "Invalid fileSize",
                                "fileSize must be a positive number in MB.\n" +
                                "Example: 3.2"); return;
                        }
                        if (format.isEmpty()) {
                            MainApp.showError(dialog, "Invalid format",
                                "format cannot be empty.\nExample: PDF"); return;
                        }
                        if (downloadlink.isEmpty()) {
                            MainApp.showError(dialog, "Invalid downloadlink",
                                "downloadlink cannot be empty.\n" +
                                "Example: http://download.com/book.pdf"); return;
                        }
                        item = new DigitalBook(id, title, author, publisher, yr,
                                normalizeISBN(isbnRaw), Integer.parseInt(npStr),
                                Double.parseDouble(fileSizeStr), format, downloadlink);
                        break;
                    }

                    default: return;
                }

                libraryService.addItem(item);
                refreshTable();
                MainApp.showInfo(this, "Item Added",
                    "\"" + title + "\" has been added successfully.");
                dialog.dispose();

            } catch (InvalidISBNException ex) {
                MainApp.showError(dialog, "Invalid ISBN", ex.getMessage());
            } catch (NumberFormatException ex) {
                MainApp.showError(dialog, "Invalid Input",
                    "Check numeric fields: year, np, duration, vn, fileSize.");
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(okBtn);

        dialog.add(formScroll, BorderLayout.CENTER);
        dialog.add(btnPanel,   BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Helper: build a labeled section panel for one item type ──
    private JPanel makeTypePanel(String[] labels, JTextField... fields) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(GuiUtils.BG_PANEL);
        for (int i = 0; i < labels.length; i++) {
            panel.add(GuiUtils.formRow(labels[i], fields[i]));
            if (i < labels.length - 1)
                panel.add(Box.createVerticalStrut(8));
        }
        return panel;
    }

    private String buildDetail(LibraryItem item) {
        StringBuilder sb = new StringBuilder(
            "  itemId: " + item.getItemId()
            + "   |   year: " + item.getYear() + "   |   ");

        if (item instanceof DigitalBook) {
            DigitalBook db = (DigitalBook) item;
            sb.append("author: ").append(db.getauthor())
              .append("   |   publisher: ").append(db.getpublisher())
              .append("   |   ISBN: ").append(db.getISBN())
              .append("   |   np: ").append(db.getNp())
              .append("   |   fileSize: ").append(db.getFileSize()).append(" MB")
              .append("   |   format: ").append(db.getFormat())
              .append("   |   downloadlink: ").append(db.getDownloadlink());
        } else if (item instanceof Book) {
            Book b = (Book) item;
            sb.append("author: ").append(b.getauthor())
              .append("   |   publisher: ").append(b.getpublisher())
              .append("   |   ISBN: ").append(b.getISBN())
              .append("   |   np: ").append(b.getNp());
        } else if (item instanceof DVD) {
            DVD d = (DVD) item;
            sb.append("director: ").append(d.getDirector())
              .append("   |   duration: ").append(d.getDuration()).append(" min")
              .append("   |   rating: ").append(d.getRating());
        } else if (item instanceof Magazine) {
            Magazine m = (Magazine) item;
            sb.append("publisher: ").append(m.getPublisher())
              .append("   |   isDate: ").append(m.getIsDate())
              .append("   |   vn: ").append(m.getVn());
        }
        return sb.toString();
    }
}