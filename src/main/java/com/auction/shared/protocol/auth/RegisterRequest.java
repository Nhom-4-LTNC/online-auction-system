package com.auction.shared.protocol.auth;

import java.io.Serializable;

public class RegisterRequest implements Serializable {
    private final String username;
    private final String password;
    private final String email;

    public RegisterRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return String.format("RegisterRequest{username='%s', email='%s'}", username, email);
    }
}
