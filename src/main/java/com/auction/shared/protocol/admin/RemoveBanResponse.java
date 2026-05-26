package com.auction.shared.protocol.admin;

import java.io.Serial;
import java.io.Serializable;

public class RemoveBanResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String message;

    public RemoveBanResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

