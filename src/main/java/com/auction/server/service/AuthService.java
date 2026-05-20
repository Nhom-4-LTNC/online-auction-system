package com.auction.server.service;

import com.auction.shared.exception.*;
import com.auction.server.model.user.User;
import com.auction.server.repository.UserRepository;

public class AuthService {

    private static volatile AuthService instance;
    private final UserRepository userRepository = UserRepository.getInstance();
    private final UserService userService = UserService.getInstance(); // Gọi UserService để check Banned

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) instance = new AuthService();
            }
        }
        return instance;
    }

    public com.auction.shared.dto.UserDTO login(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty()) throw new AuctionAppException("Email không được để trống!");
        if (password == null || password.trim().isEmpty()) throw new AuctionAppException("Mật khẩu không được để trống!");

        User user = userRepository.login(email.trim(), password);
        if (user == null) {
            throw new AuthenticationException("Email hoặc mật khẩu không đúng!");
        }

        // Nhờ UserService kiểm tra xem User này có đang bị khóa không
        if (userService.isBanned(user)) {
            throw new UserBannedException(user.getBanEndTime());
        }

        return mapUserToDTO(user);
    }

    public com.auction.shared.dto.UserDTO register(String username, String email, String password) throws Exception {
        String cleanUsername = username != null ? username.trim() : null;
        String cleanEmail = email != null ? email.trim().toLowerCase() : null;

        validateRegisterData(cleanUsername, cleanEmail, password);

        // Kiểm tra tồn tại
        if (userRepository.getUserByEmail(cleanEmail) != null) {
            throw new DuplicateResourceException("Email này đã được đăng ký!");
        }
        if (userRepository.getUserByUsername(cleanUsername) != null) {
            throw new DuplicateResourceException("Tên đăng nhập đã tồn tại!");
        }

        // Tạo mới
        User newUser = new User(cleanUsername, password, cleanEmail);
        userRepository.addUser(newUser);

        return mapUserToDTO(newUser);
    }

    private void validateRegisterData(String username, String email, String password) throws Exception {
        if (username == null || username.isEmpty()) throw new ValidationException("Tên đăng nhập không được để trống!");
        if (email == null || email.isEmpty()) throw new ValidationException("Email không được để trống!");
        if (password == null || password.trim().isEmpty()) throw new ValidationException("Mật khẩu không được để trống!");
    }

    private com.auction.shared.dto.UserDTO mapUserToDTO(User user) {
        return new com.auction.shared.dto.UserDTO(user.getId(), user.getUsername(), user.getEmail());
    }
}