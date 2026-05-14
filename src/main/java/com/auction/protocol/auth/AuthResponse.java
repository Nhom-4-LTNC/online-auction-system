package com.auction.protocol.auth;

import com.auction.model.user.User;
import com.auction.protocol.ActionType;

import java.io.Serial;
import java.io.Serializable;

public class AuthResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = -8796839392370666593L;
    private final ActionType responseType;
    private User user;
    private String message;
    public  AuthResponse(ActionType responseType, User user, String message) {
        this.responseType = responseType;
        this.user = user;
        this.message = message;
    }

    public ActionType getResponseType() {
        return responseType;
    }
    public User getUser() {
        return user;
    }
    public String getMessage() {
        return message;
    }
    @Override
    public String toString() {
        return "AuthResponse{" +
                "responseType=" + responseType +
                ", user=" + user +
                ", message='" + message + '\'' +
                '}';
    }
}
