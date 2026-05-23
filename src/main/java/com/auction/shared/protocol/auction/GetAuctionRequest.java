package com.auction.shared.protocol.auction;

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

    @Override
    public String toString() {
        return String.format("GetAuctionRequest{auctionId=%d}", auctionId);
    }
}