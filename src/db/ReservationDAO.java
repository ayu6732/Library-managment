package com.library.db;

import com.library.models.transactions.Reservation;
import java.sql.*;
import java.util.*;

public class ReservationDAO {

    public static boolean insertReservation(Reservation res) {
        String sql = "INSERT OR IGNORE INTO reservations (reservation_id, member_id, item_id, expiration_date, notified, status) VALUES (?,?,?,?,?,?)";
        try {
            return DatabaseManager.executeUpdate(sql, res.getTransactionId(), res.getPerson().getId(), res.getItem().getItemId(), res.getExpirationDate().toString(), res.isNotified() ? 1 : 0, res.getStatus()) > 0;
        } catch (SQLException e) { System.err.println("✗ ReservationDAO.insertReservation: " + e.getMessage()); return false; }
    }

    public static boolean updateStatus(String reservationId, String status) {
        try { return DatabaseManager.executeUpdate("UPDATE reservations SET status = ? WHERE reservation_id = ?", status, reservationId) > 0; }
        catch (SQLException e) { System.err.println("✗ ReservationDAO.updateStatus: " + e.getMessage()); return false; }
    }

    public static List<String[]> getAllReservationsRaw() {
        List<String[]> rows = new ArrayList<>();
        try {
            ResultSet rs = DatabaseManager.executeQuery("SELECT reservation_id, member_id, item_id, expiration_date, notified, status FROM reservations");
            while (rs.next()) rows.add(new String[]{ rs.getString("reservation_id"), rs.getString("member_id"), rs.getString("item_id"), rs.getString("expiration_date"), rs.getString("notified"), rs.getString("status") });
            rs.close();
        } catch (SQLException e) { System.err.println("✗ ReservationDAO.getAllReservationsRaw: " + e.getMessage()); }
        return rows;
    }

    public static void syncAll(List<Reservation> reservations) {
        int count = 0;
        for (Reservation res : reservations) if (insertReservation(res)) count++;
        System.out.println("✓ ReservationDAO: synced " + count + " reservations to database.");
    }
}