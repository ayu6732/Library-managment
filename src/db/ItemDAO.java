package com.library.db;

import com.library.models.items.*;
import java.sql.*;
import java.util.*;

public class ItemDAO {

    public static boolean insertItem(LibraryItem item) {
        String sql = "INSERT OR IGNORE INTO library_items (item_id, type, title, year, available, field1, field2, field3, field4, field5, field6, field7) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            String type = item.getClass().getSimpleName();
            int available = item.isAvailable() ? 1 : 0;
            String f1="", f2="", f3="", f4="", f5="", f6="", f7="";
            if (item instanceof DigitalBook) {
                DigitalBook db = (DigitalBook) item;
                f1=db.getauthor(); f2=db.getpublisher(); f3=db.getISBN();
                f4=String.valueOf(db.getNp()); f5=String.valueOf(db.getFileSize());
                f6=db.getFormat(); f7=db.getDownloadlink();
            } else if (item instanceof Book) {
                Book b = (Book) item;
                f1=b.getauthor(); f2=b.getpublisher(); f3=b.getISBN(); f4=String.valueOf(b.getNp());
            } else if (item instanceof DVD) {
                DVD d = (DVD) item;
                f1=d.getDirector(); f2=String.valueOf(d.getDuration()); f3=d.getRating();
            } else if (item instanceof Magazine) {
                Magazine m = (Magazine) item;
                f1=m.getPublisher(); f2=m.getIsDate(); f3=String.valueOf(m.getVn());
            }
            int rows = DatabaseManager.executeUpdate(sql, item.getItemId(), type, item.getTitle(), item.getYear(), available, f1, f2, f3, f4, f5, f6, f7);
            return rows > 0;
        } catch (SQLException e) { System.err.println("✗ ItemDAO.insertItem: " + e.getMessage()); return false; }
    }

    public static boolean updateAvailability(String itemId, boolean available) {
        try { return DatabaseManager.executeUpdate("UPDATE library_items SET available = ? WHERE item_id = ?", available ? 1 : 0, itemId) > 0; }
        catch (SQLException e) { System.err.println("✗ ItemDAO.updateAvailability: " + e.getMessage()); return false; }
    }

    public static boolean deleteItem(String itemId) {
        try { return DatabaseManager.executeUpdate("DELETE FROM library_items WHERE item_id = ?", itemId) > 0; }
        catch (SQLException e) { System.err.println("✗ ItemDAO.deleteItem: " + e.getMessage()); return false; }
    }

    public static List<LibraryItem> getAllItems() {
        List<LibraryItem> items = new ArrayList<>();
        try {
            ResultSet rs = DatabaseManager.executeQuery("SELECT * FROM library_items");
            while (rs.next()) { LibraryItem item = mapRow(rs); if (item != null) items.add(item); }
            rs.close();
        } catch (SQLException e) { System.err.println("✗ ItemDAO.getAllItems: " + e.getMessage()); }
        return items;
    }

    public static LibraryItem getItemById(String itemId) {
        try {
            ResultSet rs = DatabaseManager.executeQuery("SELECT * FROM library_items WHERE item_id = ?", itemId);
            if (rs.next()) { LibraryItem item = mapRow(rs); rs.close(); return item; }
            rs.close();
        } catch (SQLException e) { System.err.println("✗ ItemDAO.getItemById: " + e.getMessage()); }
        return null;
    }

    public static void syncAll(List<LibraryItem> items) {
        for (LibraryItem item : items) insertItem(item);
        System.out.println("✓ ItemDAO: synced " + items.size() + " items to database.");
    }

    private static LibraryItem mapRow(ResultSet rs) throws SQLException {
        String type=rs.getString("type"); String itemId=rs.getString("item_id");
        String title=rs.getString("title"); int year=rs.getInt("year");
        boolean avail=rs.getInt("available")==1;
        String f1=rs.getString("field1"), f2=rs.getString("field2"), f3=rs.getString("field3"),
               f4=rs.getString("field4"), f5=rs.getString("field5"), f6=rs.getString("field6"), f7=rs.getString("field7");
        LibraryItem item = null;
        try {
            switch (type) {
                case "Book": item = new Book(itemId, title, f1, f2, year, f3, f4!=null?Integer.parseInt(f4):0); break;
                case "DigitalBook": item = new DigitalBook(itemId, title, f1, f2, year, f3, f4!=null?Integer.parseInt(f4):0, f5!=null?Double.parseDouble(f5):0, f6, f7); break;
                case "DVD": item = new DVD(itemId, title, year, f1, f2!=null?Integer.parseInt(f2):0, f3); break;
                case "Magazine": item = new Magazine(itemId, title, year, f1, f2, f3!=null?Integer.parseInt(f3):0); break;
            }
        } catch (Exception e) { System.err.println("✗ ItemDAO.mapRow error for " + itemId + ": " + e.getMessage()); }
        if (item != null && !avail) item.markAsBorrowed();
        return item;
    }
}
