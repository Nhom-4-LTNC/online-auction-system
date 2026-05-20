package com.auction.protocol.auction;

import java.io.Serial;

public class CloseAuctionRequest implements java.io.Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    public CloseAuctionRequest(int auctionId) {
        this.auctionId = auctionId;
    }
    public int getAuctionId() { return auctionId; }
}
