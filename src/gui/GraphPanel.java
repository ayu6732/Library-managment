package com.library.gui;

import com.library.models.persons.Person;
import com.library.models.transactions.Loan;
import com.library.services.*;
import com.library.utils.BorrowGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class GraphPanel extends JPanel {

    private final BorrowGraph    graph;
    private final LoanService    loanService;
    private final MemberService  memberService;
    private final LibraryService libraryService;

    private GraphCanvas canvas;
    private JScrollPane scrollPane;
    private JLabel      statusLbl;

    public GraphPanel(BorrowGraph graph, LoanService loanService,
                      MemberService memberService, LibraryService libraryService) {
        this.graph          = graph;
        this.loanService    = loanService;
        this.memberService  = memberService;
        this.libraryService = libraryService;

        setLayout(new BorderLayout(0, 10));
        setBackground(GuiUtils.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ── Header ───────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GuiUtils.BG_DARK);
        header.add(GuiUtils.pageTitle("Borrowing Graph"), BorderLayout.NORTH);
        header.add(GuiUtils.pageSubtitle("Members (circles) connected to items they borrowed (rectangles)"), BorderLayout.SOUTH);

        // ── Toolbar ──────────────────────────────────────────────
        JButton buildBtn   = GuiUtils.primaryButton("Build Graph");
        JButton refreshBtn = GuiUtils.ghostButton("Refresh");
        JButton resetBtn   = GuiUtils.ghostButton("Reset View");

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setBackground(GuiUtils.BG_DARK);
        toolbar.add(buildBtn);
        toolbar.add(refreshBtn);
        toolbar.add(resetBtn);

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        legend.setBackground(GuiUtils.BG_DARK);
        legend.add(legendItem(new Color(62, 110, 180), "Member (circle)"));
        legend.add(legendItem(new Color(139, 90, 43),  "Book/Item (rect)"));

        JPanel toolRow = new JPanel(new BorderLayout());
        toolRow.setBackground(GuiUtils.BG_DARK);
        toolRow.add(toolbar, BorderLayout.WEST);
        toolRow.add(legend,  BorderLayout.EAST);

        JPanel topArea = new JPanel(new BorderLayout(0, 8));
        topArea.setBackground(GuiUtils.BG_DARK);
        topArea.add(header,  BorderLayout.NORTH);
        topArea.add(toolRow, BorderLayout.SOUTH);

        // ── Graph canvas inside scroll pane ──────────────────────
        canvas = new GraphCanvas();

        scrollPane = new JScrollPane(canvas);
        scrollPane.setBorder(BorderFactory.createLineBorder(GuiUtils.BORDER_COL));
        scrollPane.setBackground(GuiUtils.BG_PANEL);
        scrollPane.getViewport().setBackground(new Color(250, 247, 242));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // ── Status bar ───────────────────────────────────────────
        statusLbl = new JLabel("  Press \"Build Graph\" to visualise borrow relationships.");
        statusLbl.setFont(GuiUtils.FONT_BODY);
        statusLbl.setForeground(GuiUtils.TEXT_SEC);
        statusLbl.setBackground(GuiUtils.BG_PANEL);
        statusLbl.setOpaque(true);
        statusLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, GuiUtils.BORDER_COL),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // ── Button actions ───────────────────────────────────────
        buildBtn.addActionListener(e -> buildAndDraw());
        refreshBtn.addActionListener(e -> buildAndDraw());
        resetBtn.addActionListener(e -> {
            canvas.resetView();
            scrollPane.getViewport().setViewPosition(new Point(0, 0));
        });

        add(topArea,    BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLbl,  BorderLayout.SOUTH);
    }

    private void buildAndDraw() {
        List<Loan> allLoans = new ArrayList<>();
        for (Person p : memberService.getAllMembers()) {
            allLoans.addAll(loanService.getLoanHistory(p.getId()));
        }
        graph.buildFromLoans(allLoans);

        int nc = graph.getNodeCount();
        int ec = graph.getEdgeCount();

        if (nc == 0) {
            statusLbl.setText("  No borrow data found. Add some loans first.");
            statusLbl.setForeground(GuiUtils.ACCENT_RED);
            canvas.clear();
            return;
        }
        canvas.buildFromLoans(allLoans);
        statusLbl.setText("  Graph built: " + nc + " nodes, " + ec
                + " edges.  Drag nodes to rearrange. Scroll to see all.");
        statusLbl.setForeground(GuiUtils.ACCENT_GRN);
    }

    private JPanel legendItem(Color color, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setBackground(GuiUtils.BG_DARK);
        JLabel icon = new JLabel("  ");
        icon.setOpaque(true);
        icon.setBackground(color);
        icon.setPreferredSize(new Dimension(16, 16));
        JLabel lbl = new JLabel(label);
        lbl.setFont(GuiUtils.FONT_BODY);
        lbl.setForeground(GuiUtils.TEXT_SEC);
        p.add(icon);
        p.add(lbl);
        return p;
    }

    // ══════════════════════════════════════════════════════════════
    //  GraphCanvas
    // ══════════════════════════════════════════════════════════════
    static class GraphCanvas extends JPanel implements Scrollable {

        private static final Color COL_MEMBER     = new Color(62,  110, 180);
        private static final Color COL_MEMBER_BDR = new Color(30,   70, 140);
        private static final Color COL_ITEM       = new Color(139,  90,  43);
        private static final Color COL_ITEM_BDR   = new Color( 90,  55,  20);
        private static final Color COL_EDGE       = new Color(170, 148, 110);
        private static final Color COL_EDGE_HOV   = new Color(139,  90,  43);
        private static final Color COL_HOV_GLOW   = new Color(255, 200, 100, 70);

        private static final int MEMBER_R  = 32;
        private static final int ITEM_W    = 110;
        private static final int ITEM_H    = 40;
        private static final int PAD       = 60;
        private static final int COL_GAP_V = 90;

        private final List<GraphNode> nodes = new ArrayList<>();
        private final List<int[]>     edges = new ArrayList<>();

        private GraphNode dragNode = null;
        private int       dragOffX, dragOffY;
        private GraphNode hoverNode = null;

        GraphCanvas() {
            setBackground(new Color(250, 247, 242));
            setOpaque(true);

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    dragNode = nodeAt(e.getX(), e.getY());
                    if (dragNode != null) {
                        dragOffX = e.getX() - dragNode.x;
                        dragOffY = e.getY() - dragNode.y;
                    }
                }
                @Override public void mouseReleased(MouseEvent e) { dragNode = null; }
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragNode != null) {
                        dragNode.x = Math.max(PAD, e.getX() - dragOffX);
                        dragNode.y = Math.max(PAD, e.getY() - dragOffY);
                        recomputeSize();
                        repaint();
                    }
                }
                @Override public void mouseMoved(MouseEvent e) {
                    GraphNode n = nodeAt(e.getX(), e.getY());
                    if (n != hoverNode) {
                        hoverNode = n;
                        setCursor(n != null
                                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                : Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        void buildFromLoans(List<Loan> loans) {
            nodes.clear();
            edges.clear();

            Map<String, GraphNode> memberMap = new LinkedHashMap<>();
            Map<String, GraphNode> itemMap   = new LinkedHashMap<>();

            for (Loan loan : loans) {
                String mid = loan.getPerson().getId();
                String iid = loan.getItem().getItemId();
                if (!memberMap.containsKey(mid))
                    memberMap.put(mid, new GraphNode(mid, loan.getPerson().getName(), true));
                if (!itemMap.containsKey(iid))
                    itemMap.put(iid, new GraphNode(iid, loan.getItem().getTitle(), false));
            }

            int mCount   = memberMap.size();
            int iCount   = itemMap.size();
            int neededH  = Math.max(mCount, iCount) * COL_GAP_V + PAD * 2;
            int neededW  = 720;
            setPreferredSize(new Dimension(neededW, Math.max(neededH, 420)));

            int leftX  = neededW / 4;
            int rightX = 3 * neededW / 4;

            int mi = 0;
            for (GraphNode n : memberMap.values()) {
                n.x = leftX;
                n.y = PAD + mi * COL_GAP_V + COL_GAP_V / 2;
                nodes.add(n);
                mi++;
            }
            int ii = 0;
            for (GraphNode n : itemMap.values()) {
                n.x = rightX;
                n.y = PAD + ii * COL_GAP_V + COL_GAP_V / 2;
                nodes.add(n);
                ii++;
            }

            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < nodes.size(); i++) idx.put(nodes.get(i).id, i);

            Set<String> seen = new HashSet<>();
            for (Loan loan : loans) {
                String key = loan.getPerson().getId() + "->" + loan.getItem().getItemId();
                if (seen.add(key)) {
                    Integer a = idx.get(loan.getPerson().getId());
                    Integer b = idx.get(loan.getItem().getItemId());
                    if (a != null && b != null) edges.add(new int[]{a, b});
                }
            }

            revalidate();
            repaint();
        }

        void clear() {
            nodes.clear();
            edges.clear();
            hoverNode = null;
            setPreferredSize(new Dimension(720, 420));
            revalidate();
            repaint();
        }

        void resetView() {
            List<GraphNode> members = new ArrayList<>();
            List<GraphNode> items   = new ArrayList<>();
            for (GraphNode n : nodes) (n.isMember ? members : items).add(n);
            int neededH = Math.max(members.size(), items.size()) * COL_GAP_V + PAD * 2;
            int neededW = 720;
            setPreferredSize(new Dimension(neededW, Math.max(neededH, 420)));
            int leftX = neededW / 4, rightX = 3 * neededW / 4;
            for (int i = 0; i < members.size(); i++) {
                members.get(i).x = leftX;
                members.get(i).y = PAD + i * COL_GAP_V + COL_GAP_V / 2;
            }
            for (int i = 0; i < items.size(); i++) {
                items.get(i).x = rightX;
                items.get(i).y = PAD + i * COL_GAP_V + COL_GAP_V / 2;
            }
            revalidate();
            repaint();
        }

        private void recomputeSize() {
            int maxX = 720, maxY = 420;
            for (GraphNode n : nodes) {
                maxX = Math.max(maxX, n.x + ITEM_W + PAD);
                maxY = Math.max(maxY, n.y + ITEM_H + PAD);
            }
            setPreferredSize(new Dimension(maxX, maxY));
            revalidate();
        }

        private GraphNode nodeAt(int px, int py) {
            for (int i = nodes.size() - 1; i >= 0; i--) {
                GraphNode n = nodes.get(i);
                if (n.isMember) {
                    if (Math.hypot(px - n.x, py - n.y) <= MEMBER_R) return n;
                } else {
                    if (px >= n.x - ITEM_W/2 && px <= n.x + ITEM_W/2 &&
                        py >= n.y - ITEM_H/2 && py <= n.y + ITEM_H/2) return n;
                }
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (nodes.isEmpty()) { drawEmpty(g2); g2.dispose(); return; }

            drawColumnLabels(g2);
            drawEdges(g2);
            drawNodes(g2);
            g2.dispose();
        }

        private void drawEmpty(Graphics2D g2) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g2.setColor(new Color(180, 160, 130));
            String m = "Press \"Build Graph\" to visualise borrow relationships.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(m, (getWidth() - fm.stringWidth(m)) / 2, getHeight() / 2);
        }

        private void drawColumnLabels(Graphics2D g2) {
            int w = getWidth();
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            // Members label
            g2.setColor(new Color(COL_MEMBER.getRed(), COL_MEMBER.getGreen(), COL_MEMBER.getBlue(), 160));
            String ml = "MEMBERS";
            g2.drawString(ml, w/4 - g2.getFontMetrics().stringWidth(ml)/2, 22);

            // Items label
            g2.setColor(new Color(COL_ITEM.getRed(), COL_ITEM.getGreen(), COL_ITEM.getBlue(), 160));
            String il = "BORROWED ITEMS";
            g2.drawString(il, 3*w/4 - g2.getFontMetrics().stringWidth(il)/2, 22);

            // Dashed divider
            g2.setColor(GuiUtils.BORDER_COL);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{6, 6}, 0));
            g2.drawLine(w/2, 30, w/2, getHeight() - 10);
            g2.setStroke(new BasicStroke(1));
        }

        private void drawEdges(Graphics2D g2) {
            for (int[] e : edges) {
                GraphNode from = nodes.get(e[0]);
                GraphNode to   = nodes.get(e[1]);
                boolean   hov  = (hoverNode == from || hoverNode == to);

                g2.setColor(hov ? COL_EDGE_HOV : COL_EDGE);
                g2.setStroke(new BasicStroke(hov ? 2.2f : 1.3f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Straight line — clean, no labels on edges
                g2.drawLine(from.x, from.y, to.x, to.y);

                // Small arrowhead near item rectangle
                double angle = Math.atan2(to.y - from.y, to.x - from.x);
                int tipX = to.x - (int)(Math.cos(angle) * (ITEM_W / 2 + 3));
                int tipY = to.y - (int)(Math.sin(angle) * (ITEM_H / 2 + 3));
                double sp = Math.toRadians(28);
                int al = 9;
                g2.drawLine(tipX, tipY,
                        tipX - (int)(al * Math.cos(angle - sp)),
                        tipY - (int)(al * Math.sin(angle - sp)));
                g2.drawLine(tipX, tipY,
                        tipX - (int)(al * Math.cos(angle + sp)),
                        tipY - (int)(al * Math.sin(angle + sp)));

                g2.setStroke(new BasicStroke(1));
            }
        }

        private void drawNodes(Graphics2D g2) {
            for (GraphNode n : nodes) {
                boolean hov = (n == hoverNode);

                if (n.isMember) {
                    if (hov) {
                        g2.setColor(COL_HOV_GLOW);
                        g2.fillOval(n.x-MEMBER_R-8, n.y-MEMBER_R-8,
                                (MEMBER_R+8)*2, (MEMBER_R+8)*2);
                    }
                    GradientPaint gp = new GradientPaint(
                            n.x-MEMBER_R, n.y-MEMBER_R, COL_MEMBER.brighter(),
                            n.x+MEMBER_R, n.y+MEMBER_R, COL_MEMBER);
                    g2.setPaint(gp);
                    g2.fillOval(n.x-MEMBER_R, n.y-MEMBER_R, MEMBER_R*2, MEMBER_R*2);
                    g2.setColor(hov ? COL_MEMBER.brighter() : COL_MEMBER_BDR);
                    g2.setStroke(new BasicStroke(hov ? 2.5f : 1.5f));
                    g2.drawOval(n.x-MEMBER_R, n.y-MEMBER_R, MEMBER_R*2, MEMBER_R*2);
                    g2.setStroke(new BasicStroke(1));
                    // Only the name inside — NO ID text
                    drawLabel(g2, n.label, n.x, n.y, Color.WHITE, MEMBER_R*2 - 8);

                } else {
                    int rx = n.x - ITEM_W/2, ry = n.y - ITEM_H/2;
                    if (hov) {
                        g2.setColor(COL_HOV_GLOW);
                        g2.fillRoundRect(rx-6, ry-6, ITEM_W+12, ITEM_H+12, 12, 12);
                    }
                    GradientPaint gp = new GradientPaint(
                            rx, ry, COL_ITEM.brighter(),
                            rx, ry+ITEM_H, COL_ITEM);
                    g2.setPaint(gp);
                    g2.fillRoundRect(rx, ry, ITEM_W, ITEM_H, 8, 8);
                    g2.setColor(hov ? COL_ITEM.brighter() : COL_ITEM_BDR);
                    g2.setStroke(new BasicStroke(hov ? 2.2f : 1.4f));
                    g2.drawRoundRect(rx, ry, ITEM_W, ITEM_H, 8, 8);
                    g2.setStroke(new BasicStroke(1));
                    // Only the title inside — NO ID text
                    drawLabel(g2, n.label, n.x, n.y, Color.WHITE, ITEM_W - 10);
                }
            }
        }

        /** Draws text centred at (cx, cy), truncated to fit maxWidth. */
        private void drawLabel(Graphics2D g2, String text,
                               int cx, int cy, Color color, int maxWidth) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            String s = text;
            while (fm.stringWidth(s) > maxWidth && s.length() > 3)
                s = s.substring(0, s.length()-2) + "…";
            g2.drawString(s, cx - fm.stringWidth(s)/2, cy + fm.getAscent()/2 - 1);
        }

        // ── Scrollable ────────────────────────────────────────────
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(720, 480); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d)  { return 20;  }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 100; }
        @Override public boolean getScrollableTracksViewportWidth()  { return false; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ── Node bean ─────────────────────────────────────────────────
    static class GraphNode {
        String id, label; boolean isMember; int x, y;
        GraphNode(String id, String label, boolean isMember) {
            this.id = id; this.label = label; this.isMember = isMember;
        }
    }
}