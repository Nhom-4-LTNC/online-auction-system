package com.auction.shared.protocol.admin;

import java.io.Serial;
import java.io.Serializable;

public class ApplyBanResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String message;

    public ApplyBanResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

