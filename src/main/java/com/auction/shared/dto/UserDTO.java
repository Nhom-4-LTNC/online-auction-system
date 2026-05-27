package com.auction.shared.dto;

import java.io.Serial;
import java.io.Serializable;

import com.auction.shared.enums.Role;

public class UserDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final String username;
    private final String email;
    private Role role;

    // BAN ATTRIBUTES
    private final long banStartTime;
    private final long banEndTime;

    public UserDTO(int userId, String username, String email) {
        this(userId, username, email, null, 0L, 0L);
    }

    public UserDTO(int userId, String username, String email, Role role) {
        this(userId, username, email, role, 0L, 0L);
    }

    public UserDTO(int userId, String username, String email, Role role, long banStartTime, long banEndTime) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.banStartTime = banStartTime;
        this.banEndTime = banEndTime;
    }

    public int getId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public long getBanStartTime() {
        return banStartTime;
    }

    public long getBanEndTime() {
        return banEndTime;
    }

    @Override
    public String toString() {
        return String.format(
                "UserDTO{userId=%d, username='%s', email='%s', role='%s', banEndTime=%d}",
                userId,
                username,
                email,
                role == null ? "N/A" : role.name(),
                banEndTime
        );
    }
}

