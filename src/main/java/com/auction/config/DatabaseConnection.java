package com.auction.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final Properties properties = new Properties();

    /* * KHỐI STATIC: Chỉ chạy ĐÚNG 1 LẦN khi class được nạp vào bộ nhớ.
     * Thích hợp để đọc file cấu hình và load Driver.
     */
    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                // Lỗi chí mạng: Thiếu file cấu hình -> Đánh sập app ngay lập tức
                throw new RuntimeException("[FATAL ERROR] Không tìm thấy file db.properties trong thư mục resources!");
            }
            properties.load(input);

            // Load MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (ClassNotFoundException e) {
            // Lỗi chí mạng: Thiếu thư viện JDBC trong file pom.xml
            throw new RuntimeException("[FATAL ERROR] Không tìm thấy MySQL JDBC Driver!", e);
        } catch (Exception e) {
            throw new RuntimeException("[FATAL ERROR] Lỗi khởi tạo hệ thống Database: " + e.getMessage(), e);
        }
    }

    /*
     * Hàm lấy kết nối: Chỉ ném SQLException.
     * Các lỗi ClassNotFound đã được xử lý ở khối static.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(
                properties.getProperty("db.url"),
                properties.getProperty("db.user"),
                properties.getProperty("db.password")
        );
        return conn;
    }
}