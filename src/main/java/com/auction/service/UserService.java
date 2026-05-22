package com.auction.service;

import com.auction.model.user.Admin;
import com.auction.model.user.BidderProfile;
import com.auction.model.user.Role;
import com.auction.model.user.User;
import com.auction.repository.UserRepository;
import com.auction.util.SessionManager;

import java.util.List;

public class UserService {

    private static volatile UserService instance;
    private final UserRepository userRepository = UserRepository.getInstance();

    private UserService() {}

    public static UserService getInstance() {
        if (instance == null) {
            synchronized (UserService.class) {
                if (instance == null) instance = new UserService();
            }
        }
        return instance;
    }

    public User login(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty())
            throw new Exception("Email không được để trống!");
        if (password == null || password.trim().isEmpty())
            throw new Exception("Mật khẩu không được để trống!");

        User user = userRepository.login(email.trim(), password);
        if (user == null)
            throw new Exception("Email hoặc mật khẩu không đúng!");

        return user;
    }

    public User register(String username, String email, String password) throws Exception {
        // 1. Chuẩn hóa dữ liệu (cắt khoảng trắng, chuyển email thành chữ thường)
        String cleanUsername = username != null ? username.trim() : null;
        String cleanEmail = email != null ? email.trim().toLowerCase() : null;

        // 2. Kiểm tra định dạng đầu vào cơ bản
        validateRegisterData(cleanUsername, cleanEmail, password);

        // 3. Kiểm tra logic nghiệp vụ (sự tồn tại trong hệ thống)
        checkUserExistence(cleanUsername, cleanEmail);

        // 4. Khởi tạo và lưu trữ
        // (Lưu ý thực tế: Mật khẩu nên được mã hóa (hash) ở bước này trước khi lưu)
        User newUser = new User(cleanUsername, password, cleanEmail);
        userRepository.addUser(newUser);

        return newUser;
    }

    /**
     * Trách nhiệm: Chỉ kiểm tra tính hợp lệ của các chuỗi đầu vào.
     */
    private void validateRegisterData(String username, String email, String password) throws Exception {
        if (username == null || username.isEmpty()) {
            throw new Exception("Tên đăng nhập không được để trống!");
        }
        if (email == null || email.isEmpty()) {
            throw new Exception("Email không được để trống!");
        }
        // Bạn có thể bổ sung thêm logic kiểm tra Regex cho Email ở đây sau này

        if (password == null || password.trim().isEmpty()) {
            throw new Exception("Mật khẩu không được để trống!");
        }
    }

    /**
     * Trách nhiệm: Chỉ tương tác với Database để kiểm tra trùng lặp dữ liệu.
     */

    private void checkUserExistence(String username, String email) throws Exception {
        if (userRepository.getUserByEmail(email) != null) {
            throw new Exception("Email này đã được đăng ký!");
        }
        if (findByUsername(username) != null) {
            throw new Exception("Tên đăng nhập đã tồn tại!");
        }
    }

    public User getUserById(int id) throws Exception {
        User user = userRepository.getUserById(id);
        if (user == null)
            throw new Exception("Không tìm thấy người dùng với ID: " + id);
        return user;
    }

    public User getUserByEmail(String email) throws Exception {
        if (email == null || email.trim().isEmpty())
            throw new Exception("Email không được để trống!");
        User user = userRepository.getUserByEmail(email.trim());
        if (user == null)
            throw new Exception("Không tìm thấy người dùng với email: " + email);
        return user;
    }

    public List<User> getAllUsers() throws Exception {
        return userRepository.getAllUsers();
    }

    public void updateUserBalance(User user, double balance) throws Exception {
        if (user == null) throw new Exception("Người dùng không hợp lệ!");
        // Check current user by ID (don't rely on object identity)
        var sessionUser = SessionManager.getInstance().getCurrentUser();
        if (sessionUser == null || sessionUser.getId() != user.getId()) {
            throw new Exception("Bạn chỉ có thể cập nhật số dư của chính mình!");
        }

        // Delegate to repository method that only updates balance column to avoid overwriting other fields
        userRepository.updateUserBalance(user.getId(), balance);
        // Update in-memory session object as well
        if (user.getBidderProfile() != null) {
            user.getBidderProfile().setBalance(balance);
        }
    }

    public boolean isBanned(User user) {
        return user.getBanEndTime() > System.currentTimeMillis();
    }

    public void applyBan(Admin admin, User target, long durationMillis) throws Exception {
        if (!admin.hasRole(Role.ADMIN))
            throw new Exception("Chỉ Admin mới có thể ban người dùng!");
        long now = System.currentTimeMillis();
        target.setBanStartTime(now);
        target.setBanEndTime(now + durationMillis);
        userRepository.updateUser(target);
    }

    public void removeBan(Admin admin, User target) throws Exception {
        if (!admin.hasRole(Role.ADMIN))
            throw new Exception("Chỉ Admin mới có thể gỡ ban!");
        target.setBanStartTime(0);
        target.setBanEndTime(0);
        userRepository.updateUser(target);
    }

    private User findByUsername(String username) throws Exception {
        String normalized = username.trim();
        for (User user : userRepository.getAllUsers()) {
            if (user.getUsername().equals(normalized)) return user;
        }
        return null;
    }
}
