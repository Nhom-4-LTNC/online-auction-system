package com.auction.shared.protocol.auth;

import com.auction.shared.dto.UserDTO;


import java.io.Serial;
import java.io.Serializable;

public class AuthResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UserDTO user;
    private final String message;
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

    @Override
    public String toString() {
        return String.format("AuthResponse{user=%s, message='%s'}", user, message);
    }
}
