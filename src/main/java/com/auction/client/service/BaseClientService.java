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
            throw new ClientServiceException("Không thể gửi yêu cầu tới server.");

        }

        if (response == null) {
            throw new ClientServiceException("Không nhận được phản hồi từ server.");
        }
        if (!response.isSuccess()) {
            String message = response.getErrorMessage();
            throw new ClientServiceException(message == null || message.isBlank()
                    ? "Server trả về lỗi."
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
            throw new ClientServiceException("Dữ liệu phản hồi không hợp lệ.");
        }
        return payloadType.cast(payload);
    }
}
