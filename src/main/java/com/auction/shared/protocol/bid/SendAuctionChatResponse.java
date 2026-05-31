package com.auction.shared.protocol.bid;

import java.io.Serial;
import java.io.Serializable;

public class SendAuctionChatResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String message;

    public SendAuctionChatResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

