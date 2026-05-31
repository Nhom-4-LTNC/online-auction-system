package com.auction.shared.protocol.bid;

import java.io.Serial;
import java.io.Serializable;

public class SendAuctionChatRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final String message;

    public SendAuctionChatRequest(int auctionId, String message) {
        this.auctionId = auctionId;
        this.message = message;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public String getMessage() {
        return message;
    }
}

