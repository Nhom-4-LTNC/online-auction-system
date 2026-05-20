package com.auction.protocol.auth;

import com.auction.protocol.ActionType;

import java.io.Serial;
import java.io.Serializable;

public class AuthRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 6711523004565269955L;
    private String username;
    private final String password;
    private final String email;
    //Register constructor
    public AuthRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    //Login constructor
    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getUsername() { return username;}
    public String getPassword() { return password;}
    public String getEmail() { return email;}
}
