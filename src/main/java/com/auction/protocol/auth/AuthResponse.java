package com.auction.protocol.auth;

import com.auction.dto.UserDTO;


import java.io.Serial;
import java.io.Serializable;

public class AuthResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private UserDTO user;
    private String message;
    public  AuthResponse(UserDTO user, String message) {

        this.user = user;
        this.message = message;
    }

    public UserDTO getUser() {
        return user;
    }
    public String getMessage() {
        return message;
    }

}
