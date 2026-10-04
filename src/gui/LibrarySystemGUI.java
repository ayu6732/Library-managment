package com.library.gui;

import com.library.services.*;
import com.library.utils.BorrowGraph;
import com.library.db.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class LibrarySystemGUI extends JFrame {
    
    private LibraryService libraryService;
    private MemberService memberService;
    private LoanService loanService;
    private ReservationService reservationService;
    private BorrowGraph borrowGraph;
    
    private JPanel mainPanel;
    private JLabel statusLabel;
    private CardLayout cardLayout;
    
    public LibrarySystemGUI() {
        // Initialize services
        libraryService = new LibraryService();
        memberService = new MemberService();
        loanService = new LoanService(memberService, libraryService);
        reservationService = new ReservationService(memberService, libraryService);
        borrowGraph = new BorrowGraph();
        
        // Setup JFrame
        setTitle("Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // Menu
        setJMenuBar(createMenuBar());
        
        // Main panel with CardLayout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Add all panels
        mainPanel.add(new DashboardPanel(libraryService, memberService, loanService, reservationService), "dashboard");
        mainPanel.add(new ItemsPanel(libraryService), "items");
        mainPanel.add(new MembersPanel(memberService), "members");
        mainPanel.add(new LoansPanel(loanService, memberService, libraryService), "loans");
        mainPanel.add(new ReservationsPanel(reservationService, memberService, libraryService), "reservations");
        mainPanel.add(new SearchPanel(libraryService), "search");
        mainPanel.add(new StatisticsPanel(libraryService, memberService, loanService, reservationService, borrowGraph), "statistics");
        
        // Toolbar
        JToolBar toolbar = createToolbar();
        
        // Status bar
        statusLabel = new JLabel(" Ready");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        
        // Add to frame
        add(toolbar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        
        // Load data
        loadData();
        DatabaseManager.initializeSchema();
        
        // Show dashboard
        showPanel("dashboard");
        
        // Save on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveData();
                DatabaseManager.closeConnection();
            }
        });
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem loadItem = new JMenuItem("Load");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        saveItem.addActionListener(e -> saveData());
        loadItem.addActionListener(e -> loadData());
        exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        menuBar.add(fileMenu);
        
        return menuBar;
    }
    
    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        
        String[] buttons = {"Dashboard", "Items", "Members", "Loans", "Reservations", "Search", "Statistics"};
        String[] cardNames = {"dashboard", "items", "members", "loans", "reservations", "search", "statistics"};
        
        for (int i = 0; i < buttons.length; i++) {
            JButton btn = new JButton(buttons[i]);
            final String cardName = cardNames[i];
            btn.addActionListener(e -> showPanel(cardName));
            toolbar.add(btn);
        }
        
        return toolbar;
    }
    
    private void showPanel(String name) {
        cardLayout.show(mainPanel, name);
        statusLabel.setText(" Viewing: " + name);
    }
    
    private void loadData() {
        if (new File("data/items.txt").exists()) libraryService.loadFromFile("items.txt");
        if (new File("data/members.txt").exists()) memberService.loadFromFile("members.txt");
        if (new File("data/loans.txt").exists()) loanService.loadFromFile("loans.txt");
        if (new File("data/reservations.txt").exists()) reservationService.loadFromFile("reservations.txt");
        statusLabel.setText(" Data loaded");
    }
    
    private void saveData() {
        libraryService.saveToFile("items.txt");
        memberService.saveToFile("members.txt");
        loanService.saveToFile("loans.txt");
        reservationService.saveToFile("reservations.txt");
        statusLabel.setText(" Data saved");
        JOptionPane.showMessageDialog(this, "Data saved!");
    }
    
    public static void showMessage(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg);
    }
    
    public static int confirmDialog(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Confirm", JOptionPane.YES_NO_OPTION);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibrarySystemGUI().setVisible(true));
    }
}