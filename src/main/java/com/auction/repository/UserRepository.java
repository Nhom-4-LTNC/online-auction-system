package com.auction.repository;

import com.auction.config.DatabaseConnection;
import com.auction.model.user.User;
import java.sql.*;
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

    public synchronized void addUser(User user) {
        String sql = "INSERT INTO users (username, email, password, role, balance) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
    PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, "USER");
            stmt.setDouble(5, 0.0);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
            System.out.println("User added successfully: " + user.getEmail());
        } catch (SQLException e) {
            System.out.println("Error adding user: " + e.getMessage());
        }
    }

    public synchronized User login(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                   return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during login: " + e.getMessage());
        }
        return null;
    }
    public synchronized void updateUser(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, password = ?, role = ?, balance = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setDouble(3, user.getBidderProfile().getBalance());
            stmt.setInt(4, user.getId());

            stmt.executeUpdate();
            System.out.println("User updated successfully: " + user.getId());
        } catch (SQLException e) {
            System.out.println("Error updating user: " + e.getMessage());
        }
    }
    public synchronized User getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching user by email: " + e.getMessage());
        }
        return null;
    }
    public synchronized User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching user by ID: " + e.getMessage());
        }
        return null;
    }
    public synchronized List<User> getAllUser() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching all users: " + e.getMessage());
        }
        return users;
    }
    private User mapResultSetToUser(ResultSet rs) throws SQLException{
       int id =  rs.getInt("id");
       String username = rs.getString("username");
       String email = rs.getString("email");
       String password = rs.getString("password");
       double balance = rs.getDouble("balance");

       User user = new User(id, username, email, password);
       if (user.getBidderProfile() != null) {
           user.getBidderProfile().setBalance(balance);
       }

       return user;
    }

}
