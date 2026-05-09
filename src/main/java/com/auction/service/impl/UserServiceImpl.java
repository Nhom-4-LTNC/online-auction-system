package com.auction.service.impl;

import com.auction.model.user.User;
import com.auction.model.user.UserManager;
import com.auction.service.UserService;

import java.util.Collection;

public class UserServiceImpl implements UserService {

    private final UserManager userManager = UserManager.getInstance();

    @Override
    public User login(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new Exception("Email cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new Exception("Password cannot be null or empty");
        }

        // Find user by email
        User user = userManager.getUserByEmail(email);
        if (user == null) {
            throw new Exception("User not found");
        }

        // Check password
        if (!user.getPwd().equals(password)) {
            throw new Exception("Invalid password");
        }

        return user;
    }

    @Override
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
        if (userManager.getUserByEmail(email) != null) {
            throw new Exception("Email already registered");
        }

        // Check if username already exists
        if (findUserByUsername(username) != null) {
            throw new Exception("Username already taken");
        }

        // Create new user with default password (will be changed later)
        User newUser = new User(username, password, email);
        userManager.addUser(newUser);

        return newUser;
    }

    @Override
    public User getUserById(int id) throws Exception {
        User user = userManager.getUserById(id);
        if (user == null) {
            throw new Exception("User not found with ID: " + id);
        }
        return user;
    }

    @Override
    public User getUserByEmail(String email) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new Exception("Email cannot be null or empty");
        }

        User user = userManager.getUserByEmail(email);
        if (user == null) {
            throw new Exception("User not found with email: " + email);
        }
        return user;
    }

    @Override
    public void updateUser(User user) throws Exception {
        if (user == null) {
            throw new Exception("User cannot be null");
        }

        // For now, UserManager doesn't have update method
        // We'll assume the user object is already updated in memory
        // TODO: Implement proper update when Repository pattern is added
    }

    // Helper method to find user by username
    private User findUserByUsername(String username) {
        if (username == null) return null;

        String normalizedUsername = username.trim();
        for (User user : userManager.getAllUsers()) {
            if (user.getUsername().equals(normalizedUsername)) {
                return user;
            }
        }
        return null;
    }
}
