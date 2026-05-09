package com.auction.service;

import com.auction.model.user.Admin;
import com.auction.model.user.Role;
import com.auction.model.user.User;
import com.auction.repository.UserRepository;

import java.util.List;

/*
 * UserService — tầng Service cho Người Dùng
 *
 * Trách nhiệm (thay thế cho UserManager + BanManager cũ):
 *   1. Đăng nhập, đăng ký, tìm kiếm người dùng
 *   2. Quản lý lệnh ban/unban (trước đây nằm trong BanManager)
 *   3. Lưu thay đổi qua UserRepository
 *
 * Luồng dữ liệu: Controller → UserService → UserRepository → users.dat
 */
public class UserService {

    // Singleton: cả ứng dụng chỉ dùng một instance
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
        if (username == null || username.trim().isEmpty())
            throw new Exception("Tên đăng nhập không được để trống!");
        if (email == null || email.trim().isEmpty())
            throw new Exception("Email không được để trống!");
        if (password == null || password.trim().isEmpty())
            throw new Exception("Mật khẩu không được để trống!");

        // Kiểm tra email đã tồn tại chưa
        if (userRepository.getUserByEmail(email.trim()) != null)
            throw new Exception("Email này đã được đăng ký!");

        // Kiểm tra username đã tồn tại chưa
        if (findUserByUsername(username) != null)
            throw new Exception("Tên đăng nhập đã tồn tại!");

        User newUser = new User(username.trim(), password, email.trim());
        userRepository.addUser(newUser);
        return newUser;
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

    public List<User> getAllUsers() {
        return userRepository.getAllUser();
    }

    public void updateUser(User user) throws Exception {
        if (user == null) throw new Exception("Người dùng không hợp lệ!");
        userRepository.updateUser(user);
    }


    public boolean isBanned(User user) {
        return user.getBanEndTime() > System.currentTimeMillis();
    }

    // Admin ra lệnh ban: khóa tài khoản trong durationMillis mili-giây
    public void applyBan(Admin admin, User target, long durationMillis) throws Exception {
        if (!admin.hasRole(Role.ADMIN))
            throw new Exception("Chỉ Admin mới có thể ban người dùng!");
        long now = System.currentTimeMillis();
        target.setBanStartTime(now);
        target.setBanEndTime(now + durationMillis);
        userRepository.updateUser(target);
    }

    // Admin gỡ ban: mở khóa tài khoản
    public void removeBan(Admin admin, User target) throws Exception {
        if (!admin.hasRole(Role.ADMIN))
            throw new Exception("Chỉ Admin mới có thể gỡ ban!");
        target.setBanStartTime(0);
        target.setBanEndTime(0);
        userRepository.updateUser(target);
    }


    private User findUserByUsername(String username) {
        String normalized = username.trim();
        for (User user : userRepository.getAllUser()) {
            if (user.getUsername().equals(normalized)) return user;
        }
        return null;
    }
}
