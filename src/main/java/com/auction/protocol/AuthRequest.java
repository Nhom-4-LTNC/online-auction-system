package com.auction.protocol;

import java.io.Serial;
import java.io.Serializable;

public class AuthRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 6711523004565269955L;
    private final ActionType requestType;
    private String username;
    private final String password;
    private final String email;
    //Register constructor
    public AuthRequest(String username, String email, String password) {
        this.requestType = ActionType.REGISTER;
        this.username = username;
        this.email = email;
        this.password = password;
    }
    //Login constructor
    public AuthRequest(String email, String password) {
        this.requestType = ActionType.LOGIN;
        this.email = email;
        this.password = password;
    }

    public String getUsername() { return username;}
    public String getPassword() { return password;}
    public ActionType getRequestType() { return requestType;}
    public String getEmail() { return email;}
}
