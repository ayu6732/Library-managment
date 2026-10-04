package com.library.utils;

import com.library.models.persons.Person;
import com.library.models.items.LibraryItem;
import com.library.models.transactions.Loan;

import java.util.*;
import java.util.LinkedHashMap;

public class BorrowGraph {

    private Map<String, Set<String>> adjacencyList;
    private Map<String, String> nodeLabels;

    public BorrowGraph() {
        this.adjacencyList = new HashMap<>();
        this.nodeLabels    = new HashMap<>();
    }

    public void addBorrowRelationship(Person person, LibraryItem item) {
        String memberNode = "M:" + person.getId();
        String itemNode   = "I:" + item.getItemId();
        nodeLabels.put(memberNode, person.getName() + " [" + person.getId() + "]");
        nodeLabels.put(itemNode,   item.getTitle()  + " [" + item.getItemId() + "]");
        adjacencyList.putIfAbsent(memberNode, new HashSet<>());
        adjacencyList.putIfAbsent(itemNode,   new HashSet<>());
        adjacencyList.get(memberNode).add(itemNode);
    }

    public void buildFromLoans(List<Loan> loans) {
        adjacencyList.clear();
        nodeLabels.clear();
        for (Loan loan : loans) {
            addBorrowRelationship(loan.getPerson(), loan.getItem());
        }
        System.out.println("✓ Borrow graph built: " + adjacencyList.size() + " nodes, " + countEdges() + " edges.");
    }

    public Set<String> getItemsBorrowedBy(String memberId) {
        String memberNode = "M:" + memberId;
        return adjacencyList.getOrDefault(memberNode, Collections.emptySet());
    }

    public Set<String> getMembersWhoBorrowed(String itemId) {
        String itemNode = "I:" + itemId;
        Set<String> members = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : adjacencyList.entrySet()) {
            if (entry.getKey().startsWith("M:") && entry.getValue().contains(itemNode)) {
                members.add(entry.getKey());
            }
        }
        return members;
    }

    public String getMostActiveMember() {
        String topNode = null; int maxEdges = 0;
        for (Map.Entry<String, Set<String>> entry : adjacencyList.entrySet()) {
            if (entry.getKey().startsWith("M:") && entry.getValue().size() > maxEdges) {
                maxEdges = entry.getValue().size();
                topNode  = entry.getKey();
            }
        }
        return topNode != null ? nodeLabels.getOrDefault(topNode, topNode) : "N/A";
    }

    public String getMostBorrowedItem() {
        Map<String, Integer> inDegree = new HashMap<>();
        for (Set<String> neighbours : adjacencyList.values()) {
            for (String neighbour : neighbours) {
                if (neighbour.startsWith("I:"))
                    inDegree.put(neighbour, inDegree.getOrDefault(neighbour, 0) + 1);
            }
        }
        String topItem = null; int maxCount = 0;
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() > maxCount) { maxCount = entry.getValue(); topItem = entry.getKey(); }
        }
        return topItem != null ? nodeLabels.getOrDefault(topItem, topItem) : "N/A";
    }

    public void displayGraph() {
        System.out.println("\n=== BORROW RELATIONSHIP GRAPH ===");
        System.out.println("Nodes : " + adjacencyList.size());
        System.out.println("Edges : " + countEdges());
        long memberCount = adjacencyList.keySet().stream().filter(k -> k.startsWith("M:")).count();
        long itemCount   = adjacencyList.keySet().stream().filter(k -> k.startsWith("I:")).count();
        System.out.println("Members in graph : " + memberCount);
        System.out.println("Items in graph   : " + itemCount);
        System.out.println("\n--- Adjacency List (Member → Items borrowed) ---");
        for (Map.Entry<String, Set<String>> entry : adjacencyList.entrySet()) {
            if (!entry.getKey().startsWith("M:")) continue;
            String memberLabel = nodeLabels.getOrDefault(entry.getKey(), entry.getKey());
            System.out.print("  " + memberLabel + " → ");
            if (entry.getValue().isEmpty()) {
                System.out.println("(no borrows)");
            } else {
                List<String> itemLabels = new ArrayList<>();
                for (String itemNode : entry.getValue())
                    itemLabels.add(nodeLabels.getOrDefault(itemNode, itemNode));
                System.out.println(String.join(", ", itemLabels));
            }
        }
        System.out.println("\n🏆 Most Active Member : " + getMostActiveMember());
        System.out.println("📚 Most Borrowed Item : " + getMostBorrowedItem());
        System.out.println("==========================================");
    }

    private int countEdges() {
        int total = 0;
        for (Set<String> neighbours : adjacencyList.values()) total += neighbours.size();
        return total;
    }

    public Map<String, Integer> getItemBorrowCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : adjacencyList.entrySet()) {
            if (!entry.getKey().startsWith("M:")) continue;
            for (String itemNode : entry.getValue()) {
                String label = nodeLabels.getOrDefault(itemNode, itemNode);
                // Shorten label for display
                if (label.length() > 16) label = label.substring(0, 15) + "\u2026";
                counts.put(label, counts.getOrDefault(label, 0) + 1);
            }
        }
        return counts;
    }

    public int getNodeCount() { return adjacencyList.size(); }
    public int getEdgeCount() { return countEdges(); }
}