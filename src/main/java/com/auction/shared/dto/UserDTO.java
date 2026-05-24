package com.auction.shared.dto;

import com.auction.shared.enums.Role;

import java.io.Serial;
import java.io.Serializable;

public class UserDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final String username;
    private final String email;
    private Role role;

    public UserDTO(int userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public UserDTO(int userId, String username, String email, Role role) {
        this(userId, username, email);
        this.role = role;
    }
    public int getId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }

    @Override
    public String toString() {
        return String.format("UserDTO{userId=%d, username='%s', email='%s', role='%s'}", userId, username, email, role == null ? "N/A" : role.name());
    }
}
