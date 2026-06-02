package com.auction.client.service;

import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.auth.AuthResponse;
import com.auction.shared.protocol.auth.LoginRequest;
import com.auction.shared.protocol.auth.RegisterRequest;

public class AuthClientService extends BaseClientService {

    public AuthResponse login(String email, String password) {
        Request<LoginRequest> request = new Request<>(
                ActionType.LOGIN,
                new LoginRequest(email, password)
        );

        return sendAndExtract(request, AuthResponse.class);
    }

    public AuthResponse register(String username, String email, String password) {
        Request<RegisterRequest> request = new Request<>(
                ActionType.REGISTER,
                new RegisterRequest(username, password, email)
        );

        return sendAndExtract(request, AuthResponse.class);
    }

    public AuthResponse logout() {
        Request<java.io.Serializable> request = new Request<>(ActionType.LOGOUT, null);
        return sendAndExtract(request, AuthResponse.class);
    }
}
