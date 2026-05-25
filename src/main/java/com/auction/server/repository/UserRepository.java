package com.auction.server.repository;

import com.auction.server.database.DatabaseConnection;
import com.auction.shared.enums.Role;
import com.auction.server.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private static volatile UserRepository instance;

    private UserRepository() {}

    public static UserRepository getInstance() {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) instance = new UserRepository();
            }
        }
        return instance;
    }

    public void addUser(User user) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            addUser(conn, user);
        }
    }

    public void addUser(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, password, role, balance, ban_start_time, ban_end_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.isAdmin() ? Role.ADMIN.name() : Role.USER.name());
            stmt.setDouble(5, user.getBalance());
            stmt.setLong(6, user.getBanStartTime());
            stmt.setLong(7, user.getBanEndTime());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insert user affected no rows.");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        }
    }

    public User login(String email, String password) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return login(conn, email, password);
        }
    }

    public User login(Connection conn, String email, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public void updateUser(User user) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            updateUser(conn, user);
        }
    }

    public void updateUser(Connection conn, User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ?, password = ?, role = ?, " +
                "balance = ?, ban_start_time = ?, ban_end_time = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.isAdmin() ? Role.ADMIN.name() : Role.USER.name());
            stmt.setDouble(5, user.getBalance());
            stmt.setLong(6, user.getBanStartTime());
            stmt.setLong(7, user.getBanEndTime());
            stmt.setInt(8, user.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("No user found for id=" + user.getId());
            }
        }
    }

    public User getUserByEmail(String email) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getUserByEmail(conn, email);
        }
    }

    public User getUserByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }
    public double addUserBalance(Connection conn, int userId, double amount) throws SQLException {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, userId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Không tìm thấy User với ID = " + userId);
            }
        }

        User updatedUser = getUserById(conn, userId);
        if (updatedUser == null) {
            throw new SQLException("Không tìm thấy User sau khi cập nhật balance, ID = " + userId);
        }

        return updatedUser.getBalance();
    }
    public double getUserBalance(Connection conn, int userId) throws SQLException {
        String sql = "SELECT balance FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        throw new SQLException("Không tìm thấy balance cho User với ID = " + userId);
    }
    public User getUserByUsername(String username) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getUserByUsername(conn, username);
        }
    }

    public User getUserByUsername(Connection conn, String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public User getUserById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getUserById(conn, id);
        }
    }

    public User getUserById(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public List<User> getAllUsers() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAllUsers(conn);
        }
    }

    public List<User> getAllUsers(Connection conn) throws SQLException {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        Role role = Role.USER;
        String roleStr = rs.getString("role");
        if (Role.ADMIN.name().equalsIgnoreCase(roleStr)) {
            role = Role.ADMIN;
        }

        User user = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("email"),
                role
        );
        user.setBalance(rs.getDouble("balance"));
        user.setBanStartTime(rs.getLong("ban_start_time"));
        user.setBanEndTime(rs.getLong("ban_end_time"));
        return user;
    }
}
