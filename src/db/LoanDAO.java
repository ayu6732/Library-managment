package com.library.db;

import com.library.models.transactions.Loan;
import java.sql.*;
import java.util.*;

public class LoanDAO {

    public static boolean insertLoanTransactional(Loan loan) {
        try {
            DatabaseManager.beginTransaction();
            String insertSql = "INSERT OR IGNORE INTO loans (loan_id, member_id, item_id, due_date, return_date, status) VALUES (?,?,?,?,?,?)";
            int loanRows = DatabaseManager.executeUpdate(insertSql,
                    loan.getTransactionId(), loan.getPerson().getId(), loan.getItem().getItemId(),
                    loan.getDueDate().toString(),
                    loan.getReturnDate() != null ? loan.getReturnDate().toString() : null,
                    loan.getStatus());
            if (loanRows == 0) { DatabaseManager.rollback(); return false; }
            DatabaseManager.executeUpdate("UPDATE library_items SET available = 0 WHERE item_id = ?", loan.getItem().getItemId());
            DatabaseManager.commit();
            System.out.println("✓ LoanDAO: transaction committed.");
            return true;
        } catch (SQLException e) { DatabaseManager.rollback(); System.err.println("✗ LoanDAO.insertLoanTransactional: " + e.getMessage()); return false; }
    }

    public static boolean returnLoanTransactional(String loanId, String itemId, String returnDate) {
        try {
            DatabaseManager.beginTransaction();
            DatabaseManager.executeUpdate("UPDATE loans SET status = 'COMPLETED', return_date = ? WHERE loan_id = ?", returnDate, loanId);
            DatabaseManager.executeUpdate("UPDATE library_items SET available = 1 WHERE item_id = ?", itemId);
            DatabaseManager.commit();
            System.out.println("✓ LoanDAO: return transaction committed.");
            return true;
        } catch (SQLException e) { DatabaseManager.rollback(); System.err.println("✗ LoanDAO.returnLoanTransactional: " + e.getMessage()); return false; }
    }

    public static List<String[]> getAllLoansRaw() {
        List<String[]> rows = new ArrayList<>();
        try {
            ResultSet rs = DatabaseManager.executeQuery("SELECT loan_id, member_id, item_id, due_date, return_date, status FROM loans");
            while (rs.next()) rows.add(new String[]{ rs.getString("loan_id"), rs.getString("member_id"), rs.getString("item_id"), rs.getString("due_date"), rs.getString("return_date"), rs.getString("status") });
            rs.close();
        } catch (SQLException e) { System.err.println("✗ LoanDAO.getAllLoansRaw: " + e.getMessage()); }
        return rows;
    }

    public static List<String[]> getActiveLoansForMember(String memberId) {
        List<String[]> rows = new ArrayList<>();
        try {
            ResultSet rs = DatabaseManager.executeQuery("SELECT loan_id, item_id, due_date FROM loans WHERE member_id = ? AND status = 'ACTIVE'", memberId);
            while (rs.next()) rows.add(new String[]{ rs.getString("loan_id"), rs.getString("item_id"), rs.getString("due_date") });
            rs.close();
        } catch (SQLException e) { System.err.println("✗ LoanDAO.getActiveLoansForMember: " + e.getMessage()); }
        return rows;
    }

    public static void syncAll(List<Loan> loans) {
        String sql = "INSERT OR IGNORE INTO loans (loan_id, member_id, item_id, due_date, return_date, status) VALUES (?,?,?,?,?,?)";
        int count = 0;
        for (Loan loan : loans) {
            try {
                DatabaseManager.executeUpdate(sql, loan.getTransactionId(), loan.getPerson().getId(), loan.getItem().getItemId(),
                        loan.getDueDate().toString(), loan.getReturnDate() != null ? loan.getReturnDate().toString() : null, loan.getStatus());
                count++;
            } catch (SQLException e) { System.err.println("✗ LoanDAO.syncAll skip " + loan.getTransactionId() + ": " + e.getMessage()); }
        }
        System.out.println("✓ LoanDAO: synced " + count + " loans to database.");
    }
}