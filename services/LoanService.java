package com.library.services;

import com.library.models.transactions.Loan;
import com.library.models.items.LibraryItem;
import com.library.models.persons.*;

import java.util.*;
import java.io.*;
import java.time.LocalDate;

public class LoanService extends Service<Loan> {

    private LinkedList<Loan> loans;
    private Map<String, Integer> loanCountPerMember;
    private int nextLoanId;
    private MemberService memberService;
    private LibraryService libraryService;

    public LoanService(MemberService memberService, LibraryService libraryService) {
        this.loans               = new LinkedList<>();
        this.loanCountPerMember  = new HashMap<>();
        this.nextLoanId          = 1;
        this.memberService       = memberService;
        this.libraryService      = libraryService;
        System.out.println("✓ LoanService initialized.");
    }

    public Loan borrowItem(Person person, LibraryItem item) {
        if (!item.isAvailable()) {
            System.out.println("✗ Item is not available: " + item.getTitle());
            return null;
        }
        int currentLoans = getActiveLoansForPerson(person.getId()).size();
        int maxLimit     = getPersonLoanLimit(person);
        if (currentLoans >= maxLimit) {
            System.out.println("✗ Loan limit exceeded!");
            System.out.println("  " + person.getName() + " has " + currentLoans + "/" + maxLimit + " active loans.");
            return null;
        }
        item.markAsBorrowed();
        String loanId = "L" + String.format("%04d", nextLoanId++);
        Loan loan = new Loan(loanId, person, item);
        loans.add(loan);
        loanCountPerMember.put(person.getId(), loanCountPerMember.getOrDefault(person.getId(), 0) + 1);
        libraryService.incrementBorrowCount(item.getItemId());
        saveToFile("loans.txt");
        System.out.println("\n ✓ Item borrowed successfully!");
        System.out.println("\n Loan ID: " + loanId);
        System.out.println("\n  Borrower: " + person.getName());
        System.out.println("\n  Item: " + item.getTitle());
        System.out.println("\n  Due Date: " + loan.getDueDate());
        return loan;
    }

    public boolean returnItem(String loanId) {
        Loan loan = findLoanById(loanId);
        if (loan == null) {
            System.out.println("\n ✗ Loan not found: " + loanId);
            return false;
        }
        if (loan.getStatus().equals("COMPLETED") || loan.getReturnDate() != null) {
            System.out.println("\n ✗ This loan is already completed/returned.");
            return false;
        }
        loan.getItem().markAsReturned();
        loan.complete();
        saveToFile("loans.txt");
        System.out.println("\n ✓ Item returned successfully!");
        return true;
    }

    public boolean renewLoan(String loanId, int additionalDays) {
        Loan loan = findLoanById(loanId);
        if (loan == null) {
            System.out.println("\n ✗ Loan not found: " + loanId);
            return false;
        }
        loan.renewLoan(additionalDays);
        return true;
    }

    public List<Loan> getActiveLoansForPerson(String personId) {
        List<Loan> activeLoans = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.getPerson().getId().equals(personId) && loan.getStatus().equals("ACTIVE"))
                activeLoans.add(loan);
        }
        return activeLoans;
    }

    public List<Loan> getAllActiveLoans() {
        List<Loan> activeLoans = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.getStatus().equals("ACTIVE")) activeLoans.add(loan);
        }
        return activeLoans;
    }

    public List<Loan> getOverdueLoans() {
        List<Loan> overdueLoans = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.isOverdue()) overdueLoans.add(loan);
        }
        return overdueLoans;
    }

    public double calculateTotalLateFees() {
        double total = 0.0;
        for (Loan loan : loans) total += loan.calculateFees();
        return total;
    }

    public List<Loan> getLoanHistory(String personId) {
        List<Loan> history = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.getPerson().getId().equals(personId)) history.add(loan);
        }
        return history;
    }

    @Override
    public Loan findById(String loanId) { return findLoanById(loanId); }

    public Loan findLoanById(String loanId) {
        for (Loan loan : loans) {
            if (loan.getTransactionId().equals(loanId)) return loan;
        }
        return null;
    }

    public Map<String, Integer> getLoanCountPerMember() {
        return Collections.unmodifiableMap(loanCountPerMember);
    }

    private int getPersonLoanLimit(Person person) {
        if (person instanceof Professor) return ((Professor) person).getMaxLoanLimit();
        if (person instanceof Staff)     return ((Staff) person).getMaxLoanLimit();
        if (person instanceof Student)   return ((Student) person).getMaxLoanLimit();
        if (person instanceof Member)    return ((Member) person).getMaxLoanLimit();
        return 5;
    }

    @Override
    public void displayAll() {
        if (loans.isEmpty()) { System.out.println("\n📋 No loans recorded.\n"); return; }
        System.out.println("ALL LOANS");
        System.out.println("Total Loans: " + loans.size());
        for (Loan loan : loans) { loan.displayInfo(); System.out.println("----------------------------------------"); }
    }

    public void displayActiveLoans() {
        List<Loan> activeLoans = getAllActiveLoans();
        if (activeLoans.isEmpty()) { System.out.println("\n 📋 No active loans.\n"); return; }
        System.out.println("ACTIVE LOANS");
        System.out.println("Count: " + activeLoans.size());
        for (Loan loan : activeLoans) loan.displayInfo();
    }

    public void displayOverdueLoans() {
        List<Loan> overdueLoans = getOverdueLoans();
        if (overdueLoans.isEmpty()) { System.out.println("\n ✅ No overdue loans!\n"); return; }
        System.out.println("\n ⚠️ OVERDUE LOANS ⚠️");
        System.out.println("\n Count: " + overdueLoans.size());
        for (Loan loan : overdueLoans) loan.displayInfo();
    }

    @Override
    public void displayStatistics() {
        int active = 0, completed = 0, overdue = 0;
        double totalFees = 0.0;
        for (Loan loan : loans) {
            if (loan.getStatus().equals("ACTIVE"))    active++;
            if (loan.getStatus().equals("COMPLETED")) completed++;
            if (loan.isOverdue()) { overdue++; totalFees += loan.calculateFees(); }
        }
        System.out.println("LOAN STATISTICS");
        System.out.println("📊 Total Loans:     " + loans.size());
        System.out.println("✅ Active:          " + active);
        System.out.println("✔️  Completed:      " + completed);
        System.out.println("⚠️  Overdue:        " + overdue);
        System.out.println("💰 Total Fees:      " + String.format("%.2f", totalFees) + " DA");
        if (!loanCountPerMember.isEmpty()) {
            String topMemberId = Collections.max(loanCountPerMember.entrySet(), Map.Entry.comparingByValue()).getKey();
            System.out.println("🏆 Most Active Member ID: " + topMemberId + " (" + loanCountPerMember.get(topMemberId) + " loans)");
        }
    }

    @Override
    public boolean saveToFile(String filename) {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();
        String fullPath = "data/" + filename;
        try (PrintWriter writer = new PrintWriter(new FileWriter(fullPath))) {
            for (Loan loan : loans) {
                String line = loan.getTransactionId() + "|" + loan.getPerson().getId() + "|"
                        + loan.getItem().getItemId() + "|" + loan.getDueDate() + "|"
                        + (loan.getReturnDate() != null ? loan.getReturnDate() : "NULL") + "|"
                        + loan.getStatus();
                writer.println(line);
            }
            System.out.println("\n ✓ " + loans.size() + " loans saved to: " + filename);
            return true;
        } catch (IOException e) {
            System.out.println("\n ✗ Error saving loans: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean loadFromFile(String filename) {
        int loadedCount = 0, errorCount = 0;
        String fullPath = "data/" + filename;
        File file = new File(fullPath);
        if (!file.exists()) {
            System.out.println("ℹ️  Loans file not found: " + fullPath + " (starting fresh)");
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fullPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split("\\|");
                    if (parts.length < 6) { errorCount++; continue; }
                    String loanId = parts[0].trim(); String personId = parts[1].trim();
                    String itemId = parts[2].trim(); String dueDateStr = parts[3].trim();
                    String returnDateStr = parts[4].trim(); String status = parts[5].trim();
                    boolean alreadyExists = false;
                    for (Loan existing : loans) {
                        if (existing.getTransactionId().equals(loanId)) { alreadyExists = true; break; }
                    }
                    if (alreadyExists) continue;
                    Person person = memberService.findMemberById(personId);
                    if (person == null) { errorCount++; continue; }
                    LibraryItem item = libraryService.findItemById(itemId);
                    if (item == null) { errorCount++; continue; }
                    LocalDate dueDate    = LocalDate.parse(dueDateStr);
                    LocalDate returnDate = returnDateStr.equals("NULL") ? null : LocalDate.parse(returnDateStr);
                    Loan loan = new Loan(loanId, person, item);
                    loan.setDueDate(dueDate);
                    try {
                        if (status.equals("COMPLETED")) {
                            loan.setStatus("COMPLETED");
                            if (returnDate != null) {
                                loan.setReturnDate(returnDate);
                            } else {
                                java.lang.reflect.Field f = Loan.class.getDeclaredField("returnDate");
                                f.setAccessible(true);
                                f.set(loan, dueDate);
                            }
                            if (!item.isAvailable()) item.markAsReturned();
                        } else if (status.equals("ACTIVE")) {
                            loan.setStatus("ACTIVE");
                            if (item.isAvailable()) item.markAsBorrowed();
                        } else if (status.equals("CANCELLED")) {
                            loan.setStatus("CANCELLED");
                        }
                    } catch (Exception e) { errorCount++; continue; }
                    if (loanId.startsWith("L")) {
                        try {
                            int num = Integer.parseInt(loanId.substring(1));
                            if (num >= nextLoanId) nextLoanId = num + 1;
                        } catch (NumberFormatException ignored) {}
                    }
                    loans.add(loan);
                    loanCountPerMember.put(personId, loanCountPerMember.getOrDefault(personId, 0) + 1);
                    loadedCount++;
                } catch (Exception e) { errorCount++; }
            }
            System.out.println("✓ Successfully loaded " + loadedCount + " loans from: " + filename);
            if (errorCount > 0) System.out.println("⚠️  Skipped " + errorCount + " loans due to errors");
            return true;
        } catch (IOException e) {
            System.out.println("✗ Error loading loans: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void clear() {
        loans.clear();
        loanCountPerMember.clear();
        nextLoanId = 1;
        System.out.println("✓ All loans cleared.");
    }
}