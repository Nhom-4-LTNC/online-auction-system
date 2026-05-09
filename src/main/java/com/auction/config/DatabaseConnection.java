package com.auction.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    public static final String URL = "jdbc:mysql://localhost:3306/online_auction_db?useSSL=false&serverTimezone=UTC";
    public static final String USER = "president23";
    public static final String PASSWORD = "d4c";

    public static Connection getConnection() {
       try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
           System.out.println("Database connection established successfully.");
            return conn;
       } catch (SQLException e) {
            System.out.println("Error connecting to database: " + e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found: " + e.getMessage());
            return null;
        }
    }
}
