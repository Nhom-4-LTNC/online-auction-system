package com.auction.shared.protocol.auth;

import java.io.Serializable;

public class LoginRequest implements Serializable {
    private final String email;
    private final String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
    public String getEmail() { return email; }
    public String getPassword() { return password; }

    @Override
    public String toString() {
        return String.format("LoginRequest{email='%s', password='[REDACTED]'}", email);
    }
}
