package com.auction.shared.protocol;

import java.io.Serial;
import java.io.Serializable;

public class Response <T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ActionType action;
    private final ResponseStatus status;
    private final T payload;
    private final String errorMessage;

    public Response(ActionType action, T payload) {
        this.action = action;
        this.status =  ResponseStatus.SUCCESS;
        this.payload = payload;
        this.errorMessage = null;
    }

    public Response(ActionType action, String errorMessage) {
        this.action = action;
        this.status =  ResponseStatus.ERROR;
        this.payload = null;
        this.errorMessage = errorMessage;
    }

    public static <T> Response <T> success(ActionType action, T payload) {
        return new Response<>(action, payload);
    }

    public static <T> Response <T> error(ActionType action, String errorMessage) {
        return new Response<>(action, errorMessage);
    }

    public ActionType getAction() { return action; }
    public ResponseStatus getStatus() { return status; }
    public T getPayload() { return payload; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isSuccess() { return status == ResponseStatus.SUCCESS; }

}
