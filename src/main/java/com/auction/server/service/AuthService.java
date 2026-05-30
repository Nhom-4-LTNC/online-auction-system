package com.auction.server.service;

import com.auction.server.model.user.User;
import com.auction.server.repository.UserRepository;
import com.auction.server.security.PasswordHasher;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.Role;
import com.auction.shared.exception.AuthenticationException;
import com.auction.shared.exception.DuplicateResourceException;
import com.auction.shared.exception.UserBannedException;
import com.auction.shared.exception.ValidationException;

public class AuthService {

    private static volatile AuthService instance;
    private final UserRepository userRepository = UserRepository.getInstance();
    private final UserService userService = UserService.getInstance();

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) instance = new AuthService();
            }
        }
        return instance;
    }

    public UserDTO login(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email khong duoc de trong!");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Mat khau khong duoc de trong!");
        }

        User user = userRepository.getUserByEmail(email.trim().toLowerCase());
        if (user == null || !isPasswordValidAndUpgradeIfNeeded(user, password)) {
            throw new AuthenticationException("Email hoac mat khau khong dung!");
        }

        if (userService.isBanned(user)) {
            throw new UserBannedException(user.getBanEndTime());
        }

        user.setBalance(userRepository.getUserBalance(user.getId()));
        return mapUserToDTO(user);
    }

    public UserDTO register(String username, String email, String password) throws Exception {
        String cleanUsername = username != null ? username.trim() : null;
        String cleanEmail = email != null ? email.trim().toLowerCase() : null;

        validateRegisterData(cleanUsername, cleanEmail, password);

        if (userRepository.getUserByEmail(cleanEmail) != null) {
            throw new DuplicateResourceException("Email nay da duoc dang ky!");
        }
        if (userRepository.getUserByUsername(cleanUsername) != null) {
            throw new DuplicateResourceException("Ten dang nhap da ton tai!");
        }

        String passwordHash = PasswordHasher.hash(password);
        User newUser = new User(cleanUsername, passwordHash, cleanEmail, Role.USER);
        userRepository.addUser(newUser);
        newUser.setBalance(userRepository.getUserBalance(newUser.getId()));

        return mapUserToDTO(newUser);
    }

    private void validateRegisterData(String username, String email, String password) throws Exception {
        if (username == null || username.isEmpty()) {
            throw new ValidationException("Ten dang nhap khong duoc de trong!");
        }
        if (email == null || email.isEmpty()) {
            throw new ValidationException("Email khong duoc de trong!");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Mat khau khong duoc de trong!");
        }
    }

    private boolean isPasswordValidAndUpgradeIfNeeded(User user, String rawPassword) throws Exception {
        String storedPassword = user.getPassword();

        if (PasswordHasher.isBcryptHash(storedPassword)) {
            return PasswordHasher.matches(rawPassword, storedPassword);
        }

        if (!rawPassword.equals(storedPassword)) {
            return false;
        }

        user.setPassword(PasswordHasher.hash(rawPassword));
        userRepository.updateUser(user);
        return true;
    }

    private UserDTO mapUserToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getBalance(),
                user.getBanStartTime(),
                user.getBanEndTime()
        );
    }
}
