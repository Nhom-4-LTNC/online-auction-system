package com.auction.service;

import com.auction.model.user.User;
import com.auction.repository.UserRepository;

public class UserService {

    private final UserRepository userRepository = UserRepository.getInstance();

    public User login(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new Exception("Email cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new Exception("Password cannot be null or empty");
        }

        // Use repository login method
        User user = userRepository.login(email, password);
        if (user == null) {
            throw new Exception("Invalid email or password");
        }

        return user;
    }

    public User register(String username, String email, String password) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new Exception("Username cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new Exception("Email cannot be null or empty");
        }

        if (password == null) {
            throw new Exception("Password cannot be null");
        }
        // Check if user already exists
        if (userRepository.getUserByEmail(email) != null) {
            throw new Exception("Email already registered");
        }

        // Check if username already exists
        if (findUserByUsername(username) != null) {
            throw new Exception("Username already taken");
        }

        // Create new user with default password (will be changed later)
        User newUser = new User(username, password, email);
        userRepository.addUser(newUser);

        return newUser;
    }

    public User getUserById(int id) throws Exception {
        User user = userRepository.getUserById(id);
        if (user == null) {
            throw new Exception("User not found with ID: " + id);
        }
        return user;
    }

    public User getUserByEmail(String email) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new Exception("Email cannot be null or empty");
        }

        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            throw new Exception("User not found with email: " + email);
        }
        return user;
    }

    public void updateUser(User user) throws Exception {
        if (user == null) {
            throw new Exception("User cannot be null");
        }

        userRepository.updateUser(user);
    }

    // Helper method to find user by username
    private User findUserByUsername(String username) {
        if (username == null) return null;

        String normalizedUsername = username.trim();
        for (User user : userRepository.getAllUser()) {
            if (user.getUsername().equals(normalizedUsername)) {
                return user;
            }
        }
        return null;
    }
}
