package com.auction.client.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.auction.client.network.Client;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.admin.ApplyBanRequest;
import com.auction.shared.protocol.admin.ApplyBanResponse;
import com.auction.shared.protocol.admin.GetAllUsersResponse;
import com.auction.shared.protocol.admin.RemoveBanRequest;
import com.auction.shared.protocol.admin.RemoveBanResponse;

public class AdminClientService {

    private final Client client;

    public AdminClientService() {
        this.client = Client.getInstance();
    }

    public List<UserDTO> getAllUsers() throws IOException {
        try {
            Response<?> response = client.sendRequestAndWait(new Request<>(ActionType.GET_ALL_USERS, null), 5000);
            if (!response.isSuccess()) {
                throw new ClientServiceException(response.getErrorMessage());
            }

            Object payload = response.getPayload();
            if (!(payload instanceof GetAllUsersResponse resp)) {
                throw new ClientServiceException("Invalid payload: expected GetAllUsersResponse");
            }

            return resp.getUsers() == null ? Collections.emptyList() : resp.getUsers();
        } catch (ClientServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to get all users", e);
        }
    }

    public ApplyBanResponse applyBan(int targetUserId, long durationMillis) throws IOException {
        try {
            ApplyBanRequest req = new ApplyBanRequest(targetUserId, durationMillis);
            Response<?> response = client.sendRequestAndWait(new Request<>(ActionType.APPLY_BAN, req), 5000);

            if (!response.isSuccess()) {
                throw new ClientServiceException(response.getErrorMessage());
            }

            Object payload = response.getPayload();
            if (!(payload instanceof ApplyBanResponse r)) {
                throw new ClientServiceException("Invalid payload: expected ApplyBanResponse");
            }
            return r;
        } catch (ClientServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to apply ban", e);
        }
    }

    public RemoveBanResponse removeBan(int targetUserId) throws IOException {
        try {
            RemoveBanRequest req = new RemoveBanRequest(targetUserId);
            Response<?> response = client.sendRequestAndWait(new Request<>(ActionType.REMOVE_BAN, req), 5000);

            if (!response.isSuccess()) {
                throw new ClientServiceException(response.getErrorMessage());
            }

            Object payload = response.getPayload();
            if (!(payload instanceof RemoveBanResponse r)) {
                throw new ClientServiceException("Invalid payload: expected RemoveBanResponse");
            }
            return r;
        } catch (ClientServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to remove ban", e);
        }
    }
}

