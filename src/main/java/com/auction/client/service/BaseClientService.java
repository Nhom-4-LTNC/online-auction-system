package com.auction.client.service;

import com.auction.client.network.Client;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;

public abstract class BaseClientService {

    protected static final long DEFAULT_TIMEOUT_MILLIS = 10_000L;

    protected final Client client = Client.getInstance();

    protected <T> T sendAndExtract(Request<?> request, Class<T> payloadType) {
        Response<?> response;
        try {
            response = client.sendRequestAndWait(request, DEFAULT_TIMEOUT_MILLIS);
        } catch (Exception e) {
            throw new ClientServiceException("Failed to send request to server");

        }

        if (response == null) {
            throw new ClientServiceException("No response from server");
        }
        if (!response.isSuccess()) {
            String message = response.getErrorMessage();
            throw new ClientServiceException(message == null || message.isBlank()
                    ? "Server returned an error"
                    : message);
        }

        Object payload = response.getPayload();
        if (payloadType == Void.class || payloadType == Void.TYPE) {
            return null;
        }
        if (payload == null) {
            return null;
        }
        if (!payloadType.isInstance(payload)) {
            throw new ClientServiceException("Invalid response payload type");
        }
        return payloadType.cast(payload);
    }
}
