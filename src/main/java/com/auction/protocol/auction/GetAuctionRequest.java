package com.auction.protocol.auction;

import java.io.Serial;
import java.io.Serializable;

public class GetAuctionRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;

    public GetAuctionRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() {
        return auctionId;
    }
}