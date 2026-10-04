package com.library.db;

import java.sql.*;

public class DatabaseManager {

    // Change these 3 lines to match your MySQL setup
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/my_database";
    private static final String DB_USER = "root";       // phpMyAdmin username
    private static final String DB_PASS = "";           // phpMyAdmin password (empty by default)

    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try { connection.close(); connection = null; }
            catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static boolean testConnection() {
        try {
            getConnection();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void initializeSchema() {
        // Tables already created via schema.sql in phpMyAdmin
        // Also ensure the app_settings table exists for persisted settings
        try {
            getConnection();
            String createSettings =
                "CREATE TABLE IF NOT EXISTS app_settings (" +
                "  setting_key   VARCHAR(100) PRIMARY KEY, " +
                "  setting_value TEXT NOT NULL" +
                ")";
            executeUpdate(createSettings);
        } catch (SQLException e) {
            System.err.println("DB connection failed: " + e.getMessage());
        }
    }

    /** Persist a key-value setting (e.g. admin password) to the database. */
    public static void saveSetting(String key, String value) {
        try {
            // MySQL: INSERT ... ON DUPLICATE KEY UPDATE
            String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) " +
                         "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
            executeUpdate(sql, key, value);
        } catch (SQLException e) {
            System.err.println("✗ DatabaseManager.saveSetting: " + e.getMessage());
        }
    }

    /** Load a setting value by key; returns defaultValue if not found. */
    public static String loadSetting(String key, String defaultValue) {
        try {
            ResultSet rs = executeQuery(
                "SELECT setting_value FROM app_settings WHERE setting_key = ?", key);
            if (rs.next()) {
                String val = rs.getString("setting_value");
                rs.close();
                return val;
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("✗ DatabaseManager.loadSetting: " + e.getMessage());
        }
        return defaultValue;
    }

    public static void beginTransaction() throws SQLException {
        getConnection().setAutoCommit(false);
    }

    public static void commit() throws SQLException {
        getConnection().commit();
        getConnection().setAutoCommit(true);
    }

    public static void rollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static ResultSet executeQuery(String sql, Object... params) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
        return ps.executeQuery();
    }

    public static int executeUpdate(String sql, Object... params) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
        int rows = ps.executeUpdate();
        ps.close();
        return rows;
    }
}