package com.library.gui;

import com.library.services.*;
import com.library.utils.ReflectionUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class ReflectionPanel extends JPanel {

    private final LibraryService libraryService;
    private final MemberService  memberService;
    private final LoanService    loanService;
    private final JTextArea      output;

    public ReflectionPanel(LibraryService ls, MemberService ms, LoanService los) {
        this.libraryService = ls;
        this.memberService  = ms;
        this.loanService    = los;

        setLayout(new BorderLayout(0, 16));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("\uD83D\uDD2C  Object Inspector"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("Java Reflection API: inspect any object's private fields, type hierarchy and methods at runtime"), BorderLayout.SOUTH);

        JComboBox<String> typeCb = GuiUtils.styledCombo("Library Item", "Member", "Loan");
        JTextField idField       = GuiUtils.styledField("Enter ID to inspect");
        idField.setPreferredSize(new Dimension(200, 32));
        JButton inspBtn = GuiUtils.primaryButton("\uD83D\uDD2C Inspect Object");
        JButton clsBtn  = GuiUtils.ghostButton("\uD83D\uDCCB Class Hierarchy");

        JPanel toolbar = GuiUtils.toolbar(typeCb, idField, inspBtn, clsBtn);

        output = GuiUtils.monoArea();
        output.setForeground(GuiUtils.ACCENT);
        output.setText("Select a type, enter an ID, then press Inspect.\n\nExamples:\n  Items:   B001, D001\n  Members: P001, S001\n  Loans:   L0001\n");

        JScrollPane outputScroll = GuiUtils.scrollMono(output);
        JPanel outputCard = GuiUtils.sectionCard("Reflection Output", outputScroll);

        inspBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            Object obj = null;
            switch ((String) typeCb.getSelectedItem()) {
                case "Library Item": obj = libraryService.findItemById(id);  break;
                case "Member":       obj = memberService.findMemberById(id); break;
                case "Loan":         obj = loanService.findLoanById(id);     break;
            }
            if (obj == null) { output.setText("\u2717 Object not found for ID: " + id + "\n"); return; }
            final Object finalObj = obj;
            captureAndShow(() -> ReflectionUtils.inspectGeneric(finalObj));
        });

        clsBtn.addActionListener(e -> captureAndShow(() -> {
            ReflectionUtils.printClassInfo(com.library.models.items.Book.class);
            ReflectionUtils.printClassInfo(com.library.models.persons.Professor.class);
            ReflectionUtils.printClassInfo(com.library.models.transactions.Loan.class);
        }));

        JPanel topArea = new JPanel(new BorderLayout(0, 12));
        topArea.setBackground(GuiUtils.BG_DARK);
        topArea.add(header,  BorderLayout.NORTH);
        topArea.add(toolbar, BorderLayout.SOUTH);

        add(topArea,    BorderLayout.NORTH);
        add(outputCard, BorderLayout.CENTER);
    }

    private void captureAndShow(Runnable action) {
        StringWriter sw = new StringWriter();
        PrintStream old = System.out;
        PrintStream ps  = new PrintStream(new OutputStream() {
            @Override public void write(int b) { sw.write(b); }
            @Override public void write(byte[] b, int off, int len) { sw.write(new String(b, off, len)); }
        });
        System.setOut(ps);
        action.run();
        System.setOut(old);
        output.setText(sw.toString());
    }
}