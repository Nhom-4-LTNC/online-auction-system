package com.auction.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    public static final String URL = "jdbc:mysql://4P1uXCyyKSzbDay.root:CK9pnI67aQFR4MuW@gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/auction_db";
    public static final String USER = "4P1uXCyyKSzbDay.root";
    public static final String PASSWORD = "CK9pnI67aQFR4MuW";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connection established successfully.");
            return conn;
    }
}
