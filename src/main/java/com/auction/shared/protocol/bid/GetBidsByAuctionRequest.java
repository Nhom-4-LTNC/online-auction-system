package com.auction.shared.protocol.bid;

import java.io.Serial;

public class GetBidsByAuctionRequest implements java.io.Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;

    public GetBidsByAuctionRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    @Override
    public String toString() {
        return String.format("GetBidsByAuctionRequest{auctionId=%d}", auctionId);
    }
}
