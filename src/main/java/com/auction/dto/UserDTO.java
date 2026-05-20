package com.auction.dto;

import java.io.Serial;
import java.io.Serializable;

public class UserDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final String username;
    private final String email;

    public UserDTO(int userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public int getId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
