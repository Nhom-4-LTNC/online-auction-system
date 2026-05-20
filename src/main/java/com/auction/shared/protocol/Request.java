package com.auction.shared.protocol;

import java.io.Serial;
import java.io.Serializable;

public class Request<T extends Serializable> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ActionType type;
    private final T payload;
    public Request(ActionType type, T message) {
        this.type = type;
        this.payload = message;
    }
    public ActionType getAction() {
        return type;
    }
    public T getPayload() {
        return payload;
    }
}
