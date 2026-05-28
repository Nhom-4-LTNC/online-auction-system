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
    private double balance;

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
        this(userId, username, email, role, 0.0, banStartTime, banEndTime);
    }

    public UserDTO(int userId, String username, String email, Role role, double balance,
                   long banStartTime, long banEndTime) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.balance = balance;
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

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
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
                "UserDTO{userId=%d, username='%s', email='%s', role='%s', balance=%.2f, banEndTime=%d}",
                userId,
                username,
                email,
                role == null ? "N/A" : role.name(),
                balance,
                banEndTime
        );
    }
}

