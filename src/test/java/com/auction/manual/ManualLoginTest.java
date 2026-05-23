package com.auction.manual;

import com.auction.shared.network.NetworkConfig;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.auth.LoginRequest;

public class ManualLoginTest {
    public static void main(String[] args) throws Exception {
        try (ManualClient client = new ManualClient(NetworkConfig.SERVER_IP, NetworkConfig.PORT)) {
            LoginRequest payload = new LoginRequest("test@gmail.com", "Test@123");

            Response<?> response = client.send(
                    new Request<>(ActionType.LOGIN, payload)
            );

            System.out.println("Status: " + response.getStatus());
            System.out.println("Payload: " + response.getPayload());
            System.out.println("Error: " + response.getErrorMessage());
        }
    }
}
