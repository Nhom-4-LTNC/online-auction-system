package com.auction.repository;

import com.auction.config.DatabaseConnection;
import com.auction.model.user.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

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
        String sql = "INSERT INTO users (username, email, password, role, balance) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, "USER"); // Vai trò mặc định
            stmt.setDouble(5, 0.0);    // Số dư mặc định

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Thêm người dùng thất bại, không có dòng nào được thêm.");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("[UserRepository - addUser] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi khi thêm người dùng: " + e.getMessage(), e);
        }
    }

    public User login(String email, String password) throws Exception {
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
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("[UserRepository - login] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi hệ thống khi xác thực đăng nhập: " + e.getMessage(), e);
        }
        return null; // Không tìm thấy (sai tài khoản/mật khẩu)
    }

    public void updateUser(User user) throws Exception {
        // Đã bổ sung cập nhật ban_start_time và ban_end_time từ User model
        String sql = "UPDATE users SET username = ?, email = ?, password = ?, role = ?, balance = ?, ban_start_time = ?, ban_end_time = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());

            // Xử lý Role
            String roleStr = user.hasRole(com.auction.model.user.Role.ADMIN) ? "ADMIN" : "USER";
            stmt.setString(4, roleStr);

            // Xử lý số dư
            double balance = (user.getBidderProfile() != null) ? user.getBidderProfile().getBalance() : 0.0;
            stmt.setDouble(5, balance);

            // Xử lý thời gian Ban
            stmt.setLong(6, user.getBanStartTime());
            stmt.setLong(7, user.getBanEndTime());

            stmt.setInt(8, user.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("Không tìm thấy người dùng với ID = " + user.getId() + " để cập nhật.");
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("[UserRepository - updateUser] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi khi cập nhật thông tin người dùng: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật riêng trường balance cho người dùng (an toàn hơn khi chỉ cần thay đổi số dư).
     */
    public void updateUserBalance(int userId, double balance) throws Exception {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, balance);
            stmt.setInt(2, userId);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("Không tìm thấy người dùng với ID = " + userId + " để cập nhật balance.");
            }

        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("[UserRepository - updateUserBalance] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi khi cập nhật số dư người dùng: " + e.getMessage(), e);
        }
    }

    public User getUserByEmail(String email) throws Exception {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("[UserRepository - getUserByEmail] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi truy xuất cơ sở dữ liệu (Email): " + e.getMessage(), e);
        }
        return null;
    }

    // TỐI ƯU HÓA: Thêm hàm tìm kiếm trực tiếp bằng username bằng SQL
    public User getUserByUsername(String username) throws Exception {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("[UserRepository - getUserByUsername] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi truy xuất cơ sở dữ liệu (Username): " + e.getMessage(), e);
        }
        return null;
    }

    public User getUserById(int id) throws Exception {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("[UserRepository - getUserById] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi truy xuất cơ sở dữ liệu (ID): " + e.getMessage(), e);
        }
        return null;
    }

    public List<User> getAllUsers() throws Exception {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("[UserRepository - getAllUsers] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi lấy danh sách người dùng: " + e.getMessage(), e);
        }
        return users;
    }

    /**
     * Truy vấn và trả về nhiều người dùng theo danh sách ID (trả về Map id->User).
     */
    public Map<Integer, User> getUsersByIds(List<Integer> ids) throws Exception {
        Map<Integer, User> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;

        // Build placeholders
        String placeholders = ids.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT * FROM users WHERE id IN (" + placeholders + ")";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int idx = 1;
            for (Integer id : ids) stmt.setInt(idx++, id);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User u = mapResultSetToUser(rs);
                    map.put(u.getId(), u);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("[UserRepository - getUsersByIds] Lỗi: " + e.getMessage());
            throw new Exception("Lỗi khi tải danh sách người dùng: " + e.getMessage(), e);
        }
        return map;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String password = rs.getString("password");
        double balance = rs.getDouble("balance");
        String roleStr = rs.getString("role");

        User user = new User(id, username, password, email);

        if (user.getBidderProfile() != null) {
            user.getBidderProfile().setBalance(balance);
        }

        if ("ADMIN".equalsIgnoreCase(roleStr)) {
            user.addRole(com.auction.model.user.Role.ADMIN);
        }

        try {
            user.setBanStartTime(rs.getLong("ban_start_time"));
            user.setBanEndTime(rs.getLong("ban_end_time"));
        } catch (SQLException ignored) {
            // Đề phòng trường hợp database cũ chưa có cột này thì không bị crash
        }

        return user;
    }
}