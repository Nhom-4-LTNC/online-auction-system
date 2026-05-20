package com.auction.protocol.auction;

import java.io.Serial;
import java.io.Serializable;

/**
 * Client -> Server: request to subscribe/unsubscribe to updates for a specific auctionId.
 */
public class SubscribeAuctionRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final boolean subscribe; // true = subscribe, false = unsubscribe

    public SubscribeAuctionRequest(int auctionId, boolean subscribe) {
        this.auctionId = auctionId;
        this.subscribe = subscribe;
    }

    public int getAuctionId() { return auctionId; }
    public boolean isSubscribe() { return subscribe; }
}

